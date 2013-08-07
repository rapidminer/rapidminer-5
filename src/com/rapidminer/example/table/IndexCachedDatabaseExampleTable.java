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

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.rapidminer.example.Attribute;
import com.rapidminer.tools.LoggingHandler;
import com.rapidminer.tools.jdbc.DatabaseHandler;

/**
 * This class is another data supplier for example sets. For performance reasons
 * one should use a {@link MemoryExampleTable} if the data is small enough for
 * the main memory.
 * 
 * @author Ingo Mierswa, Simon Fischer
 */
public class IndexCachedDatabaseExampleTable extends AbstractExampleTable {
	
	private static final long serialVersionUID = -3514641049341063136L;

	public static final int DEFAULT_BATCH_SIZE = 1500; // must be larger than 1000 due to plotter sampling
		
	public static final String INDEX_COLUMN_NAME = "RM_INDEX";
	
	public static final String MAPPING_TABLE_NAME_PREFIX = "RM_MAPPING_";
	
	
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
	
//	private String openQuote;	
//	private String closeQuote;
	
	private String tableName;
	
	private MemoryExampleTable batchExampleTable;
	
	private int currentBatchStartCursor = -1;
	
	private int size = -1;
	
	private int dataManagementType;
	
	private String mappingTableName;
	
	private String mappingPrimaryKey;
	
	
	public IndexCachedDatabaseExampleTable(DatabaseHandler databaseHandler, String tableName, int dataManagementType, boolean dropMappingTable, LoggingHandler logging) throws SQLException {
		super(new ArrayList<Attribute>());
		this.databaseHandler = databaseHandler;
//		this.openQuote = this.databaseHandler.getProperties().getIdentifierQuoteOpen();
//		this.closeQuote = this.databaseHandler.getProperties().getIdentifierQuoteClose();
		this.tableName = tableName;
		this.dataManagementType = dataManagementType;
		
		// init size
		this.size = getSizeForTable(this.tableName);
				
		// init index
		createIndex(dropMappingTable, logging);
		
		// first: add attributes
		initAttributes();
		
		// second: create batch table
		updateBatchAndCursors(0);
	}
	
	private void createIndex(boolean dropMappingTable, LoggingHandler logging) throws SQLException {
		// check if key and create key or key mapping table if necessary
		String primaryKeyName = getPrimaryKeyName(this.tableName);
		if (primaryKeyName == null) {
			this.mappingTableName = null; // use key created below directly
			this.mappingPrimaryKey = null;
			
			// no key: create RM_INDEX key
			logging.logNote("No primary key found: creating a new primary key with name '" + INDEX_COLUMN_NAME + "' for table '" + tableName + "'. This might take some time...");
			createRMPrimaryKeyIndex(databaseHandler, this.tableName);
			logging.logNote("Creation of primary key '" + INDEX_COLUMN_NAME + "' for table '" + tableName + "' finished.");
		} else {
			// key exists --> check for name --> wrong --> create mapping table
			if (!primaryKeyName.equals(INDEX_COLUMN_NAME)) {
				// init mapping names
				this.mappingTableName = MAPPING_TABLE_NAME_PREFIX + this.tableName;
				this.mappingPrimaryKey = primaryKeyName;
				
				// check if mapping table exists
				Statement statement = this.databaseHandler.createStatement(false);
				boolean exists = false;
				try {
		            // check if table already exists (no exception and more than zero columns :-)
					ResultSet existingResultSet = statement.executeQuery(databaseHandler.getStatementCreator().makeSelectEmptySetStatement(this.mappingTableName));
		            if (existingResultSet.getMetaData().getColumnCount() > 0)
		                exists = true;
					existingResultSet.close();
				} catch (SQLException e) {
					// exception will be throw if table does not exist
				}
				statement.close();
				
				if (exists) {
					int mappingSize = getSizeForTable(this.mappingTableName);
					if (mappingSize != this.size) {
						logging.logWarning("Size of internal mapping table '" + this.mappingTableName + "' and data table '" + this.tableName + "' differs. Recreate new mapping table!");
						dropMappingTable = true;
					}
				}
				
				if (exists && dropMappingTable) {
					// drop mapping table if parameter is set
					String dropSQL = databaseHandler.getStatementCreator().makeDropStatement(this.mappingTableName);
					statement = this.databaseHandler.createStatement(false);
					statement.executeUpdate(dropSQL);
					statement.close();
					exists = false;
				}
				
				if (!exists) {
					// copy key column
					logging.logNote("Primary key '" + primaryKeyName + "' found: creating a new mapping table '" + this.mappingTableName + "' which maps from the RapidMiner index '" + INDEX_COLUMN_NAME + "' to the primary key. This might take some time..."); 
					String copyKeyQuery = 
						"CREATE TABLE " + databaseHandler.getStatementCreator().makeIdentifier(this.mappingTableName)+
						" AS ( SELECT " + databaseHandler.getStatementCreator().makeIdentifier(primaryKeyName) + 
						" FROM " + databaseHandler.getStatementCreator().makeIdentifier(this.tableName)+ " )";

					try {
						statement = this.databaseHandler.createStatement(true);
						try {
							statement.execute(copyKeyQuery);
						}
						finally {
							statement.close();
						}
					}
					catch (SQLException ex) {
						logging.logWarning("Failed to create mapping table using standard method, attempting secondary option");						
						copyKeyQuery = 
								"SELECT " + databaseHandler.getStatementCreator().makeIdentifier(primaryKeyName) +
								" INTO " + databaseHandler.getStatementCreator().makeIdentifier(this.mappingTableName) + 
								" FROM " + databaseHandler.getStatementCreator().makeIdentifier(this.tableName);
						
						statement = this.databaseHandler.createStatement(true);
						statement.execute(copyKeyQuery);
						statement.close();	
					}

					// add new RM_INDEX key
					logging.logNote("Creating new primary key for mapping table '" + this.mappingTableName + "'...");
					createRMPrimaryKeyIndex(databaseHandler, this.mappingTableName);
					logging.logNote("Creation of mapping table '" + this.mappingTableName + "' finished.");
				}
			} else {
				this.mappingTableName = null; // use found key directly
				this.mappingPrimaryKey = null;
			}
		}
	}
	
	/** Subclasses might want to override this method if they do not support auto_increment. */
	protected void createRMPrimaryKeyIndex(DatabaseHandler databaseHandler, String tableName) throws SQLException {
		String addKeyQuery = 
				"ALTER TABLE " + databaseHandler.getStatementCreator().makeIdentifier(tableName)+
				" ADD " + databaseHandler.getStatementCreator().makeIdentifier(INDEX_COLUMN_NAME)+ 
				" INT NOT NULL AUTO_INCREMENT PRIMARY KEY";
			
			try {
			Statement statement = databaseHandler.createStatement(true, true);
				try {
					statement.execute(addKeyQuery);					
				}
				finally {
					statement.close();
				}
			}
			catch (SQLException ex) {
				addKeyQuery = 
						"ALTER TABLE " + databaseHandler.getStatementCreator().makeIdentifier(tableName)+
						" ADD " + databaseHandler.getStatementCreator().makeIdentifier(INDEX_COLUMN_NAME)+ 
						" INT NOT NULL IDENTITY(1,1) PRIMARY KEY";
				
			Statement statement = databaseHandler.createStatement(true, true);
				try {
					statement.execute(addKeyQuery);					
				}
				finally {
					statement.close();
				}
			}
	}
	
	private String getPrimaryKeyName(String tableName) throws SQLException {
	    DatabaseMetaData meta = this.databaseHandler.getConnection().getMetaData();
	    ResultSet primaryKeys = meta.getPrimaryKeys(null, null, tableName);
	    String primaryKeyName = null;
	    while (primaryKeys.next()) {
	    	primaryKeyName = primaryKeys.getString(4); // name is in the fourth column
	    	break;
	    }
	    primaryKeys.close();
	    return primaryKeyName;
	}
	
	private void initAttributes() throws SQLException {
		Statement attributeStatement = this.databaseHandler.createStatement(false);
		String limitedQuery = databaseHandler.getStatementCreator().makeSelectEmptySetStatement(tableName);
		ResultSet attributeResultSet = attributeStatement.executeQuery(limitedQuery);
		
		List<Attribute> attributes = DatabaseHandler.createAttributes(attributeResultSet);
		Iterator<Attribute> a = attributes.iterator();
		while (a.hasNext()) {
			if (a.next().getName().equals(INDEX_COLUMN_NAME))
				a.remove();
		}
		addAttributes(attributes);

		attributeResultSet.close();
		attributeStatement.close();
	}
	
    private void updateBatchAndCursors(int desiredRow) throws SQLException {
    	desiredRow++; // RM starts counting with 0, DB with 1
    	
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
    	
    	if (newOffset < 1) {
    		newOffset = 1;
    		newBatch = true;
    	}
    	
    	// retrieve new batch
    	if (newBatch) {
    		if (this.mappingTableName == null) { // work directly on table
    			Statement batchStatement = this.databaseHandler.createStatement(false);
    			String limitedQuery = 
    				"SELECT * FROM " + databaseHandler.getStatementCreator().makeIdentifier(tableName) + 
    				" WHERE " + databaseHandler.getStatementCreator().makeIdentifier(INDEX_COLUMN_NAME) + " >= " + newOffset +
    				" AND " + databaseHandler.getStatementCreator().makeIdentifier(INDEX_COLUMN_NAME) + " < " + (newOffset + DEFAULT_BATCH_SIZE);
    			ResultSet batchResultSet = batchStatement.executeQuery(limitedQuery);
    			this.batchExampleTable = createExampleTableFromBatch(batchResultSet);
    			batchResultSet.close();
    			batchStatement.close();
    			this.currentBatchStartCursor = newOffset;
    		} else { // work with mapping table
    			Statement batchStatement = this.databaseHandler.createStatement(false);
    			String limitedQuery = 
    				"SELECT * FROM " + databaseHandler.getStatementCreator().makeIdentifier(this.tableName)+ 
    				"," + databaseHandler.getStatementCreator().makeIdentifier(this.mappingTableName) +
    				" WHERE " + databaseHandler.getStatementCreator().makeIdentifier(INDEX_COLUMN_NAME) + " >= " + newOffset +
    				" AND " + databaseHandler.getStatementCreator().makeIdentifier(INDEX_COLUMN_NAME) + " < " + (newOffset + DEFAULT_BATCH_SIZE) +
    				" AND " + databaseHandler.getStatementCreator().makeIdentifier(this.tableName) + "." + databaseHandler.getStatementCreator().makeIdentifier(this.mappingPrimaryKey)+ 
    				" = " + databaseHandler.getStatementCreator().makeIdentifier(this.mappingTableName) + "." + databaseHandler.getStatementCreator().makeIdentifier(this.mappingPrimaryKey);
    			ResultSet batchResultSet = batchStatement.executeQuery(limitedQuery);
    			this.batchExampleTable = createExampleTableFromBatch(batchResultSet);
    			batchResultSet.close();
    			batchStatement.close();
    			this.currentBatchStartCursor = newOffset;
    		}
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
			return new NonWritableDataRow(this.batchExampleTable.getDataRow(index - this.currentBatchStartCursor + 1));
		} catch (SQLException e) {
			throw new RuntimeException("Cannot retrieve data from database: " + e, e);
		}
	}

	public DataRowReader getDataRowReader() {
		return new CachedDataRowReader();
	}

	private int getSizeForTable(String sizeTable) {
		int size = 0;
		try {
			Statement countStatement = this.databaseHandler.createStatement(false);
			String countQuery = "SELECT count(*) FROM " + databaseHandler.getStatementCreator().makeIdentifier(sizeTable);
			ResultSet countResultSet = countStatement.executeQuery(countQuery);
			countResultSet.next();
			size = countResultSet.getInt(1);
			countResultSet.close();
			countStatement.close();
		} catch (SQLException e) {
			// do nothing
		}	
		return size;
	}
	
	public int size() {
        return this.size;
	}
}
