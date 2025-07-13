package es.rodrigonant.p2ai.xls2llm.document.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import es.rodrigonant.p2ai.xls2llm.document.DocumentReader;
import es.rodrigonant.p2ai.xls2llm.model.Question;
import es.rodrigonant.p2ai.xls2llm.model.Request2LLM;
import es.rodrigonant.p2ai.xls2llm.model.handling.InputException;

@Component
public class DocumentReaderXlsImpl implements DocumentReader {

	public final int ID_COL = 1;
	public final int HEADER_COL_INI = 2;
	public final int HEADER_COL_FIN = 8;
	public final int NEXTLINES_ROW_INI = 3;
	public final int NEXTLINES_COL = HEADER_COL_INI;
	
	
	@Override
	public Request2LLM getDocument(File xlsFile) {
		Request2LLM document;		
		
		try {
			Question q = new Question();
			List<String> nextLines = new ArrayList<>();
			
			FileInputStream file = new FileInputStream(xlsFile);
			Workbook workbook = new XSSFWorkbook(file);
			
			Sheet sheet = workbook.getSheetAt(0);

			Map<Integer, List<String>> data = new HashMap<>();
			int i = 0;
			for (Row row : sheet) {
				if (isHeaderRow(row)) {
					q.addRowQuestion(getContentOnHeaderCols(row));
				} else if (isNextLinesRow(row)){
					nextLines.add(getContentOnNextLine(row));
				}
			}
			q.setNextLines(nextLines.toArray(new String[0]));
			
			document = new Request2LLM(q, 10);
		} catch (IOException e) {
			throw new InputException(e);
		}
		
		return document;
	}
	
	private boolean isHeaderRow(Row row) {
		// col B == idx 1
		return row.getCell(1).getStringCellValue().equals("0");
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
	
}
