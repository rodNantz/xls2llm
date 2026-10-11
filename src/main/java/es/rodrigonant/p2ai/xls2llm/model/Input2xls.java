package es.rodrigonant.p2ai.xls2llm.model;

import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.HashSet;
import java.util.Set;

import es.rodrigonant.p2ai.xls2llm.aop.Logger;
import es.rodrigonant.p2ai.xls2llm.model.classification.CategorizationResponse;
import es.rodrigonant.p2ai.xls2llm.model.classification.CategoryCol;
import es.rodrigonant.p2ai.xls2llm.model.classification.CommentRow;

public class Input2xls {

	Dictionary<Integer,Dictionary<Integer,String>> content = new Hashtable<>();
	private final Set<String> invalidCells = new HashSet<>();
	private boolean hasMissingResults;
	private final static Logger LOG = new Logger(Input2xls.class);
	
	public Enumeration<Integer> getRowIndexes() {
		return content.keys();		
	}
	
	public Enumeration<Integer> getColIndexes(int l) {
		return content.get(l).keys();		
	}
	
	public void putContent(int l, int c, String value) {
		Dictionary<Integer, String> line = content.get(l);
		if (line == null) {
			line = new Hashtable<>();
		}
		line.put(c, value);
		content.put(l, line);
	}
	
	public String getCellContent(int l, int c) {
		return content.get(l).get(c);
	}

	public void markInvalid(int row, int column) {
		if (content.get(row) == null || content.get(row).get(column) == null) putContent(row, column, "");
		invalidCells.add(row + ":" + column);
	}

	public void markValidationStatus(int row, int variableCount, boolean valid) {
		putContent(row, variableCount, valid ? "S" : "N");
		if (!valid) markInvalid(row, variableCount);
	}

	public boolean isInvalid(int row, int column) { return invalidCells.contains(row + ":" + column); }

	public void markMissingResult(int row, int variableCount) {
		markReviewStatus(row, variableCount, "MISSING_LLM_RESULT");
	}

	public void markReviewStatus(int row, int variableCount, String status) {
		for (int column = 0; column < variableCount; column++) markInvalid(row, column);
		putContent(row, variableCount, status);
		markInvalid(row, variableCount);
		hasMissingResults = true;
	}

	public boolean hasMissingResults() { return hasMissingResults; }
	
	public static Input2xls fromCategorizationResponseList(List<CategorizationResponse> responses, int rowOffset) {
		Input2xls input = new Input2xls();
        for (CategorizationResponse response : responses) {
            for (CommentRow comment : response.getComments()) {
             long id = comment.getCommentId();
                if (comment.getCategories() == null) continue;
                for (CategoryCol category : comment.getCategories()) {
                    String value = category.getCode();
                    LOG.debug(""+ id + ": " + value);
					int colIdx = category.getEvaluationType() == null ? 0 : category.getEvaluationType().ordinal();
                    input.putContent(Math.toIntExact(id), colIdx, value);
                }
            }
        }
        
        return input;
	}
	
}
