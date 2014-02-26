/*
 *  RapidMiner
 *
 *  Copyright (C) 2001-2014 by RapidMiner and the contributors
 *
 *  Complete list of developers available at our web site:
 *
 *       http://rapidminer.com
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
import java.sql.Date;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Time;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

import com.rapidminer.RapidMiner;
import com.rapidminer.example.Attribute;
import com.rapidminer.example.AttributeRole;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.table.AttributeFactory;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.ProcessStoppedException;
import com.rapidminer.operator.UserError;
import com.rapidminer.operator.io.DatabaseExampleSetWriter;
import com.rapidminer.parameter.ParameterHandler;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeBoolean;
import com.rapidminer.parameter.ParameterTypeCategory;
import com.rapidminer.parameter.ParameterTypeDatabaseConnection;
import com.rapidminer.parameter.ParameterTypeDatabaseSchema;
import com.rapidminer.parameter.ParameterTypeDatabaseTable;
import com.rapidminer.parameter.ParameterTypeEnumeration;
import com.rapidminer.parameter.ParameterTypeFile;
import com.rapidminer.parameter.ParameterTypePassword;
import com.rapidminer.parameter.ParameterTypeSQLQuery;
import com.rapidminer.parameter.ParameterTypeString;
import com.rapidminer.parameter.ParameterTypeTupel;
import com.rapidminer.parameter.UndefinedParameterError;
import com.rapidminer.parameter.conditions.BooleanParameterCondition;
import com.rapidminer.parameter.conditions.EqualTypeCondition;
import com.rapidminer.repository.RepositoryLocation;
import com.rapidminer.tools.I18N;
import com.rapidminer.tools.LogService;
import com.rapidminer.tools.LoggingHandler;
import com.rapidminer.tools.Ontology;
import com.rapidminer.tools.ParameterService;
import com.rapidminer.tools.ProgressListener;
import com.rapidminer.tools.Tools;
import com.rapidminer.tools.jdbc.connection.ConnectionEntry;
import com.rapidminer.tools.jdbc.connection.DatabaseConnectionService;
import com.rapidminer.tools.jdbc.connection.FieldConnectionEntry;

/**
 * <p>This class hides the database. Using
 * {@link DatabaseHandler#connect(String,String,boolean)}, you can establish a
 * connection to the database. Once connected, queries and updates are possible.</p>
 * 
 * <p>Please note that the queries does not end with the statement terminator (e.g.
 * ; for Oracle or GO for Sybase). The JDBC driver will automatically add the correct
 * terminator.</p>
 * 
 * @author Ingo Mierswa
 */
public class DatabaseHandler {

	public static final String PARAMETER_DEFINE_CONNECTION = "define_connection";

	public static final String[] CONNECTION_MODES = { "predefined", "url", "jndi" };

	public static final int CONNECTION_MODE_PREDEFINED = 0;

	public static final int CONNECTION_MODE_URL = 1;

	public static final int CONNECTION_MODE_JNDI = 2;

	public static final String PARAMETER_CONNECTION = "connection";

	/** The parameter name for &quot;Indicates the used database system&quot; */
	public static final String PARAMETER_DATABASE_SYSTEM = "database_system";

	/** The parameter name for &quot;The complete URL connection string for the database, e.g. 'jdbc:mysql://foo.bar:portnr/database'&quot; */
	public static final String PARAMETER_DATABASE_URL = "database_url";

	/** The parameter name for &quot;Database username.&quot; */
	public static final String PARAMETER_USERNAME = "username";

	/** The parameter name for &quot;Password for the database.&quot; */
	public static final String PARAMETER_PASSWORD = "password";

	public static final String PARAMETER_DEFINE_QUERY = "define_query";

	public static final String PARAMETER_JNDI_NAME = "jndi_name";

	public static final String[] QUERY_MODES = { "query", "query file", "table name" };

	public static final int QUERY_QUERY = 0;

	public static final int QUERY_FILE = 1;

	public static final int QUERY_TABLE = 2;

	/** The parameter name for &quot;SQL query. If not set, the query is read from the file specified by 'query_file'.&quot; */
	public static final String PARAMETER_QUERY = "query";

	/** The parameter name for &quot;File containing the query. Only evaluated if 'query' is not set.&quot; */
	public static final String PARAMETER_QUERY_FILE = "query_file";

	/** The parameter name for &quot;Use this table if work_on_database is true or no other query is specified.&quot; */
	public static final String PARAMETER_TABLE_NAME = "table_name";

	public static final String PARAMETER_USE_DEFAULT_SCHEMA = "use_default_schema";

	public static final String PARAMETER_SCHEMA_NAME = "schema_name";

	public static final String[] OVERWRITE_MODES = new String[] {
			"none",
			"overwrite first, append then",
			"overwrite",
			"append"
	};

	public static final int OVERWRITE_MODE_NONE = 0;

	public static final int OVERWRITE_MODE_OVERWRITE_FIRST = 1;

	public static final int OVERWRITE_MODE_OVERWRITE = 2;

	public static final int OVERWRITE_MODE_APPEND = 3;

	/** Used for logging purposes. */
	private String databaseURL;

	private StatementCreator statementCreator;

	private String user;

	/** The 'singleton' connection. Each database handler can handle one single connection to
	 *  a database. Will be null before the connection is established and will also be null
	 *  after {@link #disconnect()} was invoked. */
	private Connection connection;

	public static final String PARAMETER_PARAMETERS = "parameters";

	public static final String PARAMETER_PREPARE_STATEMENT = "prepare_statement";

	private static final String[] SQL_TYPES = { "VARCHAR", "INTEGER", "REAL", "LONG" };

	//	private static class DHIdentifier {
	//		private String url;
	//		private String username;
	//
	//		private DHIdentifier(String url, String username) {
	//			super();
	//			this.url = url;
	//			this.username = username;
	//		}
	//
	//		@Override
	//		public int hashCode() {
	//			final int prime = 31;
	//			int result = 1;
	//			result = prime * result + ((url == null) ? 0 : url.hashCode());
	//			result = prime * result + ((username == null) ? 0 : username.hashCode());
	//			return result;
	//		}
	//
	//		@Override
	//		public boolean equals(Object obj) {
	//			if (this == obj)
	//				return true;
	//			if (obj == null)
	//				return false;
	//			if (getClass() != obj.getClass())
	//				return false;
	//			DHIdentifier other = (DHIdentifier) obj;
	//			if (url == null) {
	//				if (other.url != null)
	//					return false;
	//			} else if (!url.equals(other.url))
	//				return false;
	//			if (username == null) {
	//				if (other.username != null)
	//					return false;
	//			} else if (!username.equals(other.username))
	//				return false;
	//			return true;
	//		}
	//	}
	//	private static final Map<DHIdentifier,DatabaseHandler> POOL = new HashMap<DHIdentifier,DatabaseHandler>();
	//	private static final Object POOL_LOCK = new Object();

	/**
	 * Constructor of the database handler. This constructor expects the URL definition
	 * of the database which is needed by the System DriverManager to create an appropriate
	 * driver. Please note that this database handler still must be connected via invoking
	 * the method {@link #connect(char[], Properties, boolean)}. If you want to directly use
	 * a connected database handler you might use the static method
	 * {@link #getConnectedDatabaseHandler(String, String, String, JDBCProperties, LoggingHandler)} instead.
	 */
	private DatabaseHandler(String databaseURL, String user) {
		this.databaseURL = databaseURL;
		this.user = user;
	}

	public static DatabaseHandler getConnectedDatabaseHandler(ConnectionEntry entry) throws SQLException {
		//synchronized (POOL_LOCK) {
		//    		DHIdentifier id = new DHIdentifier(entry.getURL(), entry.getName());
		//    		DatabaseHandler pooled = POOL.get(id);
		//    		if ((pooled != null) && !pooled.connection.isClosed()) {
		//    			return pooled;
		//    		} else {
		DatabaseHandler handler = new DatabaseHandler(entry.getURL(), entry.getUser());
		Properties props;
		if (entry instanceof FieldConnectionEntry) {
			props = ((FieldConnectionEntry) entry).getConnectionProperties();
		} else {
			props = new Properties();
		}
		handler.connect(entry.getPassword(), props, true);
		//    			POOL.put(id, handler);
		return handler;
		//    		}
		//    	}
	}

	public static DatabaseHandler getHandler(Connection connection) throws OperatorException, SQLException {
		DatabaseHandler databaseHandler = new DatabaseHandler("preconnected", "unknown");
		databaseHandler.connection = connection;
		databaseHandler.statementCreator = new StatementCreator(connection);
		return databaseHandler;
	}

	/** Returns a connected database handler instance from the given connection data. If the password
	 *  is null, it will be queries by the user during this method.
	 *  This will create a connection with auto commit enabled.
	 *  */
	public static DatabaseHandler getConnectedDatabaseHandler(String databaseURL, String username, String password) throws OperatorException, SQLException {
		return getConnectedDatabaseHandler(databaseURL, username, password, true);
	}

	public static DatabaseHandler getConnectedDatabaseHandler(String databaseURL, String username, String password, boolean autoCommit) throws SQLException {
		//		synchronized (POOL_LOCK) {
		//			DHIdentifier id = new DHIdentifier(databaseURL, username);
		//    		DatabaseHandler pooled = POOL.get(id);
		//    		if ((pooled != null) && !pooled.connection.isClosed()) {
		//    			return pooled;
		//    		} else {

		// We should not force a password here. 
		// E.G. for MS SQL Server with Windows authentication, both user and password can be null.
		// If we really need it, we should do it in connect, if user is not null
//		if (password == null) {
//			password = RapidMiner.getInputHandler().inputPassword("Password for user '" + username + "' required");
//		}
		DatabaseHandler databaseHandler = new DatabaseHandler(databaseURL, username);
		databaseHandler.connect(password.toCharArray(), new Properties(), autoCommit);
		//    			POOL.put(id, databaseHandler);
		return databaseHandler;
		//    		}
		//		}
	}

	public StatementCreator getStatementCreator() {
		return statementCreator;
	}

	/**
	 * Establishes a connection to the database. Afterwards, queries and updates
	 * can be executed using the methods this class provides.
	 * 
	 * @param username
	 *            Name with which to log in to the database. Might be null.
	 * @param passwd
	 *            Password with which to log in to the database. Might be null.
	 * @param props
	 * 			  The connection properties. (not the jdbc properties)
	 * @param autoCommit
	 *            If TRUE, all changes to the database will be committed
	 *            automatically. If FALSE, the commit()-Method has to be called
	 *            to make changes permanent.
	 */
	private void connect(char[] passwd, Properties props, boolean autoCommit) throws SQLException {
		if (connection != null) {
			throw new SQLException("Connection to database '" + databaseURL + "' already exists!");
		}
		//LogService.getRoot().config("Connecting to "+databaseURL+" as "+this.user+".");
		LogService.getRoot().log(Level.CONFIG,
				"com.rapidminer.tools.jdbc.DatabaseHandler.connecting_to_database",
				new Object[] { databaseURL, this.user });
		DriverManager.setLoginTimeout(30);
		props.put("SetBigStringTryClob", "true");
		if (this.user != null && !user.isEmpty()) {
			props.put("user", user);
			props.put("password", new String(passwd));
		}
		connection = DriverManager.getConnection(databaseURL, props);
		connection.setAutoCommit(autoCommit);
		statementCreator = new StatementCreator(connection);

	}

	/** Closes the connection to the database. */
	public void disconnect() throws SQLException {
		if (connection != null) {
			connection.close();
			unregister();
		}
	}

	private void unregister() {
		//		synchronized (POOL_LOCK) {
		//			POOL.remove(new DHIdentifier(this.databaseURL, this.user));
		//		}
	}

	/** Returns the connection. Might be used in order to create statements. The return
	 *  value might be null if this database handler is not connected. */
	public Connection getConnection() {
		return connection;
	}

	public Statement createStatement(boolean scrollable, boolean updateable) throws SQLException {
		if (connection == null) {
			throw new SQLException("Could not create a statement for '" + databaseURL + "': not connected.");
		}
		Statement statement = null;

		if (scrollable && updateable)
			statement = connection.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);
		else if (scrollable) {
			statement = connection.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_READ_ONLY);
		} else
			statement = connection.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
		return statement;

	}

	/** Create a statement where result sets will have the properties
	 *  TYPE_SCROLL_SENSITIVE and CONCUR_UPDATABLE. This means that the
	 *  ResultSet is scrollable and also updatable. It will also directly show
	 *  all changes to the database made by others after this ResultSet was obtained.
	 *  Will throw an {@link SQLException} if the handler is not connected. */
	public Statement createStatement(boolean scrollable) throws SQLException {
		return createStatement(scrollable, false);
	}

	/** Create a prepared statement where result sets will have the properties
	 *  TYPE_SCROLL_SENSITIVE and CONCUR_UPDATABLE. This means that the
	 *  ResultSet is scrollable and also updatable. It will also directly show
	 *  all changes to the database made by others after this ResultSet was obtained.
	 *  Will throw an {@link SQLException} if the handler is not connected. */
	public PreparedStatement createPreparedStatement(String sqlString, boolean scrollableAndUpdatable) throws SQLException {
		if (connection == null) {
			throw new SQLException("Could not create a prepared statement for '" + databaseURL + "': not connected.");
		}
		if (scrollableAndUpdatable)
			return connection.prepareStatement(sqlString, ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);
		else
			return connection.prepareStatement(sqlString, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
	}

	/**
	 * Makes all changes to the database permanent. Invoking this method explicit is
	 * usually not necessary if the connection was created with AUTO_COMMIT set to true.
	 */
	public void commit() throws SQLException {
		if (connection == null || connection.isClosed()) {
			throw new SQLException("Could not commit: no open connection to database '" + databaseURL + "' !");
		}
		connection.commit();
	}

	/**
	 * Executes the given SQL-Query. Only SQL-statements starting with "SELECT"
	 * (disregarding case) will be accepted. Any errors will result in an
	 * SQL-Exception being thrown.
	 * 
	 * @param sqlQuery An SQL-String.
	 * @return A ResultSet-Object with the results of the query. The ResultSet
	 *         is scrollable, but not updatable. It will not show changes to
	 *         the database made by others after this ResultSet was obtained.
	 *
	 * @deprecated Use the method {@link #createStatement(boolean)} instead and perform the queries explicitely since this method would not allow closing the statement
	 */
	@Deprecated
	public ResultSet query(String sqlQuery) throws SQLException {
		if (!sqlQuery.toLowerCase().startsWith("select")) {
			throw new SQLException("Query: Only SQL-Statements starting with SELECT are allowed: " + sqlQuery);
		}
		Statement st = createStatement(false);
		ResultSet rs = st.executeQuery(sqlQuery);
		return rs;
	}

	/** Adds a column for the given attribute to the table with name tableName. */
	public void addColumn(Attribute attribute, String tableName) throws SQLException {
		// drop the column if necessary
		boolean exists = existsColumnInTable(tableName, attribute.getName());

		if (exists) {
			removeColumn(attribute, tableName);
		}

		// create new column
		Statement st = null;
		try {
			st = connection.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE);
			String query =
					"ALTER TABLE " +
							statementCreator.makeIdentifier(tableName) +
							" ADD COLUMN " +
							statementCreator.makeColumnCreator(attribute);
			st.execute(query);
		} catch (SQLException e) {
			throw e;
		} finally {
			if (st != null)
				st.close();
		}
	}

	/**
	 * Removes the column of the given attribute from the table with name
	 * tableName.
	 */
	public void removeColumn(Attribute attribute, String tableName) throws SQLException {
		Statement st = null;
		try {
			st = connection.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE);
			String query =
					"ALTER TABLE " +
							statementCreator.makeIdentifier(tableName) +
							" DROP COLUMN " +
							statementCreator.makeColumnIdentifier(attribute);
			st.execute(query);
		} catch (SQLException e) {
			throw e;
		} finally {
			if (st != null)
				st.close();
		}
	}

	/**
	 * Drops the table with the given name
	 * 
	 * @param tableName
	 * @throws SQLException
	 */
	public void dropTable(TableName tableName) throws SQLException {
		Statement statement = createStatement(false);
		statement.executeUpdate(statementCreator.makeDropStatement(tableName));
		statement.close();
	}

	@Deprecated
	public void dropTable(String tableName) throws SQLException {
		dropTable(new TableName(tableName));
	}

	/**
	 * Deletes all entries of the table with the given name.
	 * 
	 * @param tableName
	 * @throws SQLException
	 */
	public void emptyTable(TableName tableName) throws SQLException {
		Statement statement = createStatement(false);
		statement.executeUpdate(statementCreator.makeDeleteStatement(tableName));
		statement.close();
	}

	/**
	 * Deletes all entries of the table with the given name.
	 * 
	 * @param tableName
	 * @throws SQLException
	 * @Deprecated use {@link #emptyTable(TableName)} instead.
	 */
	@Deprecated
	public void emptyTable(String tableName) throws SQLException {
		emptyTable(new TableName(tableName));
	}

	/** Delegate method: Creates a new table in this connection and fills it with the provided data.
	 * @param generatedPrimaryKeyAttributeName
	 * 
	 *  @throws SQLException if the table should be overwritten but a table with this name already exists
	 */
	public void createTable(ExampleSet exampleSet, String tableName, int overwriteMode, boolean firstAttempt, int defaultVarcharLength) throws SQLException {
		createTable(exampleSet, tableName, overwriteMode, firstAttempt, defaultVarcharLength, false, "does_not_matter");
	}

	public void createTable(ExampleSet exampleSet, String tableName, int overwriteMode, boolean firstAttempt, int defaultVarcharLength, boolean addAutoGeneratedPrimaryKeys, String generatedPrimaryKeyAttributeName) throws SQLException {
		try {
			createTable(exampleSet, new TableName(tableName), overwriteMode, firstAttempt, defaultVarcharLength, addAutoGeneratedPrimaryKeys, generatedPrimaryKeyAttributeName, 1, null);
		} catch (ProcessStoppedException e) {
			// this cannot happen with a null operator parameter
		}
	}

	public void createTable(ExampleSet exampleSet, TableName tableName, int overwriteMode, boolean firstAttempt, int defaultVarcharLength, boolean addAutoGeneratedPrimaryKeys, String generatedPrimaryKeyAttributeName, int batchSize) throws SQLException {
		try {
			createTable(exampleSet, tableName, overwriteMode, firstAttempt, defaultVarcharLength, addAutoGeneratedPrimaryKeys, generatedPrimaryKeyAttributeName, batchSize, null);
		} catch (ProcessStoppedException e) {
			// this cannot happen with a null operator parameter
		}
	}

	/** Creates a new table in this connection and fills it with the provided data.
	 * @param generatedPrimaryKeyAttributeName
	 * 
	 *  @throws SQLException if the table should be overwritten but a table with this name already exists
	 * @throws ProcessStoppedException 
	 */
	public void createTable(ExampleSet exampleSet, TableName tableName, int overwriteMode, boolean firstAttempt, int defaultVarcharLength, boolean addAutoGeneratedPrimaryKeys, String generatedPrimaryKeyAttributeName, int batchSize, Operator operator) throws SQLException, ProcessStoppedException {
		// either drop the table or throw an exception (depending on the parameter 'overwrite')
		Statement statement = createStatement(false);
		boolean exists = existsTable(tableName);

		// drop table?
		if (exists) {
			switch (overwriteMode) {
				case OVERWRITE_MODE_NONE:
					throw new SQLException("Table with name '" + tableName + "' already exists and overwriting mode is not activated." + Tools.getLineSeparator() +
							"Please change table name or activate overwriting mode.");
				case OVERWRITE_MODE_OVERWRITE:
					statement.executeUpdate(statementCreator.makeDropStatement(tableName));
					// create new table
					exampleSet.recalculateAllAttributeStatistics(); // necessary for updating the possible nominal values
					String createTableString = statementCreator.makeTableCreator(exampleSet.getAttributes(), tableName, defaultVarcharLength);
					statement.executeUpdate(createTableString);
					statement.close();
					break;
				case OVERWRITE_MODE_OVERWRITE_FIRST:
					if (firstAttempt) {
						statement.executeUpdate(statementCreator.makeDropStatement(tableName));
						// create new table
						exampleSet.recalculateAllAttributeStatistics(); // necessary for updating the possible nominal values
						createTableString = statementCreator.makeTableCreator(exampleSet.getAttributes(), tableName, defaultVarcharLength);
						statement.executeUpdate(createTableString);
						statement.close();
					}
					break;
				default:
					break;
			}
		} else {
			// create new table
			exampleSet.recalculateAllAttributeStatistics(); // necessary for updating the possible nominal values
			String createTableString = statementCreator.makeTableCreator(exampleSet.getAttributes(), tableName, defaultVarcharLength);
			statement.executeUpdate(createTableString);
			statement.close();
		}

		// fill table
		Attribute genPrimaryKeyAttribute = null;
		PreparedStatement batchSizeInsertStatement = null;

		try {
			batchSizeInsertStatement = getInsertIntoTableStatement(tableName, exampleSet, addAutoGeneratedPrimaryKeys);

			if (batchSizeInsertStatement != null) {

				boolean oldAutoCommitStatus = true;
				oldAutoCommitStatus = batchSizeInsertStatement.getConnection().getAutoCommit();
				/* set auto commit of the connection to false to speed up insert and to enter transaction mode */
				batchSizeInsertStatement.getConnection().setAutoCommit(false);

				if (addAutoGeneratedPrimaryKeys) {
					genPrimaryKeyAttribute = AttributeFactory.createAttribute(generatedPrimaryKeyAttributeName, Ontology.INTEGER);
					exampleSet.getExampleTable().addAttribute(genPrimaryKeyAttribute);
					exampleSet.getAttributes().addRegular(genPrimaryKeyAttribute);
				}

				int counter = 0;
				int rowCounter = 0;
				ArrayList<Integer> generatedKeysList = new ArrayList<Integer>();
				boolean needToCommitBatch = false;
				for (Example example : exampleSet) {
					applyBatchInsertIntoTable(batchSizeInsertStatement, example, exampleSet.getAttributes().allAttributeRoles(), addAutoGeneratedPrimaryKeys, genPrimaryKeyAttribute);
					needToCommitBatch = true;
					++rowCounter;
					if ((rowCounter % batchSize) == 0) {
						batchSizeInsertStatement.executeBatch();
						needToCommitBatch = false;
						if (addAutoGeneratedPrimaryKeys) {

							ResultSet generatedKeys = batchSizeInsertStatement.getGeneratedKeys();
							while (generatedKeys.next()) {
								int key = generatedKeys.getInt(1);
								generatedKeysList.add(key);
							}
						}

					}
					if (operator != null) {
						++counter;
						if (counter % 100 == 0) {
							operator.checkForStop();
							counter = 0;
						}
					}
				}
				if (needToCommitBatch) {
					batchSizeInsertStatement.executeBatch();
					needToCommitBatch = false;
					if (addAutoGeneratedPrimaryKeys) {
						ResultSet generatedKeys = batchSizeInsertStatement.getGeneratedKeys();
						while (generatedKeys.next()) {
							int key = generatedKeys.getInt(1);
							generatedKeysList.add(key);
						}
					}
				}
				if (addAutoGeneratedPrimaryKeys) {
					if (generatedKeysList.size() != exampleSet.size()) {
						throw new SQLException("The table does not contain a auto increment primary key. Please deactivate the Parameter \""
								+ DatabaseExampleSetWriter.PARAMETER_GET_GENERATED_PRIMARY_KEYS + "\".");
					}
					for (int i = 0; i < generatedKeysList.size(); i++) {
						exampleSet.getExample(i).setValue(genPrimaryKeyAttribute, generatedKeysList.get(i));
					}
				}
				batchSizeInsertStatement.getConnection().commit();
				batchSizeInsertStatement.getConnection().setAutoCommit(oldAutoCommitStatus);
			}

		} finally {
			if (batchSizeInsertStatement != null) {
				batchSizeInsertStatement.close();
			}
		}
	}

//    private PreparedStatement getInsertIntoTableStatement(TableName tableName, ExampleSet exampleSet, boolean addAutoGeneratedPrimaryKeys) throws SQLException {
//    	return getInsertIntoTableStatement(tableName, exampleSet, addAutoGeneratedPrimaryKeys, 1);
//    }

	private PreparedStatement getInsertIntoTableStatement(TableName tableName, ExampleSet exampleSet, boolean addAutoGeneratedPrimaryKeys) throws SQLException {
		if (connection == null) {
			throw new SQLException("Could not create a prepared statement for '" + databaseURL + "': not connected.");
		}
		if (addAutoGeneratedPrimaryKeys) {
			return connection.prepareStatement(statementCreator.makeInsertStatement(tableName, exampleSet), Statement.RETURN_GENERATED_KEYS);
		} else {
			// Commented method will cause Exception on Access.
			// return connection.prepareStatement(statementCreator.makeInsertStatement(tableName, exampleSet), Statement.NO_GENERATED_KEYS);
			return connection.prepareStatement(statementCreator.makeInsertStatement(tableName, exampleSet));

		}
		//return createPreparedStatement(statementCreator.makeInsertStatement(tableName, exampleSet), true);
	}

//    private void applyInsertIntoTable(PreparedStatement statement, Example example, Iterator<AttributeRole> attributes, boolean addAutoGeneratedPrimaryKeys, Attribute genPrimaryKey) throws SQLException {
//    	List<Example> wrapperList = new LinkedList<Example>();
//    	wrapperList.add(example);
//    	applyBatchInsertIntoTable(statement, wrapperList, attributes, addAutoGeneratedPrimaryKeys, genPrimaryKey);
//    }

	private void applyBatchInsertIntoTable(PreparedStatement statement, Example example, Iterator<AttributeRole> attributes, boolean addAutoGeneratedPrimaryKeys, Attribute genPrimaryKey) throws SQLException {
		// create List of attributes from attributes iterator for reuse:
		List<Attribute> attributeList = new LinkedList<Attribute>();
		while (attributes.hasNext()) {
			attributeList.add(attributes.next().getAttribute());
		}

		int counter = 1;
		for (Attribute attribute : attributeList) {
			if (addAutoGeneratedPrimaryKeys && attribute == genPrimaryKey) {
				continue;
			}
			double value = example.getValue(attribute);
			if (Double.isNaN(value)) {
				int sqlType = statementCreator.getSQLTypeForRMValueType(attribute.getValueType()).getDataType();
				statement.setNull(counter, sqlType);
			} else {
				if (attribute.isNominal()) {
					String valueString = attribute.getMapping().mapIndex((int) value);
					statement.setString(counter, valueString);
				} else {
					if (Ontology.ATTRIBUTE_VALUE_TYPE.isA(attribute.getValueType(), Ontology.DATE_TIME)) {
						if (Ontology.ATTRIBUTE_VALUE_TYPE.isA(attribute.getValueType(), Ontology.TIME)) {
							statement.setTime(counter, new Time((long) value));
						} else {
							statement.setTimestamp(counter, new Timestamp((long) value));
						}
					} else {
						statement.setDouble(counter, value);
					}
				}
			}
			counter++;
		}

		statement.addBatch();

	}

	//	private String getCreateTableString(ExampleSet exampleSet, String tableName, int defaultVarcharLength) {
	//		// define all attribute names and types
	//		StringBuffer result = new StringBuffer();
	//		result.append("CREATE TABLE " + properties.getIdentifierQuoteOpen() + tableName + properties.getIdentifierQuoteClose() + "(");
	//		Iterator<AttributeRole> a = exampleSet.getAttributes().allAttributeRoles();
	//		boolean first = true;
	//		while (a.hasNext()) {
	//			if (!first)
	//				result.append(", ");
	//			first = false;
	//			AttributeRole attributeRole = a.next();
	//			result.append(getCreateAttributeString(attributeRole, defaultVarcharLength));
	//		}
	//
	//		// set primary key
	//		Attribute idAttribute = exampleSet.getAttributes().getId();
	//		if (idAttribute != null) {
	//			result.append(", PRIMARY KEY( " + properties.getIdentifierQuoteOpen() + idAttribute.getName() + properties.getIdentifierQuoteClose() + " )");
	//		}
	//
	//		result.append(")");
	//		return result.toString();
	//	}

	//	/** Creates the name and type string for the given attribute. Id attributes
	//	 *  must not be null.
	//	 */
	//	private String getCreateAttributeString(AttributeRole attributeRole, int defaultVarcharLength) {
	//		Attribute attribute = attributeRole.getAttribute();
	//		StringBuffer result = new StringBuffer(properties.getIdentifierQuoteOpen() + attribute.getName() + properties.getIdentifierQuoteClose() + " ");
	//		if (attribute.isNominal()) {
	//			int varCharLength = 1; // at least length 1
	//			if (defaultVarcharLength != -1) {
	//				varCharLength = defaultVarcharLength;
	//			} else {
	//				for (String value : attribute.getMapping().getValues()) {
	//					varCharLength = Math.max(varCharLength, value.length());
	//				}
	//			}
	//			if (attribute.getValueType() != Ontology.STRING)
	//				result.append(properties.getVarcharName() + "(" + varCharLength + ")");
	//			else
	//				result.append(properties.getTextName());
	//		} else {
	//			if (Ontology.ATTRIBUTE_VALUE_TYPE.isA(attribute.getValueType(), Ontology.INTEGER)) {
	//				result.append(properties.getIntegerName());
	//			} else if (Ontology.ATTRIBUTE_VALUE_TYPE.isA(attribute.getValueType(), Ontology.DATE_TIME)){
	//				if (Ontology.ATTRIBUTE_VALUE_TYPE.isA(attribute.getValueType(), Ontology.DATE)){
	//					result.append(properties.getDateName());
	//				} else if (Ontology.ATTRIBUTE_VALUE_TYPE.isA(attribute.getValueType(), Ontology.TIME)){
	//					result.append(properties.getTimeName());
	//				} else { // Date_time
	//					result.append(properties.getDateTimeName());
	//				}
	//			} else {
	//				result.append(properties.getRealName());
	//			}
	//		}
	//
	//		// id must not be null
	//		if (attributeRole.isSpecial())
	//			if (attributeRole.getSpecialName().equals(Attributes.ID_NAME))
	//				result.append(" NOT NULL");
	//
	//		return result.toString();
	//	}

	/**
	 * Returns for the given SQL-type the name of the corresponding RapidMiner-Type
	 * from com.rapidminer.tools.Ontology.
	 */
	public static int getRapidMinerTypeIndex(int sqlType) {
		switch (sqlType) {
			case Types.BIGINT:
			case Types.INTEGER:
			case Types.TINYINT:
			case Types.SMALLINT:
				return Ontology.INTEGER;

			case Types.FLOAT:
			case Types.REAL:
			case Types.DECIMAL:
			case Types.DOUBLE:
				return Ontology.REAL;

			case Types.NUMERIC:
				return Ontology.NUMERICAL;
			case Types.BLOB:
			case Types.CLOB:
			case Types.LONGVARCHAR:
				return Ontology.STRING;

			case Types.CHAR:
			case Types.VARCHAR:
			case Types.BINARY:
			case Types.BIT:
			case Types.LONGVARBINARY:
			case Types.JAVA_OBJECT:
			case Types.STRUCT:
			case Types.VARBINARY:
				return Ontology.NOMINAL;

			case Types.DATE:
				return Ontology.DATE;
			case Types.TIME:
				return Ontology.TIME;
			case Types.TIMESTAMP:
				return Ontology.DATE_TIME;

			default:
				return Ontology.NOMINAL;
		}
	}

	/**
	 * Creates a list of attributes reflecting the result set's column meta
	 * data.
	 */
	public static List<Attribute> createAttributes(ResultSet rs) throws SQLException {
		List<Attribute> attributes = new LinkedList<Attribute>();

		if (rs == null) {
			throw new IllegalArgumentException("Cannot create attributes: ResultSet must not be null!");
		}

		ResultSetMetaData metadata;
		try {
			metadata = rs.getMetaData();
		} catch (NullPointerException npe) {
			throw new RuntimeException("Could not create attribute list: ResultSet object seems closed.");
		}

		int numberOfColumns = metadata.getColumnCount();

		for (int column = 1; column <= numberOfColumns; column++) {
			String name = metadata.getColumnLabel(column);
			Attribute attribute = AttributeFactory.createAttribute(name, getRapidMinerTypeIndex(metadata.getColumnType(column)));
			attributes.add(attribute);
		}

		return attributes;
	}

	/** Checks whether a table with the given name exists. */
	private boolean existsTable(TableName tableName) throws SQLException {
		ResultSet tableNames = connection.getMetaData().getTables(tableName.getCatalog(), tableName.getSchema(), tableName.getTableName(), null);
		return tableNames.next();
	}

	/** Checks whether the given column exists in the given data base. */
	private boolean existsColumnInTable(String tableName, String columnName) throws SQLException {
		return connection.getMetaData().getColumns(null, null, tableName, columnName).next();
	}

	public Map<TableName, List<ColumnIdentifier>> getAllTableMetaData() throws SQLException {
		return getAllTableMetaData(null, 0, 0, true);
	}

	/** Fetches meta data about all tables and, if selected, all columns in the database.
	 *  The returned map maps table names to column descriptions.
	 *  If fetchColumns is false, all lists in the returned map will be empty lists, so basically
	 *  only the key set contains useful information. */
	public Map<TableName, List<ColumnIdentifier>> getAllTableMetaData(ProgressListener progressListener, int minProgress, int maxProgress, boolean fetchColumns) throws SQLException {
		if (connection == null) {
			throw new SQLException("Could not retrieve all table names: no open connection to database '" + databaseURL + "' !");
		}
		if (connection.isClosed()) {
			unregister();
			throw new SQLException("Could not retrieve all table names: connection is closed.");
		}

		DatabaseMetaData metaData = connection.getMetaData();
		String[] types;
		if (!"false".equals(ParameterService.getParameterValue(RapidMiner.PROPERTY_RAPIDMINER_TOOLS_DB_ONLY_STANDARD_TABLES))) {
			types = new String[] { "TABLE" };
		} else {
			types = null;
		}
		//ResultSet tableNames = metaData.getTables(null, null, "%", types);
		ResultSet tableNames = metaData.getTables(null, null, null, types);
		List<TableName> tableNameList = new LinkedList<TableName>();

		while (tableNames.next()) {
			String tableName = tableNames.getString("TABLE_NAME");
			String tableSchem = tableNames.getString("TABLE_SCHEM");
			String tableCat = tableNames.getString("TABLE_CAT");

			String remark = tableNames.getString("REMARKS");

			//tableNameList.add(tableCat+"."+tableSchem+"."+tableName);
			final TableName tableNameObject = new TableName(tableName, tableSchem, tableCat);
			tableNameObject.setComment(remark);
			tableNameList.add(tableNameObject);
		}
		tableNames.close();

		Map<TableName, List<ColumnIdentifier>> result = new LinkedHashMap<TableName, List<ColumnIdentifier>>();
		Iterator<TableName> i = tableNameList.iterator();
		final int size = tableNameList.size();
		int count = 0;
		while (i.hasNext()) {
			TableName tableName = i.next();
			if (progressListener != null && size > 0) {
				progressListener.setCompleted(count * (maxProgress - minProgress) / size + minProgress);
			}
			count++;
			if (fetchColumns) {
				try {
					// test: will fail if user can not use this table
					List<ColumnIdentifier> columnNames = getAllColumnNames(tableName, metaData);
					result.put(tableName, columnNames);
				} catch (SQLException e) {
					//LogService.getRoot().log(Level.WARNING, "Failed to fetch column meta data for table '"+tableName+"': "+e, e);
					LogService.getRoot().log(Level.WARNING,
							I18N.getMessage(LogService.getRoot().getResourceBundle(),
									"com.rapidminer.tools.jdbc.DatabaseHandler.fetching_column_metadata_error",
									tableName, e),
							e);
					result.put(tableName, Collections.<ColumnIdentifier> emptyList());
				}
			} else {
				result.put(tableName, Collections.<ColumnIdentifier> emptyList());
			}
		}
		return result;
	}

	public List<ColumnIdentifier> getAllColumnNames(TableName tableName, DatabaseMetaData metaData) throws SQLException {
		if (tableName == null) {
			throw new SQLException("Cannot read column names: table name must not be null!");
		}

		Statement statement = null;
		ResultSet columnResult = null;
		ResultSet emptyQueryResult = null;
		try {
			statement = createStatement(false);
			try {
				columnResult = metaData.getColumns(tableName.getCatalog(), tableName.getSchema(), tableName.getTableName(), "%");
				List<ColumnIdentifier> result = new LinkedList<ColumnIdentifier>();
				while (columnResult.next()) {
					String remarks = columnResult.getString("REMARKS");
					if (remarks != null && remarks.isEmpty()) {
						remarks = null;
					}
					result.add(new ColumnIdentifier(this, tableName,
							columnResult.getString("COLUMN_NAME"),
							columnResult.getInt("DATA_TYPE"),
							columnResult.getString("TYPE_NAME"),
							remarks));
				}
				//columnResult.close();
				return result;
			} catch (SQLException e) {
				// Fallback for Oracle with illegal characters in table name. (Will throw exception in
				// getMetaData().getColumns())
				List<ColumnIdentifier> result = new LinkedList<ColumnIdentifier>();
				String emptySelect = "SELECT * FROM " + statementCreator.makeIdentifier(tableName) + " WHERE 0=1";
				emptyQueryResult = statement.executeQuery(emptySelect);
				final ResultSetMetaData resultSetMetaData = emptyQueryResult.getMetaData();
				for (int i = 0; i < resultSetMetaData.getColumnCount(); i++) {
					result.add(new ColumnIdentifier(this, tableName,
							resultSetMetaData.getColumnName(i + 1),
							resultSetMetaData.getColumnType(i + 1),
							resultSetMetaData.getColumnTypeName(i + 1),
							null));
				}
				return result;
			}
		} finally {
			if (columnResult != null) {
				columnResult.close();
			}
			if (emptyQueryResult != null) {
				emptyQueryResult.close();
			}
			if (statement != null) {
				statement.close();
			}
		}
	}

	public static DatabaseHandler getConnectedDatabaseHandler(Operator operator) throws OperatorException, SQLException {
		switch (operator.getParameterAsInt(PARAMETER_DEFINE_CONNECTION)) {
			case CONNECTION_MODE_PREDEFINED:
				String repositoryName = null;
				if (operator.getProcess() != null) {
					RepositoryLocation repositoryLocation = operator.getProcess().getRepositoryLocation();
					if (repositoryLocation != null) {
						repositoryName = repositoryLocation.getRepositoryName();
					}
				}
				ConnectionEntry entry = DatabaseConnectionService.getConnectionEntry(operator.getParameterAsString(PARAMETER_CONNECTION), repositoryName);
				if (entry == null) {
					throw new UserError(operator, 318, operator.getParameterAsString(PARAMETER_CONNECTION));
				}
				return getConnectedDatabaseHandler(entry); //.getURL(), entry.getUser(), new String(entry.getPassword()));
			case DatabaseHandler.CONNECTION_MODE_JNDI:
				final String jndiName = operator.getParameterAsString(PARAMETER_JNDI_NAME);
				try {
					InitialContext ctx;
					ctx = new InitialContext();
					DataSource source = (DataSource) ctx.lookup(jndiName);
					return getHandler(source.getConnection());
				} catch (NamingException e) {
					throw new OperatorException("Failed to lookup '" + jndiName + "': " + e, e);
				}
			case DatabaseHandler.CONNECTION_MODE_URL:
			default:
				return getConnectedDatabaseHandler(operator.getParameterAsString(PARAMETER_DATABASE_URL),
						operator.getParameterAsString(PARAMETER_USERNAME),
						operator.getParameterAsString(PARAMETER_PASSWORD));
		}
	}

	/** Returns the table selected by parameters {@link #PARAMETER_USE_DEFAULT_SCHEMA}, {@link #PARAMETER_SCHEMA_NAME},
	 *  and {@link #PARAMETER_TABLE_NAME}. */
	public static TableName getSelectedTableName(ParameterHandler operator) throws UndefinedParameterError {
		if (operator.getParameterAsBoolean(PARAMETER_USE_DEFAULT_SCHEMA)) {
			return new TableName(operator.getParameterAsString(PARAMETER_TABLE_NAME));
		} else {
			return new TableName(operator.getParameterAsString(PARAMETER_TABLE_NAME), operator.getParameterAsString(PARAMETER_SCHEMA_NAME), null);
		}
	}

	public static ConnectionEntry getConnectionEntry(Operator operator) {
		RepositoryLocation repositoryLocation = operator.getProcess().getRepositoryLocation();
		String repositoryName = null;
		if (repositoryLocation != null) {
			repositoryName = repositoryLocation.getRepositoryName();
		}
		return getConnectionEntry(operator, repositoryName);
	}

	public static ConnectionEntry getConnectionEntry(ParameterHandler parameterHandler) {
		return (getConnectionEntry(parameterHandler, null));
	}

	public static ConnectionEntry getConnectionEntry(ParameterHandler parameterHandler, String repositoryName) {
		try {
			final int connectionMode = parameterHandler.getParameterAsInt(DatabaseHandler.PARAMETER_DEFINE_CONNECTION);
			switch (connectionMode) {
				case DatabaseHandler.CONNECTION_MODE_PREDEFINED:
					return DatabaseConnectionService.getConnectionEntry(parameterHandler.getParameterAsString(DatabaseHandler.PARAMETER_CONNECTION), repositoryName);
				case DatabaseHandler.CONNECTION_MODE_URL:
					final String connectionUrl = parameterHandler.getParameterAsString(DatabaseHandler.PARAMETER_DATABASE_URL);
					final String connectionUsername = parameterHandler.getParameterAsString(DatabaseHandler.PARAMETER_USERNAME);
					final char[] connectionPassword = parameterHandler.getParameterAsString(DatabaseHandler.PARAMETER_PASSWORD).toCharArray();
					return new ConnectionEntry("urlConnection", DatabaseService.getJDBCProperties().get(parameterHandler.getParameterAsInt(DatabaseHandler.PARAMETER_DATABASE_SYSTEM))) {

						@Override
						public String getURL() {
							return connectionUrl;
						}

						@Override
						public String getUser() {
							return connectionUsername;
						}

						@Override
						public char[] getPassword() {
							return connectionPassword;
						}
					};
				case DatabaseHandler.CONNECTION_MODE_JNDI:
				default:
					return null;
			}
		} catch (UndefinedParameterError e) {}
		return null;
	}

	public static List<ParameterType> getConnectionParameterTypes(ParameterHandler handler) {
		List<ParameterType> types = new LinkedList<ParameterType>();
		ParameterType type = new ParameterTypeCategory(PARAMETER_DEFINE_CONNECTION, "Indicates how the database connection should be specified.", CONNECTION_MODES, CONNECTION_MODE_PREDEFINED);
		type.setExpert(false);
		types.add(type);

		type = new ParameterTypeDatabaseConnection(PARAMETER_CONNECTION, "A predefined database connection.");
		type.registerDependencyCondition(new EqualTypeCondition(handler, PARAMETER_DEFINE_CONNECTION, CONNECTION_MODES, true, CONNECTION_MODE_PREDEFINED));
		type.setExpert(false);
		types.add(type);

		type = new ParameterTypeCategory(PARAMETER_DATABASE_SYSTEM, "The used database system.", DatabaseService.getDBSystemNames(), 0);
		type.registerDependencyCondition(new EqualTypeCondition(handler, PARAMETER_DEFINE_CONNECTION, CONNECTION_MODES, true, CONNECTION_MODE_URL));
		type.setExpert(false);
		types.add(type);

		type = new ParameterTypeString(PARAMETER_DATABASE_URL, "The URL connection string for the database, e.g. 'jdbc:mysql://foo.bar:portnr/database'");
		type.registerDependencyCondition(new EqualTypeCondition(handler, PARAMETER_DEFINE_CONNECTION, CONNECTION_MODES, true, CONNECTION_MODE_URL));
		type.setExpert(false);
		types.add(type);
		type = new ParameterTypeString(PARAMETER_USERNAME, "The database username.");
		type.registerDependencyCondition(new EqualTypeCondition(handler, PARAMETER_DEFINE_CONNECTION, CONNECTION_MODES, true, CONNECTION_MODE_URL));
		type.setExpert(false);
		types.add(type);
		type = new ParameterTypePassword(PARAMETER_PASSWORD, "The password for the database.");
		type.registerDependencyCondition(new EqualTypeCondition(handler, PARAMETER_DEFINE_CONNECTION, CONNECTION_MODES, true, CONNECTION_MODE_URL));
		type.setExpert(false);
		types.add(type);

		type = new ParameterTypeString(PARAMETER_JNDI_NAME, "JNDI name for a data source.");
		type.registerDependencyCondition(new EqualTypeCondition(handler, PARAMETER_DEFINE_CONNECTION, CONNECTION_MODES, true, CONNECTION_MODE_JNDI));
		type.setExpert(false);
		types.add(type);

		return types;
	}

	public static List<ParameterType> getQueryParameterTypes(ParameterHandler handler, boolean tableOnly) {
		List<ParameterType> types = new LinkedList<ParameterType>();
		ParameterType type = null;
		if (!tableOnly) {
			type = new ParameterTypeCategory(PARAMETER_DEFINE_QUERY, "Specifies whether the database query should be defined directly, through a file or implicitely by a given table name.", QUERY_MODES, QUERY_QUERY);
			type.setExpert(false);
			types.add(type);

			type = new ParameterTypeSQLQuery(PARAMETER_QUERY, "An SQL query.");
			type.registerDependencyCondition(new EqualTypeCondition(handler, PARAMETER_DEFINE_QUERY, QUERY_MODES, true, QUERY_QUERY));
			type.setExpert(false);
			types.add(type);

			type = new ParameterTypeFile(PARAMETER_QUERY_FILE, "A file containing an SQL query.", null, true);
			type.registerDependencyCondition(new EqualTypeCondition(handler, PARAMETER_DEFINE_QUERY, QUERY_MODES, true, QUERY_FILE));
			type.setExpert(false);
			types.add(type);
		}

		type = new ParameterTypeBoolean(PARAMETER_USE_DEFAULT_SCHEMA, "If checked, the user's default schema will be used.", true);
		if (!tableOnly) {
			type.registerDependencyCondition(new EqualTypeCondition(handler, PARAMETER_DEFINE_QUERY, QUERY_MODES, true, QUERY_TABLE));
		}
		type.setExpert(true);
		types.add(type);

		type = new ParameterTypeDatabaseSchema(PARAMETER_SCHEMA_NAME, "The schema name to use, unless use_default_schema is true.", true);
		type.registerDependencyCondition(new BooleanParameterCondition(handler, PARAMETER_USE_DEFAULT_SCHEMA, true, false));
		type.setExpert(true);
		types.add(type);

		type = new ParameterTypeDatabaseTable(PARAMETER_TABLE_NAME, "A database table.");
		if (!tableOnly) {
			type.registerDependencyCondition(new EqualTypeCondition(handler, PARAMETER_DEFINE_QUERY, QUERY_MODES, true, QUERY_TABLE));
		} else {
			type.setOptional(false);
		}
		type.setExpert(false);
		types.add(type);

		return types;
	}

	public static List<ParameterType> getStatementPreparationParamterTypes(ParameterHandler handler) {
		List<ParameterType> types = new LinkedList<ParameterType>();
		ParameterTypeBoolean prepareParam = new ParameterTypeBoolean(DatabaseHandler.PARAMETER_PREPARE_STATEMENT, "If checked, the statement is prepared, and '?'-parameters can be filled in using the parameter 'parameters'.", false);
		types.add(prepareParam);
		ParameterType argumentType = new ParameterTypeTupel("parameter", "Parameter to insert when statement is prepared",
				new ParameterTypeCategory("type", "SQL type to use for insertion.", SQL_TYPES, 0),
				new ParameterTypeString("parameter", "Parameter"));
		ParameterTypeEnumeration paramsParam = new ParameterTypeEnumeration(DatabaseHandler.PARAMETER_PARAMETERS, "Parameters to insert into '?' placeholders when statement is prepared.",
				argumentType);
		paramsParam.registerDependencyCondition(new BooleanParameterCondition(handler, DatabaseHandler.PARAMETER_PREPARE_STATEMENT, false, true));
		types.add(paramsParam);
		return types;
	}

	/** Executes a statement. Parameter must have parameters of {@link #getStatementPreparationParamterTypes(ParameterHandler)}
	 *  added.
	 *  If prepared statement was selected in parameter handler, a PreparedStatement is executed,
	 *  and parameters specified in parameter handler will be filled in.
	 *  Statement is closed unless isQuery.
	 * 
	 * @param sql The sql statement
	 * @param isQuery If true, a ResultSet is returned
	 * @return ResultSet if isQuery is true, null otherwise
	 * @throws OperatorException
	 */
	public ResultSet executeStatement(String sql, boolean isQuery, Operator parameterHandler, Logger logger) throws SQLException, OperatorException {
		ResultSet resultSet = null;
		Statement statement;
		if (parameterHandler.getParameterAsBoolean(DatabaseHandler.PARAMETER_PREPARE_STATEMENT)) {
			PreparedStatement prepared = getConnection().prepareStatement(sql);
			String[] parameters = ParameterTypeEnumeration.transformString2Enumeration(parameterHandler.getParameterAsString(DatabaseHandler.PARAMETER_PARAMETERS));
			for (int i = 0; i < parameters.length; i++) {
				String[] argDescription = ParameterTypeTupel.transformString2Tupel(parameters[i]);
				final String sqlType = argDescription[0];
				final String replacementValue = argDescription[1];
				if ("VARCHAR".equals(sqlType)) {
					prepared.setString(i + 1, replacementValue);
				} else if ("REAL".equals(sqlType)) {
					try {
						prepared.setDouble(i + 1, Double.parseDouble(replacementValue));
					} catch (NumberFormatException e) {
						prepared.close();
						throw new UserError(parameterHandler, 158, replacementValue, sqlType);
					}
				} else if ("LONG".equals(sqlType)) {
					try {
						prepared.setLong(i + 1, Long.parseLong(replacementValue));
					} catch (NumberFormatException e) {
						prepared.close();
						throw new UserError(parameterHandler, 158, replacementValue, sqlType);
					}
				} else if ("INTEGER".equals(sqlType)) {
					try {
						prepared.setInt(i + 1, Integer.parseInt(replacementValue));
					} catch (NumberFormatException e) {
						prepared.close();
						throw new UserError(parameterHandler, 158, replacementValue, sqlType);
					}
				} else {
					prepared.close();
					throw new OperatorException("Illegal data type: " + sqlType);
				}
			}
			if (isQuery) {
				resultSet = prepared.executeQuery();
			} else {
				prepared.execute();
			}
			statement = prepared;
		} else {
			logger.info("Executing query: '" + sql + "'");
			statement = createStatement(false);
			if (isQuery) {
				resultSet = statement.executeQuery(sql);
			} else {
				statement.execute(sql);
			}
		}
		logger.fine("Query executed.");
		if (!isQuery) {
			statement.close();
		}
		return resultSet;
	}

	/**
	 * Return the url of the database for this instance.
	 * @return
	 */
	public String getDatabaseUrl() {
		return databaseURL;
	}

	/**
	 * Updates a given table with the values from the given {@link ExampleSet}. The row(s) to update are specified via dbIdColumn and idAtt parameters.
	 * If the id column of the table does not contain an id value of the {@link ExampleSet}, the row is inserted instead.
	 * The {@link ExampleSet} attribute names must be a subset of the table column names, otherwise the operation will fail.
	 * @param exampleSet the {@link ExampleSet}
	 * @param selectedTableName the name of the table in the database
	 * @param idAttributes the id {@link Attribute}s of the {@link ExampleSet}
	 * @param logger optional: a {@link Logger}
	 * @throws SQLException
	 */
	public void updateTable(ExampleSet exampleSet, TableName selectedTableName, Set<Attribute> idAttributes, Logger logger) throws SQLException {
		StatementCreator sc = new StatementCreator(getConnection());

		List<ColumnIdentifier> allColumnNames = getAllColumnNames(selectedTableName, getConnection().getMetaData());
		List<String> columnTableNames = new LinkedList<String>();
		for (ColumnIdentifier identifier : allColumnNames) {
			columnTableNames.add(identifier.getColumnName());
		}
		StringBuilder updateAttStringBuilder = new StringBuilder();
		StringBuilder updateIdStringBuilder = new StringBuilder();
		StringBuilder insertAttStringBuilder = new StringBuilder();
		StringBuilder insertValStringBuilder = new StringBuilder();
		updateAttStringBuilder.append("SET ");
		insertAttStringBuilder.append('(');
		insertValStringBuilder.append('(');
		Iterator<Attribute> it = exampleSet.getAttributes().allAttributes();
		int attCount = 0;
		List<Attribute> attList = new LinkedList<Attribute>();
		List<Attribute> allAttList = new LinkedList<Attribute>();
		List<String> idAttNameList = new LinkedList<String>();
		for (Attribute idAtt : idAttributes) {
			idAttNameList.add(idAtt.getName());
		}
		while (it.hasNext()) {
			Attribute att = it.next();
			String attName = att.getName();

			// make sure the table contains all columns of the example set
			if (!columnTableNames.contains(attName)) {
				throw new SQLException("Table does not contain column for attribute " + attName + "!");
			}

			// iterate over all attributes except for the id attributes and add them to the update statement
			if (!idAttNameList.contains(attName)) {
				attCount++;
				attList.add(att);
				updateAttStringBuilder.append(sc.makeIdentifier(attName) + " = ?");
				updateAttStringBuilder.append(", ");
				insertAttStringBuilder.append(sc.makeIdentifier(attName) + ", ");
				insertValStringBuilder.append("?, ");
			} else {
				// id attributes need to be added to the insert statement as well
				insertAttStringBuilder.append(sc.makeIdentifier(attName) + ", ");
				insertValStringBuilder.append("?, ");
				updateIdStringBuilder.append(sc.makeIdentifier(attName) + " = ?");
				updateIdStringBuilder.append(" AND ");
			}
			allAttList.add(att);
		}

		// fix the StringBuilders
		if (updateAttStringBuilder.substring(updateAttStringBuilder.length() - 2, updateAttStringBuilder.length()).equals(", ")) {
			updateAttStringBuilder.delete(updateAttStringBuilder.length() - 2, updateAttStringBuilder.length());
		}
		if (updateIdStringBuilder.substring(updateIdStringBuilder.length() - 5, updateIdStringBuilder.length()).equals(" AND ")) {
			updateIdStringBuilder.delete(updateIdStringBuilder.length() - 5, updateIdStringBuilder.length());
		}
		if (insertAttStringBuilder.substring(insertAttStringBuilder.length() - 2, insertAttStringBuilder.length()).equals(", ")) {
			insertAttStringBuilder.delete(insertAttStringBuilder.length() - 2, insertAttStringBuilder.length());
			insertAttStringBuilder.append(')');
		}
		if (insertValStringBuilder.substring(insertValStringBuilder.length() - 2, insertValStringBuilder.length()).equals(", ")) {
			insertValStringBuilder.delete(insertValStringBuilder.length() - 2, insertValStringBuilder.length());
			insertValStringBuilder.append(')');
		}

		// create prepared statements for UPDATE and INSERT (which is used in case UPDATE finds no matching row to update)		
		String updateStatementString = "UPDATE " + sc.makeIdentifier(selectedTableName) + " " + updateAttStringBuilder.toString() + " WHERE " + updateIdStringBuilder.toString();
		String insertStatementString = "INSERT INTO " + sc.makeIdentifier(selectedTableName) + " " + insertAttStringBuilder.toString() + " VALUES " + insertValStringBuilder.toString();
		String selectStatementString = "SELECT COUNT(*) FROM " + sc.makeIdentifier(selectedTableName) + " WHERE " + updateIdStringBuilder.toString();
		PreparedStatement prepUpdateStatement = createPreparedStatement(updateStatementString, false);
		PreparedStatement prepInsertStatement = createPreparedStatement(insertStatementString, false);
		PreparedStatement prepSelectStatement = createPreparedStatement(selectStatementString, false);

		// iterate over all examples
		for (Example ex : exampleSet) {
			// get the id value and set it in the statements
			int idCount = 0;
			for (Attribute idAtt : idAttributes) {
				if (idAtt.isNumerical()) {
					// id attributes may not be numerical, so check type
					prepUpdateStatement.setDouble(attCount + ++idCount, ex.getValue(idAtt));
					prepInsertStatement.setDouble(allAttList.indexOf(idAtt) + 1, ex.getValue(idAtt));
					prepSelectStatement.setDouble(idAttNameList.indexOf(idAtt.getName()) + 1, ex.getValue(idAtt));
				} else if (idAtt.isNominal()) {
					prepUpdateStatement.setString(attCount + ++idCount, ex.getValueAsString(idAtt));
					prepInsertStatement.setString(allAttList.indexOf(idAtt) + 1, ex.getValueAsString(idAtt));
					prepSelectStatement.setString(idAttNameList.indexOf(idAtt.getName()) + 1, ex.getValueAsString(idAtt));
				} else {
					// date, time, date_time are not covered above, so handle them here
					if (idAtt.getValueType() == Ontology.DATE) {
						Date date = new Date(new Double(ex.getValue(idAtt)).longValue());
						prepUpdateStatement.setDate(attCount + ++idCount, date);
						prepInsertStatement.setDate(allAttList.indexOf(idAtt) + 1, date);
						prepSelectStatement.setDate(idAttNameList.indexOf(idAtt.getName()) + 1, date);
					} else if (idAtt.getValueType() == Ontology.TIME) {
						Time time = new Time(new Double(ex.getValue(idAtt)).longValue());
						prepUpdateStatement.setTime(attCount + ++idCount, time);
						prepInsertStatement.setTime(allAttList.indexOf(idAtt) + 1, time);
						prepSelectStatement.setTime(idAttNameList.indexOf(idAtt.getName()) + 1, time);
					} else if (idAtt.getValueType() == Ontology.DATE_TIME) {
						Timestamp timestamp = new Timestamp(new Double(ex.getValue(idAtt)).longValue());
						prepUpdateStatement.setTimestamp(attCount + ++idCount, timestamp);
						prepInsertStatement.setTimestamp(allAttList.indexOf(idAtt) + 1, timestamp);
						prepSelectStatement.setTimestamp(idAttNameList.indexOf(idAtt.getName()) + 1, timestamp);
					}
				}
			}

			// fill statements with attribute values
			for (Attribute att : attList) {
				double value = ex.getValue(att);
				// Simon's fix for missing values in the next four lines
				if (Double.isNaN(value)) {
					int sqlType = statementCreator.getSQLTypeForRMValueType(att.getValueType()).getDataType();
					prepUpdateStatement.setNull(attList.indexOf(att) + 1, sqlType);
					prepInsertStatement.setNull(allAttList.indexOf(att) + 1, sqlType);
				} else if (att.isNumerical()) {
					prepUpdateStatement.setDouble(attList.indexOf(att) + 1, ex.getValue(att));
					prepInsertStatement.setDouble(allAttList.indexOf(att) + 1, ex.getValue(att));
				} else if (att.isNominal()) {
					prepUpdateStatement.setString(attList.indexOf(att) + 1, ex.getValueAsString(att));
					prepInsertStatement.setString(allAttList.indexOf(att) + 1, ex.getValueAsString(att));
				} else {
					// date, time, date_time are not covered above, so handle them here
					if (att.getValueType() == Ontology.DATE) {
						Date date = new Date(new Double(ex.getValue(att)).longValue());
						prepUpdateStatement.setDate(attList.indexOf(att) + 1, date);
						prepInsertStatement.setDate(allAttList.indexOf(att) + 1, date);
					} else if (att.getValueType() == Ontology.TIME) {
						Time time = new Time(new Double(ex.getValue(att)).longValue());
						prepUpdateStatement.setTime(attList.indexOf(att) + 1, time);
						prepInsertStatement.setTime(allAttList.indexOf(att) + 1, time);
					} else if (att.getValueType() == Ontology.DATE_TIME) {
						Timestamp timestamp = new Timestamp(new Double(ex.getValue(att)).longValue());
						prepUpdateStatement.setTimestamp(attList.indexOf(att) + 1, timestamp);
						prepInsertStatement.setTimestamp(allAttList.indexOf(att) + 1, timestamp);
					}
				}
			}

			int updatedRowCount = 0;
			// try to update if there is at least one NOT id attribute
			if (attCount > 0) {
				if (logger != null) {
					logger.fine(prepUpdateStatement.toString());
				}
				updatedRowCount = prepUpdateStatement.executeUpdate();
			} else {
				// if there are only id attributes, try and see if entry already exists
				ResultSet rs = prepSelectStatement.executeQuery();
				if (rs.next()) {
					updatedRowCount = rs.getInt(1);
				}
			}
			// no update done || row with given id values does not yet exist, so insert it
			if (updatedRowCount <= 0) {
				if (logger != null) {
					logger.fine(prepInsertStatement.toString());
				}
				prepInsertStatement.executeUpdate();
			}
		}
	}

}
