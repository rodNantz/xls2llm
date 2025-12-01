package es.rodrigonant.p2ai.xls2llm.model.custom;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import es.rodrigonant.p2ai.xls2llm.model.AlternativeI;
import es.rodrigonant.p2ai.xls2llm.model.Code;

@JsonFormat(shape = JsonFormat.Shape.OBJECT)
public enum V2alt implements AlternativeI {
	
	@JsonProperty("00: Contra/Crítica/Ataque ao Nordeste")
	_00_CONTRA_CRITICA_ATAQUE("00", "Contra/Crítica/Ataque ao Nordeste"),
	@JsonProperty("01: Favorável/defesa do Nordeste")
	_01_FAVORAVEL_DEFESA("01", "Favorável/defesa do Nordeste");
	
	public String code;
	public String description;
	
	V2alt(String code, String description) {
		// Validate code using Code class, but store as String
		this.code = new Code(code).toString();
		this.description = description;
	}
	
	public String getCode() { return code; }
	public String getDescription() { return description; }
}
