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

import java.awt.Color;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.ListCellRenderer;
import javax.swing.border.EmptyBorder;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.table.AbstractTableModel;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.rapidminer.gui.tools.ExtendedJScrollPane;
import com.rapidminer.gui.tools.ExtendedJTable;
import com.rapidminer.gui.tools.ExtendedJTextField;
import com.rapidminer.gui.tools.ExtendedJTextField.TextChangeListener;
import com.rapidminer.gui.tools.ResourceAction;
import com.rapidminer.gui.tools.UpdateQueue;
import com.rapidminer.gui.tools.dialogs.wizards.AbstractWizard;
import com.rapidminer.gui.tools.dialogs.wizards.AbstractWizard.WizardStepDirection;
import com.rapidminer.gui.tools.dialogs.wizards.WizardStep;
import com.rapidminer.io.process.XMLTools;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.tools.I18N;
import com.rapidminer.tools.XMLException;
import com.rapidminer.tools.container.Pair;
import com.rapidminer.tools.io.Encoding;

/**
 * This step allows to enter an XPath expression whose
 * matches will be used as examples.
 * 
 * @author Sebastian Land, Marius Helf
 */
public class XMLExampleExpressionWizardStep extends WizardStep {

    private static Properties XML_PROPERTIES = new Properties();
    {
        XML_PROPERTIES.setProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
    }

    
    
    /**
     * A model which contains rows of element names, attribute names and attribute values.
     * 
     * @author Sebastian Land, Marius Helf
     *
     */
    private class AttributeTableModel extends AbstractTableModel {
        private static final long serialVersionUID = 1L;
        
        private static final int ELEMENT_COLUMN = 0;
        private static final int ATTRIBUTE_COLUMN = 1;
        private static final int VALUE_COLUMN = 2;
        private static final int COLUMN_COUNT = 3;

        private List<XMLDomHelper.AttributeNamespaceValue> attributes = new LinkedList<XMLDomHelper.AttributeNamespaceValue>();

        @Override
        public int getRowCount() {
            if (attributes != null)
                return attributes.size();
            return 0;
        }

        @Override
        public int getColumnCount() {
            return COLUMN_COUNT;
        }

        @Override
        public String getColumnName(int column) {
            switch (column) {
            case ELEMENT_COLUMN:
            	return I18N.getGUILabel("importwizard.xml.example_expression.attribute_table.element_column_header");
            case ATTRIBUTE_COLUMN:
            	return I18N.getGUILabel("importwizard.xml.example_expression.attribute_table.attribute_column_header");
            case VALUE_COLUMN:
            	return I18N.getGUILabel("importwizard.xml.example_expression.attribute_table.value_column_header");
            }
            return "";
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            switch (columnIndex) {
            case ATTRIBUTE_COLUMN:
            	String namespace = attributes.get(rowIndex).getNamespace();
            	String name = attributes.get(rowIndex).getName();
            	name = (namespace!=null)?configuration.getNamespaceId(namespace)+":"+name:name;
                return name;
            case VALUE_COLUMN:
                return attributes.get(rowIndex).getValue();
            case ELEMENT_COLUMN:
                return attributes.get(rowIndex).getElement();
            }
            return null;
        }

		public void setAttributes(Set<XMLDomHelper.AttributeNamespaceValue> allAttributes) {
			attributes.clear();
			attributes.addAll(allAttributes);
			fireTableDataChanged();
		}
    }

    private XMLResultSetConfiguration configuration;

    private JPanel component = new JPanel(new GridBagLayout());
    private XMLTreeModel xmlTreeModel;
    private XMLTreeView xmlTreeView = new XMLTreeView(new HashMap<String,String>());
    private AttributeTableModel attributeTableModel = new AttributeTableModel();
    private ExtendedJTable attributeTable = new ExtendedJTable();
    private JList matchesList = new JList();
    /**
     * A label which displays the status of the current XPath entered/selected by the user.
     */
    JLabel errorLabel = new JLabel();
    
    
    /**
     * A model which provides the XML code of all elements which match the current selected XPath.
     */
    private XPathMatchesListModel matchesListModel;
    
    /**
     * User editable field for displaying/entering the current XPath.  
     */
    private ExtendedJTextField expressionField = new ExtendedJTextField();

	private JButton applyButton;

    /**
     * There must be a configuration given, but might be empty.
     * 
     * @throws OperatorException
     */
    public XMLExampleExpressionWizardStep(AbstractWizard parent, final XMLResultSetConfiguration configuration) throws OperatorException {
        super("importwizard.xml.example_expression");
        this.configuration = configuration;
        
        attributeTable.setModel(attributeTableModel);
        // only select entire rows
        attributeTable.setCellSelectionEnabled(false);
        attributeTable.setRowSelectionAllowed(true);

        // adding components

        JPanel leftBarPanel = new JPanel(new GridBagLayout());
        {
            GridBagConstraints leftBarConstraints = new GridBagConstraints();
            leftBarConstraints.insets = new Insets(0, 5, 5, 5);
            leftBarConstraints.fill = GridBagConstraints.BOTH;
            leftBarConstraints.weightx = 1;
            leftBarConstraints.weighty = 0.7;
            leftBarConstraints.gridwidth = GridBagConstraints.REMAINDER;
            leftBarPanel.add(new ExtendedJScrollPane(xmlTreeView), leftBarConstraints);
            leftBarConstraints.weighty = 0.3;
            leftBarConstraints.insets = new Insets(5, 5, 5, 5);
            leftBarPanel.add(new ExtendedJScrollPane(attributeTable), leftBarConstraints);
            leftBarConstraints.weighty = 0;
            leftBarConstraints.insets = new Insets(5, 5, 0, 5);
            applyButton = new JButton();
            applyButton.setAction(new ResourceAction("importwizard.xml.example_expression.apply_selection") {
				private static final long serialVersionUID = 1L;

				@Override
				public void actionPerformed(ActionEvent e) {
					String xpath = getXPathFromSelection();
					expressionField.setText(xpath);
				}
			});
            applyButton.setEnabled(false);
            leftBarPanel.add(applyButton, leftBarConstraints);
        }
        JPanel rightBarPanel = new JPanel(new GridBagLayout());
        {
        	GridBagConstraints rightBarConstraints = new GridBagConstraints();
        	rightBarConstraints.insets = new Insets(0, 5, 0, 5);
        	rightBarConstraints.weightx = 1;
        	rightBarConstraints.weighty = 0;
        	rightBarConstraints.gridwidth = GridBagConstraints.REMAINDER;
        	rightBarConstraints.fill = GridBagConstraints.BOTH;
        	JLabel matchesLabel = new JLabel(I18N.getGUILabel("importwizard.xml.example_expression.matches_label", "100"));
        	rightBarPanel.add(matchesLabel, rightBarConstraints);
        	rightBarConstraints.weighty = 1;
            rightBarConstraints.insets = new Insets(5, 5, 5, 5);
            rightBarPanel.add(new ExtendedJScrollPane(matchesList), rightBarConstraints);
        }
        
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(5, 5, 5, 5);
        c.fill = GridBagConstraints.BOTH;
        c.weighty = 1d;
        c.weightx = 0.3d;
        component.add(leftBarPanel, c);
        
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.weightx = .7;
        component.add(rightBarPanel, c);

        c.weightx = 1d;
        c.weighty = 0d;

        errorLabel.setForeground(Color.RED);
        c.weighty = 0;
        component.add(errorLabel, c);
        component.add(expressionField, c);

        // listeners
        final UpdateQueue xpathUpdateQueue = new UpdateQueue("xpath_updater");
        parent.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosed(WindowEvent e) {
				xpathUpdateQueue.shutdown();
			}
		});
    	xpathUpdateQueue.start();
        expressionField.getModel().addTextChangeListener(new TextChangeListener() {
            @Override
            public void informTextChanged(final String newValue) {
            	errorLabel.setForeground(Color.GRAY);
            	errorLabel.setText(I18N.getGUILabel("xml_reader.wizard.evaluating"));
                if (matchesListModel != null) {                	
                	xpathUpdateQueue.execute(new Runnable() {
                		public void run() {
                			matchesListModel.setXPathExpression(newValue);                			
                            fireStateChanged();
                		}
                	});
                }
            }
        });

        xmlTreeView.getSelectionModel().addTreeSelectionListener(new TreeSelectionListener() {
            /**
             * Whenever the selection changes, the attributeTableModel is updated to contain only
             * those attributes which have the same names and values in all selected Elements. 
             * 
             * @see javax.swing.event.TreeSelectionListener#valueChanged(javax.swing.event.TreeSelectionEvent)
             */
            @Override
            public void valueChanged(TreeSelectionEvent e) {
            	Set<Element> elements = xmlTreeView.getElementsFromSelection();
            	List<Pair<String,String>> commonAncestors = XMLDomHelper.getCommonAncestorNames(elements);
            	int ancestorCount = commonAncestors.size();
            	Set<Element> ancestorsAtCurrentLevel = elements;
        		Set<XMLDomHelper.AttributeNamespaceValue> allAttributes = new HashSet<XMLDomHelper.AttributeNamespaceValue>();
            	for (int i = ancestorCount-1; i >= 0; --i) {
            		Set<XMLDomHelper.AttributeNamespaceValue> currentLevelAttributes = XMLDomHelper.getCommonAttributes(ancestorsAtCurrentLevel);
            		for(XMLDomHelper.AttributeNamespaceValue attribute : currentLevelAttributes) {
            			String elementName = commonAncestors.get(i).getSecond();
            			elementName = getXPathFromElementList(commonAncestors.subList(0, i+1));
            			attribute.setElement(elementName);
            		}
            		allAttributes.addAll(currentLevelAttributes);
            		ancestorsAtCurrentLevel = XMLDomHelper.getDirectAncestors(ancestorsAtCurrentLevel);
            	}
            	attributeTableModel.setAttributes(allAttributes);
            	applyButton.setEnabled(!elements.isEmpty());
            }
        });

        // configure renderer
        matchesList.setCellRenderer(new ListCellRenderer() {
            private JTextArea area = new JTextArea();
            private JPanel wraperPanel = new JPanel(new GridLayout(1, 1));
            private JPanel emptyPanel = new JPanel();
            {
                wraperPanel.add(area);
                wraperPanel.setBorder(new EmptyBorder(new Insets(0, 0, 1, 0)));
                wraperPanel.setBackground(Color.BLACK);
                wraperPanel.setOpaque(true);
            }

            @Override
            public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                ByteArrayOutputStream os = new ByteArrayOutputStream();
                try {
                    Charset encoding = Encoding.getEncoding("UTF-8");
                    XMLTools.stream(new DOMSource((Node) value), new StreamResult(new OutputStreamWriter(os, encoding)), encoding, XML_PROPERTIES);
                    area.setText(os.toString("UTF-8"));
                    return wraperPanel;
                } catch (XMLException e) {
                    return emptyPanel;
                } catch (UnsupportedEncodingException e) {
                    return emptyPanel;
                }
            }
        });
    }
    
    

    
    /**
     * Returns an XPath which resembles the user's selection in both the xml tree and the 
     * attribute table. 
     */
    private String getXPathFromSelection() {
    	List<Pair<String,String>> commonAncestors = XMLDomHelper.getCommonAncestorNames(xmlTreeView.getElementsFromSelection());
    	
    	int[] selectedAttributeRows = attributeTable.getSelectedRows();
    	
    	// map element names to attribute value xpaths
    	Map<String,String> elementToAttributeValues = new HashMap<String, String>();
    	for (int rowIndex : selectedAttributeRows) {
    		int modelIndex = attributeTable.getModelIndex(rowIndex);
    		
    		String elementName = (String)attributeTableModel.getValueAt(modelIndex, AttributeTableModel.ELEMENT_COLUMN);
    		String attributeName = (String)attributeTableModel.getValueAt(modelIndex, AttributeTableModel.ATTRIBUTE_COLUMN);
    		String attributeValue = (String)attributeTableModel.getValueAt(modelIndex, AttributeTableModel.VALUE_COLUMN);
    		StringBuilder builder = new StringBuilder();
    		String currentXPath = elementToAttributeValues.get(elementName);
    		if (currentXPath != null) {
    			builder.append(currentXPath);
    		}
    		builder.append("[@");
    		builder.append(attributeName);
    		builder.append("=\"");
    		builder.append(attributeValue);
    		builder.append("\"]");
			elementToAttributeValues.put(elementName, builder.toString());
    	}
    	
    	int i = 0;
		StringBuilder builder = new StringBuilder();
		if (!commonAncestors.isEmpty()) {
			builder.append("/");
	    	for (Pair<String,String > ancestor : commonAncestors) {
	    		++i;
	    		String xPathWoAttributes = getXPathFromElementList(commonAncestors.subList(0, i));
	    		String xPathForAttributes = elementToAttributeValues.get(xPathWoAttributes);
	    		builder.append("/");
	    		if (ancestor.getFirst() != null) {
	    			builder.append(configuration.getNamespaceId(ancestor.getFirst()));
	    			builder.append(":");
	    		}
	    		builder.append(ancestor.getSecond());
	    		if (xPathForAttributes != null) {
	    			builder.append(xPathForAttributes);
	    		}
	    	}
		}
		return builder.toString();
	}

	/**
	 * Creates an XPath expression from the given element names.
	 * @param elementNames A list of {@link Pair}s with first==elementNamespace and second==elementName. 
	 */
	private String getXPathFromElementList(List<Pair<String, String>> elementNames) {
		StringBuilder sb = new StringBuilder();
    	if (!elementNames.isEmpty()) {
    		sb.append("/");
    	}
    	for (Pair<String,String> element : elementNames) {
    		sb.append("/");
    		if (element.getFirst() != null) {
    			sb.append(configuration.getNamespaceId(element.getFirst()));
    			sb.append(":");
    		}
    		sb.append(element.getSecond());
    	}
		return sb.toString();
	}

	@Override
    protected boolean performEnteringAction(WizardStepDirection direction) {
        try {
            xmlTreeModel = new XMLTreeModel(configuration.getDocumentObjectModel().getDocumentElement(), false);
            xmlTreeView.setNamespacesMap(configuration.getNamespacesMap());
            xmlTreeView.setModel(xmlTreeModel);
            matchesListModel = new XPathMatchesListModel(configuration.getDocumentObjectModel(), configuration.getNamespacesMap(), configuration.getDefaultNamespaceURI(), 100);
            matchesListModel.addListener(new XPathMatchesListModel.XPathMatchesResultListener() {
    			@Override
    			public void informStateChange(String message, boolean error) {
    				errorLabel.setText(message);
    				if (error) {
    					errorLabel.setForeground(Color.RED);
    				} else {
    					errorLabel.setForeground(Color.BLACK);
    				}
    			}
    		});
            matchesList.setModel(matchesListModel);
            if (configuration.getExampleXPath() != null)
                expressionField.setText(configuration.getExampleXPath());
            else
                expressionField.setText("");
        } catch (OperatorException e) {
        	errorLabel.setForeground(Color.RED);
            errorLabel.setText(I18N.getGUILabel("xml_reader.wizard.cannot_load_dom", e));
        }
        return true;
    }

    @Override
    protected boolean performLeavingAction(WizardStepDirection direction) {
    	configuration.setExampleXPath(expressionField.getText());
    	return true;
    }

    @Override
    protected JComponent getComponent() {
        return component;
    }

    @Override
    protected boolean canProceed() {
    	 return matchesListModel != null && matchesListModel.getSize() > 0;
    }

    @Override
    protected boolean canGoBack() {
        return true;
    }
}
