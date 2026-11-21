package es.rodrigonant.p2ai.xls2llm.constants;

public class ExcelConstants {

	// we onky write columns/rows greater than these initial values.
	// TODO read from properties
	
	public static final int INITIAL_LINE = 1; // header
	public static final int CONTENT_INITIAL_LINE = 3;	// 1st tweet
	
	public static final int ID_COL = 1;		 // col B
	public static final int INITIAL_COL = 2; // col C
	public static final int CONTENT_INITIAL_COL = 3;	// 1st tweet
	
	public static final int FINAL_COL = 8;	 // col I
	
}
