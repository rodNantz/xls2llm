package es.rodrigonant.p2ai.xls2llm.model;

import es.rodrigonant.p2ai.xls2llm.aop.Logger;

public class ProgressBar {

	int currState;
	final int total;
	private final Logger LOG = new Logger(ProgressBar.class);
	
	public ProgressBar(int total) {
		if (total <= 0) {
			throw new IllegalArgumentException("Total must be greater than 0, got: " + total);
		}
		this.currState = 0;
		this.total = total;
		LOG.info("ProgressBar created with total: " + total);
	}
	
	public void setState(int state) {
		if (state < 0) {
			throw new IllegalArgumentException("State cannot be negative, got: " + state);
		}
		if (state > this.total) {
			//throw new IllegalArgumentException("State cannot exceed total (" + this.total + "), got: " + state);
			LOG.error("TODO state is greater than ttotal");
		}
		this.currState = state;
	}
	
	public int getPercent() {
		return (int) ((currState * 100) / total);
	}
	
	public int getCurrState() {
		return this.currState;
	}
	
	public int getTotal() {
		return this.total;
	}
	
}
