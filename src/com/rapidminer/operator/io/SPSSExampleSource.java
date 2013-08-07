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
import com.rapidminer.example.table.DataRowFactory;
import com.rapidminer.example.table.MemoryExampleTable;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeBoolean;
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
 * This operator can read spss files.
 * 
 * @rapidminer.index spss
 * @author Tobias Malbrecht
 */
public class SPSSExampleSource extends BytewiseExampleSource {

	static {
		AbstractReader.registerReaderDescription(new ReaderDescription("sav", SPSSExampleSource.class, PARAMETER_FILENAME));
	}

	/** The parameter name for &quot;Determines which SPSS variable properties should be used for attribute naming.&quot; */
	public static final String PARAMETER_ATTRIBUTE_NAMING_MODE = "attribute_naming_mode";

	/** The parameter name for &quot;Use SPSS value labels as values.&quot; */
	public static final String PARAMETER_USE_VALUE_LABELS = "use_value_labels";

	/** The parameter name for &quot;Recode SPSS user missings to missing values.&quot; */
	public static final String PARAMETER_RECODE_USER_MISSINGS = "recode_user_missings";

	/** The parameter name for &quot;The fraction of the data set which should be read (1 = all; only used if sample_size = -1)&quot; */
	public static final String PARAMETER_SAMPLE_RATIO = "sample_ratio";

	/** The parameter name for &quot;The exact number of samples which should be read (-1 = all; if not -1, sample_ratio will not have any effect)&quot; */
	public static final String PARAMETER_SAMPLE_SIZE = "sample_size";

	/** File suffix for spss files. */
	private static final String SPSS_FILE_SUFFIX = "sav";

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

	/** File format constants... */
	private static final int CODE_HEADER = 0x24464C32;

	private static final int LENGTH_HEADER = 176;

	private static final int INDEX_CODE_HEADER = 0;

	private static final int INDEX_HEADER_PRODUCT_NAME = 4;

	private static final int LENGTH_HEADER_PRODUCT_NAME = 60;

	private static final int INDEX_HEADER_LAYOUT_CODE = 64;

	private static final int CODE_HEADER_LAYOUT_CODE = 2;

	private static final int INDEX_HEADER_CASE_SIZE = 68;

	private static final int INDEX_HEADER_COMPRESSED = 72;

	private static final int INDEX_HEADER_WEIGHT_INDEX = 76;

	private static final int INDEX_HEADER_NUMBER_OF_CASES = 80;

	private static final int INDEX_HEADER_BIAS = 84;

	private static final int INDEX_HEADER_DATE = 92;

	private static final int LENGTH_HEADER_DATE = 9;

	private static final int INDEX_HEADER_TIME = 101;

	private static final int LENGTH_HEADER_TIME = 8;

	private static final int INDEX_HEADER_DATASET_LABEL = 109;

	private static final int LENGTH_HEADER_DATASET_LABEL = 64;

	private static final int CODE_VARIABLE = 2;

	private static final int LENGTH_VARIABLE = 32;

	private static final int INDEX_VARIABLE_TYPE = 4;

	private static final int INDEX_VARIABLE_LABELED = 8;

	private static final int INDEX_VARIABLE_NUMBER_OF_MISSING_VALUES = 12;

	private static final int INDEX_VARIABLE_PRINT_FORMAT = 16;

	private static final int INDEX_VARIABLE_NAME = 24;

	private static final int LENGTH_VARIABLE_NAME = 8;

	private static final int FORMAT_DATE = 20;

	private static final int FORMAT_EDATE = 38;

	private static final int FORMAT_SDATE = 39;

	private static final int FORMAT_TIME = 21;

	private static final int FORMAT_DATETIME = 22;

	private static final int CODE_VALUE_LABEL = 3;

	private static final int CODE_VALUE_LABEL_VARIABLE = 4;

	private static final int CODE_DOCUMENT = 6;

	private static final int LENGTH_DOCUMENT_LINE = 80;

	private static final int CODE_INFORMATION_HEADER = 7;

	private static final int LENGTH_INFORMATION_HEADER = 12;

	private static final int INDEX_INFORMATION_HEADER_SUBTYPE = 0;

	private static final int INDEX_INFORMATION_HEADER_SIZE = 4;

	private static final int INDEX_INFORMATION_HEADER_COUNT = 8;

	private static final int CODE_INFORMATION_HEADER_SUBTYPE_MACHINE_32 = 3;

	private static final int LENGTH_INFORMATION_HEADER_SUBTYPE_MACHINE_32 = 32;

	private static final int CODE_INFORMATION_HEADER_SUBTYPE_MACHINE_64 = 4;

	private static final int LENGTH_INFORMATION_HEADER_SUBTYPE_MACHINE_64 = 24;

	private static final int CODE_INFORMATION_HEADER_AUXILIARY_VARIABLE_PARAMETERS = 11;

	private static final int LENGTH_INFORMATION_HEADER_SINGLE_AUXILIARY_VARIABLE_PARAMETERS = 12;

	private static final int CODE_LONG_VARIABLE_NAMES = 13;

	private static final int CODE_LONG_VARIABLE_NAME_RECORDS_DIVIDER = 9;

	private static final int CODE_DICTIONARY_TERMINATION = 999;

	private static final int LENGTH_COMMAND_CODE_BLOCK = 8;

	private static final int CODE_COMMAND_CODE_IGNORED = 0;

	private static final int CODE_COMMAND_CODE_EOF = 252;

	private static final int CODE_COMMAND_CODE_NOT_COMPRESSIBLE = 253;

	private static final int CODE_COMMAND_CODE_ALL_SPACES_STRING = 254;

	private static final int CODE_COMMAND_CODE_SYSTEM_MISSING = 255;

	private static final int LENGTH_VALUE_BLOCK = 8;

	private static final long GREGORIAN_CALENDAR_OFFSET_IN_MILLISECONDS = -12219379200000L;

	/** SPSS file variable header definition. */
	private static class Variable {
		private static final int TYPE_NUMERICAL = 0;

		private static final int MEASURE_NOMINAL = 1;

		private static final int MEASURE_ORDINAL = 2;

		private static final int MEASURE_CONTINUOUS = 3;

		private int type;

		private boolean labeled;

		private int printFormat;

		private int numberOfMissingValues;

		private String name;

		private String label;

		private double[] missingValues;

		private LinkedHashMap<Double, String> valueLabels;

		private int measure;

		private boolean isDateVariable() {
			return (printFormat == FORMAT_DATE) ||
			(printFormat == FORMAT_EDATE) ||
			(printFormat == FORMAT_SDATE); 
		}

		private boolean isTimeVariable() {
			return (printFormat == FORMAT_TIME);
		}

		private boolean isDateTimeVariable() {
			return (printFormat == FORMAT_DATETIME);
		}
	}

	public SPSSExampleSource(OperatorDescription description) {
		super(description);
	}

	@Override
	protected String getFileSuffix() {
		return SPSS_FILE_SUFFIX;
	}

	@Override
	protected ExampleSet readStream(InputStream inputStream, DataRowFactory dataRowFactory) throws IOException, UndefinedParameterError {
		int attributeNamingMode = getParameterAsInt(PARAMETER_ATTRIBUTE_NAMING_MODE);
		boolean useValueLabels = getParameterAsBoolean(PARAMETER_USE_VALUE_LABELS);
		boolean recodeUserMissings = getParameterAsBoolean(PARAMETER_RECODE_USER_MISSINGS);
		int sampleSize = getParameterAsInt(PARAMETER_SAMPLE_SIZE);
		double sampleRatio = getParameterAsDouble(PARAMETER_SAMPLE_RATIO);
		RandomGenerator randomGenerator = RandomGenerator.getRandomGenerator(this);

		byte[] buffer = new byte[500];
		boolean reverseEndian = false;

		// read file header
		read(inputStream, buffer, LENGTH_HEADER);
		if (extractInt(buffer, INDEX_CODE_HEADER, false) != CODE_HEADER) {
			throw new IOException(GENERIC_ERROR_MESSAGE);
		}
		String productName = extractString(buffer, INDEX_HEADER_PRODUCT_NAME, LENGTH_HEADER_PRODUCT_NAME);
		int layoutCode = extractInt(buffer, INDEX_HEADER_LAYOUT_CODE, false);
		if (layoutCode != CODE_HEADER_LAYOUT_CODE) {
			reverseEndian = true;
			layoutCode = extractInt(buffer, INDEX_HEADER_LAYOUT_CODE, reverseEndian);
			if (layoutCode != CODE_HEADER_LAYOUT_CODE) {
				throw new IOException(GENERIC_ERROR_MESSAGE);
			}
		}
		int caseSize = extractInt(buffer, INDEX_HEADER_CASE_SIZE, reverseEndian);
		boolean compressed = ((extractInt(buffer, INDEX_HEADER_COMPRESSED, reverseEndian) == 1) ? true : false);
		int weightIndex = extractInt(buffer, INDEX_HEADER_WEIGHT_INDEX, reverseEndian);
		int numberOfExamples = extractInt(buffer, INDEX_HEADER_NUMBER_OF_CASES, reverseEndian);
		double bias = extractDouble(buffer, INDEX_HEADER_BIAS, reverseEndian);
		String date = extractString(buffer, INDEX_HEADER_DATE, LENGTH_HEADER_DATE);
		String time = extractString(buffer, INDEX_HEADER_TIME, LENGTH_HEADER_TIME);
		String dataSetLabel = extractString(buffer, INDEX_HEADER_DATASET_LABEL, LENGTH_HEADER_DATASET_LABEL);

		StringBuffer logMessage = new StringBuffer("SPSSExampleSource starts reading..." + Tools.getLineSeparator());
		logMessage.append((compressed ? "" : "un") + "compressed, written by  " + productName + "  at " + time + ", " + date + Tools.getLineSeparator());
		if (dataSetLabel.equals("")) {
			logMessage.append("no file label, ");
		} else {
			logMessage.append("file label is " + dataSetLabel + Tools.getLineSeparator());
		}
		logMessage.append("contains " + numberOfExamples + " examples, case size is " + caseSize + "x8=" + caseSize * 8 + " Bytes" + Tools.getLineSeparator());
		logMessage.append("weight index is " + weightIndex + Tools.getLineSeparator());
		log(logMessage.toString());

		// read variables
		List<Variable> variables = new LinkedList<Variable>();
		LinkedHashMap<Integer, Integer> variableNrTranslations = new LinkedHashMap<Integer, Integer>();
		{
			int variableNr = 0;
			for (int i = 0; i < caseSize; i++) {
				read(inputStream, buffer, LENGTH_VARIABLE);
				if (extractInt(buffer, 0, reverseEndian) != CODE_VARIABLE) {
					throw new IOException("file corrupt (missing variable definitions)");
				}
				Variable variable = new Variable();
				variable.type = extractInt(buffer, INDEX_VARIABLE_TYPE, reverseEndian);
				variable.labeled = ((extractInt(buffer, INDEX_VARIABLE_LABELED, reverseEndian) == 1) ? true : false);
				variable.numberOfMissingValues = extractInt(buffer, INDEX_VARIABLE_NUMBER_OF_MISSING_VALUES, reverseEndian);
				variable.printFormat = (0x00FF0000 & extractInt(buffer, INDEX_VARIABLE_PRINT_FORMAT, reverseEndian)) >> 16;
			variable.name = extractString(buffer, INDEX_VARIABLE_NAME, LENGTH_VARIABLE_NAME);
			if (variable.labeled) {
				read(inputStream, buffer, LENGTH_VARIABLE, LENGTH_INT_32);
				int labelLength = extractInt(buffer, LENGTH_VARIABLE, reverseEndian);
				int adjLabelLength = labelLength;
				if (labelLength % LENGTH_INT_32 != 0) {
					adjLabelLength = labelLength + LENGTH_INT_32 - (labelLength % LENGTH_INT_32);
				}
				read(inputStream, buffer, adjLabelLength);
				variable.label = extractString(buffer, 0, labelLength);
			}
			if (variable.numberOfMissingValues != 0) {
				read(inputStream, buffer, variable.numberOfMissingValues * LENGTH_DOUBLE);
				variable.missingValues = new double[variable.numberOfMissingValues];
				for (int j = 0; j < variable.numberOfMissingValues; j++) {
					variable.missingValues[j] = extractDouble(buffer, j * LENGTH_DOUBLE, reverseEndian);
				}
			}
			if (variable.type != -1) {
				variables.add(variable);
				variableNrTranslations.put(i, variableNr);
				variableNr++;
			}
			}
		}

		// read other header records
		boolean valueLabelsRead = false;
		LinkedHashMap<Double, String> valueLabels = null;
		boolean terminated = false;
		do {
			int count = 0;
			read(inputStream, buffer, LENGTH_INT_32);
			int recordType = extractInt(buffer, 0, reverseEndian);
			switch (recordType) {
			case CODE_VALUE_LABEL:
				read(inputStream, buffer, LENGTH_INT_32);
				count = extractInt(buffer, 0, reverseEndian);
				valueLabels = new LinkedHashMap<Double, String>();
				for (int i = 0; i < count; i++) {
					read(inputStream, buffer, LENGTH_DOUBLE);
					double labelValue = extractDouble(buffer, 0, reverseEndian);
					read(inputStream, buffer, LENGTH_BYTE);
					int labelLength = buffer[0];
					int adjLabelLength = labelLength + LENGTH_DOUBLE - (labelLength % LENGTH_DOUBLE) - 1;
					read(inputStream, buffer, adjLabelLength);
					String labelLabel = extractString(buffer, 0, adjLabelLength);
					valueLabels.put(labelValue, labelLabel);
				}
				valueLabelsRead = true;
				break;
			case CODE_VALUE_LABEL_VARIABLE:
				if (!valueLabelsRead) {
					throw new IOException(GENERIC_ERROR_MESSAGE + ": value labels have not been read");
				}
				valueLabelsRead = false;
				read(inputStream, buffer, LENGTH_INT_32);
				count = extractInt(buffer, 0, reverseEndian);
				for (int i = 0; i < count; i++) {
					read(inputStream, buffer, LENGTH_INT_32);
					int variableNr = variableNrTranslations.get(extractInt(buffer, 0, reverseEndian) - 1);
					if (variableNr < variables.size()) {
						Variable variable = variables.get(variableNr);
						variable.valueLabels = valueLabels;
					}
				}
				break;
			case CODE_DOCUMENT:
				read(inputStream, buffer, LENGTH_INT_32);
				count = extractInt(buffer, 0, reverseEndian);
				for (int i = 0; i < count; i++) {
					read(inputStream, buffer, LENGTH_DOCUMENT_LINE);
				}
				break;
			case CODE_INFORMATION_HEADER:
				read(inputStream, buffer, 0, LENGTH_INFORMATION_HEADER);
				int subType = extractInt(buffer, INDEX_INFORMATION_HEADER_SUBTYPE, reverseEndian);
				int size = extractInt(buffer, INDEX_INFORMATION_HEADER_SIZE, reverseEndian);
				count = extractInt(buffer, INDEX_INFORMATION_HEADER_COUNT, reverseEndian);
				switch (subType) {
				case CODE_INFORMATION_HEADER_SUBTYPE_MACHINE_32:
					read(inputStream, buffer, LENGTH_INFORMATION_HEADER_SUBTYPE_MACHINE_32);
					break;
				case CODE_INFORMATION_HEADER_SUBTYPE_MACHINE_64:
					read(inputStream, buffer, LENGTH_INFORMATION_HEADER_SUBTYPE_MACHINE_64);
					break;
				case CODE_INFORMATION_HEADER_AUXILIARY_VARIABLE_PARAMETERS:
					for (int i = 0; i < variables.size(); i++) {
						read(inputStream, buffer, LENGTH_INFORMATION_HEADER_SINGLE_AUXILIARY_VARIABLE_PARAMETERS);
						Variable variable = variables.get(i);
						variable.measure = extractInt(buffer, 0, reverseEndian);
					}
					break;
				case CODE_LONG_VARIABLE_NAMES:
					buffer = new byte[count * size];
					read(inputStream, buffer, count * size);
					String longVariableNamesString = new String(buffer);
					String[] longVariableNamePairs = longVariableNamesString.split(new String(new char[] { (byte) CODE_LONG_VARIABLE_NAME_RECORDS_DIVIDER }));
					for (int i = 0; i < longVariableNamePairs.length; i++) {
						String[] keyLongVariablePair = longVariableNamePairs[i].split("=");
						if (keyLongVariablePair.length != 2) {
							continue;
						}
						for (Variable variable : variables) {
							if (variable.name.equals(keyLongVariablePair[0])) {
								variable.name = keyLongVariablePair[1];
							}
						}
					}
					buffer = new byte[500];
					break;
				default:
					buffer = new byte[count * size];
				read(inputStream, buffer, count * size);
				buffer = new byte[500];
				break;
				}
				break;
			case CODE_DICTIONARY_TERMINATION:
				read(inputStream, buffer, LENGTH_INT_32);
				terminated = true;
				break;
			default:
				break;
			}
		} while (!terminated);

		// create attributes from variables
		AttributeSet attributeSet = new AttributeSet();
		Attribute attribute = null;
		for (int i = 0; i < variables.size(); i++) {
			Variable variable = variables.get(i);
			String attributeName = null;
			if (variable.label == null) {
				variable.label = variable.name;
			}
			switch (attributeNamingMode) {
			case USE_VAR_NAME:
				attributeName = variable.name;
				break;
			case USE_VAR_LABEL:
				attributeName = variable.label;
				break;
			case USE_VAR_NAME_LABELED:
				attributeName = variable.name + " (" + variable.label + ")";
				break;
			case USE_VAR_LABEL_NAMED:
				attributeName = variable.label + " (" + variable.name + ")";
				break;
			default:
				attributeName = variable.name;
			}
			if (variable.type == Variable.TYPE_NUMERICAL) {
				// TODO: check completeness of date variable types
				if (variable.isDateVariable()) {
					attribute = AttributeFactory.createAttribute(attributeName, Ontology.DATE);
				} else if (variable.isTimeVariable()) {
					attribute = AttributeFactory.createAttribute(attributeName, Ontology.TIME);
				} else if (variable.isDateTimeVariable()) {
					attribute = AttributeFactory.createAttribute(attributeName, Ontology.DATE_TIME);
				} else {
					switch (variable.measure) {
					case Variable.MEASURE_NOMINAL:
						attribute = AttributeFactory.createAttribute(attributeName, Ontology.NOMINAL);
						break;
					case Variable.MEASURE_ORDINAL:
						attribute = AttributeFactory.createAttribute(attributeName, Ontology.NOMINAL);
						break;
					case Variable.MEASURE_CONTINUOUS:
						attribute = AttributeFactory.createAttribute(attributeName, Ontology.NUMERICAL);
						break;
					default:
						if (useValueLabels && variable.valueLabels != null) {
							attribute = AttributeFactory.createAttribute(attributeName, Ontology.NOMINAL);	                		
						} else {
							attribute = AttributeFactory.createAttribute(attributeName, Ontology.NUMERICAL);
						}
					}
				}
			} else {
				attribute = AttributeFactory.createAttribute(attributeName, Ontology.STRING);
			}

			// map strings to values for nominal attributes
			if (attribute.isNominal()) {
				if (variable.valueLabels != null) {
					Iterator<Double> iterator = variable.valueLabels.keySet().iterator();
					while (iterator.hasNext()) {
						Double numericValue = iterator.next();
						boolean missing = false;
						if (recodeUserMissings) {
							for (int j = 0; j < variable.numberOfMissingValues; j++) {
								if (numericValue == variable.missingValues[j]) {
									missing = true;
									break;
								}
							}
						}
						if (!missing) {
							if (useValueLabels) {
								attribute.getMapping().mapString(variable.valueLabels.get(numericValue));
							} else {
								attribute.getMapping().mapString(java.lang.Double.toString(numericValue));
							}
						}
					}
				}
			}
			attributeSet.addAttribute(attribute);
		}

		// initialize sampling functionality
		OrderedSamplingWithoutReplacement sampling = null;
		if (sampleSize != -1) {
			sampling = new OrderedSamplingWithoutReplacement(randomGenerator, numberOfExamples, sampleSize);
		} else {
			sampling = new OrderedSamplingWithoutReplacement(randomGenerator, numberOfExamples, sampleRatio);
		}

		// read data
		Attribute weight = weightIndex == 0 ? null : attributeSet.getAttribute(variableNrTranslations.get(weightIndex - 1));
		MemoryExampleTable table = new MemoryExampleTable(attributeSet.getAllAttributes());
		int commandCodeCounter = 0;
		int bytesRead = 0;
		for (int i = 0; i < numberOfExamples; i++) {
			String[] values = new String[variables.size()];
			if (!compressed) {
				for (int j = 0; j < variables.size(); j++) {
					read(inputStream, buffer, LENGTH_DOUBLE);
					values[j] = Double.toString(extractDouble(buffer, 0, reverseEndian));
				}
			} else {
				for (int j = 0; j < variables.size(); j++) {
					boolean readValue = false;
					String value = null;
					Variable variable = variables.get(j);
					for (;;) {
						if (commandCodeCounter % LENGTH_COMMAND_CODE_BLOCK == 0) {
							commandCodeCounter = 0;
							bytesRead = read(inputStream, buffer, 0, LENGTH_COMMAND_CODE_BLOCK);
							if (bytesRead == -1) {
								break;
							}
						}
						int commandCode = 0x000000FF & buffer[commandCodeCounter];
						switch (commandCode) {
						case CODE_COMMAND_CODE_IGNORED:
							break;
						case CODE_COMMAND_CODE_EOF:
							// clear remaining command buffer for safety
							for (int k = commandCodeCounter + 1; k < LENGTH_COMMAND_CODE_BLOCK; k++) {
								buffer[k] = (byte) 0;
							}
							break;
						case CODE_COMMAND_CODE_NOT_COMPRESSIBLE:
							bytesRead = read(inputStream, buffer, LENGTH_COMMAND_CODE_BLOCK, LENGTH_VALUE_BLOCK);
							if (bytesRead == -1) {
								throw new IOException("file corrupt (data inconsistency)");
							}
							if (variable.type == 0) {
								double numericValue = extractDouble(buffer, LENGTH_COMMAND_CODE_BLOCK, reverseEndian);
								if (variable.isDateVariable() || variable.isTimeVariable() || variable.isDateTimeVariable()) {
									numericValue = (long) numericValue * 1000 + GREGORIAN_CALENDAR_OFFSET_IN_MILLISECONDS;
								}
								value = java.lang.Double.toString(numericValue);
								if (variable.measure != Variable.MEASURE_CONTINUOUS) {
									if (useValueLabels) {
										if (variable.valueLabels != null) {
											String label = variable.valueLabels.get(numericValue);
											value = label;
										}
									}
								}
								if (recodeUserMissings) {
									for (int k = 0; k < variable.numberOfMissingValues; k++) {
										if (Tools.isEqual(numericValue, variable.missingValues[k])) {
											value = null;
										}
									}
								}
								readValue = true;
							} else {
								if (value == null) {
									value = new String(buffer, LENGTH_COMMAND_CODE_BLOCK, LENGTH_VALUE_BLOCK);
								} else {
									value = value + new String(buffer, LENGTH_COMMAND_CODE_BLOCK, LENGTH_VALUE_BLOCK);
								}
								if (value.length() >= variables.get(j).type) {
									value = value.trim();
									readValue = true;
								}
							}
							break;
						case CODE_COMMAND_CODE_ALL_SPACES_STRING:
							value = value == null ? String.valueOf("        ") : value.concat(String.valueOf("        "));
							if (value.length() >= variables.get(j).type) {
								value = value.trim();
								readValue = true;
							}
							break;
						case CODE_COMMAND_CODE_SYSTEM_MISSING:
							value = null;
							readValue = true;
							break;
						default:
							double numericValue = commandCode - bias;
						value = java.lang.Double.toString(numericValue);
						if (variable.measure != Variable.MEASURE_CONTINUOUS) {
							if (useValueLabels) {
								if (variable.valueLabels != null) {
									String label = variable.valueLabels.get(numericValue);
									value = label;
								}
							}
						}
						if (recodeUserMissings) {
							for (int k = 0; k < variable.numberOfMissingValues; k++) {
								if (Tools.isEqual(numericValue, variable.missingValues[k])) {
									value = null;
								}
							}
						}
						readValue = true;
						break;
						}
						commandCodeCounter++;
						if (readValue) {
							values[j] = value;
							break;
						}
					}
				}        		
			}
			// add data to table
			if (sampling == null) {
				table.addDataRow(dataRowFactory.create(values, table.getAttributes()));				
			} else {
				if (sampling.acceptElement()) {
					table.addDataRow(dataRowFactory.create(values, table.getAttributes()));
				}
			}
		}
		inputStream.close();

		ExampleSet exampleSet = table.createExampleSet();
		exampleSet.getAttributes().setWeight(weight);
		return exampleSet;
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();
		ParameterType type = new ParameterTypeCategory(PARAMETER_ATTRIBUTE_NAMING_MODE, "Determines which SPSS variable properties should be used for attribute naming.", ATTRIBUTE_NAMING_MODES, USE_VAR_NAME);
		type.setExpert(false);
		types.add(type);
		type = new ParameterTypeBoolean(PARAMETER_USE_VALUE_LABELS, "Use SPSS value labels as values.", true);
		type.setExpert(false);
		types.add(type);
		type = new ParameterTypeBoolean(PARAMETER_RECODE_USER_MISSINGS, "Recode SPSS user missings to missing values.", true);
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
