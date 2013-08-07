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

import javax.swing.AbstractAction;
import javax.swing.ImageIcon;

import com.rapid_i.repository.wsimport.ExecutionResponse;
import com.rapid_i.repository.wsimport.ProcessContextWrapper;
import com.rapidminer.RepositoryProcessLocation;
import com.rapidminer.gui.MainFrame;
import com.rapidminer.gui.RapidMinerGUI;
import com.rapidminer.gui.tools.ProgressThread;
import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.gui.tools.dialogs.ConfirmDialog;
import com.rapidminer.gui.tools.dialogs.DecisionRememberingConfirmDialog;
import com.rapidminer.repository.Repository;
import com.rapidminer.repository.RepositoryConstants;
import com.rapidminer.repository.RepositoryException;
import com.rapidminer.repository.RepositoryLocation;
import com.rapidminer.repository.remote.RemoteProcessEntry;
import com.rapidminer.repository.remote.RemoteRepository;
import com.rapidminer.tools.I18N;


/**
 * Starts the corresponding action.
 * 
 * @author Marco Boeck
 */
public class RunRemoteNowAction extends AbstractAction {

	private static final long serialVersionUID = 1;
	
    private final MainFrame mainFrame;
    
    public RunRemoteNowAction(MainFrame mainFrame) {
        super(I18N.getMessage(I18N.getGUIBundle(), "gui.action.run_remote_now.label"));
        this.mainFrame = mainFrame;
        
        String tip = I18N.getMessageOrNull(I18N.getGUIBundle(), "gui.action.run_remote_now.tip");
		if (tip != null) {
			putValue(SHORT_DESCRIPTION, tip);
		}
		String iconName = I18N.getMessageOrNull(I18N.getGUIBundle(), "gui.action.run_remote_now.icon");
		if (iconName != null) {
			ImageIcon small = SwingTools.createIcon("16/" + iconName);
			ImageIcon large = SwingTools.createIcon("24/" + iconName);
			putValue(LARGE_ICON_KEY, large);
			putValue(SMALL_ICON, small != null ? small : large);
		}
    }

    public void actionPerformed(ActionEvent e) {
    	executeProcessOnRA(mainFrame.getProcess().getRepositoryLocation());
    }
    
    /**
     * Executes the process.
     * @param mainFrame
     * @param location
     */
    public static synchronized void executeProcessOnRA(final RepositoryLocation repoLoc) {
    	if (repoLoc == null) {
    		return;
    	}
    	// check if user really wants to execute process on RA
    	if (!DecisionRememberingConfirmDialog.confirmAction("execute_process_remotely_now", RapidMinerGUI.PROPERTY_RUN_REMOTE_NOW)) {
    		return;
		}
    	
    	// check if process selected is the same as the current process in the GUI, if so check if it has been edited and ask for save
    	// before continuing. Otherwise the last version would be executed which can result in confusion (and therefore support tickets..)
    	if (RapidMinerGUI.getMainFrame().getProcess().getProcessLocation() != null) {
    		String mainFrameProcessLocString = ((RepositoryProcessLocation) RapidMinerGUI.getMainFrame().getProcess().getProcessLocation()).getRepositoryLocation().getPath();
    		if (repoLoc.getPath().equals(mainFrameProcessLocString) && RapidMinerGUI.getMainFrame().isChanged()) {
    			if (SwingTools.showConfirmDialog("save_before_remote_run", ConfirmDialog.OK_CANCEL_OPTION) == ConfirmDialog.CANCEL_OPTION) {
    				// user does not want to save "dirty" process, abort
    				return;
    			}
    			SaveAction.save(RapidMinerGUI.getMainFrame().getProcess());
    		}
    	}

    	new ProgressThread("run_remote_now") {
			
			@Override
			public void run() {
				try {
		    		Repository repo = repoLoc.getRepository();
		    		// check preconditions, e.g. process has a valid processLocation and the repository is a RemoteRepository
		    		if (repoLoc.locateEntry() instanceof RemoteProcessEntry && repo instanceof RemoteRepository) {
		    			try {
		    				String processLocString = repoLoc.getPath();
		    				ProcessContextWrapper pcWrapper = new ProcessContextWrapper();
		    				ExecutionResponse response = ((RemoteRepository)repoLoc.getRepository()).getProcessService().executeProcessSimple(processLocString, null, pcWrapper);
		    				// in case of error, show it
		    				if (response.getStatus() != RepositoryConstants.OK) {
		    					SwingTools.showSimpleErrorMessage("run_proc_remote", response.getErrorMessage());
		    				}
		    			} catch (Exception e1) {
		    				SwingTools.showSimpleErrorMessage("error_connecting_to_server", e1);
		    				return;
		    			}
		    		} else {
		    			SwingTools.showVerySimpleErrorMessage("run_remote_now_general_error");
		    		}
		    	} catch (RepositoryException e1) {
		    		SwingTools.showVerySimpleErrorMessage("run_remote_now_repo_error");
		    	}
			}
		}.start();
    	
    }
    
    @Override
    public void setEnabled(boolean enabled) {
    	super.setEnabled(enabled);
    	
    	// change tooltip so if the action is disabled the user can see what needs to be done to enable the action
    	String tip = null;
    	if (enabled) {
    		tip = I18N.getMessageOrNull(I18N.getGUIBundle(), "gui.action.run_remote_now.tip");
    	} else {
    		tip = I18N.getMessageOrNull(I18N.getGUIBundle(), "gui.action.run_remote_now.disabled.tip");
    	}
    	if (tip != null) {
    		putValue(SHORT_DESCRIPTION, tip);
    	}
    }
    
}
