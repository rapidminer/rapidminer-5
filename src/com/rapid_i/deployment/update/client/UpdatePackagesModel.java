/*
 *  RapidMiner
 *
 *  Copyright (C) 2001-2014 by RapidMiner and the contributors
 *
 *  Complete list of developers available at our web site:
 *
 *       http://rapidminer.com
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

import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Observable;
import java.util.Set;

import com.rapidminer.RapidMiner;
import com.rapidminer.deployment.client.wsimport.PackageDescriptor;
import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.tools.Tools;
import com.rapidminer.tools.plugin.Dependency;

/**
 * @author Simon Fischer, Dominik Halfkann
 */
public class UpdatePackagesModel extends Observable {

	private final Map<PackageDescriptor, Boolean> selectionMap = new HashMap<PackageDescriptor, Boolean>();

	private UpdateServerAccount usAccount;

	/** Read the comment of {@link #isPurchased(PackageDescriptor)}. */
	private Set<String> purchasedPackages = new HashSet<String>();

	private PackageDescriptorCache cache;

	public UpdatePackagesModel(PackageDescriptorCache cache, final UpdateServerAccount usAccount) {
		this.cache = cache;
		this.usAccount = usAccount;
	}

	/** Should only be accessed within a thread. **/
	public void updatePurchasedPackages() {
		if (usAccount.isLoggedIn()) {
			try {
				purchasedPackages = new HashSet<String>(UpdateManager.getAccountService().getLicensedProducts());
				UpdatePackagesModel.this.setChanged();
				UpdatePackagesModel.this.notifyObservers();
			} catch (Exception e1) {
				SwingTools.showSimpleErrorMessage("error_accessing_marketplace_account", e1, e1.getMessage());
				purchasedPackages = new HashSet<String>();
			}
		} else {
			purchasedPackages = new HashSet<String>();
		}
	}

	public void clearPurchasedPackages() {
		if (!usAccount.isLoggedIn()) {
			purchasedPackages = new HashSet<String>();
			for (Map.Entry<PackageDescriptor, Boolean> selectionEntry : selectionMap.entrySet()) {
				if (selectionEntry.getKey().isRestricted() && selectionEntry.getValue()) {
					toggleSelectionForInstallation(selectionEntry.getKey());
				}
			}
		}
	}

	public void setSelectedForInstallation(PackageDescriptor desc, boolean selected) {
		selectionMap.put(desc, true);
	}

	public void forceNotifyObservers() {
		setChanged();
		notifyObservers();
	}

	public void toggleSelectionForInstallation(PackageDescriptor desc) {
		if (desc != null) {
			boolean select = !isSelectedForInstallation(desc);
			if (isUpToDate(desc)) {
				select = false;
			}
			if (UpdateManager.PACKAGE_TYPE_RAPIDMINER_PLUGIN.equals(desc.getPackageTypeName())) {
			} else if (UpdateManager.PACKAGE_TYPE_STAND_ALONE.equals(desc.getPackageTypeName())) {
				String longVersion = RapidMiner.getLongVersion();
				String myVersion = ManagedExtension.normalizeVersion(longVersion);
				String remoteVersion = ManagedExtension.normalizeVersion(desc.getVersion());
				if ((myVersion != null) && (remoteVersion.compareTo(myVersion) <= 0)) {
					select = false;
				}
			}
			if (UpdateManager.COMMERCIAL_LICENSE_NAME.equals(desc.getLicenseName()) && !isPurchased(desc)) {
				select = false;
				SwingTools.showMessageDialog("purchase_package", desc.getName());
			}
			selectionMap.put(desc, select);
			this.setChanged();
			this.notifyObservers(desc);
		}
	}
	
	public void clearFromSelectionMap(List<PackageDescriptor> toClear) {
		for(PackageDescriptor desc : toClear) {
			selectionMap.remove(desc);
			this.setChanged();
			this.notifyObservers(desc);
		}
	}

	public boolean isSelectedForInstallation(PackageDescriptor desc) {
		Boolean selected = selectionMap.get(desc);
		return (selected != null) && selected.booleanValue();
	}

	public List<PackageDescriptor> getInstallationList() {
		List<PackageDescriptor> downloadList = new LinkedList<PackageDescriptor>();
		for (Entry<PackageDescriptor, Boolean> entry : selectionMap.entrySet()) {
			if (entry.getValue()) {
				downloadList.add(entry.getKey());
			}
		}
		return downloadList;
	}

	public boolean isUpToDate(PackageDescriptor desc) {
		ManagedExtension ext = ManagedExtension.get(desc.getPackageId());
		if (ext != null) {
			String remoteVersion = ManagedExtension.normalizeVersion(desc.getVersion());
			String myVersion = ManagedExtension.normalizeVersion(ext.getLatestInstalledVersion());
			if ((myVersion != null) && (remoteVersion.compareTo(myVersion) <= 0)) {
				return true;
			} else {
				return false;
			}
		} else {
			return false;
		}
	}

	/**
	 * Currently, this is an unused feature. There are no extensions that can be purchased. Don't be afraid, RapidMiner
	 * is, and will always be, open source and free. However, future extensions like connectors to SAP or other data
	 * sources requiring proprietary drivers with expensive license fees may only be available on a commercial basis,
	 * for obvious reasons :-)
	 */
	public boolean isPurchased(PackageDescriptor desc) {
		return purchasedPackages.contains(desc.getPackageId());
	}

	public String getExtensionURL(PackageDescriptor descriptor) {
		return UpdateManager.getBaseUrl() + "/faces/product_details.xhtml?productId=" + descriptor.getPackageId();
	}

	public String toString(PackageDescriptor descriptor, String changes) {
		StringBuilder b = new StringBuilder("<html><body>");
		b.append("<h1>");
		b.append(descriptor.getName());
		if (descriptor.isRestricted()) {
			b.append("&nbsp;<img src=\"icon:///").append("16/currency_euro.png").append("\"/>");
		}
		b.append("</h1><hr/>");
		Date date = new Date(descriptor.getCreationTime().toGregorianCalendar().getTimeInMillis());
		String keyStyle=" style=\"padding-right:5px;color:gray;width:80px;\"";
		b.append("<table style=\"margin:5px 0 10px 5px\"><thead><tr><td "+keyStyle+">Version</td><td>").append(descriptor.getVersion());
		b.append("</td></tr><tr><td "+keyStyle+">Release date</td><td>").append(Tools.formatDate(date));
		b.append("</td></tr><tr><td "+keyStyle+">File size</td><td>").append(Tools.formatBytes(descriptor.getSize())).append("</td></tr>");
		b.append("</td></tr><tr><td "+keyStyle+">License</td><td>").append(descriptor.getLicenseName()).append("</td></tr>");
		if ((descriptor.getDependencies() != null) && !descriptor.getDependencies().isEmpty()) {
			b.append("<tr><td "+keyStyle+">Dependencies</td><td>");
			boolean first = true;
			for (Dependency dependency : Dependency.parse(descriptor.getDependencies())) {
				if (!first) {
					b.append(", ");
				} else {
					first = false;
				}
				b.append(cache.getPackageInfo(dependency.getPluginExtensionId()).getName()); 
			}
			b.append("</td></tr>");
		}
		b.append("</table><div>").append(descriptor.getLongDescription()).append("</div>");

		if (changes != null && !changes.trim().equals("")) {
			b.append("<h2>Changes</h2>");
			b.append(changes);
		}
		b.append("</body></html>");
		return b.toString();
	}
}
