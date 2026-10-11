package es.rodrigonant.p2ai.xls2llm.service;

import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

import es.rodrigonant.p2ai.xls2llm.model.ProgressBar;

@Service
public class ProgressTracker {

    private final ConcurrentHashMap<Long, ProgressBar> progressMap = new ConcurrentHashMap<>();

    public ProgressBar createProgress(Long uploadId, int total) {
        ProgressBar progress = new ProgressBar(total);
        progressMap.put(uploadId, progress);
        return progress;
    }

    public ProgressBar getProgress(Long uploadId) {
        return progressMap.get(uploadId);
    }

    public void updateProgress(Long uploadId, int state) {
        ProgressBar progress = progressMap.get(uploadId);
        if (progress == null) {
            throw new IllegalArgumentException("Progress not found for uploadId: " + uploadId);
        }
        progress.setState(state);
    }

    public void removeProgress(Long uploadId) {
        progressMap.remove(uploadId);
    }

}
