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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
    private static final String SANITIZED_SUFFIX = "-sanitized-v9";
    private static final Pattern STRING_WITH_NAME_PATTERN =
            Pattern.compile(
                    "(<([a-zA-Z0-9_:-]+)[^>]*name\\s*=\\s*(['\"])prompt_header\\3[^>]*>)(.*?)(</\\2>)",
                    Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
    private static final Pattern PLACEHOLDER_PATTERN =
            Pattern.compile("\\\\?" + "\\{" + "[^}]*" + "\\}");

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
        Matcher matcher = STRING_WITH_NAME_PATTERN.matcher(original);
        StringBuffer buffer = new StringBuffer(original.length());
        boolean modified = false;
        while (matcher.find()) {
            String opening = matcher.group(1);
            String content = matcher.group(4);
            String closing = matcher.group(5);
            if (!isStringElement(opening, matcher.group(2))) {
                continue;
            }
            String sanitized = sanitizePlaceholders(content);
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

    private static String sanitizePlaceholders(String content) {
        if (content == null || content.isEmpty()) {
            return content;
        }
        Matcher placeholderMatcher = PLACEHOLDER_PATTERN.matcher(content);
        StringBuffer sanitizedBuffer = new StringBuffer(content.length());
        Map<String, Integer> placeholderOrder = new LinkedHashMap<>();
        boolean replaced = false;
        int nextIndex = 1;
        while (placeholderMatcher.find()) {
            String placeholder = placeholderMatcher.group();
            String placeholderKey = canonicalizePlaceholderKey(placeholder);
            Integer assignedIndex = placeholderOrder.get(placeholderKey);
            if (assignedIndex == null) {
                assignedIndex = nextIndex++;
                placeholderOrder.put(placeholderKey, assignedIndex);
            }
            String replacement = "%" + assignedIndex + "$s";
            placeholderMatcher.appendReplacement(
                    sanitizedBuffer, Matcher.quoteReplacement(replacement));
            replaced = true;
        }
        if (!replaced) {
            return content;
        }
        placeholderMatcher.appendTail(sanitizedBuffer);
        return sanitizedBuffer.toString();
    }

    private static boolean isStringElement(String openingTag, String tagName) {
        if (tagName == null) {
            return false;
        }
        if ("string".equalsIgnoreCase(tagName)) {
            return true;
        }
        if (!"item".equalsIgnoreCase(tagName)) {
            return false;
        }
        String lowerOpening = openingTag.toLowerCase();
        return lowerOpening.contains("type=\"string\"")
                || lowerOpening.contains("type='string'");
    }

    private static String canonicalizePlaceholderKey(String placeholder) {
        if (placeholder == null || placeholder.isEmpty()) {
            return "";
        }
        int start = 0;
        int end = placeholder.length();
        if (placeholder.charAt(start) == '\\') {
            start++;
        }
        if (start < end && placeholder.charAt(start) == '{') {
            start++;
        }
        if (end > start && placeholder.charAt(end - 1) == '}') {
            end--;
        }
        if (start >= end) {
            return "";
        }
        StringBuilder normalized = new StringBuilder(end - start);
        boolean seenContent = false;
        boolean pendingWhitespace = false;
        for (int index = start; index < end; index++) {
            char current = placeholder.charAt(index);
            if (current == '\\') {
                continue;
            }
            if (Character.isWhitespace(current)) {
                if (seenContent) {
                    pendingWhitespace = true;
                }
            } else {
                if (pendingWhitespace) {
                    normalized.append(' ');
                    pendingWhitespace = false;
                }
                normalized.append(current);
                seenContent = true;
            }
        }
        return normalized.toString();
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
