package es.rodrigonant.p2ai.xls2llm.model.classification;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import es.rodrigonant.p2ai.xls2llm.model.V1alt;

public class CommentRow {

	private long commentId;
	private List<CategoryCol> categories;
	
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
	
	@Override
	public String toString() {
		return "CommentRow{" +
				"commentId=" + commentId +
				", categories=" + categories +
				'}';
	}
}