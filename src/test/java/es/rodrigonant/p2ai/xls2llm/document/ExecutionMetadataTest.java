package es.rodrigonant.p2ai.xls2llm.document;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.InputStream;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import es.rodrigonant.p2ai.xls2llm.document.impl.DocumentMgrXlsImpl;

class ExecutionMetadataTest {
    @TempDir
    Path tempDir;

    @Test
    void writesProfileAndExecutionIdentityIntoOutputWorkbook() throws Exception {
        String source = "src/main/resources/xls/test1-simple.xlsx";
        Path output = tempDir.resolve("classified.xlsx");
        Map<String, String> metadata = new LinkedHashMap<>();
        metadata.put("Profile", "nordeste-eleicoes-2022@1.0.0");
        metadata.put("Execution ID", "2468");
        metadata.put("Validation success rate", "80.00%");

        new DocumentMgrXlsImpl().writeExecutionMetadata(source, output.toString(), metadata);

        try (InputStream stream = java.nio.file.Files.newInputStream(output);
             XSSFWorkbook workbook = new XSSFWorkbook(stream)) {
            var sheet = workbook.getSheet("XLS2LLM");
            assertNotNull(sheet);
            assertEquals("Profile", sheet.getRow(1).getCell(0).getStringCellValue());
            assertEquals("nordeste-eleicoes-2022@1.0.0", sheet.getRow(1).getCell(1).getStringCellValue());
            assertEquals("Execution ID", sheet.getRow(2).getCell(0).getStringCellValue());
            assertEquals("2468", sheet.getRow(2).getCell(1).getStringCellValue());
            assertEquals("Validation success rate", sheet.getRow(3).getCell(0).getStringCellValue());
            assertEquals("80.00%", sheet.getRow(3).getCell(1).getStringCellValue());
        }
    }
}
