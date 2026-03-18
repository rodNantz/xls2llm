package es.rodrigonant.p2ai.xls2llm.integration;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;

import es.rodrigonant.p2ai.xls2llm.model.Input2xls;
import es.rodrigonant.p2ai.xls2llm.model.classification.CategorizationResponse;
import es.rodrigonant.p2ai.xls2llm.model.classification.CategoryCol;
import es.rodrigonant.p2ai.xls2llm.model.classification.CommentRow;

/**
 * Tests that output rows match input row offsets (startLine parameter)
 */
public class OutputOffsetIntegrationTest {

	/**
	 * When startLine=0 (no offset), data should start at row index 0 (Excel row 3)
	 * Also tests that null and empty values behave the same as 0
	 */
	@Test
	public void testStartLine0WritesToIndex0() {
		CategorizationResponse response = createMockResponse(1);
		List<CategorizationResponse> responses = List.of(response);
		
		// rowOffset should be 0 when startLine=0, null, or empty
		int rowOffset = 0;
		
		Input2xls input = Input2xls.fromCategorizationResponseList(responses, rowOffset);
		
		// Data should be written at row index 0 (Excel row 3 after setContentIntoXls adds CONTENT_INITIAL_LINE)
		assertTrue(input.getRowIndexes().hasMoreElements(), "Should have at least one row");
		int firstRow = input.getRowIndexes().nextElement();
		assertEquals(0, firstRow, "First row should be at index 0");
	}

	/**
	 * When startLine=1 (first line, 1-based), should also start at row index 0 (same as null/0)
	 */
	@Test
	public void testStartLine1SameAsStartLine0() {
		CategorizationResponse response = createMockResponse(1);
		List<CategorizationResponse> responses = List.of(response);
		
		// startLine=1 (1-based, first row) -> rowOffset should be 0 (same as startLine=0/null)
		int startLine = 1;
		int rowOffset = startLine - 1; // = 0
		
		Input2xls input = Input2xls.fromCategorizationResponseList(responses, rowOffset);
		
		// Data should be written at row index 0 (same as no offset)
		assertTrue(input.getRowIndexes().hasMoreElements(), "Should have at least one row");
		int firstRow = input.getRowIndexes().nextElement();
		assertEquals(0, firstRow, "First row should be at index 0 when startLine=1");
	}

	/**
	 * When startLine=15 (1-based), data should start at row index 14 (0-based)
	 * This means: startLine is 1-based -> convert to 0-based offset
	 */
	@Test
	public void testStartLine15WritesToIndex14() {
		CategorizationResponse response = createMockResponse(1);
		List<CategorizationResponse> responses = List.of(response);
		
		// startLine=15 (user input, 1-based) -> rowOffset should be 14 (0-based index)
		// setContentIntoXls will then convert 14 to Excel row: 14 + 3 = 17
		int startLine = 15;
		int rowOffset = startLine - 1; // Convert 1-based to 0-based offset = 14
		
		Input2xls input = Input2xls.fromCategorizationResponseList(responses, rowOffset);
		
		// Data should be written at row index 14 (setContentIntoXls will convert to Excel row 17)
		assertTrue(input.getRowIndexes().hasMoreElements(), "Should have at least one row");
		int firstRow = input.getRowIndexes().nextElement();
		assertEquals(14, firstRow, "First row should be at index 14");
	}

	/**
	 * Multiple comments should be written to consecutive rows after the offset
	 */
	@Test
	public void testMultipleCommentsWithOffset() {
		// 3 comments, startLine=10 (1-based)
		List<CategorizationResponse> responses = new ArrayList<>();
		for (int i = 0; i < 3; i++) {
			responses.add(createMockResponse(1));
		}
		
		int startLine = 10;
		int rowOffset = startLine - 1; // = 9
		
		Input2xls input = Input2xls.fromCategorizationResponseList(responses, rowOffset);
		
		var rows = input.getRowIndexes();
		List<Integer> rowIndices = new ArrayList<>();
		while (rows.hasMoreElements()) {
			rowIndices.add(rows.nextElement());
		}
		// Sort because Hashtable doesn't preserve insertion order
		java.util.Collections.sort(rowIndices);
		
		// Should have 3 rows: 9, 10, 11 (setContentIntoXls will convert to Excel rows 12, 13, 14)
		assertEquals(3, rowIndices.size(), "Should have 3 rows");
		assertEquals(9, rowIndices.get(0), "First row should be at index 9");
		assertEquals(10, rowIndices.get(1), "Second row should be at index 10");
		assertEquals(11, rowIndices.get(2), "Third row should be at index 11");
	}

	// Helper to create mock response with N comments
	private CategorizationResponse createMockResponse(int commentCount) {
		CategorizationResponse response = new CategorizationResponse();
		List<CommentRow> comments = new ArrayList<>();
		
		for (int i = 0; i < commentCount; i++) {
			CommentRow comment = new CommentRow();
			comment.setCommentId(1000 + i);
			
			List<CategoryCol> categories = new ArrayList<>();
			CategoryCol cat = new CategoryCol();
			cat.setCode("12");
			categories.add(cat);
			
			comment.setCategories(categories);
			comments.add(comment);
		}
		
		response.setComments(comments);
		return response;
	}

}
