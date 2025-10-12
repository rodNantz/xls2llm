package es.rodrigonant.p2ai.xls2llm.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Question example:
 *
 * rowSystemQuestions: [ 
 * 					  "Para cada comentário, identifique se apresenta conteúdo contra (de ataque ou crítica) ao Nordeste [...]",
 * 				 	  "V1 [POS_NOR]: Nesta categoria, procura-se identificar o posicionamento dos comentaristas [...]",
 * 					  "V2 [...]"
 * 			  	 	   ],
 * mainQuestion: "Analise os seguntes tweets: ",
 * nextLines: [
 * 				"eu sou do nordeste e sou [...]", 
 *				"Brasil, campeão mundial de geração de energia [...]"
 * 			  ]
 * 
 */
public class Question {

	// cada linha do header (idx==0) tem várias colunas
	private List<String[]> rowZeroSystemQuestions; 
	private String rowZeroUserQuestion; 
	// linhas idx > 0
	private List<String> nextLines;

	
	public Question(String mainQuestion) {
		setRowZeroSystemQuestions(new ArrayList<>());
		rowZeroUserQuestion = mainQuestion;
		nextLines = new ArrayList<>();
	}

	
	public void setAddQuestion(char code, String rowZeroQuestion) {
		if (code == 'S')
			addRowZeroSystemQuestion(rowZeroQuestion);
		else
			this.rowZeroUserQuestion = rowZeroQuestion;
	}
	
	public List<String> getNextLines() {
		return nextLines;
	}

	public void setNextLines(List<String> nextLines) {
		this.nextLines = nextLines;
	}

	public void addNextLine(String rowLine) {
		this.nextLines.add(rowLine);
	}
	
	public String getRowZeroUserQuestion() {
		return rowZeroUserQuestion;
	}

	public void setRowZeroUserQuestion(String rowZeroUserQuestion) {
		this.rowZeroUserQuestion = rowZeroUserQuestion;
	}
	
	public List<String[]> getRowZeroSystemQuestions() {
		return rowZeroSystemQuestions;
	}

	public void setRowZeroSystemQuestions(List<String[]> rowQuestion) {
		this.rowZeroSystemQuestions = rowQuestion;
	}
	
	public void addRowZeroSystemQuestion(String... questions) {
		this.rowZeroSystemQuestions.add(questions);
	}
	
	public List<Question> split(int batchSize){
		List<Question> qsts = new ArrayList<Question>();
		if (nextLines != null) {
			int tL = 0;
			int bL = 0;
			Question q = null;
			for (String nLine: nextLines) {
				if (bL == 0) {
					q = new Question(this.rowZeroUserQuestion);
					q.setRowZeroSystemQuestions(rowZeroSystemQuestions);
				}
				q.nextLines.add(nLine);
				bL++;
				tL++;
				if (bL == batchSize || tL == nextLines.size()) {
					bL = 0;
					qsts.add(q);
				}
			}
		} else {
			qsts.add(this);
		}
		
		return qsts;
	}
	
	@Override
	public String toString() {
		return toString(true);
	}	
	
	public String toString(boolean includeMainQst) {
		return toString(includeMainQst, null);
	}
	
	public String toString(boolean includeMainQst, Integer lineLimit) {
		StringBuilder sb = new StringBuilder();
		for (String[] qLine: rowZeroSystemQuestions) {
			for (String col : qLine) {
				sb.append(col);
				sb.append("\n");
			}
			sb.append("\n");
		}
		
		if (includeMainQst) {
			sb.append(rowZeroUserQuestion);
		}
		sb.append("\n\n");
		
		int i = 1;
		if (nextLines != null) {
			for (String nLine: nextLines) {
				if (lineLimit != null && i > lineLimit)
					break;
				sb.append(nLine);
				sb.append("\n\n");
				i++;
			}
		}
		
		return sb.toString();
	}

}
