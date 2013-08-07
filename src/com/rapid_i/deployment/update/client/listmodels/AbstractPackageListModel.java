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
package com.rapid_i.deployment.update.client.listmodels;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.swing.AbstractListModel;
import javax.swing.SwingUtilities;

import com.rapid_i.deployment.update.client.PackageDescriptorCache;
import com.rapid_i.deployment.update.client.UpdateManager;
import com.rapidminer.deployment.client.wsimport.PackageDescriptor;
import com.rapidminer.gui.tools.ProgressThread;
import com.rapidminer.tools.I18N;
import com.rapidminer.tools.ProgressListener;

/**
 * Abstract Class for PackageListModels as used in the RapidMiner Update Dialog.
 * @author Dominik Halfkann
 *
 */
public abstract class AbstractPackageListModel extends AbstractListModel {

	private static final long serialVersionUID = 1L;
	
	protected PackageDescriptorCache cache;
	
	protected boolean updatedOnce = false;
	private boolean forceUpdate = false;

	protected boolean fetching = false;
	protected int completed = 0;

	protected List<String> packageNames = new ArrayList<String>();
	
	private String noPackagesMessageKey = "gui.dialog.update.tab.no_packages";
	
	public AbstractPackageListModel(PackageDescriptorCache cache, String noPackagesMessageKey) {
		this.cache = cache;
		this.noPackagesMessageKey = noPackagesMessageKey;
	}
	
	public void update(boolean forceUpdate) {
			this.forceUpdate = forceUpdate;
			update();
	}
	
	public synchronized void update() {
		if (shouldUpdate() || forceUpdate) {
			fetching = true;
			new ProgressThread("fetching_updates", false) {
				@Override
				public void run() {
					try {
						getProgressListener().setTotal(100);
						setCompleted(getProgressListener(), 5);
						packageNames = fetchPackageNames();
						setCompleted(getProgressListener(), 25);
	
						int a = 0;
						Iterator<String> it = packageNames.iterator();
						int size = packageNames.size();
						while(it.hasNext()) {
							String packageName = it.next();
							PackageDescriptor desc = cache.getPackageInfo(packageName);
							cache.getPackageChanges(packageName);
							a++;
							setCompleted(getProgressListener(), 30 + 70 * a / size);
							if (desc == null) it.remove();
						}
						modifyPackageList();
						updatedOnce = true;
						SwingUtilities.invokeLater(new Runnable() {
							@Override
							public void run() {
								fetching = false;
								fireContentsChanged(this, 0, packageNames.size() > 0 ? packageNames.size() : 1);						
							}					
						});	
					} finally {
						fetching = false;
						getProgressListener().complete();
					}
				}
			}.start();
			forceUpdate = false;
		}
	}
	
	protected boolean shouldUpdate() {
		return !updatedOnce;
	}
	
	private void setCompleted(ProgressListener listener, int progress) {
		listener.setCompleted(progress);
		this.completed = progress;
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				fireContentsChanged(this, 0, packageNames.size() > 0 ? packageNames.size() : 1);
			}
		});
	}

	public abstract List<String> handleFetchPackageNames();
	
	public List<String> fetchPackageNames() {
		List<String> packageNames = handleFetchPackageNames();
		List<String> result = new ArrayList<String>(packageNames.size());
		boolean containsRM = false;
		for (String pid : packageNames) {
			if (UpdateManager.PACKAGEID_RAPIDMINER.equals(pid) || UpdateManager.getRMPackageId().equals(pid)) {
				if (containsRM) {
					// noop
				} else {
					result.add(UpdateManager.getRMPackageId());
					containsRM = true;
				}
			} else {
				result.add(pid);
			}
		}
		return result;
	}
	
	public void modifyPackageList() {
		return;
	}

	@Override
	public int getSize() {
		if (fetching) {
			return 1;
		} else {
			return packageNames.size() > 0 ? packageNames.size() : 1;	
		}		
	}

	@Override
	public Object getElementAt(int index) {
		if (fetching) return I18N.getMessage(I18N.getGUIBundle(), "gui.dialog.update.tab.loading", completed);
		if (packageNames.size() == 0) {			
			return I18N.getMessage(I18N.getGUIBundle(), noPackagesMessageKey);
		} 
		return cache.getPackageInfo(packageNames.get(index));
	}
	
	public List<String> getAllPackageNames() {
		return packageNames;
	}
	
	public PackageDescriptorCache getCache() {
		return cache;
	}
	
	public String getChanges(String packageId) {
		return cache.getPackageChanges(packageId);
	}

	public void updateView(PackageDescriptor descr) {
		if (descr != null) {
			int index = packageNames.indexOf(descr.getPackageId());
			fireContentsChanged(this, index, index);
		}
	}
	
	public void updateView() {
		fireContentsChanged(this, 0, packageNames.size() > 0 ? packageNames.size() : 1);
	}
	
	public void add(PackageDescriptor desc) {
		packageNames.add(desc.getPackageId());
		fireIntervalAdded(this, packageNames.size()-1, packageNames.size()-1);
	}
	
	
}
