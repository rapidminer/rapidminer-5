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
package com.rapidminer.repository.gui.actions;

import com.rapidminer.gui.actions.RunRemoteNowAction;
import com.rapidminer.repository.gui.RepositoryTree;
import com.rapidminer.repository.remote.RemoteProcessEntry;

/**
 * Starts the corresponding action.
 * 
 * @author Marco Boeck
 */
public class RunRemoteNowProcessAction extends AbstractRepositoryAction<RemoteProcessEntry> {
	
	private static final long serialVersionUID = 1L;

	/**
	 * Creates the corresponding action.
	 * @param tree the {@link RepositoryTree} instance on which to register
	 */
	public RunRemoteNowProcessAction(RepositoryTree tree) {
		super(tree, RemoteProcessEntry.class, false, "run_remote_now");
	}

	@Override
	public void actionPerformed(RemoteProcessEntry process) {
		if (process != null) {
			RunRemoteNowAction.executeProcessOnRA(process.getLocation());
		}
	}

}
