package es.rodrigonant.p2ai.xls2llm.integration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import es.rodrigonant.p2ai.xls2llm.service.ProgressTracker;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Frontend JavaScript Polling Simulation")
public class FrontendPollingSimulationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProgressTracker progressTracker;

    @Autowired
    private ObjectMapper objectMapper;

    private final Long uploadId = 999999L;

    @BeforeEach
    void setUp() {
        progressTracker.removeProgress(uploadId);
    }

    @Test
    @DisplayName("Frontend polling scenario: Initial poll during processing")
    void testFrontendPollingFlow() throws Exception {
        // Simulate: Frontend submits form, starts polling immediately
        // Backend creates progress tracker
        progressTracker.createProgress(uploadId, 100);

        // Poll 1: Initial (0%)
        MvcResult poll1 = mockMvc.perform(get("/api/upload/" + uploadId + "/progress"))
                .andExpect(status().isOk())
                .andReturn();
        Map<String, Object> data1 = objectMapper.readValue(poll1.getResponse().getContentAsString(), Map.class);
        assertEquals(0, data1.get("current"), "First poll should show 0 current");
        assertEquals(0, data1.get("percentage"), "First poll should show 0 percentage");
        System.out.println("Poll 1: " + data1);

        // Simulate backend processing batch 1
        progressTracker.updateProgress(uploadId, 25);

        // Poll 2: 25%
        MvcResult poll2 = mockMvc.perform(get("/api/upload/" + uploadId + "/progress"))
                .andExpect(status().isOk())
                .andReturn();
        Map<String, Object> data2 = objectMapper.readValue(poll2.getResponse().getContentAsString(), Map.class);
        assertEquals(25, data2.get("current"), "Second poll should show 25 current");
        assertEquals(25, data2.get("percentage"), "Second poll should show 25 percentage");
        System.out.println("Poll 2: " + data2);

        // Simulate backend processing batch 2
        progressTracker.updateProgress(uploadId, 50);

        // Poll 3: 50%
        MvcResult poll3 = mockMvc.perform(get("/api/upload/" + uploadId + "/progress"))
                .andExpect(status().isOk())
                .andReturn();
        Map<String, Object> data3 = objectMapper.readValue(poll3.getResponse().getContentAsString(), Map.class);
        assertEquals(50, data3.get("current"), "Third poll should show 50 current");
        assertEquals(50, data3.get("percentage"), "Third poll should show 50 percentage");
        System.out.println("Poll 3: " + data3);

        // Simulate backend completing
        progressTracker.updateProgress(uploadId, 100);

        // Poll 4: 100% (complete)
        MvcResult poll4 = mockMvc.perform(get("/api/upload/" + uploadId + "/progress"))
                .andExpect(status().isOk())
                .andReturn();
        Map<String, Object> data4 = objectMapper.readValue(poll4.getResponse().getContentAsString(), Map.class);
        assertEquals(100, data4.get("current"), "Final poll should show 100 current");
        assertEquals(100, data4.get("percentage"), "Final poll should show 100 percentage");
        System.out.println("Poll 4: " + data4);
    }

    @Test
    @DisplayName("Verify JSON structure exactly matches frontend expectations")
    void testJSONStructureForFrontend() throws Exception {
        progressTracker.createProgress(uploadId, 50);
        progressTracker.updateProgress(uploadId, 25);

        MvcResult result = mockMvc.perform(get("/api/upload/" + uploadId + "/progress"))
                .andExpect(status().isOk())
                .andReturn();

        String json = result.getResponse().getContentAsString();
        Map<String, Object> data = objectMapper.readValue(json, Map.class);

        // Verify all required fields exist for frontend
        assertTrue(data.containsKey("current"), "JSON must have 'current' field");
        assertTrue(data.containsKey("total"), "JSON must have 'total' field");
        assertTrue(data.containsKey("percentage"), "JSON must have 'percentage' field");

        // Verify types are correct (numbers, not strings)
        assertEquals(Integer.class, data.get("current").getClass(), "current should be int");
        assertEquals(Integer.class, data.get("total").getClass(), "total should be int");
        assertEquals(Integer.class, data.get("percentage").getClass(), "percentage should be int");

        // Verify values are correct
        assertEquals(25, data.get("current"));
        assertEquals(50, data.get("total"));
        assertEquals(50, data.get("percentage"));

        System.out.println("JSON structure verified: " + json);
    }

    @Test
    @DisplayName("Test 500ms polling interval (frontend default)")
    void testPollingWith500msInterval() throws Exception {
        progressTracker.createProgress(uploadId, 40);

        // Simulate 500ms interval polling
        long startTime = System.currentTimeMillis();
        for (int i = 0; i < 4; i++) {
            progressTracker.updateProgress(uploadId, (i + 1) * 10);

            MvcResult result = mockMvc.perform(get("/api/upload/" + uploadId + "/progress"))
                    .andExpect(status().isOk())
                    .andReturn();

            Map<String, Object> data = objectMapper.readValue(result.getResponse().getContentAsString(), Map.class);
            int expectedPercent = ((i + 1) * 10 * 100) / 40;
            assertEquals(expectedPercent, data.get("percentage"), 
                    "Interval " + i + ": percentage should be " + expectedPercent);

            // Simulate 500ms polling interval
            if (i < 3) {
                Thread.sleep(100); // Shorter for test, but mimics pattern
            }
        }
        long totalTime = System.currentTimeMillis() - startTime;
        System.out.println("Completed 4 polls in " + totalTime + "ms");
    }

}
