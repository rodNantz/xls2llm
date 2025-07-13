package es.rodrigonant.p2ai.xls2llm.llmapi.impl;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.models.ChatModel;
import com.openai.models.chat.completions.ChatCompletion;
import com.openai.models.chat.completions.ChatCompletionCreateParams;
import com.openai.models.responses.Response;
import com.openai.models.responses.ResponseCreateParams;

import es.rodrigonant.p2ai.xls2llm.model.Request2LLM;
import jakarta.annotation.PostConstruct;

@Service
public class OpenAIService implements es.rodrigonant.p2ai.xls2llm.llmapi.LLMService {

	OpenAIClient client;
	
	@Value( "${llm.openai.key}" )
	private String apiKey;
	
	@Value( "${llm.openai.org}" )
	protected String orgId;
	
	@Value( "${llm.openai.project}" )
	protected String projId;
	
	@Value( "${llm.openai.url}" )
	protected String baseUrl;
	
	@PostConstruct
	public void setup() {
		client = OpenAIOkHttpClient.builder()
			    .fromEnv()
			    .apiKey(apiKey)
			    .organization(orgId)
			    .project(projId)
			    .baseUrl(baseUrl)
			    .build();
	}

	@Override
	public ChatCompletion promptCompletion(Request2LLM req) {
		ChatCompletionCreateParams params = ChatCompletionCreateParams.builder()
		        .addUserMessage(req.question().toString())
		        .model(ChatModel.GPT_4_1)
		        .build();
		
		ChatCompletion completion = client.chat().completions().create(params);
		return completion;
	}
	
}
