package es.rodrigonant.p2ai.xls2llm.model;

public class Alternative {

	public static Alternative of(String string, String string2) {
		return new Alternative(string2, string2);
	}

	// attributes
	private String code;				// ex.: "00"
	private String description;			// ex.: "Contra/Crítica/Ataque ao Nordeste"
	
	
	private Alternative(String code, String description) {
		// Validate code using Code class, but store as String
		this.setCode(new Code(code).toString());
		this.setDescription(description);
	}
	
	// getters & setters	
	public String getCode() {
		return code;
	}
	public void setCode(String code) {
		// Validate before setting
		this.code = new Code(code).toString();
	}
	public void setCode(Code code) {
		this.code = code.toString();
	}
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	
}