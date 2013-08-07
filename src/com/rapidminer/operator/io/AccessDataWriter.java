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

import java.io.File;
import java.io.OutputStream;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;

import com.rapidminer.example.ExampleSet;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.UserError;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeCategory;
import com.rapidminer.parameter.ParameterTypeFile;
import com.rapidminer.parameter.ParameterTypePassword;
import com.rapidminer.parameter.ParameterTypeString;
import com.rapidminer.tools.jdbc.DatabaseHandler;

/**
 * Writes an ExampleSet to an existing Access database using
 * the JDBC-ODBC-Bridge.
 * 
 * @author Tobias Malbrecht
 */
public class AccessDataWriter extends AbstractStreamWriter {

	public static final String PARAMETER_DATABASE_FILE = "database_file";

	public static final String PARAMETER_USERNAME = "username";

	public static final String PARAMETER_PASSWORD = "password";

	public static final String PARAMETER_TABLE_NAME = "table_name";

	/** The parameter name for &quot;Indicates if an existing table should be overwritten.&quot; */
	public static final String PARAMETER_OVERWRITE_MODE = "overwrite_mode";

	public AccessDataWriter(OperatorDescription description) {
		super(description);
	}

	@Override
	public ExampleSet write(ExampleSet exampleSet) throws OperatorException {
		File databaseFile = getParameterAsFile(PARAMETER_DATABASE_FILE);
		String databaseURL = "jdbc:odbc:DRIVER={Microsoft Access Driver (*.mdb)};DBQ=" + databaseFile.getAbsolutePath();
		String username = getParameterAsString(PARAMETER_USERNAME);
		if (username == null) {
			username = "noUser";
		}
		String password = getParameterAsString(PARAMETER_PASSWORD);
		if (password == null) {
			password = "noPassword";
		}
		DatabaseHandler handler = null;
		try {
			handler = DatabaseHandler.getConnectedDatabaseHandler(databaseURL, username, password);
			handler.createTable(exampleSet, getParameterAsString(PARAMETER_TABLE_NAME), getParameterAsInt(PARAMETER_OVERWRITE_MODE), getApplyCount() == 0, -1);
			handler.disconnect();
			return exampleSet;
		} catch (SQLException e) {
			throw new UserError(this, e, 304, e.getMessage());
		}
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = new LinkedList<ParameterType>();
		types.add(new ParameterTypeFile(PARAMETER_DATABASE_FILE, "The mdb file containing the Access database which should be written to.", "mdb", false, false));
		types.add(new ParameterTypeString(PARAMETER_USERNAME, "The username for the Access database.", true, false));
		ParameterTypePassword type = new ParameterTypePassword(PARAMETER_PASSWORD, "The password for the database.");
		type.setExpert(false);
		types.add(type);
		types.add(new ParameterTypeString(PARAMETER_TABLE_NAME, "The name of the table within the Access database to which the data set should be written.", false, false));
		types.add(new ParameterTypeCategory(PARAMETER_OVERWRITE_MODE, "Indicates if an existing table should be overwritten or if data should be appended.",
				DatabaseHandler.OVERWRITE_MODES, DatabaseHandler.OVERWRITE_MODE_NONE));
		return types;
	}

	@Override
	String getFileParameterName() {
		return null;
	}

	@Override
	void writeStream(ExampleSet exampleSet, OutputStream outputStream) throws OperatorException {

	}

	@Override
	String[] getFileExtensions() {
		return null;
	}
}
