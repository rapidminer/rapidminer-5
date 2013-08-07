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

import java.util.Collections;
import java.util.List;

import com.rapid_i.deployment.update.client.PackageDescriptorCache;
import com.rapidminer.deployment.client.wsimport.UpdateService;
import com.rapidminer.tools.I18N;

/**
 * 
 * @author Dominik Halfkann
 *
 */
public class SearchPackageListModel extends AbstractPackageListModel {

	private static final long serialVersionUID = 1L;

	private String searchString = "";
	private boolean searched = false;
	private boolean shouldUpdate = false;

	public SearchPackageListModel(PackageDescriptorCache cache) {
		super(cache, "gui.dialog.update.tab.no_search_results");
	}

	public void search(String searchString) {
		if (searchString != null) {
			searched = true;
			this.searchString = searchString;
			shouldUpdate = true;
			update();
			shouldUpdate = false;
		}
	}

	@Override
	public List<String> handleFetchPackageNames() {
		UpdateService updateService = cache.getUpdateService();
		
		if (searchString != null && !searchString.equals("") && searchString.length() > 0) {
			List<String> searchResults = updateService.searchFor(searchString);
			return searchResults;
		} else {
			return Collections.emptyList();
		}
	}
	
	@Override
	public Object getElementAt(int index) {
		if (fetching) return I18N.getMessage(I18N.getGUIBundle(), "gui.dialog.update.tab.loading", completed);
		if (packageNames.size() == 0) {
			return searched ? I18N.getMessage(I18N.getGUIBundle(), "gui.dialog.update.tab.no_search_results") : "";
		}
		return cache.getPackageInfo(packageNames.get(index));
	}
	
	@Override
	protected boolean shouldUpdate() {
		return shouldUpdate;
	}
}
