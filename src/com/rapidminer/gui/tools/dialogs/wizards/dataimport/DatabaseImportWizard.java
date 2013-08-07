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
package com.rapidminer.gui.tools.dialogs.wizards.dataimport;

import java.sql.SQLException;

import javax.swing.JComponent;

import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.gui.tools.dialogs.DatabaseConnectionDialog;
import com.rapidminer.gui.tools.dialogs.SQLQueryBuilder;
import com.rapidminer.gui.tools.dialogs.wizards.WizardStep;
import com.rapidminer.operator.OperatorCreationException;
import com.rapidminer.operator.io.DatabaseDataReader;
import com.rapidminer.tools.OperatorService;
import com.rapidminer.tools.jdbc.DatabaseHandler;
import com.rapidminer.tools.jdbc.connection.DatabaseConnectionService;
import com.rapidminer.tools.jdbc.connection.FieldConnectionEntry;


/**
 * A wizard to import a database table into the repository.
 * 
 * @author Tobias Malbrecht
 */
public class DatabaseImportWizard extends DataImportWizard {
	private static final long serialVersionUID = -4308448171060612833L;

	private DatabaseDataReader reader = null;
	
	private FieldConnectionEntry connectionEntry = null;
	
	public DatabaseImportWizard(String i18nKey, Object ... i18nArgs) throws SQLException {
		super(i18nKey, i18nArgs);
		try {
			reader = OperatorService.createOperator(com.rapidminer.operator.io.DatabaseDataReader.class);
		} catch (OperatorCreationException e) {
			
		}
		final SQLQueryBuilder sqlQueryBuilder = new SQLQueryBuilder(null);
		addStep(new WizardStep("database_connection") {
			private final DatabaseConnectionDialog dialog = new DatabaseConnectionDialog("manage_db_connections");
			{
				dialog.addChangeListener(DatabaseImportWizard.this);
			}
			
			@Override
			protected boolean canGoBack() {
				return false;
			}

			@Override
			protected boolean canProceed() {
				FieldConnectionEntry entry = dialog.getConnectionEntry(false);
				if (entry != null) {
					return true;
				}
				return false;
			}

			@Override
			protected JComponent getComponent() {
				return dialog.makeConnectionManagementPanel();
			}
			
			@Override
			protected boolean performLeavingAction(WizardStepDirection direction) {
				FieldConnectionEntry entry = dialog.getConnectionEntry(false);
				if (entry == null) {
					return false;
				}
				try {
		            if (!DatabaseConnectionService.testConnection(entry)) {
		            	throw new SQLException();
		            }
				} catch (SQLException e) {
					SwingTools.showVerySimpleErrorMessage("db_connection_failed", entry.getDatabase(), entry.getHost(), entry.getPort(), entry.getURL());
					return false;
				}
				reader.setParameter(DatabaseHandler.PARAMETER_DEFINE_CONNECTION, DatabaseHandler.CONNECTION_MODES[DatabaseHandler.CONNECTION_MODE_URL]);
				reader.setParameter(DatabaseHandler.PARAMETER_DATABASE_SYSTEM, entry.getProperties().getName());
				reader.setParameter(DatabaseHandler.PARAMETER_DATABASE_URL, entry.getURL());
				reader.setParameter(DatabaseHandler.PARAMETER_USERNAME, entry.getUser());
				reader.setParameter(DatabaseHandler.PARAMETER_PASSWORD, new String(entry.getPassword()));
				connectionEntry = entry;
				sqlQueryBuilder.setConnectionEntry(connectionEntry);
				return true;
			}
		});
		addStep(new WizardStep("database_query") {			
			{
				sqlQueryBuilder.addChangeListener(DatabaseImportWizard.this);
			}
			
			@Override
			protected boolean canGoBack() {
				return true;
			}

			@Override
			protected boolean canProceed() {
				return sqlQueryBuilder.getQuery().length() > 0;
			}

			@Override
			protected JComponent getComponent() {
				return sqlQueryBuilder.makeQueryBuilderPanel();
			}
			
			@Override
			protected boolean performEnteringAction(WizardStepDirection direction) {
				sqlQueryBuilder.setConnectionEntry(connectionEntry);
				return true;
			}
			
			@Override
			protected boolean performLeavingAction(WizardStepDirection direction) {
				reader.setParameter(DatabaseHandler.PARAMETER_DEFINE_QUERY, DatabaseHandler.QUERY_MODES[DatabaseHandler.QUERY_QUERY]);
				reader.setParameter(DatabaseHandler.PARAMETER_QUERY, sqlQueryBuilder.getQuery());
				// TODO: Check
				//reader.getGeneratedMetaData();
				return true;
			}
		});
//		addStep(new MetaDataDeclerationWirzardStep("select_attributes", (AbstractDataReader)reader){
//			@Override
//			protected boolean canGoBack() {
//				return true;
//			}
//
//			@Override
//			protected boolean canProceed() {
//				return true;
//			}
//
//		});
		addStep(new RepositoryLocationSelectionWizardStep(this, null, true,true) {
			@Override
			protected boolean performLeavingAction(WizardStepDirection direction) {
				if (direction == WizardStepDirection.FINISH) {
					return transferData(reader, getRepositoryLocation());
				} else {
					return super.performLeavingAction(direction);
				}
			}
		});
		layoutDefault();
	}
}
