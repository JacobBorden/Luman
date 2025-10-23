package com.lumen.build;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public final class SanitizePromptHeaderTransformTest {
    private static final Method SANITIZE_FILE_METHOD;

    static {
        try {
            SANITIZE_FILE_METHOD =
                    SanitizePromptHeaderTransform.class.getDeclaredMethod(
                            "sanitizePromptHeaderFile", File.class);
            SANITIZE_FILE_METHOD.setAccessible(true);
        } catch (NoSuchMethodException exception) {
            throw new ExceptionInInitializerError(exception);
        }
    }

    @Test
    public void sanitizePromptHeaderFile_handlesStringItemElements() throws IOException {
        Path valuesFile = createValuesFile(
                "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                        + "<resources>\n"
                        + "    <item type='string' name='prompt_header'>\\\"{str}\\\"</item>\n"
                        + "</resources>\n");

        boolean modified = sanitize(valuesFile.toFile());
        assertTrue("Expected item string to be sanitized", modified);

        String sanitized = Files.readString(valuesFile, StandardCharsets.UTF_8);
        assertEquals(
                "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                        + "<resources>\n"
                        + "    <item type='string' name='prompt_header'>\\\"%1$s\\\"</item>\n"
                        + "</resources>\n",
                sanitized);
        assertNoInvalidUnicodeEscapes(sanitized);
    }

    @Rule public final TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void sanitizePromptHeaderFile_rewritesRepeatedPlaceholderWithoutInvalidEscapes()
            throws IOException {
        Path valuesFile = createValuesFile(
                "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                        + "<resources>\n"
                        + "    <string name='prompt_header'>\"{ user }\" - {org} - {user}</string>\n"
                        + "</resources>\n");

        String original = Files.readString(valuesFile, StandardCharsets.UTF_8);
        boolean modified = sanitize(valuesFile.toFile());
        assertTrue(
                "Expected prompt header resource to be sanitized. Original was: " + original,
                modified);

        String sanitized = Files.readString(valuesFile, StandardCharsets.UTF_8);
        assertEquals(
                "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                        + "<resources>\n"
                        + "    <string name='prompt_header'>\"%1$s\" - %2$s - %1$s</string>\n"
                        + "</resources>\n",
                sanitized);
        assertNoInvalidUnicodeEscapes(sanitized);
    }

    @Test
    public void sanitizePromptHeaderFile_convertsQuotedPlaceholder() throws IOException {
        Path valuesFile = createValuesFile(
                "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                        + "<resources>\n"
                        + "    <string name='prompt_header'>\"{str}\"</string>\n"
                        + "</resources>\n");

        boolean modified = sanitize(valuesFile.toFile());
        assertTrue("Expected prompt header placeholder to be sanitized", modified);

        String sanitized = Files.readString(valuesFile, StandardCharsets.UTF_8);
        assertEquals(
                "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                        + "<resources>\n"
                        + "    <string name='prompt_header'>\"%1$s\"</string>\n"
                        + "</resources>\n",
                sanitized);
        assertNoInvalidUnicodeEscapes(sanitized);
    }

    @Test
    public void sanitizePromptHeaderFile_handlesXliffPlaceholder() throws IOException {
        Path valuesFile = createValuesFile(
                "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                        + "<resources xmlns:xliff=\"urn:oasis:names:tc:xliff:document:1.2\">\n"
                        + "    <string name='prompt_header'><xliff:g id=\"value\">{str}</xliff:g></string>\n"
                        + "</resources>\n");

        boolean modified = sanitize(valuesFile.toFile());
        assertTrue("Expected xliff placeholder to be sanitized", modified);

        String sanitized = Files.readString(valuesFile, StandardCharsets.UTF_8);
        assertEquals(
                "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                        + "<resources xmlns:xliff=\"urn:oasis:names:tc:xliff:document:1.2\">\n"
                        + "    <string name='prompt_header'><xliff:g id=\"value\">%1$s</xliff:g></string>\n"
                        + "</resources>\n",
                sanitized);
        assertNoInvalidUnicodeEscapes(sanitized);
    }

    @Test
    public void sanitizePromptHeaderFile_fixesInvalidUnicodeEscapes() throws IOException {
        Path valuesFile = createValuesFile(
                "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                        + "<resources>\n"
                        + "    <string name='prompt_header'>\\\"{str}\\\" and \\user</string>\n"
                        + "</resources>\n");

        boolean modified = sanitize(valuesFile.toFile());
        assertTrue("Expected invalid unicode escape to be sanitized", modified);

        String sanitized = Files.readString(valuesFile, StandardCharsets.UTF_8);
        assertEquals(
                "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                        + "<resources>\n"
                        + "    <string name='prompt_header'>\\\"%1$s\\\" and \\u005Cuser</string>\n"
                        + "</resources>\n",
                sanitized);
        assertNoInvalidUnicodeEscapes(sanitized);
    }

    @Test
    public void sanitizePromptHeaderFile_handlesMultipleInvalidUnicodeEscapes() throws IOException {
        Path valuesFile = createValuesFile(
                "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                        + "<resources>\n"
                        + "    <string name='prompt_header'>\\\"{str}\\\" -> \\User and \\user and \\u</string>\n"
                        + "</resources>\n");

        boolean modified = sanitize(valuesFile.toFile());
        assertTrue("Expected invalid unicode escapes to be sanitized", modified);

        String sanitized = Files.readString(valuesFile, StandardCharsets.UTF_8);
        assertEquals(
                "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                        + "<resources>\n"
                        + "    <string name='prompt_header'>\\\"%1$s\\\" -> \\u005CUser and \\u005Cuser and \\u005Cu</string>\n"
                        + "</resources>\n",
                sanitized);
        assertNoInvalidUnicodeEscapes(sanitized);
    }

    @Test
    public void sanitizePromptHeaderFile_escapesUppercaseUnicodePrefixWithDigits()
            throws IOException {
        Path valuesFile = createValuesFile(
                "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                        + "<resources>\n"
                        + "    <string name='prompt_header'>Emoji: \\U0001F600 and {str}</string>\n"
                        + "</resources>\n");

        boolean modified = sanitize(valuesFile.toFile());
        assertTrue("Expected uppercase unicode prefix to be sanitized", modified);

        String sanitized = Files.readString(valuesFile, StandardCharsets.UTF_8);
        assertEquals(
                "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                        + "<resources>\n"
                        + "    <string name='prompt_header'>Emoji: \\u005CU0001F600 and %1$s</string>\n"
                        + "</resources>\n",
                sanitized);
        assertNoInvalidUnicodeEscapes(sanitized);
    }

    @Test
    public void sanitizePromptHeaderFile_leavesValidUnicodeEscapesUntouched() throws IOException {
        Path valuesFile = createValuesFile(
                "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                        + "<resources>\n"
                        + "    <string name='prompt_header'>Degree: \\u00B0 and {org}</string>\n"
                        + "</resources>\n");

        boolean modified = sanitize(valuesFile.toFile());
        assertTrue("Expected placeholder to be sanitized", modified);

        String sanitized = Files.readString(valuesFile, StandardCharsets.UTF_8);
        assertEquals(
                "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                        + "<resources>\n"
                        + "    <string name='prompt_header'>Degree: \\u00B0 and %1$s</string>\n"
                        + "</resources>\n",
                sanitized);
        assertNoInvalidUnicodeEscapes(sanitized);
    }

    private Path createValuesFile(String contents) throws IOException {
        File resDir = temporaryFolder.newFolder("res", "values");
        Path file = resDir.toPath().resolve("strings.xml");
        Files.writeString(file, contents, StandardCharsets.UTF_8);
        return file;
    }

    private static boolean sanitize(File file) {
        try {
            return (Boolean) SANITIZE_FILE_METHOD.invoke(null, file);
        } catch (IllegalAccessException | InvocationTargetException exception) {
            throw new RuntimeException("Unable to invoke sanitizePromptHeaderFile", exception);
        }
    }

    private static void assertNoInvalidUnicodeEscapes(String content) {
        Pattern invalidUnicodePattern =
                Pattern.compile("(\\\\u(?![0-9a-fA-F]{4}))|(\\\\U)");
        Matcher matcher = invalidUnicodePattern.matcher(content);
        if (matcher.find()) {
            throw new AssertionError(
                    "Found invalid unicode escape sequence at index " + matcher.start() +
                            " in sanitized content: " + content);
        }
    }
}
