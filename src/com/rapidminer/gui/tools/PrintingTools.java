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
package com.rapidminer.gui.tools;

import java.awt.Component;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;

import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.PrintRequestAttributeSet;
import javax.swing.JMenu;

import com.rapidminer.gui.actions.ExportPdfAction;
import com.rapidminer.gui.actions.ExportViewAction;
import com.rapidminer.gui.actions.PrintAction;
import com.rapidminer.gui.actions.PrintPreviewAction;
import com.rapidminer.gui.tools.components.DropDownButton;

/** This class has static references to a printer job and page format.
 *  It also serves as a factory for printer menus.
 *  
 * @author Simon Fischer
 *
 */
public class PrintingTools {

	private static final PrinterJob PRINTER_JOB = PrinterJob.getPrinterJob();
	private static PageFormat pageFormat = getPrinterJob().defaultPage();
	private static PrintRequestAttributeSet printSettings = new HashPrintRequestAttributeSet();
	
	public static PrinterJob getPrinterJob() {
		return PRINTER_JOB;
	}

	public static void setPageFormat(PageFormat pageFormat) {
		PrintingTools.pageFormat = pageFormat;
	}

	public static PageFormat getPageFormat() {
		return pageFormat;
	}

	public static void editPrintSettings() {
		getPrinterJob().pageDialog(printSettings);
	}
	
	public static PrintRequestAttributeSet getPrintSettings() {
		return printSettings;
	}
	
	public static void print(Printable printable) {
		getPrinterJob().setPrintable(printable);
		if (getPrinterJob().printDialog()) {
			try {
				PrintingTools.getPrinterJob().print(printSettings);
			} catch (PrinterException e) {
				e.printStackTrace();
			}
		}
	}

	public static void print(Printable printable, PrintRequestAttributeSet printSettings) {
		getPrinterJob().setPrintable(printable);
		if (getPrinterJob().printDialog(printSettings)) {
			try {
				PrintingTools.getPrinterJob().print(printSettings);
			} catch (PrinterException e) {
				e.printStackTrace();
			}
		}
	}
	
	public static DropDownButton makeExportPrintDropDownButton(Component component, String componentName) {
		return DropDownButton.makeDropDownButton(new ResourceActionAdapter(true, "export_and_print"), 
				new PrintAction(component, componentName),
				new PrintPreviewAction(component, componentName),
				new ExportPdfAction(component, componentName),
				new ExportViewAction(component, componentName));
	}

	public static JMenu makeExportPrintMenu(Component component, String componentName) {
		JMenu menu = new JMenu(new ResourceActionAdapter(true, "export_and_print")); 
		menu.add(new PrintAction(component, componentName));
		menu.add(new PrintPreviewAction(component, componentName));
		menu.add(new ExportViewAction(component, componentName));
		return menu;
	}
}
