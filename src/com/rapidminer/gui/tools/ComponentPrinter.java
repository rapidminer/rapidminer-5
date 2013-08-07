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
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.awt.print.Printable;

import com.rapidminer.Process;
import com.rapidminer.ProcessLocation;
import com.rapidminer.gui.RapidMinerGUI;


/**
 * A Printable that can print an arbitrary component. It scales and translates
 * the pages such that the complete component is visible.
 * 
 * @author Simon Fischer, Ingo Mierswa
 */
public class ComponentPrinter implements Printable {

	private Component component = null;

	private static final Font TITLE_FONT = new Font("LucidaSans", Font.PLAIN, 9);

	/** The given component should be printed. */
	public ComponentPrinter(Component component) {
		this.component = component;
	}

	public int print(Graphics g, java.awt.print.PageFormat pageFormat, int pageIndex) {
		if (pageIndex >= 1)
			return NO_SUCH_PAGE;

		String title = "RapidMiner";
		Process process = RapidMinerGUI.getMainFrame().getProcess(); 
		if (process != null) {
			ProcessLocation loc = process.getProcessLocation();
			if (loc != null) {
				title += ": " + loc.getShortName();
			}
		}
		Rectangle2D rect = TITLE_FONT.getStringBounds(title, ((Graphics2D) g).getFontRenderContext());
		g.setFont(TITLE_FONT);
		g.drawString(title, (int) (pageFormat.getImageableX() + pageFormat.getImageableWidth() / 2 - rect.getWidth() / 2), (int) (pageFormat.getImageableY() - rect.getY()));

		Graphics2D translated = (Graphics2D) g.create((int) pageFormat.getImageableX(), (int) (pageFormat.getImageableY() + rect.getHeight() * 2), (int) pageFormat.getImageableWidth(), (int) (pageFormat.getImageableHeight() - rect.getHeight() * 2));
		double widthFactor = pageFormat.getImageableWidth() / component.getWidth();
		double heightFactor = pageFormat.getImageableHeight() / component.getHeight();
		double scaleFactor = Math.min(widthFactor, heightFactor); 
		translated.scale(scaleFactor, scaleFactor);

		component.print(translated);
		translated.dispose();
		return PAGE_EXISTS;
	}

}
