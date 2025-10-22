package com.lumen.build;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import org.gradle.api.GradleException;
import org.gradle.api.artifacts.transform.InputArtifact;
import org.gradle.api.artifacts.transform.TransformAction;
import org.gradle.api.artifacts.transform.TransformOutputs;
import org.gradle.api.artifacts.transform.TransformParameters;
import org.gradle.api.file.FileSystemLocation;
import org.gradle.api.provider.Provider;

public abstract class SanitizePromptHeaderTransform implements TransformAction<TransformParameters.None> {
    private static final String SANITIZED_SUFFIX = "-sanitized-v10";
    private static final String PLACEHOLDER_REPLACEMENT = "%1$s";
    private static final Pattern PROMPT_HEADER_PATTERN =
            Pattern.compile(
                    "(<string\\b[^>]*\\bname\\s*=\\s*(['\"])prompt_header\\2[^>]*>)(.*?)(</string>)",
                    Pattern.DOTALL);

    @InputArtifact
    public abstract Provider<FileSystemLocation> getInputArtifact();

    @Override
    public void transform(TransformOutputs outputs) {
        File input = getInputArtifact().get().getAsFile();
        String outputName;
        if (input.getName().endsWith(".aar")) {
            outputName =
                    input.getName().substring(0, input.getName().length() - 4) + SANITIZED_SUFFIX + ".aar";
        } else {
            outputName = input.getName() + SANITIZED_SUFFIX;
        }
        File output = outputs.file(outputName);
        File tempDir;
        try {
            tempDir = Files.createTempDirectory("prompt-header-" + input.getName()).toFile();
        } catch (IOException e) {
            throw new GradleException("Unable to create temp directory for " + input.getAbsolutePath(), e);
        }

        try {
            unzipTo(input, tempDir);
            boolean patched = replaceInvalidPromptHeaderStrings(tempDir);
            if (patched) {
                zipFrom(tempDir, output);
            } else {
                copyFile(input, output);
            }
        } catch (IOException ex) {
            throw new GradleException("Failed to sanitize " + input.getAbsolutePath(), ex);
        } finally {
            try {
                deleteRecursively(tempDir.toPath());
            } catch (IOException ignored) {
                // best effort cleanup
            }
        }
    }

    private static void copyFile(File input, File output) throws IOException {
        output.getParentFile().mkdirs();
        Files.copy(input.toPath(), output.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
    }

    private static boolean replaceInvalidPromptHeaderStrings(File root) throws IOException {
        if (root == null || !root.exists()) {
            return false;
        }
        boolean patchedAny = false;
        List<File> valuesFiles = findValuesXmlFiles(root);
        for (File file : valuesFiles) {
            boolean filePatched = sanitizePromptHeaderFile(file);
            patchedAny = patchedAny || filePatched;
        }
        return patchedAny;
    }

    private static boolean sanitizePromptHeaderFile(File file) throws IOException {
        String original = Files.readString(file.toPath(), StandardCharsets.UTF_8);
        Matcher matcher = PROMPT_HEADER_PATTERN.matcher(original);
        StringBuffer buffer = new StringBuffer(original.length());
        boolean modified = false;
        while (matcher.find()) {
            String opening = matcher.group(1);
            String content = matcher.group(3);
            String closing = matcher.group(4);
            String sanitized = content;
            if (content != null) {
                String replaced = sanitizePromptHeaderContent(content);
                if (!replaced.equals(content)) {
                    sanitized = replaced;
                }
            }
            if (!sanitized.equals(content)) {
                modified = true;
            }
            matcher.appendReplacement(
                    buffer, Matcher.quoteReplacement(opening + sanitized + closing));
        }
        matcher.appendTail(buffer);
        if (!modified) {
            return false;
        }
        Files.writeString(file.toPath(), buffer.toString(), StandardCharsets.UTF_8);
        return true;
    }

    static String sanitizePromptHeaderContent(String content) {
        if (content == null || content.isEmpty()) {
            return content;
        }
        StringBuilder sanitized = new StringBuilder(content.length());
        int index = 0;
        while (index < content.length()) {
            PlaceholderMatch match = findNextPlaceholder(content, index);
            if (match == null) {
                sanitized.append(content, index, content.length());
                break;
            }
            if (match.start > index) {
                sanitized.append(content, index, match.start);
            }
            sanitized.append(PLACEHOLDER_REPLACEMENT);
            index = match.end;
        }
        return sanitized.toString();
    }

    private static PlaceholderMatch findNextPlaceholder(String content, int fromIndex) {
        for (int candidate = fromIndex; candidate < content.length(); candidate++) {
            PlaceholderMatch match = matchPlaceholderAt(content, candidate);
            if (match != null) {
                return match;
            }
        }
        return null;
    }

    private static PlaceholderMatch matchPlaceholderAt(String content, int index) {
        int leftEnd = matchBracketToken(content, index, true);
        if (leftEnd == -1) {
            return null;
        }
        int strStart = skipWhitespace(content, leftEnd);
        int strEnd = matchStrToken(content, strStart);
        if (strEnd == -1) {
            return null;
        }
        int rightStart = skipWhitespace(content, strEnd);
        int rightEnd = matchBracketToken(content, rightStart, false);
        if (rightEnd == -1) {
            return null;
        }
        return new PlaceholderMatch(index, rightEnd);
    }

    private static int matchBracketToken(String content, int index, boolean left) {
        int backslashCount = countBackslashes(content, index);
        int unicodeEnd = matchUnicodeEscape(content, index, backslashCount, left);
        if (unicodeEnd != -1) {
            return unicodeEnd;
        }
        int tokenStart = index + backslashCount;
        int htmlEnd = matchHtmlEntity(content, tokenStart, left);
        if (htmlEnd != -1) {
            return htmlEnd;
        }
        int literalEnd = matchLiteralBrace(content, tokenStart, left);
        if (literalEnd != -1) {
            return literalEnd;
        }
        return -1;
    }

    private static int countBackslashes(String content, int index) {
        int cursor = index;
        int length = content.length();
        while (cursor < length && content.charAt(cursor) == '\\') {
            cursor++;
        }
        return cursor - index;
    }

    private static int matchUnicodeEscape(
            String content, int index, int backslashCount, boolean leftBracket) {
        if (backslashCount == 0) {
            return -1;
        }
        int cursor = index + backslashCount;
        if (cursor >= content.length()) {
            return -1;
        }
        char letter = content.charAt(cursor);
        if (!isCharIgnoreCase(letter, 'u')) {
            return -1;
        }
        cursor++;
        cursor = skipWhitespace(content, cursor);
        cursor = consumeLeadingZeros(content, cursor);
        char[] digits = leftBracket ? HEX_LEFT_DIGITS : HEX_RIGHT_DIGITS;
        cursor = consumeSequence(content, cursor, digits, true);
        return cursor;
    }

    private static int matchHtmlEntity(String content, int index, boolean leftBracket) {
        if (index >= content.length() || content.charAt(index) != '&') {
            return -1;
        }
        int cursor = index + 1;
        if (cursor >= content.length() || content.charAt(cursor) != '#') {
            return -1;
        }
        cursor++;
        cursor = skipWhitespace(content, cursor);
        boolean hex = false;
        if (cursor < content.length()) {
            char mode = content.charAt(cursor);
            if (isCharIgnoreCase(mode, 'x')) {
                hex = true;
                cursor++;
                cursor = skipWhitespace(content, cursor);
            }
        }
        if (hex) {
            cursor = consumeLeadingZeros(content, cursor);
            char[] digits = leftBracket ? HEX_LEFT_DIGITS : HEX_RIGHT_DIGITS;
            cursor = consumeSequence(content, cursor, digits, true);
        } else {
            cursor = consumeLeadingZeros(content, cursor);
            char[] digits = leftBracket ? DECIMAL_LEFT_DIGITS : DECIMAL_RIGHT_DIGITS;
            cursor = consumeSequence(content, cursor, digits, false);
        }
        if (cursor == -1) {
            return -1;
        }
        cursor = skipWhitespace(content, cursor);
        if (cursor >= content.length() || content.charAt(cursor) != ';') {
            return -1;
        }
        return cursor + 1;
    }

    private static int matchLiteralBrace(String content, int index, boolean leftBracket) {
        if (index >= content.length()) {
            return -1;
        }
        char expected = leftBracket ? '{' : '}';
        if (content.charAt(index) != expected) {
            return -1;
        }
        return index + 1;
    }

    private static int matchStrToken(String content, int index) {
        if (index + 3 > content.length()) {
            return -1;
        }
        if (!content.regionMatches(true, index, "str", 0, 3)) {
            return -1;
        }
        return index + 3;
    }

    private static int skipWhitespace(String content, int index) {
        int cursor = index;
        int length = content.length();
        while (cursor < length && Character.isWhitespace(content.charAt(cursor))) {
            cursor++;
        }
        return cursor;
    }

    private static int consumeLeadingZeros(String content, int index) {
        int cursor = index;
        while (true) {
            int next = skipWhitespace(content, cursor);
            if (next < content.length() && content.charAt(next) == '0') {
                cursor = next + 1;
            } else {
                return next;
            }
        }
    }

    private static int consumeSequence(
            String content, int index, char[] sequence, boolean ignoreCase) {
        int cursor = index;
        for (char expected : sequence) {
            cursor = skipWhitespace(content, cursor);
            if (cursor >= content.length()) {
                return -1;
            }
            char actual = content.charAt(cursor);
            if (ignoreCase) {
                actual = Character.toLowerCase(actual);
                expected = Character.toLowerCase(expected);
            }
            if (actual != expected) {
                return -1;
            }
            cursor++;
        }
        return cursor;
    }

    private static boolean isCharIgnoreCase(char value, char expected) {
        return Character.toLowerCase(value) == Character.toLowerCase(expected);
    }

    private static final char[] HEX_LEFT_DIGITS = {'7', 'b'};
    private static final char[] HEX_RIGHT_DIGITS = {'7', 'd'};
    private static final char[] DECIMAL_LEFT_DIGITS = {'1', '2', '3'};
    private static final char[] DECIMAL_RIGHT_DIGITS = {'1', '2', '5'};

    private static final class PlaceholderMatch {
        private final int start;
        private final int end;

        private PlaceholderMatch(int start, int end) {
            this.start = start;
            this.end = end;
        }
    }

    private static List<File> findValuesXmlFiles(File root) {
        List<File> files = new ArrayList<>();
        Deque<File> stack = new ArrayDeque<>();
        stack.push(root);
        while (!stack.isEmpty()) {
            File current = stack.pop();
            File[] children = current.listFiles();
            if (children == null) {
                continue;
            }
            for (File child : children) {
                if (child.isDirectory()) {
                    stack.push(child);
                } else if (child.getName().endsWith(".xml") && isValuesDirectory(child.getParentFile())) {
                    files.add(child);
                }
            }
        }
        return files;
    }

    private static boolean isValuesDirectory(File directory) {
        if (directory == null) {
            return false;
        }
        String name = directory.getName();
        if (!name.startsWith("values")) {
            return false;
        }
        File parent = directory.getParentFile();
        return parent != null && "res".equals(parent.getName());
    }

    private static void unzipTo(File input, File destination) throws IOException {
        destination.mkdirs();
        try (ZipInputStream zis = new ZipInputStream(new BufferedInputStream(new FileInputStream(input)))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                File entryFile = new File(destination, entry.getName());
                if (entry.isDirectory()) {
                    entryFile.mkdirs();
                } else {
                    File parent = entryFile.getParentFile();
                    if (parent != null) {
                        parent.mkdirs();
                    }
                    try (OutputStream output = new BufferedOutputStream(new FileOutputStream(entryFile))) {
                        zis.transferTo(output);
                    }
                }
                zis.closeEntry();
            }
        }
    }

    private static void zipFrom(File source, File output) throws IOException {
        output.getParentFile().mkdirs();
        try (ZipOutputStream zos = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(output)))) {
            Path root = source.toPath();
            Files.walk(root).forEach(path -> {
                Path relative = root.relativize(path);
                if (relative.getNameCount() == 0) {
                    return;
                }
                String normalized = relative.toString().replace(File.separatorChar, '/');
                try {
                    if (Files.isDirectory(path)) {
                        if (!normalized.endsWith("/")) {
                            normalized = normalized + "/";
                        }
                        zos.putNextEntry(new ZipEntry(normalized));
                        zos.closeEntry();
                    } else {
                        zos.putNextEntry(new ZipEntry(normalized));
                        Files.copy(path, zos);
                        zos.closeEntry();
                    }
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            });
        } catch (UncheckedIOException exception) {
            throw exception.getCause();
        }
    }

    private static void deleteRecursively(Path root) throws IOException {
        if (!Files.exists(root)) {
            return;
        }
        Files.walk(root)
            .sorted(Comparator.reverseOrder())
            .forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException ignored) {
                    // best effort cleanup
                }
            });
    }
}
