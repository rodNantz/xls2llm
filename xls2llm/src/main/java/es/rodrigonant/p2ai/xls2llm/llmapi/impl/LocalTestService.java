package es.rodrigonant.p2ai.xls2llm.llmapi.impl;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import com.openai.client.okhttp.OpenAIOkHttpClient;

@Service
@Primary
@Qualifier("test-service")
public class LocalTestService extends BaseService implements es.rodrigonant.p2ai.xls2llm.llmapi.LLMService {
	
	@Value( "${llm.openai.test.key}" )
	private String apiKey;
	
	@Value( "${llm.openai.test.org}" )
	protected String orgId;
	
	@Value( "${llm.openai.test.project}" )
	protected String projId;
	
	@Value( "${llm.openai.test.url}" )
	protected String url;

	@Override
	public String setupProperties() {
		client = OpenAIOkHttpClient.builder()
			    .fromEnv()
			    .apiKey(apiKey)
			    .organization(orgId)
			    .project(projId)
			    .baseUrl(url)
			    .build();
		return url;
	}
	
}
