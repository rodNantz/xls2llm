package es.rodrigonant.p2ai.xls2llm.model;

import java.util.ArrayList;
import java.util.List;

public class Answer {

	private List<String> nextLines;
	
	public Answer() {
		nextLines = new ArrayList<>();
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
	
	public List<Answer> split(int batchSize){
		List<Answer> answers = new ArrayList<Answer>();
		if (nextLines != null) {
			int tL = 0;
			int bL = 0;	//batchLine
			Answer a = null;
			for (String nLine: nextLines) {
				if (bL == 0) {
					a = new Answer();
				}
				a.nextLines.add(nLine);
				bL++;
				tL++;
				if (bL == batchSize || tL == nextLines.size()) {
					bL = 0;
					answers.add(a);
				}
			}
		} else {
			answers.add(this);
		}
		
		return answers;
	}
	
	@Override
	public String toString() {
		return toString(null);
	}	
	
	public String toString(Integer lineLimit) {
		StringBuilder sb = new StringBuilder();		
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
