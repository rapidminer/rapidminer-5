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

import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;

import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.NodeList;

/**
 * A tree model for JTree which holds the structure of an XML {@link Element} and its subnodes.
 * 
 * @author Sebastian Land, Marius Helf
 *
 */
public class XMLTreeModel implements TreeModel {

    private LinkedList<TreeModelListener> listeners = new LinkedList<TreeModelListener>();
    private Element rootElement;
	private boolean provideAttributes;

    public XMLTreeModel(Element rootElement, boolean provideAttributes) {
        this.rootElement = rootElement;
        this.provideAttributes = provideAttributes;
    }

    @Override
    public Object getRoot() {
        return rootElement;
    }

    @Override
    public Object getChild(Object parent, int index) {
        Element element = (Element) parent;
        NodeList childNodes = element.getChildNodes();
        int elementIndex = 0;
        if (provideAttributes) {
        	// first search attributes
        	NamedNodeMap attributes = element.getAttributes();
            for (int i = 0; i < attributes.getLength(); i++) {
                if (elementIndex == index) {
                	return attributes.item(i);
                }
            	elementIndex++;
            }
        }
        
        for (int i = 0; i < childNodes.getLength(); i++) {
            if (childNodes.item(i) instanceof Element) {
                if (elementIndex == index)
                    return childNodes.item(i);
                else
                    elementIndex++;
            }
        }
        return null;
    }

    @Override
    public int getChildCount(Object parent) {
    	if (parent instanceof Attr) {
    		return 0;
    	}
    	
        Element element = (Element) parent;
        NodeList childNodes = element.getChildNodes();
        int childCount = 0;
        for (int i = 0; i < childNodes.getLength(); i++) {
            if (childNodes.item(i) instanceof Element) {
                childCount++;
            }
        }
        if (provideAttributes) {
        	childCount += element.getAttributes().getLength();
        }
        return childCount;
    }

    @Override
    public boolean isLeaf(Object node) {
        return getChildCount(node) == 0;
    }

    @Override
    public void valueForPathChanged(TreePath path, Object newValue) {
        // firing event to all listener
        for (TreeModelListener listener : listeners) {
            listener.treeNodesChanged(new TreeModelEvent(this, path));
        }
    }

    @Override
    public int getIndexOfChild(Object parent, Object child) {
        int elementIndex = 0;
    	if (provideAttributes) {
    		if (parent instanceof Attr) {
    			return -1;
    		}
            Element element = (Element) parent;

    		if (child instanceof Attr) {
    			NamedNodeMap attributes = element.getAttributes();
    			for (int i = 0; i < attributes.getLength(); ++i) {
    				if (child == attributes.item(i)) {
    					return elementIndex;
    				}
    				elementIndex++;
    			}
    		}
    	}
    	
        Element element = (Element) parent;
        NodeList childNodes = element.getChildNodes();
        for (int i = 0; i < childNodes.getLength(); i++) {
            if (childNodes.item(i) instanceof Element) {
                if (child == childNodes.item(i))
                    return elementIndex;
                elementIndex++;
            }
        }
        return -1;
    }

    @Override
    public void addTreeModelListener(TreeModelListener l) {
        listeners.add(l);
    }

    @Override
    public void removeTreeModelListener(TreeModelListener l) {
        listeners.remove(l);
    }
}
