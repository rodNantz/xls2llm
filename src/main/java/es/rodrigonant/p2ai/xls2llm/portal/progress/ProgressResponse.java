package es.rodrigonant.p2ai.xls2llm.portal.progress;

public class ProgressResponse {

    private final int current;
    private final int total;
    private final int percentage;
    private final String status;
    private final String error;
    private final int lastCompletedLine;
    private final int nextLine;
    private final int secondPassCurrent;
    private final int secondPassTotal;
    private final int secondPassPercentage;

    public ProgressResponse(int current, int total, int percentage, String status, String error,
            int lastCompletedLine, int nextLine, int secondPassCurrent, int secondPassTotal,
            int secondPassPercentage) {
        this.current = current;
        this.total = total;
        this.percentage = percentage;
        this.status = status;
        this.error = error;
        this.lastCompletedLine = lastCompletedLine;
        this.nextLine = nextLine;
        this.secondPassCurrent = secondPassCurrent;
        this.secondPassTotal = secondPassTotal;
        this.secondPassPercentage = secondPassPercentage;
    }

    public int getCurrent() {
        return current;
    }

    public int getTotal() {
        return total;
    }

    public int getPercentage() {
        return percentage;
    }
    public String getStatus() { return status; }
    public String getError() { return error; }
    public int getLastCompletedLine() { return lastCompletedLine; }
    public int getNextLine() { return nextLine; }
    public int getSecondPassCurrent() { return secondPassCurrent; }
    public int getSecondPassTotal() { return secondPassTotal; }
    public int getSecondPassPercentage() { return secondPassPercentage; }

}
