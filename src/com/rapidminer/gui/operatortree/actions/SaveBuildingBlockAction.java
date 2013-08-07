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
package com.rapidminer.gui.operatortree.actions;

import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;

import com.rapidminer.gui.actions.Actions;
import com.rapidminer.gui.templates.BuildingBlock;
import com.rapidminer.gui.templates.SaveAsBuildingBlockDialog;
import com.rapidminer.gui.tools.ResourceAction;
import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.operator.Operator;
import com.rapidminer.tools.FileSystemService;


/**
 * Start the corresponding action.
 * 
 * @author Ingo Mierswa
 */
public class SaveBuildingBlockAction extends ResourceAction {

    private static final long serialVersionUID = 2238740826770976483L;

    private Actions actions;

    public SaveBuildingBlockAction(Actions actions) {
        super(true, "save_building_block");
        setCondition(OPERATOR_SELECTED, MANDATORY);
        setCondition(ROOT_SELECTED, DISALLOWED);
        this.actions = actions;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        Operator selectedOperator = this.actions.getSelectedOperator();
        if (selectedOperator != null) {
            SaveAsBuildingBlockDialog dialog = new SaveAsBuildingBlockDialog(selectedOperator);
            dialog.setVisible(true);
            if (dialog.isOk()) {
                BuildingBlock buildingBlock = dialog.getBuildingBlock(selectedOperator);
                String name = buildingBlock.getName();
                try {
                    File buildingBlockFile = FileSystemService.getUserConfigFile(name + ".buildingblock");
                    buildingBlock.save(buildingBlockFile);
                } catch (IOException ioe) {
                    SwingTools.showSimpleErrorMessage("cannot_write_building_block_file", ioe);
                }
            }
        }
    }
}
