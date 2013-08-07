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
package com.rapidminer.tools.config.gui;

import javax.swing.JComponent;

import com.rapidminer.tools.config.Configurable;

/**
 * Abstract class for providing a {@link JComponent} which can be used by the 
 * {@link ConfigurationDialog} in order to modify the parameters of a {@link Configurable}
 * @author Dominik Halfkann
 *
 * @param <T> A subclass of {@link Configurable} which should be configured through the panel.
 */
public abstract class ConfigurationPanel<T extends Configurable> {

	/**
	 * Create JComponent with empty data fields.
	 * @return
	 */
	public abstract JComponent getComponent();

	/**
	 * Updates the data fields in the JComponent
	 * @param configurable
	 */
	public abstract void updateComponents(T configurable);
	
	/** Directly returns the Configurable from input fields **/
	public abstract void updateConfigurable(T configurable);
	

	/** Returns true if the required fields are filled, false otherwise **/
	public abstract boolean checkFields();
}
