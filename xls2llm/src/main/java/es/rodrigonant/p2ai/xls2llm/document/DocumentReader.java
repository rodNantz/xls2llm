package es.rodrigonant.p2ai.xls2llm.document;

import java.io.File;

import es.rodrigonant.p2ai.xls2llm.model.Request2LLM;

public interface DocumentReader {

	Request2LLM getDocument(String xmlFile);

}
