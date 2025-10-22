package com.lumen.build;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

import java.io.IOException;
import java.io.StringReader;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class SanitizePromptHeaderTransformTest {

    @Test
    public void sanitizesPromptHeaderStringElement() {
        String source =
                "<resources>\n"
                        + "    <string name=\"prompt_header\">\"{str}\"</string>\n"
                        + "</resources>";
        Element sanitized = getSingleElement(sanitizeXml(source), "string");

        assertEquals("prompt_header", sanitized.getAttribute("name"));
        assertEquals("\"%1$s\"", sanitized.getTextContent());
    }

    @Test
    public void sanitizesPromptHeaderStringItemElement() {
        String source =
                "<resources>\n"
                        + "    <item name=\"prompt_header\" type=\"string\">\"{str}\"</item>\n"
                        + "</resources>";
        Element sanitized = getSingleElement(sanitizeXml(source), "item");

        assertEquals("prompt_header", sanitized.getAttribute("name"));
        assertEquals("string", sanitized.getAttribute("type"));
        assertEquals("\"%1$s\"", sanitized.getTextContent());
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

    @Test
    public void sanitizesPromptHeaderWhenPlaceholderSpansXliffSegments() {
        String source =
                "<resources xmlns:xliff=\"urn:oasis:names:tc:xliff:document:1.2\">\n"
                        + "    <string name=\"prompt_header\">\"<xliff:g id=\"open\">{</xliff:g>"
                        + "<xliff:g id=\"value\">str</xliff:g><xliff:g id=\"close\">}</xliff:g>\""
                        + "</string>\n"
                        + "</resources>";
        Element sanitized = getSingleElement(sanitizeXml(source), "string");

        assertEquals("prompt_header", sanitized.getAttribute("name"));
        assertEquals("\"%1$s\"", sanitized.getTextContent());
    }

    private static String sanitizeXml(String source) {
        return SanitizePromptHeaderTransform.sanitizePromptHeaderResourcesXml(source);
    }

    private static String sanitizeContent(String source) {
        return SanitizePromptHeaderTransform.sanitizePromptHeaderContent(source);
    }

    private static Element getSingleElement(String xml, String tagName) {
        Document document = parseXml(xml);
        NodeList nodes = document.getElementsByTagName(tagName);
        assertEquals(1, nodes.getLength());
        return (Element) nodes.item(0);
    }

    private static Document parseXml(String xml) {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        DocumentBuilder builder;
        try {
            builder = factory.newDocumentBuilder();
        } catch (ParserConfigurationException exception) {
            throw new RuntimeException(exception);
        }
        try {
            return builder.parse(new InputSource(new StringReader(xml)));
        } catch (SAXException | IOException exception) {
            throw new RuntimeException(exception);
        }
    }
}
