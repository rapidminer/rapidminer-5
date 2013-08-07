/*
 *  RapidMiner
 *
 *  Copyright (C) 2001-2013 by Rapid-I and the contributors
 *
 *  Complete list of developers available at our web site:
 *
 *       http://rapid-i.com
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Affero General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Affero General Public License for more details.
 *
 *  You should have received a copy of the GNU Affero General Public License
 *  along with this program.  If not, see http://www.gnu.org/licenses/.
 */
package com.rapidminer.operator.nio.model;

import static com.rapidminer.operator.nio.ExcelExampleSource.PARAMETER_SHEET_NUMBER;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.logging.Level;

import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableModel;

import jxl.Workbook;
import jxl.WorkbookSettings;
import jxl.read.biff.BiffException;

import org.apache.poi.POIXMLException;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.WorkbookFactory;

import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.UserError;
import com.rapidminer.operator.nio.Excel2007SheetTableModel;
import com.rapidminer.operator.nio.ExcelExampleSource;
import com.rapidminer.operator.nio.ExcelSheetTableModel;
import com.rapidminer.operator.ports.metadata.ExampleSetMetaData;
import com.rapidminer.parameter.UndefinedParameterError;
import com.rapidminer.tools.I18N;
import com.rapidminer.tools.LogService;
import com.rapidminer.tools.ProgressListener;
import com.rapidminer.tools.Tools;
import com.rapidminer.tools.io.Encoding;

/**
 * A class holding information about configuration of the Excel Result Set
 * 
 * @author Sebastian Land, Marco Boeck
 */
public class ExcelResultSetConfiguration implements DataResultSetFactory {

	private int rowOffset = -1;
	private int columnOffset = -1;
	private int rowLast = Integer.MAX_VALUE;
	private int columnLast = Integer.MAX_VALUE;
	/** Numbering starts at 0. */
	private int sheet = -1;

	private Charset encoding;
	private org.apache.poi.ss.usermodel.Workbook workbookPOI;
	private InputStream workbookPOIInputStream;
	private jxl.Workbook workbookJXL;
	private File workbookFile;

	private boolean isEmulatingOldNames;

	private String timezone;
	private String datePattern;

	/**
	 * This constructor must read in all settings from the parameters of the given operator.
	 * 
	 * @throws OperatorException
	 */
	public ExcelResultSetConfiguration(ExcelExampleSource excelExampleSource) throws OperatorException {
		if (excelExampleSource.isParameterSet(ExcelExampleSource.PARAMETER_IMPORTED_CELL_RANGE)) {
			parseExcelRange(excelExampleSource.getParameterAsString(ExcelExampleSource.PARAMETER_IMPORTED_CELL_RANGE));
		}
		//		else {
		//			throw new UserError(null, 205, ExcelExampleSource.PARAMETER_IMPORTED_CELL_RANGE, excelExampleSource.getName());
		//		}

		if (excelExampleSource.isParameterSet(PARAMETER_SHEET_NUMBER)) {
			this.sheet = excelExampleSource.getParameterAsInt(PARAMETER_SHEET_NUMBER) - 1;
		}
		if (excelExampleSource.isFileSpecified()) {
			this.workbookFile = excelExampleSource.getSelectedFile();
		} else {
			
			String excelParamter;
			try {
				excelParamter = excelExampleSource.getParameter(ExcelExampleSource.PARAMETER_EXCEL_FILE);
			} catch (UndefinedParameterError e) {
				excelParamter = null;
			}
			if (excelParamter != null && !"".equals(excelParamter)) {
				File excelFile = new File(excelParamter);
				if (excelFile.exists()) {
					this.workbookFile = excelFile;
				}
			}
		}
		//		if (excelExampleSource.isParameterSet(PARAMETER_EXCEL_FILE)) {
		//			this.workbookFile = excelExampleSource.getParameterAsFile(PARAMETER_EXCEL_FILE);
		//		}

		if (excelExampleSource.isParameterSet(AbstractDataResultSetReader.PARAMETER_DATE_FORMAT)) {
			datePattern = excelExampleSource.getParameterAsString(AbstractDataResultSetReader.PARAMETER_DATE_FORMAT);
		}

		if (excelExampleSource.isParameterSet(AbstractDataResultSetReader.PARAMETER_TIME_ZONE)) {
			timezone = excelExampleSource.getParameterAsString(AbstractDataResultSetReader.PARAMETER_TIME_ZONE);
		}

		encoding = Encoding.getEncoding(excelExampleSource);

		isEmulatingOldNames = excelExampleSource.getCompatibilityLevel().isAtMost(ExcelExampleSource.CHANGE_5_0_11_NAME_SCHEMA);
	}

	/**
	 * This will create a completely empty result set configuration
	 */
	public ExcelResultSetConfiguration() {
	}

	/**
	 * Returns the RowOffset
	 */
	public int getRowOffset() {
		return rowOffset;
	}

	/**
	 * Returns the ColumnOffset
	 */
	public int getColumnOffset() {
		return columnOffset;
	}

	/** 
	 * Returns if there is an active workbook.
	 * */
	public boolean hasWorkbook() {
		return workbookJXL != null || workbookPOI != null;
	}

	/**
	 * Creates an excel table model (either {@link ExcelSheetTableModel} or {@link Excel2007SheetTableModel}, depending on file).
	 * @param sheetIndex the index of the sheet (0-based)
	 * @return
	 * @throws BiffException
	 * @throws IOException
	 * @throws InvalidFormatException
	 */
	public AbstractTableModel createExcelTableModel(int sheetIndex) throws BiffException, IOException, InvalidFormatException {
		if (getFile().getAbsolutePath().endsWith(".xlsx")) {
			// excel 2007 file
			if (workbookPOI == null) {
				createWorkbookPOI();
			}
			Excel2007SheetTableModel excelSheetTableModel = new Excel2007SheetTableModel(workbookPOI.getSheetAt(sheetIndex));
			return excelSheetTableModel;
		} else {
			// excel pre 2007 file
			if (workbookJXL == null) {
				createWorkbookJXL();
			}
			ExcelSheetTableModel excelSheetTableModel = new ExcelSheetTableModel(workbookJXL.getSheet(sheetIndex));
			return excelSheetTableModel;
		}
	}

	/**
	 * Returns the number of sheets in the excel file
	 * @return
	 * @throws IOException 
	 * @throws BiffException 
	 * @throws InvalidFormatException 
	 */
	public int getNumberOfSheets() throws BiffException, IOException, InvalidFormatException {
		if (getFile().getAbsolutePath().endsWith(".xlsx")) {
			// excel 2007 file
			if (workbookPOI == null) {
				createWorkbookPOI();
			}
			return workbookPOI.getNumberOfSheets();
		} else {
			// excel pre 2007 file
			if (workbookJXL == null) {
				createWorkbookJXL();
			}
			return workbookJXL.getNumberOfSheets();
		}
	}

	/**
	 * Returns the names of all sheets in the excel file
	 * @return
	 * @throws IOException 
	 * @throws BiffException 
	 * @throws InvalidFormatException 
	 */
	public String[] getSheetNames() throws BiffException, IOException, InvalidFormatException {
		if (getFile().getAbsolutePath().endsWith(".xlsx")) {
			// excel 2007 file
			if (workbookPOI == null) {
				createWorkbookPOI();
			}
			String[] sheetNames = new String[getNumberOfSheets()];
			for (int i = 0; i < getNumberOfSheets(); i++) {
				sheetNames[i] = workbookPOI.getSheetName(i);
			}
			return sheetNames;
		} else {
			// excel pre 2007 file
			if (workbookJXL == null) {
				createWorkbookJXL();
			}
			return workbookJXL.getSheetNames();
		}
	}

	/**
	 * Returns the encoding for this configuration.
	 * @return
	 */
	public Charset getEncoding() {
		return this.encoding;
	}

	/**
	 * This returns the file of the referenced excel file
	 */
	public File getFile() {
		return workbookFile;
	}

	/**
	 * This will set the workbook file. It will assure that an existing preopened workbook will be closed if files
	 * differ.
	 */
	public void setWorkbookFile(File selectedFile) {
		if (selectedFile.equals(this.workbookFile)) {
			return;
		}
		if (workbookJXL != null) {
			workbookJXL.close();
			workbookJXL = null;
		}
		workbookFile = selectedFile;
		try {
			if (workbookPOIInputStream != null) {
				workbookPOIInputStream.close();
			}
		} catch (IOException e) {
			LogService.getRoot().log(Level.WARNING, I18N.getMessage(LogService.getRoot().getResourceBundle(), "com.rapidminer.operator.nio.model.ExcelResultSetConfiguration.close_workbook_error", e.getMessage()), e);
		}
		workbookPOI = null;
		workbookPOIInputStream = null;
		rowOffset = 0;
		columnOffset = 0;
		rowLast = Integer.MAX_VALUE;
		columnLast = Integer.MAX_VALUE;
		sheet = 0;
	}

	public int getRowLast() {
		return rowLast;
	}

	public void setRowLast(int rowLast) {
		this.rowLast = rowLast;
	}

	public int getColumnLast() {
		return columnLast;
	}

	public void setColumnLast(int columnLast) {
		this.columnLast = columnLast;
	}

	public int getSheet() {
		return sheet;
	}

	public void setSheet(int sheet) {
		this.sheet = sheet;
	}

	public void setRowOffset(int rowOffset) {
		this.rowOffset = rowOffset;
	}

	public void setColumnOffset(int columnOffset) {
		this.columnOffset = columnOffset;
	}

	@Override
	public DataResultSet makeDataResultSet(Operator operator) throws OperatorException {
		if (getFile() == null) {
			throw new UserError(operator, 205, ExcelExampleSource.PARAMETER_EXCEL_FILE, "");
		}
		if (getFile().getAbsolutePath().endsWith(".xlsx")) {
			// excel 2007 file
			return new Excel2007ResultSet(operator, this);
		} else if (getFile().getAbsolutePath().endsWith(".xls")) {
			// excel pre 2007 file
			return new ExcelResultSet(operator, this);
		} else {
			// we might also get a file object that has neither .xlsx nor .xls as file ending,
			// so we have no choice but to try and open the file with the pre 2007 JXL lib to see if it works.
			// If it does not work, it's an excel 2007 file.
			try {
				Workbook.getWorkbook(getFile());
				return new ExcelResultSet(operator, this);
			} catch (Exception e) {
				return new Excel2007ResultSet(operator, this);
			}
		}
	}

	/** See class comment on {@link ExcelSheetTableModel} for a comment why that class is not used here.
	 *  In fact we are using a {@link DefaultPreview} here as well. */
	@Override
	public TableModel makePreviewTableModel(ProgressListener listener) throws OperatorException {
		final DataResultSet resultSet = makeDataResultSet(null);
		try {
			return new DefaultPreview(resultSet, listener);
		} catch (ParseException e) {
			throw new UserError(null, 302, getFile().getPath(), e.getMessage());
		} finally {
			if(resultSet != null) {
				resultSet.close();
			}
		}
	}

	public void closeWorkbook() {
		if (workbookJXL != null) {
			workbookJXL.close();
			workbookJXL = null;
		}
		try {
			if (workbookPOIInputStream != null) {
				workbookPOIInputStream.close();
			}
		} catch (IOException e) {
			LogService.getRoot().log(Level.WARNING, I18N.getMessage(LogService.getRoot().getResourceBundle(), "com.rapidminer.operator.nio.model.ExcelResultSetConfiguration.close_workbook_error", e.getMessage()), e);
		}
		workbookPOI = null;
		workbookPOIInputStream = null;
	}

	@Override
	public void setParameters(AbstractDataResultSetReader source) {
		String range = Tools.getExcelColumnName(columnOffset) + (rowOffset + 1) + ":" + Tools.getExcelColumnName(columnLast) + (rowLast + 1);
		source.setParameter(ExcelExampleSource.PARAMETER_IMPORTED_CELL_RANGE, range);
		source.setParameter(PARAMETER_SHEET_NUMBER, String.valueOf(sheet + 1));
		source.setParameter(ExcelExampleSource.PARAMETER_EXCEL_FILE, workbookFile.getAbsolutePath());
	}

	public void parseExcelRange(String range) throws OperatorException {
		String[] split = range.split(":", 2);
		try {
			int[] topLeft = parseExcelCell(split[0]);
			columnOffset = topLeft[0];
			rowOffset = topLeft[1];
			if (split.length < 2) {
				rowLast = Integer.MAX_VALUE;
				columnLast = Integer.MAX_VALUE;
			} else {
				int[] bottomRight = parseExcelCell(split[1]);
				columnLast = bottomRight[0];
				rowLast = bottomRight[1];
			}
		} catch (OperatorException e) {
			throw new UserError(null, e, 223, range);
		}
	}

	private static int[] parseExcelCell(String string) throws OperatorException {
		int i = 0;
		int column = 0;
		int row = 0;
		while (i < string.length() && (Character.isLetter(string.charAt(i)))) {
			char c = string.charAt(i);
			c = Character.toUpperCase(c);
			if (c < 'A' || c > 'Z')
				throw new UserError(null, 224, string);
			column *= 26;
			column += (c - 'A') + 1;
			i++;
		}
		if (i < string.length()) { // at least one digit left
			String columnStr = string.substring(i);
			try {
				row = Integer.parseInt(columnStr);
			} catch (NumberFormatException e) {
				throw new UserError(null, 224, string);
			}
		}
		return new int[] { column - 1, row - 1 };
	}

	@Override
	public String getResourceName() {
		return workbookFile.getAbsolutePath();
	}

	@Override
	public ExampleSetMetaData makeMetaData() {
		final ExampleSetMetaData result = new ExampleSetMetaData();
		if (rowLast != Integer.MAX_VALUE) {
			result.setNumberOfExamples(rowLast - rowOffset + 1);
		}
		return result;
	}

	/**
	 * This returns whether the old naming style should be kept from prior to 5.1.000 versions.
	 */
	public boolean isEmulatingOldNames() {
		return isEmulatingOldNames;
	}

	@Override
	public void close() {
		if (workbookJXL != null) {
			workbookJXL.close();
		}
		try {
			if (workbookPOIInputStream != null) {
				workbookPOIInputStream.close();
				workbookPOIInputStream = null;
			}
		} catch (IOException e) {
			LogService.getRoot().log(Level.WARNING, I18N.getMessage(LogService.getRoot().getResourceBundle(), "com.rapidminer.operator.nio.model.ExcelResultSetConfiguration.close_workbook_error", e.getMessage()), e);
		}
	}

	/**
	 * Creates the JXL workbook.
	 * @throws BiffException
	 * @throws IOException
	 */
	private void createWorkbookJXL() throws BiffException, IOException {
		File file = getFile();
		WorkbookSettings workbookSettings = new WorkbookSettings();
		if (encoding != null) {
			workbookSettings.setEncoding(encoding.name());
		}
		workbookJXL = Workbook.getWorkbook(file, workbookSettings);
	}

	/**
	 * Creates the POI workbook.
	 * @throws InvalidFormatException
	 * @throws IOException
	 */
	private void createWorkbookPOI() throws InvalidFormatException, IOException {
		workbookPOIInputStream = new FileInputStream(getFile());
		try {
			workbookPOI = WorkbookFactory.create(workbookPOIInputStream);
		} catch (IllegalArgumentException e) {
			// Thrown if the selected file is not a valid .xlsx file at all
			throw new IOException(e.getMessage());
		} catch (POIXMLException e) {
			// Thrown if the selected file is a partially broken .xlsx file
			throw new IOException(I18N.getMessage(I18N.getErrorBundle(), "import.excel.excel_file_broken"));
		}
	}

	/**
	 * @return the timezone
	 */
	public String getTimezone() {
		return this.timezone;
	}

	/**
	 * @return the datePattern
	 */
	public String getDatePattern() {
		return this.datePattern;
	}

	/**
	 * @param timezone the timezone to set
	 */
	public void setTimezone(String timezone) {
		this.timezone = timezone;
	}

	/**
	 * @param datePattern the datePattern to set
	 */
	public void setDatePattern(String datePattern) {
		this.datePattern = datePattern;
	}

}
