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
package com.rapidminer.operator.nio.xml;

import java.text.NumberFormat;
import java.util.LinkedList;
import java.util.List;

import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.OperatorVersion;
import com.rapidminer.operator.nio.model.AbstractDataResultSetReader;
import com.rapidminer.operator.nio.model.DataResultSetFactory;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeBoolean;
import com.rapidminer.parameter.ParameterTypeConfiguration;
import com.rapidminer.parameter.ParameterTypeEnumeration;
import com.rapidminer.parameter.ParameterTypeList;
import com.rapidminer.parameter.ParameterTypeString;
import com.rapidminer.parameter.conditions.BooleanParameterCondition;
import com.rapidminer.tools.DateParser;
import com.rapidminer.tools.StrictDecimalFormat;

/**
 * This is an operator for reading XML files. It will first parse the DOM tree of
 * the given XML file and then apply an XPath expression whose matches will become
 * a single example.
 * For each defined attribute another XPath expression is evaluated over all of the
 * matches to fill the values of each example.
 * 
 * @author Sebastian Land
 */
public class XMLExampleSource extends AbstractDataResultSetReader {

    public static final String PARAMETER_FILE = "file";

    public static final String PARAMETER_XPATH_FOR_EXAMPLES = "xpath_for_examples";

    public static final String PARAMETER_XPATHS_FOR_ATTRIBUTES = "xpaths_for_attributes";
    public static final String PARAMETER_XPATH_FOR_ATTRIBUTE = "xpath_for_attribute";

    public static final String PARAMETER_USE_NAMESPACES = "use_namespaces";
    public static final String PARAMETER_USE_DEFAULT_NAMESPACE = "use_default_namespace";
    public static final String PARAMETER_DEFAULT_NAMESPACE = "default_namespace";
    public static final String PARAMETER_NAMESPACES = "namespaces";
    public static final String PARAMETER_NAMESPACE = "namespace";
    public static final String PARAMETER_NAMESPACE_ID = "id";

	/**
	 * After this version the whole element including xml tags is inserted into the exmample set, if an
	 * XPath for an attribute matches a whole element instead of the text() tag. Also if multiple elements are
	 * matched, all of them are added to the output example set.
	 */
	public static final OperatorVersion CHANGE_5_1_013_NODE_OUTPUT = new OperatorVersion(5, 1, 13);
    
    public XMLExampleSource(OperatorDescription description) {
        super(description);
    }

    @Override
    protected DataResultSetFactory getDataResultSetFactory() throws OperatorException {
        return new XMLResultSetConfiguration(this);
    }

    @Override
    protected NumberFormat getNumberFormat() throws OperatorException {
        return StrictDecimalFormat.getInstance(this, true);
    }

    @Override
    protected boolean isSupportingFirstRowAsNames() {
        return false;
    }

	@Override
	protected String getFileParameterName() {
		return PARAMETER_FILE;
	}

	@Override
	protected String getFileExtension() {
		return "xml";
	}

    @Override
    public List<ParameterType> getParameterTypes() {
        List<ParameterType> types = new LinkedList<ParameterType>();

        ParameterType type = new ParameterTypeConfiguration(XMLExampleSourceConfigurationWizardCreator.class, this);
        type.setExpert(false);
        types.add(type);

        types.add(makeFileParameterType());
        //types.add(new ParameterTypeFile(PARAMETER_FILE, "This specifies the xml file to load. This can either be a file in the local file system or an accessible URL.", ".xml", false, false));
        
        types.add(new ParameterTypeString(PARAMETER_XPATH_FOR_EXAMPLES, "The matches of this XPath Expression will form the examples. Each match becomes one example whose attribute values are extracted from the matching part of the xml file.", false));

        types.add(new ParameterTypeEnumeration(PARAMETER_XPATHS_FOR_ATTRIBUTES, "This XPaths expressions will be evaluated for each match to the XPath expression for examples to derive values for attributes. Each expression forms one attribute in the resulting ExampleSet.",
                new ParameterTypeString(PARAMETER_XPATH_FOR_ATTRIBUTE, "This XPath expression will be evaluated agains each match to the XPath expression for examples to derive values for this attribute. Each line in this list forms one attribute in the resulting ExampleSet."), false));

        types.add(new ParameterTypeBoolean(PARAMETER_USE_NAMESPACES, "If not checked namespaces in the XML document will be completely ignored. This might make formulating XPath expressions easier, but elements with the same name might collide if separated by namespace.", true));
        type = new ParameterTypeList(PARAMETER_NAMESPACES, "Specifies pairs of identifier and namespace for use in XPath queries. The namespace for (x)html is bound automatically to the identifier h.",
                new ParameterTypeString(PARAMETER_NAMESPACE_ID, "The id of this namespace. With this id the namespace can be referred to in the XPath expression."),
                new ParameterTypeString(PARAMETER_NAMESPACE, "The namespace to which the id should be bound.", false));
        type.registerDependencyCondition(new BooleanParameterCondition(this, PARAMETER_USE_NAMESPACES, false, true));
        types.add(type);
        type = new ParameterTypeBoolean(PARAMETER_USE_DEFAULT_NAMESPACE, "If checkedyou can specify an namespace uri that will be used when no namespace is specified in the XPath expression.", true);
        type.registerDependencyCondition(new BooleanParameterCondition(this, PARAMETER_USE_NAMESPACES, false, true));
        types.add(type);
        type = new ParameterTypeString(PARAMETER_DEFAULT_NAMESPACE, "This is the default namespace that will be assumed for all elements in the XPath expression that have no explict namespace mentioned.", true);
        type.registerDependencyCondition(new BooleanParameterCondition(this, PARAMETER_USE_DEFAULT_NAMESPACE, false, true));
        types.add(type);

        // Numberformats
        types.addAll(StrictDecimalFormat.getParameterTypes(this, true));
        types.addAll(DateParser.getParameterTypes(this));

        types.addAll(super.getParameterTypes());
        return types;
    }
    
    @Override
    public OperatorVersion[] getIncompatibleVersionChanges() {
    	return new OperatorVersion[] {CHANGE_5_1_013_NODE_OUTPUT};
    }
}
