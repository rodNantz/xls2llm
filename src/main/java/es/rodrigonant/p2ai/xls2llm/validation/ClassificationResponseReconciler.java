package es.rodrigonant.p2ai.xls2llm.validation;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Component;

import es.rodrigonant.p2ai.xls2llm.model.Question;
import es.rodrigonant.p2ai.xls2llm.model.classification.CategorizationResponse;
import es.rodrigonant.p2ai.xls2llm.model.classification.CommentRow;

/** Keeps usable results and identifies missing or duplicate IDs without failing the whole batch. */
@Component
public class ClassificationResponseReconciler {
    public Result reconcile(Question question, List<CategorizationResponse> responses) {
        Set<Long> expected = new HashSet<>(question.getCommentIds());
        Set<Long> received = new HashSet<>();
        Set<Long> duplicateIds = new HashSet<>();
        List<Long> unexpectedIds = new ArrayList<>();
        List<CategorizationResponse> acceptedResponses = new ArrayList<>();

        if (responses != null) {
            for (CategorizationResponse response : responses) {
                if (response == null || response.getComments() == null) continue;
                List<CommentRow> acceptedComments = new ArrayList<>();
                for (CommentRow comment : response.getComments()) {
                    if (comment == null) continue;
                    long commentId = comment.getCommentId();
                    if (!expected.contains(commentId)) {
                        unexpectedIds.add(commentId);
                    } else if (!received.add(commentId)) {
                        duplicateIds.add(commentId);
                    } else {
                        if (comment.getCategories() == null) comment.setCategories(List.of());
                        acceptedComments.add(comment);
                    }
                }
                CategorizationResponse acceptedResponse = new CategorizationResponse();
                acceptedResponse.setComments(acceptedComments);
                acceptedResponses.add(acceptedResponse);
            }
        }

        Set<Long> missingIds = new HashSet<>(expected);
        missingIds.removeAll(received);
        return new Result(List.copyOf(acceptedResponses), Set.copyOf(missingIds),
                Set.copyOf(duplicateIds), List.copyOf(unexpectedIds));
    }

    public record Result(
            List<CategorizationResponse> responses,
            Set<Long> missingIds,
            Set<Long> duplicateIds,
            List<Long> unexpectedIds) {}
}
