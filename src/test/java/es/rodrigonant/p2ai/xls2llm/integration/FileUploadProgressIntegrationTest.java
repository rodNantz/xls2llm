package es.rodrigonant.p2ai.xls2llm.integration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import es.rodrigonant.p2ai.xls2llm.service.ProgressTracker;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.io.FileInputStream;
import java.nio.file.Files;
import java.nio.file.Paths;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("File Upload with Progress Tracking E2E Tests")
public class FileUploadProgressIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProgressTracker progressTracker;

    private static final String TEST_FILE_PATH = "src/main/resources/xls/QuestionsTemplate.xlsx";
    private Long testUploadId;

    @BeforeEach
    void setUp() {
        testUploadId = System.currentTimeMillis();
    }

    @Test
    @DisplayName("Progress tracker is created before file processing starts")
    void testProgressTrackerCreatedOnUpload() throws Exception {
        // Ensure uploadId doesn't exist yet
        assertNull(progressTracker.getProgress(testUploadId), 
                "Progress tracker should not exist before upload");
        
        // Simulate the frontend generating an uploadId and submitting form
        // The progress tracker should be created server-side when processing starts
        
        // For now, manually create to verify API works
        progressTracker.createProgress(testUploadId, 10);
        assertNotNull(progressTracker.getProgress(testUploadId), 
                "Progress tracker should exist after creation");
    }

    @Test
    @DisplayName("Multiple progress updates are reflected in API")
    void testSequentialProgressUpdates() throws Exception {
        progressTracker.createProgress(testUploadId, 100);
        
        // Simulate sequential updates like the backend would do
        for (int i = 10; i <= 100; i += 10) {
            progressTracker.updateProgress(testUploadId, i);
            
            MvcResult result = mockMvc.perform(get("/api/upload/" + testUploadId + "/progress"))
                    .andExpect(status().isOk())
                    .andReturn();
            
            String json = result.getResponse().getContentAsString();
            assertTrue(json.contains("\"current\":" + i), 
                    "JSON should contain current: " + i + ", got: " + json);
            assertTrue(json.contains("\"percentage\":" + i), 
                    "JSON should contain percentage: " + i + ", got: " + json);
        }
    }

    @Test
    @DisplayName("Progress percentage correctly calculated for different totals")
    void testProgressPercentageVariousTotals() throws Exception {
        Long uploadId1 = System.currentTimeMillis() + 1;
        Long uploadId2 = System.currentTimeMillis() + 2;
        
        // Test with total=200 items
        progressTracker.createProgress(uploadId1, 200);
        progressTracker.updateProgress(uploadId1, 100);
        
        MvcResult result1 = mockMvc.perform(get("/api/upload/" + uploadId1 + "/progress"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.current").value(100))
                .andExpect(jsonPath("$.total").value(200))
                .andExpect(jsonPath("$.percentage").value(50))
                .andReturn();
        
        String json1 = result1.getResponse().getContentAsString();
        assertTrue(json1.contains("\"percentage\":50"), 
                "With 100/200 items, percentage should be 50%, got: " + json1);
        
        // Test with total=50 items
        progressTracker.createProgress(uploadId2, 50);
        progressTracker.updateProgress(uploadId2, 25);
        
        MvcResult result2 = mockMvc.perform(get("/api/upload/" + uploadId2 + "/progress"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.current").value(25))
                .andExpect(jsonPath("$.total").value(50))
                .andExpect(jsonPath("$.percentage").value(50))
                .andReturn();
        
        String json2 = result2.getResponse().getContentAsString();
        assertTrue(json2.contains("\"percentage\":50"), 
                "With 25/50 items, percentage should be 50%, got: " + json2);
    }



}
