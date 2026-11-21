package es.rodrigonant.p2ai.xls2llm.llmapi.impl;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.models.ChatModel;
import com.openai.models.chat.completions.ChatCompletion;
import com.openai.models.chat.completions.ChatCompletionCreateParams;
import com.openai.models.responses.Response;
import com.openai.models.responses.ResponseCreateParams;

import es.rodrigonant.p2ai.xls2llm.model.Question;
import es.rodrigonant.p2ai.xls2llm.model.Request2LLM;
import jakarta.annotation.PostConstruct;

@Service
@Qualifier("gpt-service")
public class OpenAIService extends BaseService implements es.rodrigonant.p2ai.xls2llm.llmapi.LLMService {
	
	@Value( "${llm.openai.key}" )
	protected String apiKey;
	
	@Value( "${llm.openai.org}" )
	protected String orgId;
	
	@Value( "${llm.openai.project}" )
	protected String projId;
	
	@Value( "${llm.openai.url}" )
	protected String url;

	public String setupProperties() {
		client = OpenAIOkHttpClient.builder()
			    .apiKey(apiKey)
			    .organization(orgId)
			    .project(projId)
			    .baseUrl(url)
			    .build();
		return url;
	}
	
}
