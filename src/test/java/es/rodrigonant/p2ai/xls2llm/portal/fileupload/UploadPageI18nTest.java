package es.rodrigonant.p2ai.xls2llm.portal.fileupload;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class UploadPageI18nTest {
    @Autowired
    private MockMvc mockMvc;

    @Test
    void rendersProjectCustomizationAndEnglishUi() throws Exception {
        mockMvc.perform(get("/").param("lang", "en"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString(
                        "Codificação sobre o Nordeste e Nordestinos")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString(
                        "Classify an Excel spreadsheet")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString(
                        "nordeste-eleicoes-2022")))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString(
                        "Customization source"))))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString(
                        "1.0.0"))));
    }
}
