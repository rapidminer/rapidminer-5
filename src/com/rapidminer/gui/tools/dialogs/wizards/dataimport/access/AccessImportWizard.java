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
package com.rapidminer.gui.tools.dialogs.wizards.dataimport.access;

import java.io.File;
import java.sql.SQLException;

import javax.swing.JComponent;

import com.rapidminer.gui.tools.SimpleFileFilter;
import com.rapidminer.gui.tools.dialogs.SQLQueryBuilder;
import com.rapidminer.gui.tools.dialogs.wizards.WizardStep;
import com.rapidminer.gui.tools.dialogs.wizards.dataimport.DataImportWizard;
import com.rapidminer.gui.tools.dialogs.wizards.dataimport.FileSelectionWizardStep;
import com.rapidminer.gui.tools.dialogs.wizards.dataimport.RepositoryLocationSelectionWizardStep;
import com.rapidminer.operator.OperatorCreationException;
import com.rapidminer.operator.io.DatabaseDataReader;
import com.rapidminer.repository.RepositoryLocation;
import com.rapidminer.tools.OperatorService;
import com.rapidminer.tools.jdbc.DatabaseHandler;
import com.rapidminer.tools.jdbc.connection.AccessConnectionEntry;



/**
 * A wizard to import access database tables into the repository.
 * 
 * @author Tobias Malbrecht
 */
public class AccessImportWizard extends DataImportWizard {
	private static final long serialVersionUID = -4308448171060612833L;
	
	private final AccessConnectionEntry connectionEntry = new AccessConnectionEntry();
	
	private DatabaseDataReader reader = null;
	
	public AccessImportWizard(String i18nKey, Object ... i18nArgs) throws SQLException {
		this(i18nKey, null, null, i18nArgs);
	}
	
	public AccessImportWizard(String i18nKey, File preselectedFile, RepositoryLocation preselectedLocation, Object ... i18nArgs) throws SQLException {
		super(i18nKey, i18nArgs);
		connectionEntry.setFile(preselectedFile);
		try {
			reader = OperatorService.createOperator(com.rapidminer.operator.io.DatabaseDataReader.class);
		} catch (OperatorCreationException e) {
			
		}
		if (preselectedFile == null) {
			addStep(new FileSelectionWizardStep(this, new SimpleFileFilter("Access File (.mdb)", ".mdb")) {
				@Override
				protected boolean performLeavingAction(WizardStepDirection direction) {
					connectionEntry.setFile(getSelectedFile());
					return true;
				}
			});
		}
		addStep(new WizardStep("database_query") {
			private final SQLQueryBuilder dialog = new SQLQueryBuilder(null); //DatabaseHandler.getConnectedDatabaseHandler(connectionEntry));
			{
				dialog.addChangeListener(AccessImportWizard.this);
			}
			
			@Override
			protected boolean canGoBack() {
				return true;
			}

			@Override
			protected boolean canProceed() {
				return dialog.getQuery().length() > 0;
			}

			@Override
			protected JComponent getComponent() {
				return dialog.makeQueryBuilderPanel();
			}
			
			@Override
			protected boolean performEnteringAction(WizardStepDirection direction) {
				dialog.setConnectionEntry(connectionEntry);
				return true;
			}
			
			@Override
			protected boolean performLeavingAction(WizardStepDirection direction) {
				reader.setParameter(DatabaseHandler.PARAMETER_DEFINE_CONNECTION, DatabaseHandler.CONNECTION_MODES[DatabaseHandler.CONNECTION_MODE_URL]);
				reader.setParameter(DatabaseHandler.PARAMETER_DATABASE_SYSTEM, connectionEntry.getProperties().getName());
				reader.setParameter(DatabaseHandler.PARAMETER_DATABASE_URL, connectionEntry.getURL());
				reader.setParameter(DatabaseHandler.PARAMETER_USERNAME, connectionEntry.getUser());
				reader.setParameter(DatabaseHandler.PARAMETER_PASSWORD, new String(connectionEntry.getPassword()));
				reader.setParameter(DatabaseHandler.PARAMETER_DEFINE_QUERY, DatabaseHandler.QUERY_MODES[DatabaseHandler.QUERY_QUERY]);
				reader.setParameter(DatabaseHandler.PARAMETER_QUERY, dialog.getQuery());
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
		addStep(new RepositoryLocationSelectionWizardStep(this, preselectedLocation != null ? preselectedLocation.getAbsoluteLocation() : null, true,true) {
			@Override
			protected boolean performLeavingAction(WizardStepDirection direction) {
				return transferData(reader, getRepositoryLocation());
			}
		});
		layoutDefault();
	}
}
