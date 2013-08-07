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

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import javax.xml.transform.TransformerException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.OperatorVersion;
import com.rapidminer.operator.UserError;
import com.rapidminer.operator.nio.model.DataResultSet;
import com.rapidminer.operator.nio.model.ParseException;
import com.rapidminer.operator.nio.model.ParsingError;
import com.rapidminer.tools.Ontology;
import com.rapidminer.tools.ProgressListener;
import com.rapidminer.tools.xml.MapBasedNamespaceContext;

/**
 * 
 * @author Sebastian Land
 */
public class XMLResultSet implements DataResultSet {

    private NodeList exampleNodes = null;
    private XPathExpression[] attributeExpressions = null;
    private String[] attributeNames = null;
    private int[] attributeValueTypes = null;

    private int currentExampleIndex = -1;
    private String[] currentExampleValues = null;
	private OperatorVersion operatorVersion;

    /**
     * The constructor to build an ExcelResultSet from the given configuration. The calling operator might be null. It
     * is only needed for error handling.
     */
    public XMLResultSet(Operator callingOperator, XMLResultSetConfiguration configuration, OperatorVersion operatorVersion) throws OperatorException {
        // creating XPath environment
        XPathFactory factory = XPathFactory.newInstance();
        XPath xpath = factory.newXPath();
        this.operatorVersion = operatorVersion;

        final Map<String, String> namespacesMap = configuration.getNamespacesMap();
        xpath.setNamespaceContext(new MapBasedNamespaceContext(namespacesMap, configuration.getDefaultNamespaceURI()));

        // generating Example's expression
        XPathExpression exampleExpression = null;
        try {
        	String exampleXPath = configuration.getExampleXPath();
        	if (exampleXPath == null) {
        		throw new UserError(callingOperator, 217, XMLExampleSource.PARAMETER_XPATH_FOR_EXAMPLES, callingOperator.getName(), "");
        	}
            exampleExpression = xpath.compile(exampleXPath);
        } catch (XPathExpressionException e1) {
        	throw new UserError(null, 214, configuration.getExampleXPath());
        }

        // generating Attribute's expressions
        int i = 0;
        List<String> attributeXPathsList = configuration.getAttributeXPaths();
        attributeExpressions = new XPathExpression[attributeXPathsList.size()];
        attributeNames = new String[attributeXPathsList.size()];
        for (String expressionString : attributeXPathsList) {
            attributeNames[i] = expressionString;
            try {
                attributeExpressions[i] = xpath.compile(expressionString);
            } catch (XPathExpressionException e) {
            	throw new UserError(null, 214, expressionString);
            }
            i++;
        }
        attributeValueTypes = new int[attributeXPathsList.size()];
        Arrays.fill(attributeValueTypes, Ontology.NOMINAL);
        currentExampleValues = new String[attributeXPathsList.size()];

        try {
            exampleNodes = (NodeList) exampleExpression.evaluate(configuration.getDocumentObjectModel(), XPathConstants.NODESET);
        } catch (UserError e) {
        	e.setOperator(callingOperator);
        	throw e;
        } catch (XPathExpressionException e) {
        	throw new UserError(callingOperator, 214, configuration.getExampleXPath());
        }
    }

    @Override
    public boolean hasNext() {
        return exampleNodes.getLength() > currentExampleIndex + 1;
    }

    @Override
    public void next(ProgressListener listener) throws OperatorException {
        currentExampleIndex++;
        if (currentExampleIndex >= exampleNodes.getLength()) {
            throw new NoSuchElementException("No further match to examples XPath expression in XML file. Accessed " + currentExampleIndex + " but has has " + exampleNodes.getLength());
        }

        for (int i = 0; i < attributeExpressions.length; i++) {
            try {
            	Node item = exampleNodes.item(currentExampleIndex);
				if (operatorVersion.compareTo(XMLExampleSource.CHANGE_5_1_013_NODE_OUTPUT) > 0) {
	                NodeList nodeList = (NodeList)attributeExpressions[i].evaluate(item, XPathConstants.NODESET);
					currentExampleValues[i] = XMLDomHelper.nodeListToString(nodeList);
            	} else {
					currentExampleValues[i] = (String)attributeExpressions[i].evaluate(item, XPathConstants.STRING);
            	}
            } catch (XPathExpressionException e) {
                currentExampleValues[i] = null;
            } catch (TransformerException e) {
                currentExampleValues[i] = null;
			}
        }
    }

    @Override
    public int getNumberOfColumns() {
        return attributeNames.length;
    }

    @Override
    public String[] getColumnNames() {
        return attributeNames;
    }

    @Override
    public boolean isMissing(int columnIndex) {
        return currentExampleValues[columnIndex] == null;
    }

    @Override
    /**
     * This method is not supported by the XML result set. Anytime it is called a ParseException will be thrown.
     */
    public Number getNumber(int columnIndex) throws ParseException {
        throw new ParseException(new ParsingError(currentExampleIndex, columnIndex, ParsingError.ErrorCode.UNPARSEABLE_REAL, ""));
    }

    @Override
    public String getString(int columnIndex) throws ParseException {
        return currentExampleValues[columnIndex];
    }

    @Override
    /**
     * This method is not supported by the XML result set. Anytime it is called a ParseException will be thrown.
     */
    public Date getDate(int columnIndex) throws ParseException {
        throw new ParseException(new ParsingError(currentExampleIndex, columnIndex, ParsingError.ErrorCode.UNPARSEABLE_DATE, ""));
    }

    @Override
    public ValueType getNativeValueType(int columnIndex) throws ParseException {
        return ValueType.STRING;
    }

    @Override
    public void close() throws OperatorException {
        // Nothing to close: inputstream to File or URL has already be closed in constructor
    }

    @Override
    public void reset(ProgressListener listener) throws OperatorException {
        currentExampleIndex = -1;
    }

    @Override
    public int[] getValueTypes() {
        return attributeValueTypes;
    }

    @Override
    public int getCurrentRow() {
        return currentExampleIndex;
    }

}
