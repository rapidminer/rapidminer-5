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

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.table.AttributeFactory;
import com.rapidminer.example.table.DataRow;
import com.rapidminer.example.table.DataRowFactory;
import com.rapidminer.example.table.MemoryExampleTable;
import com.rapidminer.operator.Annotations;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.UserError;
import com.rapidminer.operator.ports.metadata.AttributeMetaData;
import com.rapidminer.operator.ports.metadata.ExampleSetMetaData;
import com.rapidminer.operator.ports.metadata.MetaData;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeCategory;
import com.rapidminer.tools.I18N;
import com.rapidminer.tools.LogService;
import com.rapidminer.tools.Ontology;
import com.rapidminer.tools.ParameterService;
import com.rapidminer.tools.Tools;
import com.rapidminer.tools.jdbc.ColumnIdentifier;
import com.rapidminer.tools.jdbc.DatabaseHandler;
import com.rapidminer.tools.jdbc.StatementCreator;
import com.rapidminer.tools.jdbc.TableName;
import com.rapidminer.tools.jdbc.connection.ConnectionEntry;
import com.rapidminer.tools.jdbc.connection.ConnectionProvider;

/**
 * Reads an {@link ExampleSet} from an SQL {@link Connection} table. SQL datatypes are mapped to value types of
 * {@link Attribute}s by using {@link DatabaseHandler#getRapidMinerTypeIndex(int)}. Data is copied into main memory.
 * 
 * Data can be read from either a table name or a query. In the first case, the meta data is retrieved from the database
 * meta data for that table for reasons of efficiency. In the latter case, a LIMIT 0 is appended to the query, which may
 * fail on some systems but which enables us to retrieve the structure of the table without performing the entire query
 * itself.
 * 
 * @author Simon Fischer
 * 
 */
public class DatabaseDataReader extends AbstractExampleSource implements ConnectionProvider {

	/** System property to decide whether meta data should be fetched from DB for database queries. */
	public static final String PROPERTY_EVALUATE_MD_FOR_SQL_QUERIES = "rapidminer.gui.evaluate_meta_data_for_sql_queries";

	public DatabaseDataReader(OperatorDescription description) {
		super(description);
	}

	private DatabaseHandler databaseHandler;

	@Override
	public ExampleSet read() throws OperatorException {
		try {
			ExampleSet result = super.read();
			return result;
		} finally {
			if (databaseHandler != null && databaseHandler.getConnection() != null) {
				try {
					databaseHandler.getConnection().close();
				} catch (SQLException e) {
					getLogger().log(Level.WARNING, "Error closing database connection: " + e, e);
				}
			}
		}
	}

	protected ResultSet getResultSet() throws OperatorException {
		try {
			databaseHandler = DatabaseHandler.getConnectedDatabaseHandler(this);
			String query = getQuery(databaseHandler.getStatementCreator());
			if (query == null) {
				throw new UserError(this, 202, new Object[] { "query", "query_file", "table_name" });
			}
			return databaseHandler.executeStatement(query, true, this, getLogger());
		} catch (SQLException sqle) {
			throw new UserError(this, sqle, 304, sqle.getMessage());
		}
	}

	@Override
	public ExampleSet createExampleSet() throws OperatorException {
		ResultSet resultSet = getResultSet();
		MemoryExampleTable table;
		try {
			List<Attribute> attributes = getAttributes(resultSet);
			table = createExampleTable(resultSet, attributes, getParameterAsInt(ExampleSource.PARAMETER_DATAMANAGEMENT), getLogger());
		} catch (SQLException e) {
			throw new UserError(this, e, 304, e.getMessage());
		} finally {
			try {
				resultSet.close();
			} catch (SQLException e) {
				getLogger().log(Level.WARNING, "DB error closing result set: " + e, e);
			}
		}
		return table.createExampleSet();
	}

	@Override
	public MetaData getGeneratedMetaData() throws OperatorException {
		ExampleSetMetaData metaData = new ExampleSetMetaData();
		try {
			databaseHandler = DatabaseHandler.getConnectedDatabaseHandler(this);
			switch (getParameterAsInt(DatabaseHandler.PARAMETER_DEFINE_QUERY)) {
				case DatabaseHandler.QUERY_TABLE:
					List<ColumnIdentifier> columns = databaseHandler.getAllColumnNames(DatabaseHandler.getSelectedTableName(this), databaseHandler.getConnection().getMetaData());
					for (ColumnIdentifier column : columns) {
						metaData.addAttribute(new AttributeMetaData(column.getColumnName(),
								DatabaseHandler.getRapidMinerTypeIndex(column.getSqlType())));
					}
					break;
				case DatabaseHandler.QUERY_QUERY:
				case DatabaseHandler.QUERY_FILE:
				default:
					if (!"false".equals(ParameterService.getParameterValue(PROPERTY_EVALUATE_MD_FOR_SQL_QUERIES))) {
						String query = getQuery(databaseHandler.getStatementCreator());
						PreparedStatement prepared = databaseHandler.getConnection().prepareStatement(query);
						// query = "SELECT * FROM (" + query + ") dummy WHERE 1=0";
						// ResultSet resultSet = databaseHandler.executeStatement(query, true, this, getLogger());
						List<Attribute> attributes = getAttributes(prepared.getMetaData());
						for (Attribute att : attributes) {
							metaData.addAttribute(new AttributeMetaData(att));
						}
						prepared.close();
					}
					break;
			}
		} catch (SQLException e) {
			//LogService.getRoot().log(Level.WARNING, "Failed to fetch meta data: " + e, e);
			LogService.getRoot().log(Level.WARNING,
					I18N.getMessage(LogService.getRoot().getResourceBundle(),
							"com.rapidminer.operator.io.DatabaseDataReader.fetching_meta_data_error",
							e),
					e);

		} finally {
			try {
				if (databaseHandler != null && databaseHandler.getConnection() != null) {
					databaseHandler.disconnect();
				}
			} catch (SQLException e) {
				getLogger().log(Level.WARNING, "DB error closing connection: " + e, e);
			}
		}
		return metaData;
	}

	public static MemoryExampleTable createExampleTable(ResultSet resultSet, List<Attribute> attributes, int dataManagementType, Logger logger) throws SQLException, OperatorException {
		ResultSetMetaData metaData = resultSet.getMetaData();
		Attribute[] attributeArray = attributes.toArray(new Attribute[attributes.size()]);
		MemoryExampleTable table = new MemoryExampleTable(attributes);
		DataRowFactory factory = new DataRowFactory(dataManagementType, '.');
		while (resultSet.next()) {
			DataRow dataRow = factory.create(attributeArray.length);
			// double[] data = new double[attributeArray.length];
			for (int i = 1; i <= metaData.getColumnCount(); i++) {
				Attribute attribute = attributeArray[i - 1];
				int valueType = attribute.getValueType();
				double value;
				if (Ontology.ATTRIBUTE_VALUE_TYPE.isA(valueType, Ontology.DATE_TIME)) {
					Timestamp timestamp = resultSet.getTimestamp(i);
					if (resultSet.wasNull()) {
						value = Double.NaN;
					} else {
						value = timestamp.getTime();
					}
				} else if (Ontology.ATTRIBUTE_VALUE_TYPE.isA(valueType, Ontology.NUMERICAL)) {
					value = resultSet.getDouble(i);
					if (resultSet.wasNull()) {
						value = Double.NaN;
					}
				} else {
					if (Ontology.ATTRIBUTE_VALUE_TYPE.isA(valueType, Ontology.NOMINAL)) {
						String valueString;
						if (metaData.getColumnType(i) == Types.CLOB) {
							Clob clob = resultSet.getClob(i);
							if (clob != null) {
								BufferedReader in = null;
								try {
									in = new BufferedReader(clob.getCharacterStream());
									String line = null;
									try {
										StringBuffer buffer = new StringBuffer();
										while ((line = in.readLine()) != null) {
											buffer.append(line + "\n");
										}
										valueString = buffer.toString();
									} catch (IOException e) {
										throw new OperatorException("Database error occurred: " + e, e);
									}
								} finally {
									try {
										in.close();
									} catch (IOException e) {}
								}
							} else {
								valueString = null;
							}
						} else {
							valueString = resultSet.getString(i);
						}
						if (resultSet.wasNull() || valueString == null) {
							value = Double.NaN;
						} else {
							value = attribute.getMapping().mapString(valueString);
						}
					} else {
						if (logger != null) {
							logger.warning("Unknown column type: " + attribute);
						}
						value = Double.NaN;
					}
				}
				dataRow.set(attribute, value);
				// data[i-1] = value;
			}
			table.addDataRow(dataRow); // new DoubleArrayDataRow(data));
		}
		return table;
	}

	public static List<Attribute> getAttributes(ResultSet resultSet) throws SQLException {
		ResultSetMetaData metaData = resultSet.getMetaData();
		return getAttributes(metaData);
	}

	private static List<Attribute> getAttributes(ResultSetMetaData metaData) throws SQLException {
		List<Attribute> result = new LinkedList<Attribute>();

		if (metaData != null) {
			// A map mapping original column names to a counter specifying how often
			// they were chosen
			Map<String, Integer> duplicateNameMap = new HashMap<String, Integer>();

			for (int columnIndex = 1; columnIndex <= metaData.getColumnCount(); columnIndex++) {

				// column name from DB
				String dbColumnName = metaData.getColumnLabel(columnIndex);

				// name that will be used in example set
				String columnName = dbColumnName;

				// check original name first
				Integer duplicateCount = duplicateNameMap.get(dbColumnName);
				boolean isUnique = duplicateCount == null;
				if (isUnique) {
					// name is unique
					duplicateNameMap.put(columnName, new Integer(1));
				} else {
					// name already present, iterate until unique
					while (!isUnique) {
						// increment duplicate counter
						duplicateCount = new Integer(duplicateCount.intValue() + 1);

						// create new name proposal
						columnName = dbColumnName + "_" + (duplicateCount - 1);  // -1 because of compatibility

						// check if new name is already taken
						isUnique = duplicateNameMap.get(columnName) == null;
					}

					// save new duplicate count for old db column name
					duplicateNameMap.put(dbColumnName, duplicateCount);
				}

				int attributeType = DatabaseHandler.getRapidMinerTypeIndex(metaData.getColumnType(columnIndex));
				final Attribute attribute = AttributeFactory.createAttribute(columnName, attributeType);
				attribute.getAnnotations().setAnnotation("sql_type", metaData.getColumnTypeName(columnIndex));
				result.add(attribute);
			}
		}

		return result;
	}

	private String getQuery(StatementCreator sc) throws OperatorException {
		switch (getParameterAsInt(DatabaseHandler.PARAMETER_DEFINE_QUERY)) {
			case DatabaseHandler.QUERY_QUERY: {
				String query = getParameterAsString(DatabaseHandler.PARAMETER_QUERY);
				if (query != null) {
					query = query.trim();
				}
				return query;
			}
			case DatabaseHandler.QUERY_FILE: {
				File queryFile = getParameterAsFile(DatabaseHandler.PARAMETER_QUERY_FILE);
				if (queryFile != null) {
					String query = null;
					try {
						query = Tools.readTextFile(queryFile);
					} catch (IOException ioe) {
						throw new UserError(this, ioe, 302, new Object[] { queryFile, ioe.getMessage() });
					}
					if (query == null || query.trim().length() == 0) {
						throw new UserError(this, 205, queryFile);
					}
					return query;
				}
			}
			case DatabaseHandler.QUERY_TABLE:
				TableName tableName = DatabaseHandler.getSelectedTableName(this);
				//final String tableName = getParameterAsString(DatabaseHandler.PARAMETER_TABLE_NAME);
				return "SELECT * FROM " + sc.makeIdentifier(tableName);
		}
		return null;
	}

	@Override
	public ConnectionEntry getConnectionEntry() {
		return DatabaseHandler.getConnectionEntry(this);
	}

	@Override
	protected void addAnnotations(ExampleSet result) {
		try {
			if (databaseHandler != null) {
				result.getAnnotations().setAnnotation(Annotations.KEY_SOURCE,
						getQuery(databaseHandler.getStatementCreator()));
			}
		} catch (OperatorException e) {}
	}

	@Override
	protected boolean isMetaDataCacheable() {
		return true;
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> list = super.getParameterTypes();
		list.addAll(DatabaseHandler.getConnectionParameterTypes(this));
		list.addAll(DatabaseHandler.getQueryParameterTypes(this, false));
		list.addAll(DatabaseHandler.getStatementPreparationParamterTypes(this));

		list.add(new ParameterTypeCategory(ExampleSource.PARAMETER_DATAMANAGEMENT, "Determines, how the data is represented internally.", DataRowFactory.TYPE_NAMES, DataRowFactory.TYPE_DOUBLE_ARRAY, false));
		return list;
	}

}
// /*
// * RapidMiner
// *
// * Copyright (C) 2001-2012 by Rapid-I and the contributors
// *
// * Complete list of developers available at our web site:
// *
// * http://rapid-i.com
// *
// * This program is free software: you can redistribute it and/or modify
// * it under the terms of the GNU Affero General Public License as published by
// * the Free Software Foundation, either version 3 of the License, or
// * (at your option) any later version.
// *
// * This program is distributed in the hope that it will be useful,
// * but WITHOUT ANY WARRANTY; without even the implied warranty of
// * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
// * GNU Affero General Public License for more details.
// *
// * You should have received a copy of the GNU Affero General Public License
// * along with this program. If not, see http://www.gnu.org/licenses/.
// */
// package com.rapidminer.operator.io;
//
// import java.io.BufferedReader;
// import java.io.File;
// import java.io.IOException;
// import java.sql.Clob;
// import java.sql.ResultSet;
// import java.sql.ResultSetMetaData;
// import java.sql.SQLException;
// import java.sql.Statement;
// import java.sql.Types;
// import java.util.Date;
// import java.util.LinkedList;
// import java.util.List;
//
// import com.rapidminer.example.ExampleSet;
// import com.rapidminer.operator.Annotations;
// import com.rapidminer.operator.OperatorDescription;
// import com.rapidminer.operator.OperatorException;
// import com.rapidminer.operator.UserError;
// import com.rapidminer.parameter.ParameterType;
// import com.rapidminer.tools.Ontology;
// import com.rapidminer.tools.Tools;
// import com.rapidminer.tools.jdbc.DatabaseHandler;
// import com.rapidminer.tools.jdbc.StatementCreator;
// import com.rapidminer.tools.jdbc.connection.ConnectionEntry;
// import com.rapidminer.tools.jdbc.connection.ConnectionProvider;
//
//
// /**
// * <p>This operator reads an {@link com.rapidminer.example.ExampleSet} from an SQL
// * database. The SQL query can be passed to RapidMiner via a parameter or, in case of
// * long SQL statements, in a separate file. Please note that column names are
// * often case sensitive. Databases may behave differently here.</p>
// *
// * <p>Please note that this operator supports two basic working modes:</p>
// * <ol>
// * <li>reading the data from the database and creating an example table in main memory</li>
// * <li>keeping the data in the database and directly working on the database table </li>
// * </ol>
// * <p>The latter possibility will be turned on by the parameter &quot;work_on_database&quot;.
// * Please note that this working mode is still regarded as experimental and errors might
// * occur. In order to ensure proper data changes the database working mode is only allowed
// * on a single table which must be defined with the parameter &quot;table_name&quot;.
// * IMPORTANT: If you encounter problems during data updates (e.g. messages that the result set is not
// * updatable) you probably have to define a primary key for your table.</p>
// *
// * <p>If you are not directly working on the database, the data will be read with an arbitrary
// * SQL query statement (SELECT ... FROM ... WHERE ...) defined by &quot;query&quot; or &quot;query_file&quot;.
// * The memory mode is the recommended way of using this operator. This is especially important for
// * following operators like learning schemes which would often load (most of) the data into main memory
// * during the learning process. In these cases a direct working on the database is not recommended
// * anyway.</p>
// *
// * <h5>Warning</h5>
// * As the java <code>ResultSetMetaData</code> interface does not provide
// * information about the possible values of nominal attributes, the internal
// * indices the nominal values are mapped to will depend on the ordering
// * they appear in the table. This may cause problems only when processes are
// * split up into a training process and an application or testing process.
// * For learning schemes which are capable of handling nominal attributes, this
// * is not a problem. If a learning scheme like a SVM is used with nominal data,
// * RapidMiner pretends that nominal attributes are numerical and uses indices for the
// * nominal values as their numerical value. A SVM may perform well if there are
// * only two possible values. If a test set is read in another process, the
// * nominal values may be assigned different indices, and hence the SVM trained
// * is useless. This is not a problem for label attributes, since the classes can
// * be specified using the <code>classes</code> parameter and hence, all
// * learning schemes intended to use with nominal data are safe to use.
// *
// * @rapidminer.todo Fix the above problem. This may not be possible effeciently since
// * it is not supported by the Java ResultSet interface.
// *
// * @author Ingo Mierswa, Tobias Malbrecht
// */
// public class DatabaseDataReader extends AbstractDataReader implements ConnectionProvider {
//
// /** The database connection handler. */
// private DatabaseHandler databaseHandler;
//
// /** This is only used for the case that the data is read into memory. */
// private Statement statement;
//
//
// public DatabaseDataReader(OperatorDescription description) {
// super(description);
// getParameterType(DatabaseDataReader.PARAMETER_ERROR_TOLERANT).setHidden(true);
// }
//
// public void tearDown() {
// if (this.statement != null) {
// try {
// this.statement.close();
// } catch (SQLException e) {
// logWarning("Cannot close statement.");
// }
// this.statement = null;
// }
// }
//
// private String getQuery(StatementCreator sc) throws OperatorException {
// switch (getParameterAsInt(DatabaseHandler.PARAMETER_DEFINE_QUERY)) {
// case DatabaseHandler.QUERY_QUERY:
// {
// String query = getParameterAsString(DatabaseHandler.PARAMETER_QUERY);
// if (query != null) {
// query = query.trim();
// }
// return query;
// }
// case DatabaseHandler.QUERY_FILE:
// {
// File queryFile = getParameterAsFile(DatabaseHandler.PARAMETER_QUERY_FILE);
// if (queryFile != null) {
// String query = null;
// try {
// query = Tools.readTextFile(queryFile);
// } catch (IOException ioe) {
// throw new UserError(this, ioe, 302, new Object[] { queryFile, ioe.getMessage() });
// }
// if ((query == null) || (query.trim().length() == 0)) {
// throw new UserError(this, 205, queryFile);
// }
// return query;
// }
// }
// case DatabaseHandler.QUERY_TABLE:
// final String tableName = getParameterAsString(DatabaseHandler.PARAMETER_TABLE_NAME);
// return "SELECT * FROM " + sc.makeIdentifier(tableName);
// }
// return null;
// }
//
// /**
// * This method reads the file whose name is given, extracts the database
// * access information and the query from it and executes the query. The
// * query result is returned as a ResultSet.
// */
// public ResultSet getResultSet() throws OperatorException {
// ResultSet rs = null;
// try {
// databaseHandler = DatabaseHandler.getConnectedDatabaseHandler(this);
// String query = getQuery(databaseHandler.getStatementCreator());
// if (query == null) {
// throw new UserError(this, 202, new Object[] { "query", "query_file", "table_name" });
// }
// // getLogger().info("Executing query: '" + query + "'");
// // this.statement = databaseHandler.createStatement(false);
// // rs = this.statement.executeQuery(query);
// // log("Query executed.");
// rs = databaseHandler.executeStatement(query, true, this, getLogger());
// } catch (SQLException sqle) {
// throw new UserError(this, sqle, 304, sqle.getMessage());
// }
// return rs;
// }
//
// @Override
// public void processFinished() {
// disconnect();
// }
//
// private void disconnect() {
// // close statement
// tearDown();
//
// // close database connection
// if (databaseHandler != null) {
// try {
// databaseHandler.disconnect();
// databaseHandler = null;
// } catch (SQLException e) {
// logWarning("Cannot disconnect from database: " + e);
// }
// }
// }
//
// @Override
// protected DataSet getDataSet() throws OperatorException {
// return new DataSet() {
// private ResultSet resultSet = getResultSet();
//
// private ResultSetMetaData metaData = null;
// {
// // if (!attributeNamesDefinedByUser()){
//
// try {
// clearAllReaderSettings();
// metaData = resultSet.getMetaData();
// int numberOfColumns = metaData.getColumnCount();
// String[] columnNames = new String[numberOfColumns];
// int[] columnTypes = new int[numberOfColumns];
// for (int i = 0; i < numberOfColumns; i++) {
// columnNames[i] = metaData.getColumnLabel(i + 1);
// columnTypes[i] = DatabaseHandler.getRapidMinerTypeIndex(metaData.getColumnType(i + 1));
// }
// setAttributeNames(columnNames);
// setAttributeNamesDefinedByUser(true);
//
// List<Integer> list = new LinkedList<Integer>();
// for (int i = 0; i<columnTypes.length; i++){
// list.add(columnTypes[i]);
// }
// setValueTypes(list);
// } catch (SQLException e) {
// throw new OperatorException("Could not read result set meta data.");
// }
//
// // }
// }
//
// private Object[] values = new Object[getColumnCount()];
//
// @Override
// // TODO throw operator exception in case of SQL exception
// public boolean next() {
// try {
// if (resultSet.next()) {
// for (int i = 0; i < getColumnCount(); i++) {
// if (Ontology.ATTRIBUTE_VALUE_TYPE.isA(DatabaseHandler.getRapidMinerTypeIndex(metaData.getColumnType(i + 1)),
// Ontology.NUMERICAL)) {
// values[i] = Double.valueOf(resultSet.getDouble(i + 1));
// } else if (Ontology.ATTRIBUTE_VALUE_TYPE.isA(DatabaseHandler.getRapidMinerTypeIndex(metaData.getColumnType(i + 1)),
// Ontology.DATE_TIME)) {
// values[i] = resultSet.getTimestamp(i + 1);
// } else if (metaData.getColumnType(i + 1) == Types.CLOB) {
// Clob clob = resultSet.getClob(i + 1);
// if (clob != null) {
// BufferedReader in = null;
// try {
// in = new BufferedReader(clob.getCharacterStream());
// String line = null;
// try {
// StringBuffer buffer = new StringBuffer();
// while ((line = in.readLine()) != null) {
// buffer.append(line + "\n");
// }
// values[i] = buffer.toString();
// } catch (IOException e) {
// values[i] = null;
// }
// } finally {
// try {
// in.close();
// } catch (IOException e) {}
// }
// } else {
// values[i] = null;
// }
// } else if (Ontology.ATTRIBUTE_VALUE_TYPE.isA(DatabaseHandler.getRapidMinerTypeIndex(metaData.getColumnType(i + 1)),
// Ontology.NOMINAL)) {
// values[i] = resultSet.getString(i + 1);
// }
// if (resultSet.wasNull()) {
// values[i] = null;
// }
// }
// return true;
// }
// return false;
// } catch (SQLException e) {
// // throw new OperatorException(e.getMessage(), e);
// return false;
// }
// }
//
// @Override
// public int getNumberOfColumnsInCurrentRow() {
// // we can rely on columnCount here since it was already set
// return getColumnCount();
// }
//
// @Override
// public boolean isMissing(int columnIndex) {
// return values[columnIndex] == null;
// }
//
// @Override
// public Number getNumber(int columnIndex) {
// try {
// if (Ontology.ATTRIBUTE_VALUE_TYPE.isA(DatabaseHandler.getRapidMinerTypeIndex(metaData.getColumnType(columnIndex +
// 1)), Ontology.NUMERICAL)) {
// return (Double) values[columnIndex];
// }
// } catch (SQLException e) {
// }
// return null;
// }
//
// @Override
// public Date getDate(int columnIndex) {
// try {
// if (Ontology.ATTRIBUTE_VALUE_TYPE.isA(DatabaseHandler.getRapidMinerTypeIndex(metaData.getColumnType(columnIndex +
// 1)), Ontology.DATE_TIME)) {
// return (Date) values[columnIndex];
// }
// } catch (SQLException e) {
// }
// return null;
// }
//
// @Override
// public String getString(int columnIndex) {
// try {
// if (values[columnIndex] == null){
// return "";
// }
// if (Ontology.ATTRIBUTE_VALUE_TYPE.isA(DatabaseHandler.getRapidMinerTypeIndex(metaData.getColumnType(columnIndex +
// 1)), Ontology.NOMINAL)) {
// return (String) values[columnIndex];
// }
// return values[columnIndex].toString();
// } catch (SQLException e) {
// }
// return null;
// }
//
// @Override
// public void close() throws OperatorException {
// disconnect();
// }
// };
// }
//
// @Override
// public ConnectionEntry getConnectionEntry() {
// return DatabaseHandler.getConnectionEntry(this);
// }
//
// @Override
// protected void addAnnotations(ExampleSet result) {
// try {
// if (databaseHandler != null) {
// result.getAnnotations().setAnnotation(Annotations.KEY_SOURCE, getQuery(databaseHandler.getStatementCreator()));
// }
// } catch (OperatorException e) {
// }
// }
//
//
// @Override
// public List<ParameterType> getParameterTypes() {
// List<ParameterType> list = super.getParameterTypes();
// list.addAll(DatabaseHandler.getConnectionParameterTypes(this));
// list.addAll(DatabaseHandler.getQueryParameterTypes(this, false));
// list.addAll(DatabaseHandler.getStatementPreparationParamterTypes(this));
// return list;
// }
//
// }
