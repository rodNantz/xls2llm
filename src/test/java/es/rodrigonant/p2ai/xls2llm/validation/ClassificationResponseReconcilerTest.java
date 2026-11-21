package es.rodrigonant.p2ai.xls2llm.validation;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

import es.rodrigonant.p2ai.xls2llm.model.Question;
import es.rodrigonant.p2ai.xls2llm.model.classification.CategorizationResponse;
import es.rodrigonant.p2ai.xls2llm.model.classification.CommentRow;

class ClassificationResponseReconcilerTest {
    @Test
    void keepsReceivedRowsAndReportsMissingDuplicateAndUnexpectedIds() {
        Question question = new Question("Classify");
        question.setCommentIds(List.of(10L, 11L, 12L));

        CommentRow first = new CommentRow();
        first.setCommentId(10L);
        CommentRow duplicate = new CommentRow();
        duplicate.setCommentId(10L);
        CommentRow unexpected = new CommentRow();
        unexpected.setCommentId(99L);
        CategorizationResponse input = new CategorizationResponse();
        input.setComments(List.of(first, duplicate, unexpected));

        ClassificationResponseReconciler.Result result = new ClassificationResponseReconciler()
                .reconcile(question, List.of(input));

        assertEquals(List.of(10L), result.responses().get(0).getComments().stream()
                .map(CommentRow::getCommentId).toList());
        assertEquals(java.util.Set.of(11L, 12L), result.missingIds());
        assertEquals(java.util.Set.of(10L), result.duplicateIds());
        assertEquals(List.of(99L), result.unexpectedIds());
    }

    @Test
    void treatsNullBatchAsAllRowsMissingInsteadOfAborting() {
        Question question = new Question("Classify");
        question.setCommentIds(List.of(4L, 5L));

        ClassificationResponseReconciler.Result result = new ClassificationResponseReconciler()
                .reconcile(question, null);

        assertEquals(java.util.Set.of(4L, 5L), result.missingIds());
        assertEquals(List.of(), result.responses());
    }
}
