package es.rodrigonant.p2ai.xls2llm;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.openai.models.chat.completions.ChatCompletion;

import es.rodrigonant.p2ai.xls2llm.document.DocumentManager;
import es.rodrigonant.p2ai.xls2llm.llmapi.LLMService;
import es.rodrigonant.p2ai.xls2llm.llmapi.impl.OpenAIService;
import es.rodrigonant.p2ai.xls2llm.model.Input2xls;
import es.rodrigonant.p2ai.xls2llm.model.Question;
import es.rodrigonant.p2ai.xls2llm.model.Request2LLM;

@SpringBootTest
class DocumentManagerTest {

	@Autowired
	DocumentManager dr;
	String xmlFilePath = "xls/test1.xlsx";
	String xmlChangeFilePath = "src/test/resources/xls/test1-copy.xlsx";
	
	@Test
	void xlsDocTest() {
		Request2LLM req = dr.getDocument(xmlFilePath, 10);
		assertNotNull(req);
		
		List<String[]> mQst = req.question().getRowQuestion();
		List<String> nxtQsts = req.question().getNextLines();
				
		assertNotNull(mQst);
		System.out.println("mQst.get(0): "+ Arrays.asList(mQst.get(0)));
		assertNotNull(mQst.get(0));
		
		System.out.println("nxtQsts: "+ nxtQsts);
		assertNotNull(nxtQsts);
	}

	@Test
	void xlsWriterTest() {
		Request2LLM req = dr.getDocument(xmlFilePath, 10);
		List<String> nxtQsts = req.question().getNextLines();
		// write
		int l = 0;
		int c = 0;	// TODO iterate to other cols
		Input2xls input = new Input2xls();
		
		for (String qst : nxtQsts) {
			input.putContent(l, c, "Answer to row "+ (l+1));
			l++;
		}
		
		dr.writeDocument(xmlFilePath, xmlChangeFilePath, input);	
		//req = dr.getDocument(xmlChangeFilePath, 10);
		
//		List<String> nxtAnswers = req.answer().getNextLines();
//		assertEquals("Answer to Row 1", nxtAnswers.get(0));
	}
	
}
