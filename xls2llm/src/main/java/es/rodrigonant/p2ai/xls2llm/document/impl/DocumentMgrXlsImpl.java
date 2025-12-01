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

import org.apache.commons.io.IOUtils;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
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
	
	private Sheet sheet;
	
	@Override
	public Request2LLM getDocument(String xlsFile, Integer rowLimit) {
		Request2LLM document;
		Workbook workbook = null;
		try {
			Question q = new Question(null);
			List<String> nextLines = new ArrayList<>();
			Answer a = new Answer();
			// TODO extra dimension on nextAnswers
			List<String> nextAnswers = new ArrayList<>();
			
			InputStream file = getFileFromResourceAsStream(xlsFile);
			workbook = new XSSFWorkbook(file);
			
			this.sheet = workbook.getSheetAt(0);

			for (Row row : sheet) {
				// Only apply rowLimit to data rows, taking into account INITIAL_LINE
				if (rowLimit != null && row.getRowNum() >= (ExcelConstants.CONTENT_INITIAL_LINE + rowLimit))
					break;
				
				if (isHeaderContent(row, "S")) {
					q.addRowZeroSystemQuestion(getContentOnHeaderColumns(row, COL_INI, COL_FIN));
				} else if (isHeaderContent(row, "Q")){
					q.setRowZeroUserQuestion(getContentOnCell(row, COL_INI));
				} else if (isNextLinesRow(row)){
					debug(row.toString());
					nextLines.add(getContentOnCell(row, COL_INI));
//					for (int c = COL_INI; c <= COL_FIN; c++) {
//						nextAnswers.add(row.getCell(c).getStringCellValue());
//					}
				}
			}
			q.setNextLines(nextLines);
			a.setNextLines(nextAnswers);
			
			document = new Request2LLM(q, a, rowLimit);
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
	

	private void debug(String string) {
		// TODO Auto-generated method stub
		System.out.println("DEBUG "+ string);
	}


	public void writeDocumentMin(String xlsFile, String xlsFileToChg, Input2xls input) {
		InputStream fis = getFileFromResourceAsStream(xlsFile);
		try {
			XSSFWorkbook workbook = new XSSFWorkbook(fis);
			this.sheet = workbook.getSheetAt(0);
			
			Enumeration<Integer> rIdxs = input.getRowIndexes();
			while (rIdxs.hasMoreElements()) {
				int r = rIdxs.nextElement();
				Enumeration<Integer> cIdxs = input.getColIndexes(r);
				while (cIdxs.hasMoreElements()) {
					int c = cIdxs.nextElement();
					setContentIntoXls(r, c, input.getCellContent(r, c));
				}		
			}
			File file = new File(xlsFileToChg);
			System.out.println("Writing to "+ file.getAbsolutePath());
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
	        this.sheet = workbook.getSheetAt(0);
	
	        Enumeration<Integer> rIdxs = input.getRowIndexes();
	        while (rIdxs.hasMoreElements()) {
	            int r = rIdxs.nextElement();
	            Enumeration<Integer> cIdxs = input.getColIndexes(r);
	            while (cIdxs.hasMoreElements()) {
	                int c = cIdxs.nextElement();
	                setContentIntoXls(r, c, input.getCellContent(r, c));
	            }
	        }
	
	        workbook.write(outputStream);
	        outputStream.flush();
	    } catch (IOException e) {
	        e.printStackTrace();
	        throw new InputException(e);
	    }
	}
	
	private boolean isHeaderContent(Row row, String systemOrUserQst) {
		System.err.println(row.getCell(ID_COL));
		return row.getCell(ID_COL).getCellType() == CellType.STRING &&
			   row.getCell(ID_COL).getStringCellValue().equals(systemOrUserQst);
		
	}
	
	private boolean isNextLinesRow(Row row) {
		// col B == idx 1
		return (row.getCell(ID_COL).getCellType() == CellType.NUMERIC &&
				row.getCell(ID_COL).getNumericCellValue() > 0);
	}

	private String[] getContentOnHeaderColumns(Row row, int fromCol, int toCol) {
		List<String> cols = new ArrayList<>();
		for (int i = COL_INI; i <= COL_FIN; i++) {
			cols.add( row.getCell(i).getStringCellValue() );
		}
		return cols.toArray(new String[0]);
	}
	
	private String getContentOnCell(Row row, int col) {
		return row.getCell(COL_INI).getStringCellValue();
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

	private void setContentIntoXls(int rowNo, int cellNo, String value) {
		Row r = this.sheet.getRow(rowNo+INI_NEXTLINES); // 10-1
		if (r == null) {
		   // First cell in the row, create
		   r = this.sheet.createRow(rowNo+INI_NEXTLINES);
		}
		
		Cell c = r.getCell(cellNo+INI_CNT_COL); // 4-1
		if (c == null) {
		    // New cell
		    c = r.createCell(cellNo+INI_CNT_COL, CellType.STRING);
		}
		System.out.println("Setting cell at row: " + (rowNo+INI_NEXTLINES) + ", col: " + (cellNo+INI_CNT_COL) + " with value: " + value);
		c.setCellValue(value);
	}

	
}