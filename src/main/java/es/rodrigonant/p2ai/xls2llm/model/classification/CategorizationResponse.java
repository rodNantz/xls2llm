package es.rodrigonant.p2ai.xls2llm.model.classification;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public class CategorizationResponse {

	/* Single comment categorization output.
	 * 
	 * 		cat1	cat2	cat3
	 * 01		
	 */
	
	private List<CommentRow> comments;

	@JsonProperty("comments")
	public List<CommentRow> getComments() {
		return comments;
	}
	
	public void setComments(List<CommentRow> comments) {
		this.comments = comments;
	}
	
	@Override
	public String toString() {
		return "CategorizationResponse{" +
				"comments=" + comments +
				'}';
	}
	
}