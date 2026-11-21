package es.rodrigonant.p2ai.xls2llm.model;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;

import es.rodrigonant.p2ai.xls2llm.model.handling.InputException;

public class Validation {

	@Value("#{'${custom.valid.categories}'.split(',')}") 
	private List<String> validCategories;
	
	@Value("#{'${custom.valid.codes}'.split(',')}") 
	private List<String> validCodes;
	
	public void validateCatAndCode(Object category, Object code) throws InputException {
		if (validCategories.contains(category.toString()) && validCodes.contains(code.toString()))
			return;
		throw new InputException("Validation failed for category/code: "+ category +", "+ code);
	}
}
