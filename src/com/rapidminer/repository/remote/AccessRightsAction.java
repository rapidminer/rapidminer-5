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
package com.rapidminer.repository.remote;

import java.awt.event.ActionEvent;
import java.util.List;

import javax.swing.SwingUtilities;

import com.rapid_i.repository.wsimport.AccessRights;
import com.rapidminer.gui.tools.ProgressThread;
import com.rapidminer.gui.tools.ResourceAction;
import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.repository.RepositoryException;

/**
 * 
 * @author Simon Fischer
 *
 */
public class AccessRightsAction extends ResourceAction {

	private static final long serialVersionUID = 1L;

	private RemoteEntry entry;

	public AccessRightsAction(RemoteEntry entry) {
		super("repository.edit_access_rights");
		this.entry = entry;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		// this is for example the case when the credentials are invalid
		try {
			if (entry.getRepository().getRepositoryService() == null) {
				SwingTools.showVerySimpleErrorMessage("error_access_rights");
				return;
			}
		} catch (RepositoryException e1) {
			SwingTools.showSimpleErrorMessage("error_contacting_repository", e1, e1.getMessage());
			return;
		}
		
		new ProgressThread("download_from_repository") {
			@Override
			public void run() {
				
				try {
					final List<String> groupNames = entry.getRepository().getRepositoryService().getAllGroupNames();
					final List<AccessRights> accessRights = entry.getAccessRights();
					SwingUtilities.invokeLater(new Runnable() {
						public void run() {
							new AccessRightsDialog(entry, accessRights, groupNames).setVisible(true);			
						}
					});					
				} catch (RepositoryException e) {
					SwingTools.showSimpleErrorMessage("error_contacting_repository", e, e.getMessage());
				}					
			}
		}.start();		
	}
}
