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
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;

import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.rapidminer.gui.tools.ExtendedJScrollPane;
import com.rapidminer.gui.tools.ExtendedJTable;
import com.rapidminer.gui.tools.dialogs.wizards.AbstractWizard;
import com.rapidminer.gui.tools.dialogs.wizards.AbstractWizard.WizardStepDirection;
import com.rapidminer.gui.tools.dialogs.wizards.WizardStep;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.tools.I18N;

/**
 * This wizard steps lets the user specify the mapping of namespace URIs to namespace ids.
 * 
 * @author Marius Helf
 *
 */
public class XMLNamespaceMapWizardStep extends WizardStep {

	private static final String NO_DEFAULT_NAMESPACE = "<none>";
	private XMLResultSetConfiguration configuration;
	private JPanel component = new JPanel(new GridBagLayout());
	private JComboBox defaultNamespaceComboBox = new JComboBox();
	private NamespaceMapTableModel namespaceMapModel;
	private JLabel statusLabel;

	public XMLNamespaceMapWizardStep(AbstractWizard parent,  final XMLResultSetConfiguration configuration) {
		super("importwizard.xml.namespace_mapping");
		this.configuration = configuration;
		
		// init model for namespace map and add action listener
		namespaceMapModel = new NamespaceMapTableModel(null);
		// fire state changed whenever the namespace map table changes
		namespaceMapModel.addTableModelListener(new TableModelListener() {
			@Override
			public void tableChanged(TableModelEvent e) {
				fireStateChanged();
			}
		});
		
		// add components
		ExtendedJTable namespaceMapTable = new ExtendedJTable();
		namespaceMapTable.setModel(namespaceMapModel);
		GridBagConstraints gridConstraint = new GridBagConstraints();
		gridConstraint.insets = new Insets(0, 5, 5, 5);
		gridConstraint.fill = GridBagConstraints.BOTH;
		gridConstraint.weightx = 1;
		gridConstraint.weighty = 1;
		gridConstraint.gridwidth = GridBagConstraints.REMAINDER;
		component.add(new ExtendedJScrollPane(namespaceMapTable), gridConstraint);
		
		// init default namespace controls
		gridConstraint.gridwidth = 1;
		gridConstraint.weightx = 0;
		gridConstraint.weighty = 0;

		component.add(new JLabel(I18N.getGUILabel("importwizard.xml.namespace_mapping.default_namespace")), gridConstraint);
		gridConstraint.weightx = 1;
		gridConstraint.gridwidth = GridBagConstraints.REMAINDER;
		component.add(defaultNamespaceComboBox, gridConstraint);
		
		statusLabel = new JLabel("");
		component.add(statusLabel);
		
		this.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				try {
					configuration.getDocumentObjectModel();
				} catch (OperatorException e1) {
					statusLabel.setText(I18N.getGUILabel("xml_reader.wizard.cannot_load_dom"));
					statusLabel.setForeground(Color.RED);
					return;
				}
				if (namespaceMapModel.getIdNamespaceMap().size() != namespaceMapModel.getRowCount()) {
					// duplicate or undefined namespace prefix
					statusLabel.setText(I18N.getGUILabel("xml_reader.wizard.undefined_or_duplicate_namespace_prefix"));
					statusLabel.setForeground(Color.RED);
					return;
				} else if (defaultNamespaceComboBox.getSelectedItem() == null) {
					// no default namespace
					statusLabel.setText(I18N.getGUILabel("xml_reader.wizard.undefined_default_namespace"));
					statusLabel.setForeground(Color.RED);
					return;
				}
				statusLabel.setText(I18N.getGUILabel("xml_reader.wizard.status_ok"));
				statusLabel.setForeground(Color.BLACK);
			}
		});
	}

	@Override
	protected JComponent getComponent() {
		return component;
	}

	@Override
	protected boolean canProceed() {
		try {
			return namespaceMapModel.getIdNamespaceMap().size() == namespaceMapModel.getRowCount() && defaultNamespaceComboBox.getSelectedItem() != null && configuration.getDocumentObjectModel() != null;
		} catch (OperatorException e) {
			// if xml document cannot be read, return false:
			return false;
		}
	}

	@Override
	protected boolean canGoBack() {
		return true;
	}
	
	@Override
	protected boolean performEnteringAction(WizardStepDirection direction) {
		if (direction != WizardStepDirection.BACKWARD) {
			configuration.setNamespaceAware(true);
			// get all namespaces
			Element rootElement = null;
			try {
				rootElement = configuration.getDocumentObjectModel().getDocumentElement();
			} catch (OperatorException e) {
				// do nothing here, the error will be detected at the change listener triggered by
				// fireStateChanged() at the end of this method
			}
			
			// get namespace mappings from document
			Map<String,String> namespaceUriToIdMap = getNamespaces(rootElement);

			// get mapping from configuration 
			Map<String,String> namespaceIdToUriMap = configuration.getNamespacesMap();
			for(Entry<String,String> idToUri : namespaceIdToUriMap.entrySet()) {
				namespaceUriToIdMap.put(idToUri.getValue(), idToUri.getKey());
			}
			
			// search for default namespace
			String defaultNamespaceUri = null;
			if (configuration.getDefaultNamespaceURI() == null) {
				for(Entry<String,String> idToUri : namespaceUriToIdMap.entrySet()) {
					if (idToUri.getValue() == null) {
						defaultNamespaceUri = idToUri.getKey();
						break;
					}
				}
			}
			
			namespaceMapModel.initializeData(namespaceUriToIdMap);
			
			// init default namespace combobox
			defaultNamespaceComboBox.removeAllItems();
			defaultNamespaceComboBox.addItem(NO_DEFAULT_NAMESPACE);
			String[] namespaces = new String[0];
			namespaces = namespaceUriToIdMap.keySet().toArray(namespaces);
			Arrays.sort(namespaces);
			for (String namespace : namespaces) {
				defaultNamespaceComboBox.addItem(namespace);
			}
			if (configuration.getDefaultNamespaceURI() != null) {
				defaultNamespaceComboBox.setSelectedItem(configuration.getDefaultNamespaceURI());
			} else if (defaultNamespaceUri != null) {
				defaultNamespaceComboBox.setSelectedItem(defaultNamespaceUri);
			} else {
				defaultNamespaceComboBox.setSelectedItem(NO_DEFAULT_NAMESPACE);
			}
			fireStateChanged();
		}
		return true;
	}
	
	/**
	 * Returns a map containing all namespaces defined in element and (recursively) its child-elements as keys
	 * and the corresponding namespace id/prefix as value. 
	 * 
	 */
	protected Map<String,String> getNamespaces(Node node) {
		Map<String,String> namespaceUriToIdMap = new HashMap<String, String>();
		
		if (node == null) {
			return namespaceUriToIdMap;
		}
		String namespace = node.getNamespaceURI();
		if (namespace != null) {
			String id = node.getPrefix();
			if ((id != null && !id.isEmpty()) || !namespaceUriToIdMap.containsKey(namespace)) {
				namespaceUriToIdMap.put(namespace, id);
			}
		}
		NodeList children = node.getChildNodes();
		for (int i = 0; i < children.getLength(); ++i) {
			Node child = children.item(i);
			for (Entry<String,String> uriToId : getNamespaces(child).entrySet()) {
				if ((uriToId.getValue() != null && !uriToId.getValue().isEmpty()) || !namespaceUriToIdMap.containsKey(uriToId.getKey())) {
					namespaceUriToIdMap.put(uriToId.getKey(), uriToId.getValue());
				}
			}
		}
		
		// get namespaces from xmlns:xxx attributes 
		NamedNodeMap attributes = node.getAttributes();
		if (attributes != null ) {
			for (int i = 0; i < attributes.getLength(); ++i) {
				Node attributeNode = attributes.item(i);
				if (attributeNode instanceof Attr) {
					Attr attribute = (Attr)attributeNode;
					if (attribute.getPrefix() != null && attribute.getPrefix().equalsIgnoreCase("xmlns")) {
						String id = attribute.getLocalName();
						String namespaceFromAttribute = attribute.getValue();
						if ((id != null && !id.isEmpty()) || !namespaceUriToIdMap.containsKey(namespaceFromAttribute)) {
							namespaceUriToIdMap.put(namespaceFromAttribute, id);
						}
					}
				}

			}
		}
		return namespaceUriToIdMap;
	}

	@Override
	protected boolean performLeavingAction(WizardStepDirection direction) {
		configuration.setNamespacesMap(namespaceMapModel.getIdNamespaceMap());
		String selectedNamespace = (String)defaultNamespaceComboBox.getSelectedItem();
		if (selectedNamespace != NO_DEFAULT_NAMESPACE) {
			configuration.setDefaultNamespaceURI(selectedNamespace);
		} else {
			configuration.setDefaultNamespaceURI(null);
		}
		return true;
	}

}
