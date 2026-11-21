package es.rodrigonant.p2ai.xls2llm.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

@JsonSerialize(using = CodeSerializer.class)
public class Code {
	
	private int code;

	/**
	 * Construct from int (0-9 only)
	 */
	public Code(int code) {
		if (code < 0 || code > 9) {
			throw new IllegalArgumentException("Code must be between 0 and 9");
		}
		this.code = code;
	}

	/**
	 * Construct from String (must be one digit)
	 */
	@JsonCreator
	public Code(String code) {
		if (code == null || !code.matches("\\d")) {
			throw new IllegalArgumentException("Code string must be exactly one digit (0-9): " + code);
		}
		this.code = Integer.parseInt(code);
	}

	/**
	 * Static factory for clarity
	 */
	public static Code fromString(String code) {
		return new Code(code);
	}
	
	@JsonIgnore
	public int getCode() {
		return code;
	}
	
	@Override
	@JsonValue
	public String toString() {
		return Integer.toString(code);
	}
	
	public static Code _00() {
		return new Code(0);
	}
	
}
