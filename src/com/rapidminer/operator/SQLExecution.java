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
package com.rapidminer.operator;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

import com.rapidminer.operator.io.CachedDatabaseExampleSource;
import com.rapidminer.operator.io.DatabaseDataReader;
import com.rapidminer.operator.ports.DummyPortPairExtender;
import com.rapidminer.operator.ports.PortPairExtender;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeFile;
import com.rapidminer.parameter.ParameterTypeText;
import com.rapidminer.parameter.TextType;
import com.rapidminer.tools.Tools;
import com.rapidminer.tools.jdbc.DatabaseHandler;

/**
 * <p>This operator performs an arbitrary SQL statement on an SQL
 * database. The SQL query can be passed to RapidMiner via a parameter or, in case of
 * long SQL statements, in a separate file. Please note that column names are
 * often case sensitive. Databases may behave differently here.</p>
 * 
 * <p>Please note that this operator cannot be used to load data from databases 
 * but mereley to execute SQL statements like CREATE or ADD etc. In oder to load
 * data from a database, the operators {@link DatabaseDataReader} or
 * {@link CachedDatabaseExampleSource} can be used.</p>
 * 
 * @author Ingo Mierswa
 */
public class SQLExecution extends Operator {

	/** The parameter name for &quot;SQL query. If not set, the query is read from the file specified by 'query_file'.&quot; */
	public static final String PARAMETER_QUERY = "query";

	/** The parameter name for &quot;File containing the query. Only evaluated if 'query' is not set.&quot; */
	public static final String PARAMETER_QUERY_FILE = "query_file";
	
	private PortPairExtender dummyPorts = new DummyPortPairExtender("through", getInputPorts(), getOutputPorts());

	public SQLExecution(OperatorDescription description) {
		super(description);

		dummyPorts.start();

		getTransformer().addRule(dummyPorts.makePassThroughRule());
	}

	@Override
	public void doWork() throws OperatorException {
		try {
			DatabaseHandler databaseHandler = DatabaseHandler.getConnectedDatabaseHandler(this);
			String query = getQuery();
			databaseHandler.executeStatement(query, false, this, getLogger());
//			Statement statement;
//			if (getParameterAsBoolean(DatabaseHandler.PARAMETER_PREPARE_STATEMENT)) {
//				PreparedStatement prepared = databaseHandler.getConnection().prepareStatement(query);
//				String[] parameters = ParameterTypeEnumeration.transformString2Enumeration(getParameter(DatabaseHandler.PARAMETER_PARAMETERS));
//				for (int i = 0; i < parameters.length; i++) {
//					prepared.setString(i+1, parameters[i]);
//				}
//				prepared.execute();
//				statement = prepared;
//			} else {
//				getLogger().info("Executing query: '" + query + "'");
//				statement = databaseHandler.createStatement(false);
//				statement.execute(query);
//			}
//			
//			getLogger().info("Query executed.");			
//			statement.close();			
			databaseHandler.disconnect();
		} catch (SQLException sqle) {
			throw new UserError(this, sqle, 304, sqle.getMessage());
		}

		dummyPorts.passDataThrough();
	}

	private String getQuery() throws OperatorException {
		String query = getParameterAsString(PARAMETER_QUERY);
		if (query != null)
			query = query.trim();

		String parameterUsed = null;
		boolean warning = false;

		if ((query == null) || (query.length() == 0)) {
			File queryFile = getParameterAsFile(PARAMETER_QUERY_FILE);
			if (queryFile != null) {
				try {
					query = Tools.readTextFile(queryFile);
					parameterUsed = "query_file";
				} catch (IOException ioe) {
					throw new UserError(this, ioe, 302, new Object[] { queryFile, ioe.getMessage() });
				}
				if ((query == null) || (query.trim().length() == 0)) {
					throw new UserError(this, 205, queryFile);
				}
			}
		} else {
			parameterUsed = "query";
			if (isParameterSet(PARAMETER_QUERY_FILE)) {
				warning = true;
			}
		}

		if (query == null) {
			throw new UserError(this, 202, new Object[] { "query", "query_file" });
		}

		if (warning) {
			logWarning("Only one of the parameters 'query' and 'query_file' has to be set. Using value of '" + parameterUsed + "'.");
		}

		return query;
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();
		types.addAll(DatabaseHandler.getConnectionParameterTypes(this));
		
//		ParameterType type = new ParameterTypeCategory(PARAMETER_DATABASE_SYSTEM, "Indicates the used database system", DatabaseService.getDBSystemNames(), 0);
//		type.setExpert(false);
//		types.add(type);
//
//		types.add(new ParameterTypeString(PARAMETER_DATABASE_URL, "The complete URL connection string for the database, e.g. 'jdbc:mysql://foo.bar:portnr/database'", false, false));
//		types.add(new ParameterTypeString(PARAMETER_USERNAME, "Database username.", false, false));
//		type = new ParameterTypePassword(PARAMETER_PASSWORD, "Password for the database.");
//		type.setExpert(false);
//		types.add(type);

		ParameterType type = new ParameterTypeText(PARAMETER_QUERY, "SQL query. If not set, the query is read from the file specified by 'query_file'.", TextType.SQL);
		type.setExpert(false);
		types.add(type);

		types.add(new ParameterTypeFile(PARAMETER_QUERY_FILE, "File containing the query. Only evaluated if 'query' is not set.", null, true));
		types.addAll(DatabaseHandler.getStatementPreparationParamterTypes(this));
		
		return types;
	}	
}
