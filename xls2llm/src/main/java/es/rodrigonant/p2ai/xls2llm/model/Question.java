package es.rodrigonant.p2ai.xls2llm.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Question example:
 *
 * 0. rowQuestion: { 
 * 					 { "Para cada comentário, identifique se apresenta conteúdo contra (de ataque ou crítica) ao Nordeste [...]",
 * 				 	   "V1 [POS_NOR]: Nesta categoria, procura-se identificar o posicionamento dos comentaristas [...]",
 * 					   "V2 [...]"
 * 					 },
 *				 	 { "Os seguintes tweets:"
 *					 }
 * 			  	   }
 * nextLines: {
 * 				"eu sou do nordeste e sou [...]", 
 *				"Brasil, campeão mundial de geração de energia [...]"
 * 			  }
 * 
 */
public class Question {

	// cada linha do header (idx==0) tem várias colunas
	private List<String[]> rowZeroQuestion; 
	// linhas idx > 0
	private String[] nextLines;
	
	public Question() {
		rowZeroQuestion = new ArrayList<>();
	}

	
	public String[] getNextLines() {
		return nextLines;
	}

	public void setNextLines(String[] nextLines) {
		this.nextLines = nextLines;
	}

	public List<String[]> getRowQuestion() {
		return rowZeroQuestion;
	}

	public void setRowQuestion(List<String[]> rowQuestion) {
		this.rowZeroQuestion = rowQuestion;
	}
	
	public void addRowQuestion(String... questions) {
		this.rowZeroQuestion.add(questions);
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		for (String[] qLine: rowZeroQuestion) {
			for (String col : qLine) {
				sb.append(col);
				sb.append("\n\n");
			}
			sb.append("\n\n\n");
		}
		
		for (String nLine: nextLines) {
			sb.append(nLine);
			sb.append("\n\n");
		}
		
		return sb.toString();
	}
	
}
