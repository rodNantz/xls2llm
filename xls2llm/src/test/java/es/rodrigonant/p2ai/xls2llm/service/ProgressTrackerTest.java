package es.rodrigonant.p2ai.xls2llm.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;

import es.rodrigonant.p2ai.xls2llm.model.ProgressBar;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ProgressTracker Service Tests")
class ProgressTrackerTest {

    private ProgressTracker progressTracker;

    @BeforeEach
    void setUp() {
        progressTracker = new ProgressTracker();
    }

    @Test
    @DisplayName("createProgress creates a new ProgressBar with given total")
    void testCreateProgress() {
        ProgressBar progress = progressTracker.createProgress(1L, 100);
        assertNotNull(progress);
        assertEquals(100, progress.getTotal());
        assertEquals(0, progress.getCurrState());
    }

    @Test
    @DisplayName("getProgress retrieves created progress")
    void testGetProgress() {
        progressTracker.createProgress(1L, 100);
        ProgressBar progress = progressTracker.getProgress(1L);
        assertNotNull(progress);
        assertEquals(100, progress.getTotal());
    }

    @Test
    @DisplayName("getProgress returns null for non-existent uploadId")
    void testGetProgressNotFound() {
        ProgressBar progress = progressTracker.getProgress(999L);
        assertNull(progress);
    }

    @Test
    @DisplayName("updateProgress modifies existing progress state")
    void testUpdateProgress() {
        progressTracker.createProgress(1L, 100);
        progressTracker.updateProgress(1L, 50);
        ProgressBar progress = progressTracker.getProgress(1L);
        assertEquals(50, progress.getCurrState());
    }

    @Test
    @DisplayName("updateProgress throws exception if uploadId doesn't exist")
    void testUpdateProgressNotFound() {
        assertThrows(IllegalArgumentException.class, () -> progressTracker.updateProgress(999L, 50));
    }

    @Test
    @DisplayName("removeProgress deletes the progress tracking")
    void testRemoveProgress() {
        progressTracker.createProgress(1L, 100);
        progressTracker.removeProgress(1L);
        ProgressBar progress = progressTracker.getProgress(1L);
        assertNull(progress);
    }

    @Test
    @DisplayName("Multiple concurrent progress tracking")
    void testMultipleProgressTracking() {
        progressTracker.createProgress(1L, 100);
        progressTracker.createProgress(2L, 200);
        progressTracker.createProgress(3L, 300);

        progressTracker.updateProgress(1L, 50);
        progressTracker.updateProgress(2L, 100);
        progressTracker.updateProgress(3L, 150);

        assertEquals(50, progressTracker.getProgress(1L).getCurrState());
        assertEquals(100, progressTracker.getProgress(2L).getCurrState());
        assertEquals(150, progressTracker.getProgress(3L).getCurrState());
    }

    @Test
    @DisplayName("getProgress percentage calculation")
    void testProgressPercentage() {
        progressTracker.createProgress(1L, 100);
        progressTracker.updateProgress(1L, 50);
        ProgressBar progress = progressTracker.getProgress(1L);
        assertEquals(50, progress.getPercent());
    }

    @Test
    @DisplayName("createProgress with same uploadId replaces previous progress")
    void testCreateProgressOverwrite() {
        progressTracker.createProgress(1L, 100);
        progressTracker.updateProgress(1L, 50);
        
        progressTracker.createProgress(1L, 200);
        ProgressBar progress = progressTracker.getProgress(1L);
        
        assertEquals(200, progress.getTotal());
        assertEquals(0, progress.getCurrState());
    }

    @Test
    @DisplayName("updateProgress with invalid state throws exception")
    void testUpdateProgressInvalidState() {
        progressTracker.createProgress(1L, 100);
        assertThrows(IllegalArgumentException.class, () -> progressTracker.updateProgress(1L, 150));
    }

}
