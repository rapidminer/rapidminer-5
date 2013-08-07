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


/** Reference to a database table with name, schema, and database
 * 
 *  This class can actually contain more meta data, including comments. 
 *  
 * @author Simon Fischer
 *
 */
public class TableName {

	private final String tableName;
	private final String schema;
	private final String catalog;
	
	private String comment;
	
	/** Uses default schema and catalog. */
	public TableName(String tableName) {
		this(tableName, null, null);
	}
	
	public TableName(String tableName, String schemaName, String catalogName) {
		this.tableName = tableName;
		this.schema = schemaName;
		this.catalog = catalogName;
	}

	public String getTableName() {
		return tableName;
	}

	public String getSchema() {
		return schema;
	}

	public String getCatalog() {
		return catalog;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((catalog == null) ? 0 : catalog.hashCode());
		result = prime * result + ((schema == null) ? 0 : schema.hashCode());
		result = prime * result + ((tableName == null) ? 0 : tableName.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		TableName other = (TableName) obj;
		if (catalog == null) {
			if (other.catalog != null)
				return false;
		} else if (!catalog.equals(other.catalog))
			return false;
		if (schema == null) {
			if (other.schema != null)
				return false;
		} else if (!schema.equals(other.schema))
			return false;
		if (tableName == null) {
			if (other.tableName != null)
				return false;
		} else if (!tableName.equals(other.tableName))
			return false;
		return true;
	}


	@Override
	public String toString() {	
		return (schema != null) ?  (schema+"."+tableName) : tableName;
	}

	public void setComment(String comment) {
		if ((comment != null) && comment.isEmpty()) {
			comment = null;
		}
		this.comment = comment;
	}

	public String getComment() {
		return comment;
	}
}
