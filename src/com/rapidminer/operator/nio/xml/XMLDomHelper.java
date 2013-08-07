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

import java.io.StringWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Stack;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.rapidminer.tools.container.Pair;

/**
 * Provides helper functions for XML DOM, like finding common ancestors of nodes,
 * creating XPath expression etc.
 * 
 * @author Marius Helf
 *
 */
public class XMLDomHelper {
    /**
     * A simple class for storing a tuple of element name, attribute name, attribute namespace and Attribute value.
     * 
     * Implements the hashCode() and equals() functions, so that storing in any container with "hashed in its name
     * is possible. 
     * 
     * @author Marius Helf
     *
     */
    public static class AttributeNamespaceValue {
		private String name = null;
		private String namespace = null;
		private String value = null;
		private String element = null;

		public AttributeNamespaceValue() {
    	}
		

    	@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result
					+ ((element == null) ? 0 : element.hashCode());
			result = prime * result + ((name == null) ? 0 : name.hashCode());
			result = prime * result
					+ ((namespace == null) ? 0 : namespace.hashCode());
			result = prime * result + ((value == null) ? 0 : value.hashCode());
			return result;
		}



		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			AttributeNamespaceValue other = (AttributeNamespaceValue) obj;
			if (element == null) {
				if (other.element != null)
					return false;
			} else if (!element.equals(other.element))
				return false;
			if (name == null) {
				if (other.name != null)
					return false;
			} else if (!name.equals(other.name))
				return false;
			if (namespace == null) {
				if (other.namespace != null)
					return false;
			} else if (!namespace.equals(other.namespace))
				return false;
			if (value == null) {
				if (other.value != null)
					return false;
			} else if (!value.equals(other.value))
				return false;
			return true;
		}



		public String getName() {
			return name;
		}

		public String getNamespace() {
			return namespace;
		}

		public String getValue() {
			return value;
		}

		public String getElement() {
			return element;
		}

		public void setElement(String element) {
			this.element = element;
		}

		public void setName(String name) {
			this.name = name;
		}

		public void setNamespace(String namespace) {
			this.namespace = namespace;
		}

		public void setValue(String value) {
			this.value = value;
		}
		
		@Override
		public String toString() {
			StringBuilder builder = new StringBuilder();
			if (element != null) {
				builder.append(element);
			}
			builder.append("[@");
			if (namespace != null) {
				builder.append(namespace);
				builder.append(":");
			}
			builder.append(name);
			builder.append("=\"");
			if (value != null) {
				builder.append(value);
			}
			builder.append("\"]");
			return builder.toString();
		}
    }

	
	
    /**
     * Creates a list of all elements, which are common ancestors of the element sets.
     * They are matched by tag and namespace.
     * The first element in the list resembles the farthest ancestor.
     * Each element in the list is a Pair, with first=namespace and second=tagName
     */
    public static List<Pair<String,String>> getCommonAncestorNames(Set<Element> elements) {
    	// Loop over path elements of the first path, beginning at the end.
    	// Continue as long as all other elements have the same element name at the
    	// same position, relative to the end.
    	Iterator<Element> it = elements.iterator();
    	if (!it.hasNext()) {
    		return new LinkedList<Pair<String,String>>();
    	}
    	
    	// create reference element. All other elements will be compared
    	// to this element.
    	Element referenceElement = it.next();
    	String referenceElementNS = referenceElement.getNamespaceURI();
    	String referenceElementName = referenceElement.getLocalName();
    	
    	// loop over all remaining elements and return an empty list
    	// if one of them does not match the reference element
    	while (it.hasNext()) {
    		Element currentElement = it.next();
			String currentElementNS = currentElement.getNamespaceURI();
			String currentElementName = currentElement.getLocalName();

			// check if namespaces match. Since they may be null, this 
			// comparison is a bit clumsy
			if (currentElementNS != null && referenceElementNS != null) {
				if (!currentElementNS.equals(referenceElementNS)) {
					return new LinkedList<Pair<String,String>>();
				}
			} else {
				if (referenceElementNS != currentElementNS) {
					return new LinkedList<Pair<String,String>>();
				}
			}
			
			// compare element names
			if (!currentElementName.equals(referenceElementName)) {
				return new LinkedList<Pair<String,String>>();
			}
    	}
    	
    	// all ancestors are equal regarding namespace and element name. Add
    	// name and namespace to list and recurse one level deeper
    	LinkedList<Pair<String,String>> commonAncestors = new LinkedList<Pair<String,String>>();
    	
    	// find all direct ancestors
    	Set<Element> directAncestors = getDirectAncestors(elements);
    	// if each element has a parent node, recurse
    	if (!directAncestors.isEmpty()) {
    		commonAncestors.addAll(getCommonAncestorNames(directAncestors));
    	}
    	// add current element namespace and tagname to list
    	commonAncestors.add(new Pair<String,String>(referenceElementNS, referenceElementName));
    	return commonAncestors;
    }

    /**
     * Returns a list of all attributes, whose name, value and namespace are equal for all elements
     * in elements.
     */
    public static Set<AttributeNamespaceValue> getCommonAttributes(Set<Element> elements)
    {
    	// generate attribute-value set for each element:
    	Set<AttributeNamespaceValue> commonAttributeValueSet = null;
    	for (Element element : elements) {
    		Set<AttributeNamespaceValue> elementAttributeValueSet = new HashSet<AttributeNamespaceValue>();
    		NamedNodeMap attributes = element.getAttributes();
    		for (int i = 0; i < attributes.getLength(); ++i) {
    			Attr attribute = (Attr)attributes.item(i);

    			if (!attribute.getLocalName().equals("xmlns") && !(attribute.getNamespaceURI() != null && attribute.getNamespaceURI().equals("http://www.w3.org/2000/xmlns/"))) {
    				AttributeNamespaceValue attributeNSValue = new AttributeNamespaceValue();   					
    				attributeNSValue.setName(attribute.getLocalName());
    				attributeNSValue.setNamespace(attribute.getNamespaceURI());
    				attributeNSValue.setValue(attribute.getValue());

    				// add vector to set
    				elementAttributeValueSet.add(attributeNSValue);
    			}
    		}
    		
    		if (commonAttributeValueSet == null) {
    			commonAttributeValueSet = elementAttributeValueSet;
    		} else {
    			// perform intersection of common attributes and attributes of current element
    			commonAttributeValueSet.retainAll(elementAttributeValueSet);
    		}
    		
    		// break if intersection is empty
    		if (commonAttributeValueSet.isEmpty()) {
    			return commonAttributeValueSet;
    		}
    	}
    	return commonAttributeValueSet;
    }

    /**
     * Returns a set of the direct ancestors of each element in elements.
     * If one element does not have an ancestor, the whole set will be empty.
     */
    public static Set<Element> getDirectAncestors(Set<Element> elements) {
    	Set<Element> directAncestors = new HashSet<Element>();
    	for (Element element : elements) {
    		if (element.getParentNode() != null && element.getParentNode() instanceof Element) {
    			directAncestors.add((Element)element.getParentNode());
    		} else {
    			return new HashSet<Element>();
    		}
    	}
    	return directAncestors;
    }
    
    /**
     * Returns the XPath to retrieve targetElement from rootElement. rootElement may be null, in this case the XPath starts with and includes
     * the farthest non-null ancestor of targetElement. If rootElement == targetElement, an empty string
     * is returned. 
     * @param includeElementIndex Indicates if the element indices in the form elementName[n] should
     * be included in the XPath. 
     * @param namespacesMap Maps namespace ids to namespace URIs.
     */
    public static String getXPath(Element rootElement, Element targetElement, boolean includeElementIndex, Map<String,String> namespacesMap) {
    	Stack<Element> elementPath = new Stack<Element>();
    	
    	// since we need the mapping the other way round, we invert the map
    	Map<String,String> namespaceUriToIdMap = new HashMap<String, String>();
    	for (Entry<String, String> entry : namespacesMap.entrySet()) {
    		namespaceUriToIdMap.put(entry.getValue(), entry.getKey());
    	}
    	
    	
    	
    	// recursively find all ancestors of targetElement (up to, not including, rootElement) 
    	{
	    	Element currentElement = targetElement;
	    	while (currentElement != null && currentElement != rootElement) {
	    		elementPath.push(currentElement);
	    		Node parent = currentElement.getParentNode();
	    		if (parent instanceof Element) {
	    			currentElement = (Element)currentElement.getParentNode();
	    		} else {
	    			currentElement = null;
	    		}
	    	}
    	}
    	
    	// construct XPath
    	StringBuilder builder = new StringBuilder();
    	while (!elementPath.isEmpty()) {
    		Element currentElement = elementPath.pop();
    		if (builder.length() > 0) {
    			// don't include "/" at the beginning
    			builder.append("/");
    		}
    		
    		if (namespacesMap != null) {
	    		String namespace = currentElement.getNamespaceURI();
	    		if (namespace != null) {
	    			namespace = namespaceUriToIdMap.get(namespace);
	    			builder.append(namespace);
	    			builder.append(":");
	    		}
    		}
    		builder.append(currentElement.getLocalName());
    		if (includeElementIndex) {
    			int index = getElementIndex(currentElement);
    			builder.append("[");
    			builder.append(index);
    			builder.append("]");
    		}
    	}
    	return builder.toString();
    }

	/**
	 * Returns the index of element in the list of all elements with the same name in its parent node.
	 * If element's parent node is null, this function returns 0.
	 */
	public static int getElementIndex(Element element) {
    	int index = 1;
    	Node sibling = element;
    	while ((sibling = sibling.getPreviousSibling()) != null) {
    		if (sibling instanceof Element) {
    			Element siblingElement = (Element) sibling;
    			
    			// check if element names and element namespaces match 
    			if (element.getLocalName().equals(siblingElement.getLocalName()) 
    					&& (element.getNamespaceURI() == null?siblingElement.getNamespaceURI()==null:element.getNamespaceURI().equals(siblingElement.getNamespaceURI()))) {
    				++index;
    			}
    		}
    	}
    	return index;
	}


	public static String nodeListToString(NodeList nodeList) throws TransformerException {
		StringWriter stringWriter = new StringWriter();
		for (int i = 0; i < nodeList.getLength(); ++i) {
			Node node = nodeList.item(i);
			if (node instanceof Element) {
				Transformer transformer = TransformerFactory.newInstance().newTransformer();
				transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
				transformer.transform(new DOMSource(node), new StreamResult(stringWriter));
			} else {
				stringWriter.append(node.getTextContent());
			}
		}
		return stringWriter.toString();
	}
}
