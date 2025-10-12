package es.rodrigonant.p2ai.xls2llm.model.classification;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import es.rodrigonant.p2ai.xls2llm.model.AlternativeI;
import es.rodrigonant.p2ai.xls2llm.model.Category;

public class CategoryCol {

	@JsonProperty
	private long id;				// "V1 [POS_NOR]"
	@JsonIgnore
	private AlternativeI evaluation;
										// ex.: 00
										/* exs.: 	00: Contra/Crítica/Ataque ao Nordeste
										 * 			01: Favorável/defesa do Nordeste
										 */
	// getters & setters
	@JsonIgnore
	public long getCategory() {
		return id;
	}
	public void setCategory(long category) {
		this.id = category;
	}
	
	@JsonProperty("evaluation")
	public AlternativeI getEvaluation() {
		return evaluation;
	}
	public void setEvaluation(AlternativeI evaluation) {
		this.evaluation = evaluation;
	}

	@Override
	public String toString() {
		return "Category{" +
//				"categoryId=" + categoryId +
				"category='" + id + '\'' +
				", evaluation=" + evaluationString() +
				'}';
	}
	
	@JsonIgnore
	public String evaluationString() {
		if (evaluation != null) {
			return String.format("> %s" , evaluation.getCode());
		} else {
			return "null";
		}
	}
}