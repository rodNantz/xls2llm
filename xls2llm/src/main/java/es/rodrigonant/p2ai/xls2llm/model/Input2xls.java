package es.rodrigonant.p2ai.xls2llm.model;

import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;

import es.rodrigonant.p2ai.xls2llm.model.classification.CategorizationResponse;
import es.rodrigonant.p2ai.xls2llm.model.classification.CategoryCol;
import es.rodrigonant.p2ai.xls2llm.model.classification.CommentRow;

public class Input2xls {

	// content indexes ALWAYS consider the offsets above.
	Dictionary<Integer,Dictionary<Integer,String>> content = new Hashtable<>();
	
	public Enumeration<Integer> getRowIndexes() {
		return content.keys();		
	}
	
	public Enumeration<Integer> getColIndexes(int l) {
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
	
	public static Input2xls fromCategorizationResponseList(List<CategorizationResponse> responses) {
		Input2xls input = new Input2xls();
        int rowIdx = 0;
        for (CategorizationResponse response : responses) {
            for (CommentRow comment : response.getComments()) {
            	long id = comment.getCommentId();
                int colIdx = 0;
                for (CategoryCol category : comment.getCategories()) {
                    String value = String.format("%s -> %s", 
                    		category.getCode(),
                    		category.toString());
                    System.out.println(""+ id + ": " + value);
                    input.putContent(rowIdx, colIdx, value);
                    colIdx++;
                }
                rowIdx++;
            }
        }
        
        return input;
	}
	
}
