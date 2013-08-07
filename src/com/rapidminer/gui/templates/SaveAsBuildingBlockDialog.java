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
package com.rapidminer.gui.templates;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import com.rapidminer.gui.tools.ResourceLabel;
import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.gui.tools.dialogs.ButtonDialog;
import com.rapidminer.io.process.XMLExporter;
import com.rapidminer.io.process.XMLImporter;
import com.rapidminer.io.process.XMLTools;
import com.rapidminer.operator.Operator;
import com.rapidminer.tools.FileSystemService;
import com.rapidminer.tools.XMLException;


/**
 * The save as building block dialog assists the user in creating a new process
 * building block. Building blocks are saved in the local .rapidminer directory of the
 * user. The name and description can be specified by the user.
 * 
 * @author Ingo Mierswa, Tobias Malbrecht
 */
public class SaveAsBuildingBlockDialog extends ButtonDialog {

    private static final long serialVersionUID = 7662184237558085856L;

    private boolean ok = false;

    private final JTextField nameField = new JTextField();

    private final JTextField descriptionField = new JTextField();

    /** Creates a new save as building block dialog. */
    public SaveAsBuildingBlockDialog(Operator operator) {
        super("save_building_block", true,new Object[]{});

        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;

        JLabel nameLabel = new ResourceLabel("save_building_block.name");
        c.weightx   = 0;
        c.gridwidth = GridBagConstraints.RELATIVE;
        c.insets    = new Insets(0, 0, GAP, GAP);
        panel.add(nameLabel, c);

        nameField.setText(operator.getName());
        c.weightx   = 1;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.insets    = new Insets(0, 0, GAP, 0);
        panel.add(nameField, c);

        JLabel descriptionLabel = new ResourceLabel("save_building_block.description");
        c.insets    = new Insets(0, 0, 0, GAP);
        c.weightx   = 0;
        c.gridwidth = GridBagConstraints.RELATIVE;
        panel.add(descriptionLabel, c);

        descriptionField.setText(operator.getUserDescription());
        c.weightx   = 1;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.insets    = new Insets(0, 0, 0, 0);
        panel.add(descriptionField, c);

        JButton okButton = makeOkButton();
        layoutDefault(panel, okButton, makeCloseButton());
        getRootPane().setDefaultButton(okButton);
    }

    public boolean isOk() {
        return ok;
    }

    public BuildingBlock getBuildingBlock(Operator operator) {
        String name = nameField.getText();
        String xmlString = null;
        try {
            xmlString = XMLTools.toString(new XMLExporter().exportSingleOperator(operator), XMLImporter.PROCESS_FILE_CHARSET);
        } catch (XMLException e) {
            // cannot happen
            throw new RuntimeException("Cannot create process XML: "+e, e);
        } catch (IOException e) {
            // cannot happen
            throw new RuntimeException("Cannot create process XML: "+e, e);
        }
        return new BuildingBlock(name, descriptionField.getText(),
                operator.getOperatorDescription().getIconName(),
                xmlString, BuildingBlock.USER_DEFINED);
    }

    private boolean checkIfNameOk() {
        String name = nameField.getText();
        if ((name == null) || (name.length() == 0)) {
            SwingTools.showVerySimpleErrorMessage("no_template_name");
            return false;
        }

        //		File[] preDefinedBuildingBlockFiles = ParameterService.getConfigFile("buildingblocks").listFiles(new FileFilter() {
        //
        //			public boolean accept(File file) {
        //				return file.getName().endsWith(".buildingblock");
        //			}
        //		});
        File[] userDefinedBuildingBlockFiles = FileSystemService.getUserRapidMinerDir().listFiles(new FileFilter() {
            @Override
            public boolean accept(File file) {
                return file.getName().endsWith(".buildingblock");
            }
        });

        //File[] buildingBlockFiles = new File[preDefinedBuildingBlockFiles.length + userDefinedBuildingBlockFiles.length];
        //System.arraycopy(preDefinedBuildingBlockFiles, 0, buildingBlockFiles, 0, preDefinedBuildingBlockFiles.length);
        //System.arraycopy(userDefinedBuildingBlockFiles, 0, buildingBlockFiles, preDefinedBuildingBlockFiles.length, userDefinedBuildingBlockFiles.length);

        for (int i = 0; i < userDefinedBuildingBlockFiles.length; i++) {
            String tempName = userDefinedBuildingBlockFiles[i].getName().substring(0, userDefinedBuildingBlockFiles[i].getName().lastIndexOf("."));
            if (tempName.equals(name)) {
                SwingTools.showVerySimpleErrorMessage("save_building_block.name_used", name);
                return false;
            }
        }
        return true;
    }

    @Override
    protected void ok() {
        if (checkIfNameOk()) {
            ok = true;
            dispose();
        }
    }

    @Override
    protected void cancel() {
        ok = false;
        dispose();
    }
}
