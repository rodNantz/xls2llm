package es.rodrigonant.p2ai.xls2llm.model.classification;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import es.rodrigonant.p2ai.xls2llm.model.custom.V1alt;

public class CommentRow {

	private long commentId;
	private List<CategoryCol> categories;
	private String justification;
	private List<String> triggerSpans;
	private boolean ambiguous;
	private String reviewReason;
	
	@JsonProperty
	public long getCommentId() {
		return commentId;
	}
	public void setCommentId(long commentId) {
		this.commentId = commentId;
	}
	
	@JsonProperty
	public List<CategoryCol> getCategories() {
		return categories;
	}
	public void setCategories(List<CategoryCol> categories) {
		this.categories = categories;
	}

	@JsonProperty
	public String getJustification() { return justification; }
	public void setJustification(String justification) { this.justification = justification; }

	@JsonProperty
	public List<String> getTriggerSpans() { return triggerSpans; }
	public void setTriggerSpans(List<String> triggerSpans) { this.triggerSpans = triggerSpans; }

	@JsonProperty
	public boolean isAmbiguous() { return ambiguous; }
	public void setAmbiguous(boolean ambiguous) { this.ambiguous = ambiguous; }

	@JsonProperty
	public String getReviewReason() { return reviewReason; }
	public void setReviewReason(String reviewReason) { this.reviewReason = reviewReason; }
	
	@Override
	public String toString() {
		return "CommentRow{" +
				"commentId=" + commentId +
				", categories=" + categories +
				'}';
	}
}
