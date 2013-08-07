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

package com.rapidminer.repository.gui;

import java.util.HashSet;

import javax.swing.JTree;
import javax.swing.tree.TreePath;

import com.rapidminer.repository.Entry;
import com.rapidminer.repository.MalformedRepositoryLocationException;
import com.rapidminer.repository.Repository;
import com.rapidminer.repository.RepositoryException;
import com.rapidminer.repository.RepositoryLocation;
import com.rapidminer.tools.LogService;

/**
 *  A utility class to save and restore expansion states and selection paths of the repository tree.
 *   
 * @author Nils Woehler, Venkatesh Umaashankar
 *
 */
public class RepositoryTreeUtil {

	private TreePath selectedPath;
	private HashSet<String> expandedNodes;
	private HashSet<String> expandedRepositories;

	public void saveSelectionPath(TreePath path) {
		selectedPath = path;
	}

	public void restoreSelectionPath(JTree parentTree) {
		if (selectedPath != null) {
			parentTree.setSelectionPath(selectedPath);
			parentTree.scrollPathToVisible(parentTree.getSelectionPath());
		}
	}

	public void saveExpansionState(JTree tree) {

		expandedNodes = new HashSet<String>();
		expandedRepositories = new HashSet<String>();

		saveSelectionPath(tree.getSelectionPath());

		for (int i = 0; i < tree.getRowCount(); i++) {
			TreePath path = tree.getPathForRow(i);
			if (tree.isExpanded(path)) {
				Entry entry = (Entry) path.getLastPathComponent();
				String absoluteLocation = entry.getLocation().getAbsoluteLocation();
				if (entry instanceof Repository) {
					expandedRepositories.add(absoluteLocation);
				} else {
					expandedNodes.add(absoluteLocation);
				}

			}
		}
	}

	public void locateExpandedEntries() {

		for (String absoluteLocation : expandedNodes) {
			try {
				RepositoryLocation repositoryLocation = new RepositoryLocation(absoluteLocation);
				repositoryLocation.locateEntry();
			} catch (MalformedRepositoryLocationException e) {
				LogService.getRoot().warning("Unable to expand the location:" + absoluteLocation);
				e.printStackTrace();
			} catch (RepositoryException e) {
				LogService.getRoot().warning("Unable to expand the location:" + absoluteLocation);
				e.printStackTrace();
			}

		}

	}

	public void restoreExpansionState(JTree tree) {

		for (int i = 0; i < tree.getRowCount(); i++) {
			TreePath path = tree.getPathForRow(i);
			Object entryObject = path.getLastPathComponent();
			if (entryObject instanceof Entry) {
				Entry entry = (Entry) entryObject;
				String absoluteLocation = entry.getLocation().getAbsoluteLocation();
				if (expandedRepositories.contains(absoluteLocation) || expandedNodes.contains(absoluteLocation)) {
					tree.expandPath(path);
				}
			}

		}
		restoreSelectionPath(tree);
	}

}
