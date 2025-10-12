package es.rodrigonant.p2ai.xls2llm.model.classification;

import com.fasterxml.jackson.annotation.JsonProperty;

public class CategoryCode {
	
	/* exs.: 	00: Contra/Crítica/Ataque ao Nordeste
	 * 			01: Favorável/defesa do Nordeste
	 */
	
	private int catId;
	private String catDescription;
	
	public CategoryCode() {}

	@JsonProperty
	public int getCatId() {
		return catId;
	}
	public void setCatId(int catId) {
		this.catId = catId;
	}

	@JsonProperty
	public String getCatDescription() {
		return catDescription;
	}
	public void setCatDescription(String catDescription) {
		this.catDescription = catDescription;
	}

	@Override
	public String toString() {
		return catId + ": " + catDescription;
	}
}