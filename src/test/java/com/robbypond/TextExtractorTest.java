package com.robbypond;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import org.apache.tika.sax.BodyContentHandler;
import org.junit.jupiter.api.Test;
import org.xml.sax.SAXException;

class TextExtractorTest {

  @Test
  void testExtractTextFromInputStream() throws Exception {
    String content = "<html><body><p>Hello, World!</p></body></html>";
    ByteArrayInputStream inputStream = new ByteArrayInputStream(
        content.getBytes(StandardCharsets.UTF_8));

    TextExtractor extractor = new TextExtractor();
    String result = extractor.extractText(inputStream);

    assertTrue(result.contains("Hello, World!"));
  }

  @Test
  void testExtractTextFromBytes() throws Exception {
    String content = "Hello, Byte Array!";
    byte[] bytes = content.getBytes(StandardCharsets.UTF_8);

    TextExtractor extractor = new TextExtractor();
    String result = extractor.extractText(bytes);

    assertTrue(result.contains("Hello, Byte Array!"));
  }

  @Test
  void testExtractTextWithLimit() throws Exception {
    String content = "1234567890";
    ByteArrayInputStream inputStream = new ByteArrayInputStream(
        content.getBytes(StandardCharsets.UTF_8));

    TextExtractor extractor = new TextExtractor(5);
    String result = extractor.extractText(inputStream);

    assertEquals(5, result.length());
    assertEquals("12345", result);
  }

  @Test
  void testCustomHandler() throws Exception {
    String content = "<html><body><p>Custom Handler Test</p></body></html>";
    ByteArrayInputStream inputStream = new ByteArrayInputStream(
        content.getBytes(StandardCharsets.UTF_8));

    TextExtractor extractor = new TextExtractor();

    // Register a custom handler for text/html that converts everything to uppercase
    extractor.registerCustomHandler("text/html", (limit) -> new BodyContentHandler(limit) {
      @Override
      public void characters(char[] ch, int start, int length) throws SAXException {
        for (int i = start; i < start + length; i++) {
          ch[i] = Character.toUpperCase(ch[i]);
        }
        super.characters(ch, start, length);
      }
    });

    String result = extractor.extractText(inputStream);

    assertTrue(result.contains("CUSTOM HANDLER TEST"));
  }
}