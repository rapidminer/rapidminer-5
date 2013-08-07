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

import static com.rapidminer.operator.nio.xml.XMLExampleSource.PARAMETER_DEFAULT_NAMESPACE;
import static com.rapidminer.operator.nio.xml.XMLExampleSource.PARAMETER_FILE;
import static com.rapidminer.operator.nio.xml.XMLExampleSource.PARAMETER_NAMESPACES;
import static com.rapidminer.operator.nio.xml.XMLExampleSource.PARAMETER_USE_DEFAULT_NAMESPACE;
import static com.rapidminer.operator.nio.xml.XMLExampleSource.PARAMETER_USE_NAMESPACES;
import static com.rapidminer.operator.nio.xml.XMLExampleSource.PARAMETER_XPATHS_FOR_ATTRIBUTES;
import static com.rapidminer.operator.nio.xml.XMLExampleSource.PARAMETER_XPATH_FOR_EXAMPLES;

import java.io.CharConversionException;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;

import javax.swing.table.TableModel;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import com.rapidminer.RapidMiner;
import com.rapidminer.gui.tools.VersionNumber;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.OperatorVersion;
import com.rapidminer.operator.UserError;
import com.rapidminer.operator.nio.model.AbstractDataResultSetReader;
import com.rapidminer.operator.nio.model.DataResultSet;
import com.rapidminer.operator.nio.model.DataResultSetFactory;
import com.rapidminer.operator.nio.model.DefaultPreview;
import com.rapidminer.operator.nio.model.ParseException;
import com.rapidminer.operator.ports.metadata.ExampleSetMetaData;
import com.rapidminer.parameter.ParameterTypeEnumeration;
import com.rapidminer.parameter.ParameterTypeList;
import com.rapidminer.tools.I18N;
import com.rapidminer.tools.LogService;
import com.rapidminer.tools.ProgressListener;

/**
 * This is the {@link DataResultSetFactory} for the XML Import. It is able
 * to read the parameters stored in the operator to create a {@link DataResultSet} accordingly.
 * 
 * @author Sebastian Land, Marius Helf
 */
public class XMLResultSetConfiguration implements DataResultSetFactory {

    private String fileName;
    
    /**
     * Maps ids to namespaces.
     */
    private Map<String, String> namespaceMap;
    private String exampleXPath;
    private List<String> attributeXPaths;
    private boolean isNamespaceAware;
    private String defaultNamespaceURI;

	private Document prefetchedDocument;
	private OperatorVersion xmlExampleSourceCompatibilityVersion;

    /**
     * This creates a completely empty configuration
     */
    public XMLResultSetConfiguration() {
        namespaceMap = new HashMap<String, String>();
        VersionNumber rmVersion = RapidMiner.getVersion();
    	this.xmlExampleSourceCompatibilityVersion = new OperatorVersion(rmVersion.getMajorNumber(), rmVersion.getMinorNumber(), rmVersion.getPatchLevel());
    }

    public void setDefaultNamespaceURI(String defaultNamespaceURI) {
    	this.defaultNamespaceURI = defaultNamespaceURI;
    }
    
    
    /**
     * This constructor will read all the needed parameters from the given operator.
     */
    public XMLResultSetConfiguration(XMLExampleSource operator) throws OperatorException {
        this();
        
    	VersionNumber rmVersion = RapidMiner.getVersion();
    	if (operator instanceof XMLExampleSource) {
    		XMLExampleSource xmlExampleSource = (XMLExampleSource)operator; 
    		xmlExampleSourceCompatibilityVersion = xmlExampleSource.getCompatibilityLevel();
    	} else {
    		xmlExampleSourceCompatibilityVersion = new OperatorVersion(rmVersion.getMajorNumber(), rmVersion.getMinorNumber(), rmVersion.getPatchLevel());
    	}


//        if (operator.isParameterSet(PARAMETER_FILE))
//            fileName = operator.getParameterAsString(PARAMETER_FILE);
        if (operator.isFileSpecified())
            fileName = operator.getSelectedFile().getAbsolutePath();

        if (operator.isParameterSet(PARAMETER_XPATH_FOR_EXAMPLES))
            exampleXPath = operator.getParameterAsString(PARAMETER_XPATH_FOR_EXAMPLES);

        if (operator.getParameterAsBoolean(PARAMETER_USE_DEFAULT_NAMESPACE) && operator.isParameterSet(PARAMETER_DEFAULT_NAMESPACE)) {
            defaultNamespaceURI = operator.getParameterAsString(PARAMETER_DEFAULT_NAMESPACE);
        } else {
            defaultNamespaceURI = null;
        }

        isNamespaceAware = operator.getParameterAsBoolean(PARAMETER_USE_NAMESPACES);
        if (isNamespaceAware && operator.isParameterSet(PARAMETER_NAMESPACES)) {
            for (String[] pair : operator.getParameterList(PARAMETER_NAMESPACES)) {
                namespaceMap.put(pair[0], pair[1]);
            }
        }

        attributeXPaths = new ArrayList<String>();
        if (operator.isParameterSet(PARAMETER_XPATHS_FOR_ATTRIBUTES))
            for (String attributeXPath : ParameterTypeEnumeration.transformString2Enumeration(operator.getParameterAsString(PARAMETER_XPATHS_FOR_ATTRIBUTES))) {
                attributeXPaths.add(attributeXPath);
            }

    }

    @Override
    public DataResultSet makeDataResultSet(Operator operator) throws OperatorException {
        return new XMLResultSet(operator, this, xmlExampleSourceCompatibilityVersion);
    }

    @Override
    public TableModel makePreviewTableModel(ProgressListener listener) throws OperatorException, ParseException {
        // TODO: Avoid double load of result set.
        return new DefaultPreview(makeDataResultSet(null), listener);
    }

    @Override
    public String getResourceName() {
        return fileName;
    }

    /**
     * This returns the full resource identifier.
     */
    public String getResourceIdentifier() {
        return fileName;
    }

    @Override
    public ExampleSetMetaData makeMetaData() {
        ExampleSetMetaData emd = new ExampleSetMetaData();
        emd.numberOfExamplesIsUnkown();
        return emd;
    }

    @Override
    public void setParameters(AbstractDataResultSetReader operator) {
        operator.setParameter(PARAMETER_FILE, fileName);
        operator.setParameter(PARAMETER_XPATH_FOR_EXAMPLES, exampleXPath);
        operator.setParameter(PARAMETER_USE_NAMESPACES, Boolean.toString(isNamespaceAware));
        operator.setParameter(PARAMETER_USE_DEFAULT_NAMESPACE, Boolean.toString(getDefaultNamespaceURI() != null));
        if (getDefaultNamespaceURI() != null) {
        	// leave unchanged if user did not select a namespace.
        	// this parameter is not used anyway then, since PARAMETER_USE_DEFAULT_NAMESPACE is null in this case. 
        	operator.setParameter(PARAMETER_DEFAULT_NAMESPACE, getDefaultNamespaceURI());
        }

        List<String[]> list = new LinkedList<String[]>();
        for (Map.Entry<String, String> entry : namespaceMap.entrySet()) {
            list.add(new String[] { entry.getKey(), entry.getValue() });
        }
        operator.setParameter(PARAMETER_NAMESPACES, ParameterTypeList.transformList2String(list));
        operator.setParameter(PARAMETER_XPATHS_FOR_ATTRIBUTES, ParameterTypeEnumeration.transformEnumeration2String(attributeXPaths));
    }

    @Override
    public void close() {
    }

    /**
     * This method defines whether the XML should be parsed namespace aware or not.
     */
    public boolean isNamespaceAware() {
        return isNamespaceAware;
    }

    /**
     * This method has to return the String representing the xpath expression that should form the examples.
     */
    public String getExampleXPath() {
        return exampleXPath;
    }

    public void setExampleXPath(String exampleXPath) {
		this.exampleXPath = exampleXPath;
	}

	/**
     * This method must return the XPath expressions in order
     */
    public List<String> getAttributeXPaths() {
        return attributeXPaths;
    }
    
    public void setAttributeXPaths(List<String> attributeXPaths) {
    	this.attributeXPaths = attributeXPaths;
    }

    /**
     * This method has to return all defined namespaces. The key will be the prefix used
     * for identifying the namespace, the value is the URI.
     */
    public Map<String, String> getNamespacesMap() {
        return namespaceMap;
    }

    /**
     * This method will return the ID as saved in the namespaceMap for the given uri.
     * If no such uri is registered null is returned.
     */
    public String getNamespaceId(String namespaceURI) {
        for (Entry<String, String> entry: namespaceMap.entrySet()) {
            if (entry.getValue().equals(namespaceURI))
                return entry.getKey();
        }
        return null;
    }

    /**
     * This sets the used resource identifier.
     */
    public void setResourceIdentifier(String resourceIdentifier) {
        this.fileName = resourceIdentifier;
        this.prefetchedDocument = null; //reseting cached dom.
    }

    /**
     * This will load the DOM from the current xml file if necessary or return the
     * already loaded one. This avoids multiple loaded instances of the same xml file.
     */
    public Document getDocumentObjectModel() throws OperatorException {
        if (prefetchedDocument == null) {        	
            // load document: After expressions to fail fast in case expressions are syntactically wrong
            DocumentBuilderFactory domFactory = DocumentBuilderFactory.newInstance();
            domFactory.setValidating(false);

            domFactory.setNamespaceAware(isNamespaceAware());
            try {
                domFactory.setFeature("http://xml.org/sax/features/namespaces", isNamespaceAware());
                domFactory.setFeature("http://xml.org/sax/features/validation", false);
                domFactory.setFeature("http://apache.org/xml/features/nonvalidating/load-dtd-grammar", false);
                domFactory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);

                DocumentBuilder builder = domFactory.newDocumentBuilder();
                String resourceIdentifier = getResourceIdentifier();
                if (resourceIdentifier == null) {
                	throw new UserError(null, "file_consumer.no_file_defined");
                }
                this.prefetchedDocument = builder.parse(new File(resourceIdentifier));
                return prefetchedDocument;
            } catch (ParserConfigurationException e) {
            	//LogService.getRoot().log(Level.WARNING, "Failed to configure XML parser: "+e, e);
    			LogService.getRoot().log(Level.WARNING,
    					I18N.getMessage(LogService.getRoot().getResourceBundle(), 
    					"com.rapidminer.operator.nio.xml.XMLResultSetConfiguration.configuring_xml_parser_error", 
    					e),
    					e);
            	throw new OperatorException("Failed to configure XML parser: "+e, e);
            } catch (SAXException e) {
            	//LogService.getRoot().log(Level.WARNING, "Failed to parse XML document: "+e, e);
    			LogService.getRoot().log(Level.WARNING,
    					I18N.getMessage(LogService.getRoot().getResourceBundle(), 
    					"com.rapidminer.operator.nio.xml.XMLResultSetConfiguration.parsing_xml_document_error", 
    					e),
    					e);
            	throw new UserError(null, 401, e.getMessage());
            } catch (CharConversionException e) { 
    			LogService.getRoot().log(Level.WARNING,
    					I18N.getMessage(LogService.getRoot().getResourceBundle(), 
    					"com.rapidminer.operator.nio.xml.XMLResultSetConfiguration.parsing_xml_document_error", 
    					e),
    					e);
    			throw new UserError(null, 401, e.getMessage());
        	} catch (IOException e) {
            	//LogService.getRoot().log(Level.WARNING, "Failed to parse XML document: "+e, e);
    			LogService.getRoot().log(Level.WARNING,
    					I18N.getMessage(LogService.getRoot().getResourceBundle(), 
    					"com.rapidminer.operator.nio.xml.XMLResultSetConfiguration.parsing_xml_document_error", 
    					e),
    					e);
            	throw new UserError(null, 302, getResourceIdentifier(), e.getMessage());
            }
            //throw new UserError(null, 100);
        } else {
            return prefetchedDocument;
        }
    }

    /**
     * This returns a string for the default namespace uri or null if no one defined.
     */
    public String getDefaultNamespaceURI() {
        return defaultNamespaceURI;
    }

	public void setNamespacesMap(Map<String, String> idNamespaceMap) {
		this.namespaceMap = idNamespaceMap;
	}

	public void setNamespaceAware(boolean b) {
		this.isNamespaceAware = b;
	}

	public OperatorVersion getXmlExampleSourceCompatibilityVersion() {
		return xmlExampleSourceCompatibilityVersion;
	}
	
	
}
