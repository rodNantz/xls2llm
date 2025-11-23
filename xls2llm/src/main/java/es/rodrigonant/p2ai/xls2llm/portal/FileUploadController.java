package es.rodrigonant.p2ai.xls2llm.portal;

import es.rodrigonant.p2ai.xls2llm.document.DocumentManager;
import es.rodrigonant.p2ai.xls2llm.llmapi.LLMService;
import es.rodrigonant.p2ai.xls2llm.model.Input2xls;
import es.rodrigonant.p2ai.xls2llm.model.Request2LLM;
import es.rodrigonant.p2ai.xls2llm.model.classification.CategorizationResponse;
import es.rodrigonant.p2ai.xls2llm.model.classification.CommentRow;
import es.rodrigonant.p2ai.xls2llm.model.classification.CategoryCol;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.util.List;

@Controller
public class FileUploadController {

    @Autowired
    private DocumentManager dr;

    @Autowired
    @Qualifier("gpt-service")
    private LLMService llmService;

    @GetMapping("/")
    public String index() {
        return "formUpload";
    }

    @PostMapping("/upload")
    public ResponseEntity<InputStreamResource> handleFileUpload(@RequestParam("file") MultipartFile file, Model model) throws IOException {
        // Save uploaded file to temp location
        File tempInput = File.createTempFile("input", ".xlsx");
        file.transferTo(tempInput);
        String inputPath = tempInput.getAbsolutePath();
        String outputPath = tempInput.getParent() + File.separator + "output-" + tempInput.getName();

        // Process file (reuse logic from CommandLineRunnerV1)
        Request2LLM req = dr.getDocument(inputPath, 10);
        List<CategorizationResponse> responses = llmService.promptCategorization(req);
        Input2xls input = new Input2xls();
        int rowIdx = 0;
        for (CategorizationResponse response : responses) {
            for (CommentRow comment : response.getComments()) {
                long id = comment.getCommentId();
                int colIdx = 0;
                for (CategoryCol category : comment.getCategories()) {
                    String value = String.format("-> %s", category.toString());
                    input.putContent(rowIdx, colIdx, value);
                    colIdx++;
                }
                rowIdx++;
            }
        }
        dr.writeDocument(inputPath, outputPath, input);

        // Return the written file as a download
        File result = new File(outputPath);
        InputStreamResource resource = new InputStreamResource(new FileInputStream(result));
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + result.getName())
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .contentLength(result.length())
                .body(resource);
    }
}
