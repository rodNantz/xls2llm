package es.rodrigonant.p2ai.xls2llm.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public interface AlternativeI {

	@JsonProperty
    public String getCode();	 
	@JsonProperty
	public String getDescription();
    
	default String name() {
		return String.format("%s: %s", getCode(), getDescription());
	}
	
}