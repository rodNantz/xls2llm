package es.rodrigonant.p2ai.xls2llm.integration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import es.rodrigonant.p2ai.xls2llm.model.ProgressBar;
import es.rodrigonant.p2ai.xls2llm.service.ProgressTracker;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Progress Bar E2E Flow Tests")
public class ProgressFlowTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProgressTracker progressTracker;

    @Autowired
    private ObjectMapper objectMapper;

    private final Long testUploadId = 123L;

    @BeforeEach
    void setUp() {
        // Clear any stale progress data
        progressTracker.removeProgress(testUploadId);
    }

    @Test
    @DisplayName("Simulates complete upload progress flow: 0% -> 50% -> 100%")
    void testCompleteProgressFlow() throws Exception {
        int totalItems = 100;
        
        // Step 1: Create progress tracker with 100 items
        progressTracker.createProgress(testUploadId, totalItems);
        
        // Step 2: Check initial progress (0%)
        MvcResult result0 = mockMvc.perform(get("/api/upload/" + testUploadId + "/progress"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.current").value(0))
                .andExpect(jsonPath("$.total").value(100))
                .andExpect(jsonPath("$.percentage").value(0))
                .andReturn();
        
        String json0 = result0.getResponse().getContentAsString();
        assertNotNull(json0, "JSON response should not be null");
        assertTrue(json0.contains("\"current\":0"), "JSON should contain current: 0, got: " + json0);
        assertTrue(json0.contains("\"total\":100"), "JSON should contain total: 100, got: " + json0);
        assertTrue(json0.contains("\"percentage\":0"), "JSON should contain percentage: 0, got: " + json0);
        
        // Step 3: Simulate processing 50 items
        progressTracker.updateProgress(testUploadId, 50);
        
        MvcResult result50 = mockMvc.perform(get("/api/upload/" + testUploadId + "/progress"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.current").value(50))
                .andExpect(jsonPath("$.total").value(100))
                .andExpect(jsonPath("$.percentage").value(50))
                .andReturn();
        
        String json50 = result50.getResponse().getContentAsString();
        assertNotNull(json50, "JSON response should not be null");
        assertTrue(json50.contains("\"current\":50"), "JSON should contain current: 50, got: " + json50);
        assertTrue(json50.contains("\"percentage\":50"), "JSON should contain percentage: 50, got: " + json50);
        
        // Step 4: Simulate completion (100 items processed)
        progressTracker.updateProgress(testUploadId, 100);
        
        MvcResult result100 = mockMvc.perform(get("/api/upload/" + testUploadId + "/progress"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.current").value(100))
                .andExpect(jsonPath("$.total").value(100))
                .andExpect(jsonPath("$.percentage").value(100))
                .andReturn();
        
        String json100 = result100.getResponse().getContentAsString();
        assertNotNull(json100, "JSON response should not be null");
        assertTrue(json100.contains("\"current\":100"), "JSON should contain current: 100, got: " + json100);
        assertTrue(json100.contains("\"percentage\":100"), "JSON should contain percentage: 100, got: " + json100);
    }

    @Test
    @DisplayName("ProgressBar percentage calculation with various totals")
    void testProgressBarCalculations() {
        // Test cases: (total, state, expectedPercent)
        Object[][] testCases = {
            {100, 0, 0},
            {100, 25, 25},
            {100, 50, 50},
            {100, 75, 75},
            {100, 100, 100},
            {200, 50, 25},
            {200, 100, 50},
            {200, 150, 75},
            {200, 200, 100},
            {3, 1, 33},   // Integer division check
            {10, 1, 10},
            {10, 3, 30},
        };

        for (Object[] tc : testCases) {
            int total = (int) tc[0];
            int state = (int) tc[1];
            int expected = (int) tc[2];
            
            ProgressBar pb = new ProgressBar(total);
            pb.setState(state);
            
            int actual = pb.getPercent();
            assertEquals(expected, actual, 
                    "For total=" + total + ", state=" + state + 
                    ": expected=" + expected + ", got=" + actual);
        }
    }

    @Test
    @DisplayName("Progress tracker maintains separate progress for multiple uploads")
    void testMultipleUploadsIndependent() throws Exception {
        Long upload1 = 1L;
        Long upload2 = 2L;
        
        progressTracker.createProgress(upload1, 100);
        progressTracker.createProgress(upload2, 200);
        
        // Update upload1 to 50%
        progressTracker.updateProgress(upload1, 50);
        
        // Update upload2 to 100 (50%)
        progressTracker.updateProgress(upload2, 100);
        
        // Verify upload1 is 50%
        mockMvc.perform(get("/api/upload/" + upload1 + "/progress"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.percentage").value(50));
        
        // Verify upload2 is 50%
        mockMvc.perform(get("/api/upload/" + upload2 + "/progress"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.percentage").value(50));
        
        // Further update upload1 to 100%
        progressTracker.updateProgress(upload1, 100);
        
        // Verify upload1 is now 100% while upload2 remains 50%
        mockMvc.perform(get("/api/upload/" + upload1 + "/progress"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.percentage").value(100));
        
        mockMvc.perform(get("/api/upload/" + upload2 + "/progress"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.percentage").value(50));
    }

}
