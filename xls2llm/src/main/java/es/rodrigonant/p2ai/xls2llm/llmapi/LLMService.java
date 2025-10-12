package es.rodrigonant.p2ai.xls2llm.llmapi;

import java.util.List;

import com.openai.models.chat.completions.ChatCompletion;

import es.rodrigonant.p2ai.xls2llm.model.Request2LLM;
import es.rodrigonant.p2ai.xls2llm.model.classification.CategorizationResponse;

public interface LLMService {
	
	List<ChatCompletion> promptCompletion(Request2LLM req);
	
	List<CategorizationResponse> promptCategorization(Request2LLM req);
	
	String setupProperties();
}
