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
package com.rapidminer.gui.dialog;

import java.awt.BorderLayout;
import java.util.Collection;
import java.util.logging.Level;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

import com.rapidminer.Process;
import com.rapidminer.gui.RapidMinerGUI;
import com.rapidminer.gui.properties.WizardPropertyTable;
import com.rapidminer.gui.templates.OperatorParameterPair;
import com.rapidminer.gui.templates.Template;
import com.rapidminer.gui.templates.TemplatesDialog;
import com.rapidminer.gui.tools.ExtendedJScrollPane;
import com.rapidminer.gui.tools.dialogs.wizards.AbstractWizard;
import com.rapidminer.gui.tools.dialogs.wizards.WizardStep;
import com.rapidminer.tools.I18N;
import com.rapidminer.tools.LogService;


/**
 * The wizard dialog assists the user in creating a new process setup. Template
 * processes are loaded from the etc/templates directory or from the user
 * directory &quot;.rapidminer&quot;. Instances of all processes are created and the
 * parameters can be set.
 * 
 * @author Ingo Mierswa, Simon Fischer, Tobias Malbrecht
 */
public class TemplateWizardDialog extends AbstractWizard {

    private static final long serialVersionUID = 1L;

    private Process process = null;
    private Template template;
    private Collection<OperatorParameterPair> parameters = null; 
	
	public TemplateWizardDialog() {
		super(RapidMinerGUI.getMainFrame(), "open_template", true, new Object[]{});
		addStep(new WizardStep("open_template.choose_template") {

			private TemplatesDialog dialog = new TemplatesDialog(Template.ALL);
			{
				dialog.addChangeListener(TemplateWizardDialog.this);
			}

			private JPanel panel = dialog.createTemplateManagementPanel();

			@Override
			protected boolean canGoBack() {
				return false;
			}

			@Override
			protected boolean canProceed() {
				if (dialog.getSelectedTemplate() != null) {
					return true;
				}
				return false;
			}
			
			protected boolean performLeavingAction(WizardStepDirection direction) {
				try {
					template = dialog.getSelectedTemplate();
					process = template.getProcess();
					parameters = template.getParameters();
				} catch (Exception e) {
					//LogService.getRoot().log(Level.WARNING, "Error loading process template: "+e, e);
					LogService.getRoot().log(Level.WARNING,
							I18N.getMessage(LogService.getRoot().getResourceBundle(), 
							"com.rapidminer.gui.dialog.TemplateWizardDialog.loading_process_template_error", 
							e),
							e);
					return false;
				}
				return true;
			}

			@Override
			protected JComponent getComponent() {
				return panel;
			}
			
		});
		addStep(new WizardStep("open_template.parameters") {
			private WizardPropertyTable propertyTable = new WizardPropertyTable();
			private JLabel headerLabel = new JLabel();
			@Override
			protected boolean canGoBack() {
				return true;
			}

			@Override
			protected boolean canProceed() {
				return true;
			}
			
			protected boolean performEnteringAction(WizardStepDirection direction) {
				if (process == null || parameters == null) {
					return false;
				}
				if(!propertyTable.setProcess(process, parameters)) {
					return false;
				}
				headerLabel.setText(template.getHTMLDescription());
				return true;
			}
			
			protected boolean performLeavingAction(WizardStepDirection direction) {
				RapidMinerGUI.getMainFrame().setProcess(process, true);
				return true;
			}

			@Override
			protected JComponent getComponent() {
				JPanel panel = new JPanel(new BorderLayout());
				ExtendedJScrollPane tablePane = new ExtendedJScrollPane(propertyTable);
				tablePane.setBorder(createBorder());
				
				panel.add(tablePane, BorderLayout.CENTER);
				panel.add(headerLabel, BorderLayout.NORTH);
				
				return panel;
			}
			
		});
		layoutDefault();
	}
}
