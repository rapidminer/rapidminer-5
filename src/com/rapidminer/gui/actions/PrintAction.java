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
import java.awt.print.PrinterException;

import com.rapidminer.gui.tools.ComponentPrinter;
import com.rapidminer.gui.tools.PrintingTools;
import com.rapidminer.gui.tools.ResourceAction;
import com.rapidminer.gui.tools.SwingTools;


/**
 * Start the corresponding action.
 * 
 * @author Ingo Mierswa
 */
public class PrintAction extends ResourceAction {

	private static final long serialVersionUID = -9086092676881347047L;

	private final Component component;
	
	public PrintAction(Component component, String componentName) {
		super("print", componentName);
		this.component = component;
	}

	public void actionPerformed(ActionEvent e) {
		PrintingTools.getPrinterJob().setPrintable(new ComponentPrinter(component));
		if (PrintingTools.getPrinterJob().printDialog()) {
			try {
				PrintingTools.getPrinterJob().print();
			} catch (PrinterException pe) {
				SwingTools.showSimpleErrorMessage("printer_error", pe);
			}
		}	
	}

	protected static void pageSetup() {
		PrintingTools.setPageFormat(PrintingTools.getPrinterJob().pageDialog(PrintingTools.getPageFormat()));		
	}	
}
