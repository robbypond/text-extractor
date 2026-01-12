package com.robbypond;

import org.apache.tika.detect.DefaultDetector;
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

public class TextExtractor {

    private final Parser parser;
    private final int writeLimit;
    private final Map<String, Function<Integer, ContentHandler>> customHandlers = new HashMap<>();

    public TextExtractor() {
        this(-1);
    }

    public TextExtractor(int writeLimit) {
        this.parser = new AutoDetectParser();
        this.writeLimit = writeLimit;
    }

    public void registerCustomHandler(String mimeType, Function<Integer, ContentHandler> handlerFactory) {
        customHandlers.put(mimeType, handlerFactory);
    }

    public String extractText(InputStream inputStream) throws IOException, TikaException, SAXException {
        if (!inputStream.markSupported()) {
            inputStream = new BufferedInputStream(inputStream);
        }

        Metadata metadata = new Metadata();
        ParseContext context = new ParseContext();
        
        // Detect mime type
        Detector detector = new DefaultDetector();
        MediaType mediaType = detector.detect(inputStream, metadata);
        String mimeType = mediaType.toString();

        ContentHandler handler;
        if (customHandlers.containsKey(mimeType)) {
            handler = customHandlers.get(mimeType).apply(writeLimit);
        } else {
            handler = new BodyContentHandler(writeLimit);
        }

        try {
            parser.parse(inputStream, handler, metadata, context);
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

    public String extractText(byte[] bytes) throws IOException, TikaException, SAXException {
        try (InputStream inputStream = new ByteArrayInputStream(bytes)) {
            return extractText(inputStream);
        }
    }

    public String extractText(String filePath) throws IOException, TikaException, SAXException {
        Path path = Paths.get(filePath);
        try (InputStream inputStream = Files.newInputStream(path)) {
            return extractText(inputStream);
        }
    }
}