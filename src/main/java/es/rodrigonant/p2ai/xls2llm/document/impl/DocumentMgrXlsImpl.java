package es.rodrigonant.p2ai.xls2llm.document.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.LinkedHashMap;

import org.apache.commons.io.IOUtils;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.DefaultIndexedColorMap;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import es.rodrigonant.p2ai.xls2llm.constants.ExcelConstants;
import es.rodrigonant.p2ai.xls2llm.document.DocumentManager;
import es.rodrigonant.p2ai.xls2llm.model.Answer;
import es.rodrigonant.p2ai.xls2llm.model.Input2xls;
import es.rodrigonant.p2ai.xls2llm.model.Question;
import es.rodrigonant.p2ai.xls2llm.model.Request2LLM;
import es.rodrigonant.p2ai.xls2llm.model.handling.InputException;

@Service
public class DocumentMgrXlsImpl implements DocumentManager {

	public final int ID_COL = ExcelConstants.ID_COL;
	public final int HEADER_ROW_INI = ExcelConstants.INITIAL_LINE;
	public final int COL_INI = ExcelConstants.INITIAL_COL;
	public final int COL_FIN = ExcelConstants.FINAL_COL;
	public final int INI_NEXTLINES = ExcelConstants.CONTENT_INITIAL_LINE;
	public final int INI_CNT_COL = ExcelConstants.CONTENT_INITIAL_COL;
	public final int COL_CONTENT = ExcelConstants.INITIAL_COL;
	
	private static final Logger LOG = LoggerFactory.getLogger(DocumentMgrXlsImpl.class);
	
	@Override
	public Request2LLM getDocument(String xlsFile, Integer rowLimit) {
		Request2LLM document;
		Workbook workbook = null;
		try {
			Question q = new Question(null);
			List<String> nextLines = new ArrayList<>();
			List<Long> commentIds = new ArrayList<>();
			Answer a = new Answer();
			// TODO extra dimension on nextAnswers
			List<String> nextAnswers = new ArrayList<>();
			
			InputStream file = getFileFromResourceAsStream(xlsFile);
			workbook = new XSSFWorkbook(file);
			
			Sheet sheet = workbook.getSheetAt(0);

			for (Row row : sheet) {
				// Only apply rowLimit to data rows, taking into account INITIAL_LINE
				if (rowLimit != null && row.getRowNum() >= (ExcelConstants.CONTENT_INITIAL_LINE + rowLimit))
					break;
				
				if (isHeaderContent(row, "S")) {
					q.addRowZeroSystemQuestion(getContentOnHeaderColumns(row, COL_INI, COL_FIN));
				} else if (isHeaderContent(row, "Q")){
					q.setRowZeroUserQuestion(getContentOnCell(row, COL_INI));
				} else if (isNextLinesRow(row)){
					LOG.debug(row.toString());
					nextLines.add(getContentOnCell(row, COL_INI));
					commentIds.add((long) (row.getRowNum() - ExcelConstants.CONTENT_INITIAL_LINE));
//					for (int c = COL_INI; c <= COL_FIN; c++) {
//						nextAnswers.add(row.getCell(c).getStringCellValue());
//					}
				}
			}
			q.setNextLines(nextLines);
			q.setCommentIds(commentIds);
			a.setNextLines(nextAnswers);
			
			document = new Request2LLM(q, a, rowLimit);
			LOG.info("getDocument: "+ document.toString());
		} catch (IOException e) {
			throw new InputException(e);
		} finally {
			try {
				if (workbook != null)
					workbook.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}	
		
		return document;
	}
	
	@Override
	public List<Request2LLM> getDocument(String xlsFile, Integer rowLimit, Integer batchSize) {
		Request2LLM document;
		List<Request2LLM> documents = new ArrayList<>();
		InputStream file = getFileFromResourceAsStream(xlsFile);
		try (Workbook workbook = new XSSFWorkbook(file)){
			List<String> nextLines = new ArrayList<>();
			List<Long> commentIds = new ArrayList<>();
			Question q = new Question(null);
			Answer a = new Answer();
			List<String> nextAnswers = new ArrayList<>();
			
			Sheet sheet = workbook.getSheetAt(0);
			int batchCount = 0;
			// Capture header info (system/user questions) once so we can create new Question objects per batch
			List<String[]> headerSystemQuestions = new ArrayList<>();
			String headerUserQuestion = null;
			for (Row row : sheet) {
				// Diagnostic logging: show row num and ID cell raw data to debug counting
				Cell idCellDbg = row.getCell(ID_COL);
				String idInfoDbg;
				if (idCellDbg == null) idInfoDbg = "<null>";
				else if (idCellDbg.getCellType() == CellType.NUMERIC) idInfoDbg = "NUM(" + idCellDbg.getNumericCellValue() + ")";
				else idInfoDbg = idCellDbg.getCellType() + "(" + idCellDbg.toString() + ")";
				LOG.debug("Row#" + row.getRowNum() + " ID:" + idInfoDbg + " -> isNextLinesRow=" + isNextLinesRow(row));
				// Process header rows regardless of content start
				if (isHeaderContent(row, "S")) {
					// collect header system questions
					headerSystemQuestions.add(getContentOnHeaderColumns(row, COL_INI, COL_FIN));
					continue;
				} else if (isHeaderContent(row, "Q")){
					// capture header user question
					headerUserQuestion = getContentOnCell(row, COL_INI);
					continue;
				}

				// Skip rows before the data start
				if (row.getRowNum() < ExcelConstants.CONTENT_INITIAL_LINE) {
					continue;
				}

				// Enforce rowLimit BEFORE treating the row as data. This prevents rows beyond the limit
				// from being added to batches (previous logic added them and only checked limit later).
				if (rowLimit != null && row.getRowNum() >= (ExcelConstants.CONTENT_INITIAL_LINE + rowLimit)) {
					// If there are pending lines that didn't yet form a complete batch, flush them as a final document
					if (!nextLines.isEmpty()) {
						Question qBatch = new Question(headerUserQuestion);
						qBatch.setRowZeroSystemQuestions(headerSystemQuestions);
						qBatch.setNextLines(nextLines);
						qBatch.setCommentIds(commentIds);
						Answer aBatch = new Answer();
						aBatch.setNextLines(nextAnswers);
						document = new Request2LLM(qBatch, aBatch, batchSize);
						documents.add(document);
					}
					break;
				}

				// Treat as data row if primary content cell is non-empty (robust to missing numeric ID)
				if (isDataRow(row)){
					LOG.debug(row.toString());
					nextLines.add(getContentOnCell(row, COL_INI));
					commentIds.add((long) (row.getRowNum() - ExcelConstants.CONTENT_INITIAL_LINE));
					batchCount++;
				}

				if (batchCount >= batchSize) {
					// Create document for the batch: create fresh Question/Answer objects so earlier batches are not mutated
					Question qBatch = new Question(headerUserQuestion);
					qBatch.setRowZeroSystemQuestions(headerSystemQuestions);
					qBatch.setNextLines(nextLines);
					qBatch.setCommentIds(commentIds);
					Answer aBatch = new Answer();
					aBatch.setNextLines(nextAnswers);
					document = new Request2LLM(qBatch, aBatch, batchSize);
					// Reset for next batch
					nextLines = new ArrayList<>();
					commentIds = new ArrayList<>();
					nextAnswers = new ArrayList<>();
					batchCount = 0;
					documents.add(document);
				}
			}

			return documents;
		} catch (IOException e) {
			throw new InputException(e);
		}
	}

	@Override
	public List<Request2LLM> getDocument(String xlsFile, Integer startLine, Integer rowLimit, Integer batchSize) {
		Request2LLM document;
		List<Request2LLM> documents = new ArrayList<>();
		InputStream file = getFileFromResourceAsStream(xlsFile);
		try (Workbook workbook = new XSSFWorkbook(file)){
			List<String> nextLines = new ArrayList<>();
			List<Long> commentIds = new ArrayList<>();
			Question q = new Question(null);
			Answer a = new Answer();
			List<String> nextAnswers = new ArrayList<>();
			
			Sheet sheet = workbook.getSheetAt(0);
			int batchCount = 0;
			// Capture header info (system/user questions) once so we can create new Question objects per batch
			List<String[]> headerSystemQuestions = new ArrayList<>();
			String headerUserQuestion = null;
			
			// Calculate the effective start row based on startLine offset
			// startLine is 1-based (user sees "line 1, line 2, etc."), so subtract 1 to get 0-based offset, then add CONTENT_INITIAL_LINE
			int contentStartRow = ExcelConstants.CONTENT_INITIAL_LINE + (startLine != null && startLine > 0 ? startLine - 1 : 0);
			
			// Calculate the effective end row: rowLimit is also 1-based, so subtract 1 to get 0-based offset
			// Otherwise process to the end of the file
			int endRow = (rowLimit != null && rowLimit > 0) ? (ExcelConstants.CONTENT_INITIAL_LINE + rowLimit - 1) : Integer.MAX_VALUE;
			
			// Find the last row with data to check if startLine is beyond available content
			int lastRowNum = sheet.getLastRowNum();
			LOG.info("getDocument: startLine=" + startLine + ", rowLimit=" + rowLimit + ", contentStartRow=" + contentStartRow + ", endRow=" + endRow + ", lastRowNum=" + lastRowNum);
			if (contentStartRow > lastRowNum) {
				// startLine puts us beyond all available rows
				LOG.info("startLine beyond available rows, returning empty list");
				return documents;
			}
			
			for (Row row : sheet) {
				// Process header rows regardless of content start
				if (isHeaderContent(row, "S")) {
					// collect header system questions
					headerSystemQuestions.add(getContentOnHeaderColumns(row, COL_INI, COL_FIN));
					continue;
				} else if (isHeaderContent(row, "Q")){
					// capture header user question
					headerUserQuestion = getContentOnCell(row, COL_INI);
					continue;
				}

				// Skip rows before the calculated data start (accounts for startLine offset)
				if (row.getRowNum() < contentStartRow) {
					continue;
				}

			// Enforce rowLimit AFTER applying startLine offset. This prevents rows beyond the limit
			// from being added to batches.
			if (row.getRowNum() > endRow) {
				// If there are pending lines that didn't yet form a complete batch, flush them as a final document
				if (!nextLines.isEmpty()) {
					Question qBatch = new Question(headerUserQuestion);
					qBatch.setRowZeroSystemQuestions(headerSystemQuestions);
					qBatch.setNextLines(nextLines);
					qBatch.setCommentIds(commentIds);
					Answer aBatch = new Answer();
					aBatch.setNextLines(nextAnswers);
					document = new Request2LLM(qBatch, aBatch, batchSize);
					documents.add(document);
				}
				break;
			}

				// Treat as data row if primary content cell is non-empty (robust to missing numeric ID)
				if (isDataRow(row)){
					LOG.debug(row.toString());
					nextLines.add(getContentOnCell(row, COL_INI));
					commentIds.add((long) (row.getRowNum() - ExcelConstants.CONTENT_INITIAL_LINE));
					batchCount++;
				}

				if (batchCount >= batchSize) {
					// Create document for the batch: create fresh Question/Answer objects so earlier batches are not mutated
					Question qBatch = new Question(headerUserQuestion);
					qBatch.setRowZeroSystemQuestions(headerSystemQuestions);
					qBatch.setNextLines(nextLines);
					qBatch.setCommentIds(commentIds);
					Answer aBatch = new Answer();
					aBatch.setNextLines(nextAnswers);
					document = new Request2LLM(qBatch, aBatch, batchSize);
					// Reset for next batch
					nextLines = new ArrayList<>();
					commentIds = new ArrayList<>();
					nextAnswers = new ArrayList<>();
					batchCount = 0;
					documents.add(document);
				}
			}

			return documents;
		} catch (IOException e) {
			throw new InputException(e);
		}
	}


	public void writeDocumentMin(String xlsFile, String xlsFileToChg, Input2xls input) {
		InputStream fis = getFileFromResourceAsStream(xlsFile);
		try {
			XSSFWorkbook workbook = new XSSFWorkbook(fis);
			Sheet sheet = workbook.getSheetAt(0);
			Map<Short, org.apache.poi.ss.usermodel.CellStyle> errorStyles = new HashMap<>();
			
			Enumeration<Integer> rIdxs = input.getRowIndexes();
			while (rIdxs.hasMoreElements()) {
				int r = rIdxs.nextElement();
				Enumeration<Integer> cIdxs = input.getColIndexes(r);
				while (cIdxs.hasMoreElements()) {
					int c = cIdxs.nextElement();
					setContentIntoXls(sheet, r, c, input.getCellContent(r, c));
					if (input.isInvalid(r, c)) markCellInvalid(workbook, sheet, r, c, errorStyles);
				}		
			}
			File file = new File(xlsFileToChg);
			LOG.info("Writing to "+ file.getAbsolutePath());
			File targetFile = new File(xlsFileToChg);
			//targetFile.createNewFile();
						
//		    Files.copy(
//		    		fis, 
//		    		targetFile.toPath(), 
//		    		StandardCopyOption.REPLACE_EXISTING);
//		    IOUtils.closeQuietly(fis);
			
		    //IOUtils.copy(fis, new FileOutputStream(file.getAbsolutePath()));
			
			FileOutputStream outputStream = new FileOutputStream(targetFile.getAbsolutePath());
			//fis.transferTo(outputStream);
			workbook.write(outputStream);
			outputStream.flush();
			workbook.close();
			LOG.info("Wrote to (min) "+ new File(xlsFileToChg).getAbsolutePath());
		} catch (IOException e) {
			e.printStackTrace();
			throw new InputException(e);
		}	
	}

	
	public void writeDocument(String xlsFile, String xlsFileToChg, Input2xls input) {
	    try (
	        InputStream fis = getFileFromResourceAsStream(xlsFile);
	        XSSFWorkbook workbook = new XSSFWorkbook(fis);
	        FileOutputStream outputStream = new FileOutputStream(xlsFileToChg)
	    ){
	        Sheet sheet = workbook.getSheetAt(0);
	        Map<Short, org.apache.poi.ss.usermodel.CellStyle> errorStyles = new HashMap<>();
	        if (input.hasMissingResults()) {
	            Row header = sheet.getRow(0);
	            if (header == null) header = sheet.createRow(0);
	            int statusColumn = INI_CNT_COL + 6;
	            Cell statusHeading = header.getCell(statusColumn);
	            if (statusHeading == null) statusHeading = header.createCell(statusColumn, CellType.STRING);
	            statusHeading.setCellValue("LLM result status");
	        }
	
	        Enumeration<Integer> rIdxs = input.getRowIndexes();
	        while (rIdxs.hasMoreElements()) {
	            int r = rIdxs.nextElement();
	            Enumeration<Integer> cIdxs = input.getColIndexes(r);
	            while (cIdxs.hasMoreElements()) {
	                int c = cIdxs.nextElement();
	                setContentIntoXls(sheet, r, c, input.getCellContent(r, c));
	                if (input.isInvalid(r, c)) markCellInvalid(workbook, sheet, r, c, errorStyles);
	            }
	        }
	
	        workbook.write(outputStream);
	        outputStream.flush();
	        LOG.info("Wrote to " + new File(xlsFileToChg).getAbsolutePath());
	    } catch (IOException e) {
	        e.printStackTrace();
	        throw new InputException(e);
	    }
	}
	
	private boolean isHeaderContent(Row row, String systemOrUserQst) {
		Cell id = row.getCell(ID_COL);
		if (id == null) return false;
		if (id.getCellType() == CellType.STRING) {
			return id.getStringCellValue().equals(systemOrUserQst);
		}
		return false;
	}
	
	private boolean isNextLinesRow(Row row) {
		Cell idCell = row.getCell(ID_COL);
		if (idCell == null) return false;
		try {
			if (idCell.getCellType() == CellType.NUMERIC) {
				return idCell.getNumericCellValue() > 0;
			} else if (idCell.getCellType() == CellType.STRING) {
				String s = idCell.getStringCellValue();
				if (s == null || s.trim().isEmpty()) return false;
				try { return Double.parseDouble(s.trim()) > 0; } catch (NumberFormatException ex) { return false; }
			}
		} catch (Exception e) {
			LOG.debug("Error reading ID cell in isNextLinesRow: " + e.getMessage());
		}
		return false;
	}

	// New helper: treat as data row when primary content cell (COL_INI) contains text/number
	private boolean isDataRow(Row row) {
		if (row == null) return false;
		// only consider rows at/after content initial line
		if (row.getRowNum() < ExcelConstants.CONTENT_INITIAL_LINE) return false;
		try {
			for (int i = COL_INI; i <= COL_FIN; i++) {
				Cell c = row.getCell(i);
				if (c == null) continue;
				if (c.getCellType() == CellType.STRING) {
					String s = c.getStringCellValue();
					if (s != null && !s.trim().isEmpty()) return true;
				} else if (c.getCellType() == CellType.NUMERIC) {
					return true;
				} else if (c.getCellType() == CellType.FORMULA) {
					try { if (!c.getStringCellValue().trim().isEmpty()) return true; } catch (Exception ex) { try { if (c.getNumericCellValue() != 0) return true; } catch (Exception e) { /* ignore */ } }
				}
			}
		} catch (Exception e) {
			LOG.debug("Error reading content cells in isDataRow: " + e.getMessage());
		}
		return false;
	}

	private String[] getContentOnHeaderColumns(Row row, int fromCol, int toCol) {
		List<String> cols = new ArrayList<>();
		for (int i = fromCol; i <= toCol; i++) {
			Cell cell = row.getCell(i);
			if (cell == null) { cols.add(""); continue; }
			if (cell.getCellType() == CellType.STRING) cols.add(cell.getStringCellValue());
			else if (cell.getCellType() == CellType.NUMERIC) cols.add(String.valueOf(cell.getNumericCellValue()));
			else {
				try { cols.add(cell.getStringCellValue()); } catch (Exception e) { cols.add(""); }
			}
		}
		return cols.toArray(new String[0]);
	}
	
	private String getContentOnCell(Row row, int col) {
		Cell cell = row.getCell(col);
		if (cell == null) return "";
		if (cell.getCellType() == CellType.STRING) return cell.getStringCellValue();
		if (cell.getCellType() == CellType.NUMERIC) return String.valueOf(cell.getNumericCellValue());
		try { return cell.getStringCellValue(); } catch (Exception e) { return ""; }
	}
	
	
	private InputStream getFileFromResourceAsStream(String fileName) {
        // First, try as a file on the filesystem
        File file = new File(fileName);
        if (file.exists() && file.isFile()) {
            try {
                return new FileInputStream(file);
            } catch (IOException e) {
                throw new IllegalArgumentException("Cannot open file: " + fileName, e);
            }
        }
        // Fallback: try as a classpath resource
        ClassLoader classLoader = getClass().getClassLoader();
        InputStream inputStream = classLoader.getResourceAsStream(fileName);
        if (inputStream == null) {
            throw new IllegalArgumentException("file not found! " + fileName);
        } else {
            return inputStream;
        }
    }

	private void setContentIntoXls(Sheet sheet, int rowNo, int cellNo, String value) {
		Row r = sheet.getRow(rowNo+INI_NEXTLINES); // 10-1
		if (r == null) {
		   // First cell in the row, create
		   r = sheet.createRow(rowNo+INI_NEXTLINES);
		}
		
		Cell c = r.getCell(cellNo+INI_CNT_COL); // 4-1
		if (c == null) {
		    // New cell
		    c = r.createCell(cellNo+INI_CNT_COL, CellType.STRING);
		}
		LOG.info("Setting cell at row: " + (rowNo+INI_NEXTLINES) + ", col: " + (cellNo+INI_CNT_COL) + " with value: " + value);
		c.setCellValue(value);
	}

	@Override
	public void writeExecutionMetadata(String sourceFile, String outputFile, Map<String, String> metadata) {
		File output = new File(outputFile);
		String workbookPath = output.exists() ? outputFile : sourceFile;
		try (
				InputStream input = getFileFromResourceAsStream(workbookPath);
				XSSFWorkbook workbook = new XSSFWorkbook(input);
				FileOutputStream stream = new FileOutputStream(outputFile)) {
			Sheet sheet = workbook.getSheet("XLS2LLM");
			if (sheet == null) sheet = workbook.createSheet("XLS2LLM");
			else {
				for (int rowIndex = sheet.getLastRowNum(); rowIndex >= 0; rowIndex--) {
					Row row = sheet.getRow(rowIndex);
					if (row != null) sheet.removeRow(row);
				}
			}

			Row heading = sheet.createRow(0);
			heading.createCell(0, CellType.STRING).setCellValue("Execution metadata");
			int rowIndex = 1;
			for (Map.Entry<String, String> entry : new LinkedHashMap<>(metadata).entrySet()) {
				Row row = sheet.createRow(rowIndex++);
				row.createCell(0, CellType.STRING).setCellValue(entry.getKey());
				row.createCell(1, CellType.STRING).setCellValue(entry.getValue() == null ? "" : entry.getValue());
			}
			sheet.autoSizeColumn(0);
			sheet.autoSizeColumn(1);
			workbook.write(stream);
		} catch (IOException e) {
			throw new InputException(e);
		}
	}

	private void markCellInvalid(Workbook workbook, Sheet sheet, int rowNo, int cellNo,
			Map<Short, org.apache.poi.ss.usermodel.CellStyle> errorStyles) {
		Cell cell = sheet.getRow(rowNo + INI_NEXTLINES).getCell(cellNo + INI_CNT_COL);
		var sourceStyle = cell.getCellStyle();
		var errorStyle = errorStyles.computeIfAbsent(sourceStyle.getIndex(), key -> {
			var sourceFont = workbook.getFontAt(sourceStyle.getFontIndexAsInt());
			var redFont = (XSSFFont) workbook.createFont();
			redFont.setFontName(sourceFont.getFontName());
			redFont.setFontHeight(sourceFont.getFontHeight());
			redFont.setBold(sourceFont.getBold());
			redFont.setItalic(sourceFont.getItalic());
			redFont.setStrikeout(sourceFont.getStrikeout());
			redFont.setTypeOffset(sourceFont.getTypeOffset());
			redFont.setUnderline(sourceFont.getUnderline());
			redFont.setCharSet(sourceFont.getCharSet());
			redFont.setColor(new XSSFColor(new byte[] { (byte) 0xFF, 0x00, 0x00 }, new DefaultIndexedColorMap()));
			var style = workbook.createCellStyle();
			style.cloneStyleFrom(sourceStyle);
			style.setFont(redFont);
			return style;
		});
		cell.setCellStyle(errorStyle);
	}

	
}
