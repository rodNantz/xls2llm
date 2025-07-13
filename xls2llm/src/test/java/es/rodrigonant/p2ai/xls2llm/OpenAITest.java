package es.rodrigonant.p2ai.xls2llm;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.openai.models.chat.completions.ChatCompletion;
import com.openai.models.chat.completions.ChatCompletion.Choice;

import es.rodrigonant.p2ai.xls2llm.llmapi.LLMService;
import es.rodrigonant.p2ai.xls2llm.model.Question;
import es.rodrigonant.p2ai.xls2llm.model.Request2LLM;

@SpringBootTest
class OpenAITest {

	@Autowired
	private LLMService service;
	
	@Test
	public void lmStudioTest() {
		Question q = new Question();
		q.addRowQuestion("Olá, quão comuns são os nomes abaixo?", "Amanda", "Alice", "Rodrigo", "Gabriel", "Lívia");
		Request2LLM req = new Request2LLM(q, 100);

		ChatCompletion response = service.promptCompletion(req);
		System.out.println(response.toString());
		
		assertNotEquals(0, response.choices().size());
		for (Choice choice : response.choices()) {
			choice.message().content();
		}
		
		assertNotNull(response);
	}

}
