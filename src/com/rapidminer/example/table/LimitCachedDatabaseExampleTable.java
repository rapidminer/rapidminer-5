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
import java.util.ArrayList;
import java.util.List;

import com.rapidminer.example.Attribute;
import com.rapidminer.tools.jdbc.DatabaseHandler;

/**
 * This class is another data supplier for example sets. For performance reasons
 * one should use a {@link MemoryExampleTable} if the data is small enough for
 * the main memory.
 * 
 * @author Ingo Mierswa, Simon Fischer
 */
public class LimitCachedDatabaseExampleTable extends AbstractExampleTable {
	
	private static final long serialVersionUID = -3514641049341063136L;

	private static final int DEFAULT_BATCH_SIZE = 1500; // must be larger than 1000 due to plotter sampling
		
	private class CachedDataRowReader implements DataRowReader {

		private int currentTotalCursor = 0;
		
		public boolean hasNext() {
			return this.currentTotalCursor < size();
		}

		public DataRow next() {
			DataRow dataRow = getDataRow(currentTotalCursor);
			this.currentTotalCursor++;
			return dataRow;
		}

		/** Will throw a new {@link UnsupportedOperationException} since {@link DataRowReader} does not have
		 *  to implement remove. */
		public void remove() {
			throw new UnsupportedOperationException("The method 'remove' is not supported by DataRowReaders on databases!");
		}	
	}
	
	private DatabaseHandler databaseHandler;
	
	private String tableName;
	
	private MemoryExampleTable batchExampleTable;
	
	private int currentBatchStartCursor = -1;
	
	private int size = -1;
	
	private int dataManagementType;
	
	public LimitCachedDatabaseExampleTable(DatabaseHandler databaseHandler, String tableName, int dataManagementType) throws SQLException {
		super(new ArrayList<Attribute>());
		this.databaseHandler = databaseHandler;
		this.tableName = tableName;
		this.dataManagementType = dataManagementType;
		
		// first: add attributes
		initAttributes();
		
		// second: create batch table
		this.updateBatchAndCursors(0);
	}
	
	private void initAttributes() throws SQLException {
		Statement attributeStatement = this.databaseHandler.createStatement(false);
		String limitedQuery = databaseHandler.getStatementCreator().makeSelectEmptySetStatement(tableName);
		//"SELECT * FROM " + databaseHandler.getProperties().getIdentifierQuoteOpen() + tableName + databaseHandler.getProperties().getIdentifierQuoteClose() + " LIMIT 1";
		ResultSet attributeResultSet = attributeStatement.executeQuery(limitedQuery);
		
		addAttributes(DatabaseHandler.createAttributes(attributeResultSet));

		attributeResultSet.close();
		attributeStatement.close();
	}
	
    private void updateBatchAndCursors(int desiredRow) throws SQLException {
    	// simple fetching strategy...
    	boolean newBatch = false;
    	int newOffset = this.currentBatchStartCursor;
    	if (desiredRow > this.currentBatchStartCursor + (int)(0.9d * DEFAULT_BATCH_SIZE)) { // reaching the upper end
    		newOffset = desiredRow - (int)(0.1 * DEFAULT_BATCH_SIZE);
    		newBatch = true;
    	} else if (desiredRow < this.currentBatchStartCursor) { // reaching the lower end
    		newOffset = desiredRow - (int)(0.7 * DEFAULT_BATCH_SIZE);
    		newBatch = true;
    	}
    	
    	if (newOffset < 0) {
    		newOffset = 0;
    		newBatch = true;
    	}
    	
    	// retrieve new batch
    	if (newBatch) {
    		Statement batchStatement = this.databaseHandler.createStatement(false);
    		String limitedQuery = "SELECT * FROM " + databaseHandler.getStatementCreator().makeIdentifier(tableName) + " LIMIT " + DEFAULT_BATCH_SIZE + " OFFSET " + newOffset;
    		ResultSet batchResultSet = batchStatement.executeQuery(limitedQuery);
    		this.batchExampleTable = createExampleTableFromBatch(batchResultSet);
    		batchResultSet.close();
    		batchStatement.close();
    		this.currentBatchStartCursor = newOffset;
    	}
    }
    
    private MemoryExampleTable createExampleTableFromBatch(ResultSet batchResultSet) {
    	List<Attribute> attributes = new ArrayList<Attribute>(getAttributes().length);
    	for (Attribute attribute : getAttributes()) {
    		attributes.add(attribute);
    	}
		DataRowReader reader = new ResultSetDataRowReader(new DataRowFactory(dataManagementType, '.'), attributes, batchResultSet);
		return new MemoryExampleTable(attributes, reader);
    }
	
	public DataRow getDataRow(int index) {
		try {
			updateBatchAndCursors(index);
			return new NonWritableDataRow(this.batchExampleTable.getDataRow(index - this.currentBatchStartCursor));
		} catch (SQLException e) {
			throw new RuntimeException("Cannot retrieve data from database: " + e);
		}
	}

	public DataRowReader getDataRowReader() {
		return new CachedDataRowReader();
	}

	public int size() {
		if (this.size < 0) {
			try {
				Statement countStatement = this.databaseHandler.createStatement(false);
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
}
