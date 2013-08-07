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

import com.rapidminer.operator.IOObject;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.ports.metadata.MetaData;
import com.rapidminer.tools.jdbc.ColumnIdentifier;
import com.rapidminer.tools.jdbc.TableName;
import com.rapidminer.tools.jdbc.connection.ConnectionEntry;

/** Converts a reference to a table in a database to an {@link IOObject}, e.g. an example set.
 * 
 * @author Simon Fischer
 *
 */
public interface DBConnectionToIOObjectConverter {

	/** Retrieves the actual data and returns it. */
	public IOObject convert(ConnectionEntry connection, TableName tableName) throws OperatorException;
	
	/** Returns meta data describing the entry. */
	public MetaData convertMetaData(ConnectionEntry connection, TableName tableName, List<ColumnIdentifier> columns);

	/** Returns a suffix to be appended to entries in the repository tree to identify the converter. */
	public String getSuffix();
	
}
