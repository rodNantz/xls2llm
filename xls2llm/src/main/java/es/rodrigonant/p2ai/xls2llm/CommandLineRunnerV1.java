package es.rodrigonant.p2ai.xls2llm;

import java.awt.Desktop;
import java.awt.EventQueue;
import java.io.File;
import java.io.IOException;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.openai.models.chat.completions.ChatCompletion;
import com.openai.models.chat.completions.ChatCompletion.Choice;

import es.rodrigonant.p2ai.xls2llm.document.DocumentManager;
import es.rodrigonant.p2ai.xls2llm.llmapi.LLMService;
import es.rodrigonant.p2ai.xls2llm.model.Input2xls;
import es.rodrigonant.p2ai.xls2llm.model.Request2LLM;
import es.rodrigonant.p2ai.xls2llm.model.classification.CategorizationResponse;
import es.rodrigonant.p2ai.xls2llm.model.classification.CommentRow;
import es.rodrigonant.p2ai.xls2llm.model.classification.CategoryCol;

@Component
@Profile("!test")
public class CommandLineRunnerV1 implements CommandLineRunner {

	@Autowired
    private DocumentManager dr;

    private LLMService llmService;

    private static final String xmlFilePath = "xls/test1-simple.xlsx";
    private static final String xmlChangeFilePath = "src/main/resources/xls/test1-simple-copy.xlsx";
    private static final int limit = 10;

    public CommandLineRunnerV1(@Qualifier("test-service") LLMService service) {
		this.llmService = service;
	}
    
    @Override
    public void run(String... args) {
        // Read document
        Request2LLM req = dr.getDocument(xmlFilePath, limit);
        List<String[]> sysQsts = req.question().getRowZeroSystemQuestions();
        String mQst = req.question().getRowZeroQuestion();
        List<String> nxtQsts = req.question().getNextLines();

        System.out.println("Main Question: " + mQst);
        if (!sysQsts.isEmpty()) {
            System.out.println("System Questions: ");
            for (String[] sysQ : sysQsts) {
                for (String s : sysQ) {
                    System.out.println("  " + s);
                }
            }
        }
        System.out.println("Next Questions: " + nxtQsts);

        // Call LLMService - use promptCategorization
        List<CategorizationResponse> responses = llmService.promptCategorization(req);
        System.out.println("Categorization Responses: " + responses);

        // Write to new xlsx
        Input2xls input = new Input2xls();
        int rowIdx = 0;
        for (CategorizationResponse response : responses) {
            for (CommentRow comment : response.getComments()) {
            	long id = comment.getCommentId();
                int colIdx = 0;
                for (CategoryCol category : comment.getCategories()) {
                    String value = String.format("-> %s", 
                    		//category.getCategoryName(), 
                    		//category.getEvaluation().getCode(), category.getEvaluation().getDescription());
                    		category.getEvaluation());
                    System.out.println(""+ id + " -> " + value);
                    input.putContent(rowIdx, colIdx, value);
                    colIdx++;
                }
                rowIdx++;
            }
        }
        dr.writeDocument(xmlFilePath, xmlChangeFilePath, input);

        // Open the new file
        File file = new File(xmlChangeFilePath);
        if(file.exists()) {         //checks file exists or not
            if(!Desktop.isDesktopSupported()) {        //check if Desktop is supported by Platform or not
                System.out.println("Desktop not supported");
                fileOpenOnWinWorkaround(file);
            } else {
                Desktop desktop = Desktop.getDesktop();
                EventQueue.invokeLater(() -> {
                    try {
                        desktop.open(file);
                        System.out.println("Opened file: " + file.getAbsolutePath());
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
            }
        } else {
            System.err.println("File not found: " + file.getAbsolutePath());
        }
    }
    
    
    private void fileOpenOnWinWorkaround(File file) {
        final String command = "explorer.exe /SELECT,\"" + file.getAbsolutePath() + "\"";
        try {
			Runtime.getRuntime().exec(command);
			System.out.println("Opened file: " + file.getAbsolutePath());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
    }
}