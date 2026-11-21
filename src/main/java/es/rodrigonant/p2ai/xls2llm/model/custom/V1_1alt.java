package es.rodrigonant.p2ai.xls2llm.model.custom;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import es.rodrigonant.p2ai.xls2llm.model.AlternativeI;
import es.rodrigonant.p2ai.xls2llm.model.Code;

@JsonFormat(shape = JsonFormat.Shape.OBJECT)
public enum V1_1alt implements AlternativeI {
	@JsonProperty("0: Comportamentos estereotipados/reducionistas")
	_00_COMPORTAMENTOS_ESTEREOTIPADOS_REDUCIONISTAS("0", "Comportamentos estereotipados/reducionistas"),
	@JsonProperty("2: Econômico ou recursos materiais")
	_02_ECONOMICO_OU_RECURSOS_MATERIAIS("2", "Econômico ou recursos materiais");
	
	public String code;
	public String description;
	
	V1_1alt(String code, String description) {
		// Validate code using Code class, but store as String
		this.code = new Code(code).toString();
		this.description = description;
	}
	
	public String getCode() { return code; }
	public String getDescription() { return description; }
}