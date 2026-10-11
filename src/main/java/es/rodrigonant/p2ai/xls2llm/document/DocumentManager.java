package es.rodrigonant.p2ai.xls2llm.document;

import java.util.List;
import java.util.Map;

import es.rodrigonant.p2ai.xls2llm.model.Input2xls;
import es.rodrigonant.p2ai.xls2llm.model.Request2LLM;

public interface DocumentManager {
	
	Request2LLM getDocument(String xlsFile, Integer rowLimit);
	
	List<Request2LLM> getDocument(String xlsFile, Integer rowLimit, Integer batchSize);

	List<Request2LLM> getDocument(String xlsFile, Integer startLine, Integer rowLimit, Integer batchSize);

	void writeDocument(String xlsFile, String xlsFileToChg, Input2xls input);

	void writeExecutionMetadata(String sourceFile, String outputFile, Map<String, String> metadata);

}
