package es.rodrigonant.p2ai.xls2llm.portal;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import es.rodrigonant.p2ai.xls2llm.GenericTest;
import es.rodrigonant.p2ai.xls2llm.model.Question;
import es.rodrigonant.p2ai.xls2llm.model.Request2LLM;
import es.rodrigonant.p2ai.xls2llm.portal.fileupload.FileUploadController;


public class FileUploadControllerTest extends GenericTest {
	
	@Autowired
	FileUploadController controller;
	
	@Test
	void testUploadFile() {
		Question q = new Question("Categorize os tweets nas categorias (V1) quanto à codificação. ");
		q.addRowZeroSystemQuestion("V1 - Nesta categoria, procura-se identificar o posicionamento dos comentaristas em relação ao Nordeste: "
				+ "Código 00 - Contra//Crítica//Ataque ao Nordeste; "
				+ "Código 01 - A Favor//Defesa do Nordeste."); 
		q.addNextLine("#1 - @soinformando @MineiroInquieto @jairbolsonaro eu sou do nordeste e sou 22");
		q.addNextLine("#2 - Brasil, campeão mundial de geração de energia. - Reindustrialização do Nordeste e mais empregos para a região. https://t.co/WvEIHOkuXO");
		Request2LLM req = new Request2LLM(q, null, 10);
		
		List<Request2LLM> requests = new ArrayList<>();
		requests.add(req);
		int reqsSize = controller.getRequestSize(requests, 100);
		assertEquals(2, reqsSize);
	}
}
