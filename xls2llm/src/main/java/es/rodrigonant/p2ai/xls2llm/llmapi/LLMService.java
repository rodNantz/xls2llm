package es.rodrigonant.p2ai.xls2llm.llmapi;

import com.openai.models.chat.completions.ChatCompletion;

import es.rodrigonant.p2ai.xls2llm.model.Request2LLM;

public interface LLMService {
	
	ChatCompletion promptCompletion(Request2LLM req);
	
}
