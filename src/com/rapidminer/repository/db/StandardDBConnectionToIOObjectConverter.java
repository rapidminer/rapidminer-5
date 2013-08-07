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
package com.rapidminer.repository.db;

import java.util.List;

import com.rapidminer.example.ExampleSet;
import com.rapidminer.operator.IOObject;
import com.rapidminer.operator.OperatorCreationException;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.io.DatabaseDataReader;
import com.rapidminer.operator.ports.metadata.AttributeMetaData;
import com.rapidminer.operator.ports.metadata.ExampleSetMetaData;
import com.rapidminer.operator.ports.metadata.MetaData;
import com.rapidminer.tools.OperatorService;
import com.rapidminer.tools.jdbc.ColumnIdentifier;
import com.rapidminer.tools.jdbc.DatabaseHandler;
import com.rapidminer.tools.jdbc.TableName;
import com.rapidminer.tools.jdbc.connection.ConnectionEntry;

/** Simply converts the table to an {@link ExampleSet}.
 * 
 * @author Simon Fischer
 *
 */
public class StandardDBConnectionToIOObjectConverter implements DBConnectionToIOObjectConverter {

	@Override
	public IOObject convert(ConnectionEntry connection, TableName tableName) throws OperatorException {
		DatabaseDataReader reader;
		try {
			reader = OperatorService.createOperator(DatabaseDataReader.class);
		} catch (OperatorCreationException e) {
			throw new OperatorException("Failed to create database reader: "+e, e);
		}
		reader.setParameter(DatabaseHandler.PARAMETER_CONNECTION, connection.getName());
		reader.setParameter(DatabaseHandler.PARAMETER_DEFINE_CONNECTION, DatabaseHandler.CONNECTION_MODES[DatabaseHandler.CONNECTION_MODE_PREDEFINED]);
		reader.setParameter(DatabaseHandler.PARAMETER_TABLE_NAME, tableName.getTableName());
		if (tableName.getSchema() != null) {
			reader.setParameter(DatabaseHandler.PARAMETER_USE_DEFAULT_SCHEMA, String.valueOf(false));
			reader.setParameter(DatabaseHandler.PARAMETER_SCHEMA_NAME, tableName.getSchema());
		}
		reader.setParameter(DatabaseHandler.PARAMETER_DEFINE_QUERY, DatabaseHandler.QUERY_MODES[DatabaseHandler.QUERY_TABLE]);

		return reader.read();
	}

	@Override
	public MetaData convertMetaData(ConnectionEntry connection, TableName tableName, List<ColumnIdentifier> columns) {
		ExampleSetMetaData metaData = new ExampleSetMetaData();
		for (ColumnIdentifier column : columns) {
			metaData.addAttribute(new AttributeMetaData(column.getColumnName(), 
					DatabaseHandler.getRapidMinerTypeIndex(column.getSqlType())));
		}
		return metaData;
	}
	
	@Override
	public String getSuffix() {
		return "Example Sets";
	}

}
