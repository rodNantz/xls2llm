package es.rodrigonant.p2ai.xls2llm;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import es.rodrigonant.p2ai.xls2llm.model.Question;
import es.rodrigonant.p2ai.xls2llm.model.Request2LLM;

@SpringBootTest
public class RequestBuildTest {

	@Test
	public void splitRequestTest() {
		final String Q_STR = "Quais são os nomes mais comuns?";
		Question q = new Question(Q_STR);
		final String Q_SYS_STR = "Quanto aos nomes apresentados, considerar países como o Brasil e Portugal, ordenado do mais comum ao menos comum";
		q.addRowZeroSystemQuestion(Q_SYS_STR);
		
		List<String> nextLines = new ArrayList<>();
		nextLines.addAll(List.of("Amanda, Alice, Rodrigo", "Vinicius, Gabriel, Lívia"));

		q.setNextLines(nextLines);
		Request2LLM req = new Request2LLM(q, null, 1);	// batch size 1
		List<Question> qsts = q.split(req.batchSize());
		
		for(Question qq : qsts) {
			System.out.println(qq);
		}
		assertEquals(qsts.get(0).getRowZeroQuestion(), Q_STR);
		assertEquals(qsts.get(1).getRowZeroQuestion(), Q_STR);
		
		assertEquals(qsts.get(0).getRowZeroSystemQuestions().getFirst()[0], Q_SYS_STR);
		assertEquals(qsts.get(1).getRowZeroSystemQuestions().getFirst()[0], Q_SYS_STR);
		
		assertEquals(qsts.get(0).getNextLines().getFirst(), "Amanda, Alice, Rodrigo");
		assertEquals(qsts.get(1).getNextLines().getFirst(), "Vinicius, Gabriel, Lívia");
	}
	
}

