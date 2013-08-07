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
import java.awt.event.ActionEvent;

import javax.swing.JButton;
import javax.swing.JPanel;

import com.rapidminer.gui.MainFrame;
import com.rapidminer.gui.tools.ResourceAction;


/**
 * The manage building blocks dialog assists the user in managing the saved building blocks.
 * Building Blocks are saved in the local &quot;.rapidminer&quot;
 * directory of the user. In this dialog the user can delete his templates.
 * 
 * @author Ingo Mierswa, Tobias Malbrecht
 */
public class ManageBuildingBlocksDialog extends BuildingBlockDialog {

	private static final long serialVersionUID = -2146505003821251075L;

	public ManageBuildingBlocksDialog(MainFrame mainFrame) {
		super("manage_building_blocks", BuildingBlock.USER_DEFINED);
		
		JPanel panel = new JPanel(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.HORIZONTAL;
		c.insets = new Insets(0, 0, GAP, 0);
		c.weightx = 1;
		c.weighty = 0;
		c.gridwidth = GridBagConstraints.REMAINDER;
		panel.add(filterPanel, c);

		c.fill      = GridBagConstraints.BOTH;
		c.insets = new Insets(0, 0, 0, 0);
		c.weightx   = 1;
		c.weighty   = 1;
		panel.add(listPane, c);
		
		JButton deleteButton = new JButton(new ResourceAction("delete_building_block") {
			private static final long serialVersionUID = -3730654557987864138L;

			public void actionPerformed(ActionEvent e) {
				delete();
			}
		});
		JButton okButton = makeOkButton("management_building_blocks_dialog_apply");
		layoutDefault(panel, NORMAL, deleteButton, okButton);
		getRootPane().setDefaultButton(okButton);
	}
	
	@Override
	protected void actOnDoubleClick() {}
}
