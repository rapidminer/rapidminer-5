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
package com.rapidminer.example.table;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.logging.Level;

import com.rapidminer.example.Attribute;
import com.rapidminer.tools.LogService;
import com.rapidminer.tools.jdbc.DatabaseHandler;


/**
 * This class is another data supplier for example sets. For performance reasons
 * one should use a {@link MemoryExampleTable} if the data is small enough for
 * the main memory. Additionally, directly working on databases is highly experimental and
 * therefore usually not recommended.
 * 
 * @author Ingo Mierswa
 */
public class DatabaseExampleTable extends AbstractExampleTable {

    private static final long serialVersionUID = -3683705313093987482L;

	private transient ResultSet resultSet;
    
	private transient DatabaseHandler databaseHandler;

	private transient Statement statement;
	
	private String tableName;

	private int size = 0;
    
	private DatabaseExampleTable(List<Attribute> attributes, DatabaseHandler databaseHandler, String tableName) throws SQLException {
		super(attributes);
		this.databaseHandler = databaseHandler;
		this.tableName = tableName;
		this.resetResultSet();
	}

	public static DatabaseExampleTable createDatabaseExampleTable(DatabaseHandler databaseHandler, String tableName) throws SQLException {
		// derive attribute list
    	Statement statement = databaseHandler.createStatement(false);
        ResultSet rs = statement.executeQuery(databaseHandler.getStatementCreator().makeSelectEmptySetStatement(tableName));
		List<Attribute> attributes = DatabaseHandler.createAttributes(rs);
		rs.close();
		statement.close();
		
		// create database example table
		DatabaseExampleTable table = new DatabaseExampleTable(attributes, databaseHandler, tableName);
		return table;
	}
    
    private void resetResultSet() throws SQLException {
    	if (statement != null) {
    		statement.close();
    		statement = null;
    	}
		this.statement = this.databaseHandler.createStatement(true, true);
        this.resultSet = this.statement.executeQuery(databaseHandler.getStatementCreator().makeSelectAllStatement(tableName));
//        		"SELECT * FROM " + 
//        		databaseHandler.getProperties().getIdentifierQuoteOpen() + 
//        		tableName + 
//        		databaseHandler.getProperties().getIdentifierQuoteClose());
    }
    
	public DataRowReader getDataRowReader() {
		try {
            return new DatabaseDataRowReader(resultSet);
		} catch (SQLException e) {
			throw new RuntimeException("Error while creating database DataRowReader: " + e, e);
		}
	}

	/**
	 * Returns the data row with the desired row index.
	 */
	public DataRow getDataRow(int index) {
        try {
            this.resultSet.absolute(index + 1);
            DatabaseDataRow dataRow = new DatabaseDataRow(resultSet);
            return dataRow;
        } catch (SQLException e) {
            //LogService.getGlobal().log("Cannot retrieve data row with absolute row index: " + e.getMessage(), LogService.WARNING);
            LogService.getRoot().log(Level.WARNING, "com.rapidminer.example.table.DatabaseExampleTable.retrieving_data_row_error", e.getMessage());
        }
        return null;
	}

	@Override
	public int addAttribute(Attribute attribute) {
		int index = super.addAttribute(attribute);
		
        // will be invoked by super constructor, hence this check
        if (databaseHandler == null)
			return index;
        
		try {
            close();
			databaseHandler.addColumn(attribute, tableName);
            resetResultSet();
		} catch (SQLException e) {
			throw new RuntimeException("Error while adding a column '" + attribute.getName() + "'to database: " + e, e);
		}
		return index;
	}

	@Override
	public void removeAttribute(Attribute attribute) {
		super.removeAttribute(attribute);
		try {
            close();
			databaseHandler.removeColumn(attribute, tableName);
            resetResultSet();
		} catch (SQLException e) {
			throw new RuntimeException("Error while removing a column '"+attribute.getName()+"' from database: " + e, e);
		}
	}

	public int size() {
		if (this.size < 0) {
			try {
				Statement countStatement = this.databaseHandler.createStatement(false);
				//String countQuery = "SELECT count(*) FROM " + databaseHandler.getProperties().getIdentifierQuoteOpen() + tableName + databaseHandler.getProperties().getIdentifierQuoteClose();
				String countQuery = databaseHandler.getStatementCreator().makeSelectSizeStatement(tableName);
				ResultSet countResultSet = countStatement.executeQuery(countQuery);
				countResultSet.next();
				this.size = countResultSet.getInt(1);
				countResultSet.close();
				countStatement.close();
			} catch (SQLException e) {
				// do nothing
			}
		}
        return this.size;
	}
    
    private void close() {
        if (this.statement != null) {
            try {
                this.statement.close();
                this.statement = null;
            } catch (SQLException e) {
                //LogService.getGlobal().log("DatabaseExampleTable: cannot close result set: " + e.getMessage(), LogService.WARNING);
            	LogService.getRoot().log(Level.WARNING, "com.rapidminer.example.table.DatabaseExampleTable.closing_result_set_error", e.getMessage());
            }
        }
    }
    
    @Override
	protected void finalize() {
        close();
    }
}
