package es.rodrigonant.p2ai.xls2llm;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import com.openai.models.chat.completions.ChatCompletion;

import es.rodrigonant.p2ai.xls2llm.document.DocumentManager;
import es.rodrigonant.p2ai.xls2llm.llmapi.LLMService;
import es.rodrigonant.p2ai.xls2llm.llmapi.impl.OpenAIService;
import es.rodrigonant.p2ai.xls2llm.model.Alternative;
import es.rodrigonant.p2ai.xls2llm.model.AlternativeI;
import es.rodrigonant.p2ai.xls2llm.model.Input2xls;
import es.rodrigonant.p2ai.xls2llm.model.Question;
import es.rodrigonant.p2ai.xls2llm.model.Request2LLM;
import es.rodrigonant.p2ai.xls2llm.model.classification.CategorizationResponse;
import es.rodrigonant.p2ai.xls2llm.model.classification.CategoryCol;
import es.rodrigonant.p2ai.xls2llm.model.classification.CommentRow;
import es.rodrigonant.p2ai.xls2llm.model.classification.EvaluationWrapper;
import es.rodrigonant.p2ai.xls2llm.model.custom.V1_1alt;
import es.rodrigonant.p2ai.xls2llm.model.custom.V1alt;

class DocumentManagerTest extends GenericTest {

	@Autowired
	DocumentManager dr;
	String xmlFilePath = "xls/test1.xlsx";
	String xmlChangeFilePath = "src/test/resources/xls/test1-copy-doctest.xlsx";
	
	@Test
	void xlsDocTest() {
		Request2LLM req = dr.getDocument(xmlFilePath, 10);
		assertNotNull(req);
		
		List<String[]> sysQst = req.question().getRowZeroSystemQuestions();
		String mQst = req.question().getRowZeroUserQuestion();
		List<String> nxtQsts = req.question().getNextLines();
				
		assertNotNull(mQst);
		System.out.println("mQst: "+ mQst);
		
		assertNotNull(sysQst);
		System.out.println("mQst.get(0): "+ Arrays.asList(sysQst.get(0)));
		assertNotNull(sysQst.get(0));
		
		System.out.println("nxtQsts: "+ nxtQsts);
		assertNotNull(nxtQsts);
	}

	@Test
	void xlsWriterTest() {
		Request2LLM req = dr.getDocument(xmlFilePath, 10);
		
		// write to xlsx
		List<CategorizationResponse> responses = createMockResponses();

		Input2xls input = new Input2xls();
		int row = 0;
		for (CategorizationResponse response : responses) {
			for (CommentRow comment : response.getComments()) {
				long comId = comment.getCommentId();
				int col = 0;
				for (CategoryCol category : comment.getCategories()) {
					String value = String.format("%s | %s", category.getCode(), 
							category.toString());
					System.out.println("Writing row "+ comId +": row "+ row +", col "+ col +": "+ value);
					// código para célula
					input.putContent(row, col, value);
					col++;
				}
				row++;
			}
		}
		dr.writeDocument(xmlFilePath, xmlChangeFilePath, input);
		
		File wFile = new File(xmlChangeFilePath);
        assertTrue(wFile.exists());
        
        try {
			Runtime.getRuntime().exec("explorer.exe /SELECT,\"" + wFile.getAbsolutePath() + "\"");
			System.out.println("Opened file: " + wFile.getAbsolutePath());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
	}
	
	
	private List<CategorizationResponse> createMockResponses() {
		// Create Category objects
		CategoryCol cat1 = CategoryCol.fromV1alt(1, V1alt._00_CONTRA_CRITICA_ATAQUE);

		CategoryCol cat2 = CategoryCol.fromV1_1alt(1.1, V1_1alt._02_ECONOMICO_OU_RECURSOS_MATERIAIS);

		// Add categories to a list
		List<CategoryCol> categories = new ArrayList<>();
		categories.add(cat1);
		categories.add(cat2);

		// Example usage of code/description
		System.out.println("cat1 eval code: " + cat1.toString());
//		System.out.println("cat1 eval description: " + cat1.getEvaluation().getDescription());
		System.out.println("cat2 eval code: " + cat2.toString());
//		System.out.println("cat2 eval code: " + cat2.getEvaluation().getCode());
//		System.out.println("cat2 eval description: " + cat2.getEvaluation().getDescription());

		// Create a CommentRow
		CommentRow commentRow = new CommentRow();
		commentRow.setCommentId(1L);
		commentRow.setCategories(categories);
		
		// Create a CommentRow
		CommentRow commentRow2 = new CommentRow();
		commentRow2.setCommentId(2L);
		commentRow2.setCategories(categories);

		// Add CommentRow to a list
		List<CommentRow> commentRows = new ArrayList<>();
		commentRows.add(commentRow);

		// Create CategorizationResponse and set comments
		CategorizationResponse response = new CategorizationResponse();
		response.setComments(commentRows);
		
		List<CategorizationResponse> responses = new ArrayList<>();
		responses.add(response);
		
		return responses;
	}
}