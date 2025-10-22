package com.lumen.build;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class SanitizePromptHeaderTransformTest {

    @Test
    public void replacesLiteralPlaceholder() {
        assertEquals("\"%1$s\"", sanitize("\"{str}\""));
    }

    @Test
    public void replacesEscapedPlaceholder() {
        assertEquals("%1$s", sanitize("\\{str\\}"));
    }

    @Test
    public void replacesUnicodeEscapedPlaceholder() {
        assertEquals("%1$s", sanitize("\\u007bstr\\u007d"));
    }

    @Test
    public void replacesHtmlEntityPlaceholder() {
        assertEquals("%1$s", sanitize("&#123;str&#125;"));
    }

    @Test
    public void replacesDecimalEntityWithLeadingZeros() {
        assertEquals("%1$s", sanitize("&#00123;str&#000125;"));
    }

    @Test
    public void replacesMixedEncodingPlaceholder() {
        assertEquals("%1$s", sanitize("&#123;str\\u007d"));
        assertEquals("%1$s", sanitize("\\u007bstr&#125;"));
        assertEquals("%1$s", sanitize("&#x7B;str\\u007d"));
    }

    @Test
    public void replacesPlaceholderWithWhitespace() {
        String source = "{\n  STR\t}\"";
        assertEquals("%1$s\"", sanitize(source));
    }

    @Test
    public void replacesEmbeddedPlaceholder() {
        String source = "Prefix {str} suffix";
        assertEquals("Prefix %1$s suffix", sanitize(source));
    }

    @Test
    public void replacesMultiplePlaceholders() {
        String source = "{str} and &#x00007B; str &#x7D;";
        assertEquals("%1$s and %1$s", sanitize(source));
    }

    @Test
    public void leavesNonPlaceholderContent() {
        String source = "{string}";
        assertEquals(source, sanitize(source));
    }

    private static String sanitize(String content) {
        return SanitizePromptHeaderTransform.sanitizePromptHeaderContent(content);
    }
}
