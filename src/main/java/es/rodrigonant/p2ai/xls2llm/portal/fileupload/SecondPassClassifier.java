package es.rodrigonant.p2ai.xls2llm.portal.fileupload;

import java.util.ArrayList;
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
import java.util.function.IntConsumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import es.rodrigonant.p2ai.xls2llm.llmapi.LLMService;
import es.rodrigonant.p2ai.xls2llm.model.Question;
import es.rodrigonant.p2ai.xls2llm.model.classification.CategorizationResponse;
import es.rodrigonant.p2ai.xls2llm.model.classification.CategoryCol;
import es.rodrigonant.p2ai.xls2llm.model.classification.ClassificationStatus;
import es.rodrigonant.p2ai.xls2llm.model.classification.CommentRow;
import es.rodrigonant.p2ai.xls2llm.validation.ClassificationResponseReconciler;
import es.rodrigonant.p2ai.xls2llm.validation.ClassificationValidator;
import es.rodrigonant.p2ai.xls2llm.validation.ProjectProfileLoader;

/** Runs one bounded second pass over only hard-invalid and review-required comments. */
@Component
public class SecondPassClassifier {
    private static final Logger LOG = LoggerFactory.getLogger(SecondPassClassifier.class);

    private final LLMService llmService;
    private final ClassificationValidator validator;
    private final ClassificationResponseReconciler reconciler;
    private final ProjectProfileLoader profileLoader;
    private final int batchSize;
    private final int concurrency;

    public SecondPassClassifier(@Qualifier("gpt-service") LLMService llmService,
            ClassificationValidator validator,
            ClassificationResponseReconciler reconciler, ProjectProfileLoader profileLoader,
            @Value("${xls2llm.classification.second-pass.batch-size:10}") int batchSize,
            @Value("${xls2llm.classification.concurrency:4}") int concurrency) {
        this.llmService = llmService;
        this.validator = validator;
        this.reconciler = reconciler;
        this.profileLoader = profileLoader;
        if (batchSize < 1) throw new IllegalArgumentException("Second-pass batch size must be positive");
        if (concurrency < 1) throw new IllegalArgumentException("Second-pass concurrency must be positive");
        this.batchSize = batchSize;
        this.concurrency = concurrency;
    }

    public Result run(Question originalQuestion, Map<Long, CommentRow> originalComments,
            Map<Long, ClassificationStatus> initialStatuses, Map<Long, String> initialReasons) {
        return run(originalQuestion, originalComments, initialStatuses, initialReasons, ignored -> {}, ignored -> {});
    }

    public Result run(Question originalQuestion, Map<Long, CommentRow> originalComments,
            Map<Long, ClassificationStatus> initialStatuses, Map<Long, String> initialReasons,
            IntConsumer onCandidatesDiscovered, IntConsumer onCandidatesSettled) {
        Map<Long, CommentRow> finalComments = new LinkedHashMap<>(originalComments);
        Map<Long, ClassificationStatus> finalStatuses = new HashMap<>(initialStatuses);
        Map<Long, String> finalReasons = new HashMap<>(initialReasons);
        int attempts = 0;
        int correctedN = 0;
        int resolvedR = 0;
        List<RetryItem> candidates = new ArrayList<>();

        Map<Long, String> textById = new HashMap<>();
        for (int i = 0; i < originalQuestion.getCommentIds().size(); i++) {
            textById.put(originalQuestion.getCommentIds().get(i), originalQuestion.getNextLines().get(i));
        }
        for (Long commentId : originalQuestion.getCommentIds()) {
            ClassificationStatus status = initialStatuses.get(commentId);
            if (status != null && status.requiresSecondPass()) {
                candidates.add(new RetryItem(commentId, textById.get(commentId), status,
                        initialReasons.getOrDefault(commentId, ""), originalComments.get(commentId)));
            }
        }
        if (candidates.isEmpty()) return new Result(finalComments, finalStatuses, finalReasons,
                attempts, correctedN, resolvedR, 0, 0L);
        onCandidatesDiscovered.accept(candidates.size());

        List<List<RetryItem>> batches = new ArrayList<>();
        for (int start = 0; start < candidates.size(); start += batchSize) {
            batches.add(candidates.subList(start, Math.min(start + batchSize, candidates.size())));
        }
        attempts = candidates.size();
        AtomicInteger promptCount = new AtomicInteger();
        AtomicLong promptDurationNanos = new AtomicLong();
        ExecutorService executor = Executors.newFixedThreadPool(Math.min(concurrency, batches.size()));
        try {
            List<Future<List<CategorizationResponse>>> calls = new ArrayList<>();
            for (List<RetryItem> batch : batches) {
                Question secondQuestion = buildSecondPassQuestion(originalQuestion, batch);
                calls.add(executor.submit(() -> {
                    long started = System.nanoTime();
                    promptCount.incrementAndGet();
                    try {
                        return llmService.promptCategorization(secondQuestion, new ArrayList<>());
                    } finally {
                        promptDurationNanos.addAndGet(System.nanoTime() - started);
                    }
                }));
            }

            // Calls overlap, while reconciliation and all result updates remain serialized here.
            for (int batchIndex = 0; batchIndex < batches.size(); batchIndex++) {
                List<RetryItem> batch = batches.get(batchIndex);
                List<CategorizationResponse> rawResponses;
                try {
                    rawResponses = calls.get(batchIndex).get();
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException("Interrupted while waiting for second-pass results", interrupted);
                } catch (ExecutionException secondPassFailure) {
                    Throwable cause = secondPassFailure.getCause();
                    String detail = cause instanceof RuntimeException runtime ? messageOf(runtime)
                            : cause == null ? secondPassFailure.getMessage() : cause.toString();
                    LOG.warn("Second pass failed for commentIds={} (preserving first-pass results): {}",
                            batch.stream().map(RetryItem::commentId).toList(), detail, cause);
                    for (RetryItem item : batch) {
                        finalReasons.put(item.commentId(), append(finalReasons.get(item.commentId()),
                                "Second pass unavailable; original result retained: " + detail));
                    }
                    onCandidatesSettled.accept(batch.size());
                    continue;
                }

                Question secondQuestion = buildSecondPassQuestion(originalQuestion, batch);
                ClassificationResponseReconciler.Result reconciliation = reconciler.reconcile(secondQuestion,
                        rawResponses);
                Map<Long, CommentRow> secondComments = commentsById(reconciliation.responses());
                for (RetryItem item : batch) {
                CommentRow secondComment = secondComments.get(item.commentId());
                if (secondComment == null || reconciliation.duplicateIds().contains(item.commentId())) {
                    finalReasons.put(item.commentId(), append(finalReasons.get(item.commentId()),
                            "Second pass returned no unique result; original result retained."));
                    continue;
                }

                ClassificationValidator.ValidationResult secondValidation = validator.validate(secondComment);
                if (item.initialStatus() == ClassificationStatus.INVALID) {
                    finalComments.put(item.commentId(), secondComment);
                    if (secondValidation.hasHardErrors()) {
                        finalStatuses.put(item.commentId(), ClassificationStatus.INVALID);
                        finalReasons.put(item.commentId(), append(
                                ClassificationValidator.summarizeIssues(secondValidation.issues()),
                                "Second pass did not resolve the hard validation errors."));
                    } else if (secondValidation.requiresReview()) {
                        finalStatuses.put(item.commentId(), ClassificationStatus.REVIEW);
                        finalReasons.put(item.commentId(), "Second pass corrected hard errors; "
                                + ClassificationValidator.summarizeIssues(secondValidation.reviewReasons()));
                    } else {
                        finalStatuses.put(item.commentId(), ClassificationStatus.VALID);
                        finalReasons.put(item.commentId(), "Retake passed");
                        correctedN++;
                    }
                } else {
                    CommentRow firstComment = item.firstComment();
                    if (firstComment != null && !secondValidation.hasHardErrors()
                            && !secondValidation.requiresReview() && sameCodes(firstComment, secondComment)) {
                        finalComments.put(item.commentId(), secondComment);
                        finalStatuses.put(item.commentId(), ClassificationStatus.VALID);
                        finalReasons.put(item.commentId(), "Retake passed");
                        resolvedR++;
                    } else {
                        finalStatuses.put(item.commentId(), ClassificationStatus.REVIEW);
                        finalReasons.put(item.commentId(), append(item.initialReason(), describeSecondOpinion(
                                secondComment, secondValidation)));
                    }
                }
                }
                onCandidatesSettled.accept(batch.size());
            }
        } finally {
            executor.shutdownNow();
        }

        return new Result(finalComments, finalStatuses, finalReasons, attempts, correctedN, resolvedR,
                promptCount.get(), promptDurationNanos.get());
    }

    private Question buildSecondPassQuestion(Question original, List<RetryItem> batch) {
        Question question = new Question(original.getRowZeroUserQuestion());
        question.setRowZeroSystemQuestions(new ArrayList<>(original.getRowZeroSystemQuestions()));
        question.addRowZeroSystemQuestion("SECOND PASS: Return exactly one complete result for each supplied commentId. "
                + "For TARGETED_CORRECTION items, use the validation issue and previous codes to repair the result; "
                + "preserve unaffected valid codes where appropriate. For INDEPENDENT_SECOND_OPINION items, "
                + "classify the comment independently without inferring or seeing a previous answer. "
                + "Follow all project-profile rules and return the full structured schema.");
        for (RetryItem item : batch) {
            question.addNextLine(item.commentId(), item.text());
            if (item.initialStatus() == ClassificationStatus.INVALID) {
                question.setSecondPassContext(item.commentId(), "TARGETED_CORRECTION",
                        "Validation issue: " + item.initialReason() + ". Previous codes: "
                                + formatCodes(item.firstComment()));
            } else {
                question.setSecondPassContext(item.commentId(), "INDEPENDENT_SECOND_OPINION", null);
            }
        }
        return question;
    }

    private Map<Long, CommentRow> commentsById(List<CategorizationResponse> responses) {
        Map<Long, CommentRow> comments = new HashMap<>();
        if (responses == null) return comments;
        for (CategorizationResponse response : responses) {
            if (response == null || response.getComments() == null) continue;
            for (CommentRow comment : response.getComments()) {
                if (comment != null) comments.putIfAbsent(comment.getCommentId(), comment);
            }
        }
        return comments;
    }

    private boolean sameCodes(CommentRow first, CommentRow second) {
        return codesByName(first).equals(codesByName(second));
    }

    private Map<String, String> codesByName(CommentRow comment) {
        Map<String, String> codes = new LinkedHashMap<>();
        if (comment == null || comment.getCategories() == null) return codes;
        for (var variable : profileLoader.getProfile().variables()) {
            String code = comment.getCategories().stream()
                    .filter(category -> category != null
                            && category.getEvaluationType() == variable.evaluationType())
                    .map(CategoryCol::getCode).findFirst().orElse(null);
            codes.put(variable.name(), validator.serializeCode(variable.name(), code));
        }
        return codes;
    }

    private String formatCodes(CommentRow comment) {
        if (comment == null) return "no previous result";
        Map<String, String> codes = codesByName(comment);
        return codes.toString();
    }

    private String describeSecondOpinion(CommentRow comment,
            ClassificationValidator.ValidationResult validation) {
        if (validation.hasHardErrors()) {
            return "Second opinion failed hard validation: "
                    + ClassificationValidator.summarizeIssues(validation.issues());
        }
        if (validation.requiresReview()) {
            return "Second opinion also requires review: "
                    + ClassificationValidator.summarizeIssues(validation.reviewReasons());
        }
        return "Second opinion disagrees; alternative codes: " + formatCodes(comment)
                + ". Original classification retained for review.";
    }

    private String append(String first, String second) {
        if (first == null || first.isBlank()) return second;
        if (second == null || second.isBlank()) return first;
        return first + "; " + second;
    }

    private String messageOf(RuntimeException exception) {
        StringBuilder detail = new StringBuilder();
        Throwable cause = exception;
        while (cause != null) {
            if (!detail.isEmpty()) detail.append(" <- ");
            detail.append(cause.getClass().getSimpleName());
            if (cause.getMessage() != null && !cause.getMessage().isBlank()) {
                detail.append(": ").append(cause.getMessage());
            }
            cause = cause.getCause();
        }
        return detail.toString();
    }

    private record RetryItem(long commentId, String text, ClassificationStatus initialStatus, String initialReason,
            CommentRow firstComment) {}

    public record Result(Map<Long, CommentRow> finalComments, Map<Long, ClassificationStatus> finalStatuses,
            Map<Long, String> finalReasons, int attempted,
            int correctedN, int resolvedR, int promptCount, long promptDurationNanos) {}
}
