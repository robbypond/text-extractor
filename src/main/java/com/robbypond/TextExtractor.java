package com.robbypond;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import org.apache.tika.config.TikaConfig;
import org.apache.tika.detect.Detector;
import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.mime.MediaType;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
import org.apache.tika.sax.BodyContentHandler;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

/**
 * Extracts text from various input sources using Apache Tika.
 */
public class TextExtractor {

  private final Parser parser;
  private final Detector detector;
  private final int writeLimit;
  private final Map<String, Function<Integer, ContentHandler>> customHandlers = new HashMap<>();

  /**
   * Creates a new TextExtractor with no write limit.
   *
   * @throws TikaException if Tika configuration fails
   * @throws IOException   if Tika configuration file cannot be read
   */
  public TextExtractor() throws TikaException, IOException, SAXException {
    this(-1);
  }

  /**
   * Creates a new TextExtractor with the specified write limit.
   *
   * @param writeLimit the maximum number of characters to extract, or -1 for no limit
   * @throws TikaException if Tika configuration fails
   * @throws IOException   if Tika configuration file cannot be read
   */
  public TextExtractor(int writeLimit) throws TikaException, IOException, SAXException {
    TikaConfig config = new TikaConfig(TextExtractor.class.getResourceAsStream("/tika-config.xml"));
    this.parser = new AutoDetectParser(config);
    this.detector = config.getDetector();
    this.writeLimit = writeLimit;
  }

  /**
   * Registers a custom content handler factory for a specific MIME type.
   *
   * @param mimeType       the MIME type to handle
   * @param handlerFactory a function that creates a ContentHandler given a write limit
   */
  public void registerCustomHandler(String mimeType,
      Function<Integer, ContentHandler> handlerFactory) {
    customHandlers.put(mimeType, handlerFactory);
  }

  /**
   * Extracts text from an InputStream.
   *
   * @param inputStream the input stream to read from
   * @return the extracted text
   * @throws IOException   if an I/O error occurs
   * @throws TikaException if a Tika error occurs
   * @throws SAXException  if a SAX error occurs
   */
  public String extractText(InputStream inputStream)
      throws IOException, TikaException, SAXException {
    InputStream streamToUse = inputStream;
    if (!streamToUse.markSupported()) {
      streamToUse = new BufferedInputStream(streamToUse);
    }

    Metadata metadata = new Metadata();
    ParseContext context = new ParseContext();

    // Detect mime type
    MediaType mediaType = detector.detect(streamToUse, metadata);
    String mimeType = mediaType.toString();

    ContentHandler handler;
    if (customHandlers.containsKey(mimeType)) {
      handler = customHandlers.get(mimeType).apply(writeLimit);
    } else {
      handler = new BodyContentHandler(writeLimit);
    }

    try {
      parser.parse(streamToUse, handler, metadata, context);
    } catch (SAXException e) {
      // If the write limit is reached, Tika throws a SAXException.
      // We check if it's due to the limit and if so, ignore it to return partial content.
      if (writeLimit != -1 && handler.toString().length() >= writeLimit) {
        // Limit reached, return what we have so far
        return handler.toString();
      }
      throw e;
    }

    return handler.toString();
  }

  /**
   * Extracts text from a byte array.
   *
   * @param bytes the byte array to read from
   * @return the extracted text
   * @throws IOException   if an I/O error occurs
   * @throws TikaException if a Tika error occurs
   * @throws SAXException  if a SAX error occurs
   */
  public String extractText(byte[] bytes) throws IOException, TikaException, SAXException {
    try (InputStream inputStream = new ByteArrayInputStream(bytes)) {
      return extractText(inputStream);
    }
  }

  /**
   * Extracts text from a file path.
   *
   * @param filePath the path to the file
   * @return the extracted text
   * @throws IOException   if an I/O error occurs
   * @throws TikaException if a Tika error occurs
   * @throws SAXException  if a SAX error occurs
   */
  public String extractText(String filePath) throws IOException, TikaException, SAXException {
    Path path = Paths.get(filePath);
    try (InputStream inputStream = Files.newInputStream(path)) {
      return extractText(inputStream);
    }
  }
}