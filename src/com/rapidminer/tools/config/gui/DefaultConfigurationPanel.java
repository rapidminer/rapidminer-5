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

import java.awt.BorderLayout;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.JComponent;
import javax.swing.JPanel;

import com.rapidminer.gui.properties.GenericParameterPanel;
import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.Parameters;
import com.rapidminer.parameter.UndefinedParameterError;
import com.rapidminer.tools.config.Configurable;


/**
 * The default implementation of the {@link ConfigurationPanel}.
 * The DefaultCongurationPanel creates a {@link GenericParameterPanel} for managing {@link Configurable}s.
 * 
 * The I18N Keys can be found at the {@link Configurable} interface.
 * 
 * @author Dominik Halfkann
 *
 */
public class DefaultConfigurationPanel extends ConfigurationPanel<Configurable> {

	private JPanel panelWrapper = new JPanel(new BorderLayout());
	private GenericParameterPanel panel = null;
	private List<ParameterType> parameterList;
	
	public DefaultConfigurationPanel(List<ParameterType> parameterList) {
		this.parameterList = parameterList;
		panel = new GenericParameterPanel(new Parameters(parameterList));
		panelWrapper.add(panel, BorderLayout.CENTER);
		
	}
	
	@Override
	public boolean checkFields() {
		Parameters parameters = panel.getParameters();
		Set<String> keys = parameters.getKeys();
		for (String key : keys) {
			String value = null;
			try {
				value = parameters.getParameter(key);
				if (!parameters.getParameterType(key).isOptional() && (value == null || value.equals(""))) {
					SwingTools.showVerySimpleErrorMessage("configuration.dialog.missing", key);
					return false;
				}
			} catch (UndefinedParameterError e) {
				SwingTools.showVerySimpleErrorMessage("configuration.dialog.missing", key);
				return false;
			}
		}
		
		return true;
	}

	@Override
	public JComponent getComponent() {
		return panelWrapper;
	}

	@Override
	public void updateComponents(Configurable configurable) {
		Parameters panelParameters = new Parameters(parameterList); //panel.getParameters();
		Map<String, String> configParameters = configurable.getParameters();
		
		panelParameters.setParameter("Name", configurable.getName());
		
		for (String key : configParameters.keySet()) {
			panelParameters.setParameter(key, configParameters.get(key));
		}
		panel.clearProperties();
		panelWrapper.remove(panel);
		panel = new GenericParameterPanel(panelParameters);
		panelWrapper.add(panel, BorderLayout.CENTER);
	}

	@Override
	public void updateConfigurable(Configurable configurable)  {
		panel.fireEditingStoppedEvent();
		Parameters parameters = panel.getParameters();

		Set<String> keys = parameters.getKeys();
		Map<String, String> parameterValues = new HashMap<String, String>();
		for (String key : keys) {
			String value = null;
			Exception cause;
			try {
				value = parameters.getParameter(key);
			} catch (UndefinedParameterError e) {
				// may occur, do nothing
			}
			// value may be null, so change it to an empty string to prevent unwanted errors
			if (value == null) value = "";
			if (key.equals("Name")) {
				configurable.setName(value);
			} else {
				parameterValues.put(key, value);
			}
			
		}

		configurable.configure(parameterValues);
	}

}
