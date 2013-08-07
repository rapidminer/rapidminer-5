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

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.logging.Level;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.AttributeRole;
import com.rapidminer.example.Attributes;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.tools.LogService;
import com.rapidminer.tools.Ontology;

/**
 * Helper class to create SQL statements and escape column names.
 * 
 * @author Simon Fischer
 */
public class StatementCreator {

	/**
	 * Maps types as defined by {@link Ontology#ATTRIBUTE_VALUE_TYPE} to syntactical SQL information. Must be
	 * linkedHashMap to ensure prefered types are taken first.
	 */
	private Map<Integer, DataTypeSyntaxInformation> typeMap = new LinkedHashMap<Integer, DataTypeSyntaxInformation>();
	private String identifierQuote;
	private long defaultVarCharLength = -1;

	public StatementCreator(Connection connection) throws SQLException {
		this(connection, -1);
	}

	StatementCreator(Connection connection, long defaultVarcharLength) throws SQLException {
		this.defaultVarCharLength = defaultVarcharLength;
		buildTypeMap(connection);
	}

	public void setDefaultVarcharLength(long defaultVarcharLength) {
		this.defaultVarCharLength = defaultVarcharLength;
	}

	// initialization

	private void buildTypeMap(Connection con) throws SQLException {
		DatabaseMetaData dbMetaData = con.getMetaData();
		this.identifierQuote = dbMetaData.getIdentifierQuoteString();

		//LogService.getRoot().fine("Identifier quote character is: " + this.identifierQuote);
		LogService.getRoot().log(Level.FINE, "com.rapidminer.tools.jdbc.StatementCreator.initialization_of_quote_character", this.identifierQuote);
		// Maps java sql Types to data types as reported by the sql driver
		Map<Integer, DataTypeSyntaxInformation> dataTypeToMDMap = new HashMap<Integer, DataTypeSyntaxInformation>();
		ResultSet typesResult = dbMetaData.getTypeInfo();
		while (typesResult.next()) {
			DataTypeSyntaxInformation dtmd = new DataTypeSyntaxInformation(typesResult);
			// for duplicate keys, we go with the first match.
			// By definition of getTypeInfo() the closest match will be first.
			if (!dataTypeToMDMap.containsKey(dtmd.getDataType())) {
				dataTypeToMDMap.put(dtmd.getDataType(), dtmd);
			}
		}

		registerSyntaxInfo(Ontology.NOMINAL, dataTypeToMDMap, Types.VARCHAR);
		registerSyntaxInfo(Ontology.STRING, dataTypeToMDMap, Types.CLOB, Types.BLOB, Types.LONGVARCHAR, Types.LONGNVARCHAR, Types.VARCHAR);
		registerSyntaxInfo(Ontology.REAL, dataTypeToMDMap, Types.DOUBLE, Types.REAL, Types.FLOAT);
		registerSyntaxInfo(Ontology.NUMERICAL, dataTypeToMDMap, Types.DOUBLE, Types.REAL, Types.FLOAT);
		registerSyntaxInfo(Ontology.INTEGER, dataTypeToMDMap, Types.INTEGER);
		registerSyntaxInfo(Ontology.DATE, dataTypeToMDMap, Types.DATE, Types.TIMESTAMP);
		registerSyntaxInfo(Ontology.DATE_TIME, dataTypeToMDMap, Types.TIMESTAMP);
		registerSyntaxInfo(Ontology.TIME, dataTypeToMDMap, Types.TIME, Types.TIMESTAMP);
		registerSyntaxInfo(Ontology.ATTRIBUTE_VALUE, dataTypeToMDMap, Types.DOUBLE, Types.REAL, Types.FLOAT); // fallback;
																												// same
																												// will
																												// be
																												// used
																												// by
																												// actual
																												// insertion
																												// code
		registerSyntaxInfo(Ontology.BINOMINAL, dataTypeToMDMap, Types.VARCHAR); // fallback; same will be used by actual
																				// insertion code
	}

	private void registerSyntaxInfo(int attributeType, Map<Integer, DataTypeSyntaxInformation> dataTypeToMDMap, int... possibleDataTypes) throws SQLException {
		for (int i : possibleDataTypes) {
			DataTypeSyntaxInformation si = dataTypeToMDMap.get(i);
			if (si != null) {
				typeMap.put(attributeType, si);
				//LogService.getRoot().fine("Mapping " + Ontology.ATTRIBUTE_VALUE_TYPE.mapIndex(attributeType) + " to " + si);
				LogService.getRoot().log(Level.FINE, 
						"com.rapidminer.tools.jdbc.StatementCreator.mapping_ontology_value_type",
						new Object[] {Ontology.ATTRIBUTE_VALUE_TYPE.mapIndex(attributeType), si});
				return;
			}
		}
		//LogService.getRoot().warning("No SQL value type found for " + Ontology.ATTRIBUTE_VALUE_TYPE.mapIndex(attributeType));
		LogService.getRoot().log(Level.WARNING,
				"com.rapidminer.tools.jdbc.StatementCreator.no_sql_value_type_found",
				Ontology.ATTRIBUTE_VALUE_TYPE.mapIndex(attributeType));
	}

	// mapping types

	/** Maps attribute types as defined by {@link Ontology#ATTRIBUTE_VALUE_TYPE} to SQL types. */
	private String mapAttributeTypeToSQLDataType(int type) {
		DataTypeSyntaxInformation typeStr = getSQLTypeForRMValueType(type);
		return typeStr.getTypeName();
	}

	/** Maps RM ontology attribute value types to syntax information as reported by the database driver. */
	DataTypeSyntaxInformation getSQLTypeForRMValueType(int type) {
		int parent = type;
		while (parent != Ontology.ATTRIBUTE_VALUE) {
			DataTypeSyntaxInformation si = typeMap.get(parent);
			if (si != null) {
				return si;
			} else {
				parent = Ontology.ATTRIBUTE_VALUE_TYPE.getParent(parent);
			}
		}
		throw new NoSuchElementException("No SQL type mapped to attribute type " + Ontology.ATTRIBUTE_VALUE_TYPE.mapIndex(type));
	}

	// statement creation

	/**
	 * Creates an SQL statement for creating a table where each attribute is mapped to a column of an appropriate type.
	 * 
	 * @param defaultVarcharLength
	 * @throws SQLException
	 */
	public String makeTableCreator(Attributes attributes, TableName tableName, int defaultVarcharLength) throws SQLException {
		this.defaultVarCharLength = defaultVarcharLength;
		// define all attribute names and types
		StringBuilder b = new StringBuilder();
		b.append("CREATE TABLE ");
		b.append(makeIdentifier(tableName));
		b.append(" (");
		Iterator<AttributeRole> a = attributes.allAttributeRoles();
		boolean first = true;
		while (a.hasNext()) {
			if (!first)
				b.append(", ");
			first = false;
			AttributeRole attributeRole = a.next();
			makeColumnCreator(b, attributeRole);
		}

		// use id as primary key
		Attribute idAttribute = attributes.getId();
		if (idAttribute != null) {
			b.append(", PRIMARY KEY( ");
			b.append(makeColumnIdentifier(idAttribute));
			b.append(")");
		}
		b.append(")");
		return b.toString();
	}

	public String makeIdentifier(TableName tableName) {
		if (tableName.getSchema() != null) {
			return makeIdentifier(tableName.getSchema()) + "." + makeIdentifier(tableName.getTableName());
		} else {
			return makeIdentifier(tableName.getTableName());
		}
	}
	
	/** Quotes and escapes the given name such that it can be used as an SQL table or column identifier. */
	public String makeIdentifier(String identifier) {
		// if (isLegalIdentifier(identifier)) {
		// return identifier;
		// }
		if (identifier == null) {
			throw new NullPointerException("Identifier must not be null");
		}
		// for performance reasons, don't use regexp when identifier has length 1
		if (identifierQuote != null) {
			switch (identifierQuote.length()) {
			case 0:
				break;
			case 1:
				identifier = identifier.replace(identifierQuote.charAt(0), '_');
				break;
			default:
				identifier = identifier.replace(identifierQuote, "_");
			}
		}
		return this.identifierQuote +
				identifier +
				this.identifierQuote;
	}

	/*
	 * private boolean isLegalIdentifier(String identifier) { for (char c : identifier.toCharArray()) { if
	 * (!Character.isLetter(c) && (c != '_')) { return false; } } return true; }
	 */

	/**
	 * Creates an SQL INSERT statement for filling attributes into a table. This can be used to make a prepared
	 * statement where the i-th parameter is mapped to the i-th attribute in the example set.
	 */
	public String makeInsertStatement(TableName tableName, ExampleSet exampleSet) throws SQLException {
		return makeInsertStatement(tableName, exampleSet, 1);
	}
	
	public String makeInsertStatement(TableName tableName, ExampleSet exampleSet, int batchSize) throws SQLException {
		StringBuilder b = new StringBuilder("INSERT INTO ");
		b.append(makeIdentifier(tableName));
		b.append(" (");
		Iterator<Attribute> a = exampleSet.getAttributes().allAttributes();
		boolean first = true;
		while (a.hasNext()) {
			Attribute attribute = a.next();
			if (!first)
				b.append(", ");
			b.append(makeColumnIdentifier(attribute));
			first = false;
		}
		b.append(")");
		b.append(" VALUES ");
		int size = exampleSet.getAttributes().allSize();
		for (int r = 0; r < batchSize; ++r) {
			if (r != 0) {
				b.append(",");
			}
			b.append("(");
			for (int i = 0; i < size; i++) {
				if (i != 0)
					b.append(", ");
				b.append("?");
			}
			b.append(")");
		}
		return b.toString();
	}

	/**
	 * This will create an alteration statement for the given role. That can be used for
	 * constructing an table alteration
	 */
	public void makeColumnAlter(StringBuilder b, AttributeRole role) throws SQLException {
		b.append("ALTER ");
		makeColumnCreator(b, role);
	}
	/**
	 * This will create an add statement for the given role that can be used for constructing
	 * a table alteration.
	 */
	public void makeColumnAdd(StringBuilder b, AttributeRole role) throws SQLException {
		b.append("ADD ");
		makeColumnCreator(b, role);
	}
	/**
	 * This will create an add statement for the given role that can be used for constructing
	 * a table alteration.
	 */
	public void makeColumnDrop(StringBuilder b, Attribute attribute) throws SQLException {
		b.append("DROP ");
		b.append(makeColumnIdentifier(attribute));
		b.append(" CASCADE");
	}
	/**
	 * Makes an SQL fragment that can be used for creating a table column. Basically, this is the quoted column name
	 * plus the SQL data type.
	 * 
	 * @throws SQLException
	 */
	private void makeColumnCreator(StringBuilder b, AttributeRole role) throws SQLException {
		final Attribute attribute = role.getAttribute();
		b.append(makeColumnIdentifier(attribute));
		b.append(" ");
		DataTypeSyntaxInformation si = getSQLTypeForRMValueType(attribute.getValueType());
		b.append(si.getTypeName());

		// varchar length parameter
		if (attribute.isNominal()) {
			if ((si.getPrecision() > 0) && (defaultVarCharLength > si.getPrecision())) {
				throw new SQLException("minimum requested varchar length >" + si.getPrecision() + " which is the maximum length for columns of SQL type " + si.getTypeName());
			}
			int maxLength = 1;
			for (String value : attribute.getMapping().getValues()) {
				final int length = value.length();
				if (length > maxLength) {
					maxLength = length;
					if ((si.getPrecision() > 0) && (length > si.getPrecision())) {
						throw new SQLException("Attribute " + attribute.getName() + " contains values with length >" + si.getPrecision() + " which is the maximum length for columns of SQL type " + si.getTypeName());
					}
					if ((defaultVarCharLength != -1) && (maxLength > defaultVarCharLength)) {
						throw new SQLException("Attribute " + attribute.getName() + " contains values with length >" + defaultVarCharLength + " which is the requested default varchar length.");
					}
				}
			}
			if (si.getTypeName().toLowerCase().startsWith("varchar")) {
				if (defaultVarCharLength != -1 && defaultVarCharLength < maxLength) {
					// won't happen as an SQLException is thrown earlier in this case
					b.append("(").append(maxLength).append(")");
				} else if (defaultVarCharLength != -1 && maxLength < defaultVarCharLength) {
					b.append("(").append(defaultVarCharLength).append(")");
				} else {
					b.append("(").append(maxLength).append(")");
				}
			}
		}

		// IDs must not be missing
		if (role.isSpecial()) {
			if (role.getSpecialName().equals(Attributes.ID_NAME)) {
				b.append(" NOT NULL");
			}
		}
	}

	/**
	 * Makes an SQL fragment that can be used for creating a table column. Basically, this is the quoted column name
	 * plus the SQL data type.
	 */
	private void makeColumnCreator(StringBuilder b, Attribute attribute) {
		b.append(makeColumnIdentifier(attribute));
		b.append(" ");
		b.append(mapAttributeTypeToSQLDataType(attribute.getValueType()));
	}

	public String makeColumnCreator(Attribute attribute) {
		StringBuilder b = new StringBuilder();
		makeColumnCreator(b, attribute);
		return b.toString();
	}

	/** Quotes and escapes the name of an attribute such that it can be used as an SQL column identifier. */
	public String makeColumnIdentifier(Attribute attribute) {
		return makeIdentifier(attribute.getName());
	}

	/**
	 * DROP TABLE ...
	 * 
	 * @param tableName
	 * @return
	 */
	public String makeDropStatement(TableName tableName) {
		return "DROP TABLE " + makeIdentifier(tableName);
	}

	public String makeDropStatement(String tableName) {
		return makeDropStatement(new TableName(tableName));
	}

	/**
	 * DELETE FROM ...
	 * 
	 * @param tableName
	 * @return
	 */
	public String makeDeleteStatement(TableName tableName) {
		return "DELETE FROM " + makeIdentifier(tableName);
	}

	public String makeDeleteStatement(String tableName) {
		return makeDeleteStatement(new TableName(tableName));
	}

	/** SELECT * */
	public String makeSelectAllStatement(String tableName) {
		return "SELECT * FROM " + makeIdentifier(tableName);
	}
	
	/** SELECT * */
	public String makeSelectAllStatement(TableName tableName) {
		return "SELECT * FROM " + makeIdentifier(tableName);
	}

	public String makeSelectStatement(String tableName, boolean distinct, String... columns) {
		StringBuilder b = new StringBuilder("SELECT ");
		if (distinct) {
			b.append(" DISTINCT ");
		}
		boolean first = true;
		for (String col : columns) {
			if (first) {
				first = false;
			} else {
				b.append(", ");
			}
			b.append(makeIdentifier(col));
		}
		b.append(" FROM ");
		b.append(makeIdentifier(tableName));
		return b.toString();
	}

	/** Selects count(*). */
	public String makeSelectSizeStatement(String tableName) {
		return "SELECT count(*) FROM " + makeIdentifier(tableName);
	}

	/** Selects count(*). */
	public String makeSelectSizeStatement(TableName tableName) {
		return "SELECT count(*) FROM " + makeIdentifier(tableName);
	}

	/**
	 * Makes a statement selecting all attributes but no rows. Can be used to fetch header meta data.
	 * 
	 * @deprecated You don't want to use this method. Use the table meta data.
	 */
	@Deprecated
	public String makeSelectEmptySetStatement(String tableName) {
		return "SELECT * FROM " + makeIdentifier(tableName) + " WHERE 1=0";
	}

	public String makeClobCreator(String columnName, int minLength) {
		StringBuilder b = new StringBuilder();
		b.append(makeIdentifier(columnName)).append(" ");

		final String typeString = mapAttributeTypeToSQLDataType(Ontology.STRING);
		b.append(typeString);
		if (typeString.toLowerCase().startsWith("varchar")) {
			if (minLength != -1) {
				b.append("(").append(minLength).append(")");
			} else {
				b.append("(").append(defaultVarCharLength).append(")");
			}
		}
		return b.toString();
	}

	public String makeVarcharCreator(String columnName, int minLength) {
		if (minLength == 0) {
			minLength = 1;
		}
		StringBuilder b = new StringBuilder();
		b.append(makeIdentifier(columnName)).append(" ");

		final String typeString = mapAttributeTypeToSQLDataType(Ontology.NOMINAL);
		b.append(typeString);
		if (typeString.toLowerCase().startsWith("varchar")) {
			if (minLength != -1) {
				b.append("(").append(minLength).append(")");
			} else {
				b.append("(").append(defaultVarCharLength).append(")");
			}
		}
		return b.toString();
	}

	public String makeIntegerCreator(String columnName) {
		StringBuilder b = new StringBuilder();
		b.append(makeIdentifier(columnName)).append(" ");
		final String typeString = mapAttributeTypeToSQLDataType(Ontology.INTEGER);
		b.append(typeString);
		return b.toString();
	}

}
