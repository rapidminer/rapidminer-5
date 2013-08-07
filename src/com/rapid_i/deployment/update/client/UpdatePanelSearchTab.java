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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTextField;

import com.rapid_i.deployment.update.client.listmodels.SearchPackageListModel;
import com.rapidminer.tools.I18N;

/**
 * 
 * @author Dominik Halfkann
 *
 */
public class UpdatePanelSearchTab extends UpdatePanelTab {

	private static final long serialVersionUID = 1L;
	private JTextField searchField;
	private JButton searchButton;
	private SearchPackageListModel searchModel;

	public final Action searchAction = new AbstractAction() {

		private static final long serialVersionUID = 1L;
		private String oldSearch = "";

		@Override
		public void actionPerformed(ActionEvent e) {
			String value = searchField.getText();
			if (value != null && !value.equals(oldSearch)) {
				searchModel.search(value);
				oldSearch = value;
				getPackageList().clearSelection();
			}
		}

	};

	public UpdatePanelSearchTab(UpdatePackagesModel updateModel, PackageDescriptorCache packageDescriptorCache, UpdateServerAccount usAccount) {
		this(updateModel, new SearchPackageListModel(packageDescriptorCache), usAccount);
	}

	private UpdatePanelSearchTab(UpdatePackagesModel updateModel, SearchPackageListModel model, UpdateServerAccount usAccount) {
		super(updateModel, model, usAccount);
		this.searchModel = model;
	}

	@Override
	protected JComponent makeTopPanel() {	
		JPanel panel = new JPanel(new BorderLayout());
		panel.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, Color.LIGHT_GRAY));

		searchField = new JTextField(12);
		searchField.setToolTipText(I18N.getMessage(I18N.getGUIBundle(), "gui.field.update.search.tip"));

		searchField.addKeyListener(new KeyAdapter() {
				public void keyPressed(KeyEvent e) {
					int key = e.getKeyCode();
					if (key == KeyEvent.VK_ENTER) {
						searchAction.actionPerformed(null);
						e.consume();
					}
				}
			}
		);

		searchButton = new JButton(I18N.getMessage(I18N.getGUIBundle(), "gui.dialog.update.tab.search.search_button"));
		searchButton.addActionListener(new ActionListener(){
			@Override
			public void actionPerformed(ActionEvent e) {
				searchAction.actionPerformed(null);
			}
		});
		
		panel.add(searchField, BorderLayout.CENTER);
		panel.add(searchButton, BorderLayout.EAST);

		return panel;
	}
}
