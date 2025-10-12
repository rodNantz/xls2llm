package es.rodrigonant.p2ai.xls2llm.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonFormat(shape = JsonFormat.Shape.OBJECT)
public enum V1alt implements AlternativeI {
	@JsonProperty("00: Contra/Crítica/Ataque ao Nordeste")
	_00_CONTRA_CRITICA_ATAQUE("00", "Contra/Crítica/Ataque ao Nordeste"),
	@JsonProperty("01: Favorável/defesa do Nordeste")
	_01_FAVORAVEL_DEFESA("01", "Favorável/defesa do Nordeste");
	
	public String code;
	public String description;
	
	V1alt(String code, String description) {
		this.code = code;
		this.description = description;
	}
	
	public String getCode() { return code; }
	public String getDescription() { return description; }
}
