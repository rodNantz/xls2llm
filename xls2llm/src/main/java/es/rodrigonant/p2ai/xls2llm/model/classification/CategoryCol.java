package es.rodrigonant.p2ai.xls2llm.model.classification;

import jakarta.validation.constraints.Pattern;

import com.fasterxml.jackson.annotation.JsonProperty;
import es.rodrigonant.p2ai.xls2llm.model.V1alt;
import es.rodrigonant.p2ai.xls2llm.model.V1_1alt;
import es.rodrigonant.p2ai.xls2llm.model.V1_2alt;

public class CategoryCol {
 	
	@JsonProperty
	private double id;
	@JsonProperty
	private CategoryType evaluationType;
	@JsonProperty
	@Pattern(regexp = "\\d{2}", message = "Code must be two digits")
	private String code;
	@JsonProperty
	private String description;

	// Factory methods for type-safe construction
	public static CategoryCol fromV1alt(double id, V1alt alt) {
		CategoryCol col = new CategoryCol();
		col.setCategory(id);
		col.setEvaluationType(CategoryType.V1ALT);
		col.setCode(alt.getCode().toString());
		col.setDescription(alt.getDescription());
		return col;
	}
	public static CategoryCol fromV1_1alt(double id, V1_1alt alt) {
		CategoryCol col = new CategoryCol();
		col.setCategory(id);
		col.setEvaluationType(CategoryType.V1_1ALT);
		col.setCode(alt.getCode().toString());
		col.setDescription(alt.getDescription());
		return col;
	}
	public static CategoryCol fromV1_2alt(double id, V1_2alt alt) {
		CategoryCol col = new CategoryCol();
		col.setCategory(id);
		col.setEvaluationType(CategoryType.V1_2ALT);
		col.setCode(alt.getCode().toString());
		col.setDescription(alt.getDescription());
		return col;
	}
	// getters & setters
	@JsonProperty
	public double getCategory() {
		return id;
	}
	public void setCategory(double d) {
		this.id = d;
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
        // Validate before setting
        this.code = code;
    }
//    public void setCode(Code code) {
//        this.code = code.toString();
//    }
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
				"code='" + code + '\'' +
				", description='" + description + '\'' +
				", category id='" + id + '\'' +
				", evaluationType='" + evaluationType + '\'' +
				'}';
	}
	
}