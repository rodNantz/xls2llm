package es.rodrigonant.p2ai.xls2llm.model;

public record Request2LLM (
			Question question, Answer answer, int batchSize
			) {}
