package es.rodrigonant.p2ai.xls2llm.model.classification;

import java.util.Map;
import com.fasterxml.jackson.annotation.JsonProperty;

public class Categorization {
	private Map<String, String> categories;
	private Map<Integer, String> comments;

	/* Single comment categorisation output.
	 * 
	 * 		cat1	cat2	cat3
	 * 01		
	 */
	

	public Categorization() {}

	@JsonProperty
	public Map<String, String> getCategories() {
		return categories;
	}
	public void setCategories(Map<String, String> categories) {
		this.categories = categories;
	}

	@JsonProperty
	public Map<Integer, String> getComments() {
		return comments;
	}
	public void setComments(Map<Integer, String> comments) {
		this.comments = comments;
	}
}