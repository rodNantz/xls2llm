package es.rodrigonant.p2ai.xls2llm.factory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import es.rodrigonant.p2ai.xls2llm.document.DocumentManager;
import es.rodrigonant.p2ai.xls2llm.llmapi.LLMService;

@Service
public class ServiceFactory {

	@Autowired
	private DocumentManager docM;
	
	@Autowired
    @Qualifier("test-service")
	private LLMService llmService;
	
	public LLMService getLLMService() {
		return this.llmService;
	}

	public DocumentManager getDocumentManager() {
		return this.docM;
	}
	
	
}
