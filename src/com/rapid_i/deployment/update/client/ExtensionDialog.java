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
package com.rapid_i.deployment.update.client;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collection;

import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import com.rapidminer.gui.RapidMinerGUI;
import com.rapidminer.gui.tools.ResourceAction;
import com.rapidminer.gui.tools.ResourceLabel;
import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.gui.tools.dialogs.ButtonDialog;
import com.rapidminer.gui.tools.dialogs.ConfirmDialog;

/** Dialog to select versions and (de-)activate extensions
 * 
 * @author Simon Fischer
 *
 */
public class ExtensionDialog extends ButtonDialog {

	public static final Action MANAGE_EXTENSIONS = new ResourceAction("manage_extensions") {
		{
			setCondition(EDIT_IN_PROGRESS, DONT_CARE);
		}
		private static final long serialVersionUID = 1L;
		@Override
		public void actionPerformed(ActionEvent e) {
			new ExtensionDialog().setVisible(true);
		}
	};

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private boolean changed = false;
	
	public ExtensionDialog() {
		super("manage_extensions");

		
		Collection<ManagedExtension> allExtensions = ManagedExtension.getAll();
		if (allExtensions.isEmpty()) {
			JLabel label = new ResourceLabel("no_extensions_installed");
			label.setPreferredSize(new Dimension(300, 100));
			layoutDefault(label, makeCloseButton());
		} else {
			final JComponent main = new JPanel(new GridBagLayout());
			GridBagConstraints c = new GridBagConstraints();
			c.anchor = GridBagConstraints.FIRST_LINE_START;
			c.fill = GridBagConstraints.BOTH;
			c.weightx = 1;
			c.weighty = 0;

			for (final ManagedExtension ext : allExtensions) {
				c.gridwidth = 1;
				c.weightx = 0.7;
				final JCheckBox activate = new JCheckBox(ext.getName());
				main.add(activate, c);
				c.weightx = 0.3;
				c.gridwidth = GridBagConstraints.RELATIVE;							
				final JComboBox versionCombo = new JComboBox(ext.getInstalledVersions().toArray());
				main.add(versionCombo, c);
				
				final JButton deleteButton = new JButton();
				ResourceAction uninstallAction = new ResourceAction(true, "uninstall_extension") {
					private static final long serialVersionUID = 1L;
					@Override
					public void actionPerformed(ActionEvent e) {
						if (SwingTools.showConfirmDialog("really_uninstall_extension", ConfirmDialog.YES_NO_OPTION, ext.getName()+" v."+ext.getSelectedVersion()) == ConfirmDialog.YES_OPTION) {
							String selectedVersion = ext.getSelectedVersion();
							if (ext.uninstallActiveVersion()) {
								activate.setSelected(false);
								versionCombo.removeItem(selectedVersion);
								versionCombo.setSelectedIndex(-1);
								// Did that remove last installed version?
								if (ManagedExtension.get(ext.getPackageId()) == null) {
									main.remove(activate);
									main.remove(versionCombo);
									main.remove(deleteButton);
								}
							} else {
								SwingTools.showVerySimpleErrorMessage("error_uninstalling_extension");
							}
							changed = true;
						}						
					}					
				};
				deleteButton.setAction(uninstallAction);
				
				c.gridwidth = GridBagConstraints.REMAINDER;
				c.weightx = 0;
				main.add(deleteButton, c);

				activate.addActionListener(new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent e) {
						changed = true;
						versionCombo.setEnabled(activate.isSelected());
						ext.setActive(activate.isSelected());
					}
				});

				versionCombo.addActionListener(new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent e) {
						changed = true;
						ext.setSelectedVersion((String)versionCombo.getSelectedItem());
					}
				});
				activate.setSelected(ext.isActive());
				versionCombo.setEnabled(ext.isActive());
				versionCombo.setSelectedItem(ext.getSelectedVersion());
			}
			c.gridwidth = GridBagConstraints.REMAINDER;
			c.weighty = 1;
			main.add(new JPanel(), c);
			
			JScrollPane mainScrollPane = new JScrollPane(main);
			if (mainScrollPane.getPreferredSize().getHeight() < 50) {
				mainScrollPane.setPreferredSize(new Dimension((int)mainScrollPane.getPreferredSize().getWidth(), 50));
			}
			layoutDefault(mainScrollPane, makeCloseButton());
		}		
		changed = false;
	}

	@Override
	protected void close() {
		if (changed) {
			ManagedExtension.saveConfiguration();
			if (SwingTools.showConfirmDialog("manage_extensions.restart", ConfirmDialog.YES_NO_OPTION) == ConfirmDialog.YES_OPTION) {
				RapidMinerGUI.getMainFrame().exit(true);
			}
		}		
		super.close();
	}

}
