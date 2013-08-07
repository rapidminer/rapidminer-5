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

import javax.swing.JToolBar;


/**
 * This toolbar extension is not floatable and activate the hover effect.
 * 
 * @author Ingo Mierswa
 */
public class ExtendedJToolBar extends JToolBar {

	private static final long serialVersionUID = -9219638829666999431L;
	
	public ExtendedJToolBar() {
		super();
		setFloatable(false);
		setRollover(true);
		setBorder(null);
	}
}
