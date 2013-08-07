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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.rapidminer.parameter.ParameterType;
import com.rapidminer.repository.remote.RemoteRepository;

/**
 * Abstract standard implementation of the {@link Configurable} class.
 * @author Simon Fischer, Dominik Halfkann
 *
 */
public abstract class AbstractConfigurable implements Configurable {

	private int id = -1;
	private String name = "name undefined";
	private Map<String,String> parameters = new HashMap<String, String>();
	private RemoteRepository source;
	
	@Override
	public int getId() {
		return id;
	}
	
	@Override
	public void setId(int id) {
		this.id = id;
	}
	
	@Override
	public String getParameter(String key) {
		return parameters.get(key);
	}
	
	@Override
	public void setParameter(String key, String value) {
		parameters.put(key, value);
	}
	@Override
	public void configure(Map<String,String> parameters) {
		this.parameters.clear();		
		this.parameters.putAll(parameters);
	}
	@Override
	public Map<String,String> getParameters() {
		return parameters;
	}
	
	@Override
	public String getName() {
		return this.name;
	}
	
	@Override
	public void setName(String name) {
		this.name = name;
	}
	
	@Override
	public void setSource(RemoteRepository source) {
		this.source = source;
	}
	
	@Override
	public RemoteRepository getSource() {
		return source;
	}
	
	@Override
	public String getShortInfo() {
		return null;
	}
	
	@Override
	public boolean hasSameValues(Configurable comparedConfigurable) {
		if (!name.equals(comparedConfigurable.getName())) {
			return false;
		}
		
		if (this.parameters.size() != comparedConfigurable.getParameters().size()) return false;
		
		for (Map.Entry<String, String> parameterEntry : this.parameters.entrySet()) {
			if (!parameterEntry.getValue().toString().equals(comparedConfigurable.getParameter(parameterEntry.getKey()).toString())) {
				// If the string comparison of the 2 objects with equals() returns false
				return false;
			}
		}
		return true;
	}
	
	@Override
	public boolean isEmptyOrDefault(Configurator configurator) {
		if (this.getName() != null && !this.getName().equals("")) {
			return false;
		} else if (this.getParameters() != null && this.getParameters().size() > 0) {
			for (String key : this.getParameters().keySet()) {
				// find default value
				String defaultValue = "";
				@SuppressWarnings("unchecked")
				List<ParameterType> types = configurator.getParameterTypes();
				for (ParameterType type : types) {
					if (type.getKey().equals(key)) {
						defaultValue = type.getDefaultValueAsString();
					}
				}
				if (this.getParameters().get(key) != null && !this.getParameters().get(key).equals("") && !this.getParameters().get(key).equals(defaultValue)) {
					return false;
				}
			}
			return true;
		}
		return true;
	}
}
