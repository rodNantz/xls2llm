package es.rodrigonant.p2ai.xls2llm.model;

import java.util.List;

public record Request2LLM (
			Question question, int batchSize
			) {}
