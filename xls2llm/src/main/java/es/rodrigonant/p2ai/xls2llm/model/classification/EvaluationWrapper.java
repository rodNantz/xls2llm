package es.rodrigonant.p2ai.xls2llm.model.classification;

import com.fasterxml.jackson.annotation.JsonProperty;
import es.rodrigonant.p2ai.xls2llm.model.V1alt;
import es.rodrigonant.p2ai.xls2llm.model.V1_1alt;
import es.rodrigonant.p2ai.xls2llm.model.V1_2alt;

public class EvaluationWrapper {
    @JsonProperty
    private V1alt v1alt;
    @JsonProperty
    private V1_1alt v1_1alt;
    @JsonProperty
    private V1_2alt v1_2alt;

    public EvaluationWrapper(V1alt v1alt) {
    	setV1alt(v1alt);
    }
    
    public EvaluationWrapper(V1_1alt v1_1alt) {
		setV1_1alt(v1_1alt);
	}
    
    public EvaluationWrapper(V1_2alt v1_2alt) {
    	setV1_2alt(v1_2alt);
    }
    
    public V1alt getV1alt() { return v1alt; }
    public void setV1alt(V1alt v1alt) { this.v1alt = v1alt; }

    public V1_1alt getV1_1alt() { return v1_1alt; }
    public void setV1_1alt(V1_1alt v1_1alt) { this.v1_1alt = v1_1alt; }

    public V1_2alt getV1_2alt() { return v1_2alt; }
    public void setV1_2alt(V1_2alt v1_2alt) { this.v1_2alt = v1_2alt; }
}
