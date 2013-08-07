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
package com.rapidminer.gui;

import com.rapidminer.gui.flow.ErrorTable;
import com.rapidminer.gui.flow.ProcessPanel;
import com.rapidminer.gui.processeditor.CommentEditor;
import com.rapidminer.gui.processeditor.NewOperatorEditor;
import com.rapidminer.gui.processeditor.ProcessContextProcessEditor;
import com.rapidminer.gui.processeditor.XMLEditor;
import com.rapidminer.gui.processeditor.results.ResultDisplay;
import com.rapidminer.gui.properties.OperatorPropertyPanel;
import com.rapidminer.gui.tools.LoggingViewer;
import com.rapidminer.gui.tools.SystemMonitor;
import com.rapidminer.gui.tools.WelcomeScreen;
import com.rapidminer.repository.gui.RepositoryBrowser;
import com.vlsolutions.swing.docking.DockingConstants;
import com.vlsolutions.swing.docking.DockingContext;
import com.vlsolutions.swing.docking.ws.WSDesktop;
import com.vlsolutions.swing.docking.ws.WSDockKey;


/** Collection of {@link Perspective}s that can be applied, saved, created.
 * 
 * @author Simon Fischer
 *
 */
public class Perspectives extends ApplicationPerspectives {

	public Perspectives(DockingContext context) {
		super(context);
	}

	protected void makePredefined() {
		addPerspective("design", false);
		restoreDefault("design");
		addPerspective("result", false);
		restoreDefault("result");
		addPerspective("welcome", false);
		restoreDefault("welcome");
	}

	protected void restoreDefault(String perspectiveName) {
		WSDockKey processPanelKey = new WSDockKey(ProcessPanel.PROCESS_PANEL_DOCK_KEY);
		WSDockKey propertyTableKey = new WSDockKey(OperatorPropertyPanel.PROPERTY_EDITOR_DOCK_KEY);
		WSDockKey messageViewerKey = new WSDockKey(LoggingViewer.LOG_VIEWER_DOCK_KEY);
		WSDockKey resultsKey = new WSDockKey(ResultDisplay.RESULT_DOCK_KEY);
		WSDockKey repositoryKey = new WSDockKey(RepositoryBrowser.REPOSITORY_BROWSER_DOCK_KEY);
		WSDockKey newOperatorEditorKey = new WSDockKey(NewOperatorEditor.NEW_OPERATOR_DOCK_KEY);
		WSDockKey errorTableKey = new WSDockKey(ErrorTable.ERROR_TABLE_DOCK_KEY);
		WSDockKey xmlEditorKey = new WSDockKey(XMLEditor.XML_EDITOR_DOCK_KEY);
		WSDockKey commentEditorKey = new WSDockKey(CommentEditor.COMMENT_EDITOR_DOCK_KEY);
		WSDockKey operatorHelpKey = new WSDockKey(OperatorDocViewer.OPERATOR_HELP_DOCK_KEY);
		WSDockKey processContextEditorKey = new WSDockKey(ProcessContextProcessEditor.PROCESS_CONTEXT_DOCKKEY);
		WSDockKey welcomeKey = new WSDockKey(WelcomeScreen.WELCOME_SCREEN_DOCK_KEY);
		//WSDockKey overviewKey = new WSDockKey(OverviewPanel.OVERVIEW_DOCK_KEY);

		if ("design".equals(perspectiveName)) {
			Perspective designPerspective = getPerspective("design");
			WSDesktop designDesktop = designPerspective.getWorkspace().getDesktop(0);
			designDesktop.clear();
			designDesktop.addDockable(processPanelKey);		
			designDesktop.split(processPanelKey, propertyTableKey, DockingConstants.SPLIT_RIGHT, 0.8);				
			designDesktop.split(propertyTableKey, operatorHelpKey, DockingConstants.SPLIT_BOTTOM, .66);
			designDesktop.createTab(propertyTableKey, processContextEditorKey, 1);
			
			designDesktop.createTab(operatorHelpKey, commentEditorKey, 1);

//			designDesktop.split(processPanelKey, overviewKey, DockingConstants.SPLIT_LEFT, 0.25);
//			designDesktop.split(overviewKey, newOperatorEditorKey, DockingConstants.SPLIT_BOTTOM, 0.2);
//			designDesktop.createTab(newOperatorEditorKey, repositoryKey, 1);
			
			designDesktop.split(processPanelKey, newOperatorEditorKey, DockingConstants.SPLIT_LEFT, 0.25);
			designDesktop.split(newOperatorEditorKey, repositoryKey, DockingConstants.SPLIT_BOTTOM, 0.5);
			//designDesktop.createTab(newOperatorEditorKey, repositoryKey, 1);

			designDesktop.split(processPanelKey, errorTableKey, DockingConstants.SPLIT_BOTTOM, 0.8);
			designDesktop.createTab(errorTableKey, messageViewerKey, 1);

			designDesktop.createTab(processPanelKey, xmlEditorKey, 1);
		} else if ("result".equals(perspectiveName)) {
			Perspective resultPerspective = getPerspective("result");
			WSDesktop resultsDesktop = resultPerspective.getWorkspace().getDesktop(0);
			resultsDesktop.clear();
			resultsDesktop.addDockable(resultsKey);
			resultsDesktop.split(resultsKey, messageViewerKey, DockingConstants.SPLIT_BOTTOM, 0.8);
			resultsDesktop.split(messageViewerKey, new WSDockKey(SystemMonitor.SYSTEM_MONITOR_DOCK_KEY), DockingConstants.SPLIT_RIGHT, 0.8);
			resultsDesktop.split(resultsKey, repositoryKey, DockingConstants.SPLIT_RIGHT, 0.8);		
		} else if ("welcome".equals(perspectiveName)) {
			Perspective welcomePerspective = getPerspective("welcome");	
			WSDesktop welcomeDesktop = welcomePerspective.getWorkspace().getDesktop(0);
			welcomeDesktop.clear();
			welcomeDesktop.addDockable(welcomeKey);
		} else {
			throw new IllegalArgumentException("Not a predevined perspective: "+perspectiveName);
		}
	}
}
