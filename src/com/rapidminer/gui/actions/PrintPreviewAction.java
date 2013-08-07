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

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.print.Printable;

import javax.swing.JToolBar;

import com.rapidminer.gui.RapidMinerGUI;
import com.rapidminer.gui.tools.ComponentPrinter;
import com.rapidminer.gui.tools.PrintingTools;
import com.rapidminer.gui.tools.ResourceAction;

import de.java.print.PreviewDialog;


/**
 * Start the corresponding action.
 * 
 * @author Ingo Mierswa
 */
public class PrintPreviewAction extends ResourceAction {

	private static final long serialVersionUID = -20782278995130227L;
	
	private final Component component;
	
	public PrintPreviewAction(Component component, String componentName) {
		super("print_preview", componentName);
		this.component = component;
	}

	public void actionPerformed(ActionEvent e) {
		Printable printer = new ComponentPrinter(component);
		PreviewDialog dialog = new PreviewDialog("Print Preview", RapidMinerGUI.getMainFrame(), printer, PrintingTools.getPageFormat(), 1);
		Component[] dialogComponents = dialog.getContentPane().getComponents();
		for (Component c : dialogComponents) {
			if (c instanceof JToolBar)
				((JToolBar)c).setFloatable(false);
		}
		dialog.pack();
		dialog.setLocationRelativeTo(RapidMinerGUI.getMainFrame());
		dialog.setVisible(true);
	}
}
