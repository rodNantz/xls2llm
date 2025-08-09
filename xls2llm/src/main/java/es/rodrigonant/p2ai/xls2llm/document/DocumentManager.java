package es.rodrigonant.p2ai.xls2llm.document;

import es.rodrigonant.p2ai.xls2llm.model.Input2xls;
import es.rodrigonant.p2ai.xls2llm.model.Request2LLM;

public interface DocumentManager {

	Request2LLM getDocument(String xlsFile, Integer rowLimit);

	void writeDocument(String xlsFile, String xlsFileToChg, Input2xls input);
	
}
