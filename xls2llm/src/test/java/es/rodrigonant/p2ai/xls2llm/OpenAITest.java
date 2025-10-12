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
import es.rodrigonant.p2ai.xls2llm.model.classification.CategorizationResponse;
import es.rodrigonant.p2ai.xls2llm.model.classification.CategoryCol;
import es.rodrigonant.p2ai.xls2llm.model.classification.CommentRow;

@SpringBootTest
class OpenAITest {

	// test-service / gpt-service
	private LLMService service;
	
	@Autowired
	public OpenAITest(@Qualifier("test-service") LLMService service) {
		this.service = service;
	}
	
//	@Test
//	public void lmStudioSingleRowTest() {
//		Question q = new Question("Quais são os nomes mais comuns?");
//		q.addRowZeroSystemQuestion("Considerar o contexto dos nomes abaixo em países como o Brasil e Portugal, ordernar do mais comum ao menos comum"); 
//		q.addNextLine("Amanda, Alice, Rodrigo, Gabriel, Vinicius, Lívia");
//		Request2LLM req = new Request2LLM(q, null, 100);
//
//		List<ChatCompletion> response = service.promptCompletion(req);
//		System.out.println(response.get(0).toString());
//		
//		assertNotEquals(0, response.get(0).choices().size());
//		for (Choice choice : response.get(0).choices()) {
//			choice.message().content();
//		}
//		
//		assertNotNull(response);
//	}
	
	@Test
	public void lmStudioSingleRowParametrizedTest() {
		Question q = new Question("Categorize os tweets nas categorias (V1) quanto à codificação. ");
		q.addRowZeroSystemQuestion("V1 - Nesta categoria, procura-se identificar o posicionamento dos comentaristas em relação ao Nordeste: "
				+ "Código 00 - Contra//Crítica//Ataque ao Nordeste; "
				+ "Código 01 - A Favor//Defesa do Nordeste."); 
		q.addNextLine("#1 - @soinformando @MineiroInquieto @jairbolsonaro eu sou do nordeste e sou 22");
		Request2LLM req = new Request2LLM(q, null, 100);

		List<CategorizationResponse> responses = service.promptCategorization(req);
		assertNotNull(responses);
		System.out.println(responses.get(0).toString());
		
		assertNotEquals(0, responses.size());
		for (CategorizationResponse choice : responses) {
			assertNotNull(choice);
			for (CommentRow comment : choice.getComments()) {
				assertNotNull(comment);
				System.out.println(comment.getCommentId() + " -> " + comment.getCategories());
				for (CategoryCol cat : comment.getCategories()) {
					assertNotNull(cat);
					System.out.println("\t" + cat);
				}
				
			}
		}
	}
	
}
