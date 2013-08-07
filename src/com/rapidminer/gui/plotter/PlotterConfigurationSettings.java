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
package com.rapidminer.gui.plotter;

import java.util.HashMap;

/**
 * This is the data holding class for plotter settings. It is used by the PlotterConfigurationModel to store
 * the actual parameters and their values.
 * 
 * @author Sebastian Land
 */
public class PlotterConfigurationSettings {
	private String plotterName;	
	private HashMap<String, String> parameterSettings = new HashMap<String, String>();
	private HashMap<String, Class<? extends Plotter>> availablePlotters;
	
	public PlotterConfigurationSettings() {}
	
	/**
	 * This is the clone constructor.
	 */
	private PlotterConfigurationSettings(PlotterConfigurationSettings other) {
		plotterName = other.plotterName;
		parameterSettings.putAll(other.parameterSettings);
		availablePlotters = other.availablePlotters;
	}

	@Override
	public PlotterConfigurationSettings clone() {
		return new PlotterConfigurationSettings(this);
	}
	
	public String getPlotterName() {
		return plotterName;
	}
	public void setPlotterName(String plotterName) {
		this.plotterName = plotterName;
	}
	public HashMap<String, String> getParameterSettings() {
		return parameterSettings;
	}

	public HashMap<String, Class<? extends Plotter>> getAvailablePlotters() {
		return availablePlotters;
	}

	public void setAvailablePlotters(HashMap<String, Class<? extends Plotter>> availablePlotters) {
		this.availablePlotters = availablePlotters;
	}
	
	/* Parameter methods */
	
	/**
	 * This method sets a parameter specified by the key to the 
	 * given value. Calling this method will not inform any listener since it's only a data storage.
	 * Please use setParameterValue of PlotterConfigurationModel instead.
	 */
	public void setParameterValue(String key, String value) {
		parameterSettings.put(key, value);
	}
	
	/** This method will return the parameter value of the given generalized key.
	 *  Generalized keys will be used internally by the PlotterSettings.
	 */
	public String getParameterValue(String generalizedKey) {
		return parameterSettings.get(generalizedKey);
	}
	
	/**
	 * This method checks whether the parameter with this generalized key is stored.
	 */
	public boolean isParameterSet(String generalizedKeyName) {
		return parameterSettings.containsKey(generalizedKeyName);
	}
}
