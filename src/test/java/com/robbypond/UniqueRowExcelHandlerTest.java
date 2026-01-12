package com.robbypond;

import org.apache.tika.sax.BodyContentHandler;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UniqueRowExcelHandlerTest {

    @Test
    void testDuplicateRowsAreFilteredWithRealFile() throws Exception {
        // We use a CSV file because Tika parses CSVs into XHTML tables similar to Excel,
        // and it's easier to create a text-based CSV than a binary Excel file.
        // The UniqueRowExcelHandler works on the XHTML structure (tr/td), so it should work for CSVs too.
        
        Path path = Paths.get("src/test/resources/test.xlsx");
        
        TextExtractor extractor = new TextExtractor();
        
        // Register the handler for text/csv
        extractor.registerCustomHandler("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", (limit) -> new UniqueRowExcelHandler(new BodyContentHandler(limit)));
        
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