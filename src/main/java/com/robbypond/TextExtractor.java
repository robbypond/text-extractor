package com.robbypond;

import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
import org.apache.tika.sax.BodyContentHandler;
import org.xml.sax.SAXException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class TextExtractor {

    private final Parser parser;
    private final int writeLimit;

    public TextExtractor() {
        this(-1);
    }

    public TextExtractor(int writeLimit) {
        this.parser = new AutoDetectParser();
        this.writeLimit = writeLimit;
    }

    public String extractText(InputStream inputStream) throws IOException, TikaException, SAXException {
        BodyContentHandler handler = new BodyContentHandler(writeLimit);
        Metadata metadata = new Metadata();
        ParseContext context = new ParseContext();

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