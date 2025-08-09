package es.rodrigonant.p2ai.xls2llm.model;

import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;

public class Input2xls {

	// content indexes ALWAYS consider the offsets above.
	Dictionary<Integer,Dictionary<Integer,String>> content = new Hashtable<>();
	
	public Enumeration<Integer> getRowIndexes() {
		return content.keys();		
	}
	
	public Enumeration<Integer> getColIndexes(Integer l) {
		return content.get(l).keys();		
	}
	
	public void putContent(int l, int c, String value) {
		Dictionary<Integer, String> line = content.get(l);
		if (line == null) {
			line = new Hashtable<>();
		}
		line.put(c, value);
		content.put(l, line);
	}
	
	public String getCellContent(int l, int c) {
		return content.get(l).get(c);		
	}
	
}
