package com.lumen.build;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.gradle.api.GradleException;
import org.gradle.api.artifacts.transform.InputArtifact;
import org.gradle.api.artifacts.transform.TransformAction;
import org.gradle.api.artifacts.transform.TransformOutputs;
import org.gradle.api.artifacts.transform.TransformParameters;
import org.gradle.api.file.FileSystemLocation;
import org.gradle.api.provider.Provider;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public abstract class SanitizePromptHeaderTransform implements TransformAction<TransformParameters.None> {
    private static final String SANITIZED_SUFFIX = "-sanitized-v12";
    private static final String PLACEHOLDER_REPLACEMENT = "%1$s";
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
        String sanitized = sanitizePromptHeaderResourcesXml(original);
        if (sanitized.equals(original)) {
            return false;
        }
        Files.writeString(file.toPath(), sanitized, StandardCharsets.UTF_8);
        return true;
    }

    static String sanitizePromptHeaderResourcesXml(String xml) {
        Document document = parseXmlDocument(xml);
        if (document == null) {
            return xml;
        }
        boolean modified = sanitizePromptHeaderDocument(document);
        if (!modified) {
            return xml;
        }
        try {
            return serializeDocument(document, hasXmlDeclaration(xml));
        } catch (TransformerException exception) {
            throw new GradleException("Failed to serialize sanitized resources", exception);
        }
    }

    private static Document parseXmlDocument(String xml) {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setExpandEntityReferences(false);
        try {
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        } catch (ParserConfigurationException ignored) {
            // ignore when feature not supported
        }
        try {
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        } catch (IllegalArgumentException ignored) {
            // ignore when attribute not supported
        }
        DocumentBuilder builder;
        try {
            builder = factory.newDocumentBuilder();
        } catch (ParserConfigurationException exception) {
            return null;
        }
        try {
            return builder.parse(new InputSource(new StringReader(xml)));
        } catch (SAXException | IOException exception) {
            return null;
        }
    }

    private static boolean sanitizePromptHeaderDocument(Document document) {
        boolean modified = false;
        NodeList stringNodes = document.getElementsByTagName("string");
        modified = sanitizePromptHeaderElements(stringNodes, false) || modified;
        NodeList itemNodes = document.getElementsByTagName("item");
        modified = sanitizePromptHeaderElements(itemNodes, true) || modified;
        return modified;
    }

    private static boolean sanitizePromptHeaderElements(NodeList nodes, boolean requireStringType) {
        boolean modified = false;
        for (int index = 0; index < nodes.getLength(); index++) {
            Node node = nodes.item(index);
            if (!(node instanceof Element)) {
                continue;
            }
            Element element = (Element) node;
            if (!"prompt_header".equals(element.getAttribute("name"))) {
                continue;
            }
            if (requireStringType) {
                String type = element.getAttribute("type");
                if (type == null || type.isEmpty() || !"string".equalsIgnoreCase(type)) {
                    continue;
                }
            }
            if (sanitizePromptHeaderElement(element)) {
                modified = true;
            }
        }
        return modified;
    }

    private static boolean sanitizePromptHeaderElement(Element element) {
        String textContent = element.getTextContent();
        if (textContent == null || textContent.isEmpty()) {
            return false;
        }
        String sanitized = sanitizePromptHeaderContent(textContent);
        if (sanitized.equals(textContent)) {
            return false;
        }
        clearChildren(element);
        Node textNode = element.getOwnerDocument().createTextNode(sanitized);
        element.appendChild(textNode);
        return true;
    }

    private static void clearChildren(Element element) {
        while (element.hasChildNodes()) {
            element.removeChild(element.getFirstChild());
        }
    }

    private static boolean hasXmlDeclaration(String xml) {
        int index = 0;
        int length = xml.length();
        while (index < length && Character.isWhitespace(xml.charAt(index))) {
            index++;
        }
        return index + 5 < length
                && xml.charAt(index) == '<'
                && xml.startsWith("?xml", index + 1);
    }

    private static String serializeDocument(Document document, boolean includeDeclaration)
            throws TransformerException {
        TransformerFactory factory = TransformerFactory.newInstance();
        try {
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        } catch (TransformerException ignored) {
            // ignore when feature not supported
        }
        try {
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "");
        } catch (IllegalArgumentException ignored) {
            // ignore when attribute not supported
        }
        Transformer transformer = factory.newTransformer();
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        transformer.setOutputProperty(
                OutputKeys.OMIT_XML_DECLARATION, includeDeclaration ? "no" : "yes");
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
        StringWriter writer = new StringWriter();
        transformer.transform(new DOMSource(document), new StreamResult(writer));
        return writer.toString();
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
