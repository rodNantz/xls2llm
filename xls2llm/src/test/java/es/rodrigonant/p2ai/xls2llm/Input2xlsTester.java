package es.rodrigonant.p2ai.xls2llm;

import java.util.Enumeration;

import org.junit.jupiter.api.Test;

import es.rodrigonant.p2ai.xls2llm.model.Input2xls;

public class Input2xlsTester extends GenericTest {
	
	@Test
	void input2xlsTest() {
		Input2xls input = new Input2xls();
		input.putContent(1, 1, "A1");
		input.putContent(1, 2, "B1");
		input.putContent(2, 1, "A2");
		input.putContent(2, 2, "B2");
		
		assert(input.getCellContent(1, 1).equals("A1"));
		assert(input.getCellContent(1, 2).equals("B1"));
		assert(input.getCellContent(2, 1).equals("A2"));
		assert(input.getCellContent(2, 2).equals("B2"));
		
		Enumeration<Integer> rows = input.getRowIndexes();
		while (rows.hasMoreElements()) {
			Integer r = rows.nextElement();
			System.out.println("Row: "+r);
			Enumeration<Integer> cols = input.getColIndexes(r);
			while (cols.hasMoreElements()) {
				Integer c = cols.nextElement();
				System.out.println("  Col: "+c+" Value: "+input.getCellContent(r, c));
			}
		}
	}
	
}
