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
package com.rapidminer.tools;

import java.awt.Desktop;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

import javax.swing.Action;
import javax.swing.event.HyperlinkListener;

import com.rapidminer.gui.MainFrame;
import com.rapidminer.gui.RapidMinerGUI;
import com.rapidminer.gui.actions.SettingsAction;
import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorCreationException;

/** Convenience class for invoking certain actions from URLs, e.g. from inside a
 *  {@link HyperlinkListener}. URLs of the form "rm://actionName" will be interpreted.
 *  Actions can register themselves by invoking {@link RMUrlHandler#register(String, Action)}. 
 * 
 * @author Simon Fischer
 *
 */
public class RMUrlHandler {

	public static final String URL_PREFIX = "rm://";
	public static final String PREFERENCES_URL = URL_PREFIX + "preferences";
	
	private static final Map<String,Action> ACTION_MAP = new HashMap<String,Action>();
	
	static {
		register("preferences", new SettingsAction());
	}
	
	/**
	 * 
	 * @return true iff we understand the url.
	 */
	public static boolean handleUrl(String url) {
		if (url.startsWith(URL_PREFIX)) {
			String suffix = url.substring(URL_PREFIX.length());
			if (suffix.startsWith("opdoc/")) {
				String opName = suffix.substring("opdoc/".length());
				Operator op;
				try {
					op = OperatorService.createOperator(opName);
					RapidMinerGUI.getMainFrame().getOperatorDocViewer().setDisplayedOperator(op);
				} catch (OperatorCreationException e) {
					//LogService.getRoot().log(Level.WARNING, "Cannot create operator: "+opName, e);
					LogService.getRoot().log(Level.WARNING,
							I18N.getMessage(LogService.getRoot().getResourceBundle(), 
							"com.rapidminer.tools.RMUrlHandler.creating_operator_error", 
							opName),
							e);
				}
				return true;
			}
			if (suffix.startsWith("operator/")) {
				String opName = suffix.substring("operator/".length());
				MainFrame mainFrame = RapidMinerGUI.getMainFrame();
				mainFrame.selectOperator(mainFrame.getProcess().getOperator(opName));
				return true;			
			}
			Action action = ACTION_MAP.get(suffix);
			if (action != null) {
				action.actionPerformed(null);				
			} else {
				//LogService.getRoot().warning("No action associated with URL "+url);
				LogService.getRoot().log(Level.WARNING, "com.rapidminer.tools.RMUrlHandler.no_action_associated_with_url", url);
			}			
			return true; // we didn't make it, but noone else can, so we return true.
		} else if (url.startsWith("http://") || (url.startsWith("https://"))) {
			try {
				Desktop.getDesktop().browse(new URI(url));
			} catch (Exception e) {
				SwingTools.showSimpleErrorMessage("cannot_open_browser", e);				
			}
			return true;
		} else {
			return false;
		}
	}

	public static void register(String name, Action action) {
		ACTION_MAP.put(name, action);
	}
}
