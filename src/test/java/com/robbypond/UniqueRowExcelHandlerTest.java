package com.robbypond;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.apache.tika.sax.BodyContentHandler;
import org.junit.jupiter.api.Test;

class UniqueRowExcelHandlerTest {

  @Test
  void testDuplicateRowsAreFilteredWithRealFile() throws Exception {
    Path path = Paths.get("src/test/resources/test.xlsx");

    TextExtractor extractor = new TextExtractor();

    // Register the handler for text/csv
    extractor.registerCustomHandler(
        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
        (limit) -> new UniqueRowExcelHandler(new BodyContentHandler(limit)));

    String result;
    try (InputStream is = Files.newInputStream(path)) {
      result = extractor.extractText(is);
    }

    assertTrue(result.contains("Column 1\tColumn 2"));

    // Count occurrences of "Row1" (it appears in the duplicate row)
    int count = countOccurrences(result, "Column 1\tColumn 2");

    // Debug output if test fails
    if (count != 1) {
      System.out.println("Result content:\n" + result);
    }

    assertEquals(1, count, "Duplicate row 'Column 1\tColumn 2' should be filtered out");
  }

  private int countOccurrences(String str, String sub) {
    int count = 0;
    int idx = 0;
    while ((idx = str.indexOf(sub, idx)) != -1) {
      count++;
      idx += sub.length();
    }
    return count;
  }
}