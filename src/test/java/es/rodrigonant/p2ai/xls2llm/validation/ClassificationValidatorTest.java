package es.rodrigonant.p2ai.xls2llm.validation;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

import es.rodrigonant.p2ai.xls2llm.model.classification.CategoryCol;
import es.rodrigonant.p2ai.xls2llm.model.classification.CategoryType;
import es.rodrigonant.p2ai.xls2llm.model.classification.CommentRow;

class ClassificationValidatorTest {
    private ClassificationValidator validator;

    @BeforeEach
    void loadProfile() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        try (InputStream input = getClass().getClassLoader()
                .getResourceAsStream("profiles/nordeste-eleicoes-2022/1.0.0.json")) {
            ProjectProfile profile = mapper.readValue(input, ProjectProfile.class);
            validator = new ClassificationValidator(new ProjectProfileLoader(profile));
        }
    }

    @Test
    void validatesDomainsAndDependenciesAndSerializesNotApplicableByVariable() {
        CommentRow valid = comment("00", "02", "999", "00", "999", "00");
        assertTrue(validator.validate(valid).valid());
        assertEquals("999", validator.serializeCode("V1.1", "99"));
        assertEquals("99", validator.serializeCode("V1", "999"));

        CommentRow inconsistent = comment("00", "02", "01", "00", "999", "00");
        ClassificationValidator.ValidationResult result = validator.validate(inconsistent);
        assertFalse(result.valid());
        assertTrue(result.issues().stream().anyMatch(issue -> issue.variable().equals("V1.2")));

        CommentRow invalidDomain = comment("00", "88", "999", "00", "999", "00");
        assertTrue(validator.validate(invalidDomain).issues().stream()
                .anyMatch(issue -> issue.message().contains("outside the configured domain")));

		CommentRow ambiguous = comment("00", "02", "999", "00", "999", "00");
		ambiguous.setAmbiguous(true);
		ambiguous.setReviewReason("Conflicting interpretations");
		assertTrue(validator.validate(ambiguous).issues().stream()
				.anyMatch(issue -> issue.message().contains("Human review required")));
    }

    private CommentRow comment(String v1, String v11, String v12, String v2, String v21, String v3) {
        CommentRow row = new CommentRow();
        row.setCommentId(5);
        row.setJustification("Reason grounded in the text");
        row.setTriggerSpans(List.of("grounded in the text"));
        List<CategoryCol> categories = new ArrayList<>();
        add(categories, CategoryType.V1ALT, v1);
        add(categories, CategoryType.V1_1ALT, v11);
        add(categories, CategoryType.V1_2ALT, v12);
        add(categories, CategoryType.V2ALT, v2);
        add(categories, CategoryType.V2_1ALT, v21);
        add(categories, CategoryType.V3ALT, v3);
        row.setCategories(categories);
        return row;
    }

    private void add(List<CategoryCol> categories, CategoryType type, String code) {
        CategoryCol category = new CategoryCol();
        category.setEvaluationType(type);
        category.setCode(code);
        categories.add(category);
    }
}
