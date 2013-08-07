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
package com.rapidminer.operator.visualization;

import java.util.List;

import com.rapidminer.ObjectVisualizer;
import com.rapidminer.gui.DatabaseExampleVisualization;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.UserError;
import com.rapidminer.operator.ports.DummyPortPairExtender;
import com.rapidminer.operator.ports.PortPairExtender;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeString;
import com.rapidminer.repository.RepositoryLocation;
import com.rapidminer.tools.ObjectVisualizerService;
import com.rapidminer.tools.jdbc.DatabaseHandler;
import com.rapidminer.tools.jdbc.connection.ConnectionEntry;
import com.rapidminer.tools.jdbc.connection.ConnectionProvider;
import com.rapidminer.tools.jdbc.connection.DatabaseConnectionService;


/**
 * Queries the database table for the row with the requested ID
 * and creates a generic example visualizer. This visualizer simply 
 * displays the attribute values of the example. Adding this operator 
 * is might be necessary to enable the visualization of single examples 
 * in the provided plotter or graph components. In contrast to the 
 * usual example visualizer, this version does not load the complete
 * data set into memory but simply queries the information from the
 * database and just shows the single row.
 * 
 * @author Ingo Mierswa, Tobias Malbrecht
 */
public class DatabaseExampleVisualizationOperator extends Operator implements ConnectionProvider {
	
	public static final String PARAMETER_ID_COLUMN = "id_column";

	private PortPairExtender dummyPorts = new DummyPortPairExtender("through", getInputPorts(), getOutputPorts());

	public DatabaseExampleVisualizationOperator(OperatorDescription description) {
		super(description);

		dummyPorts.start();

		getTransformer().addRule(dummyPorts.makePassThroughRule());
	}

	@Override
	public void doWork() throws OperatorException {
		String databaseURL = null;
		String username = null;
		String password = null;
		switch (getParameterAsInt(DatabaseHandler.PARAMETER_DEFINE_CONNECTION)) {
		case DatabaseHandler.CONNECTION_MODE_PREDEFINED:
			String repositoryName = null;
			if (getProcess() != null) {
				RepositoryLocation repositoryLocation = getProcess().getRepositoryLocation();
				if (repositoryLocation != null) {
					repositoryName = repositoryLocation.getRepositoryName();
				}
			}
			ConnectionEntry entry = DatabaseConnectionService.getConnectionEntry(getParameterAsString(DatabaseHandler.PARAMETER_CONNECTION), repositoryName);
			if (entry == null) {
				throw new UserError(this, 318, getParameterAsString(DatabaseHandler.PARAMETER_CONNECTION));
			}
			databaseURL = entry.getURL();
			username = entry.getUser();
			password = new String(entry.getPassword());
			break;
		case DatabaseHandler.CONNECTION_MODE_URL:
		    databaseURL = getParameterAsString(DatabaseHandler.PARAMETER_DATABASE_URL);
			username = getParameterAsString(DatabaseHandler.PARAMETER_USERNAME);
			password = getParameterAsString(DatabaseHandler.PARAMETER_PASSWORD);
			break;
		}
		ObjectVisualizer visualizer = 
			new DatabaseExampleVisualization(
					databaseURL,
					username,
					password,
					getParameterAsInt(DatabaseHandler.PARAMETER_DATABASE_SYSTEM),
					getParameterAsString(DatabaseHandler.PARAMETER_TABLE_NAME),
					getParameterAsString(PARAMETER_ID_COLUMN),
					getLog()
			);
		//TODO: giving this is a veeery sad hack: Normally planned to give an IOObject not present here
		ObjectVisualizerService.addObjectVisualizer(this, visualizer);

		dummyPorts.passDataThrough();
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		// super parameters are added below...
		List<ParameterType> types = super.getParameterTypes();
		types.addAll(DatabaseHandler.getConnectionParameterTypes(this));
		types.addAll(DatabaseHandler.getQueryParameterTypes(this, true));
		types.add(new ParameterTypeString(PARAMETER_ID_COLUMN, "The column of the table holding the object ids for detail data querying.", false));
		return types;
	}

	@Override
	public ConnectionEntry getConnectionEntry() {
		return DatabaseHandler.getConnectionEntry(this);
	}
}
