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
package com.rapidminer.gui.tools;

import javax.swing.Icon;
import javax.swing.table.DefaultTableModel;

import com.rapidminer.tools.Tools;
import com.rapidminer.tools.jdbc.DriverInfo;
import com.rapidminer.tools.jdbc.JDBCProperties;

/** This panel shows information about the available JDBC drivers. 
 * 
 *  @author Ingo Mierswa
 */
public class JDBCDriverTableModel extends DefaultTableModel {

	private static final long serialVersionUID = 9211315720113090453L;

	private static final String[] COLUMN_NAMES = new String[] {
		"Name", "Driver", "Available"
	};
	
	private static final String OK_ICON_NAME = "ok.png";
	private static final String ERROR_ICON_NAME = "error.png";
	
	private static Icon OK_ICON = null;
	private static Icon ERROR_ICON = null;
	
	static {
		OK_ICON = SwingTools.createIcon("16/" + OK_ICON_NAME);
		ERROR_ICON = SwingTools.createIcon("16/" + ERROR_ICON_NAME);
	}
	
	private transient DriverInfo[] driverInfos;
	
	public JDBCDriverTableModel(DriverInfo[] driverInfos) {
		this.driverInfos = driverInfos;
	}
	
	@Override
	public String getColumnName(int column) {
		return COLUMN_NAMES[column];
	}
	
	@Override
	public int getColumnCount() {
		return COLUMN_NAMES.length;
	}

	@Override
	public int getRowCount() {
		if (driverInfos != null)
			return driverInfos.length;
		else
			return 0;
	}

	@Override
	public Object getValueAt(int rowIndex, int columnIndex) {
		DriverInfo info = driverInfos[rowIndex];
		switch (columnIndex) {
		case 0:
			return info.getShortName();
		case 1:			
			if (info.getClassName() != null) {
				return info.getClassName();
			} else {
				JDBCProperties props = info.getProperties();
				if (props == null) {
					return "Unknown";
				} else {
					return Tools.toString(props.getDriverClasses());
				}
			}
		case 2:
			if (info.getClassName() != null) {
				if (OK_ICON != null) {
					return OK_ICON;
				} else {
					return "Ok";
				}
			} else {
				if (ERROR_ICON != null) {
					return ERROR_ICON;
				} else {
					return "No Driver Available";
				}
			}
		default:
			return null;
		}
	}
	
	@Override
	public boolean isCellEditable(int row, int column) {
		return false;
	}
}
