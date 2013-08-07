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

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.swing.AbstractListModel;
import javax.swing.SwingUtilities;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.rapidminer.tools.I18N;
import com.rapidminer.tools.xml.MapBasedNamespaceContext;

/**
 * A model which holds the result of XPath expression on an XML document.
 * 
 * @author Marius Helf
 *
 */
public class XPathMatchesListModel extends AbstractListModel {
    
	public static interface XPathMatchesResultListener {
		public void informStateChange(String message, boolean error);
	}
	
	private static final long serialVersionUID = 5596412058073512745L;
    private Document document;
    private XPath xpath;
    private NodeList exampleNodes;
    private List<XPathMatchesResultListener> listeners = new LinkedList<XPathMatchesListModel.XPathMatchesResultListener>();
	private int maxElements;

    /**
     * Creates a new XPathMatchesListModel.
     * @param document The XML Document to which this model refers.
     * @param namespaceMap Maps namespace IDs to namespace URIs. 
     * @param defaultNamespaceURI The default namespace URI.
     * @param maxElements The maximum number of provided elements.
     */
    public XPathMatchesListModel(Document document, Map<String, String> namespaceMap, String defaultNamespaceURI, int maxElements) {
        this.document = document;
        this.xpath = XPathFactory.newInstance().newXPath();
        this.maxElements = maxElements;
        xpath.setNamespaceContext(new MapBasedNamespaceContext(namespaceMap, defaultNamespaceURI));
    }

    public void setXPathExpression(String expression) {
        XPathExpression exampleExpression = null;
        try {
            exampleExpression = xpath.compile(expression);
        } catch (XPathExpressionException e1) {
        	fireStateChange(I18N.getGUILabel("xml_reader.wizard.illegal_xpath", e1), true);
        }
        if (exampleExpression != null) {
            try {
                final int oldSize = getSize();
                exampleNodes = (NodeList) exampleExpression.evaluate(document, XPathConstants.NODESET);                
                // check that only elements, no attributes are contained in the xpath results:
                List<String> illegalElements = new LinkedList<String>();
                for (int i = 0; i < exampleNodes.getLength(); ++i) {
                	if (!(exampleNodes.item(i) instanceof Element)) {
                		illegalElements.add(exampleNodes.item(i).getNodeName());
                	}
                }
                if (!illegalElements.isEmpty()) {
                    fireStateChange(I18N.getGUILabel("xml_reader.wizard.xpath_non_element_nodes", illegalElements.toString()), true);
                    exampleNodes = null;
                    return;
                }

                SwingUtilities.invokeLater(new Runnable() {
                	public void run() {
                		fireContentsChanged(this, 0, Math.min(oldSize, exampleNodes.getLength()));
                		if (oldSize > exampleNodes.getLength()) {
                			fireIntervalRemoved(this, exampleNodes.getLength(), oldSize - 1);
                		} else if (oldSize < exampleNodes.getLength()) {
                			fireIntervalAdded(this, oldSize, exampleNodes.getLength() - 1);
                		}

                		fireStateChange(I18N.getGUILabel("xml_reader.wizard.xpath_result", exampleNodes.getLength()), exampleNodes.getLength() == 0);
                	}
                });
            } catch (final XPathExpressionException e) {
                exampleNodes = null;
            	SwingUtilities.invokeLater(new Runnable() {
                	public void run() {
                		fireStateChange(I18N.getGUILabel("xml_reader.wizard.illegal_xpath", e.getMessage()), true);
                	}
            	});
            }
        } else {
            exampleNodes = null;
        }
    }

    /* (non-Javadoc)
     * @see javax.swing.ListModel#getSize()
     * Returns the number of matched nodes (without subnodes).
     */
    @Override
    public int getSize() {
        if (exampleNodes == null) {
            return 0;
        }
        if (exampleNodes.getLength() > maxElements) {
        	return maxElements;
        }
        return exampleNodes.getLength();
    }

    @Override
    public Object getElementAt(int index) {
        return exampleNodes.item(index);
    }
    
    public void addListener(XPathMatchesResultListener listener) {
    	listeners.add(listener);
    }
    
    public void removeListener(XPathMatchesResultListener listener) {
    	listeners.remove(listener);
    }
    
    /**
     * Informs all XPathMatchesResultListener of this model about a state change.
     * @param message
     * @param error
     */
    private void fireStateChange(String message, boolean error) {
    	for (XPathMatchesResultListener listener : listeners) {
    		listener.informStateChange(message, error);
    	}
    }

}
