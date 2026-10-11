package es.rodrigonant.p2ai.xls2llm.validation;

import java.util.List;
import java.util.Map;

import es.rodrigonant.p2ai.xls2llm.model.classification.CategoryType;

public record ProjectProfile(
        String projectId,
        String version,
        String model,
        String source,
        String instructions,
        List<Variable> variables,
        List<String> notApplicableAliases,
        Map<String, String> notApplicableOutput,
        List<Dependency> dependencies) {

    public record Variable(String name, CategoryType evaluationType, List<String> codes, boolean notApplicable) {}

    public record Dependency(
            String whenVariable,
            String whenCode,
            String requireVariable,
            Semantic requiredSemantic) {}

    public enum Semantic {
        CLASSIFIED,
        NOT_APPLICABLE
    }
}
