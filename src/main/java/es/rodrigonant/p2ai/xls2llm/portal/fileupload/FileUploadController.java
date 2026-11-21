package es.rodrigonant.p2ai.xls2llm.portal.fileupload;

import com.openai.errors.OpenAIException;
import com.openai.errors.OpenAIInvalidDataException;
import com.openai.errors.OpenAIServiceException;
import com.openai.errors.RateLimitException;
import com.openai.errors.UnauthorizedException;
import es.rodrigonant.p2ai.xls2llm.document.DocumentManager;
import es.rodrigonant.p2ai.xls2llm.llmapi.LLMService;
import es.rodrigonant.p2ai.xls2llm.model.Input2xls;
import es.rodrigonant.p2ai.xls2llm.model.ProgressBar;
import es.rodrigonant.p2ai.xls2llm.model.Question;
import es.rodrigonant.p2ai.xls2llm.model.Request2LLM;
import es.rodrigonant.p2ai.xls2llm.model.classification.CategorizationResponse;
import es.rodrigonant.p2ai.xls2llm.model.classification.CommentRow;
import es.rodrigonant.p2ai.xls2llm.model.classification.CategoryCol;
import es.rodrigonant.p2ai.xls2llm.model.classification.ClassificationStatus;
import es.rodrigonant.p2ai.xls2llm.service.ProgressTracker;
import es.rodrigonant.p2ai.xls2llm.validation.ClassificationValidator;
import es.rodrigonant.p2ai.xls2llm.validation.ProjectProfile;
import es.rodrigonant.p2ai.xls2llm.validation.ProjectProfileLoader;
import es.rodrigonant.p2ai.xls2llm.validation.ClassificationResponseReconciler;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

@Controller
public class FileUploadController {

    private static final Logger LOG = LoggerFactory.getLogger(FileUploadController.class);

    @Autowired
    private DocumentManager dr;

    @Autowired
    @Qualifier("gpt-service")
    private LLMService llmService;

    @Autowired
    private ProgressTracker progressTracker;

    @Autowired
    private ClassificationValidator classificationValidator;

    @Autowired
    private ProjectProfileLoader profileLoader;

    @Autowired
    private ClassificationResponseReconciler responseReconciler;

    @Autowired
    private SecondPassClassifier secondPassClassifier;

    @GetMapping("/")
    public String index(Model model) {
        ProjectProfile profile = profileLoader.getProfile();
        model.addAttribute("profile", profile);
        model.addAttribute("profileTitle", profile.title() == null || profile.title().isBlank()
                ? profile.projectId() : profile.title());
        return "formUpload";
    }
    
    private static final int batchSize = 10;
    @org.springframework.beans.factory.annotation.Value("${xls2llm.classification.concurrency:4}")
    private int classificationConcurrency;
    
    @PostMapping("/upload")
    public ResponseEntity<?> handleFileUpload(@RequestParam("file") MultipartFile file, @RequestParam(value = "uploadId", required = false) Long uploadId, @RequestParam(value = "startLine", required = false) Integer startLine, @RequestParam(value = "rowLimit", required = false) Integer rowLimit, Model model) throws IOException {
    	// Save uploaded file to temp location
    	boolean idWasNull = (uploadId == null);
     if (uploadId == null) {
    		uploadId = (long) (Math.random() * 100000000);
     }
        Instant executionStartedAt = Instant.now();
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
//        List<Question> qsts = req.question().split(batchSize);
        List<CategorizationResponse> responses = new ArrayList<>();
        int validatedRows = 0;
        int successfulRows = 0;
        int missingResultRows = 0;
        int unexpectedResultIds = 0;
        int duplicateResultIds = 0;
        int secondPassItems = 0;
        int secondPassCorrectedN = 0;
        int secondPassResolvedR = 0;
        boolean llmUnavailable = false;
        String interruptionError = null;
        Question allCommentsQuestion = null;
        Map<Long, CommentRow> allFirstPassComments = new LinkedHashMap<>();
        Map<Long, ClassificationStatus> allValidationStatuses = new LinkedHashMap<>();
        Map<Long, String> allValidationReasons = new LinkedHashMap<>();
        long promptDurationNanos = 0L;
        int promptCount = 0;
        ExecutorService initialPassExecutor = Executors.newFixedThreadPool(
                Math.min(classificationConcurrency, Math.max(1, reqs.size())));
        AtomicInteger initialPromptCount = new AtomicInteger();
        AtomicLong initialPromptDurationNanos = new AtomicLong();
        List<Future<List<CategorizationResponse>>> initialPassCalls = new ArrayList<>();
        for (Request2LLM req : reqs) {
            initialPassCalls.add(initialPassExecutor.submit(() -> {
                long started = System.nanoTime();
                initialPromptCount.incrementAndGet();
                try {
                    return llmService.promptCategorization(req);
                } finally {
                    initialPromptDurationNanos.addAndGet(System.nanoTime() - started);
                }
            }));
        }
        try {
        for (int requestIndex = 0; requestIndex < reqs.size(); requestIndex++) {
            Request2LLM req = reqs.get(requestIndex);
            int completedBeforeBatch = currentRowOffset - rowOffset;
            if (!llmUnavailable) {
                try {
                    responses = initialPassCalls.get(requestIndex).get();
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException("Interrupted while waiting for initial classification", ex);
                } catch (ExecutionException ex) {
                    llmUnavailable = true;
                    Throwable cause = ex.getCause() == null ? ex : ex.getCause();
                    interruptionError = cause.getMessage() == null
                            ? cause.getClass().getSimpleName() : cause.getMessage();
                    responses = List.of();
                    LOG.error("LLM failed for uploadId=" + uploadId + "; stopping after this batch: "
                            + interruptionError, cause);
                }
            } else {
                responses = List.of();
            }
            ClassificationResponseReconciler.Result reconciliation = responseReconciler
                    .reconcile(req.question(), responses);
            unexpectedResultIds += reconciliation.unexpectedIds().size();
            duplicateResultIds += reconciliation.duplicateIds().size();
            if (!reconciliation.missingIds().isEmpty() || !reconciliation.duplicateIds().isEmpty()
                    || !reconciliation.unexpectedIds().isEmpty()) {
                LOG.info("LLM response reconciliation for uploadId=" + uploadId + ": missing="
                        + reconciliation.missingIds() + ", duplicate=" + reconciliation.duplicateIds()
                        + ", unexpected=" + reconciliation.unexpectedIds());
            }
            Map<Long, CommentRow> firstPassComments = commentsById(reconciliation.responses());
            Map<Long, ClassificationStatus> validationStatuses = new HashMap<>();
            Map<Long, String> validationReasons = new HashMap<>();
            validatedRows += req.question().getCommentIds().size();
            for (Long commentId : req.question().getCommentIds()) {
                CommentRow comment = firstPassComments.get(commentId);
                if (comment == null) {
                    validationStatuses.put(commentId, ClassificationStatus.INVALID);
                    validationReasons.put(commentId, "The first pass returned no result for this comment.");
                    continue;
                }
                ClassificationValidator.ValidationResult result = classificationValidator.validate(comment);
                boolean duplicate = reconciliation.duplicateIds().contains(commentId);
                ClassificationStatus status = result.hasHardErrors() || duplicate
                        ? ClassificationStatus.INVALID
                        : result.requiresReview() ? ClassificationStatus.REVIEW : ClassificationStatus.VALID;
                String reason = result.hasHardErrors()
                        ? ClassificationValidator.summarizeIssues(result.issues()) : "";
                if (duplicate) reason = appendReason(reason, "Duplicate result for commentId=" + commentId);
                if (result.requiresReview()) {
                    reason = appendReason(reason,
                            "Review: " + ClassificationValidator.summarizeIssues(result.reviewReasons()));
                }
                validationStatuses.put(commentId, status);
                if (!reason.isBlank()) validationReasons.put(commentId, reason);
            }

            if (allCommentsQuestion == null) {
                allCommentsQuestion = new Question(req.question().getRowZeroUserQuestion());
                allCommentsQuestion.setRowZeroSystemQuestions(new ArrayList<>(req.question().getRowZeroSystemQuestions()));
            }
            for (int i = 0; i < req.question().getCommentIds().size(); i++) {
                allCommentsQuestion.addNextLine(req.question().getCommentIds().get(i), req.question().getNextLines().get(i));
            }
            allFirstPassComments.putAll(firstPassComments);
            allValidationStatuses.putAll(validationStatuses);
            allValidationReasons.putAll(validationReasons);

            Map<Long, List<ClassificationValidator.Issue>> validationIssues = new HashMap<>();
            for (Long commentId : req.question().getCommentIds()) {
                ClassificationStatus status = validationStatuses.getOrDefault(commentId,
                        ClassificationStatus.INVALID);
                CommentRow comment = firstPassComments.get(commentId);
                if (status == ClassificationStatus.VALID) successfulRows++;
                if (status == ClassificationStatus.INVALID && comment != null) {
                    ClassificationValidator.ValidationResult result = classificationValidator.validate(comment);
                    validationIssues.put(commentId, result.issues());
                }
                if (comment == null && status == ClassificationStatus.INVALID) {
                    validationIssues.put(commentId, List.of(new ClassificationValidator.Issue(
                            "", "No result after the single second pass", -1)));
                }
                if (comment != null) {
                    for (CategoryCol category : comment.getCategories()) {
                        if (category.getEvaluationType() != null) {
                            String variable = classificationValidator.variableName(category.getEvaluationType());
                            if (variable != null) category.setCode(
                                    classificationValidator.serializeCode(variable, category.getCode()));
                        }
                    }
                }
                if (status == ClassificationStatus.INVALID || status == ClassificationStatus.REVIEW) {
                    LOG.warn("Validation result uploadId=" + uploadId + ", commentId=" + commentId
                            + ", status=" + status + ", reason=" + validationReasons.get(commentId));
                }
            }
            Map<Long, CommentRow> completedComments = firstPassComments;
            missingResultRows += (int) req.question().getCommentIds().stream()
                    .filter(id -> !completedComments.containsKey(id)).count();
            List<CategorizationResponse> finalResponses = responseFromComments(firstPassComments);
            // Diagnostic logging: responses and processed counts
            int responsesCount = finalResponses.size();
            int commentsCount = 0;
            if (finalResponses != null) {
                for (CategorizationResponse cr : finalResponses) {
                    if (cr.getComments() != null) commentsCount += cr.getComments().size();
                }
            }
            LOG.info("Batch starting at rowOffset=" + currentRowOffset + ": responses.count=" + responsesCount + ", comments.count=" + commentsCount);
            Input2xls input = Input2xls.fromCategorizationResponseList(finalResponses, currentRowOffset);
            validationIssues.forEach((commentId, issues) -> issues.stream()
                    .forEach(issue -> {
                        if (issue.columnIndex() >= 0) {
                            input.markInvalid(Math.toIntExact(commentId), issue.columnIndex());
                        } else {
                            for (int column = 0; column < classificationValidator.variableCount(); column++) {
                                input.markInvalid(Math.toIntExact(commentId), column);
                            }
                        }
                    }));
            for (Long commentId : req.question().getCommentIds()) {
                ClassificationStatus status = validationStatuses.get(commentId);
                if (status != null) input.markValidationStatus(Math.toIntExact(commentId),
                        classificationValidator.variableCount(), status, validationReasons.get(commentId));
                if (!firstPassComments.containsKey(commentId)) {
                    for (int column = 0; column < classificationValidator.variableCount(); column++) {
                        input.markInvalid(Math.toIntExact(commentId), column);
                    }
                }
            }
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
            try {
                dr.writeDocument(sourcePath, outputPath, input);
            } catch (RuntimeException writeFailure) {
                progress.interrupt("Falha ao salvar o lote iniciado na linha "
                        + (effectiveStart == null ? completedBeforeBatch + 1 : effectiveStart + completedBeforeBatch)
                        + ": " + writeFailure.getMessage());
                throw writeFailure;
            }
            // Advance the offset by the actual number of comment rows written (use input rows count)
            currentRowOffset += processed;
            int completedRows = currentRowOffset - rowOffset;
            int checkpointRows = llmUnavailable ? completedBeforeBatch : completedRows;
            progress.setCheckpoint(checkpointRows,
                    checkpointRows == 0 ? 0 : (effectiveStart == null ? checkpointRows : effectiveStart + checkpointRows - 1),
                    effectiveStart == null ? checkpointRows + 1 : effectiveStart + checkpointRows);
            LOG.info("Progress: " + (currentRowOffset - rowOffset) + ", Current row offset: " + currentRowOffset);
            if (llmUnavailable) break;
        }
        } finally {
            initialPassExecutor.shutdownNow();
            promptCount += initialPromptCount.get();
            promptDurationNanos += initialPromptDurationNanos.get();
        }

        // The second pass starts only after every first-pass batch has been processed.
        if (!llmUnavailable && allCommentsQuestion != null && allValidationStatuses.values().stream()
                .anyMatch(ClassificationStatus::requiresSecondPass)) {
            SecondPassClassifier.Result secondPass = secondPassClassifier.run(allCommentsQuestion,
                    allFirstPassComments, allValidationStatuses, allValidationReasons,
                    progress::addSecondPassTotal, progress::completeSecondPassItems);
            secondPassItems += secondPass.attempted();
            secondPassCorrectedN += secondPass.correctedN();
            secondPassResolvedR += secondPass.resolvedR();
            promptCount += secondPass.promptCount();
            promptDurationNanos += secondPass.promptDurationNanos();

            List<CategorizationResponse> finalResponses = responseFromComments(secondPass.finalComments());
            Input2xls secondPassOutput = Input2xls.fromCategorizationResponseList(finalResponses, rowOffset);
            for (Long commentId : allCommentsQuestion.getCommentIds()) {
                ClassificationStatus status = secondPass.finalStatuses().get(commentId);
                if (status != null) {
                    int row = Math.toIntExact(commentId);
                    secondPassOutput.markValidationStatus(row, classificationValidator.variableCount(), status,
                            secondPass.finalReasons().get(commentId));
                    if (status == ClassificationStatus.INVALID) {
                        CommentRow comment = secondPass.finalComments().get(commentId);
                        if (comment == null) {
                            for (int column = 0; column < classificationValidator.variableCount(); column++) {
                                secondPassOutput.markInvalid(row, column);
                            }
                        } else {
                            ClassificationValidator.ValidationResult validation = classificationValidator.validate(comment);
                            for (ClassificationValidator.Issue issue : validation.issues()) {
                                if (issue.columnIndex() >= 0) secondPassOutput.markInvalid(row, issue.columnIndex());
                            }
                        }
                    }
                }
            }
            // Rebuild the finalized rows from the uploaded workbook so obsolete first-pass red styles are discarded.
            dr.writeDocument(inputPath, outputPath, secondPassOutput);
            successfulRows = (int) secondPass.finalStatuses().values().stream()
                    .filter(status -> status == ClassificationStatus.VALID).count();
        }

        ProjectProfile profile = profileLoader.getProfile();
        String validationSuccessRate = validatedRows == 0 ? "N/A"
                : String.format(java.util.Locale.ROOT, "%.2f%%", successfulRows * 100.0 / validatedRows);
        Map<String, String> executionMetadata = new java.util.LinkedHashMap<>();
        executionMetadata.put("Profile", profile.projectId() + "@" + profile.version());
        executionMetadata.put("Profile ID", profile.projectId());
        executionMetadata.put("Profile version", profile.version());
        executionMetadata.put("Model", llmService.getModelName());
        executionMetadata.put("Rows processed", String.valueOf(validatedRows));
        executionMetadata.put("Rows passed validation", String.valueOf(successfulRows));
        executionMetadata.put("Remaining errors/reviews", String.valueOf(Math.max(0, validatedRows - successfulRows)));
        executionMetadata.put("Rows missing LLM result", String.valueOf(missingResultRows));
        long totalExecutionMillis = java.time.Duration.between(executionStartedAt, Instant.now()).toMillis();
        executionMetadata.put("Total execution time", String.format(java.util.Locale.ROOT,
                "%.2f seconds", totalExecutionMillis / 1000.0));
        executionMetadata.put("Average time per AI prompt/batch", promptCount == 0 ? "N/A"
                : String.format(java.util.Locale.ROOT, "%.2f seconds",
                        promptDurationNanos / (double) promptCount / 1_000_000_000.0));
        executionMetadata.put("Second-pass items", String.valueOf(secondPassItems));
        executionMetadata.put("Second-pass N corrected", String.valueOf(secondPassCorrectedN));
        executionMetadata.put("Second-pass R resolved by agreement", String.valueOf(secondPassResolvedR));
        executionMetadata.put("Unexpected LLM result IDs", String.valueOf(unexpectedResultIds));
        executionMetadata.put("Duplicate LLM result IDs", String.valueOf(duplicateResultIds));
        executionMetadata.put("Validation success rate", validationSuccessRate);
        executionMetadata.put("Execution ID", String.valueOf(uploadId));
        executionMetadata.put("Started at (UTC)", executionStartedAt.toString());
        try {
            dr.writeExecutionMetadata(inputPath, outputPath, executionMetadata);
        } catch (RuntimeException writeFailure) {
            progress.interrupt("Falha ao salvar os metadados finais. Os lotes anteriores permanecem no arquivo parcial: "
                    + writeFailure.getMessage());
            throw writeFailure;
        }

        if (interruptionError != null) {
            int nextLine = progress.getNextLine();
            progress.interrupt("Falha ao processar o lote iniciado na linha " + nextLine + ": "
                    + interruptionError + ". O arquivo parcial foi baixado; retome usando essa linha como início.");
        } else {
            progress.complete();
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

    private Map<Long, CommentRow> commentsById(List<CategorizationResponse> responses) {
        Map<Long, CommentRow> comments = new LinkedHashMap<>();
        if (responses == null) return comments;
        for (CategorizationResponse response : responses) {
            if (response == null || response.getComments() == null) continue;
            for (CommentRow comment : response.getComments()) {
                if (comment != null) comments.putIfAbsent(comment.getCommentId(), comment);
            }
        }
        return comments;
    }

    private List<CategorizationResponse> responseFromComments(Map<Long, CommentRow> comments) {
        CategorizationResponse response = new CategorizationResponse();
        response.setComments(new ArrayList<>(comments.values()));
        return List.of(response);
    }

    private ResponseEntity<Map<String, String>> llmError(HttpStatus status, String message) {
        return ResponseEntity.status(status)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("message", message));
    }

    private String appendReason(String current, String addition) {
        if (current == null || current.isBlank()) return addition;
        if (addition == null || addition.isBlank()) return current;
        return current + "; " + addition;
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
