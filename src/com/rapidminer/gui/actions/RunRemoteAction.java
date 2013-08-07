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

import com.rapidminer.gui.MainFrame;
import com.rapidminer.gui.RapidMinerGUI;
import com.rapidminer.gui.tools.ResourceAction;
import com.rapidminer.repository.gui.RunRemoteDialog;
/**
 * 
 * @author Simon Fischer
 */
public class RunRemoteAction extends ResourceAction {

	private static final long serialVersionUID = 1L;

	public MainFrame mainFrame;
	
	public RunRemoteAction() {
		super(true, "run_remote");
		setCondition(EDIT_IN_PROGRESS, DISALLOWED);
	}
	
	@Override
	public void actionPerformed(ActionEvent e) {
		RunRemoteDialog.showDialog(RapidMinerGUI.getMainFrame().getProcess(), true);
	}

}
