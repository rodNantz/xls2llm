package es.rodrigonant.p2ai.xls2llm.model;

import es.rodrigonant.p2ai.xls2llm.aop.Logger;

public class ProgressBar {

	volatile int currState;
	final int total;
	private volatile String status = "RUNNING";
	private volatile String error;
	private volatile int lastCompletedLine;
	private volatile int nextLine;
	private volatile int secondPassCurrent;
	private volatile int secondPassTotal;
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
			LOG.info("Progress state reached or exceeded total; reporting as complete");
		}
		this.currState = state;
	}
	
	public int getPercent() {
		return (int) Math.min(100L, ((long) currState * 100) / total);
	}
	
	public int getCurrState() {
		return this.currState;
	}
	
	public int getTotal() {
		return this.total;
	}

	public String getStatus() { return status; }
	public String getError() { return error; }
	public int getLastCompletedLine() { return lastCompletedLine; }
	public int getNextLine() { return nextLine; }
	public int getSecondPassCurrent() { return secondPassCurrent; }
	public int getSecondPassTotal() { return secondPassTotal; }
	public int getSecondPassPercent() {
		return secondPassTotal == 0 ? 0 : (int) Math.min(100L,
				((long) secondPassCurrent * 100) / secondPassTotal);
	}

	public synchronized void addSecondPassTotal(int count) {
		if (count < 0) throw new IllegalArgumentException("Second-pass count cannot be negative");
		secondPassTotal += count;
	}

	public synchronized void completeSecondPassItems(int count) {
		if (count < 0) throw new IllegalArgumentException("Completed second-pass count cannot be negative");
		secondPassCurrent = Math.min(secondPassTotal, secondPassCurrent + count);
	}

	public void setCheckpoint(int processed, int lastCompletedLine, int nextLine) {
		setState(processed);
		this.lastCompletedLine = lastCompletedLine;
		this.nextLine = nextLine;
	}

	public void complete() { this.status = "COMPLETED"; }
	public void interrupt(String error) {
		this.error = error;
		this.status = "INTERRUPTED";
	}
	
}
