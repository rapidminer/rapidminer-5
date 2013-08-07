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
package com.rapidminer.operator.nio;

import javax.swing.table.AbstractTableModel;

import org.apache.poi.hssf.usermodel.HSSFDateUtil;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;

import com.rapidminer.operator.nio.model.ExcelResultSetConfiguration;
import com.rapidminer.tools.Tools;

/** Returns values backed by an operned excel workbook.
 * 
 * @author Simon Fischer, Marco Boeck
 *
 */
public class Excel2007SheetTableModel extends AbstractTableModel {
	private static final long serialVersionUID = 1L;

	private Sheet sheet;

	private ExcelResultSetConfiguration config;
	
	public Excel2007SheetTableModel(Sheet sheet) {
		this.sheet = sheet;
	}

	@Override
	public Object getValueAt(int rowIndex, int columnIndex) {
		Cell cell;
		if (config != null) {
			Row row = sheet.getRow(rowIndex + config.getRowOffset());
			if (row == null) {
				return null;
			}
			cell = row.getCell(columnIndex + config.getColumnOffset());
		} else {
			Row row = sheet.getRow(rowIndex);
			if (row == null) {
				return null;
			}
			cell = row.getCell(columnIndex);
		}
		if (cell == null) {
			return null;
		}
		if (cell.getCellType() == Cell.CELL_TYPE_BOOLEAN) {
			return cell.getBooleanCellValue();
		} else if (cell.getCellType() == Cell.CELL_TYPE_STRING) {
			return cell.getStringCellValue();
		} else if (cell.getCellType() == Cell.CELL_TYPE_NUMERIC) {
			if (HSSFDateUtil.isCellDateFormatted(cell)) {
				return cell.getDateCellValue();
			} else {
				return cell.getNumericCellValue();
			}
		} else if (cell.getCellType() == Cell.CELL_TYPE_ERROR) {
			return cell.getErrorCellValue();
		} else if (cell.getCellType() == Cell.CELL_TYPE_FORMULA) {
			return cell.getNumericCellValue();
		} else {
			// last resort, should not come to this
			// maybe return null?
			return "";
		}
	}

	@Override
	public int getRowCount() {
		if (config != null) {
			return config.getRowLast() - config.getRowOffset() + 1;
		} else {
			return sheet.getLastRowNum()+1;
		}
	}

	@Override
	public int getColumnCount() {
		if (config != null) {
			return config.getColumnLast() - config.getColumnOffset() + 1;
		} else {
			Row row = sheet.getRow(sheet.getFirstRowNum());
			if (row == null) {
				return 0;
			} else {
				return row.getLastCellNum();
			}
		}
	}

	@Override
	public String getColumnName(int columnIndex) {
		if (config != null) {
			return Tools.getExcelColumnName(columnIndex + config.getColumnOffset());
		} else {
			return Tools.getExcelColumnName(columnIndex);
		}
	}

	@Override
	public Class<?> getColumnClass(int columnIndex) {
		return String.class;
	}

	@Override
	public boolean isCellEditable(int rowIndex, int columnIndex) {
		return false;
	}
}
