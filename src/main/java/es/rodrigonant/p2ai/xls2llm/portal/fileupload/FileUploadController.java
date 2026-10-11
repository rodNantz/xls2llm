package es.rodrigonant.p2ai.xls2llm.portal.fileupload;

import com.openai.errors.OpenAIException;
import com.openai.errors.OpenAIInvalidDataException;
import com.openai.errors.OpenAIServiceException;
import com.openai.errors.RateLimitException;
import com.openai.errors.UnauthorizedException;
import es.rodrigonant.p2ai.xls2llm.aop.Logger;
import es.rodrigonant.p2ai.xls2llm.document.DocumentManager;
import es.rodrigonant.p2ai.xls2llm.llmapi.LLMService;
import es.rodrigonant.p2ai.xls2llm.model.Input2xls;
import es.rodrigonant.p2ai.xls2llm.model.ProgressBar;
import es.rodrigonant.p2ai.xls2llm.model.Question;
import es.rodrigonant.p2ai.xls2llm.model.Request2LLM;
import es.rodrigonant.p2ai.xls2llm.model.classification.CategorizationResponse;
import es.rodrigonant.p2ai.xls2llm.model.classification.CommentRow;
import es.rodrigonant.p2ai.xls2llm.model.classification.CategoryCol;
import es.rodrigonant.p2ai.xls2llm.service.ProgressTracker;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;

@Controller
public class FileUploadController {

    @Autowired
    private DocumentManager dr;

    private Logger LOG = new Logger(FileUploadController.class);
    
    @Autowired
    @Qualifier("gpt-service")
    private LLMService llmService;

    @Autowired
    private ProgressTracker progressTracker;

    @GetMapping("/")
    public String index() {
        return "formUpload";
    }
    
    private static final int batchSize = 10;
    
    @PostMapping("/upload")
    public ResponseEntity<?> handleFileUpload(@RequestParam("file") MultipartFile file, @RequestParam(value = "uploadId", required = false) Long uploadId, @RequestParam(value = "startLine", required = false) Integer startLine, @RequestParam(value = "rowLimit", required = false) Integer rowLimit, Model model) throws IOException {
    	// Save uploaded file to temp location
    	boolean idWasNull = (uploadId == null);
    	if (uploadId == null) {
    		uploadId = (long) (Math.random() * 100000000);
    	}
    	LOG.info("=== UPLOAD START === uploadId=" + uploadId + " (provided=" + !idWasNull + "), startLine=" + startLine + ", rowLimit=" + rowLimit);
        File tempInput = File.createTempFile("input", ".xlsx");
        file.transferTo(tempInput);
        String inputPath = tempInput.getAbsolutePath();
        String outputPath = tempInput.getParent() + File.separator + "output-" + tempInput.getName();

        // Process file (reuse logic from CommandLineRunnerV1)
        // If rowLimit is null or not positive, pass null to process all rows
        Integer effectiveLimit = (rowLimit != null && rowLimit > 0) ? rowLimit : null;
        Integer effectiveStart = (startLine != null && startLine > 0) ? startLine : null;
//        Request2LLM req = dr.getDocument(inputPath, effectiveLimit);
        List<Request2LLM> reqs = dr.getDocument(inputPath, effectiveStart, effectiveLimit, batchSize);
        ProgressBar progress = progressTracker.createProgress(uploadId, getRequestSize(reqs, null));
        
        // rowOffset is 0-based index from start of data. When passed to Input2xls,
        // it will be converted to Excel row by adding CONTENT_INITIAL_LINE in setContentIntoXls.
        // startLine is 1-based user input, so convert: startLine=15 -> index=14
        int rowOffset = (effectiveStart != null && effectiveStart > 0) ? effectiveStart - 1 : 0;
        int currentRowOffset = rowOffset;
        
        // for every 10 rows: call LLM and write output
        int i = 0;
//        List<Question> qsts = req.question().split(batchSize);
        List<CategorizationResponse> responses = new ArrayList<>();
        for (Request2LLM req : reqs) {
        	// Call LLM and capture the returned categorization responses for this batch
            try {
                responses = llmService.promptCategorization(req);
            } catch (RateLimitException ex) {
                LOG.error("OpenAI rate/quota limit for uploadId=" + uploadId + ": " + ex.getMessage());
                progressTracker.removeProgress(uploadId);
                return llmError(HttpStatus.TOO_MANY_REQUESTS,
                        "The OpenAI API rate or credit limit has been reached. Check your API usage or add credits at https://platform.openai.com/settings/organization/billing/.");
            } catch (UnauthorizedException ex) {
                LOG.error("OpenAI rejected the API credentials for uploadId=" + uploadId + ": " + ex.getMessage());
                progressTracker.removeProgress(uploadId);
                return llmError(HttpStatus.BAD_GATEWAY,
                        "The AI service rejected its API credentials. Check the OpenAI API key configured for the application.");
            } catch (OpenAIInvalidDataException ex) {
                LOG.error("OpenAI response could not be parsed for uploadId=" + uploadId + ": " + ex.getMessage());
                progressTracker.removeProgress(uploadId);
                return llmError(HttpStatus.BAD_GATEWAY,
                        "The AI service returned a response that could not be processed. Please try again with fewer rows.");
            } catch (OpenAIServiceException ex) {
                LOG.error("OpenAI service error for uploadId=" + uploadId + ": " + ex.getMessage());
                progressTracker.removeProgress(uploadId);
                return llmError(HttpStatus.BAD_GATEWAY,
                        "The AI service could not complete the request. Please try again later.");
            } catch (OpenAIException ex) {
                LOG.error("OpenAI request failed for uploadId=" + uploadId + ": " + ex.getMessage());
                progressTracker.removeProgress(uploadId);
                return llmError(HttpStatus.BAD_GATEWAY,
                        "The AI service is unavailable or could not process the request. Please try again later.");
            } catch (RuntimeException ex) {
                LOG.error("LLM request failed for uploadId=" + uploadId + ": " + ex.getMessage());
                progressTracker.removeProgress(uploadId);
                return llmError(HttpStatus.BAD_GATEWAY,
                        "AI processing could not be completed. Please try again later.");
            }
            try {
                validateResponseIds(req.question(), responses);
            } catch (IllegalArgumentException ex) {
                LOG.error("Invalid LLM response for uploadId=" + uploadId + ": " + ex.getMessage());
                progressTracker.removeProgress(uploadId);
                return llmError(HttpStatus.BAD_GATEWAY,
                        "The AI service returned incomplete or inconsistent classifications. Please retry the upload.");
            }
            // Diagnostic logging: responses and processed counts
            int responsesCount = responses == null ? 0 : responses.size();
            int commentsCount = 0;
            if (responses != null) {
                for (CategorizationResponse cr : responses) {
                    if (cr.getComments() != null) commentsCount += cr.getComments().size();
                }
            }
            LOG.info("Batch starting at rowOffset=" + currentRowOffset + ": responses.count=" + responsesCount + ", comments.count=" + commentsCount);
            Input2xls input = Input2xls.fromCategorizationResponseList(responses, currentRowOffset);
            // Log Input2xls entries
            Enumeration<Integer> rowsDbg = input.getRowIndexes();
            StringBuilder sb = new StringBuilder();
            int processed = 0;
            while (rowsDbg.hasMoreElements()) {
                int rr = rowsDbg.nextElement();
                Enumeration<Integer> colsDbg = input.getColIndexes(rr);
                boolean rowHasCol = false;
                while (colsDbg.hasMoreElements()) {
                    int cc = colsDbg.nextElement();
                    sb.append("[r=").append(rr).append(",c=").append(cc).append(",v=").append(input.getCellContent(rr, cc)).append("] ");
                    rowHasCol = true;
                }
                if (rowHasCol) processed++;
            }
            LOG.info("Prepared Input2xls entries: " + sb.toString());
            // Use the original input file only for the first write; subsequent writes must use the last output
            String sourcePath = (currentRowOffset == rowOffset && processed > 0) ? inputPath : outputPath;
            dr.writeDocument(sourcePath, outputPath, input);
            // Advance the offset by the actual number of comment rows written (use input rows count)
            currentRowOffset += processed;
            progressTracker.updateProgress(uploadId, currentRowOffset - rowOffset);
            LOG.info("Progress: " + (currentRowOffset - rowOffset) + ", Current row offset: " + currentRowOffset);
        }
        
        // Return the written file as a download
        File result = new File(outputPath);
        InputStreamResource resource = new InputStreamResource(new FileInputStream(result));
        
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + result.getName())
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .contentLength(result.length())
                .body(resource);
    }
    
    
    public int getRequestSize(List<Request2LLM> reqs, Integer limit) {
		int size = 0;
		for (Request2LLM r : reqs) {
			size += r.question().getNextLines().size();
		}
		// DocumentManager already applied the limit, so just return actual size
		return size;
	}

    private ResponseEntity<Map<String, String>> llmError(HttpStatus status, String message) {
        return ResponseEntity.status(status)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("message", message));
    }

    private void validateResponseIds(Question question, List<CategorizationResponse> responses) {
        java.util.Set<Long> expected = new java.util.HashSet<>(question.getCommentIds());
        java.util.Set<Long> received = new java.util.HashSet<>();
        if (responses != null) {
            for (CategorizationResponse response : responses) {
                if (response == null || response.getComments() == null) {
                    throw new IllegalArgumentException("LLM response is missing comments");
                }
                for (CommentRow comment : response.getComments()) {
                    if (comment == null || !expected.contains(comment.getCommentId())) {
                        throw new IllegalArgumentException("LLM returned a missing or unexpected commentId");
                    }
                    if (!received.add(comment.getCommentId())) {
                        throw new IllegalArgumentException("LLM returned a duplicate commentId");
                    }
                    if (comment.getCategories() == null || comment.getCategories().isEmpty()) {
                        throw new IllegalArgumentException("LLM response has no categories for commentId=" + comment.getCommentId());
                    }
                }
            }
        }
        if (!received.equals(expected)) {
            throw new IllegalArgumentException("LLM response is missing one or more commentIds");
        }
    }
    
}
