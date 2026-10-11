package es.rodrigonant.p2ai.xls2llm.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

@JsonSerialize(using = CodeSerializer.class)
public class Code {
	
	private int[] codeArr = new int[2];

	/**
	 * Construct from int (0-99 only)
	 */
	public Code(int code) {
		if (code < 0 || code > 99) {
			throw new IllegalArgumentException("Code must be between 0 and 99");
		}
		this.codeArr[0] = code / 10;
		this.codeArr[1] = code % 10;
	}

	/**
	 * Construct from String (must be two digits)
	 */
	@JsonCreator
	public Code(String code) {
		if (code == null || !code.matches("\\d{2}")) {
			throw new IllegalArgumentException("Code string must be exactly two digits (00-99): " + code);
		}
		this.codeArr[0] = code.charAt(0) - '0';
		this.codeArr[1] = code.charAt(1) - '0';
	}

	/**
	 * Static factory for clarity
	 */
	public static Code fromString(String code) {
		return new Code(code);
	}
	
	@JsonIgnore
	public int[] getCode() { 
		return codeArr; 
	}
	
	@Override
	@JsonValue
	public String toString() {
		return String.format("%d%d", codeArr[0], codeArr[1]);
	}
	
	public static Code _00() {
		return new Code(0);
	}
	
}