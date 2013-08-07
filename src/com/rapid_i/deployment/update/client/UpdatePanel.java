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
import java.awt.Dimension;
import java.util.List;
import java.util.Observable;
import java.util.Observer;

import javax.swing.JPanel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import com.rapid_i.deployment.update.client.listmodels.AbstractPackageListModel;
import com.rapid_i.deployment.update.client.listmodels.BookmarksPackageListModel;
import com.rapid_i.deployment.update.client.listmodels.LicencedPackageListModel;
import com.rapid_i.deployment.update.client.listmodels.TopDownloadsPackageListModel;
import com.rapid_i.deployment.update.client.listmodels.TopRatedPackageListModel;
import com.rapidminer.deployment.client.wsimport.PackageDescriptor;
import com.rapidminer.gui.tools.ResourceTabbedPane;
import com.rapidminer.tools.NetTools;

/**
 * 
 * @author Simon Fischer
 * 
 */
public class UpdatePanel extends JPanel {

	private final PackageDescriptorCache packageDescriptorCache;

	private final UpdateDialog updateDialog;

	private static final long serialVersionUID = 1L;

	static {
		NetTools.init();
	}

	private ResourceTabbedPane updatesTabbedPane = new ResourceTabbedPane("update");

	private UpdateServerAccount usAccount = null;
	
	private UpdatePackagesModel updateModel;

	public UpdatePanel(UpdateDialog dialog,
	                   PackageDescriptorCache packageDescriptorCache,
	                   String[] preselectedExtensions, 
	                   final UpdateServerAccount usAccount, 
	                   UpdatePackagesModel updateModel) {

		this.packageDescriptorCache = packageDescriptorCache;
		this.usAccount = usAccount;		
		this.updateModel = updateModel;

		for (String pE : preselectedExtensions) {
			PackageDescriptor desc = packageDescriptorCache.getPackageInfo(pE);
			updateModel.setSelectedForInstallation(desc, true);			
		}		
		this.updateDialog = dialog;

		setLayout(new BorderLayout());
		setPreferredSize(new Dimension(800, 320));
		setMinimumSize(new Dimension(800, 320));

		updatesTabbedPane.addTabI18N("search", createSearchListPanel());
		updatesTabbedPane.addTabI18N("updates", createUpdatesListPanel());
		updatesTabbedPane.addTabI18N("top_downloads", createUpdateListPanel(new TopDownloadsPackageListModel(packageDescriptorCache)));
		updatesTabbedPane.addTabI18N("top_rated", createUpdateListPanel(new TopRatedPackageListModel(packageDescriptorCache)));
		updatesTabbedPane.addTabI18N("purchased", createUpdateListPanel(new LicencedPackageListModel(packageDescriptorCache), true));
		updatesTabbedPane.addTabI18N("bookmarks", createUpdateListPanel(new BookmarksPackageListModel(packageDescriptorCache), true));

		updatesTabbedPane.addChangeListener(new ChangeListener() {


			@Override
			public void stateChanged(ChangeEvent e) {
				UpdatePanelTab currentTab = (UpdatePanelTab) updatesTabbedPane.getSelectedComponent();
				currentTab.selectNotify();
			}
		});

		add(updatesTabbedPane, BorderLayout.CENTER);

		updateModel.forceNotifyObservers();
		usAccount.forceNotifyObservers();
	}
	
	private class ModelUpdateOberver implements Observer {
		
		private AbstractPackageListModel model;
		
		ModelUpdateOberver(AbstractPackageListModel model) {
			this.model = model;
		}
		
		@Override
		public void update(Observable obs, Object arg) {
			if (obs instanceof UpdateServerAccount) {
				model.update(true);
			}
		}
	}
	
	private JPanel createUpdateListPanel(AbstractPackageListModel model, boolean updateOnAccountAction) {
		if (updateOnAccountAction) {
			usAccount.addObserver(new ModelUpdateOberver(model));
		}
		return createUpdateListPanel(model);
	}

	private JPanel createUpdateListPanel(AbstractPackageListModel listModel) {
		return new UpdatePanelTab(updateModel, listModel, usAccount);
	}

	private JPanel createSearchListPanel() {
		return new UpdatePanelSearchTab(updateModel, packageDescriptorCache, usAccount);
	}

	private JPanel createUpdatesListPanel() {
		return new UpdatePanelUpdatesTab(updateModel, packageDescriptorCache, usAccount);
	}

	public void startUpdate() {
		final List<PackageDescriptor> downloadList = updateModel.getInstallationList();
		updateDialog.startUpdate(downloadList);
	}
	
	public void selectUpdatesTab() {
		updatesTabbedPane.setSelectedIndex(1);
	}

}
