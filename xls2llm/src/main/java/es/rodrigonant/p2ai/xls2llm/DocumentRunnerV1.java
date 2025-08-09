package es.rodrigonant.p2ai.xls2llm;

import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

import com.openai.models.chat.completions.ChatCompletion;
import com.openai.models.chat.completions.ChatCompletion.Choice;

import es.rodrigonant.p2ai.xls2llm.document.DocumentManager;
import es.rodrigonant.p2ai.xls2llm.document.impl.DocumentMgrXlsImpl;
import es.rodrigonant.p2ai.xls2llm.factory.ServiceFactory;
import es.rodrigonant.p2ai.xls2llm.llmapi.LLMService;
import es.rodrigonant.p2ai.xls2llm.llmapi.impl.LocalTestService;
import es.rodrigonant.p2ai.xls2llm.model.Input2xls;
import es.rodrigonant.p2ai.xls2llm.model.Request2LLM;

@ComponentScan
@EnableAutoConfiguration
public class DocumentRunnerV1 {

	private static ServiceFactory factory = new ServiceFactory();
	
	private DocumentManager dr = new DocumentMgrXlsImpl();

	private LLMService llmService = new LocalTestService();
	
	private static String xmlFilePath = "xls/test1.xlsx";
	private static String xmlWriteFilePath = "src/main/resources/xls/test1-copy.xlsx";
	
	
	public static void main(String[] args) {
		// read
		SpringApplication.run(DocumentRunnerV1.class, args);
		
		int limit = 10;
		DocumentRunnerV1 runner = new DocumentRunnerV1();
		
		Request2LLM req = runner.dr.getDocument(xmlFilePath, limit);
		
		List<String[]> mQst = req.question().getRowQuestion();
		List<String> nxtQsts = req.question().getNextLines();
				
		System.out.println("mQst.get(0): "+ Arrays.asList(mQst.get(0)));
		
		System.out.println("nxtQsts: "+ nxtQsts);
		
		// call

		//LLMService service = factory.getLLMService();
		
		List<ChatCompletion> response = runner.llmService.promptCompletion(req);
		System.out.println(response.get(0).toString());
		
		// write to xlsx

		int row = 0;
		int col = 0;
		for (Choice choice : response.get(0).choices()) {
			System.out.println(choice.message().content());
			Input2xls input = new Input2xls();
			input.putContent(row, col, choice.message().content().get());
			runner.dr.writeDocument(xmlFilePath, xmlWriteFilePath, input);
		}
	}

}
