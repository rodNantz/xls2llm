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
import es.rodrigonant.p2ai.xls2llm.model.classification.CategorizationResponse;
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
        CommentRow valid = comment("0", "2", "9", "0", "9", "0");
        assertTrue(validator.validate(valid).valid());
        assertEquals("9", validator.serializeCode("V1.1", "9"));
        assertEquals("9", validator.serializeCode("V1", "9"));

        CommentRow presentWithOtherTarget = comment("0", "2", "9", "1", "0", "0");
        assertTrue(validator.validate(presentWithOtherTarget).valid());

        CommentRow inconsistent = comment("0", "2", "1", "0", "9", "0");
        ClassificationValidator.ValidationResult result = validator.validate(inconsistent);
        assertFalse(result.valid());
        assertTrue(result.issues().stream().anyMatch(issue -> issue.variable().equals("V1.2")));

        CommentRow invalidDomain = comment("0", "8", "9", "0", "9", "0");
        assertTrue(validator.validate(invalidDomain).issues().stream()
                .anyMatch(issue -> issue.message().contains("outside the configured domain")));

		CommentRow ambiguous = comment("0", "2", "9", "0", "9", "0");
		ambiguous.setAmbiguous(true);
		ambiguous.setReviewReason("Conflicting interpretations");
		ClassificationValidator.ValidationResult ambiguousResult = validator.validate(ambiguous);
		assertTrue(ambiguousResult.requiresReview());
		assertFalse(ambiguousResult.hasHardErrors());
		assertTrue(ambiguousResult.reviewReasons().stream()
				.anyMatch(issue -> issue.message().contains("Conflicting interpretations")));
	}

    @Test
    void acceptsCorrectClassificationForVemNordesteSalvaAGenteComment() {
        // @nordeste131 @jairbolsonaro VEM NORDESTEEE!!!! SALVA A GENTEEEE
        CommentRow comment = comment("1", "9", "1", "0", "9", "0");

        ClassificationValidator.ValidationResult result = validator.validate(comment);

        assertTrue(result.valid(), () -> "Expected valid classification, got issues: " + result.issues());
    }

    @Test
    void treatsMissingJustificationAndTriggerSpansAsReviewWarningsRatherThanHardErrors() {
        CommentRow comment = comment("1", "9", "1", "0", "9", "0");
        comment.setJustification(null);
        comment.setTriggerSpans(null);

        ClassificationValidator.ValidationResult result = validator.validate(comment);

        assertFalse(result.valid());
        assertFalse(result.hasHardErrors());
        assertTrue(result.requiresReview());
        assertEquals(2, result.reviewReasons().size());
    }

    @Test
    void parsesAndValidatesSingleDigitCodesInCategoryResponseJson() throws Exception {
        String json = """
                {"comments":[{"commentId":5,"categories":[
                  {"id":1,"evaluationType":"V1ALT","code":"0"},
                  {"id":1.1,"evaluationType":"V1_1ALT","code":"2"},
                  {"id":1.2,"evaluationType":"V1_2ALT","code":"9"},
                  {"id":2,"evaluationType":"V2ALT","code":"1"},
                  {"id":2.1,"evaluationType":"V2_1ALT","code":"0"},
                  {"id":3,"evaluationType":"V3ALT","code":"0"}
                ],"justification":"Supported by the comment text.","triggerSpans":[],"ambiguous":false,"reviewReason":null}]}
                """;

        CategorizationResponse response = new ObjectMapper().readValue(json, CategorizationResponse.class);
        CommentRow comment = response.getComments().get(0);

        assertEquals("1", comment.getCategories().get(3).getCode());
        assertEquals("0", comment.getCategories().get(4).getCode());
        assertTrue(validator.validate(comment).valid());
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
