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
package com.rapidminer.gui.tools.dialogs;

import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collection;

import javax.swing.AbstractButton;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 * Dialog to manage Database connections. See {@link DatabaseConnectionDialog}
 * 
 * @author Tobias Malbrecht, Marco Boeck
 */
public class ManageDatabaseConnectionsDialog extends DatabaseConnectionDialog {
	private static final long serialVersionUID = -1314039924713463923L;

	public ManageDatabaseConnectionsDialog() {
		super("manage_db_connections");
		Collection<AbstractButton> buttons = makeButtons();
		JButton okButton = makeOkButton();
		// add own ActionListener so save action can be called before dialog is disposed
		okButton.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				SAVE_CONNECTION_ACTION.actionPerformed(null);
			}
		});
		
		JPanel allButtonPanel = new JPanel(new GridBagLayout());
		GridBagConstraints gbc = new GridBagConstraints();
		JPanel entryButtonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, GAP, GAP));
		for (AbstractButton button : buttons) {
			if (button != null) {
				entryButtonPanel.add(button);
			}
		}
		
		JPanel generalButtonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, GAP, GAP));
		generalButtonPanel.add(okButton);
		generalButtonPanel.add(makeCancelButton());
		
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.fill = GridBagConstraints.NONE;
		gbc.weightx = 0;
		allButtonPanel.add(entryButtonPanel, gbc);
		
		gbc.gridx = 1;
		gbc.gridy = 0;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.weightx = 1;
		allButtonPanel.add(new JLabel(), gbc);
		
		gbc.gridx = 2;
		gbc.gridy = 0;
		gbc.fill = GridBagConstraints.NONE;
		gbc.weightx = 0;
		allButtonPanel.add(generalButtonPanel, gbc);
		
		layoutDefault(makeConnectionManagementPanel(), allButtonPanel, LARGE);
	}
}
