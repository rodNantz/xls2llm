package es.rodrigonant.p2ai.xls2llm.document;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import java.io.InputStream;
import java.nio.file.Path;

import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import es.rodrigonant.p2ai.xls2llm.document.impl.DocumentMgrXlsImpl;
import es.rodrigonant.p2ai.xls2llm.model.Input2xls;

class MissingResultWorkbookTest {
    @TempDir
    Path tempDir;

    @Test
    void marksMissingClassificationCellsAndAddsExplicitStatus() throws Exception {
        Input2xls output = new Input2xls();
        output.markMissingResult(0, 6);
        Path target = tempDir.resolve("missing-result.xlsx");

        new DocumentMgrXlsImpl().writeDocument("src/main/resources/xls/test1-simple.xlsx", target.toString(), output);

        try (InputStream stream = java.nio.file.Files.newInputStream(target);
             XSSFWorkbook workbook = new XSSFWorkbook(stream)) {
            var sheet = workbook.getSheetAt(0);
            assertEquals("LLM result status", sheet.getRow(0).getCell(9).getStringCellValue());
            assertEquals("MISSING_LLM_RESULT", sheet.getRow(3).getCell(9).getStringCellValue());
            for (int column = 3; column < 9; column++) {
                assertEquals("", sheet.getRow(3).getCell(column).getStringCellValue());
                var font = (XSSFFont) workbook.getFontAt(
                        sheet.getRow(3).getCell(column).getCellStyle().getFontIndexAsInt());
                assertArrayEquals(new byte[] { (byte) 0xFF, 0, 0 }, font.getXSSFColor().getRGB());
            }
        }
    }

    @Test
    void writesValidationStatusToTheValidityColumn() throws Exception {
        Input2xls output = new Input2xls();
        output.markValidationStatus(0, 6, true);
        output.markValidationStatus(1, 6, false);
        Path target = tempDir.resolve("validation-status.xlsx");

        new DocumentMgrXlsImpl().writeDocument("src/main/resources/xls/test1-simple.xlsx", target.toString(), output);

        try (InputStream stream = java.nio.file.Files.newInputStream(target);
             XSSFWorkbook workbook = new XSSFWorkbook(stream)) {
            var sheet = workbook.getSheetAt(0);
            assertEquals("S", sheet.getRow(3).getCell(9).getStringCellValue());
            assertEquals("N", sheet.getRow(4).getCell(9).getStringCellValue());
        }
    }
}
