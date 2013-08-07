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
import java.util.Set;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.UserError;
import com.rapidminer.operator.tools.AttributeSubsetSelector;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.tools.jdbc.DatabaseHandler;
import com.rapidminer.tools.jdbc.TableName;
import com.rapidminer.tools.jdbc.connection.ConnectionEntry;
import com.rapidminer.tools.jdbc.connection.ConnectionProvider;

/**
 * <p>
 * This operator updates an {@link ExampleSet} in an SQL
 * database. The user can specify the database connection, a table name and the ID column name(s).
 * Note that {@link ExampleSet} rows where there is no match for the ID column(s) a new row will be inserted.
 * </p>
 * 
 * <p>
 * The most convenient way of defining the necessary parameters is the
 * configuration wizard. The most important parameters (database URL and user
 * name) will be automatically determined by this wizard. At the end, you only
 * have to define the table name and then you are ready.
 * 
 * @author Marco Boeck
 */
public class DatabaseExampleSetUpdater extends AbstractExampleSetWriter implements ConnectionProvider {

	/** Select the ID attributes */
	private final AttributeSubsetSelector attributeSelector = new AttributeSubsetSelector(this, getInputPorts().getPortByIndex(0));

	
	public DatabaseExampleSetUpdater(OperatorDescription description) {
		super(description);
	}


	@Override
	public ExampleSet write(ExampleSet exampleSet) throws OperatorException {
		try {
			Set<Attribute> idAttributeSet = attributeSelector.getAttributeSubset(exampleSet, true);
			DatabaseHandler databaseHandler = DatabaseHandler.getConnectedDatabaseHandler(this);
			TableName selectedTableName = DatabaseHandler.getSelectedTableName(this);
			for (Attribute idAtt : idAttributeSet) {
				// id attributes exist?
				if (idAtt == null) {
					throw new UserError(this, 129);
				}
			}
			databaseHandler.updateTable(exampleSet, selectedTableName, idAttributeSet, getLogger());
			
			databaseHandler.disconnect();
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
		types.addAll(attributeSelector.getParameterTypes());
		
		return types;
	}

	@Override
	public ConnectionEntry getConnectionEntry() {
		return DatabaseHandler.getConnectionEntry(this);
	}
}
