package es.rodrigonant.p2ai.xls2llm.integration;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;

import com.openai.models.chat.completions.ChatCompletion;
import com.openai.models.chat.completions.ChatCompletion.Choice;

import es.rodrigonant.p2ai.xls2llm.GenericTest;
import es.rodrigonant.p2ai.xls2llm.document.DocumentManager;
import es.rodrigonant.p2ai.xls2llm.llmapi.LLMService;
import es.rodrigonant.p2ai.xls2llm.model.Input2xls;
import es.rodrigonant.p2ai.xls2llm.model.Request2LLM;
import es.rodrigonant.p2ai.xls2llm.model.classification.CategorizationResponse;
import es.rodrigonant.p2ai.xls2llm.model.classification.CommentRow;
import es.rodrigonant.p2ai.xls2llm.model.classification.CategoryCol;

public class E2ETest extends GenericTest {

	@Autowired
	DocumentManager dr;
	String xmlFilePath = 	   "xls/test1-simple.xlsx";
	String xmlChangeFilePath = "src/test/resources/xls/test1-simple-copy.xlsx";
	
	private LLMService llmService;
	
	@Autowired
	public E2ETest(@Qualifier("gpt-service") LLMService service) {
		this.llmService = service;
	}
	

	@Test
	public void e2eTest() {
		int limit = 10;
		Request2LLM req = dr.getDocument(xmlFilePath, limit);
		List<String[]> sysQsts = req.question().getRowZeroSystemQuestions();
		String mQst = req.question().getRowZeroUserQuestion();
		List<String> nxtQsts = req.question().getNextLines();
		System.out.println("mQst: "+ mQst);
		System.out.println("sysQsts.get(0): "+ Arrays.asList(sysQsts.get(0)) +" \n ... get("+ (sysQsts.size()-1) +"): "
										 + Arrays.asList(sysQsts.get(sysQsts.size()-1)));
		System.out.println("nxtQsts: "+ nxtQsts);
		// call
		List<CategorizationResponse> responses = llmService.promptCategorization(req);
		System.out.println(responses);
		// write to xlsx
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
		
}