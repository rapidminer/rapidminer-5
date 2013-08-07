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
package com.rapidminer.operator.nio.xml;

import java.io.File;

import javax.swing.filechooser.FileFilter;

import com.rapidminer.gui.tools.dialogs.wizards.AbstractWizard;
import com.rapidminer.gui.tools.dialogs.wizards.AbstractWizard.WizardStepDirection;
import com.rapidminer.gui.tools.dialogs.wizards.dataimport.FileSelectionWizardStep;

/**
 * This step allows to select an file. With this file the {@link XMLResultSetConfiguration} will be created.
 * 
 * @author Sebastian Land
 * 
 */
public class XMLFileSelectionWizardStep extends FileSelectionWizardStep {

    private XMLResultSetConfiguration configuration;

    /**
     * There must be a configuration given, but might be empty.
     */
    public XMLFileSelectionWizardStep(AbstractWizard parent, XMLResultSetConfiguration configuration) {
        super(parent, new FileFilter() {
            @Override
            public boolean accept(File f) {
                return f.isDirectory() || f.getName().endsWith("xml");
            }
            @Override
            public String getDescription() {
                return "XML Files";
            }
        });
        this.configuration = configuration;
    }

    @Override
    protected boolean performEnteringAction(WizardStepDirection direction) {
        if (configuration.getResourceIdentifier() != null) {
            this.fileChooser.setSelectedFile(new File(configuration.getResourceIdentifier()));
        }
        return true;
    }

    @Override
    protected boolean performLeavingAction(WizardStepDirection direction) {
        configuration.setResourceIdentifier(getSelectedFile().getAbsolutePath());
        return true;
    }
}
