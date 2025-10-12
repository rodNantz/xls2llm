package es.rodrigonant.p2ai.xls2llm.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

public enum Category {
	
	@JsonProperty("V1 [POS_NOR]")
	V1_POS_NOR("V1 [POS_NOR]", new AlternativeI[] {
		V1alt._00_CONTRA_CRITICA_ATAQUE,
		V1alt._01_FAVORAVEL_DEFESA
	}),
	@JsonProperty("V1.1 [AC_DOM]")
	V1_1_AC_DOM("V1.1 [AC_DOM]", new AlternativeI[] {
		V1_1alt._00_COMPORTAMENTOS_ESTEREOTIPADOS_REDUCIONISTAS,
		V1_1alt._02_ECONOMICO_OU_RECURSOS_MATERIAIS
	}),
	@JsonProperty("V1.2 [FD_DOM]")
	V1_2_FD_DOM("V1.2 [FD_DOM]", new AlternativeI[] {
		V1_2alt._00_ORGULHO_PELO_NORDESTE,
		V1_2alt._01_EXALTACAO_A_LULA_PT
	});

	public String name;
	public AlternativeI[] alternatives;

	Category(String name, AlternativeI[] objects) {
		this.name = name;
		this.alternatives = objects;
	}

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

	@JsonFormat(shape = JsonFormat.Shape.OBJECT)
	public enum V1_1alt implements AlternativeI {
		@JsonProperty("00: Comportamentos estereotipados/reducionistas")
		_00_COMPORTAMENTOS_ESTEREOTIPADOS_REDUCIONISTAS("00", "Comportamentos estereotipados/reducionistas"),
		@JsonProperty("02: Econômico ou recursos materiais")
		_02_ECONOMICO_OU_RECURSOS_MATERIAIS("02", "Econômico ou recursos materiais");
		public String code;
		public String description;
		V1_1alt(String code, String description) {
			this.code = code;
			this.description = description;
		}
		public String getCode() { return code; }
		public String getDescription() { return description; }
	}

	@JsonFormat(shape = JsonFormat.Shape.OBJECT)
	public enum V1_2alt implements AlternativeI {
		@JsonProperty("00: Orgulho pelo Nordeste")
		_00_ORGULHO_PELO_NORDESTE("00", "Orgulho pelo Nordeste"),
		@JsonProperty("01: Exaltação a Lula/PT")
		_01_EXALTACAO_A_LULA_PT("01", "Exaltação a Lula/PT");
		public String code;
		public String description;
		V1_2alt(String code, String description) {
			this.code = code;
			this.description = description;
		}
		public String getCode() { return code; }
		public String getDescription() { return description; }
	}
}