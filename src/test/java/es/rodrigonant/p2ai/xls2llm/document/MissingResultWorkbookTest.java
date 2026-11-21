package es.rodrigonant.p2ai.xls2llm.document;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.io.InputStream;
import java.nio.file.Path;

import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFColor;
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
            assertEquals("Validation status", sheet.getRow(0).getCell(9).getStringCellValue());
            assertEquals("Validation reason", sheet.getRow(0).getCell(10).getStringCellValue());
            assertEquals("MISSING_LLM_RESULT", sheet.getRow(3).getCell(9).getStringCellValue());
            assertEquals("O LLM não retornou resultado para este comentário.",
                    sheet.getRow(3).getCell(10).getStringCellValue());
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
        output.markValidationStatus(0, 6, "S", "");
        output.markValidationStatus(1, 6, "N", "V1: código fora do domínio");
        output.markValidationStatus(2, 6, "R", "Revisão: ambiguidade sinalizada pelo modelo");
        Path target = tempDir.resolve("validation-status.xlsx");

        new DocumentMgrXlsImpl().writeDocument("src/main/resources/xls/test1-simple.xlsx", target.toString(), output);

        try (InputStream stream = java.nio.file.Files.newInputStream(target);
             XSSFWorkbook workbook = new XSSFWorkbook(stream)) {
            var sheet = workbook.getSheetAt(0);
            assertEquals("S", sheet.getRow(3).getCell(9).getStringCellValue());
            assertEquals("N", sheet.getRow(4).getCell(9).getStringCellValue());
            assertEquals("V1: código fora do domínio", sheet.getRow(4).getCell(10).getStringCellValue());
            assertEquals("R", sheet.getRow(5).getCell(9).getStringCellValue());
            assertEquals("Revisão: ambiguidade sinalizada pelo modelo",
                    sheet.getRow(5).getCell(10).getStringCellValue());
            var reviewColor = (XSSFColor) sheet.getRow(5).getCell(9)
                    .getCellStyle().getFillForegroundColorColor();
            assertArrayEquals(new byte[] { (byte) 0xFF, (byte) 0xF2, (byte) 0xCC }, reviewColor.getRGB());
        }
    }

    @Test
    void resolvedRetakeClearsOldReasonAndRedStyleWhenRebuiltFromOriginalWorkbook() throws Exception {
        String original = "src/main/resources/xls/test1-simple.xlsx";
        Path firstPass = tempDir.resolve("first-pass.xlsx");
        Input2xls invalid = new Input2xls();
        invalid.markInvalid(0, 0);
        invalid.markValidationStatus(0, 6, "N", "V1.1: Dependency from V1=0 is not satisfied");
        DocumentMgrXlsImpl document = new DocumentMgrXlsImpl();
        document.writeDocument(original, firstPass.toString(), invalid);

        Input2xls resolved = new Input2xls();
        resolved.putContent(0, 0, "0");
        resolved.markValidationStatus(0, 6, "S", "");
        document.writeDocument(original, firstPass.toString(), resolved);

        try (InputStream stream = java.nio.file.Files.newInputStream(firstPass);
             XSSFWorkbook workbook = new XSSFWorkbook(stream)) {
            var row = workbook.getSheetAt(0).getRow(3);
            assertEquals("S", row.getCell(9).getStringCellValue());
            assertEquals("", row.getCell(10).getStringCellValue());
            var font = (XSSFFont) workbook.getFontAt(row.getCell(3).getCellStyle().getFontIndexAsInt());
            assertFalse(java.util.Arrays.equals(new byte[] { (byte) 0xFF, 0, 0 }, font.getXSSFColor().getRGB()));
        }
    }
}
