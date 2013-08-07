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

import java.awt.Component;
import java.awt.Font;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTree;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;

import org.w3c.dom.Attr;
import org.w3c.dom.Element;


/**
 * A tree viewer for displaying {@link XMLTreeModel}s.
 * @author Marius Helf
 *
 */
public class XMLTreeView extends JTree {

	private static final long serialVersionUID = 1L;
	private Map<String, String> namespaceUriToIdMap;
	private Set<Object> highlightedNodes;
	private boolean showElementIndices = false;

	/**
	 * Constructs a new XMLTreeView.
	 * @param namespacesMap Maps namespace ids to namespace URIs.
	 */
	public XMLTreeView(Map<String,String> namespacesMap) {
		super();
		setNamespacesMap(namespacesMap);
		
        setCellRenderer(new DefaultTreeCellRenderer() {
            private static final long serialVersionUID = 1L;
            private JPanel emptyPanel = new JPanel();

            @Override
            public Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus) {
                StringBuilder builder = new StringBuilder();
                if (value instanceof Element) {
                    Element element = (Element) value;

                    builder.append("<");

                    // prepend namespace uri, if it not already in element name (use ":" as heuristic)
                    if (element.getNamespaceURI() != null) {
                    	builder.append(namespaceUriToIdMap.get(element.getNamespaceURI())).append(":");
                    }
                    
                    // append element name without namespace
                    builder.append(element.getLocalName());

                    if (showElementIndices) {
                    	int index = XMLDomHelper.getElementIndex(element);
                    	builder.append("[");
                    	builder.append(index);
                    	builder.append("]");
                    }
                    builder.append(">");
                	
                } else if (value instanceof Attr) {
                    builder.append("@");
                    Attr attribute = (Attr) value;

                    // prepend namespace uri, if it not already in element name (use ":" as heuristic)
                    if (attribute.getNamespaceURI() != null) {
                    	builder.append(namespaceUriToIdMap.get(attribute.getNamespaceURI())).append(":");
                    }
                    
                    // append element name without namespace
                    builder.append(attribute.getLocalName());
                } else {
                    // this should not happen, as the model only shows elements and attributes
                	return emptyPanel;
                }
                
                // now configure actual label
                JLabel treeCellRendererComponent = (JLabel) super.getTreeCellRendererComponent(tree, builder.toString(), selected, expanded, leaf, row, hasFocus);
                treeCellRendererComponent.setIcon(null);
                if (highlightedNodes.contains(value)) {
                	Font font = treeCellRendererComponent.getFont();
                	treeCellRendererComponent.setFont(new Font(font.getName(),java.awt.Font.BOLD, font.getSize()));
                }

                return treeCellRendererComponent;
            } 
        });

	}
	
	@Override
	public void setModel(TreeModel model) {
		highlightedNodes = new HashSet<Object>();
		super.setModel(model);
	}

	/**
	 * Sets the namespaces map.
	 * @param namespacesMap Maps namespace ids to namespace URIs.
	 */
	public void setNamespacesMap(Map<String, String> namespacesMap) {
		// since in this class we need uri->id instead of id->uri, invert the map:
		this.namespaceUriToIdMap = new HashMap<String,String>();
		for (Map.Entry<String, String> entry : namespacesMap.entrySet() ) {
			namespaceUriToIdMap.put(entry.getValue(), entry.getKey());
		}
	}
	
    /**
     * Returns a set of all elements which are selected in this XMLTreeModel.
     */
    public Set<Element> getElementsFromSelection() {
    	Set<Element> selection = new HashSet<Element>();
    	TreePath[] selectionPaths = getSelectionPaths();
    	if (selectionPaths == null) {
    		return selection;
    	}
    	for (TreePath path : selectionPaths) {
    		Object lastComponent = path.getLastPathComponent();
    		if (lastComponent instanceof Element) {
    			selection.add((Element)(lastComponent));
    		}
    	}
    	return selection;
    }

    /**
     * Returns a set of all attributes which are selected in this XMLTreeModel.
     */
    public Set<Attr> getAttributesFromSelection() {
    	Set<Attr> selection = new HashSet<Attr>();
    	TreePath[] selectionPaths = getSelectionPaths();
    	if (selectionPaths == null) {
    		return selection;
    	}
    	for (TreePath path : selectionPaths) {
    		Object lastComponent = path.getLastPathComponent();
    		if (lastComponent instanceof Attr) {
    			selection.add((Attr)(lastComponent));
    		}
    	}
    	return selection;
    }
    
    /**
     * Marks a tree element as highlighted. Highlighted elements are displayed with bold font. 
     * @param node The highlighted tree element.
     * @param highlighted Indicates if the element is highlighted or not.
     */
    public void setHighlighted(Object node, boolean highlighted) {
    	if (highlighted) {
    		highlightedNodes.add(node);
    	} else {
    		highlightedNodes.remove(node);
    	}
    }
    
    public void setShowElementIndices(boolean yes) {
    	showElementIndices = yes;
    }
}
