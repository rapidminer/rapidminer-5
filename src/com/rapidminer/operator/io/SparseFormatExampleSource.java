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

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.Attributes;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.table.AttributeFactory;
import com.rapidminer.example.table.DataRowFactory;
import com.rapidminer.example.table.MemoryExampleTable;
import com.rapidminer.example.table.SparseFormatDataRowReader;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.UserError;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeBoolean;
import com.rapidminer.parameter.ParameterTypeCategory;
import com.rapidminer.parameter.ParameterTypeChar;
import com.rapidminer.parameter.ParameterTypeFile;
import com.rapidminer.parameter.ParameterTypeInt;
import com.rapidminer.parameter.ParameterTypeList;
import com.rapidminer.parameter.ParameterTypeString;
import com.rapidminer.parameter.ParameterTypeStringCategory;
import com.rapidminer.parameter.conditions.BooleanParameterCondition;
import com.rapidminer.tools.Ontology;
import com.rapidminer.tools.Tools;
import com.rapidminer.tools.att.AttributeSet;
import com.rapidminer.tools.io.Encoding;


/**
 * Reads an example file in sparse format, i.e. lines have the form<br/>
 * <center>
 * 
 * <pre>
 * label index:value index:value index:value...
 * </pre>
 * 
 * </center><br/> Index may be an integer (starting with 1) for the regular
 * attributes or one of the prefixes specified by the parameter list
 * <code>prefix_map</code>. Four possible <code>format</code>s are
 * supported
 * <dl>
 * <dt>format_xy:</dt>
 * <dd>The label is the last token in each line</dd>
 * <dt>format_yx:</dt>
 * <dd>The label is the first token in each line</dd>
 * <dt>format_prefix:</dt>
 * <dd>The label is prefixed by 'l:'</dd>
 * <dt>format_separate_file:</dt>
 * <dd>The label is read from a separate file specified by
 * <code>label_file</code></dd>
 * <dt>no_label:</dt>
 * <dd>The example set is unlabeled.</dd>
 * </dl>
 * A detailed introduction to the sparse file format is given in section
 * {@rapidminer.ref sec:sparse_format|First steps/File formats/Data files}.
 * 
 * @see SparseFormatDataRowReader
 * 
 * @author Ingo Mierswa, Simon Fischer
 */
public class SparseFormatExampleSource extends AbstractExampleSource {

	/** The parameter name for &quot;Format of the sparse data file.&quot; */
	public static final String PARAMETER_FORMAT = "format";

	/** The parameter name for &quot;Name of the attribute description file.&quot; */
	public static final String PARAMETER_ATTRIBUTE_DESCRIPTION_FILE = "attribute_description_file";

	/** The parameter name for &quot;Name of the data file. Only necessary if not specified in the attribute description file.&quot; */
	public static final String PARAMETER_DATA_FILE = "data_file";

	/** The parameter name for &quot;Name of the data file containing the labels. Only necessary if format is 'format_separate_file'.&quot; */
	public static final String PARAMETER_LABEL_FILE = "label_file";

	/** The parameter name for &quot;Dimension of the example space. Only necessary if parameter 'attribute_description_file' is not set.&quot; */
	public static final String PARAMETER_DIMENSION = "dimension";

	/** The parameter name for &quot;The maximum number of examples to read from the data files (-1 = all)&quot; */
	public static final String PARAMETER_SAMPLE_SIZE = "sample_size";

	/** The parameter name for &quot;Determines, how the data is represented internally.&quot; */
	public static final String PARAMETER_DATAMANAGEMENT = "datamanagement";

	/** The parameter name for &quot;Character that is used as decimal point.&quot; */
	public static final String PARAMETER_DECIMAL_POINT_CHARACTER = "decimal_point_character";

	/** The parameter name for &quot;Maps prefixes to names of special attributes.&quot; */
	public static final String PARAMETER_PREFIX_MAP = "prefix_map";
	

	/**  Determines whether nominal values are surrounded by quotes or not. If <code>PARAMETER_USE_QUOTES == true</code> the first and last character of the nominal values are ignored. */
	public static final String PARAMETER_USE_QUOTES = "use_quotes";
	
	/**   The char that is used to surround nominal values. */
	public static final String PARAMETER_QUOTES_CHARACTER = "quotes_character";

	public SparseFormatExampleSource(OperatorDescription description) {
		super(description);
	}

	@Override
	public ExampleSet createExampleSet() throws OperatorException {

		int format = getParameterAsInt(PARAMETER_FORMAT);

		// +++++++++ special attribute prefix map +++++++++++++++
		Map<String, String> prefixMap = new HashMap<String, String>();
		Iterator<String[]> p = getParameterList(PARAMETER_PREFIX_MAP).iterator();
		while (p.hasNext()) {
			String[] prefixMapping = p.next();
			prefixMap.put(prefixMapping[0], prefixMapping[1]);
		}

		// +++++++++ attribute creation +++++++++++++++++++++++++
		File dataFile = getParameterAsFile(PARAMETER_DATA_FILE);
		File attributeDescriptionFile = getParameterAsFile(PARAMETER_ATTRIBUTE_DESCRIPTION_FILE);
		AttributeSet attributeSet = null;
		if (attributeDescriptionFile != null) {
			try {
				attributeSet = new AttributeSet(attributeDescriptionFile, false, this);
			} catch (Throwable e) {
				throw new UserError(this, e, 302, new Object[] { attributeDescriptionFile, e.getMessage() });
			}
			if ((dataFile != null) && (attributeSet.getDefaultSource() != null) && (!dataFile.equals(attributeSet.getDefaultSource()))) {
				logWarning("Attribute file names specified by parameter 'data_file' and default_source specified in '" + attributeDescriptionFile + "' do not match! Assuming the latter to be correct.");
			}
			if ((format != SparseFormatDataRowReader.FORMAT_NO_LABEL) && (attributeSet.getSpecialAttribute("label") == null)) {
				throw new UserError(this, 917, new Object[0]);
			}

			log("Found " + attributeSet.getNumberOfRegularAttributes() + " regular attributes.");
			dataFile = attributeSet.getDefaultSource();
		} else {
			int dimension = getParameterAsInt(PARAMETER_DIMENSION);
			if (dimension < 0) 
				throw new UserError(this, 921);
			attributeSet = new AttributeSet(dimension);
			for (int i = 0; i < dimension; i++) {
				Attribute attribute = AttributeFactory.createAttribute(Ontology.REAL);
				attributeSet.addAttribute(attribute);
			}
			Iterator<String> m = prefixMap.values().iterator();
			while (m.hasNext()) {
				String specialName = m.next();
				attributeSet.setSpecialAttribute(specialName, AttributeFactory.createAttribute(Ontology.REAL));
			}
			if (format != SparseFormatDataRowReader.FORMAT_NO_LABEL) {
				attributeSet.setSpecialAttribute("label", AttributeFactory.createAttribute(Ontology.NOMINAL));
			}
		}

		if (dataFile == null) {
			throw new UserError(this, 902, new Object[0]);
		}

		// +++++++++++++ reader +++++++++++++++++++++++++++++++++
		Reader inData = null;
		Reader inLabels = null;
		try {
			inData = Tools.getReader(dataFile, Encoding.getEncoding(this));
		} catch (IOException e) {
			throw new UserError(this, e, 302, new Object[] { dataFile, e.getMessage() });
		}
		File labelFile = null;
		if (format == SparseFormatDataRowReader.FORMAT_SEPARATE_FILE) {
			labelFile = getParameterAsFile(PARAMETER_LABEL_FILE);
			if (labelFile == null) {
				throw new UserError(this, 201, new Object[] { "format", SparseFormatDataRowReader.FORMAT_NAMES[SparseFormatDataRowReader.FORMAT_SEPARATE_FILE], "label_file" });
			}
			try {
				inLabels = Tools.getReader(labelFile, Encoding.getEncoding(this));
			} catch (IOException e) {
				throw new UserError(this, e, 302, new Object[] { labelFile, e.getMessage() });
			}
		}

		MemoryExampleTable table = new MemoryExampleTable(attributeSet.getAllAttributes());
		SparseFormatDataRowReader reader = new SparseFormatDataRowReader(new DataRowFactory(getParameterAsInt(PARAMETER_DATAMANAGEMENT), getParameterAsString(PARAMETER_DECIMAL_POINT_CHARACTER).charAt(0)), format, prefixMap, attributeSet, inData, inLabels, getParameterAsInt(PARAMETER_SAMPLE_SIZE), getParameterAsBoolean(PARAMETER_USE_QUOTES), getParameterAsChar(PARAMETER_QUOTES_CHARACTER));
		table.readExamples(reader);
		ExampleSet exampleSet = table.createExampleSet(attributeSet);
		return exampleSet;
	}

	@Override
	protected boolean supportsEncoding() {
		return true;
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = new LinkedList<ParameterType>();
		ParameterType type = new ParameterTypeCategory(PARAMETER_FORMAT, "Format of the sparse data file.", SparseFormatDataRowReader.FORMAT_NAMES, 0);
		type.setExpert(false);
		types.add(type);
		type = new ParameterTypeFile(PARAMETER_ATTRIBUTE_DESCRIPTION_FILE, "Name of the attribute description file.", "aml", true);
		type.setExpert(false);
		types.add(type);
		types.add(new ParameterTypeFile(PARAMETER_DATA_FILE, "Name of the data file. Only necessary if not specified in the attribute description file.", null, true));
		types.add(new ParameterTypeFile(PARAMETER_LABEL_FILE, "Name of the data file containing the labels. Only necessary if format is 'format_separate_file'.", null, true));
		types.add(new ParameterTypeInt(PARAMETER_DIMENSION, "Dimension of the example space. Only necessary if parameter 'attribute_description_file' is not set.", -1, Integer.MAX_VALUE, -1));
		types.add(new ParameterTypeInt(PARAMETER_SAMPLE_SIZE, "The maximum number of examples to read from the data files (-1 = all)", -1, Integer.MAX_VALUE, -1));
		
		types.add(new ParameterTypeBoolean(PARAMETER_USE_QUOTES, "Indicates if quotes should be regarded.", true));
		type = new ParameterTypeChar(PARAMETER_QUOTES_CHARACTER, "The quotes character.", '"', true);
		type.registerDependencyCondition(new BooleanParameterCondition(this, PARAMETER_USE_QUOTES, false, true));
		types.add(type);
		
		types.add(new ParameterTypeCategory(PARAMETER_DATAMANAGEMENT, "Determines, how the data is represented internally.", DataRowFactory.TYPE_NAMES, DataRowFactory.TYPE_DOUBLE_ARRAY));
		types.add(new ParameterTypeString(PARAMETER_DECIMAL_POINT_CHARACTER, "Character that is used as decimal point.", "."));
		types.add(new ParameterTypeList(PARAMETER_PREFIX_MAP, "Maps prefixes to names of special attributes.", 
				new ParameterTypeString("prefix", "The prefix which represents a special attribute"),
				new ParameterTypeStringCategory("special_attribute", "Maps prefixes to names of special attributes.", Attributes.KNOWN_ATTRIBUTE_TYPES)));
		types.addAll(super.getParameterTypes());
		return types;
	}

}
