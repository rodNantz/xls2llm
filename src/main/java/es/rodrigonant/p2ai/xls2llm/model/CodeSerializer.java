package es.rodrigonant.p2ai.xls2llm.model;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

public class CodeSerializer extends JsonSerializer<Code> {

    @Override
    public void serialize(Code value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        if (value == null) {
            gen.writeNull();
            return;
        }
        gen.writeString(value.toString());
    }
}
