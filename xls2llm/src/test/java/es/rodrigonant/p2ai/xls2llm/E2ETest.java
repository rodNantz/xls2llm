package es.rodrigonant.p2ai.xls2llm;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;

import com.openai.models.chat.completions.ChatCompletion;
import com.openai.models.chat.completions.ChatCompletion.Choice;

import es.rodrigonant.p2ai.xls2llm.document.DocumentManager;
import es.rodrigonant.p2ai.xls2llm.llmapi.LLMService;
import es.rodrigonant.p2ai.xls2llm.model.Input2xls;
import es.rodrigonant.p2ai.xls2llm.model.Request2LLM;

@SpringBootTest
public class E2ETest {

	@Autowired
	DocumentManager dr;
	String xmlFilePath = "xls/test1-simple.xlsx";
	String xmlChangeFilePath = "src/test/resources/xls/test1-simple-copy.xlsx";
	
	private LLMService llmService;
	
	@Autowired
	public E2ETest(@Qualifier("test-service") LLMService service) {
		this.llmService = service;
	}
	

	@Test
	public void e2eTest() {
		int limit = 10;
		
		Request2LLM req = dr.getDocument(xmlFilePath, limit);
		
		List<String[]> mQst = req.question().getRowQuestion();
		List<String> nxtQsts = req.question().getNextLines();
				
		System.out.println("mQst.get(0): "+ Arrays.asList(mQst.get(0)));
		
		System.out.println("nxtQsts: "+ nxtQsts);
		
		// call

		//LLMService service = factory.getLLMService();
		
		List<ChatCompletion> response = llmService.promptCompletion(req);
		System.out.println(response.get(0).toString());
		
		// write to xlsx

		int row = 0;
		int col = 0;
		for (Choice choice : response.get(0).choices()) {
			System.out.println(choice.message().content());
			Input2xls input = new Input2xls();
			input.putContent(row, col, choice.message().content().get());
			dr.writeDocument(xmlFilePath, xmlChangeFilePath, input);
		}
	}
		
}
