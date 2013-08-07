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
package com.rapidminer.tools.jdbc;

/**
 * This class is used to identify a column
 * 
 *
 * @author Ingo Mierswa
 */
public class ColumnIdentifier {

	private final TableName tableName;
	private final String columnName;
	private final int sqlType;
	private final String sqlTypeName;
	private final String remarks;

	private DatabaseHandler databaseHandler;
	
	public ColumnIdentifier(DatabaseHandler databaseHandler, TableName tableName, String columnName, int sqlType, String sqlTypeName, String remarks) {
		this.tableName = tableName;
		this.columnName = columnName;
		this.databaseHandler = databaseHandler;
		this.sqlType = sqlType;
		this.sqlTypeName = sqlTypeName;
		this.remarks = remarks;
	}
	
	public TableName getTableName() {
		return this.tableName;
	}
	
	public String getColumnName() {
		return this.columnName;
	}
	
	public String getFullName(boolean singleTable) {
		if (singleTable) {
			return databaseHandler.getStatementCreator().makeIdentifier(this.columnName);
//			properties.getIdentifierQuoteOpen() +
//			this.columnName +
//			properties.getIdentifierQuoteClose();
		} else {
			return databaseHandler.getStatementCreator().makeIdentifier(this.tableName + "." + this.columnName);
//			return 
//			properties.getIdentifierQuoteOpen() +
//			this.tableName +
//			properties.getIdentifierQuoteClose() +
//			"." +
//			properties.getIdentifierQuoteOpen() +
//			this.columnName +
//			properties.getIdentifierQuoteClose();
		}
	}
	
	public String getAliasName(boolean singleTable) {
		if (singleTable) {
			return databaseHandler.getStatementCreator().makeIdentifier(this.columnName);
//			properties.getIdentifierQuoteOpen() +
//			this.columnName +
//			properties.getIdentifierQuoteClose();		 	
		} else {
			return  databaseHandler.getStatementCreator().makeIdentifier(this.tableName+"__"+this.columnName);
//			properties.getIdentifierQuoteOpen() +
//			this.tableName +
//			"__" +
//			this.columnName +
//			properties.getIdentifierQuoteClose();
		}
	}	
	
	/**
	 * @return the columns remarks. Maybe <code>null</code>.
	 */
	public String getRemarks() {
		return remarks;
	}
	
	@Override
	public String toString() {
		return 
		this.tableName +
		"." +
		this.columnName;
	}

	public int getSqlType() {
		return sqlType;
	}

	public String getSqlTypeName() {
		return sqlTypeName;
	}
}
