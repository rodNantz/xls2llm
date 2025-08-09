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

	public final int ID_COL = 1;
	public final int HEADER_COL_INI = 2;
	//public final int HEADER_COL_FIN = 8;
	public final int HEADER_COL_FIN = 3;	// only cat 1 for testing
	public final int NEXTLINES_ROW_INI = 3;
	public final int NEXTLINES_COL = HEADER_COL_INI;
	
	int wIniLine = ExcelConstants.INITIAL_LINE; 
	int wIniCol = ExcelConstants.INITIAL_COL;
	int wFinalCol = wIniCol; // for test
	private Sheet sheet;
	
	@Override
	public Request2LLM getDocument(String xlsFile, Integer rowLimit) {
		Request2LLM document;
		Workbook workbook = null;
		try {
			Question q = new Question();
			List<String> nextLines = new ArrayList<>();
			Answer a = new Answer();
			// TODO extra dimension on nextAnswers
			List<String> nextAnswers = new ArrayList<>();
			
			InputStream file = getFileFromResourceAsStream(xlsFile);
			workbook = new XSSFWorkbook(file);
			
			this.sheet = workbook.getSheetAt(0);

			for (Row row : sheet) {
				if (rowLimit != null && rowLimit < row.getRowNum())
					break;
				if (isHeaderRow(row)) {
					q.addRowQuestion(getContentOnHeaderCols(row));
				} else if (isNextLinesRow(row)){
					nextLines.add(getContentOnNextLine(row));
					for (int c = wIniCol; c <= wFinalCol; c++) {
						nextAnswers.add(row.getCell(c+wIniCol).getStringCellValue());
					}
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
    ) {
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
	
	
	private boolean isHeaderRow(Row row) {
		// col B == idx 1
		if (row.getCell(1).getCellType() == CellType.NUMERIC)
			return row.getCell(1).getNumericCellValue() == 0d;
		return false;
	}
	
	private boolean isNextLinesRow(Row row) {
		// col B == idx 1
		return (row.getCell(1).getCellType() == CellType.NUMERIC &&
				row.getCell(1).getNumericCellValue() > 0);
	}

	private String[] getContentOnHeaderCols(Row row) {
		List<String> cols = new ArrayList<>();
		for (int i = HEADER_COL_INI; i <= HEADER_COL_FIN; i++) {
			cols.add( row.getCell(i).getStringCellValue() );
		}
		return cols.toArray(new String[0]);
	}
	
	private String getContentOnNextLine(Row row) {
		return row.getCell(NEXTLINES_COL).getStringCellValue();
	}
	
	
	private InputStream getFileFromResourceAsStream(String fileName) {
        // The class loader that loaded the class
        ClassLoader classLoader = getClass().getClassLoader();
        InputStream inputStream = classLoader.getResourceAsStream(fileName);

        // the stream holding the file content
        if (inputStream == null) {
            throw new IllegalArgumentException("file not found! " + fileName);
        } else {
            return inputStream;
        }
    }

	private void setContentIntoXls(int rowNo, int cellNo, String value) {
		Row r = this.sheet.getRow(rowNo+wIniLine); // 10-1
		if (r == null) {
		   // First cell in the row, create
		   r = this.sheet.createRow(rowNo+wIniLine);
		}
		
		Cell c = r.getCell(cellNo+wIniCol); // 4-1
		if (c == null) {
		    // New cell
		    c = r.createCell(cellNo+wIniCol, CellType.STRING);
		}
		System.out.println("Setting cell at row: " + (rowNo+wIniLine) + ", col: " + (cellNo+wIniCol) + " with value: " + value);
		c.setCellValue(value);
	}

	
}
