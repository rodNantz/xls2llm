package es.rodrigonant.p2ai.xls2llm.document;

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

import es.rodrigonant.p2ai.xls2llm.GenericTest;
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
	String xmlChangeFilePath2 = "src/test/resources/xls/test1-copy-doctest2.xlsx";
	
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
		dr.writeDocument(xmlFilePath, xmlChangeFilePath2, input);
		
		File wFile = new File(xmlChangeFilePath2);
        assertTrue(wFile.exists());
        
        try {
			Runtime.getRuntime().exec("explorer.exe /SELECT,\"" + wFile.getAbsolutePath() + "\"");
			System.out.println("Opened file: " + wFile.getAbsolutePath());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
	}
	
	@Test
	void xlsNewWriterTest() {
		List<CategorizationResponse> responses = createMockResponses();
		Input2xls input = Input2xls.fromCategorizationResponseList(responses, 0);
		
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
	
    @Test
    public void batchRepro() {
        String xmlFilePath = "xls/test1-simple.xlsx";
        int rowLimit = 25;
        int batchSize = 25;

        List<Request2LLM> docs = dr.getDocument(xmlFilePath, rowLimit, batchSize);
        int total = docs.stream().mapToInt(d -> d.question().getNextLines().size()).sum();

        System.out.println("Documents returned: " + docs.size());
        for (int i = 0; i < docs.size(); i++) {
            System.out.println("  doc[" + i + "] lines: " + docs.get(i).question().getNextLines().size());
        }
        System.out.println("Total lines: " + total);

        assertEquals(rowLimit, total, "Total rows read should equal requested rowLimit");
    }

    @Test
    public void testStartLineOnly() {
        String xmlFilePath = "xls/test1-simple.xlsx";
        Integer startLine = 5;
        Integer rowLimit = null;
        int batchSize = 10;

        List<Request2LLM> docs = dr.getDocument(xmlFilePath, startLine, rowLimit, batchSize);
        int total = docs.stream().mapToInt(d -> d.question().getNextLines().size()).sum();

        System.out.println("testStartLineOnly - startLine: " + startLine + ", rowLimit: " + rowLimit);
        System.out.println("Documents returned: " + docs.size());
        for (int i = 0; i < docs.size(); i++) {
            System.out.println("  doc[" + i + "] lines: " + docs.get(i).question().getNextLines().size());
        }
        System.out.println("Total lines: " + total);

        assertTrue(total > 0, "Should process rows starting from startLine");
        assertNotNull(docs);
        assertTrue(docs.size() > 0, "Should return at least one document");
    }

    @Test
    public void testStartLineWithRowLimit() {
        String xmlFilePath = "xls/test1-simple.xlsx";
        Integer startLine = 5;
        Integer rowLimit = 10;
        int batchSize = 10;

        List<Request2LLM> docs = dr.getDocument(xmlFilePath, startLine, rowLimit, batchSize);
        int total = docs.stream().mapToInt(d -> d.question().getNextLines().size()).sum();

        System.out.println("testStartLineWithRowLimit - startLine: " + startLine + ", rowLimit: " + rowLimit);
        System.out.println("Documents returned: " + docs.size());
        for (int i = 0; i < docs.size(); i++) {
            System.out.println("  doc[" + i + "] lines: " + docs.get(i).question().getNextLines().size());
        }
        System.out.println("Total lines: " + total);

        assertEquals(6, total, "Should process rows in specified range");
        assertNotNull(docs);
    }

    @Test
    public void testStartLineZero() {
        String xmlFilePath = "xls/test1-simple.xlsx";
        Integer startLine = 0;
        Integer rowLimit = 15;
        int batchSize = 10;

        List<Request2LLM> docs = dr.getDocument(xmlFilePath, startLine, rowLimit, batchSize);
        int total = docs.stream().mapToInt(d -> d.question().getNextLines().size()).sum();

        System.out.println("testStartLineZero - startLine: " + startLine + ", rowLimit: " + rowLimit);
        System.out.println("Documents returned: " + docs.size());
        System.out.println("Total lines: " + total);

        assertEquals(rowLimit, total, "startLine=0 should process from beginning");
    }

    @Test
    public void testStartLineNullWithRowLimit() {
        String xmlFilePath = "xls/test1-simple.xlsx";
        Integer startLine = null;
        Integer rowLimit = 20;
        int batchSize = 10;

        List<Request2LLM> docs = dr.getDocument(xmlFilePath, startLine, rowLimit, batchSize);
        int total = docs.stream().mapToInt(d -> d.question().getNextLines().size()).sum();

        System.out.println("testStartLineNullWithRowLimit - startLine: " + startLine + ", rowLimit: " + rowLimit);
        System.out.println("Documents returned: " + docs.size());
        System.out.println("Total lines: " + total);

        assertEquals(rowLimit, total, "null startLine should process from beginning with rowLimit");
    }

    @Test
    public void testStartLineAndRowLimitWithSmallBatch() {
        String xmlFilePath = "xls/test1-simple.xlsx";
        Integer startLine = 3;
        Integer rowLimit = 8;
        int batchSize = 3;

        List<Request2LLM> docs = dr.getDocument(xmlFilePath, startLine, rowLimit, batchSize);
        int total = docs.stream().mapToInt(d -> d.question().getNextLines().size()).sum();

        System.out.println("testStartLineAndRowLimitWithSmallBatch - startLine: " + startLine + ", rowLimit: " + rowLimit + ", batchSize: " + batchSize);
        System.out.println("Documents returned: " + docs.size());
        for (int i = 0; i < docs.size(); i++) {
            System.out.println("  doc[" + i + "] lines: " + docs.get(i).question().getNextLines().size());
        }
        System.out.println("Total lines: " + total);

        assertEquals(6, total, "Should respect startLine and rowLimit parameters");
        assertTrue(docs.size() >= 2, "Should create multiple batches with small batch size");
    }

    @Test
    public void testStartLineBeyondAvailable() {
        String xmlFilePath = "xls/test1-simple.xlsx";
        Integer startLine = 13200;  // Beyond the file's 13162 rows
        Integer rowLimit = 10;
        int batchSize = 10;

        List<Request2LLM> docs = dr.getDocument(xmlFilePath, startLine, rowLimit, batchSize);
        int total = docs.stream().mapToInt(d -> d.question().getNextLines().size()).sum();

        System.out.println("testStartLineBeyondAvailable - startLine: " + startLine + ", rowLimit: " + rowLimit);
        System.out.println("Documents returned: " + docs.size());
        System.out.println("Total lines: " + total);

        assertEquals(0, total, "Should return no documents when startLine is beyond available rows");
    }
    
}