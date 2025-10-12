package es.rodrigonant.p2ai.xls2llm.model.classification;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import es.rodrigonant.p2ai.xls2llm.model.V1alt;
import es.rodrigonant.p2ai.xls2llm.model.V1_1alt;
import es.rodrigonant.p2ai.xls2llm.model.V1_2alt;

public class CategoryCol {

	@JsonProperty
	private int id;
	@JsonProperty
	private CategoryType evaluationType;
	@JsonProperty
	private String code;
	@JsonProperty
	private String description;

	// Factory methods for type-safe construction
	public static CategoryCol fromV1alt(int id, V1alt alt) {
		CategoryCol col = new CategoryCol();
		col.setCategory(id);
		col.setEvaluationType(CategoryType.V1ALT);
		col.setCode(alt.getCode());
		col.setDescription(alt.getDescription());
		return col;
	}
	public static CategoryCol fromV1_1alt(int id, V1_1alt alt) {
		CategoryCol col = new CategoryCol();
		col.setCategory(id);
		col.setEvaluationType(CategoryType.V1_1ALT);
		col.setCode(alt.getCode());
		col.setDescription(alt.getDescription());
		return col;
	}
	public static CategoryCol fromV1_2alt(int id, V1_2alt alt) {
		CategoryCol col = new CategoryCol();
		col.setCategory(id);
		col.setEvaluationType(CategoryType.V1_2ALT);
		col.setCode(alt.getCode());
		col.setDescription(alt.getDescription());
		return col;
	}
	// getters & setters
	@JsonProperty
	public int getCategory() {
		return id;
	}
	public void setCategory(int category) {
		this.id = category;
	}
	@JsonProperty("evaluationType")
	public CategoryType getEvaluationType() {
		return evaluationType;
	}
	public void setEvaluationType(CategoryType evaluationType) {
		this.evaluationType = evaluationType;
	}
	@JsonProperty("code")
	public String getCode() {
		return code;
	}
	public void setCode(String code) {
		this.code = code;
	}
	@JsonProperty("description")
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}

	@Override
	public String toString() {
		return "Category{" +
				"category id='" + id + '\'' +
				", evaluationType='" + evaluationType + '\'' +
				", code='" + code + '\'' +
				", description='" + description + '\'' +
				'}';
	}
	
}