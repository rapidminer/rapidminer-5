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

import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.table.AttributeFactory;
import com.rapidminer.example.table.DataRow;
import com.rapidminer.example.table.DataRowFactory;
import com.rapidminer.example.table.MemoryExampleTable;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeCategory;
import com.rapidminer.parameter.ParameterTypeDouble;
import com.rapidminer.parameter.ParameterTypeInt;
import com.rapidminer.parameter.UndefinedParameterError;
import com.rapidminer.tools.Ontology;
import com.rapidminer.tools.RandomGenerator;
import com.rapidminer.tools.Tools;
import com.rapidminer.tools.att.AttributeSet;
import com.rapidminer.tools.math.sampling.OrderedSamplingWithoutReplacement;


/**
 * This operator can read stata files. Currently only stata
 * files of version 113 or 114 are supported.
 * 
 * @rapidminer.index stata
 * @author Tobias Malbrecht
 */
public class StataExampleSource extends BytewiseExampleSource {

	/** The parameter name for &quot;Determines which attribute properties should be used for attribute naming.&quot; */
	public static final String PARAMETER_ATTRIBUTE_NAMING_MODE = "attribute_naming_mode";

	/** The parameter name for &quot;Specifies how to handle attributes with value labels, i.e. whether to ignore the labels or how to use them.&quot; */
	public static final String PARAMETER_HANDLE_VALUE_LABELS = "handle_value_labels";

	/** The parameter name for &quot;The fraction of the data set which should be read (1 = all; only used if sample_size = -1)&quot; */
	public static final String PARAMETER_SAMPLE_RATIO = "sample_ratio";

	/** The parameter name for &quot;The exact number of samples which should be read (-1 = all; if not -1, sample_ratio will not have any effect)&quot; */
	public static final String PARAMETER_SAMPLE_SIZE = "sample_size";

	/** File suffix for stata files. */
	private static final String STATA_FILE_SUFFIX = "dta";

	/** Only use variable name as attribute name. */
	public static final int USE_VAR_NAME = 0;

	/** Only use variable label as attribute name. */
	public static final int USE_VAR_LABEL = 1;

	/** Use variable name with label in parentheses as attribute name. */
	public static final int USE_VAR_NAME_LABELED = 2;

	/** Use variable label with name in parentheses as attribute name. */
	public static final int USE_VAR_LABEL_NAMED = 3;

	/** String descriptions of attribute naming modes. */
	public static final String[] ATTRIBUTE_NAMING_MODES = { "name", "label", "name (label)", "label (name)" };

	/** Force attributes to be numeric even if value labels exist. */
	public static final int FORCE_NUMERIC = 0;

	/** Ignore existing value labels but let attribute be nominal. */
	public static final int IGNORE = 1;

	/** Use existing value labels for labeled values. */
	public static final int USE_ADDITIONALLY = 2;

	/** Use existing value labels and set all values without labels to unknown. */
	public static final int USE_EXCLUSIVELY = 3;

	/** String descriptions of value label handling modes. */
	public static final String[] HANDLE_VALUE_LABELS_MODES = { "force numeric", "ignore", "use additionally", "use exclusively" };

	/** File format constants... */
	private static final int CODE_STRING_TERMINATOR = 0x0;

	private static final int CODE_DS_FORMAT_VERSION_113 = 0x71;

	private static final int CODE_DS_FORMAT_VERSION_114 = 0x72;

	private static final int CODE_BYTEORDER_HILO = 0x01;

	private static final int CODE_BYTEORDER_LOHI = 0x02;

	private static final int CODE_FILETYPE = 0x01;

	private static final int LENGTH_HEADER = 109;

	private static final int INDEX_HEADER_DS_FORMAT = 0;

	private static final int INDEX_HEADER_BYTEORDER = 1;

	private static final int INDEX_HEADER_FILETYPE = 2;

	private static final int INDEX_HEADER_NUMBER_OF_ATTRIBUTES = 4;

	private static final int INDEX_HEADER_NUMBER_OF_EXAMPLES = 6;

	private static final int CODE_TYPE_BYTE = 0xfb;

	private static final int CODE_TYPE_INT = 0xfc;

	private static final int CODE_TYPE_LONG = 0xfd;

	private static final int CODE_TYPE_FLOAT = 0xfe;

	private static final int CODE_TYPE_DOUBLE = 0xff;

	private static final int LENGTH_TYPE_BYTE = 1;

	private static final int LENGTH_TYPE_INT = 2;

	private static final int LENGTH_TYPE_LONG = 4;

	private static final int LENGTH_TYPE_FLOAT = 4;

	private static final int LENGTH_TYPE_DOUBLE = 8;

	private static final int LENGTH_ATTRIBUTE_NAME = 33;

	private static final int LENGTH_ATTRIBUTE_FORMAT_VERSION_113 = 12;

	private static final int LENGTH_ATTRIBUTE_FORMAT_VERSION_114 = 49;

	private static final int LENGTH_ATTRIBUTE_VALUE_LABEL_IDENTIFIER = 33;

	private static final int LENGTH_ATTRIBUTE_LABEL = 81;

	private static final int LENGTH_EXPANSION_FIELD_HEADER = 5;

	private static final int INDEX_EXPANSION_FIELD_HEADER_TYPE = 0;

	private static final int INDEX_EXPANSION_FIELD_HEADER_LENGTH = 1;

	private static final int LENGTH_VALUE_LABEL_HEADER = 40;

	private static final int INDEX_VALUE_LABEL_HEADER_LENGTH = 0;

	private static final int INDEX_VALUE_LABEL_HEADER_NAME = 4;

	private static final int LENGTH_VALUE_LABEL_HEADER_NAME = 33;

	private static final int INDEX_VALUE_LABEL_TABLE_NUMBER_OF_ENTRIES = 0;

	private static final int INDEX_VALUE_LABEL_TABLE_TEXT_LENGTH = 4;

	private static final int INDEX_VALUE_LABEL_TABLE_OFFSETS = 8;

	private static final byte CODE_MAXIMUM_NONMISSING_BYTE = 100;

	private static final int CODE_MAXIMUM_NONMISSING_INT = 32740;

	private static final int CODE_MAXIMUM_NONMISSING_LONG = 2147483620;

	private static final double CODE_MAXIMUM_NONMISSING_FLOAT = 1.701e+38;

	private static final double CODE_MAXIMUM_NONMISSING_DOUBLE = 8.988e+307;

	public StataExampleSource(OperatorDescription description) {
		super(description);
	}

	@Override
	protected String getFileSuffix() {
		return STATA_FILE_SUFFIX;
	}

	@Override
	protected ExampleSet readStream(InputStream inputStream, DataRowFactory dataRowFactory) throws IOException, UndefinedParameterError {
		int attributeNamingMode = getParameterAsInt(PARAMETER_ATTRIBUTE_NAMING_MODE);
		int handleValueLabelsMode = getParameterAsInt(PARAMETER_HANDLE_VALUE_LABELS);
		double sampleRatio = getParameterAsDouble(PARAMETER_SAMPLE_RATIO);
		int sampleSize = getParameterAsInt(PARAMETER_SAMPLE_SIZE);
		RandomGenerator randomGenerator = RandomGenerator.getRandomGenerator(this);

		byte[] buffer = new byte[500];
		boolean reverseEndian = false;

		// read and check header
		read(inputStream, buffer, LENGTH_HEADER);
		int dataSetFormat = 0x000000FF & buffer[INDEX_HEADER_DS_FORMAT];
		if (dataSetFormat != CODE_DS_FORMAT_VERSION_113 && dataSetFormat != CODE_DS_FORMAT_VERSION_114) {
			throw new IOException("Unsupported data set format");
		}
		if (buffer[INDEX_HEADER_FILETYPE] != CODE_FILETYPE) {
			throw new IOException(GENERIC_ERROR_MESSAGE);
		}
		byte byteOrder = buffer[INDEX_HEADER_BYTEORDER];
		if (byteOrder != CODE_BYTEORDER_LOHI && byteOrder != CODE_BYTEORDER_HILO) {
			throw new IOException(GENERIC_ERROR_MESSAGE);
		}
		reverseEndian = (byteOrder == CODE_BYTEORDER_LOHI) ? true : false;
		int numberOfAttributes = extract2ByteInt(buffer, INDEX_HEADER_NUMBER_OF_ATTRIBUTES, reverseEndian);
		int numberOfExamples = extractInt(buffer, INDEX_HEADER_NUMBER_OF_EXAMPLES, reverseEndian);

		// read descriptors
		byte[] attributeTypes = new byte[numberOfAttributes];
		read(inputStream, buffer, numberOfAttributes);
		for (int i = 0; i < numberOfAttributes; i++) {
			attributeTypes[i] = buffer[i];
		}
		String[] attributeNames = new String[numberOfAttributes];
		for (int i = 0; i < numberOfAttributes; i++) {
			read(inputStream, buffer, LENGTH_ATTRIBUTE_NAME);
			String attributeNameString = new String(buffer, 0, LENGTH_ATTRIBUTE_NAME);
			attributeNames[i] = attributeNameString.substring(0, attributeNameString.indexOf(CODE_STRING_TERMINATOR)).trim();
		}

		// read sort list
		read(inputStream, buffer, 2 * (numberOfAttributes + 1));

		// read format list
		for (int i = 0; i < numberOfAttributes; i++) {
			if (dataSetFormat == CODE_DS_FORMAT_VERSION_113) {
				read(inputStream, buffer, LENGTH_ATTRIBUTE_FORMAT_VERSION_113);
			} else if (dataSetFormat == CODE_DS_FORMAT_VERSION_114) {
				read(inputStream, buffer, LENGTH_ATTRIBUTE_FORMAT_VERSION_114);
			}
		}

		// read value label identifiers
		String[] valueLabelsIdentifiers = new String[numberOfAttributes];
		boolean[] labeled = new boolean[numberOfAttributes];
		for (int i = 0; i < numberOfAttributes; i++) {
			read(inputStream, buffer, LENGTH_ATTRIBUTE_VALUE_LABEL_IDENTIFIER);
			labeled[i] = buffer[0] != 0; 
			String valueLabelsIdentifierString = new String(buffer, 0, LENGTH_ATTRIBUTE_VALUE_LABEL_IDENTIFIER);
			valueLabelsIdentifiers[i] = valueLabelsIdentifierString.substring(0, valueLabelsIdentifierString.indexOf(CODE_STRING_TERMINATOR)).trim();
			if (valueLabelsIdentifiers[i].equals("")) {
				valueLabelsIdentifiers[i] = null;
			}
		}

		// read attribute labels
		String[] attributeLabels = new String[numberOfAttributes];
		for (int i = 0; i < numberOfAttributes; i++) {
			read(inputStream, buffer, LENGTH_ATTRIBUTE_LABEL);
			String attributeLabelString = new String(buffer, 0, LENGTH_ATTRIBUTE_LABEL);
			attributeLabels[i] = attributeLabelString.substring(0, attributeLabelString.indexOf(CODE_STRING_TERMINATOR)).trim();
			if (attributeLabels[i].equals("")) {
				attributeLabels[i] = null;
			}
		}

		// read expansion fields
		for (;;) {
			read(inputStream, buffer, LENGTH_EXPANSION_FIELD_HEADER);
			int expansionFieldContentsLength = extractInt(buffer, INDEX_EXPANSION_FIELD_HEADER_LENGTH, reverseEndian);
			if (buffer[INDEX_EXPANSION_FIELD_HEADER_TYPE] == 0 && expansionFieldContentsLength == 0) {
				break;
			} else {
				read(inputStream, buffer, expansionFieldContentsLength);
			}
		}

		// create attributes
		LinkedHashMap<String, List<Attribute>> attributeValueLabelIdentifiersMap = new LinkedHashMap<String, List<Attribute>>();
		AttributeSet attributeSet = new AttributeSet(numberOfAttributes);
		for (int i = 0; i < numberOfAttributes; i++) {
			int valueType = Ontology.ATTRIBUTE_VALUE;
			switch (0x000000FF & attributeTypes[i]) {
			case CODE_TYPE_BYTE:
				valueType = Ontology.INTEGER;
				break;
			case CODE_TYPE_INT:
				valueType = Ontology.INTEGER;
				break;
			case CODE_TYPE_LONG:
				valueType = Ontology.INTEGER;
				break;
			case CODE_TYPE_FLOAT:
				valueType = Ontology.NUMERICAL;
				break;
			case CODE_TYPE_DOUBLE:
				valueType = Ontology.NUMERICAL;
				break;
			default:
				valueType = Ontology.NOMINAL;
			}
			if (labeled[i]) {
				if (handleValueLabelsMode != FORCE_NUMERIC) {
					valueType = Ontology.NOMINAL;
				}
			}
			String attributeName = null;
			switch (attributeNamingMode) {
			case USE_VAR_NAME:
				attributeName = attributeNames[i];
				break;
			case USE_VAR_LABEL:
				attributeName = attributeLabels[i] == null ? attributeNames[i] : attributeLabels[i];
				break;
			case USE_VAR_NAME_LABELED:
				attributeName = attributeLabels[i] == null ? attributeNames[i] : attributeNames[i] + " (" + attributeLabels[i] + ")";
				break;
			case USE_VAR_LABEL_NAMED:
				attributeName = attributeLabels[i] == null ? attributeNames[i] : attributeLabels[i] + " (" + attributeNames[i] + ")";
				break;
			default:
				attributeName = attributeNames[i];
			}
			Attribute attribute = AttributeFactory.createAttribute(attributeName, valueType);
			attributeSet.addAttribute(attribute);
			if (attributeValueLabelIdentifiersMap.get(valueLabelsIdentifiers[i]) == null) {
				attributeValueLabelIdentifiersMap.put(valueLabelsIdentifiers[i], new LinkedList<Attribute>());
			}
			if (valueLabelsIdentifiers[i] != null) {
				attributeValueLabelIdentifiersMap.get(valueLabelsIdentifiers[i]).add(attribute);
			}
		}

		// initialize sampling functionality
		OrderedSamplingWithoutReplacement sampling = null;
		if (sampleSize != -1) {
			sampling = new OrderedSamplingWithoutReplacement(randomGenerator, numberOfExamples, sampleSize);
		} else {
			sampling = new OrderedSamplingWithoutReplacement(randomGenerator, numberOfExamples, sampleRatio);
		}

		// read data
		MemoryExampleTable table = new MemoryExampleTable(attributeSet.getAllAttributes());
		for (int j = 0; j < numberOfExamples; j++) {
			DataRow dataRow = dataRowFactory.create(numberOfAttributes);
			for (int i = 0; i < numberOfAttributes; i++) {
				Attribute attribute = attributeSet.getAttribute(i);
				double value = Double.NaN;
				switch (0x000000FF & attributeTypes[i]) {
				case CODE_TYPE_BYTE:
					read(inputStream, buffer, LENGTH_TYPE_BYTE);
					byte byteValue = buffer[0];
					value = byteValue > CODE_MAXIMUM_NONMISSING_BYTE ? Double.NaN : byteValue;
					break;
				case CODE_TYPE_INT:
					read(inputStream, buffer, LENGTH_TYPE_INT);
					int intValue = extract2ByteInt(buffer, 0, reverseEndian);
					value = intValue > CODE_MAXIMUM_NONMISSING_INT ? Double.NaN : intValue;
					break;
				case CODE_TYPE_LONG:
					read(inputStream, buffer, LENGTH_TYPE_LONG);
					int longValue = extractInt(buffer, 0, reverseEndian);
					value = longValue > CODE_MAXIMUM_NONMISSING_LONG ? Double.NaN : longValue;
					break;
				case CODE_TYPE_FLOAT:
					read(inputStream, buffer, LENGTH_TYPE_FLOAT);
					float floatValue = extractFloat(buffer, 0, reverseEndian);
					value = floatValue > CODE_MAXIMUM_NONMISSING_FLOAT ? Double.NaN : floatValue;
					break;
				case CODE_TYPE_DOUBLE:
					read(inputStream, buffer, LENGTH_TYPE_DOUBLE);
					double doubleValue = extractDouble(buffer, 0, reverseEndian);
					value = doubleValue > CODE_MAXIMUM_NONMISSING_DOUBLE ? Double.NaN : doubleValue;
					break;
				default:
					int length = 0x000000FF & attributeTypes[i];
				read(inputStream, buffer, length);
				String stringValue = new String(buffer, 0, length);
				int stringTerminatorIndex = stringValue.indexOf(CODE_STRING_TERMINATOR);
				if (stringTerminatorIndex < 0 || stringTerminatorIndex >= length) {
					value = attribute.getMapping().mapString(stringValue.trim());
				} else {
					value = attribute.getMapping().mapString(stringValue.substring(0, stringTerminatorIndex).trim());
				}
				}
				dataRow.set(attribute, value);

			}

			// add data to table
			if (sampling == null) {
				table.addDataRow(dataRow);				
			} else {
				if (sampling.acceptElement()) {
					table.addDataRow(dataRow);
				}
			}

		}

		// read value labels
		int readLength = -1;
		LinkedHashMap<Attribute, LinkedHashMap<Double, String>> valueMappingsMap = new LinkedHashMap<Attribute, LinkedHashMap<Double, String>>();
		do {
			readLength = readWithoutLengthCheck(inputStream, buffer, LENGTH_VALUE_LABEL_HEADER);
			if (readLength > 0) {
				int length = extractInt(buffer, INDEX_VALUE_LABEL_HEADER_LENGTH, reverseEndian);
				String valueLabelIdentifierString = new String(buffer, INDEX_VALUE_LABEL_HEADER_NAME, LENGTH_VALUE_LABEL_HEADER_NAME);
				String valueLabelIdentifier = valueLabelIdentifierString.substring(0, valueLabelIdentifierString.indexOf(CODE_STRING_TERMINATOR)).trim();

				LinkedHashMap<Double, String> valueMap = new LinkedHashMap<Double, String>();
				if (length > 500) {
					buffer = new byte[length];
				}
				read(inputStream, buffer, length);
				int numberOfEntries = extractInt(buffer, INDEX_VALUE_LABEL_TABLE_NUMBER_OF_ENTRIES, reverseEndian);
				int textLength = extractInt(buffer, INDEX_VALUE_LABEL_TABLE_TEXT_LENGTH, reverseEndian);
				int[] offset = new int[numberOfEntries];
				for (int i = 0; i < numberOfEntries; i++) {
					offset[i] = extractInt(buffer, INDEX_VALUE_LABEL_TABLE_OFFSETS + i * LENGTH_INT_32, reverseEndian);
				}
				double[] values = new double[numberOfEntries];
				for (int i = 0; i < numberOfEntries; i++) {
					values[i] = extractInt(buffer, INDEX_VALUE_LABEL_TABLE_OFFSETS + numberOfEntries * LENGTH_INT_32 + i * LENGTH_INT_32, reverseEndian);
				}
				String[] nominalValues = new String[numberOfEntries];
				for (int i = 0; i < numberOfEntries; i++) {
					nominalValues[i] = extractString(buffer, INDEX_VALUE_LABEL_TABLE_OFFSETS + 2 * numberOfEntries * LENGTH_INT_32 + offset[i], textLength - offset[i]);
					int stringTerminatorIndex = nominalValues[i].indexOf(CODE_STRING_TERMINATOR);
					if (stringTerminatorIndex < 0) {
						valueMap.put(values[i], nominalValues[i].trim());
					} else {
						valueMap.put(values[i], nominalValues[i].substring(0, nominalValues[i].indexOf(CODE_STRING_TERMINATOR)).trim());
					}
				}
				for (Attribute attribute : attributeValueLabelIdentifiersMap.get(valueLabelIdentifier)) {
					valueMappingsMap.put(attribute, valueMap);
				}
			}
		} while (readLength >= 0);
		inputStream.close();

		// add value labels to data
		if (handleValueLabelsMode != FORCE_NUMERIC) {
			Attribute[] attributes = table.getAttributes();
			LinkedHashMap[] attributeValueMaps = new LinkedHashMap[numberOfAttributes];
			for (int i = 0; i < attributes.length; i++) {
				attributeValueMaps[i] = valueMappingsMap.get(attributes[i]);
			}
			for (Iterator<DataRow> iterator = table.getDataRowReader(); iterator.hasNext(); ) {
				DataRow dataRow = iterator.next();
				for (int i = 0; i < attributes.length; i++) {
					if (labeled[i] && attributeValueMaps[i] != null) {
						double originalValue = dataRow.get(attributes[i]);
						double value = Double.NaN;
						switch (handleValueLabelsMode) {
						case IGNORE:
							value = attributes[i].getMapping().mapString(Tools.formatIntegerIfPossible(originalValue));
							break;
						case USE_ADDITIONALLY: {
							String nominalValue = (String) attributeValueMaps[i].get(originalValue);
							if (nominalValue != null) {
								value = attributes[i].getMapping().mapString(nominalValue);
							} else {
								value = attributes[i].getMapping().mapString(Tools.formatIntegerIfPossible(originalValue));
							}
						}
						break;
						case USE_EXCLUSIVELY: {
							String nominalValue = (String) attributeValueMaps[i].get(originalValue);
							if (nominalValue != null) {
								value = attributes[i].getMapping().mapString(nominalValue);
							} else {
								value = Double.NaN;
							}
						}
						break;
						}
						dataRow.set(attributes[i], value);
					}
				}
			}
		}

		// create example set
		ExampleSet exampleSet = table.createExampleSet();
		return exampleSet;
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();
		ParameterType type = new ParameterTypeCategory(PARAMETER_ATTRIBUTE_NAMING_MODE, "Determines which variable properties should be used for attribute naming.", ATTRIBUTE_NAMING_MODES, USE_VAR_NAME);
		type.setExpert(false);
		types.add(type);
		type = new ParameterTypeCategory(PARAMETER_HANDLE_VALUE_LABELS, "Specifies how to handle attributes with value labels, i.e. whether to ignore the labels or how to use them.", HANDLE_VALUE_LABELS_MODES, USE_ADDITIONALLY);
		type.setExpert(false);
		types.add(type);
		type = new ParameterTypeDouble(PARAMETER_SAMPLE_RATIO, "The fraction of the data set which should be read (1 = all; only used if sample_size = -1)", 0.0d, 1.0d, 1.0d);
		type.setExpert(false);
		types.add(type);
		type = new ParameterTypeInt(PARAMETER_SAMPLE_SIZE, "The exact number of samples which should be read (-1 = all; if not -1, sample_ratio will not have any effect)", -1, Integer.MAX_VALUE, -1);
		type.setExpert(true);
		types.add(type);

		types.addAll(RandomGenerator.getRandomGeneratorParameters(this));

		return types;
	}
}
