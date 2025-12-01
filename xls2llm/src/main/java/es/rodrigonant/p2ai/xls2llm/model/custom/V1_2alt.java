package es.rodrigonant.p2ai.xls2llm.model.custom;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import es.rodrigonant.p2ai.xls2llm.model.AlternativeI;
import es.rodrigonant.p2ai.xls2llm.model.Code;

@JsonFormat(shape = JsonFormat.Shape.OBJECT)
public enum V1_2alt implements AlternativeI {
	@JsonProperty("00: Orgulho pelo Nordeste")
	_00_ORGULHO_PELO_NORDESTE("00", "Orgulho pelo Nordeste"),
	@JsonProperty("01: Exaltação a Lula/PT")
	_01_EXALTACAO_A_LULA_PT("01", "Exaltação a Lula/PT");
	
	public String code;
	public String description;
	
	V1_2alt(String code, String description) {
		// Validate code using Code class, but store as String
		this.code = new Code(code).toString();
		this.description = description;
	}
	
	public String getCode() { return code; }
	public String getDescription() { return description; }
}