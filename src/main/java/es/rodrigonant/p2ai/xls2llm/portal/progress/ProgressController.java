package es.rodrigonant.p2ai.xls2llm.portal.progress;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import es.rodrigonant.p2ai.xls2llm.model.ProgressBar;
import es.rodrigonant.p2ai.xls2llm.service.ProgressTracker;

@RestController
public class ProgressController {

    @Autowired
    private ProgressTracker progressTracker;

    @GetMapping("/api/upload/{uploadId}/progress")
    public ResponseEntity<ProgressResponse> getProgress(@PathVariable Long uploadId) {
        ProgressBar progress = progressTracker.getProgress(uploadId);
        
        if (progress == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }
        
        ProgressResponse response = new ProgressResponse(
            progress.getCurrState(),
            progress.getTotal(),
            progress.getPercent()
        );
        
        return ResponseEntity.ok(response);
    }

}
