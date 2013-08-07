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

import javax.swing.JButton;
import javax.swing.JPanel;


/**
 * This dialog can be used to add a new building block to the process setup. 
 *  
 * @author Ingo Mierswa, Tobias Malbrecht
 */
public class NewBuildingBlockDialog extends BuildingBlockDialog {
		
	private static final long serialVersionUID = 4234757981716378086L;

	
	public NewBuildingBlockDialog() {
		super("new_building_block");

		JPanel panel = new JPanel(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.HORIZONTAL;
		c.insets = new Insets(0, 0, GAP, 0);
		c.weightx = 1;
		c.weighty = 0;
		c.gridwidth = GridBagConstraints.REMAINDER;
		panel.add(filterPanel, c);
		panel.add(typePanel, c);

		c.fill      = GridBagConstraints.BOTH;
		c.insets = new Insets(0, 0, 0, 0);
		c.weightx   = 1;
		c.weighty   = 1;
		panel.add(listPane, c);
		
		JButton okButton = makeOkButton();
		layoutDefault(panel, NORMAL, okButton, makeCancelButton());
		getRootPane().setDefaultButton(okButton);
	}
}
