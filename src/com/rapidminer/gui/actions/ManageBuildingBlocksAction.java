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
package com.rapidminer.gui.actions;

import java.awt.event.ActionEvent;

import com.rapidminer.gui.MainFrame;
import com.rapidminer.gui.templates.ManageBuildingBlocksDialog;
import com.rapidminer.gui.tools.ResourceAction;


/**
 * Start the corresponding action.
 * 
 * @author Ingo Mierswa
 */
public class ManageBuildingBlocksAction extends ResourceAction {

	private static final long serialVersionUID = 3185492398152116698L;

	public MainFrame mainFrame;
	
	public ManageBuildingBlocksAction(MainFrame mainFrame) {
		super("manage_building_blocks");
		this.mainFrame = mainFrame;
	}

	public void actionPerformed(ActionEvent e) {
		ManageBuildingBlocksDialog dialog = new ManageBuildingBlocksDialog(this.mainFrame);
		dialog.setVisible(true);
	}
}
