package es.rodrigonant.p2ai.xls2llm.llmapi.impl;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.openai.client.OpenAIClient;
import com.openai.models.ChatModel;
import com.openai.models.chat.completions.ChatCompletion;
import com.openai.models.chat.completions.ChatCompletionCreateParams;
import com.openai.models.chat.completions.ChatCompletionCreateParams.ResponseFormat;
import com.openai.models.chat.completions.StructuredChatCompletion;
import com.openai.models.chat.completions.StructuredChatCompletionCreateParams;
import com.openai.models.responses.Response;
import com.openai.models.responses.ResponseCreateParams;

import es.rodrigonant.p2ai.xls2llm.llmapi.LLMService;
import es.rodrigonant.p2ai.xls2llm.model.Question;
import es.rodrigonant.p2ai.xls2llm.model.Request2LLM;
import es.rodrigonant.p2ai.xls2llm.model.classification.CategorizationResponse;
import jakarta.annotation.PostConstruct;

public abstract class BaseService implements LLMService {
	
	protected OpenAIClient client;
	protected String baseUrl;
	private static final Logger LOG = LoggerFactory.getLogger(BaseService.class);
	
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
		LOG.debug("=== OpenAI API Request ===");
		LOG.debug("URL: {}", baseUrl);
		LOG.debug("Message: {}", userMessage);
		ChatCompletionCreateParams params = ChatCompletionCreateParams.builder()
		        .addUserMessage(userMessage)
		        .model(ChatModel.GPT_4_1)
		        .build();
		
		ChatCompletion response = client.chat().completions().create(params);
		LOG.debug("=== OpenAI API Response ===");
		LOG.debug("Model: {}", response.model());
		LOG.debug("Usage - Tokens: {}", response.usage());
		return response;
	}
	
	public List<CategorizationResponse> promptCategorization(Request2LLM req) {
		List<Question> qsts = req.question().split(req.batchSize());
		List<CategorizationResponse> catResponses = new ArrayList<>();		
		if (qsts.isEmpty()) {
			throw new IllegalArgumentException("No questions to process");
		}
		for (Question q : qsts) {
			catResponses = promptCategorization(q, catResponses);
		}
		LOG.debug("Total categorization responses: {}", catResponses.size());
		return catResponses;
	}
	
	public List<CategorizationResponse> promptCategorization(Question q, List<CategorizationResponse> catResponses) {
		LOG.debug("Requesting categorization for question batch");
		
		StructuredChatCompletion<CategorizationResponse> sCC = categorizationRequest(q.toString(false), q.getRowZeroSystemQuestionsString());
		sCC.choices().stream()
	        .flatMap(choice -> choice.message().content().stream())
	        .forEach(catResponses::add);
		
		return catResponses;
	}
	
	private StructuredChatCompletion<CategorizationResponse> categorizationRequest(String userMessage, String systemMessage) {
		LOG.debug("=== OpenAI Categorization Request ===");
		LOG.debug("URL: {}", baseUrl);
		LOG.debug("SystemMessage length: {}", systemMessage.length());
		LOG.debug("UserMessage length: {}", userMessage.length());
		
		StructuredChatCompletionCreateParams<CategorizationResponse> params = ChatCompletionCreateParams.builder()
		        .addSystemMessage(systemMessage)
				.addUserMessage(userMessage)
		        .model(ChatModel.GPT_4_1)
		        .responseFormat(CategorizationResponse.class)
		        .build();
		
		StructuredChatCompletion<CategorizationResponse> response = client.chat().completions().create(params);
		LOG.debug("=== OpenAI Categorization Response ===");
		LOG.debug("Choices count: {}", response.choices().size());
		if (!response.choices().isEmpty()) {
			var firstChoice = response.choices().get(0);
			LOG.debug("First choice has content: {}", firstChoice.message().content().isPresent());
			if (firstChoice.message().content().isPresent()) {
				var content = firstChoice.message().content().get();
				if (content instanceof CategorizationResponse catResp) {
					LOG.debug("Response: {}", catResp);
				}
			}
		}
		return response;
	}
	
}