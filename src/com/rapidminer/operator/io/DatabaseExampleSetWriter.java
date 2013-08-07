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
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.UserError;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeBoolean;
import com.rapidminer.parameter.ParameterTypeCategory;
import com.rapidminer.parameter.ParameterTypeInt;
import com.rapidminer.parameter.ParameterTypeString;
import com.rapidminer.parameter.conditions.BooleanParameterCondition;
import com.rapidminer.tools.jdbc.DatabaseHandler;
import com.rapidminer.tools.jdbc.connection.ConnectionEntry;
import com.rapidminer.tools.jdbc.connection.ConnectionProvider;

/**
 * <p>
 * This operator writes an {@link com.rapidminer.example.ExampleSet} into an SQL
 * database. The user can specify the database connection and a table name.
 * Please note that the table will be created during writing if it does not
 * exist.
 * </p>
 * 
 * <p>
 * The most convenient way of defining the necessary parameters is the
 * configuration wizard. The most important parameters (database URL and user
 * name) will be automatically determined by this wizard. At the end, you only
 * have to define the table name and then you are ready.
 * </p>
 * 
 * <p>
 * This operator only supports the writing of the complete example set
 * consisting of all regular and special attributes and all examples. If this is
 * not desired perform some preprocessing operators like attribute or example
 * filter before applying this operator.
 * </p>
 * 
 * @author Ingo Mierswa, Tobias Malbrecht
 */
public class DatabaseExampleSetWriter extends AbstractExampleSetWriter implements ConnectionProvider {

	/**
	 * The parameter name for &quot;Indicates if an existing table should be
	 * overwritten.&quot;
	 */
	public static final String PARAMETER_OVERWRITE_MODE = "overwrite_mode";

	/**
	 * The parameter name for &quot;Set varchar columns to default length.&quot;
	 */
	public static final String PARAMETER_SET_DEFAULT_VARCHAR_LENGTH = "set_default_varchar_length";

	/** The parameter name for &quot;Default length of varchar columns.&quot; */
	public static final String PARAMETER_DEFAULT_VARCHAR_LENGTH = "default_varchar_length";

	
	/**
	 * The parameter allows to get back the primary keys, which were assigned to the inserted data rows.
	 */
	public static final String PARAMETER_GET_GENERATED_PRIMARY_KEYS = "add_generated_primary_keys";

	/**
	 * The name of the attributes which is added to the example set and which holds the auto generated primary keys.
	 */
	public static final String PARAMETER_GENERATED_KEYS_ATTRIBUTE_NAME = "db_key_attribute_name";

	public static final String PARAMETER_BATCH_SIZE = "batch_size";

	public DatabaseExampleSetWriter(OperatorDescription description) {
		super(description);
	}


	@Override
	public ExampleSet write(ExampleSet exampleSet) throws OperatorException {
		try {
			DatabaseHandler databaseHandler = DatabaseHandler.getConnectedDatabaseHandler(this);
			if (getParameterAsBoolean(PARAMETER_GET_GENERATED_PRIMARY_KEYS)){
				exampleSet  = (ExampleSet)exampleSet.clone();
			}
			try {
				databaseHandler.createTable(exampleSet, DatabaseHandler.getSelectedTableName(this),
						getParameterAsInt(PARAMETER_OVERWRITE_MODE), getApplyCount() == 1,
						getParameterAsBoolean(PARAMETER_SET_DEFAULT_VARCHAR_LENGTH) ? getParameterAsInt(PARAMETER_DEFAULT_VARCHAR_LENGTH) : -1,
						getParameterAsBoolean(PARAMETER_GET_GENERATED_PRIMARY_KEYS),
						getParameterAsString(PARAMETER_GENERATED_KEYS_ATTRIBUTE_NAME),
						getParameterAsInt(PARAMETER_BATCH_SIZE),
						this);
			} finally  {
				databaseHandler.disconnect();
			}
		} catch (SQLException e) {
			throw new UserError(this, e, 304, e.getMessage());
		}
		return exampleSet;
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();
		types.addAll(DatabaseHandler.getConnectionParameterTypes(this));
		types.addAll(DatabaseHandler.getQueryParameterTypes(this, true));
		types.add(new ParameterTypeCategory(PARAMETER_OVERWRITE_MODE,
				"Indicates if an existing table should be overwritten or if data should be appended.", DatabaseHandler.OVERWRITE_MODES,
				DatabaseHandler.OVERWRITE_MODE_NONE));
		types.add(new ParameterTypeBoolean(PARAMETER_SET_DEFAULT_VARCHAR_LENGTH, "Set varchar columns to default length.", false));
		ParameterType type = new ParameterTypeInt(PARAMETER_DEFAULT_VARCHAR_LENGTH, "Default length of varchar columns.", 0, Integer.MAX_VALUE, 128);
		type.registerDependencyCondition(new BooleanParameterCondition(this, PARAMETER_SET_DEFAULT_VARCHAR_LENGTH, true, true));
		types.add(type);

		type = new ParameterTypeBoolean(PARAMETER_GET_GENERATED_PRIMARY_KEYS,
				"Indicates whether a new attribute holding the auto generated primary keys is added to the result set.", false);
		type.setExpert(true);
		types.add(type);

		type = new ParameterTypeString(PARAMETER_GENERATED_KEYS_ATTRIBUTE_NAME, "The name of the attribute for the auto generated primary keys",
				"generated_primary_key", true);
		type.setExpert(true);
		type.registerDependencyCondition(new BooleanParameterCondition(this, PARAMETER_GET_GENERATED_PRIMARY_KEYS, true, true ));
		types.add(type);

		
		type = new ParameterTypeInt(PARAMETER_BATCH_SIZE, "The number of examples which are written at once with one single query to the database. Larger values can greatly improve the speed - too large values however can drastically <i>decrease</i> the performance. Additionally, some databases have restrictions on the maximum number of values written at once.",
				1, Integer.MAX_VALUE, 1, true);
		type.setExpert(true);
		types.add(type);
		return types;
	}

	@Override
	public ConnectionEntry getConnectionEntry() {
		return DatabaseHandler.getConnectionEntry(this);
	}
}
