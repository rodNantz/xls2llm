package es.rodrigonant.p2ai.xls2llm.portal.progress;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import es.rodrigonant.p2ai.xls2llm.service.ProgressTracker;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("ProgressController Integration Tests")
class ProgressControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProgressTracker progressTracker;

    @BeforeEach
    void setUp() {
        // Clear any existing progress trackers
    }

    @Test
    @DisplayName("GET /api/upload/{uploadId}/progress returns progress JSON")
    void testGetProgressSuccess() throws Exception {
        var progress = progressTracker.createProgress(1L, 100);
        progressTracker.updateProgress(1L, 50);
        progress.addSecondPassTotal(2);
        progress.completeSecondPassItems(1);

        mockMvc.perform(get("/api/upload/1/progress"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.current").value(50))
                .andExpect(jsonPath("$.total").value(100))
                .andExpect(jsonPath("$.percentage").value(50))
                .andExpect(jsonPath("$.secondPassCurrent").value(1))
                .andExpect(jsonPath("$.secondPassTotal").value(2))
                .andExpect(jsonPath("$.secondPassPercentage").value(50));
    }

    @Test
    @DisplayName("GET /api/upload/{uploadId}/progress reports interruption and resume line")
    void testInterruptedProgressIncludesResumeCheckpoint() throws Exception {
        var progress = progressTracker.createProgress(5L, 100);
        progress.setCheckpoint(40, 999, 1000);
        progress.interrupt("Network failure");

        mockMvc.perform(get("/api/upload/5/progress"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("INTERRUPTED"))
                .andExpect(jsonPath("$.error").value("Network failure"))
                .andExpect(jsonPath("$.current").value(40))
                .andExpect(jsonPath("$.lastCompletedLine").value(999))
                .andExpect(jsonPath("$.nextLine").value(1000));
    }

    @Test
    @DisplayName("GET /api/upload/{uploadId}/progress with missing uploadId returns 404")
    void testGetProgressNotFound() throws Exception {
        mockMvc.perform(get("/api/upload/999/progress"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /api/upload/{uploadId}/progress with 0% progress")
    void testGetProgressZeroPercent() throws Exception {
        progressTracker.createProgress(2L, 100);

        mockMvc.perform(get("/api/upload/2/progress"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.current").value(0))
                .andExpect(jsonPath("$.total").value(100))
                .andExpect(jsonPath("$.percentage").value(0));
    }

    @Test
    @DisplayName("GET /api/upload/{uploadId}/progress with 100% progress")
    void testGetProgressComplete() throws Exception {
        progressTracker.createProgress(3L, 100);
        progressTracker.updateProgress(3L, 100);

        mockMvc.perform(get("/api/upload/3/progress"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.current").value(100))
                .andExpect(jsonPath("$.total").value(100))
                .andExpect(jsonPath("$.percentage").value(100));
    }

    @Test
    @DisplayName("Progress updates are reflected in subsequent requests")
    void testProgressUpdates() throws Exception {
        progressTracker.createProgress(4L, 200);

        mockMvc.perform(get("/api/upload/4/progress"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.current").value(0))
                .andExpect(jsonPath("$.percentage").value(0));

        progressTracker.updateProgress(4L, 100);

        mockMvc.perform(get("/api/upload/4/progress"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.current").value(100))
                .andExpect(jsonPath("$.percentage").value(50));
    }

}
