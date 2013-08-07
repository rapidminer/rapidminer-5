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
package com.rapidminer.tools.config.gui;

import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.UIManager;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import com.rapidminer.gui.tools.ExtendedJScrollPane;
import com.rapidminer.gui.tools.FilterableListModel;
import com.rapidminer.gui.tools.ResourceAction;
import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.gui.tools.dialogs.ButtonDialog;
import com.rapidminer.gui.tools.dialogs.ConfirmDialog;
import com.rapidminer.gui.tools.dialogs.MultiConfirmDialog;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.tools.I18N;
import com.rapidminer.tools.config.Configurable;
import com.rapidminer.tools.config.ConfigurationException;
import com.rapidminer.tools.config.ConfigurationManager;
import com.rapidminer.tools.config.Configurator;

/**
 * A dialog, providing access to {@link Configurable}s of a specified typeID. Offers options to create, delete or modify {Configurable}s.
 * 
 * The I18N Keys for this dialog can be found at the {@link Configurable} interface.
 * 
 * @author Dominik Halfkann
 *
 * @param <T> A subclass of {@link Configurable} which should be configured through the dialog.
 */
public class ConfigurationDialog<T extends Configurable> extends ButtonDialog {

	private static final long serialVersionUID = 1L;
	
	public static Action getOpenWindowAction(String typeID) {
		final String finalTypeID = typeID;
		Action OPEN_WINDOW = new ResourceAction(true, "configuration." + ConfigurationManager.getInstance().getConfigurator(typeID).getI18NBaseKey()) {
			{
				setCondition(EDIT_IN_PROGRESS, DONT_CARE);
			}
			private static final long serialVersionUID = 1L;
			@SuppressWarnings("unchecked")
			@Override
			public void actionPerformed(ActionEvent e) {
				Configurator config = ConfigurationManager.getInstance().getConfigurator(finalTypeID);
				new ConfigurationDialog(config).setVisible(true);
			}
		};
		return OPEN_WINDOW;
	}
	
	
	private static final Color TEXT_SELECTED_COLOR = UIManager.getColor("Tree.selectionForeground");

	private static final Color TEXT_NON_SELECTED_COLOR = UIManager.getColor("Tree.textForeground");
	
	private Icon entryIcon = null;
	
	private Icon entryReadOnlyIcon = null;
	
	private String lastSelectedName = "";
	
	private final FilterableListModel model = new FilterableListModel();
	
	{
		Comparator<Object> comparator = new Comparator<Object>() {

			@Override
			public int compare(Object o1, Object o2) {
				if (!(o1 instanceof Configurable) || !(o2 instanceof Configurable)) {
					return o1.toString().compareTo(o2.toString());
				}
				// sort lexicographically, but make sure read only connections are listed below all normal connections
				Configurable co1 = (Configurable)o1;
				Configurable co2 = (Configurable)o2;
				/*if (co1.isReadOnly() && !co2.isReadOnly()) {
					return 1;
				} else if (!co1.isReadOnly() && co2.isReadOnly()) {
					return -1;
				} else {*/
					return co1.getName().compareTo(co2.getName());
				//}
			}
		};
		model.setComparator(comparator);
	}
	
	private String I18NKey = "";
	
	private Configurator<T> configurator;
	
	/** this is a clone of the entry which is currently being edited */
	private T currentlyEditedEntry = null;
	
	private boolean hideListChangeConfirmationDialog = false;
	
	private boolean entrySaved = false;
	
	private boolean isNewEntry = false;
	
	private ConfigurationPanel<? super T> configurationPanel = null; 

	public ConfigurationDialog(Configurator<T> configurator) {
		super("configuration."+configurator.getI18NBaseKey());
		Collection<AbstractButton> buttons = makeButtons();
		this.I18NKey = "configuration."+configurator.getI18NBaseKey();
		this.configurator = configurator;
		this.configurationPanel = configurator.createConfigurationPanel();
		setI18NVariables(this.I18NKey);
		this.setModal(true);
		
		JPanel allButtonPanel = new JPanel(new GridBagLayout());
		GridBagConstraints gbc = new GridBagConstraints();
		JPanel entryButtonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, GAP, GAP));
		for (AbstractButton button : buttons) {
			if (button != null) {
				entryButtonPanel.add(button);
			}
		}
		
		JPanel generalButtonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, GAP, GAP));
		JButton closeButton = makeCloseButton();
		closeButton.setAction(new ResourceAction("close") {
			private static final long serialVersionUID = 1L;
			@Override
			public void actionPerformed(ActionEvent e) {
				try {
					if (checkUnsavedChanges()) {
						close();
					}
				} catch (ConfigurationException e1) {
					SwingTools.showSimpleErrorMessage("configuration.dialog.general", e1, e1.getMessage());
					close();
				}
			}			
		});

		generalButtonPanel.add(closeButton);
		
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.fill = GridBagConstraints.NONE;
		gbc.weightx = 0;
		allButtonPanel.add(entryButtonPanel, gbc);
		
		gbc.gridx = 1;
		gbc.gridy = 0;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.weightx = 1;
		allButtonPanel.add(new JLabel(), gbc);
		
		gbc.gridx = 2;
		gbc.gridy = 0;
		gbc.fill = GridBagConstraints.NONE;
		gbc.weightx = 0;
		allButtonPanel.add(generalButtonPanel, gbc);
		
		layoutDefault(makeConfigurableManagementPanel(), allButtonPanel, LARGE);
		
		//model.addElement(configurator.getParameterTypes().get(0));
		setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
		this.addWindowListener(new WindowAdapter() {
		      public void windowClosing(WindowEvent e) {
		    	  try {
		    		  if (checkUnsavedChanges()) {
						  close();
					  }
		    	  } catch (ConfigurationException e1) {
		    		  SwingTools.showSimpleErrorMessage("configuration.dialog.general", e1, e1.getMessage());
		    		  close();
		    	  }
		        }
		      });
		
		// load entries
		loadEntries();
		
		CLONE_ENTRY_ACTION.setEnabled(false);
		DELETE_ENTRY_ACTION.setEnabled(false);
	}
	
	private void loadEntries() {
		List<String> entryNames = ConfigurationManager.getInstance().getAllConfigurableNames(configurator.getTypeId());
		for (String entryName : entryNames) {
			try {
				addEntries(ConfigurationManager.getInstance().lookup(configurator.getTypeId(), entryName, null));
			} catch (ConfigurationException e) {
				SwingTools.showSimpleErrorMessage("configuration.dialog.general", e, e.getMessage());
				return;
			}
		}
	}
	
	private void setI18NVariables(String baseKey) {
		//entryIcon = SwingTools.createIcon("16/" + I18N.getMessage(I18N.getGUIBundle(), "gui.dialog." + baseKey + ".connection_entry.icon"));
		entryIcon = SwingTools.createIcon("16/" + configurator.getIconName());
		entryReadOnlyIcon = SwingTools.createIcon("16/lock.png");		
	}
	
	public void addEntries(Configurable configurable) {
		model.addElement(configurable);
		
	}
	
	public JPanel makeConfigurableManagementPanel() {
		JPanel panel = new JPanel(createGridLayout(1, 2));
		JScrollPane configurableListPane = new ExtendedJScrollPane(configurableList);
		configurableListPane.setBorder(createTitledBorder(I18N.getMessage(I18N.getGUIBundle(), "gui.border." + I18NKey + ".list")));
		panel.add(configurableListPane);
		//panel.add(new JPanel());
		JComponent configPanel = configurationPanel.getComponent();
		configPanel.setBorder(createTitledBorder(I18N.getMessage(I18N.getGUIBundle(), "gui.border." + I18NKey + ".configuration")));
		panel.add(configPanel);
		return panel;
	}
	
	private final JList configurableList = new JList(model);
	{
		configurableList.setCellRenderer(new DefaultListCellRenderer() {
			private static final long serialVersionUID = 4616183160018529751L;
			
			@Override
			public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
				JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
				if (isSelected) {
					label.setForeground(TEXT_SELECTED_COLOR);
				} else {
					label.setForeground(TEXT_NON_SELECTED_COLOR);					
				}
				if (value instanceof Configurable) {
					Configurable entry = (Configurable) value;
					String remoteSource = (entry.getSource() != null) ? "<br/>Taken from: " + entry.getSource().toHtmlString() : "";
					String shortInfo = (entry.getShortInfo() != null) ?  " (" + entry.getShortInfo() + ")" : "";
					label.setText("<html>" + entry.getName() + "<small>" + shortInfo + remoteSource + "</small></html>");
					label.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
					if (entry.getSource() != null) {
						label.setIcon(entryReadOnlyIcon);
					} else {
						label.setIcon(entryIcon);						
					}
				}
				return label;
			}
		});
		configurableList.addListSelectionListener(new ListSelectionListener() {
			@Override
			public void valueChanged(ListSelectionEvent e) {
				boolean selected = configurableList.getSelectedValue() != null;
				if (selected) {
					if (hideListChangeConfirmationDialog) {
						// Directly open the selected value
						openEntry(getSelectedEntry());
					} else if (currentlyEditedEntry == null) {
						// The first time the user selects an entry
						try {
							if (hasUnsavedChanges()) {
								SwingTools.showVerySimpleErrorMessage("configuration.dialog.demandsave", getConfigurableFromInputFields().getName());
								configurableList.clearSelection();
								return;
							} else {
								openEntry(getSelectedEntry());
							}
						} catch (ConfigurationException e1) {
							SwingTools.showSimpleErrorMessage("configuration.dialog.general", e1, e1.getMessage());
							configurableList.clearSelection();
							return;
						}
					} else if (!lastSelectedName.equals(((Configurable)configurableList.getSelectedValue()).getName()) && !currentlyEditedEntry.equals((Configurable) configurableList.getSelectedValue())) {
						// The user selected an entry which differs from the previous selected entry
						if (isNewEntry) {
							// The user wants to switch from an unsaved, new entry. Demand a save!
							try {
								if (hasUnsavedChanges()) {
									int saveBeforeOpen = SwingTools.showConfirmDialog("configuration.dialog.savecurrent", ConfirmDialog.YES_NO_CANCEL_OPTION, currentlyEditedEntry.getName());
									if (saveBeforeOpen == ConfirmDialog.YES_OPTION) {
										// YES: Save entry, then open the new selected entry
										T newSelectEntry = getSelectedEntry();
										entrySaved = false;
										SAVE_ENTRY_ACTION.actionPerformed(null);
										if (entrySaved) {
											openEntry(newSelectEntry);
											configurableList.setSelectedValue(newSelectEntry, true);
										} else {
											configurableList.setSelectedValue(currentlyEditedEntry, true);
											return;
										}
									} else if (saveBeforeOpen == ConfirmDialog.NO_OPTION) {
										// NO: Delete entry, then open the new selected entry
										T tempEntry = currentlyEditedEntry;
										openEntry(getSelectedEntry());
										model.removeElement(tempEntry);
										isNewEntry = false;
										
									} else if (saveBeforeOpen == ConfirmDialog.CANCEL_OPTION) {
										configurableList.setSelectedValue(currentlyEditedEntry, true);
										return;
									}
								} else {
									openEntry(getSelectedEntry());
								}
							} catch (ConfigurationException e2) {
								SwingTools.showSimpleErrorMessage("configuration.dialog.general", e2, e2.getMessage());
								configurableList.setSelectedValue(currentlyEditedEntry, true);
								return;
							}
						} else {
							// In case the entry is already existing
							try {
								if (hasUnsavedChanges()) {
									int saveBeforeOpen = SwingTools.showConfirmDialog("configuration.dialog.savecurrent", ConfirmDialog.YES_NO_CANCEL_OPTION, currentlyEditedEntry.getName());
									if (saveBeforeOpen == ConfirmDialog.YES_OPTION) {
										// YES: Save entry, then open the new selected entry
										T newSelectEntry = getSelectedEntry();
										entrySaved = false;
										SAVE_ENTRY_ACTION.actionPerformed(null);
										if (entrySaved) {
											openEntry(newSelectEntry);
											configurableList.setSelectedValue(newSelectEntry, true);
										} else {
											configurableList.setSelectedValue(currentlyEditedEntry, true);
											return;
										}
									} else if (saveBeforeOpen == ConfirmDialog.NO_OPTION) {
										// NO: Discard changed, then open the new selected entry
										openEntry(getSelectedEntry());
									} else if (saveBeforeOpen == ConfirmDialog.CANCEL_OPTION) {
										// Cancel: Do nothing and set the selection back to the previous selected entry
										configurableList.setSelectedValue(currentlyEditedEntry, true);
										return;
									}
								} else {
									openEntry(getSelectedEntry());
								}
							} catch (ConfigurationException e2) {
								SwingTools.showSimpleErrorMessage("configuration.dialog.general", e2, e2.getMessage());
								configurableList.setSelectedValue(currentlyEditedEntry, true);
								return;
							}
						}
					} else {
						// same element, do nothing
						return;
					}
				
				}
				
				NEW_ENTRY_ACTION.setEnabled(selected);
				CLONE_ENTRY_ACTION.setEnabled(selected);
				DELETE_ENTRY_ACTION.setEnabled(selected);
			}


		});
	}

	
	public Collection<AbstractButton> makeButtons() {
		Collection<AbstractButton> list = new LinkedList<AbstractButton>();
		list.add(new JButton(SAVE_ENTRY_ACTION));
		list.add(new JButton(NEW_ENTRY_ACTION));
		list.add(new JButton(CLONE_ENTRY_ACTION));
		list.add(new JButton(DELETE_ENTRY_ACTION));
		return list;
	}
	
	/** Opens a new entry, updates the configuration panel with the new entry's values **/
	private void openEntry(T entry) {
		configurationPanel.updateComponents(entry);
		SAVE_ENTRY_ACTION.setEnabled(entry.getSource() == null);
		// Update currently edited entry
		currentlyEditedEntry = entry;
		//isNewEntry = false;
	}
	
	protected final Action SAVE_ENTRY_ACTION = new ResourceAction("configuration.dialog.save") {
		private static final long serialVersionUID = -8477647509533859436L;

		@Override
		public void actionPerformed(ActionEvent e) {
			// First let the ConfigurationPanel check the fields and display error messages
			if (configurationPanel.checkFields()) {
				T inputFieldEntry = null;
				try {
					inputFieldEntry = getConfigurableFromInputFields();
				} catch (ConfigurationException e2) {
					SwingTools.showSimpleErrorMessage("configuration.dialog.general", e2, e2.getMessage());
					return;
				}
				
				// check if entry with same name already exists
				Configurable sameNameEntry = null;
				for (int i = 0; i < model.getSize(); i++) {
					Configurable compareEntry = (Configurable) model.getElementAt(i);
					if (compareEntry.getName().equals(inputFieldEntry.getName())) {
						sameNameEntry = compareEntry; 
						break;
					}
				}
				if (sameNameEntry == null || sameNameEntry.equals(currentlyEditedEntry)) {
					// unique or unchanged name, overwrite currently edited entry if applicable
					if (currentlyEditedEntry != null) {
						model.removeElement(currentlyEditedEntry);
						ConfigurationManager.getInstance().removeConfigurable(configurator.getTypeId(), currentlyEditedEntry.getName());
					}
					
					Configurable newItem = null;
					try {
						newItem = ConfigurationManager.getInstance().create(configurator.getTypeId(), inputFieldEntry.getName());
					} catch (ConfigurationException e1) {
						SwingTools.showSimpleErrorMessage("configuration.dialog.general", e1, e1.getMessage());
						return;
					}
					
					for (ParameterType parameter : configurator.getParameterTypes()) {
						newItem.setParameter(parameter.getKey(), inputFieldEntry.getParameters().get(parameter.getKey()));
					}
					
					model.addElement(inputFieldEntry);
					configurableList.clearSelection();
					
					hideListChangeConfirmationDialog = true;
					configurableList.setSelectedValue(inputFieldEntry, true);
					hideListChangeConfirmationDialog = false;
					
					entrySaved = true;
					isNewEntry = false;
				} else {
					// name already in use by another connection, ask for overwrite and then remove the overwritten entry
					if (SwingTools.showConfirmDialog("configuration.dialog.overwrite", ConfirmDialog.YES_NO_OPTION, inputFieldEntry.getName()) == ConfirmDialog.YES_OPTION) {
						model.removeElement(sameNameEntry);
						ConfigurationManager.getInstance().removeConfigurable(configurator.getTypeId(), sameNameEntry.getName());
						if (currentlyEditedEntry != null) {
							model.removeElement(currentlyEditedEntry);
							ConfigurationManager.getInstance().removeConfigurable(configurator.getTypeId(), currentlyEditedEntry.getName());
						}
						
						Configurable newItem = null;
						try {
							newItem = ConfigurationManager.getInstance().create(configurator.getTypeId(), inputFieldEntry.getName());
						} catch (ConfigurationException e1) {
							SwingTools.showSimpleErrorMessage("configuration.dialog.general", e1, e1.getMessage());
							return;
						}
						
						for (ParameterType parameter : configurator.getParameterTypes()) {
							newItem.setParameter(parameter.getKey(), inputFieldEntry.getParameters().get(parameter.getKey()));
						}
						
						model.addElement(inputFieldEntry);
						configurableList.clearSelection();
						hideListChangeConfirmationDialog = true;
						configurableList.setSelectedValue(inputFieldEntry, true);
						hideListChangeConfirmationDialog = false;
						
						entrySaved = true;
						isNewEntry = false;
					}
					
				}
				
				// after all is done, save the new configuration
				save();
				
				NEW_ENTRY_ACTION.setEnabled(true);
			}
		}
	};
	
	private final Action NEW_ENTRY_ACTION = new ResourceAction("configuration.dialog.new") {
		private static final long serialVersionUID = 7979548709619302219L;
		
		@Override
		public void actionPerformed(ActionEvent e) {
			String alias = "New item ";
			boolean unique = false;
			int appendIndex = 1;
			do {
				for (int i = 0; i < model.getSize(); i++) {
					unique = true;
					Configurable compareEntry = (Configurable) model.getElementAt(i);
					if (compareEntry.getName().equals(alias + appendIndex)) {
						unique = false;
						appendIndex++;
						break;
					}
				}
				// do as often as needed until we have a unique name (model must have elements otherwise we have an infinite loop
			} while(!unique && model.getSize() > 0);
			
			T newEntry = null;
			try {
				newEntry = configurator.create(alias + appendIndex, Collections.<String,String>emptyMap());
			} catch (ConfigurationException e1) {
				SwingTools.showSimpleErrorMessage("configuration.dialog.general", e1, e1.getMessage());
				return;
			}

			model.addElement(newEntry);
			configurableList.setSelectedValue(newEntry, true);
			isNewEntry = true;
			openEntry(newEntry);
			
			NEW_ENTRY_ACTION.setEnabled(false);
			CLONE_ENTRY_ACTION.setEnabled(false);
		}
	};
	
	private final Action CLONE_ENTRY_ACTION = new ResourceAction("configuration.dialog.clone") {
		private static final long serialVersionUID = -6286464201049577441L;

		@Override
		public void actionPerformed(ActionEvent e) {
			
			Object value = configurableList.getSelectedValue();
			if (value instanceof Configurable) {
				Configurable selectedEntry = (Configurable) value;
				
				String alias = "Copy of " + selectedEntry.getName();
				boolean unique = false;
				int copyIndex = 0;
				do {
					for (int i = 0; i < model.getSize(); i++) {
						unique = true;
						Configurable compareEntry = (Configurable) model.getElementAt(i);
						if (compareEntry.getName().equals(alias)) {
							unique = false;
							copyIndex++;
							alias = "Copy(" + copyIndex + ") of " + selectedEntry.getName();
							break;
						}
					}
					
				} while(!unique);
				
				Configurable newEntry;
				try {
					newEntry = ConfigurationManager.getInstance().create(configurator.getTypeId(), alias);
				} catch (ConfigurationException e1) {
					SwingTools.showSimpleErrorMessage("configuration.dialog.general", e1, e1.getMessage());
					return;
				}
				
				for (Map.Entry<String, String> parameter : selectedEntry.getParameters().entrySet()) {
					newEntry.setParameter(parameter.getKey(), parameter.getValue());
				}
				
				model.addElement(newEntry);
				save();
				configurableList.setSelectedValue(newEntry, true);
			}
		}
	};

	
	
	private final Action DELETE_ENTRY_ACTION = new ResourceAction("configuration.dialog.delete") {
		private static final long serialVersionUID = 1155260480975020776L;

		@Override
		public void actionPerformed(ActionEvent e) {
			
			boolean deletedEntry = false;
			
			Object[] selectedValues = configurableList.getSelectedValues();
			boolean applyToAll = false;
			int returnOption = ConfirmDialog.CANCEL_OPTION;
			for (int i = 0; i < selectedValues.length; i++) {
				Configurable entry = (Configurable) selectedValues[i];
				if (!applyToAll) {
					MultiConfirmDialog dialog = new MultiConfirmDialog("configuration.dialog.delete", ConfirmDialog.YES_NO_CANCEL_OPTION, entry.getName());
					dialog.setVisible(true);
					applyToAll = dialog.applyToAll();
					returnOption = dialog.getReturnOption();
				}
				if (returnOption == ConfirmDialog.CANCEL_OPTION) {
					break;
				}
				if (returnOption == ConfirmDialog.YES_OPTION) {
					model.removeElement(entry);
					ConfigurationManager.getInstance().removeConfigurable(configurator.getTypeId(), entry.getName());
					isNewEntry = false;
					NEW_ENTRY_ACTION.setEnabled(true);
					deletedEntry = true;
					
					configurableList.clearSelection();
					for (int j = 0; j < selectedValues.length; j++) {
						int index = model.indexOf(selectedValues[j]);
						configurableList.getSelectionModel().addSelectionInterval(index, index);
					}
				}
			}
			if (deletedEntry) {
				if (configurableList.getModel().getSize() > 0) {
					hideListChangeConfirmationDialog = true;
					configurableList.setSelectedIndex(0);
					hideListChangeConfirmationDialog = false;
				} else {
					openEntry(null);
				}
				save();
			}
			
		}

	};
	
	/** Checks if the user changed any values or have an unsaved Item opened and asks him to save those changes before closing the dialog.
	 * Returns true if the dialog can be closed, false otherwise. 
	 * @throws ConfigurationException **/
	protected boolean checkUnsavedChanges() throws ConfigurationException {
		//T inputFieldEntry = getConfigurableFromInputFields();
		if (hasUnsavedChanges()) {
			// If the user has changed any values / new unsaved entry
			int saveBeforeOpen = SwingTools.showConfirmDialog("configuration.dialog.savecurrent", ConfirmDialog.YES_NO_CANCEL_OPTION, getConfigurableFromInputFields().getName());
			if (saveBeforeOpen == ConfirmDialog.YES_OPTION) {
				// YES: Speichere alles
				entrySaved = false;
				SAVE_ENTRY_ACTION.actionPerformed(null);
				if (entrySaved) {
					return true;
				} else {
					return false;
				}
			} else if (saveBeforeOpen == ConfirmDialog.NO_OPTION) {
				// NO: Verwerfe alles
				return true;
			} else if (saveBeforeOpen == ConfirmDialog.CANCEL_OPTION) {
				return false;
			}
		}
		return true;
	}
	
	
	/** Returns true if the user has changed any values and hasn't saved them, false otherwise **/
    private boolean hasUnsavedChanges() throws ConfigurationException {
    	if (currentlyEditedEntry == null) {
			// The first time the user selects an entry, make sure everything is empty or the default value
    		if (getConfigurableFromInputFields().isEmptyOrDefault(configurator)) {
    			return false;
    		} else {
    			return true;
    		}
		} else {
			if (isNewEntry) {
				// If it's a new entry, it needs to be saved
				return true;
			} else {
				// If it's an entry which existed before, check if the values have changed
				T inputFieldsEntry = getConfigurableFromInputFields();
				if (currentlyEditedEntry.hasSameValues(inputFieldsEntry)) {
					return false;
				} else {
					return true;
				}
				
			}
		}
    }
	
    /** Resolves the Configurable which derives from the input field values **/
	protected T getConfigurableFromInputFields() throws ConfigurationException {
		T tempEntry = configurator.create("Temp", Collections.<String,String>emptyMap());;
		configurationPanel.updateConfigurable(tempEntry);
		return tempEntry;
		//SwingTools.showSimpleErrorMessage("configuration.dialog.general", e1, e1.getMessage());
	}	
	
	private T getSelectedEntry() {
		return configurator.getConfigurableClass().cast(configurableList.getSelectedValue());
	}
	

	private void save() {
		try {
			ConfigurationManager.getInstance().saveConfiguration(configurator.getTypeId());
		} catch (ConfigurationException e) {
			SwingTools.showSimpleErrorMessage("configuration.dialog.failed_to_save_configurable", e, configurator.getName(), e.getMessage());
		}
	}


}
