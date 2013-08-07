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
package com.rapidminer.gui.tools.dialogs.wizards.dataimport;

import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FileInputStream;

import javax.swing.Action;

import com.rapidminer.gui.RapidMinerGUI;
import com.rapidminer.gui.tools.ProgressThread;
import com.rapidminer.gui.tools.ResourceAction;
import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.gui.tools.dialogs.wizards.AbstractWizard;
import com.rapidminer.repository.BlobEntry;
import com.rapidminer.repository.Entry;
import com.rapidminer.repository.Folder;
import com.rapidminer.repository.RepositoryException;
import com.rapidminer.repository.RepositoryLocation;
import com.rapidminer.tools.Tools;

/** Wizard to import general binary files into the repository.
 * 
 * @author Simon Fischer
 *
 */
public class BlobImportWizard extends AbstractWizard {

	public static final Action IMPORT_BLOB_ACTION = new ResourceAction("import_blob") {
		private static final long serialVersionUID = 1;

		@Override
		public void actionPerformed(ActionEvent e) {
			new BlobImportWizard(RapidMinerGUI.getMainFrame()).setVisible(true);
		}
	};
	
	private static final long serialVersionUID = 1L;

	private File file;
	
	public BlobImportWizard(Frame owner) {
		super(RapidMinerGUI.getMainFrame(), "import_blob");
		
		addStep(new FileSelectionWizardStep(this) {
			@Override
			protected boolean performLeavingAction(WizardStepDirection direction) {
				file = getSelectedFile();
				return true;
			}
		});
		addStep(new RepositoryLocationSelectionWizardStep(this, null, true,true) {
			@Override
			protected boolean performLeavingAction(WizardStepDirection direction) {
				if (direction == WizardStepDirection.BACKWARD) {
					return true;
				}
				try {
					RepositoryLocation repositoryLocation = new RepositoryLocation(getRepositoryLocation());
					RepositoryLocation folderLocation = repositoryLocation.parent();
					Entry entry = folderLocation.locateEntry();
					if ((entry != null) && (entry instanceof Folder)) {
						Folder parent = (Folder) entry;
						parent.createBlobEntry(repositoryLocation.getName());
						Entry newEntry = repositoryLocation.locateEntry();
						if (newEntry == null) {
							throw new RepositoryException("Creation of blob entry failed.");
						} else {
							final BlobEntry blob = (BlobEntry) newEntry;
							final String mimeType;
							String name = file.getName().toLowerCase();
							if (name.endsWith(".htm") || name.endsWith(".html")) {
								mimeType = "text/html";
							} else if (name.endsWith(".css")) {
								mimeType = "text/css";
							} else if (name.endsWith(".txt")) {
								mimeType = "text/plain";
							} else if (name.endsWith(".pdf")) {
								mimeType = "application/pdf";
							} else if (name.endsWith(".png")) {
								mimeType = "image/png";
							} else if (name.endsWith(".jpeg")) {
								mimeType = "image/jpeg";
							} else if (name.endsWith(".gif")) {
								mimeType = "image/gif";
							} else {
								mimeType = "application/octet-stream";
							}
							new ProgressThread("import_binary", true) {
								
								@Override
								public void run() {
									try {
										Tools.copyStreamSynchronously(new FileInputStream(file), blob.openOutputStream(mimeType), true);
									} catch (Exception e) {
										SwingTools.showSimpleErrorMessage("import_blob_failed", e, e.getMessage());
									}
								}
							}.start();
						}
					} else {
						throw new RepositoryException("No such folder: '"+folderLocation+"'.");
					}
					return true; 
				} catch (Exception e) {
					SwingTools.showSimpleErrorMessage("import_blob_failed", e, e.getMessage());
					return false;
				}
			}
		});
		layoutDefault();

	}

}
