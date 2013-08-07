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

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
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
import com.rapidminer.tools.RandomGenerator;


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
public class Obfuscator extends AbstractDataProcessing {


	/** The parameter name for &quot;File where the obfuscator map should be written to.&quot; */
	public static final String PARAMETER_OBFUSCATION_MAP_FILE = "obfuscation_map_file";

	public Obfuscator(OperatorDescription description) {
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
		// init		 
		Map<String, String> obfuscatorMap = new HashMap<String, String>();

		RandomGenerator random = RandomGenerator.getRandomGenerator(this);

		// obfuscate regular attributes
		Iterator<Attribute> i = exampleSet.getAttributes().allAttributes();
		while (i.hasNext()) {
			obfuscateAttribute(i.next(), obfuscatorMap, random);
		}

		File file = getParameterAsFile(PARAMETER_OBFUSCATION_MAP_FILE, true);
		if (file != null) {
			try {
				writeObfuscatorMap(obfuscatorMap, file);
			} catch (IOException e) {
				throw new UserError(this, 303, getParameterAsString(PARAMETER_OBFUSCATION_MAP_FILE), e.getMessage());
			}
		}

		return exampleSet;
	}

	private void obfuscateAttribute(Attribute attribute, Map<String, String> obfuscatorMap, RandomGenerator random) {
		String oldName = attribute.getName();
		String newName = random.nextString(8);
		attribute.setName(newName);
		attribute.setConstruction(newName);
		obfuscatorMap.put(newName, oldName);

		if (attribute.isNominal()) {
			Iterator<String> v = attribute.getMapping().getValues().iterator();
			while (v.hasNext()) {
				String oldValue = v.next();
				String newValue = random.nextString(8);
				Tools.replaceValue(attribute, oldValue, newValue);
				obfuscatorMap.put(oldName + ":" + newValue, oldValue);
			}
		}
	}

	private void writeObfuscatorMap(Map<String,String> obfuscatorMap, File file) throws IOException {
		PrintWriter out = null;
		try {
			out = new PrintWriter(new FileWriter(file));
			Iterator<Map.Entry<String,String>> i =
				obfuscatorMap.entrySet().iterator();
			while (i.hasNext()) {
				Map.Entry<String,String> e = i.next();
				out.println(e.getKey() + "\t" + e.getValue());
			}
		} catch (IOException e) {
			throw e;
		} finally {
			if (out != null) {
				out.close();		
			}
		}
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();
		ParameterType type = new ParameterTypeFile(PARAMETER_OBFUSCATION_MAP_FILE, "File where the obfuscator map should be written to.", "obf", true);
		types.add(type);

		types.addAll(RandomGenerator.getRandomGeneratorParameters(this));

		return types;
	}
	
	/**
	 * Only nominal mapping is changed, not write through on data
	 */
	@Override
	public boolean writesIntoExistingData() {
		return false;
	}
	
	@Override
	public ResourceConsumptionEstimator getResourceConsumptionEstimator() {
		return OperatorResourceConsumptionHandler.getResourceConsumptionEstimator(getInputPort(), Obfuscator.class, null);
	}
}
