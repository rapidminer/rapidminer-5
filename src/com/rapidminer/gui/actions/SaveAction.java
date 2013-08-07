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
package com.rapidminer.gui.actions;

import java.awt.event.ActionEvent;
import java.io.IOException;

import com.rapidminer.Process;
import com.rapidminer.gui.RapidMinerGUI;
import com.rapidminer.gui.tools.ResourceAction;
import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.gui.tools.dialogs.DecisionRememberingConfirmDialog;


/**
 * Start the corresponding action.
 * 
 * @author Ingo Mierswa
 */
public class SaveAction extends ResourceAction {

	private static final long serialVersionUID = -2226200404990114956L;
		
	public SaveAction() {
		super("save");
		setEnabled(false);	
		
		setCondition(EDIT_IN_PROGRESS, DONT_CARE);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		save(RapidMinerGUI.getMainFrame().getProcess());
	}
	
	public static void save(Process process) {
		try {			
			if (process.hasSaveDestination()) {
				synchronized (process) {
					if (confirmOverwriteWithNewVersion(process)) {
						process.save();
						RapidMinerGUI.addToRecentFiles(process.getProcessLocation());
					} else {
						return;
					}
				}
			} else {
				SaveAsAction.saveAs(process);
				return;
			}
			
			// check if process has really been saved or user has pressed cancel in saveAs dialog
			if (process.hasSaveDestination()) {
				RapidMinerGUI.useProcessFile(process);
				RapidMinerGUI.getMainFrame().processHasBeenSaved();
			}
		} catch (IOException ex) {
			SwingTools.showSimpleErrorMessage("cannot_save_process", ex, process.getProcessLocation(), ex.getMessage());
		}
	}
	
	private static boolean confirmOverwriteWithNewVersion(Process process) {
		return (!process.isProcessConverted()) || 
			DecisionRememberingConfirmDialog.confirmAction("save_over_with_new_version", "rapidminer.gui.saveover_new_version");
	}
}
