package es.rodrigonant.p2ai.xls2llm.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

class QuestionCommentIdTest {

	@Test
	void promptIncludesStableIdsAndSplitKeepsTheirMapping() {
		Question question = new Question("Classify the comments");
		question.setNextLines(List.of("first comment", "second comment", "third comment"));
		question.setCommentIds(List.of(12L, 18L, 27L));

		List<Question> batches = question.split(2);

		assertEquals(List.of(12L, 18L), batches.get(0).getCommentIds());
		assertEquals(List.of(27L), batches.get(1).getCommentIds());
		assertTrue(batches.get(0).toString(false).contains("\"commentId\": 12"));
		assertTrue(batches.get(0).toString(false).contains("first comment"));
	}
}
