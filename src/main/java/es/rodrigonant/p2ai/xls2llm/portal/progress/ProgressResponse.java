package es.rodrigonant.p2ai.xls2llm.portal.progress;

public class ProgressResponse {

    private final int current;
    private final int total;
    private final int percentage;

    public ProgressResponse(int current, int total, int percentage) {
        this.current = current;
        this.total = total;
        this.percentage = percentage;
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

}
