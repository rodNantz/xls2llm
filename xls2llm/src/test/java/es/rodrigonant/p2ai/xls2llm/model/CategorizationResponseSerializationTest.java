package es.rodrigonant.p2ai.xls2llm.model;

import com.fasterxml.jackson.databind.ObjectMapper;

import es.rodrigonant.p2ai.xls2llm.GenericTest;
import es.rodrigonant.p2ai.xls2llm.model.classification.CategorizationResponse;
import es.rodrigonant.p2ai.xls2llm.model.classification.CommentRow;
import es.rodrigonant.p2ai.xls2llm.model.classification.EvaluationWrapper;
import es.rodrigonant.p2ai.xls2llm.model.custom.V1_1alt;
import es.rodrigonant.p2ai.xls2llm.model.custom.V1_2alt;
import es.rodrigonant.p2ai.xls2llm.model.custom.V1alt;
import es.rodrigonant.p2ai.xls2llm.model.classification.CategoryCol;

import org.junit.jupiter.api.Test;
import java.util.Arrays;

public class CategorizationResponseSerializationTest extends GenericTest {
    @Test
    public void testSerialization() throws Exception {
        CategoryCol cat1 = CategoryCol.fromV1alt(1, V1alt._00_CONTRA_CRITICA_ATAQUE);

        CategoryCol cat2 = CategoryCol.fromV1_1alt(1.1, V1_1alt._02_ECONOMICO_OU_RECURSOS_MATERIAIS);

        CategoryCol cat3 = CategoryCol.fromV1_2alt(1.2, V1_2alt._01_EXALTACAO_A_LULA_PT);

        CommentRow commentRow = new CommentRow();
        commentRow.setCommentId(1L);
        commentRow.setCategories(Arrays.asList(cat1, cat2, cat3));

        CategorizationResponse response = new CategorizationResponse();
        response.setComments(Arrays.asList(commentRow));

        ObjectMapper mapper = new ObjectMapper();
        String json = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(response);
        System.out.println(json);
    }
}
