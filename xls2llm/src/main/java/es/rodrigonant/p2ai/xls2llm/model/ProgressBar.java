package es.rodrigonant.p2ai.xls2llm.model;

import es.rodrigonant.p2ai.xls2llm.aop.Logger;

public class ProgressBar {

	int currState;
	final int total;
	private final Logger LOG = new Logger(ProgressBar.class);
	
	public ProgressBar(int total) {
		this.currState = 0;
		this.total = total;
		LOG.info("ProgressBar created with total: " + total);
	}
	
	public void setState(int state) {
		this.currState = state;
	}
	
	public int getPercent() {
		return (int) ((currState * 100) / total);
	}
	
}
