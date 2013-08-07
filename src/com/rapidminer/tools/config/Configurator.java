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
package com.rapidminer.tools.config;

import java.util.List;
import java.util.Map;

import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeString;
import com.rapidminer.tools.I18N;
import com.rapidminer.tools.config.gui.ConfigurationPanel;
import com.rapidminer.tools.config.gui.DefaultConfigurationPanel;

/** Can be used to configure {@link Configurable}s. The {@link ConfigurationManager} will take
 *  care of saving the configuration to configuration files or to a database and to provide access
 *  to dialogs which can be used to edit these configurables. 
 *  
 *  The I18N Keys can be found at the {@link Configurable} Interface.
 * 
 * @author Simon Fischer, Dominik Halfkann
 */
public abstract class Configurator<T extends Configurable> {

	/** Returns the {@link Configurable} implementation that this configurator can configure. */
	public abstract Class<T> getConfigurableClass();
	
	/** Parameter types used to configure this Configurator. Values will be passed to {@link #configure(Map)}. */
	public abstract List<ParameterType> getParameterTypes();
	
	/** Creates a new {@link Configurable} based on parameters. The parameters passed to this method match the
	 *  ones specified by {@link #getParameterTypes()}. 
	 * @throws ConfigurationException 
	 *  @name a unique (user defined) name identifying this {@link Configurable}. */
	public T create(String name, Map<String,String> parameters) throws ConfigurationException {
		T instance;
		try {
			instance = getConfigurableClass().newInstance();
			instance.setName(name);
			instance.configure(parameters);
		} catch (InstantiationException e) {
			throw new ConfigurationException("Cannot instantiate "+getConfigurableClass(), e);
		} catch (IllegalAccessException e) {
			throw new ConfigurationException("Cannot access "+getConfigurableClass(), e);
		} catch (Throwable e) {
			throw new ConfigurationException("Cannot instantiate "+getConfigurableClass() + " (fatal error)", e);
		}
		return instance;
	}

	/** The display name used in UI components. Based on {@link #getI18NBaseKey()}. */
	public String getName() {
		return I18N.getMessage(I18N.getGUIBundle(), "gui.configurable."+getI18NBaseKey()+".name");
	}

	/** A short help text to be used in dialogs. Based on {@link #getI18NBaseKey()}. */
	public final String getDescription() {
		return I18N.getMessage(I18N.getGUIBundle(), "gui.configurable."+getI18NBaseKey()+".description");
	}

	/** A short help text to be used in dialogs. Based on {@link #getI18NBaseKey()}. */
	public final String getIconName() {
		return I18N.getMessage(I18N.getGUIBundle(), "gui.configurable."+getI18NBaseKey()+".icon");
	}
	
	/** The ID used for identifying this Configurator. Must be a valid XML tag identifier and
	 *  file name. Should include the plugin namespace. 
	 *  Example: "olap_connection". */
	public abstract String getTypeId();

	/** The base key used in I18N property files. */
	public abstract String getI18NBaseKey();	
	
	/** Creates a new panel which is used to configure a {@link Configurable}. Initializes
	 *  all components with the current parameters of the configurable. */
	public ConfigurationPanel<? super T> createConfigurationPanel() {
		List<ParameterType> parameterTypeList = this.getParameterTypes();
		parameterTypeList.add(0, new ParameterTypeString("Name", "Name of this entry", false));
		return new DefaultConfigurationPanel(parameterTypeList); // Implement via GenericPropertyPanel
	}
}
