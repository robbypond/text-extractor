package com.robbypond;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.tika.sax.ContentHandlerDecorator;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

/**
 * A ContentHandler that filters out duplicate rows in Excel files (or any XHTML table structure).
 */
public class UniqueRowExcelHandler extends ContentHandlerDecorator {

  private final Set<String> seenRows = new HashSet<>();
  private final StringBuilder currentRowText = new StringBuilder();
  private boolean inRow = false;

  // Buffer for SAX events within a row
  private final List<SaxEvent> rowEvents = new ArrayList<>();

  public UniqueRowExcelHandler(ContentHandler handler) {
    super(handler);
  }

  @Override
  public void startElement(String uri, String localName, String qName, Attributes atts)
      throws SAXException {
    if ("tr".equals(localName)) {
      inRow = true;
      currentRowText.setLength(0);
      rowEvents.clear();
    }

    if (inRow) {
      rowEvents.add(new StartElementEvent(uri, localName, qName, new AttributesImpl(atts)));
    } else {
      super.startElement(uri, localName, qName, atts);
    }
  }

  @Override
  public void endElement(String uri, String localName, String qName) throws SAXException {
    if (inRow) {
      rowEvents.add(new EndElementEvent(uri, localName, qName));

      if ("tr".equals(localName)) {
        inRow = false;
        String rowText = currentRowText.toString().trim();

        // Only process non-empty rows
        if (!rowText.isEmpty()) {
          if (!seenRows.contains(rowText)) {
            seenRows.add(rowText);
            // Replay events for this unique row
            for (SaxEvent event : rowEvents) {
              event.replay(UniqueRowExcelHandler.this);
            }
          }
        }
        rowEvents.clear();
      }
    } else {
      super.endElement(uri, localName, qName);
    }
  }

  private void superStartElement(String uri, String localName, String qName, Attributes atts)
      throws SAXException {
    super.startElement(uri, localName, qName, atts);
  }

  private void superEndElement(String uri, String localName, String qName) throws SAXException {
    super.endElement(uri, localName, qName);
  }

  private void superCharacters(char[] ch, int start, int length) throws SAXException {
    super.characters(ch, start, length);
  }

  private void superIgnorableWhitespace(char[] ch, int start, int length) throws SAXException {
    super.ignorableWhitespace(ch, start, length);
  }

  @Override
  public void characters(char[] ch, int start, int length) throws SAXException {
    if (inRow) {
      currentRowText.append(ch, start, length);
      // Copy the char array because it might be reused by the parser
      char[] copy = new char[length];
      System.arraycopy(ch, start, copy, 0, length);
      rowEvents.add(new CharactersEvent(copy));
    } else {
      super.characters(ch, start, length);
    }
  }

  @Override
  public void ignorableWhitespace(char[] ch, int start, int length) throws SAXException {
    if (inRow) {
      char[] copy = new char[length];
      System.arraycopy(ch, start, copy, 0, length);
      rowEvents.add(new IgnorableWhitespaceEvent(copy));
    } else {
      super.ignorableWhitespace(ch, start, length);
    }
  }

  // Helper classes to store SAX events
  private interface SaxEvent {

    void replay(UniqueRowExcelHandler handler) throws SAXException;
  }

  private static class StartElementEvent implements SaxEvent {

    final String uri;
    final String localName;
    final String qName;
    final Attributes atts;

    StartElementEvent(String uri, String localName, String qName, Attributes atts) {
      this.uri = uri;
      this.localName = localName;
      this.qName = qName;
      this.atts = atts;
    }

    @Override
    public void replay(UniqueRowExcelHandler handler) throws SAXException {
      handler.superStartElement(uri, localName, qName, atts);
    }
  }

  private static class EndElementEvent implements SaxEvent {

    final String uri;
    final String localName;
    final String qName;

    EndElementEvent(String uri, String localName, String qName) {
      this.uri = uri;
      this.localName = localName;
      this.qName = qName;
    }

    @Override
    public void replay(UniqueRowExcelHandler handler) throws SAXException {
      handler.superEndElement(uri, localName, qName);
    }
  }

  private static class CharactersEvent implements SaxEvent {

    final char[] ch;

    CharactersEvent(char[] ch) {
      this.ch = ch;
    }

    @Override
    public void replay(UniqueRowExcelHandler handler) throws SAXException {
      handler.superCharacters(ch, 0, ch.length);
    }
  }

  private static class IgnorableWhitespaceEvent implements SaxEvent {

    final char[] ch;

    IgnorableWhitespaceEvent(char[] ch) {
      this.ch = ch;
    }

    @Override
    public void replay(UniqueRowExcelHandler handler) throws SAXException {
      handler.superIgnorableWhitespace(ch, 0, ch.length);
    }
  }
}