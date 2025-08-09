package es.rodrigonant.p2ai.xls2llm.llmapi.impl;

import java.util.ArrayList;
import java.util.List;

import com.openai.client.OpenAIClient;
import com.openai.models.ChatModel;
import com.openai.models.chat.completions.ChatCompletion;
import com.openai.models.chat.completions.ChatCompletionCreateParams;
import com.openai.models.responses.Response;
import com.openai.models.responses.ResponseCreateParams;

import es.rodrigonant.p2ai.xls2llm.llmapi.LLMService;
import es.rodrigonant.p2ai.xls2llm.model.Question;
import es.rodrigonant.p2ai.xls2llm.model.Request2LLM;
import jakarta.annotation.PostConstruct;

public abstract class BaseService implements LLMService {
	
	protected OpenAIClient client;
	protected String baseUrl;
	
	@PostConstruct
	public void setup() {
		baseUrl = setupProperties();
	}
	
	@Override
	public List<ChatCompletion> promptCompletion(Request2LLM req) {
		List<Question> qsts = req.question().split(req.batchSize());
		List<ChatCompletion> completions = new ArrayList<>();
		
		for (Question q : qsts) {		
			ChatCompletion cc = request(q.toString());
			completions.add(cc);
		}	
		
		return completions;
	}
	
	private ChatCompletion request(String userMessage) {
		System.out.println(this.getClass() + ": Requesting "+ baseUrl);
		System.out.println(userMessage);
		ChatCompletionCreateParams params = ChatCompletionCreateParams.builder()
		        .addUserMessage(userMessage)
		        .model(ChatModel.GPT_4_1)
		        .build();
		
		return client.chat().completions().create(params);
	}
	
	private Response responseRequest(String userMessage) {		
		System.out.println(this.getClass() + ": Requesting "+ baseUrl);
		ResponseCreateParams params = ResponseCreateParams.builder()
				.input(userMessage)
				.model(ChatModel.GPT_4_1)
		        .build();
		
		return client.responses().create(params);
	}
}
