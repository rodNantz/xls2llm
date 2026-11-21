package es.rodrigonant.p2ai.xls2llm.model.classification;

/** System-level outcome codes written to the workbook status column. */
public enum ClassificationStatus {
    VALID("S"),
    INVALID("N"),
    REVIEW("R"),
    MISSING_LLM_RESULT("MISSING_LLM_RESULT"),
    DUPLICATE_LLM_RESULT("DUPLICATE_LLM_RESULT");

    private final String workbookCode;

    ClassificationStatus(String workbookCode) {
        this.workbookCode = workbookCode;
    }

    public String workbookCode() {
        return workbookCode;
    }

    public boolean requiresSecondPass() {
        return this == INVALID || this == REVIEW;
    }

    public static ClassificationStatus fromWorkbookCode(String code) {
        for (ClassificationStatus status : values()) {
            if (status.workbookCode.equals(code)) return status;
        }
        throw new IllegalArgumentException("Unknown classification status: " + code);
    }
}
