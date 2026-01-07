package es.rodrigonant.p2ai.xls2llm.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.databind.ObjectMapper;

import es.rodrigonant.p2ai.xls2llm.model.Code;

import org.junit.jupiter.api.Test;

public class CodeSerializationTest {

    @Test
    public void serializeCodeZero() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        Code code = new Code(0);
        String json = mapper.writeValueAsString(code);
        assertEquals("\"00\"", json);
    }
}
