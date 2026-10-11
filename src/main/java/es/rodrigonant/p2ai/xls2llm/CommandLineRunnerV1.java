package es.rodrigonant.p2ai.xls2llm;

import java.time.Instant;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import es.rodrigonant.p2ai.xls2llm.document.DocumentManager;
import es.rodrigonant.p2ai.xls2llm.llmapi.LLMService;
import es.rodrigonant.p2ai.xls2llm.model.Input2xls;
import es.rodrigonant.p2ai.xls2llm.model.Request2LLM;
import es.rodrigonant.p2ai.xls2llm.model.classification.CategorizationResponse;
import es.rodrigonant.p2ai.xls2llm.model.classification.CategoryCol;
import es.rodrigonant.p2ai.xls2llm.model.classification.CommentRow;
import es.rodrigonant.p2ai.xls2llm.validation.ClassificationValidator;
import es.rodrigonant.p2ai.xls2llm.validation.ProjectProfile;
import es.rodrigonant.p2ai.xls2llm.validation.ProjectProfileLoader;
import es.rodrigonant.p2ai.xls2llm.validation.ClassificationResponseReconciler;

/** Command-line equivalent of the UI upload flow; enabled with xls2llm.cli.enabled=true. */
@Component
@Profile("!test")
@ConditionalOnProperty(name = "xls2llm.cli.enabled", havingValue = "true")
public class CommandLineRunnerV1 implements ApplicationRunner {
    private static final int BATCH_SIZE = 10;

    private final DocumentManager documents;
    private final LLMService llmService;
    private final ClassificationValidator validator;
    private final ProjectProfileLoader profileLoader;
    private final ClassificationResponseReconciler responseReconciler;

    @Value("${xls2llm.cli.input:}")
    private String inputPath;

    @Value("${xls2llm.cli.output:}")
    private String outputPath;

    @Value("${xls2llm.cli.start-line:1}")
    private Integer startLine;

    @Value("${xls2llm.cli.row-limit:}")
    private String rowLimitOption;

    public CommandLineRunnerV1(
            DocumentManager documents,
            @Qualifier("gpt-service") LLMService llmService,
            ClassificationValidator validator,
            ProjectProfileLoader profileLoader,
            ClassificationResponseReconciler responseReconciler) {
        this.documents = documents;
        this.llmService = llmService;
        this.validator = validator;
        this.profileLoader = profileLoader;
        this.responseReconciler = responseReconciler;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (inputPath == null || inputPath.isBlank()) {
            throw new IllegalArgumentException("Provide --xls2llm.cli.input=<input.xlsx>");
        }
        if (startLine == null || startLine < 1) {
            throw new IllegalArgumentException("xls2llm.cli.start-line must be a one-based positive line number");
        }

        Integer rowLimit = parseRowLimit(rowLimitOption);
        String resolvedOutput = outputPath == null || outputPath.isBlank() ? defaultOutputPath(inputPath) : outputPath;
        Instant startedAt = Instant.now();
        List<Request2LLM> requests = documents.getDocument(inputPath, startLine, rowLimit, BATCH_SIZE);
        int totalRows = requests.stream().mapToInt(request -> request.question().getNextLines().size()).sum();
        int validatedRows = 0;
        int successfulRows = 0;
        int missingResultRows = 0;
        int unexpectedResultIds = 0;
        int processedRows = 0;
        boolean wroteOutput = false;
        boolean llmUnavailable = false;

        for (Request2LLM request : requests) {
            List<CategorizationResponse> responses;
            if (llmUnavailable) {
                responses = List.of();
            } else {
                try {
                    responses = llmService.promptCategorization(request);
                } catch (RuntimeException ex) {
                    llmUnavailable = true;
                    responses = List.of();
                    System.err.println("LLM request failed; remaining rows will be marked missing: " + ex.getMessage());
                }
            }
            ClassificationResponseReconciler.Result reconciliation = responseReconciler
                    .reconcile(request.question(), responses);
            responses = reconciliation.responses();
            missingResultRows += reconciliation.missingIds().size();
            unexpectedResultIds += reconciliation.unexpectedIds().size();
            if (!reconciliation.missingIds().isEmpty() || !reconciliation.duplicateIds().isEmpty()
                    || !reconciliation.unexpectedIds().isEmpty()) {
                System.err.println("Response issue: missing=" + reconciliation.missingIds()
                        + ", duplicate=" + reconciliation.duplicateIds()
                        + ", unexpected=" + reconciliation.unexpectedIds());
            }
            Map<Long, List<ClassificationValidator.Issue>> issuesByComment = new HashMap<>();
            validatedRows += request.question().getCommentIds().size();

            for (CategorizationResponse response : responses) {
                for (CommentRow comment : response.getComments()) {
                    ClassificationValidator.ValidationResult result = validator.validate(comment);
                    if (result.valid() && !reconciliation.duplicateIds().contains(comment.getCommentId())) {
                        successfulRows++;
                    }
                    else issuesByComment.put(comment.getCommentId(), result.issues());

                    for (CategoryCol category : comment.getCategories()) {
                        if (category.getEvaluationType() == null) continue;
                        String variable = validator.variableName(category.getEvaluationType());
                        if (variable != null) {
                            category.setCode(validator.serializeCode(variable, category.getCode()));
                        }
                    }
                }
            }

            Input2xls output = Input2xls.fromCategorizationResponseList(responses, 0);
            issuesByComment.forEach((commentId, issues) -> issues.forEach(issue -> {
                if (issue.columnIndex() >= 0) {
                    output.markInvalid(Math.toIntExact(commentId), issue.columnIndex());
                } else {
                    for (int column = 0; column < validator.variableCount(); column++) {
                        output.markInvalid(Math.toIntExact(commentId), column);
                    }
                }
            }));
            reconciliation.missingIds().forEach(commentId -> output.markMissingResult(
                    Math.toIntExact(commentId), validator.variableCount()));
            reconciliation.duplicateIds().forEach(commentId -> output.markReviewStatus(
                    Math.toIntExact(commentId), validator.variableCount(), "DUPLICATE_LLM_RESULT"));

            documents.writeDocument(wroteOutput ? resolvedOutput : inputPath, resolvedOutput, output);
            wroteOutput = true;

            processedRows += request.question().getNextLines().size();
            int progressPercent = totalRows == 0 ? 100 : processedRows * 100 / totalRows;
            System.out.printf("Progress: %3d%% (%d/%d rows)%n", progressPercent, processedRows, totalRows);
        }

        ProjectProfile profile = profileLoader.getProfile();
        String successRate = validatedRows == 0 ? "N/A"
                : String.format(Locale.ROOT, "%.2f%%", successfulRows * 100.0 / validatedRows);
        Map<String, String> metadata = new LinkedHashMap<>();
        metadata.put("Profile", profile.projectId() + "@" + profile.version());
        metadata.put("Profile ID", profile.projectId());
        metadata.put("Profile version", profile.version());
        metadata.put("Model", llmService.getModelName());
        metadata.put("Rows processed", String.valueOf(validatedRows));
        metadata.put("Rows passed validation", String.valueOf(successfulRows));
        metadata.put("Rows missing LLM result", String.valueOf(missingResultRows));
        metadata.put("Unexpected LLM result IDs", String.valueOf(unexpectedResultIds));
        metadata.put("Validation success rate", successRate);
        metadata.put("Execution ID", "cli-" + startedAt.toEpochMilli());
        metadata.put("Started at (UTC)", startedAt.toString());
        documents.writeExecutionMetadata(inputPath, resolvedOutput, metadata);

        System.out.printf("Processed %d rows; %d passed validation (%s). Output: %s%n",
                validatedRows, successfulRows, successRate, resolvedOutput);
    }

    private Integer parseRowLimit(String value) {
        if (value == null || value.isBlank()) return null;
        int rowLimit = Integer.parseInt(value);
        if (rowLimit < 1) throw new IllegalArgumentException("xls2llm.cli.row-limit must be positive");
        return rowLimit;
    }

    private String defaultOutputPath(String source) {
        int extension = source.toLowerCase(Locale.ROOT).lastIndexOf(".xlsx");
        return extension < 0 ? source + "-classified.xlsx"
                : source.substring(0, extension) + "-classified.xlsx";
    }

}
