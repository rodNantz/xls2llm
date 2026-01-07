package es.rodrigonant.p2ai.xls2llm.document;

import java.util.List;

import es.rodrigonant.p2ai.xls2llm.model.Input2xls;
import es.rodrigonant.p2ai.xls2llm.model.Request2LLM;

public interface DocumentManager {
	
	Request2LLM getDocument(String xlsFile, Integer rowLimit);
	
	List<Request2LLM> getDocument(String xlsFile, Integer rowLimit, Integer batchSize);

	void writeDocument(String xlsFile, String xlsFileToChg, Input2xls input);

}
