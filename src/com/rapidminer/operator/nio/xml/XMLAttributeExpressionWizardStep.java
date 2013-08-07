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
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.tree.TreeSelectionModel;
import javax.xml.transform.TransformerException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.NodeList;

import com.rapidminer.gui.tools.ColoredTableCellRenderer;
import com.rapidminer.gui.tools.ExtendedJScrollPane;
import com.rapidminer.gui.tools.ExtendedJTable;
import com.rapidminer.gui.tools.ExtendedJTableSorterModel;
import com.rapidminer.gui.tools.ResourceAction;
import com.rapidminer.gui.tools.dialogs.wizards.AbstractWizard;
import com.rapidminer.gui.tools.dialogs.wizards.AbstractWizard.WizardStepDirection;
import com.rapidminer.gui.tools.dialogs.wizards.WizardStep;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.tools.I18N;
import com.rapidminer.tools.xml.MapBasedNamespaceContext;

/**
 * A wizard step for selecting attributes from previously selected examples.
 * 
 * Gets an XPath expression and lets the user choose attributes and text content of 
 * XML elements matching the XPath.
 * 
 * @author Marius Helf
 *
 */
public class XMLAttributeExpressionWizardStep extends WizardStep {
	
	/**
	 * A simple subclass of {@link ExtendedJTable} specifically for use with anErrorAwareTableModel.
	 * Prior to rendering a cell, asks its model if in that cell there exists an error and draws a 
	 * red cell background in that case.
	 * 
	 * @author Marius Helf
	 *
	 */
	private class ExtendedJTableWithErrorIndicator extends ExtendedJTable {
		private static final long serialVersionUID = 1L;

		/** 
		 * Returns a cell renderer with red background if the model indicates an error in that cell.
		 * Returns the default cell renderer otherwise. 
		 * @see com.rapidminer.gui.tools.ExtendedJTable#getCellRenderer(int, int)
		 */
		@Override
		public TableCellRenderer getCellRenderer(int row, int col) {
			ColoredTableCellRenderer renderer = (ColoredTableCellRenderer)super.getCellRenderer(row, col);
			ErrorAwareTableModel model = (ErrorAwareTableModel)((ExtendedJTableSorterModel)getModel()).getTableModel();
			if (model.hasError(row, col)) {
				renderer.setColor(Color.RED);
			}
			return renderer;
		}
	}
	
	/**
	 * Interface to use with ExtendendJTableWithErrorIndicator.
	 * 
	 * @author Marius Helf
	 *
	 */
	private interface ErrorAwareTableModel {
		boolean hasError(int rowIndex, int columnIndex);
	}
	
	
	/**
	 * Table model to provide XPath expressions to attributes and their values in a given element.
	 * 
	 * @author Marius Helf
	 *
	 */
	private class AttributeXPathTableModel extends AbstractTableModel implements ErrorAwareTableModel {
		private static final long serialVersionUID = 1L;
		public static final int XPATH_COLUMN = 0;
		public static final int VALUE_COLUMN = 1;
		public static final int COLUMN_COUNT = 2;
		
		/**
		 * A List of Vectors of Strings, where each Vector represents a row.
		 * Which value is stored in which column is defined by XPATH_COLUMN and VALUE_COLUMN.
		 */
		List<Vector<String>> xPaths = new LinkedList<Vector<String>>();
		
		/**
		 * Sets the XPath string for a given row. aValue must be instanceof String and is
		 * interpreted as XPath expression.
		 * 
		 * The XPath is evaluated, and in the value column the value of the first match
		 * is set.
		 * 
		 * 
		 * @see javax.swing.table.AbstractTableModel#setValueAt(java.lang.Object, int, int)
		 */
		@Override
		public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
			switch (columnIndex) {
			case XPATH_COLUMN:
				Vector<String> row = xPaths.get(rowIndex);
				String xPath = (String)aValue;
				List<String> valueList = getDataForXPath(xPath);
				String value = null;
				if (valueList.size() > 0) {
					value = valueList.get(0);
				}
				row.add(XPATH_COLUMN, xPath);
				row.add(VALUE_COLUMN, value);
				fireTableCellUpdated(rowIndex, VALUE_COLUMN);
			}
		}
		

		@Override
		public int getRowCount() {
			if (xPaths != null) {
				return xPaths.size();
			}
			return 0;
		}

		@Override
		public int getColumnCount() {
			return COLUMN_COUNT;
		}
		
        @Override
        public String getColumnName(int column) {
            switch (column) {
            case XPATH_COLUMN:
                return I18N.getGUILabel("importwizard.xml.attribute_expression.xpath_table.xpath_column_header");
            case VALUE_COLUMN:
                return I18N.getGUILabel("importwizard.xml.attribute_expression.xpath_table.value_column_header");
            }
            return "";
        }



		@Override
		public Object getValueAt(int rowIndex, int columnIndex) {
			switch (columnIndex) {
			case XPATH_COLUMN:
			case VALUE_COLUMN:
				return xPaths.get(rowIndex).get(columnIndex);
			}
			return null;
		}
		
		@Override
		public boolean isCellEditable(int rowIndex, int columnIndex) {
			switch (columnIndex) {
			case XPATH_COLUMN:
				return true;
			}
			return false;
		}
		
		/**
		 * Returns the index of the first row whose XPATH_COLUMN equals the given xPath.
		 * @return
		 */
		public int rowIndexOfXPath(String xPath) {
			int idx = 0;
			for (Vector<String> row : xPaths) {
				if (row.get(XPATH_COLUMN).equals(xPath)) {
					return idx;
				}
				++idx;
			}
			return -1;
		}
		
		public boolean containsXPath(String xPath) {
			return rowIndexOfXPath(xPath) >= 0;
		}
		
		/**
		 * Adds an XPath to the model. Basically behaves like setValueAt().
		 */
		public void addXPath(String xPath) {
			if (!containsXPath(xPath)) {
				Vector<String> newRow = new Vector<String>(COLUMN_COUNT);
				newRow.add(XPATH_COLUMN, xPath);
				List<String> valueList = getDataForXPath(xPath);
				String value = null;
				if (valueList.size() > 0) {
					value = valueList.get(0);
				} 
				newRow.add(VALUE_COLUMN, value);
				xPaths.add(newRow);
				fireTableRowsInserted(xPaths.size()-1, xPaths.size()-1);
				attributeTableModel.update();
			}
		}
		
		/**
		 * @return A list of all XPaths in the model.
		 */
		public List<String> getXPaths() {
			List<String> result = new LinkedList<String>();
			for (Vector<String> row : xPaths) {
				result.add(row.get(XPATH_COLUMN));
			}
			return result;
		}

		/**
		 * Clears the model and then adds all XPaths from attriubteXPaths to the model.
		 * Also evaluates the XPaths and inserts the values, i.e. behaves like addXPath().
		 * 
		 */
		public void setXPaths(List<String> attributeXPaths) {
			xPaths.clear();
			if (attributeXPaths != null) {
				for (String xPath : attributeXPaths) {
					Vector<String> row = new Vector<String>(COLUMN_COUNT);
					row.add(XPATH_COLUMN, xPath);
					row.add(VALUE_COLUMN, null);
					xPaths.add(row);
				}
				updateValuesFromXPaths();
			}
			fireTableDataChanged();
		}
		
		/**
		 * Evaluates the given XPath in the scope of the currentElement in the XMLAttributeExpressionWizardStep.
		 * Returns the textContent of all matched Nodes as a list of strings.
		 */
		public List<String> getDataForXPath(String xPathString) {
			List<String> resultList = new LinkedList<String>();
			XPath xPath = XPathFactory.newInstance().newXPath();
			xPath.setNamespaceContext(new MapBasedNamespaceContext(configuration.getNamespacesMap(), configuration.getDefaultNamespaceURI()));
			try {
				XPathExpression xPathExpression = xPath.compile(xPathString);
				NodeList matchedNodes = (NodeList)xPathExpression.evaluate(currentElement, XPathConstants.NODESET);
				if (configuration.getXmlExampleSourceCompatibilityVersion().compareTo(XMLExampleSource.CHANGE_5_1_013_NODE_OUTPUT) > 0) {
					try {
						String resultString;
						resultString = XMLDomHelper.nodeListToString(matchedNodes);
						resultList.add(resultString);
					} catch (TransformerException e) {
						resultList.add(null);
					}
				}else {
					for (int i = 0; i < matchedNodes.getLength(); ++i) {
						// TODO operator Version beachten
						resultList.add(matchedNodes.item(i).getTextContent());
					} 
				}
			} catch (XPathExpressionException e) {
				return new LinkedList<String>();
			}
			return resultList;
		}


		/**
		 * Evaluates each XPath in this model and writes the text content of the first match into the VALUE_COLUMN.
		 */
		public void updateValuesFromXPaths() {
			int rowIndex = 0;
			for (Vector<String> row: xPaths) {
				String xPath = row.get(XPATH_COLUMN);
				List<String> valueList = getDataForXPath(xPath);
				String value = null;
				if (valueList.size() > 0) {
					value = valueList.get(0);
				} 
				row.set(VALUE_COLUMN, value);
				fireTableCellUpdated(rowIndex, VALUE_COLUMN);
				++rowIndex;
			}
		}


		/** 
		 * Returns true iff the XPath in the given row is not valid.
		 * @see com.rapidminer.operator.nio.xml.XMLAttributeExpressionWizardStep.ErrorAwareTableModel#hasError(int, int)
		 */
		@Override
		public boolean hasError(int rowIndex, int columnIndex) {
			if (getValueAt(rowIndex, VALUE_COLUMN) == null) {
				return true;
			}
			return !isValidXPath(xPaths.get(rowIndex).get(XPATH_COLUMN));
		}
		
		/**
		 * Returns true if at least one XPath in the model in invalid.
		 */
		public boolean hasErrors() {
			for (int i = 0; i < getRowCount(); ++i) {
				if (hasError(i, XPATH_COLUMN)) {
					return true;
				}
			}
			return false;
		}


		/**
		 * Removes the rows with the given indices from the model.
		 */
		public void removeRows(int[] selectedRows) {
			Arrays.sort(selectedRows);
			for (int i = selectedRows.length-1; i >= 0; --i) {
				int row = selectedRows[i];
				xPaths.remove(row);
				fireTableRowsDeleted(row, row);
			}
		}
	}
	
	
	
    /**
     * A model which contains attributes and their values.
     * In addition to the attributes the special string TEXT_NODE_TEXT can be added, representing
     * not an attribute but the textContent() of an Element.
     * 
     * Does not provide attributes which are already contained in the {@link AttributeXPathTableModel} of this
     * wizard step, since this model is used to display those attributes which the user is still able to add
     * to the final example set.
     * 
     * @author Marius Helf
     *
     */
    private class AttributeTableModel extends DefaultTableModel {
        private static final long serialVersionUID = 1L;
        
        public static final int ATTRIBUTE_COLUMN = 0;
        public static final int VALUE_COLUMN = 1;
        
        public static final String TEXT_NODE_TEXT = "text()";

        /**
         * The element whose attributes are provided by this model.
         */
        private Element element = null;
        
        public AttributeTableModel() {
        	addColumn(I18N.getGUILabel("importwizard.xml.attribute_expression.attribute_table.attribute_column_header"));
        	addColumn(I18N.getGUILabel("importwizard.xml.attribute_expression.attribute_table.value_column_header"));
        }

        /**
         * This model is not editable.
         * @see javax.swing.table.DefaultTableModel#isCellEditable(int, int)
         */
        @Override
        public boolean isCellEditable(int row, int column) {
        	return false;
        }
        
        /**
         * Rereads the values of all attributes from the current element reference of this model.
         * Attributes not yet in the model are added, those already present in the attributeXPathModel are deleted.
         */
        public void update() {
			// clear model
			setRowCount(0);
			
			if (element == null) {
				return;
			}
			
			NamedNodeMap attributes = element.getAttributes();
			for (int i = 0; i < attributes.getLength(); ++i) {
				Attr attribute = (Attr)attributes.item(i);
				String namespace = attribute.getNamespaceURI();
				String name = attributes.item(i).getLocalName();
				name = (namespace!=null)?configuration.getNamespaceId(namespace)+":"+name:name;
				if (!attributeXPathModel.containsXPath(getXPathForAttribute(name))) {
					String value = attribute.getNodeValue();
					Vector<String> row = new Vector<String>();
					row.add(ATTRIBUTE_COLUMN, name);
					row.add(VALUE_COLUMN, value);
					addRow(row);
				}
			}
			
			// add text() element
			String xPathForAttribute = getXPathForAttribute(TEXT_NODE_TEXT);
			if (!attributeXPathModel.containsXPath(xPathForAttribute)) {
				try {
					XPathFactory factory = XPathFactory.newInstance();
					XPath xpath = factory.newXPath();
					final Map<String, String> namespacesMap = configuration.getNamespacesMap();
					xpath.setNamespaceContext(new MapBasedNamespaceContext(namespacesMap, configuration.getDefaultNamespaceURI()));
					XPathExpression expression;
					expression = xpath.compile("text()");
					String result = (String)expression.evaluate(element, XPathConstants.STRING);
					Vector<String> row = new Vector<String>();
					row.add(ATTRIBUTE_COLUMN, TEXT_NODE_TEXT);
					row.add(VALUE_COLUMN, result);
					addRow(row);
				} catch (XPathExpressionException e) {
					// do nothing (simply don't add text() element)
				}

			}
			fireTableDataChanged();
        }

		/**
		 * Sets the element whose attributes are provided by this model and updates the model.
		 */
		public void setElement(Element element) {
			this.element = element;
			update();
		}

		public Element getElement() {
			return element;
		}
    }

	

	private XMLResultSetConfiguration configuration;
	
	
	/**
	 * Displays the XML structure of the current element.
	 */
	private XMLTreeView xmlTreeView;
	
    /**
     * Holds the attributes of the currently selected element.
     */
    private AttributeTableModel attributeTableModel = new AttributeTableModel();
    private ExtendedJTable attributeTable = new ExtendedJTable(attributeTableModel, true);
	
    private JPanel component = new JPanel(new GridBagLayout());
    
    /**
     * The list of all elements matched by the XPath provided by the previous wizard step.
     */
    private NodeList elementMatches = null;
    private int currentMatchIndex = 0;
	private JButton nextMatchButton;
	private JButton previousMatchButton;
	private Element currentElement;
	
	/**
	 * Displays all XPaths the user has selected to represent attributes in the resulting example set. 
	 */
	private ExtendedJTableWithErrorIndicator xPathTable;
	
	/**
	 * The model for xPathTable.
	 */
	private AttributeXPathTableModel attributeXPathModel = new AttributeXPathTableModel();
	private JButton addButton;
	private JButton deleteFromXPathTableButton;

	/**
	 * Instantiates a new XMLAttributeExpressionWizardStep and initalizes the GUI elements.
	 */
	public XMLAttributeExpressionWizardStep(AbstractWizard parent, final XMLResultSetConfiguration configuration) throws OperatorException {
		super("importwizard.xml.attribute_expression");

        this.configuration = configuration;
        
        // only select entire rows
        attributeTable.setCellSelectionEnabled(false);
        attributeTable.setRowSelectionAllowed(true);
        

        // adding components

        // left panel contains XML Element view and the attribute table with the attributes of the current element.
        JPanel leftBarPanel = new JPanel(new GridBagLayout());
        {
            GridBagConstraints leftBarConstraints = new GridBagConstraints();

            // add previous/next buttons
            leftBarConstraints.fill = GridBagConstraints.BOTH;
            leftBarConstraints.weightx = .5;
            leftBarConstraints.weighty = 0;
            leftBarConstraints.insets = new Insets(0, 5, 5, 5);
            leftBarConstraints.gridwidth = 1;
            previousMatchButton = new JButton();
            leftBarPanel.add(previousMatchButton, leftBarConstraints);
            nextMatchButton = new JButton();
            leftBarConstraints.gridwidth = GridBagConstraints.REMAINDER;
            leftBarPanel.add(nextMatchButton, leftBarConstraints);
            
            // add xml tree view
            leftBarConstraints.insets = new Insets(5, 5, 5, 5);
            leftBarConstraints.weightx = 1;
            leftBarConstraints.weighty = 0;
            leftBarConstraints.gridwidth = GridBagConstraints.REMAINDER;
            
            leftBarConstraints.insets = new Insets(5, 5, 0, 5);
            JLabel xmlTreeLabel = new JLabel(I18N.getGUILabel("importwizard.xml.attribute_expression.xml_tree_label"));
            leftBarPanel.add(xmlTreeLabel, leftBarConstraints);

            leftBarConstraints.weighty = 0.5;
            leftBarConstraints.insets = new Insets(5, 5, 5, 5);
            xmlTreeView = new XMLTreeView(configuration.getNamespacesMap());
            xmlTreeView.setShowElementIndices(true);
            xmlTreeView.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
            leftBarPanel.add(new ExtendedJScrollPane(xmlTreeView), leftBarConstraints);
            
            
            // add attribute table
            leftBarConstraints.weighty = 0;
            leftBarConstraints.insets = new Insets(5, 5, 0, 5);
            JLabel attributeTableLabel = new JLabel(I18N.getGUILabel("importwizard.xml.attribute_expression.attribute_table_label"));
            leftBarPanel.add(attributeTableLabel, leftBarConstraints);

            leftBarConstraints.weighty = 0.5;
            leftBarPanel.add(new ExtendedJScrollPane(attributeTable), leftBarConstraints);
            
            
            // add add/remove selection button
            leftBarConstraints.weightx = .5;
            leftBarConstraints.weighty = 0;
            leftBarConstraints.insets = new Insets(5, 5, 0, 5);

            // listeners for buttons
            previousMatchButton.setAction(new ResourceAction(true, "importwizard.xml.attribute_expression.previous_match") {
				private static final long serialVersionUID = 1L;

				@Override
				public void actionPerformed(ActionEvent e) {
					setCurrentMatch(currentMatchIndex-1);
				}
			});
            
            nextMatchButton.setAction(new ResourceAction(true, "importwizard.xml.attribute_expression.next_match") {
				private static final long serialVersionUID = 1L;

				@Override
				public void actionPerformed(ActionEvent e) {
					setCurrentMatch(currentMatchIndex+1);
				}
			});

            attributeXPathModel.addTableModelListener(new TableModelListener() {
				@Override
				public void tableChanged(TableModelEvent e) {
					addButton.setEnabled(attributeTable.getSelectedRows().length > 0);
					attributeTableModel.update();
					fireStateChanged();
				}
			});
            
            attributeTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
				@Override
				public void valueChanged(ListSelectionEvent e) {
					addButton.setEnabled(attributeTable.getSelectedRows().length > 0);
				}
			});
            
            attributeTable.addKeyListener(new KeyListener() {
				@Override
				public void keyTyped(KeyEvent e) {
					// do nothing
				}
				
				@Override
				public void keyReleased(KeyEvent e) {
					// do nothing
				}
				
				@Override
				public void keyPressed(KeyEvent e) {
					switch (e.getKeyCode()) {
					case KeyEvent.VK_ENTER:
						addSelectedAttributes();
						e.consume();
						break;
					}
				}
			});
            
            attributeTable.addMouseListener(new MouseListener() {
				@Override
				public void mouseClicked(MouseEvent e) {
					if (e.getButton() == MouseEvent.BUTTON1 && e.getClickCount() == 2) {
						addSelectedAttributes();
					}
				}

				@Override
				public void mousePressed(MouseEvent e) {
					// do nothing
				}

				@Override
				public void mouseReleased(MouseEvent e) {
					// do nothing
				}

				@Override
				public void mouseEntered(MouseEvent e) {
					// do nothing
				}

				@Override
				public void mouseExited(MouseEvent e) {
					// do nothing
				}
			});

            // tree selection listener, which updates the attribute table on changes of the selection
            // in the xmlTreeView.
            xmlTreeView.getSelectionModel().addTreeSelectionListener(new TreeSelectionListener() {
                @Override
                public void valueChanged(TreeSelectionEvent e) {
                	Set<Element> elements = xmlTreeView.getElementsFromSelection();	// should contain one or zero elements
                	for (Element element : elements) {
                		// for loop should iterate at most once
                		attributeTableModel.setElement(element);
                		attributeTable.getSelectionModel().setSelectionInterval(0, attributeTableModel.getRowCount()-1);
                	}
                }
            });
            
            xmlTreeView.addKeyListener(new KeyListener() {
				
				@Override
				public void keyTyped(KeyEvent e) {
					// do nothing
				}
				
				@Override
				public void keyReleased(KeyEvent e) {
					// do nothing
				}
				
				@Override
				public void keyPressed(KeyEvent e) {
					if (e.getKeyCode() == KeyEvent.VK_ENTER) {
						attributeTable.requestFocusInWindow();
						e.consume();
					}
				}
			});
            
            xmlTreeView.addMouseListener(new MouseListener() {
				@Override
				public void mouseClicked(MouseEvent e) {
					if (e.getButton() == MouseEvent.BUTTON1 && e.getClickCount() == 2) {
						attributeTable.requestFocusInWindow();
						e.consume();
					}
				}

				@Override
				public void mousePressed(MouseEvent e) {
					// do nothing
				}

				@Override
				public void mouseReleased(MouseEvent e) {
					// do nothing
				}

				@Override
				public void mouseEntered(MouseEvent e) {
					// do nothing
				}

				@Override
				public void mouseExited(MouseEvent e) {
					// do nothing
				}
			});
        }
        
        
        // mid panel contains the add/remove buttons
        JPanel midBarPanel = new JPanel(new GridBagLayout());
        {
        	GridBagConstraints midBarConstraints = new GridBagConstraints();
        	midBarConstraints.fill = GridBagConstraints.BOTH;
        	midBarConstraints.weightx = 0;
        	midBarConstraints.weighty = 0;
        	midBarConstraints.gridx = GridBagConstraints.REMAINDER;
        	
            addButton = new JButton();
            addButton.setAction(new ResourceAction(true, "importwizard.xml.attribute_expression.add_xpath") {
				private static final long serialVersionUID = 1L;
				@Override
				public void actionPerformed(ActionEvent e) {
					addSelectedAttributes();
				}
			});
            midBarPanel.add(addButton, midBarConstraints);
            
        	deleteFromXPathTableButton = new JButton();
        	midBarConstraints.weighty = 0;
        	midBarPanel.add(deleteFromXPathTableButton, midBarConstraints);
        	
        	deleteFromXPathTableButton.setAction(new ResourceAction(false, "importwizard.xml.attribute_expression.remove_xpath") {
				private static final long serialVersionUID = 1L;
				@Override
				public void actionPerformed(ActionEvent e) {
					int[] selectedRows = xPathTable.getSelectedRows();
					
					// convert to model coordinates
					for (int i = 0; i < selectedRows.length; ++i) {
						selectedRows[i] = xPathTable.getModelIndex(selectedRows[i]);
					}
					
					attributeXPathModel.removeRows(selectedRows);
				}
			});
        	deleteFromXPathTableButton.setEnabled(false);
        }
        
        
        // right panel contains only the table with the selected XPath
        JPanel rightBarPanel = new JPanel(new GridBagLayout());
        {
            GridBagConstraints rightBarConstraints = new GridBagConstraints();
            rightBarConstraints.fill = GridBagConstraints.BOTH;
            rightBarConstraints.weightx = 1;
            rightBarConstraints.weighty = 0;
            rightBarConstraints.gridx = GridBagConstraints.REMAINDER;
            rightBarConstraints.insets = new Insets(0, 5, 5, 5);

            JLabel attributeXPathTableLabel = new JLabel(I18N.getGUILabel("importwizard.xml.attribute_expression.xpath_table_label"));
            rightBarPanel.add(attributeXPathTableLabel, rightBarConstraints);
        	
            rightBarConstraints.weighty = 1;
        	xPathTable = new ExtendedJTableWithErrorIndicator();
        	xPathTable.setModel(attributeXPathModel);
        	rightBarPanel.add(new ExtendedJScrollPane(xPathTable), rightBarConstraints);

        	// only select whole rows
            xPathTable.setCellSelectionEnabled(false);
            xPathTable.setRowSelectionAllowed(true);
        	
        	
        	xPathTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
				@Override
				public void valueChanged(ListSelectionEvent e) {
					deleteFromXPathTableButton.setEnabled(xPathTable.getSelectedRowCount() > 0);
				}
			});
        	
        	xPathTable.addKeyListener(new KeyListener() {
				@Override
				public void keyTyped(KeyEvent e) {
					// do nothing
				}
				
				@Override
				public void keyReleased(KeyEvent e) {
					// do nothing
				}
				
				@Override
				public void keyPressed(KeyEvent e) {
					if (e.getKeyCode() == KeyEvent.VK_DELETE) {
						int[] selectedRows = xPathTable.getSelectedRows();
						
						// convert to model coordinates
						for (int i = 0; i < selectedRows.length; ++i) {
							selectedRows[i] = xPathTable.getModelIndex(selectedRows[i]);
						}
						
						attributeXPathModel.removeRows(selectedRows);
						e.consume();
					}
				}
			});
        }
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(5, 5, 5, 5);
        c.fill = GridBagConstraints.BOTH;
        c.weighty = 1d;
        c.weightx = 0.3d;
        component.add(leftBarPanel, c);
        c.weightx = 0;
        component.add(midBarPanel, c);
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.weightx = 0.7d;
        component.add(rightBarPanel, c);
	}

	/**
	 * Return true iff xPathString is a valid XPath, i.e. can be compiled without errors.
	 * It is NOT checked if the XPath matches anything.
	 */
	public boolean isValidXPath(String xPathString) {
		if (xPathString == null) {
			return true;
		}
		XPath xPath = XPathFactory.newInstance().newXPath();
		try {
			XPathExpression expression = xPath.compile(xPathString);
		} catch (XPathExpressionException e) {
			return false;
		}
		return true;
	}

	@Override
	protected JComponent getComponent() {
		return component;
	}
	
	/**
	 * Initializes the models for the gui elements and makes sure that the correct button are enabled etc.
	 * 
	 * @see com.rapidminer.gui.tools.dialogs.wizards.WizardStep#performEnteringAction(com.rapidminer.gui.tools.dialogs.wizards.AbstractWizard.WizardStepDirection)
	 */
	@Override
    protected boolean performEnteringAction(WizardStepDirection direction) {
		XPath exampleXPath = XPathFactory.newInstance().newXPath();
		exampleXPath.setNamespaceContext(new MapBasedNamespaceContext(configuration.getNamespacesMap(), configuration.getDefaultNamespaceURI()));
		
		
		
        try {
    		XPathExpression exampleExpression = exampleXPath.compile(configuration.getExampleXPath());
    		this.elementMatches = (NodeList) exampleExpression.evaluate(configuration.getDocumentObjectModel(), XPathConstants.NODESET);

            xmlTreeView.setNamespacesMap(configuration.getNamespacesMap());
    		setCurrentMatch(0);
        } catch (OperatorException e) {
        	// should not happen, because the user can only proceed to this step if the previous
        	// step produced a valid XPath-Expression
			e.printStackTrace();
        } catch (XPathExpressionException e) {
        	// should not happen, because the user can only proceed to this step if the previous
        	// step produced a valid XPath-Expression
			e.printStackTrace();
		}
        attributeXPathModel.setXPaths(configuration.getAttributeXPaths());
        return true;
    }

    /**
     * Selects the index-th element from the list of elements matching the XPath from the configuration object
     * as current element and updates the models of the xml element view and the attribute view accordingly.
     */
    private void setCurrentMatch(int index) {
    	int length = 0;
    	if (elementMatches != null) {
    		length = elementMatches.getLength();
    	}
    	if (index >= length-1) {
    		index = length-1;
    		nextMatchButton.setEnabled(false);
    	} else {
    		nextMatchButton.setEnabled(true);
    	}
    	if (index <= 0) {
    		previousMatchButton.setEnabled(false);
    	} else {
    		previousMatchButton.setEnabled(true);
    	}
    	
    	currentMatchIndex = index;
    	if (index >= 0) {
    		currentElement = (Element)elementMatches.item(index);
    	} else {
    		currentElement = null;
    	}
		xmlTreeView.setModel(new XMLTreeModel(currentElement, false));
		attributeTableModel.setElement(null);
		attributeXPathModel.updateValuesFromXPaths();
	}

	/**
	 * Writes the selected XPaths into the configuration object.
	 * @see com.rapidminer.gui.tools.dialogs.wizards.WizardStep#performLeavingAction(com.rapidminer.gui.tools.dialogs.wizards.AbstractWizard.WizardStepDirection)
	 */
	@Override
    protected boolean performLeavingAction(WizardStepDirection direction) {
		configuration.setAttributeXPaths(attributeXPathModel.getXPaths());
    	return true;
    }


	@Override
	protected boolean canProceed() {
		return !attributeXPathModel.hasErrors();
	}

	@Override
	protected boolean canGoBack() {
		return true;
	}

	/**
	 * Returns an XPath matching the xml attribute named attribute from the currently selected element
	 * in the xml tree view.
	 */
	private String getXPathForAttribute(String attribute) {
		StringBuilder builder = new StringBuilder();
		builder.append(XMLDomHelper.getXPath(currentElement, attributeTableModel.getElement(), true, configuration.getNamespacesMap()));						
		if (builder.length() > 0) {
			builder.append("/");
		}
		if (attribute.equals(AttributeTableModel.TEXT_NODE_TEXT)) {
			builder.append("text()");
		} else {
			builder.append("attribute::");
			builder.append(attribute);
		}
		String xPath = builder.toString();
		return xPath;
	}

	
	/**
	 * Adds the attributes which are currently selected in the attribute table to the XPath table
	 * and removes them from the attribute table.
	 */
	private void addSelectedAttributes() {
		int[] selectedRows = attributeTable.getSelectedRows();
		List<String> selectedAttributes = new LinkedList<String>();
		for (int i = 0; i < selectedRows.length; ++i) {
			int modelRow = attributeTable.getModelIndex(selectedRows[i]);
			String attribute = (String)attributeTableModel.getValueAt(modelRow, AttributeTableModel.ATTRIBUTE_COLUMN);
			selectedAttributes.add(attribute);
		}
		for (String attribute : selectedAttributes) {
			attributeXPathModel.addXPath(getXPathForAttribute(attribute));
		}
	}
}
