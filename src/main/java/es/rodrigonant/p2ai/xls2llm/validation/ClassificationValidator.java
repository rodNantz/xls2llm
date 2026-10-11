package es.rodrigonant.p2ai.xls2llm.validation;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import es.rodrigonant.p2ai.xls2llm.model.classification.CategoryCol;
import es.rodrigonant.p2ai.xls2llm.model.classification.CategoryType;
import es.rodrigonant.p2ai.xls2llm.model.classification.CommentRow;

@Component
public class ClassificationValidator {
    private final ProjectProfileLoader profileLoader;

    public ClassificationValidator(ProjectProfileLoader profileLoader) {
        this.profileLoader = profileLoader;
    }

    public ValidationResult validate(CommentRow comment) {
        ProjectProfile profile = profileLoader.getProfile();
        Map<CategoryType, ProjectProfile.Variable> byType = new EnumMap<>(CategoryType.class);
        Map<String, CategoryCol> values = new HashMap<>();
        List<Issue> issues = new ArrayList<>();
        for (ProjectProfile.Variable variable : profile.variables()) byType.put(variable.evaluationType(), variable);

        if (comment.getCategories() != null) {
            for (CategoryCol category : comment.getCategories()) {
                if (category == null || category.getEvaluationType() == null) {
                    issues.add(new Issue("", "Missing variable identity", -1));
                    continue;
                }
                ProjectProfile.Variable variable = byType.get(category.getEvaluationType());
                if (variable == null) {
                    issues.add(new Issue("", "Variable is not configured in the project profile", -1));
                    continue;
                }
                int index = profile.variables().indexOf(variable);
                if (values.putIfAbsent(variable.name(), category) != null) {
                    issues.add(new Issue(variable.name(), "Duplicate variable", index));
                }
                String code = category.getCode();
                if (code == null || !(variable.codes().contains(code)
                        || (variable.notApplicable() && profile.notApplicableAliases().contains(code)))) {
                    issues.add(new Issue(variable.name(), "Code is outside the configured domain: " + code, index));
                }
            }
        }

		if (comment.getJustification() == null || comment.getJustification().isBlank()) {
			issues.add(new Issue("", "Required justification is missing", -1));
		}
		if (comment.getTriggerSpans() == null) {
			issues.add(new Issue("", "Required triggerSpans list is missing", -1));
		}
		if (comment.isAmbiguous()) {
			issues.add(new Issue("", "Human review required: " + comment.getReviewReason(), -1));
		}

        for (ProjectProfile.Variable variable : profile.variables()) {
            if (!values.containsKey(variable.name())) {
                issues.add(new Issue(variable.name(), "Required variable is missing", profile.variables().indexOf(variable)));
            }
        }

        for (ProjectProfile.Dependency dependency : profile.dependencies()) {
            CategoryCol source = values.get(dependency.whenVariable());
            CategoryCol target = values.get(dependency.requireVariable());
            if (source == null || target == null || !matchesTrigger(profile, dependency.whenCode(), source.getCode())) continue;
            boolean isNotApplicable = profile.notApplicableAliases().contains(target.getCode());
            boolean satisfied = dependency.requiredSemantic() == ProjectProfile.Semantic.NOT_APPLICABLE
                    ? isNotApplicable
                    : !isNotApplicable && profile.variables().stream()
                            .filter(v -> v.name().equals(dependency.requireVariable()))
                            .anyMatch(v -> v.codes().contains(target.getCode()));
            if (!satisfied) {
                int index = profile.variables().stream().map(ProjectProfile.Variable::name)
                        .toList().indexOf(dependency.requireVariable());
                issues.add(new Issue(dependency.requireVariable(), "Dependency from " + dependency.whenVariable()
                        + "=" + dependency.whenCode() + " is not satisfied", index));
            }
        }

        return new ValidationResult(comment.getCommentId(), List.copyOf(issues));
    }

    public String serializeCode(String variableName, String code) {
        ProjectProfile profile = profileLoader.getProfile();
        if (profile.notApplicableAliases().contains(code)) {
            return profile.notApplicableOutput().getOrDefault(variableName, code);
        }
        return code;
    }

    public String variableName(CategoryType evaluationType) {
        return profileLoader.getProfile().variables().stream()
                .filter(variable -> variable.evaluationType() == evaluationType)
                .map(ProjectProfile.Variable::name)
                .findFirst().orElse(null);
    }

	public int variableCount() { return profileLoader.getProfile().variables().size(); }

    private boolean matchesTrigger(ProjectProfile profile, String trigger, String code) {
        if (profile.notApplicableAliases().contains(trigger)) {
            return profile.notApplicableAliases().contains(code);
        }
        return trigger.equals(code);
    }

    public record Issue(String variable, String message, int columnIndex) {}

    public record ValidationResult(long commentId, List<Issue> issues) {
        public boolean valid() { return issues.isEmpty(); }
    }
}
