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
package com.rapidminer.operator.io;

import java.sql.SQLException;
import java.util.List;

import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.table.DataRowFactory;
import com.rapidminer.example.table.IndexCachedDatabaseExampleTable;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.UserError;
import com.rapidminer.operator.meta.BatchProcessing;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeBoolean;
import com.rapidminer.parameter.ParameterTypeString;
import com.rapidminer.tools.jdbc.DatabaseHandler;
import com.rapidminer.tools.jdbc.connection.ConnectionEntry;
import com.rapidminer.tools.jdbc.connection.ConnectionProvider;


/**
 * <p>This operator reads an {@link com.rapidminer.example.ExampleSet} from an SQL
 * database. The data is load from a single table which is defined with the
 * table name parameter. Please note that table and column names are
 * often case sensitive. Databases may behave differently here.</p>
 * 
 * <p>The most convenient way of defining the necessary parameters is the 
 * configuration wizard. The most important parameters (database URL and user name) will
 * be automatically determined by this wizard and it is also possible to define
 * the special attributes like labels or ids.</p>
 * 
 * <p>In contrast to the DatabaseExampleSource operator, which loads the data into
 * the main memory, this operator keeps the data in the database and performs
 * the data reading in batches. This allows RapidMiner to access data sets of
 * arbitrary sizes without any size restrictions.</p>
 * 
 * <p>Please note the following important restrictions and notes:
 * <ul>
 * <li>only manifested tables (no views) are allowed as the base for this data caching operator,</li> 
 * <li>if no primary key and index is present, a new column named RM_INDEX is created and automatically used as primary key,</li>
 * <li>if a primary key is already present in the specified table, a new table named RM_MAPPED_INDEX is created mapping a new index column RM_INDEX to the original primary key.</li>
 * <li>users can provide the primary key column RM_INDEX themself which then has to be an integer valued index attribute, counting starts with 1 without any gaps or missing values for all rows</li>
 * </ul>
 * Beside the new index column or the mapping table creation <em>no writing actions</em> are performed
 * in the database. Moreover, <em>data sets built on top of a cached database table do not support 
 * writing actions at all</em>. Users have to materialize the data, change it, and write it 
 * back into a new table of the database (e.g.with the {@link DatabaseExampleSetWriter}. If
 * the data set is large, users can employ the operator {@link BatchProcessing} for splitting up
 * this data change task.
 * </p>
 * 
 * @author Ingo Mierswa, Tobias Malbrecht
 */
public class CachedDatabaseExampleSource extends AbstractExampleSource implements ConnectionProvider {
	
	public static final String PARAMETER_RECREATE_INDEX = "recreate_index";	

	private DatabaseHandler databaseHandler;

	public CachedDatabaseExampleSource(OperatorDescription description) {
		super(description);
	}

	@Override
	public ExampleSet createExampleSet() throws OperatorException {		
		try {
			databaseHandler = DatabaseHandler.getConnectedDatabaseHandler(this);
			String tableName = getParameterAsString(DatabaseHandler.PARAMETER_TABLE_NAME);
			boolean recreateIndex = getParameterAsBoolean(PARAMETER_RECREATE_INDEX);
			IndexCachedDatabaseExampleTable table = new IndexCachedDatabaseExampleTable(databaseHandler, tableName, DataRowFactory.TYPE_DOUBLE_ARRAY, recreateIndex, this);
			// TODO copy functionality from ResultSetExampleSource and remove ResultSetExampleSource!
			return ResultSetExampleSource.createExampleSet(table, this);
		} catch (SQLException e) {
			throw new UserError(this, e, 304, e.getMessage());
		}
	}

	
	@Override
	public void processFinished() {
		disconnect();
	}

	private void disconnect() {    	
		// close database connection
		if (databaseHandler != null) {
			try {
				databaseHandler.disconnect();
				databaseHandler = null;
			} catch (SQLException e) {
				logWarning("Cannot disconnect from database: " + e);
			}
		}        
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();
		types.addAll(DatabaseHandler.getConnectionParameterTypes(this));
		types.addAll(DatabaseHandler.getQueryParameterTypes(this, true));
		types.add(new ParameterTypeBoolean(PARAMETER_RECREATE_INDEX, "Indicates if a recreation of the index or index mapping table should be forced.", false));
		ParameterType type = new ParameterTypeString(ResultSetExampleSource.PARAMETER_LABEL_ATTRIBUTE, "The (case sensitive) name of the label attribute");
		type.setExpert(false);
		types.add(type);
		types.add(new ParameterTypeString(ResultSetExampleSource.PARAMETER_ID_ATTRIBUTE, "The (case sensitive) name of the id attribute"));
		types.add(new ParameterTypeString(ResultSetExampleSource.PARAMETER_WEIGHT_ATTRIBUTE, "The (case sensitive) name of the weight attribute"));
		return types;
	}

	@Override
	public ConnectionEntry getConnectionEntry() {
		return DatabaseHandler.getConnectionEntry(this);
	}
}
