package es.rodrigonant.p2ai.xls2llm.model.handling;

public class InputException extends RuntimeException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public InputException(Exception e) {
		super(e);
	}
	
	public InputException(String s) {
		super(s);
	}
	
}
