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
package com.rapidminer.operator.preprocessing;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.Tools;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.UserError;
import com.rapidminer.operator.annotation.ResourceConsumptionEstimator;
import com.rapidminer.operator.ports.metadata.ExampleSetMetaData;
import com.rapidminer.operator.ports.metadata.MetaData;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeFile;
import com.rapidminer.parameter.UndefinedParameterError;
import com.rapidminer.tools.OperatorResourceConsumptionHandler;


/**
 * This operator takes an <code>ExampleSet</code> as input and maps all
 * nominal values to randomly created strings. The names and the construction
 * descriptions of all attributes will also replaced by random strings. This
 * operator can be used to anonymize your data. It is possible to save the
 * obfuscating map into a file which can be used to remap the old values and
 * names. Please use the operator <code>Deobfuscator</code> for this purpose.
 * The new example set can be written with an <code>ExampleSetWriter</code>.
 * 
 * @author Ingo Mierswa
 */
public class Deobfuscator extends AbstractDataProcessing {


	/** The parameter name for &quot;File where the obfuscator map was written to.&quot; */
	public static final String PARAMETER_OBFUSCATION_MAP_FILE = "obfuscation_map_file";
	public Deobfuscator(OperatorDescription description) {
		super(description);
	}

	@Override
	protected MetaData modifyMetaData(ExampleSetMetaData metaData) throws UndefinedParameterError {
		metaData.clear();
		metaData.attributesAreSuperset();
		return metaData;
	}

	@Override
	public ExampleSet apply(ExampleSet exampleSet) throws OperatorException {		
		File file = getParameterAsFile(PARAMETER_OBFUSCATION_MAP_FILE);
		Map<String, String> obfuscatorMap = null;
		try {
			obfuscatorMap = readObfuscatorMap(file);
		} catch (IOException e) {
			throw new UserError(this, 302, getParameterAsString(PARAMETER_OBFUSCATION_MAP_FILE), e.getMessage());
		}

		// de-obfuscate attributes
		Iterator<Attribute> i = exampleSet.getAttributes().allAttributes();
		while (i.hasNext()) {
			deObfuscateAttribute(i.next(), obfuscatorMap);
		}

		return exampleSet;
	}

	private void deObfuscateAttribute(Attribute attribute, Map<String, String> obfuscatorMap) {
		String obfuscatedName = attribute.getName();
		String newName = obfuscatorMap.get(obfuscatedName);
		if (newName != null) {
			attribute.setName(newName);
			attribute.setConstruction(newName);
		} else {
			logWarning("No name found in obfuscating map for attribute '" + obfuscatedName + "'.");
		}

		if (attribute.isNominal()) {
			Iterator<String> v = attribute.getMapping().getValues().iterator();
			while (v.hasNext()) {
				String obfuscatedValue = v.next();
				String newValue = obfuscatorMap.get(newName + ":" + obfuscatedValue);
				if (newValue != null) {
					Tools.replaceValue(attribute, obfuscatedValue, newValue);
				} else {
					logWarning("No value found in obfuscating map for value '" + obfuscatedValue + "' of attribute '" + attribute.getName() + "'.");
				}
			}
		}
	}

	private Map<String, String> readObfuscatorMap(File file) throws IOException {
		Map<String, String> map = new HashMap<String, String>();
		BufferedReader in = null;
		try {
			in = new BufferedReader(new FileReader(file));
			String line = null;
			while ((line = in.readLine()) != null) {
				String[] parts = line.trim().split("\\s");
				map.put(parts[0], parts[1]);
			}
		} catch (IOException e) {
			throw e;
		} finally {
			if (in != null) {
				in.close();
			}
		}
		return map;
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();
		ParameterType type = new ParameterTypeFile(PARAMETER_OBFUSCATION_MAP_FILE, "File where the obfuscator map was written to.", "obf", false);
		type.setExpert(false);
		types.add(type);
		return types;
	}
	
	@Override
	public boolean writesIntoExistingData() {
		return false;
	}
	
	@Override
	public ResourceConsumptionEstimator getResourceConsumptionEstimator() {
		return OperatorResourceConsumptionHandler.getResourceConsumptionEstimator(getInputPort(), Deobfuscator.class, null);
	}
}
