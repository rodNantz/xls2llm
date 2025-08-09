package es.rodrigonant.p2ai.xls2llm;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;

import com.openai.models.chat.completions.ChatCompletion;
import com.openai.models.chat.completions.ChatCompletion.Choice;

import es.rodrigonant.p2ai.xls2llm.llmapi.LLMService;
import es.rodrigonant.p2ai.xls2llm.model.Question;
import es.rodrigonant.p2ai.xls2llm.model.Request2LLM;

@SpringBootTest
class OpenAITest {

	private LLMService service;
	
	@Autowired
	public OpenAITest(@Qualifier("test-service") LLMService service) {
		this.service = service;
	}
	
	@Test
	public void lmStudioSingleRowTest() {
		Question q = new Question();
		q.addRowQuestion("Quão comuns são os nomes abaixo em países como o Brasil e Portugal, do mais comum ao menos comum?"); 
		q.addNextLine("Amanda, Alice, Rodrigo, Gabriel, Lívia");
		Request2LLM req = new Request2LLM(q, null, 100);

		List<ChatCompletion> response = service.promptCompletion(req);
		System.out.println(response.get(0).toString());
		
		assertNotEquals(0, response.get(0).choices().size());
		for (Choice choice : response.get(0).choices()) {
			choice.message().content();
		}
		
		assertNotNull(response);
	}
	
}
