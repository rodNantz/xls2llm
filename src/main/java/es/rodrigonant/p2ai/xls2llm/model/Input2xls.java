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
import es.rodrigonant.p2ai.xls2llm.model.classification.ClassificationStatus;

public class Input2xls {

	Dictionary<Integer,Dictionary<Integer,String>> content = new Hashtable<>();
	private final Set<String> invalidCells = new HashSet<>();
	private final Set<String> reviewCells = new HashSet<>();
	private boolean hasMissingResults;
	private boolean hasValidationStatuses;
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

	public void markForReview(int row, int variableCount) {
		for (int column = 0; column <= variableCount + 1; column++) {
			if (content.get(row) == null || content.get(row).get(column) == null) putContent(row, column, "");
			reviewCells.add(row + ":" + column);
		}
	}

	public void markValidationStatus(int row, int variableCount, String status, String reason) {
		markValidationStatus(row, variableCount, ClassificationStatus.fromWorkbookCode(status), reason);
	}

	public void markValidationStatus(int row, int variableCount, ClassificationStatus status, String reason) {
		hasValidationStatuses = true;
		putContent(row, variableCount, status.workbookCode());
        if (status == ClassificationStatus.INVALID) {
            markInvalid(row, variableCount);
            putContent(row, variableCount + 1, reason == null || reason.isBlank() ? "Falha sem detalhe" : reason);
        } else if (status == ClassificationStatus.REVIEW) {
            markForReview(row, variableCount);
            putContent(row, variableCount + 1, reason == null || reason.isBlank() ? "Revisão necessária" : reason);
        } else {
            // Replace stale first-pass diagnostics (or record a successful retake).
            putContent(row, variableCount + 1, reason == null ? "" : reason);
        }
	}

	public void markValidationStatus(int row, int variableCount, boolean valid) {
		markValidationStatus(row, variableCount, valid ? ClassificationStatus.VALID : ClassificationStatus.INVALID,
				valid ? "" : "Falha na validação.");
	}

	public boolean isInvalid(int row, int column) { return invalidCells.contains(row + ":" + column); }
	public boolean isForReview(int row, int column) { return reviewCells.contains(row + ":" + column); }

	public void markMissingResult(int row, int variableCount) {
		markReviewStatus(row, variableCount, ClassificationStatus.MISSING_LLM_RESULT,
				"O LLM não retornou resultado para este comentário.");
	}

	public void markReviewStatus(int row, int variableCount, String status) {
		markReviewStatus(row, variableCount, status, status);
	}

	public void markReviewStatus(int row, int variableCount, String status, String reason) {
		markReviewStatus(row, variableCount, ClassificationStatus.fromWorkbookCode(status), reason);
	}

	public void markReviewStatus(int row, int variableCount, ClassificationStatus status, String reason) {
		hasValidationStatuses = true;
		for (int column = 0; column < variableCount; column++) markInvalid(row, column);
		putContent(row, variableCount, status.workbookCode());
		putContent(row, variableCount + 1, reason == null || reason.isBlank() ? status.workbookCode() : reason);
		markInvalid(row, variableCount);
		hasMissingResults = true;
	}

	public boolean hasMissingResults() { return hasMissingResults; }
	public boolean hasValidationStatuses() { return hasValidationStatuses; }
	
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
