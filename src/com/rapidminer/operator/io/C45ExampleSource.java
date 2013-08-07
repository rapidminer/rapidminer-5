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
package com.rapidminer.operator.io;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.table.AttributeFactory;
import com.rapidminer.example.table.DataRow;
import com.rapidminer.example.table.DataRowFactory;
import com.rapidminer.example.table.MemoryExampleTable;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.UserError;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeCategory;
import com.rapidminer.parameter.ParameterTypeFile;
import com.rapidminer.parameter.ParameterTypeString;
import com.rapidminer.tools.Ontology;
import com.rapidminer.tools.Tools;
import com.rapidminer.tools.io.Encoding;


/**
 * <p>Loads data given in C4.5 format (names and data file). Both files must be in
 * the same directory. You can specify one of the C4.5 files (either the data or
 * the names file) or only the filestem.</p>
 * 
 * <p>For a dataset named "foo", you will have two files: foo.data and foo.names. 
 * The .names file describes the dataset, while the .data file contains the examples 
 * which make up the dataset.</p>
 *
 * <p>The files contain series of identifiers and numbers with some surrounding 
 * syntax. A | (vertical bar) means that the remainder of the line should be 
 * ignored as a comment. Each identifier consists of a string of characters that 
 * does not include comma, question mark or colon. Embedded whitespce is also permitted 
 * but multiple whitespace is replaced by a single space.</p>
 * 
 * <p>The .names file contains a series of entries that describe the classes, 
 * attributes and values of the dataset.  Each entry can be terminated with a period, 
 * but the period can be omited if it would have been the last thing on a line.  
 * The first entry in the file lists the names of the classes, separated by commas. 
 * Each successive line then defines an attribute, in the order in which they will appear 
 * in the .data file, with the following format:</p>
 *
 * <pre>
 *   attribute-name : attribute-type
 * </pre>
 *
 * <p>
 * The attribute-name is an identifier as above, followed by a colon, then the attribute 
 * type which must be one of</p>
 * 
 * <ul>
 * <li><code>continuous</code> If the attribute has a continuous value.</li>
 * <li><code>discrete [n]</code> The word 'discrete' followed by an integer which 
 *     indicates how many values the attribute can take (not recommended, please use the method
 *     depicted below for defining nominal attributes).</li> 
 * <li><code>[list of identifiers]</code> This is a discrete, i.e. nominal, attribute with the 
 *     values enumerated (this is the prefered method for discrete attributes). The identifiers 
 *     should be separated by commas.</li>
 * <li><code>ignore</code> This means that the attribute should be ignored - it won't be used.
 *     This is not supported by RapidMiner, please use one of the attribute selection operators after
 *     loading if you want to ignore attributes and remove them from the loaded example set.</li>
 * </ul>
 * 
 * <p>Here is an example .names file:</p>
 * <pre>
 *   good, bad.
 *   dur: continuous.
 *   wage1: continuous.
 *   wage2: continuous.
 *   wage3: continuous.
 *   cola: tc, none, tcf.
 *   hours: continuous.
 *   pension: empl_contr, ret_allw, none.
 *   stby_pay: continuous.
 *   shift_diff: continuous.
 *   educ_allw: yes, no.
 *   ...
 * </pre>
 *
 * <p>Foo.data contains the training examples in the following format: one example per line, 
 * attribute values separated by commas, class last, missing values represented by "?". 
 * For example:</p>
 *
 * <pre>
 *   2,5.0,4.0,?,none,37,?,?,5,no,11,below_average,yes,full,yes,full,good
 *   3,2.0,2.5,?,?,35,none,?,?,?,10,average,?,?,yes,full,bad
 *   3,4.5,4.5,5.0,none,40,?,?,?,no,11,average,?,half,?,?,good
 *   3,3.0,2.0,2.5,tc,40,none,?,5,no,10,below_average,yes,half,yes,full,bad
 *   ...
 * </pre>
 * 
 * @author Ingo Mierswa
 */
public class C45ExampleSource extends AbstractExampleSource {

	/** The parameter name for &quot;The path to either the C4.5 names file, the data file, or the filestem (without extensions). Both files must be in the same directory.&quot; */
	public static final String PARAMETER_C45_FILESTEM = "c45_filestem";

	/** The parameter name for &quot;Determines, how the data is represented internally.&quot; */
	public static final String PARAMETER_DATAMANAGEMENT = "datamanagement";

	/** The parameter name for &quot;Character that is used as decimal point.&quot; */
	public static final String PARAMETER_DECIMAL_POINT_CHARACTER = "decimal_point_character";

	public C45ExampleSource(OperatorDescription description) {
		super(description);
	}

	@Override
	public ExampleSet createExampleSet() throws OperatorException {
		File file = getParameterAsFile(PARAMETER_C45_FILESTEM);

		// create attribute objects from names file
		Attribute label = AttributeFactory.createAttribute("label", Ontology.NOMINAL);
		List<Attribute> attributes = new LinkedList<Attribute>();

		File nameFile = getFile(file, "names");        
		BufferedReader in = null;
		Pattern separatorPattern = Pattern.compile(",");
		try {
			in = new BufferedReader(new InputStreamReader(new FileInputStream(nameFile), Encoding.getEncoding(this)));

			String line = null;
			while ((line = in.readLine()) != null) {

				// trim line
				line = line.trim();

				// skip comment parts
				int commentIndex = line.indexOf("|"); 
				if (commentIndex >= 0) {
					line = line.substring(0, commentIndex).trim();
				}

				// skip ending point in data line
				if ((line.length() > 0) && line.charAt(line.length() - 1) == '.') {
					line = line.substring(0, line.length() - 1).trim();
				}

				// skip empty lines (also comment only lines)
				if (line.length() == 0)
					continue;

				int colonIndex = line.indexOf(":"); 
				if (colonIndex >= 0) { // attribute
					String attributeName = line.substring(0, colonIndex).trim();
					String typeString = line.substring(colonIndex + 1).trim();
					int valueType = Ontology.NOMINAL;
					if (typeString.equals("continuous")) {
						valueType = Ontology.REAL;
					}
					Attribute attribute = AttributeFactory.createAttribute(attributeName, valueType);

					if ((valueType == Ontology.NOMINAL) && !typeString.equals("discrete")) {
						String possibleValuesString = typeString;
						String[] possibleValues = Tools.quotedSplit(possibleValuesString, separatorPattern);
						for (String s : possibleValues) {
							attribute.getMapping().mapString(s.trim());
						}
					}
					attributes.add(attribute);
				} else { // classes
					String[] possibleClasses = line.split(",");
					possibleClasses = Tools.quotedSplit(line, separatorPattern);
					for (String s : possibleClasses) {
						label.getMapping().mapString(s.trim());
					}
					// label: do not here but later (see below)
				}
			}
		} catch (IOException e) {
			throw new UserError(this, 302, nameFile, e.getMessage());
		} finally {
			if (in != null) {
				try {
					in.close();
				} catch (IOException e) {
					logError("Cannot close stream to file " + file);
				}
			}
		}

		// important: label is the last column in the data file
		attributes.add(label);

		// create and fill example table from data file
		File dataFile = getFile(file, "data");
		try {
			in = new BufferedReader(new InputStreamReader(new FileInputStream(dataFile),Encoding.getEncoding(this)));
		} catch (IOException e) {
			throw new UserError(this, 301, dataFile);
		}

		MemoryExampleTable table = new MemoryExampleTable(attributes);
		DataRowFactory factory = new DataRowFactory(getParameterAsInt(PARAMETER_DATAMANAGEMENT), getParameterAsString(PARAMETER_DECIMAL_POINT_CHARACTER).charAt(0));
		Attribute[] attributeArray = new Attribute[attributes.size()];
		attributes.toArray(attributeArray);
		try {
			int lineCounter = 0;
			String line = null;
			while ((line = in.readLine()) != null) {
				lineCounter++;

				// trim line
				line = line.trim();

				// skip comment parts
				int commentIndex = line.indexOf("|"); 
				if (commentIndex >= 0) {
					line = line.substring(0, commentIndex).trim();
				}

				// skip ending point in data line
				if ((line.length() > 0) && line.charAt(line.length() - 1) == '.') {
					line = line.substring(0, line.length() - 1).trim();
				}

				// skip empty lines (also comment only lines)
				if (line.length() == 0)
					continue;

				String[] tokens = Tools.quotedSplit(line, separatorPattern);
				if (tokens.length != attributes.size()) {
					in.close();
					throw new UserError(this, 302, file, "Line " + lineCounter + ": the number of tokens in each line must be the same as the number of attributes (" + attributes.size() + "), was: " + tokens.length);
				}
				DataRow row = factory.create(tokens, attributeArray);
				table.addDataRow(row);
			}

			in.close();
		} catch (IOException e) {
			throw new UserError(this, 302, nameFile, e.getMessage());
		}

		return table.createExampleSet(label);
	}

	private File getFile(File file, String extension) {
		String name = file.getName();

		String fileStem = null;
		if (name.indexOf('.') < 0) {
			fileStem = name;
		} else {
			fileStem = name.substring(0, name.lastIndexOf('.'));
		}

		return new File(file.getParent() + File.separator + fileStem + "." + extension);
	}

	@Override
	protected boolean supportsEncoding() {
		return true;
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = new LinkedList<ParameterType>();
		types.add(new ParameterTypeFile(PARAMETER_C45_FILESTEM, "The path to either the C4.5 names file, the data file, or the filestem (without extensions). Both files must be in the same directory.", null, false));
		types.add(new ParameterTypeCategory(PARAMETER_DATAMANAGEMENT, "Determines, how the data is represented internally.", DataRowFactory.TYPE_NAMES, DataRowFactory.TYPE_DOUBLE_ARRAY));
		types.add(new ParameterTypeString(PARAMETER_DECIMAL_POINT_CHARACTER, "Character that is used as decimal point.", ".", false));
		types.addAll(super.getParameterTypes());
		return types;
	}
}
