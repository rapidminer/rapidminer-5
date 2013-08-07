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
import java.util.Collections;
import java.util.List;

import com.rapid_i.deployment.update.client.PackageDescriptorCache;
import com.rapid_i.deployment.update.client.UpdateManager;
import com.rapid_i.deployment.update.client.UpdateServerAccount;
import com.rapidminer.deployment.client.wsimport.AccountService;
import com.rapidminer.gui.tools.SwingTools;


/**
 * 
 * @author Dominik Halfkann
 *
 */
public class LicencedPackageListModel extends AbstractPackageListModel {

	private static final long serialVersionUID = 1L;

	public LicencedPackageListModel(PackageDescriptorCache cache) {
		super(cache, "gui.dialog.update.tab.no_licenses");
	}
	
	@Override
	public List<String> handleFetchPackageNames() {
		UpdateServerAccount account = UpdateManager.getUpdateServerAccount();
		if (!account.isLoggedIn()) return new ArrayList<String>();
		
		AccountService accountService = null;
		try {
			accountService = UpdateManager.getAccountService();
		} catch (Exception e) {
			SwingTools.showSimpleErrorMessage("failed_update_server", e, UpdateManager.getBaseUrl());
		}
		if (accountService != null)
			return accountService.getLicensedProducts();
		return Collections.emptyList();
	}

}
