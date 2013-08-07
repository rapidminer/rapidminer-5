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

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;

/** Type information as reported by database meta data {@link DatabaseMetaData#getTypeInfo()}. 
 * 
 * @author Simon Fischer
 * */
public class DataTypeSyntaxInformation {
	private final String literalPrefix;
	private final String literalSuffix;
	private final int dataType;
	private final String typeName;
	private String createParams;
	private long precision;

	public DataTypeSyntaxInformation(ResultSet typesResult) throws SQLException {
		typeName = typesResult.getString("TYPE_NAME");
		dataType = typesResult.getInt("DATA_TYPE");
		literalPrefix = typesResult.getString("LITERAL_PREFIX");
		literalSuffix = typesResult.getString("LITERAL_SUFFIX");
		precision = typesResult.getLong("PRECISION");
		createParams = typesResult.getString("CREATE_PARAMS");
	}

	public String getTypeName() {
		return typeName;
	}

	public int getDataType() {
		return dataType;
	}

	@Override
	public String toString() {
		return getTypeName() + " (prec=" + precision + "; params=" + createParams + ")";
	}

	public long getPrecision() {
		return precision;
	}

	public String getLiteralPrefix() {
		return literalPrefix;
	}

	public String getLiteralSuffix() {
		return literalSuffix;
	}
}
