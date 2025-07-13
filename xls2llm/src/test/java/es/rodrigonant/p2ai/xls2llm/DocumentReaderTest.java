package es.rodrigonant.p2ai.xls2llm;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.openai.models.chat.completions.ChatCompletion;

import es.rodrigonant.p2ai.xls2llm.document.DocumentReader;
import es.rodrigonant.p2ai.xls2llm.llmapi.LLMService;
import es.rodrigonant.p2ai.xls2llm.llmapi.impl.OpenAIService;
import es.rodrigonant.p2ai.xls2llm.model.Question;
import es.rodrigonant.p2ai.xls2llm.model.Request2LLM;

@SpringBootTest
class DocumentReaderTest {

	@Autowired
	DocumentReader dr;
	String xmlFilePath = "xls/test1.xlsx";
	
	@Autowired
	LLMService service;
	
	@Test
	void xlsDocTest() {
		//File xmlFile = new File(xmlFilePath);
		Request2LLM req = dr.getDocument(xmlFilePath);
		assertNotNull(req);
		
		List<String[]> mQst = req.question().getRowQuestion();
		String[] nxtQsts = req.question().getNextLines();
		
		System.out.println(mQst);
		assertNotNull(mQst);
		assertNotNull(mQst.get(0));
		
		System.out.println(nxtQsts);
		assertNotNull(nxtQsts);
	}

}
