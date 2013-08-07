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

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;

import com.rapid_i.deployment.update.client.listmodels.AbstractPackageListModel;
import com.rapid_i.deployment.update.client.listmodels.UpdatesPackageListModel;
import com.rapidminer.deployment.client.wsimport.PackageDescriptor;
import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.tools.I18N;

/**
 * 
 * @author Dominik Halfkann, Nils Woehler
 *
 */
public class UpdatePanelUpdatesTab extends UpdatePanelTab {

	private static final long serialVersionUID = 1L;
	private JButton updateAllButton;

	public final Action selectAllAction = new AbstractAction() {

		private static final long serialVersionUID = 1L;

		@Override
		public void actionPerformed(ActionEvent e) {

			// check if list model contains restricted packages, that are not marked for installation
			boolean containsRestricted = false;
			for (String packageName : listModel.getAllPackageNames()) {
				PackageDescriptor pd = listModel.getCache().getPackageInfo(packageName);
				if (!updateModel.isSelectedForInstallation(pd)) {
					containsRestricted |= pd.isRestricted();
				}
			}

			if (containsRestricted && !usAccount.isLoggedIn()) {

				// Try to login
				usAccount.login(updateModel, false,

						// If login succeeds, select all purchased packages for installation
						new Runnable() {

							@Override
							public void run() {
								markAllEligible();
							}
						},

						// If login fails, select only free extensions
						new Runnable() {

							@Override
							public void run() {
								markAllEligible();
							}
						});
			} else {
				markAllEligible();
			}
		}

		/**
		 * Marks all extensions to be updated. 
		 * If the user is logged in, all purchased extensions that are available for update, are also selected.
		 * If the user is not logged in, no login dialog will be shown.
		 */
		private void markAllEligible() {
			for (String packageName : listModel.getAllPackageNames()) {
				PackageDescriptor pd = listModel.getCache().getPackageInfo(packageName);
				if (!updateModel.isSelectedForInstallation(pd)) {
					markForInstallation(pd, false, false);
				}
			}
			getModel().updateView();

			checkInstallAllEnabled();
		}

	};

	public UpdatePanelUpdatesTab(UpdatePackagesModel updateModel, PackageDescriptorCache packageDescriptorCache, UpdateServerAccount usAccount) {
		this(updateModel, new UpdatesPackageListModel(packageDescriptorCache), usAccount);
	}

	private UpdatePanelUpdatesTab(UpdatePackagesModel updateModel, AbstractPackageListModel model, UpdateServerAccount usAccount) {
		super(updateModel, model, usAccount);
	}

	private void checkInstallAllEnabled() {
		// check if all packages are selected for installation, if so disable update all button
		boolean allSelected = true;
		for (String packageName : listModel.getAllPackageNames()) {
			PackageDescriptor pd = listModel.getCache().getPackageInfo(packageName);
			allSelected &= updateModel.isSelectedForInstallation(pd);
		}
		updateAllButton.setEnabled(!allSelected);
	}

	@Override
	protected JComponent makeBottomPanel() {
		JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		panel.setMinimumSize(new Dimension(100, 35));
		panel.setPreferredSize(new Dimension(100, 35));
		panel.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, Color.LIGHT_GRAY));

		updateAllButton = new JButton(I18N.getMessage(I18N.getGUIBundle(), "gui.dialog.update.tab.updates.update_all_button"));
		updateAllButton.setIcon(SwingTools.createIcon("16/checks.png"));
		updateAllButton.setEnabled(false);
		listModel.addListDataListener(new ListDataListener() {

			@Override
			public void intervalAdded(ListDataEvent e) {}

			@Override
			public void intervalRemoved(ListDataEvent e) {}

			@Override
			public void contentsChanged(ListDataEvent e) {
				checkInstallAllEnabled();
			}

		});
		updateAllButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				selectAllAction.actionPerformed(null);
			}
		});

		panel.add(updateAllButton);
		return panel;
	}
}
