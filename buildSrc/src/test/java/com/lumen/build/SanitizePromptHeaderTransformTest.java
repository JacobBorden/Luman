package com.lumen.build;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

import org.junit.Test;

public class SanitizePromptHeaderTransformTest {

    @Test
    public void sanitizesPromptHeaderStringElement() {
        String source =
                "<resources>\n"
                        + "    <string name=\"prompt_header\">\"{str}\"</string>\n"
                        + "</resources>";
        String expected =
                "<resources>\n"
                        + "    <string name=\"prompt_header\">\"%1$s\"</string>\n"
                        + "</resources>";

        assertEquals(expected, sanitizeXml(source));
    }

    @Test
    public void sanitizesPromptHeaderStringItemElement() {
        String source =
                "<resources>\n"
                        + "    <item name=\"prompt_header\" type=\"string\">\"{str}\"</item>\n"
                        + "</resources>";
        String expected =
                "<resources>\n"
                        + "    <item name=\"prompt_header\" type=\"string\">\"%1$s\"</item>\n"
                        + "</resources>";

        assertEquals(expected, sanitizeXml(source));
    }

    @Test
    public void ignoresNonStringItemElement() {
        String source =
                "<resources>\n"
                        + "    <item name=\"prompt_header\" type=\"id\">{str}</item>\n"
                        + "</resources>";

        assertSame(source, sanitizeXml(source));
    }

    @Test
    public void sanitizesMultiplePlaceholderEncodings() {
        String source = "{str} and \\u007bstr\\u007d and &#x7b;STR&#x7D;";
        String expected = "%1$s and %1$s and %1$s";

        assertEquals(expected, sanitizeContent(source));
    }

    private static String sanitizeXml(String source) {
        return SanitizePromptHeaderTransform.sanitizePromptHeaderResourcesXml(source);
    }

    private static String sanitizeContent(String source) {
        return SanitizePromptHeaderTransform.sanitizePromptHeaderContent(source);
    }
}
