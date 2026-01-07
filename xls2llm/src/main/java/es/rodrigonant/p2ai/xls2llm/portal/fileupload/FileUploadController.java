package es.rodrigonant.p2ai.xls2llm.portal.fileupload;

import es.rodrigonant.p2ai.xls2llm.aop.Logger;
import es.rodrigonant.p2ai.xls2llm.document.DocumentManager;
import es.rodrigonant.p2ai.xls2llm.llmapi.LLMService;
import es.rodrigonant.p2ai.xls2llm.model.Input2xls;
import es.rodrigonant.p2ai.xls2llm.model.ProgressBar;
import es.rodrigonant.p2ai.xls2llm.model.Question;
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
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class FileUploadController {

    @Autowired
    private DocumentManager dr;

    private Logger LOG = new Logger(FileUploadController.class);
    
    @Autowired
    @Qualifier("gpt-service")
    private LLMService llmService;

    @GetMapping("/")
    public String index() {
        return "formUpload";
    }

    private Map<Long, ProgressBar> progressBars = new HashMap<>();
    
    @GetMapping("/upload")
    public ProgressBar retrieveProgressPct(long uploadId) {
        return progressBars.get(uploadId);
    }
    
    private static final int batchSize = 10;
    
    @PostMapping("/upload")
    public ResponseEntity<InputStreamResource> handleFileUpload(@RequestParam("file") MultipartFile file, @RequestParam(value = "rowLimit", required = false) Integer rowLimit, Model model) throws IOException {
    	// Save uploaded file to temp location
    	long uploadId = (long) (Math.random() * 100000);
        File tempInput = File.createTempFile("input", ".xlsx");
        file.transferTo(tempInput);
        String inputPath = tempInput.getAbsolutePath();
        String outputPath = tempInput.getParent() + File.separator + "output-" + tempInput.getName();

        // Process file (reuse logic from CommandLineRunnerV1)
        // If rowLimit is null or not positive, pass null to process all rows
        Integer effectiveLimit = (rowLimit != null && rowLimit > 0) ? rowLimit : null;
//        Request2LLM req = dr.getDocument(inputPath, effectiveLimit);
        List<Request2LLM> reqs = dr.getDocument(inputPath, effectiveLimit, batchSize);
        ProgressBar progress = new ProgressBar(getRequestSize(reqs, effectiveLimit));
        progressBars.put(uploadId, progress);
        // for every 10 rows: call LLM and write output
        int i = 0;
//        List<Question> qsts = req.question().split(batchSize);
        List<CategorizationResponse> responses = new ArrayList<>();
        for (Request2LLM req : reqs) {
        	// Call LLM and capture the returned categorization responses for this batch
        	responses = llmService.promptCategorization(req);
            // Diagnostic logging: responses and processed counts
            int responsesCount = responses == null ? 0 : responses.size();
            int commentsCount = 0;
            if (responses != null) {
                for (CategorizationResponse cr : responses) {
                    if (cr.getComments() != null) commentsCount += cr.getComments().size();
                }
            }
            LOG.info("Batch starting at offset=" + i + ": responses.count=" + responsesCount + ", comments.count=" + commentsCount);
            Input2xls input = Input2xls.fromCategorizationResponseList(responses, i);
            // Log Input2xls entries
            Enumeration<Integer> rowsDbg = input.getRowIndexes();
            StringBuilder sb = new StringBuilder();
            int processed = 0;
            while (rowsDbg.hasMoreElements()) {
                int rr = rowsDbg.nextElement();
                Enumeration<Integer> colsDbg = input.getColIndexes(rr);
                boolean rowHasCol = false;
                while (colsDbg.hasMoreElements()) {
                    int cc = colsDbg.nextElement();
                    sb.append("[r=").append(rr).append(",c=").append(cc).append(",v=").append(input.getCellContent(rr, cc)).append("] ");
                    rowHasCol = true;
                }
                if (rowHasCol) processed++;
            }
            LOG.info("Prepared Input2xls entries: " + sb.toString());
            // Use the original input file only for the first write; subsequent writes must use the last output
            String sourcePath = (i == 0) ? inputPath : outputPath;
            dr.writeDocument(sourcePath, outputPath, input);
            // Advance the offset by the actual number of comment rows written (use input rows count)
             i += processed;
             progress.setState(i);
             LOG.info("Progress: " + i);
        }
        
        // Return the written file as a download
        File result = new File(outputPath);
        InputStreamResource resource = new InputStreamResource(new FileInputStream(result));
        
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + result.getName())
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .contentLength(result.length())
                .body(resource);
    }
    
    
    public int getRequestSize(List<Request2LLM> reqs, Integer limit) {
		int size = 0;
		for (Request2LLM r : reqs) {
			size += r.question().getNextLines().size();
		}
		if (limit == null) return size;
		return size > limit ? limit : size;
	}
    
}