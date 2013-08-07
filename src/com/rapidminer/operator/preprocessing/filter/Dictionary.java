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
package com.rapidminer.operator.preprocessing.filter;

import java.util.List;
import java.util.Set;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.Attributes;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.table.NominalMapping;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.preprocessing.PreprocessingModel;

/** 
 * Replaces strings by interpreting the second example set as a dictionary. The 
 * 
 * @author Simon Fischer
 */
public class Dictionary extends PreprocessingModel {	

	private static final long serialVersionUID = 1441613108993813785L;

	private List<String[]> replacements;
	private String[] affectedAttributeNames;

	private boolean regexp = false;
	private boolean toLowerCase = false;

	private boolean stopAfterFirstMatch = false;
	
	public Dictionary(ExampleSet exampleSet, 
			Set<Attribute> attributesAffected, 
			List<String[]> replacements, 
			boolean regexp, 
			boolean toLowerCase,
			boolean stopAfterFistMatch) {
		super(exampleSet);
		this.stopAfterFirstMatch = stopAfterFistMatch;
		this.regexp = regexp;
		this.replacements = replacements;
		this.toLowerCase = toLowerCase;
		affectedAttributeNames = new String[attributesAffected.size()];
		int i = 0;
		for (Attribute attribute: attributesAffected) {
			affectedAttributeNames[i] = attribute.getName();
			i++;
		}
	}

	private void remap(Attributes attributes) {
		for (String attributeName: affectedAttributeNames) {
			Attribute attr = attributes.get(attributeName);
			if (attr.isNominal()) {
				NominalMapping mapping = attr.getMapping();
				for (String string : mapping.getValues()) {
					int index = mapping.getIndex(string);
					mapping.setMapping(replace(string), index);
				}
			}
		}
	}

	private String replace(String string) {		
		if (toLowerCase) {
			string = string.toLowerCase();
		}
		for (String[] replacement : replacements) {		
			if (regexp) {
				if (stopAfterFirstMatch) {
					if (string.matches(replacement[0])) {
						String newString = string.replaceAll(replacement[0], replacement[1]);
						return newString;
					}
				} else {
					string = string.replaceAll(replacement[0], replacement[1]);
				}
			} else {								
				boolean foundMatch = false;
				StringBuilder soFar = new StringBuilder("");
				String remainder = string;
				while (true) {
					int pos = remainder.indexOf(replacement[0]);
					if (pos == -1) {
						break;
					}
					foundMatch = true;
					soFar.append(remainder.substring(0, pos));
					soFar.append(replacement[1]);
					remainder = remainder.substring(pos + replacement[0].length());					
				}
				soFar.append(remainder);
				string = soFar.toString();
				if (foundMatch && stopAfterFirstMatch) {
					return string;
				}
			}
		}
		return string;
	}

	@Override
	public ExampleSet applyOnData(ExampleSet exampleSet) throws OperatorException {
		remap(exampleSet.getAttributes());
		return exampleSet;
	}

	public Attributes getTargetAttributes(ExampleSet viewParent) {
		Attributes attributes = (Attributes) viewParent.getAttributes().clone();
		remap(attributes);		
		return attributes;
	}

	public double getValue(Attribute targetAttribute, double value) {
		return value;
	}

	@Override
	public String toString() {
		StringBuilder b = new StringBuilder();
		for (String[] replacement : replacements) {
			b.append(replacement[0]);
			b.append(" -> ");
			b.append(replacement[1]);
			b.append("\n");			
		}
		return b.toString();
	}
}
