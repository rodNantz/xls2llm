package es.rodrigonant.p2ai.xls2llm.llmapi.impl;

import java.util.ArrayList;
import java.util.List;

import com.openai.client.OpenAIClient;
import com.openai.models.ChatModel;
import com.openai.models.chat.completions.ChatCompletion;
import com.openai.models.chat.completions.ChatCompletionCreateParams;
import com.openai.models.chat.completions.ChatCompletionCreateParams.ResponseFormat;
import com.openai.models.chat.completions.StructuredChatCompletion;
import com.openai.models.chat.completions.StructuredChatCompletionCreateParams;
import com.openai.models.responses.Response;
import com.openai.models.responses.ResponseCreateParams;

import es.rodrigonant.p2ai.xls2llm.aop.Logger;
import es.rodrigonant.p2ai.xls2llm.llmapi.LLMService;
import es.rodrigonant.p2ai.xls2llm.model.Question;
import es.rodrigonant.p2ai.xls2llm.model.Request2LLM;
import es.rodrigonant.p2ai.xls2llm.model.classification.CategorizationResponse;
import jakarta.annotation.PostConstruct;

public abstract class BaseService implements LLMService {
	
	protected OpenAIClient client;
	protected String baseUrl;
	private final Logger LOG = new Logger(this.getClass());
	
	@PostConstruct
	public void setup() {
		baseUrl = setupProperties();
	}
	

	public List<ChatCompletion> promptCompletion(Request2LLM req) {
		List<Question> qsts = req.question().split(req.batchSize());
		List<ChatCompletion> completions = new ArrayList<>();			
		
		for (Question q : qsts) {		
			ChatCompletion cc = request(q.toString());
			completions.add(cc);
		}	
		
		return completions;
	}
	
	public ChatCompletion promptCompletion(Question q) {
		return request(q.toString());
	}
	
	private ChatCompletion request(String userMessage) {
		LOG.info("Requesting "+ baseUrl);
		LOG.info(userMessage);
		ChatCompletionCreateParams params = ChatCompletionCreateParams.builder()
		        .addUserMessage(userMessage)
		        .model(ChatModel.GPT_4_1)
		        .build();
		
		return client.chat().completions().create(params);
	}
	
	// returns parametrized response
	public List<CategorizationResponse> promptCategorization(Request2LLM req) {
		List<Question> qsts = req.question().split(req.batchSize());
		List<CategorizationResponse> catResponses = new ArrayList<>();		
		if (qsts.isEmpty()) {
			throw new IllegalArgumentException("No questions to process");
		}
		for (Question q : qsts) {
			catResponses = promptCategorization(q, catResponses);
		}
		LOG.info("Total categorization responses: "+ catResponses.size());
		LOG.info(catResponses.toString());
		return catResponses;
	}
	
	public List<CategorizationResponse> promptCategorization(Question q, List<CategorizationResponse> catResponses) {
		LOG.info("promptCategorization");
		
		StructuredChatCompletion<CategorizationResponse> sCC = categorizationRequest(q.toString(false), q.getRowZeroSystemQuestionsString());
		sCC.choices().stream()
	        .flatMap(choice -> choice.message().content().stream())
	        .forEach(catResponses::add);
		
		return catResponses;
	}
	
	private StructuredChatCompletion<CategorizationResponse> categorizationRequest(String userMessage, String systemMessage) {
		LOG.info("categorizationRequest "+ baseUrl);
		LOG.info("systemMessage: "+ systemMessage);
		LOG.info("userMessage:" + userMessage);
		StructuredChatCompletionCreateParams<CategorizationResponse> params = ChatCompletionCreateParams.builder()
		        .addSystemMessage(systemMessage)
				.addUserMessage(userMessage)
		        .model(ChatModel.GPT_4_1)
		        .responseFormat(CategorizationResponse.class)
		        .build();
		
		return client.chat().completions().create(params);
	}
	
}