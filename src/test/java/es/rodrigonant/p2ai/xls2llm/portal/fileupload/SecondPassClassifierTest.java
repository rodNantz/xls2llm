package es.rodrigonant.p2ai.xls2llm.portal.fileupload;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.fasterxml.jackson.databind.ObjectMapper;

import es.rodrigonant.p2ai.xls2llm.llmapi.LLMService;
import es.rodrigonant.p2ai.xls2llm.model.Question;
import es.rodrigonant.p2ai.xls2llm.model.classification.CategorizationResponse;
import es.rodrigonant.p2ai.xls2llm.model.classification.CategoryCol;
import es.rodrigonant.p2ai.xls2llm.model.classification.CategoryType;
import es.rodrigonant.p2ai.xls2llm.model.classification.ClassificationStatus;
import es.rodrigonant.p2ai.xls2llm.model.classification.CommentRow;
import es.rodrigonant.p2ai.xls2llm.validation.ClassificationResponseReconciler;
import es.rodrigonant.p2ai.xls2llm.validation.ClassificationValidator;
import es.rodrigonant.p2ai.xls2llm.validation.ProjectProfile;
import es.rodrigonant.p2ai.xls2llm.validation.ProjectProfileLoader;

class SecondPassClassifierTest {
    private LLMService llmService;
    private SecondPassClassifier secondPassClassifier;

    @BeforeEach
    void setUp() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        ProjectProfile profile;
        try (InputStream input = getClass().getClassLoader()
                .getResourceAsStream("profiles/nordeste-eleicoes-2022/1.0.0.json")) {
            profile = mapper.readValue(input, ProjectProfile.class);
        }
        ProjectProfileLoader loader = new ProjectProfileLoader(profile);
        llmService = mock(LLMService.class);
        secondPassClassifier = new SecondPassClassifier(llmService, new ClassificationValidator(loader),
                new ClassificationResponseReconciler(), loader, 10, 4);
    }

    @Test
    void retriesOnlyHardInvalidItemWithTargetedCorrectionContext() {
        Question question = question(4L, "A comment to classify");
        CommentRow first = comment(4L, "0", null, "9", "1", "3", "0");
        CommentRow corrected = comment(4L, "0", "2", "9", "1", "3", "0");
        when(llmService.promptCategorization(any(Question.class), anyList()))
                .thenReturn(List.of(response(corrected)));

        Map<Long, CommentRow> firstComments = Map.of(4L, first);
        Map<Long, ClassificationStatus> statuses = Map.of(4L, ClassificationStatus.INVALID);
        Map<Long, String> reasons = Map.of(4L, "V1.1: Required variable is missing");
        SecondPassClassifier.Result result = secondPassClassifier.run(question, firstComments, statuses, reasons);

        assertEquals(ClassificationStatus.VALID, result.finalStatuses().get(4L));
        assertEquals("2", code(result.finalComments().get(4L), CategoryType.V1_1ALT));
        assertEquals(1, result.attempted());
        assertEquals(1, result.correctedN());
        ArgumentCaptor<Question> captured = ArgumentCaptor.forClass(Question.class);
        verify(llmService).promptCategorization(captured.capture(), anyList());
        String prompt = captured.getValue().toString(false);
        assertTrue(prompt.contains("TARGETED_CORRECTION"));
        assertTrue(prompt.contains("Required variable is missing"));
    }

    @Test
    void resolvesReviewOnlyWhenIndependentSecondOpinionAgreesAndIsUnambiguous() {
        Question question = question(8L, "A comment whose original interpretation was uncertain");
        CommentRow first = comment(8L, "0", "2", "9", "1", "3", "0");
        first.setAmbiguous(true);
        first.setReviewReason("First pass was uncertain");
        CommentRow second = comment(8L, "0", "2", "9", "1", "3", "0");
        when(llmService.promptCategorization(any(Question.class), anyList()))
                .thenReturn(List.of(response(second)));

        SecondPassClassifier.Result result = secondPassClassifier.run(question, Map.of(8L, first),
                Map.of(8L, ClassificationStatus.REVIEW), Map.of(8L, "Review: first pass was uncertain"));

        assertEquals(ClassificationStatus.VALID, result.finalStatuses().get(8L));
        assertEquals(1, result.resolvedR());
        assertEquals("Retake passed", result.finalReasons().get(8L));
        ArgumentCaptor<Question> captured = ArgumentCaptor.forClass(Question.class);
        verify(llmService).promptCategorization(captured.capture(), anyList());
        String prompt = captured.getValue().toString(false);
        assertTrue(prompt.contains("INDEPENDENT_SECOND_OPINION"));
        assertFalse(prompt.contains("previousResultContext"));
    }

    @Test
    void selectsOnlyNAndRRowsAndAttemptsEachExactlyOnce() {
        Question question = new Question("Classify each comment.");
        question.addRowZeroSystemQuestion("Project codebook and schema");
        question.addNextLine(10L, "Already valid");
        question.addNextLine(11L, "Needs correction");
        question.addNextLine(12L, "Needs a second opinion");

        CommentRow alreadyValid = comment(10L, "0", "2", "9", "1", "3", "0");
        CommentRow invalid = comment(11L, "0", null, "9", "1", "3", "0");
        CommentRow review = comment(12L, "0", "2", "9", "1", "3", "0");
        review.setAmbiguous(true);
        CommentRow corrected = comment(11L, "0", "2", "9", "1", "3", "0");
        CommentRow independentOpinion = comment(12L, "0", "2", "9", "1", "3", "0");
        when(llmService.promptCategorization(any(Question.class), anyList()))
                .thenReturn(List.of(response(corrected, independentOpinion)));

        AtomicInteger discovered = new AtomicInteger();
        AtomicInteger settled = new AtomicInteger();
        SecondPassClassifier.Result result = secondPassClassifier.run(question,
                Map.of(10L, alreadyValid, 11L, invalid, 12L, review),
                Map.of(10L, ClassificationStatus.VALID, 11L, ClassificationStatus.INVALID,
                        12L, ClassificationStatus.REVIEW),
                Map.of(11L, "V1.1 is missing", 12L, "First pass ambiguous"),
                discovered::addAndGet, settled::addAndGet);

        assertEquals(List.of(10L, 11L, 12L), question.getCommentIds());
        assertEquals(2, result.attempted());
        assertEquals(2, discovered.get());
        assertEquals(2, settled.get());
        assertEquals(ClassificationStatus.VALID, result.finalStatuses().get(10L));
        assertEquals(ClassificationStatus.VALID, result.finalStatuses().get(11L));
        assertEquals(ClassificationStatus.VALID, result.finalStatuses().get(12L));

        ArgumentCaptor<Question> captured = ArgumentCaptor.forClass(Question.class);
        verify(llmService).promptCategorization(captured.capture(), anyList());
        assertEquals(List.of(11L, 12L), captured.getValue().getCommentIds());
    }

    @Test
    void preservesOriginalRowAndReportsUnderlyingSecondPassFailureCause() {
        Question question = question(14L, "A comment to classify");
        CommentRow original = comment(14L, "0", null, "9", "1", "3", "0");
        when(llmService.promptCategorization(any(Question.class), anyList()))
                .thenThrow(new RuntimeException("Request failed", new IllegalStateException("HTTP 429")));

        SecondPassClassifier.Result result = secondPassClassifier.run(question, Map.of(14L, original),
                Map.of(14L, ClassificationStatus.INVALID), Map.of(14L, "V1.1 is missing"));

        assertEquals(ClassificationStatus.INVALID, result.finalStatuses().get(14L));
        assertEquals(original, result.finalComments().get(14L));
        assertTrue(result.finalReasons().get(14L).contains("HTTP 429"));
        assertTrue(result.finalReasons().get(14L).contains("Request failed"));
        assertEquals(1, result.attempted());
    }

    private Question question(long id, String text) {
        Question question = new Question("Classify each comment.");
        question.addRowZeroSystemQuestion("Project codebook and schema");
        question.addNextLine(id, text);
        return question;
    }

    private CategorizationResponse response(CommentRow... comments) {
        CategorizationResponse response = new CategorizationResponse();
        response.setComments(List.of(comments));
        return response;
    }

    private CommentRow comment(long id, String v1, String v11, String v12, String v2, String v21, String v3) {
        CommentRow row = new CommentRow();
        row.setCommentId(id);
        List<CategoryCol> categories = new ArrayList<>();
        add(categories, CategoryType.V1ALT, v1);
        add(categories, CategoryType.V1_1ALT, v11);
        add(categories, CategoryType.V1_2ALT, v12);
        add(categories, CategoryType.V2ALT, v2);
        add(categories, CategoryType.V2_1ALT, v21);
        add(categories, CategoryType.V3ALT, v3);
        row.setCategories(categories);
        row.setJustification("Text-based justification");
        row.setTriggerSpans(List.of());
        return row;
    }

    private void add(List<CategoryCol> categories, CategoryType type, String code) {
        if (code == null) return;
        CategoryCol category = new CategoryCol();
        category.setEvaluationType(type);
        category.setCode(code);
        categories.add(category);
    }

    private String code(CommentRow comment, CategoryType type) {
        return comment.getCategories().stream().filter(category -> category.getEvaluationType() == type)
                .map(CategoryCol::getCode).findFirst().orElse(null);
    }
}
