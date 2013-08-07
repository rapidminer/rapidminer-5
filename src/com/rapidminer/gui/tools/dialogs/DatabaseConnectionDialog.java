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
package com.rapidminer.gui.tools.dialogs;

import java.awt.Color;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.DriverPropertyInfo;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.Properties;
import java.util.logging.Level;

import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.UIManager;
import javax.swing.border.EtchedBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import com.rapidminer.gui.tools.ExtendedJScrollPane;
import com.rapidminer.gui.tools.FilterableListModel;
import com.rapidminer.gui.tools.ProgressThread;
import com.rapidminer.gui.tools.ResourceAction;
import com.rapidminer.gui.tools.ResourceLabel;
import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.gui.tools.components.FixedWidthLabel;
import com.rapidminer.tools.I18N;
import com.rapidminer.tools.LogService;
import com.rapidminer.tools.jdbc.DatabaseService;
import com.rapidminer.tools.jdbc.JDBCProperties;
import com.rapidminer.tools.jdbc.connection.ConnectionEntry;
import com.rapidminer.tools.jdbc.connection.DatabaseConnectionService;
import com.rapidminer.tools.jdbc.connection.FieldConnectionEntry;

/**
 * In this dialog, the user can manage all Database connections, including creation, editing and deletion of connections.
 * 
 * @author Tobias Malbrecht, Marco Boeck
 */
public class DatabaseConnectionDialog extends ButtonDialog {
	private static final long serialVersionUID = -2046390670591412166L;
	
	private static final String TEXT_CONNECTION_STATUS_UNKNOWN = I18N.getMessage(I18N.getGUIBundle(), "gui.dialog.manage_db_connections.status.unknown.label");
	
	private static final String TEXT_CONNECTION_STATUS_OK = I18N.getMessage(I18N.getGUIBundle(), "gui.dialog.manage_db_connections.status.ok.label");
	
	private static final Icon ICON_CONNECTION_STATUS_UNKNOWN = SwingTools.createIcon("16/" + I18N.getMessage(I18N.getGUIBundle(), "gui.dialog.manage_db_connections.status.unknown.icon"));
	
	private static final Icon ICON_CONNECTION_STATUS_OK = SwingTools.createIcon("16/" + I18N.getMessage(I18N.getGUIBundle(), "gui.dialog.manage_db_connections.status.ok.icon"));
	
	private static final Icon ICON_CONNECTION_STATUS_ERROR = SwingTools.createIcon("16/" + I18N.getMessage(I18N.getGUIBundle(), "gui.dialog.manage_db_connections.status.error.icon"));
	
	private static final Color TEXT_SELECTED_COLOR = UIManager.getColor("Tree.selectionForeground");

	private static final Color TEXT_NON_SELECTED_COLOR = UIManager.getColor("Tree.textForeground");
	
	private final FilterableListModel model = new FilterableListModel();
	
	{
		Comparator<Object> comparator = new Comparator<Object>() {

			@Override
			public int compare(Object o1, Object o2) {
				if (!(o1 instanceof ConnectionEntry) || !(o2 instanceof ConnectionEntry)) {
					return o1.toString().compareTo(o2.toString());
				}
				// sort lexicographically, but make sure read only connections are listed below all normal connections
				ConnectionEntry co1 = (ConnectionEntry)o1;
				ConnectionEntry co2 = (ConnectionEntry)o2;
				if (co1.isReadOnly() && !co2.isReadOnly()) {
					return 1;
				} else if (!co1.isReadOnly() && co2.isReadOnly()) {
					return -1;
				} else {
					return co1.toString().compareTo(co2.toString());
				}
			}
		};
		model.setComparator(comparator);
		for (ConnectionEntry entry : DatabaseConnectionService.getConnectionEntries()) {
			model.addElement(entry);
		}
	}
	
	private final JList connectionList = new JList(model);
	{
		connectionList.setCellRenderer(new DefaultListCellRenderer() {
			private static final long serialVersionUID = 4616183160018529751L;
			
			private final Icon entryIcon = SwingTools.createIcon("16/" + I18N.getMessage(I18N.getGUIBundle(), "gui.dialog.manage_db_connections.connection_entry.icon"));
			private final Icon entryReadOnlyIcon = SwingTools.createIcon("16/" + I18N.getMessage(I18N.getGUIBundle(), "gui.dialog.manage_db_connections.connection_readonly_entry.icon"));

			@Override
			public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
				JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
				if (isSelected) {
					label.setForeground(TEXT_SELECTED_COLOR);
				} else {
					label.setForeground(TEXT_NON_SELECTED_COLOR);					
				}
				if (value instanceof FieldConnectionEntry) {
					FieldConnectionEntry entry = (FieldConnectionEntry) value;
					String remoteRepo = (entry.getRepository() != null) ? "<br/>Taken from: " + entry.getRepository() : "";
					label.setText("<html>" + entry.getName() + " <small>(" + entry.getProperties().getName() + "; " + entry.getHost() + ":" + entry.getPort() + ")" + remoteRepo + "</small></html>");
					label.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
					if (entry.isReadOnly()) {
						label.setIcon(entryReadOnlyIcon);
					} else {
						label.setIcon(entryIcon);
					}
				}
				return label;
			}
		});
		connectionList.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if (e.getClickCount() == 2) {
					OPEN_CONNECTION_ACTION.actionPerformed(null);
				}
			}
		});
		connectionList.addListSelectionListener(new ListSelectionListener() {
			@Override
			public void valueChanged(ListSelectionEvent e) {
				boolean selected = connectionList.getSelectedValue() != null;
				OPEN_CONNECTION_ACTION.setEnabled(selected);
				CLONE_CONNECTION_ACTION.setEnabled(selected);
				
				// open delete only if not read only
				if (selected) {
					selected = !((FieldConnectionEntry)connectionList.getSelectedValue()).isReadOnly();
				}
				DELETE_CONNECTION_ACTION.setEnabled(selected);
			}
		});
	}
	
	private final JTextField aliasTextField = new JTextField(12);
	
	private final JComboBox databaseTypeComboBox = new JComboBox(DatabaseService.getDBSystemNames());
	
	private final JTextField hostTextField = new JTextField(12);
	
	private final JTextField portTextField = new JTextField(4);
	
	private final JTextField databaseTextField = new JTextField(12);
	
	private final JTextField userTextField = new JTextField(12);
	
	private final JPasswordField passwordField = new JPasswordField(12);
	
	private final JTextField urlField = new JTextField(12);
	
	private final JLabel testLabel = new FixedWidthLabel(180, TEXT_CONNECTION_STATUS_UNKNOWN, ICON_CONNECTION_STATUS_UNKNOWN);
	
	{
		urlField.setEditable(false);
		databaseTypeComboBox.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
				updateDefaults();
				updateURL(null);
			}
		});
		KeyListener keyListener = new KeyListener() {
			@Override
			public void keyPressed(KeyEvent e) {}

			@Override
			public void keyReleased(KeyEvent e) {
				updateURL(null);
			}

			@Override
			public void keyTyped(KeyEvent e) {}
		};
		portTextField.addKeyListener(keyListener);
		hostTextField.addKeyListener(keyListener);
		databaseTextField.addKeyListener(keyListener);
		userTextField.addKeyListener(keyListener);
		passwordField.addKeyListener(keyListener);
	}
	
	private final Action OPEN_CONNECTION_ACTION = new ResourceAction("manage_db_connections.open") {
		private static final long serialVersionUID = 2451337494765496601L;

		@Override
		public void actionPerformed(ActionEvent e) {
			Object value = connectionList.getSelectedValue();
			if (value instanceof FieldConnectionEntry) {
				FieldConnectionEntry entry = (FieldConnectionEntry) value;
				// setting values of connection into fields
				aliasTextField.setText(entry.getName());
				databaseTypeComboBox.setSelectedItem(entry.getProperties().getName());
				hostTextField.setText(entry.getHost());
				portTextField.setText(entry.getPort());
				databaseTextField.setText(entry.getDatabase());
				userTextField.setText(entry.getUser());
				if (entry.getPassword() == null)
					passwordField.setText("");
				else
					passwordField.setText(new String(entry.getPassword()));

				// setting fields editable depending on entry's readonly flag
				aliasTextField.setEditable(!entry.isReadOnly());
				databaseTypeComboBox.setEnabled(!entry.isReadOnly());
				hostTextField.setEditable(!entry.isReadOnly());
				portTextField.setEditable(!entry.isReadOnly());
				databaseTextField.setEditable(!entry.isReadOnly());
				userTextField.setEditable(!entry.isReadOnly());
				passwordField.setEditable(!entry.isReadOnly());
				
				// disabling actions if needed
				SAVE_CONNECTION_ACTION.setEnabled(!entry.isReadOnly());
				SHOW_ADVANCED_PROPERTIES.setEnabled(!entry.isReadOnly());
				
				// updating URL
				updateURL(entry);
				
				// do not use the real entry, otherwise properties (not jdbc properties) will be set on the real one even without saving
				// using this clone is possible because equals is overwritten and just compares all values
				currentlyEditedEntry = new FieldConnectionEntry(entry.getName(), entry.getProperties(), entry.getHost(), entry.getPort(), entry.getDatabase(), entry.getUser(), entry.getPassword());
				currentlyEditedEntry.setConnectionProperties(entry.getConnectionProperties());
			}
		}
	};
	
	protected final Action SAVE_CONNECTION_ACTION = new ResourceAction("manage_db_connections.save") {
		private static final long serialVersionUID = -8477647509533859436L;

		@Override
		public void actionPerformed(ActionEvent e) {
			final FieldConnectionEntry entry = checkFields(true);
			if (entry != null) {
				// no longer clones the entry, but instead modifies the selected one (as one would expect)
				// clone moved to CLONE_CONNECTION_ACTION

				// check if entry with same name already exists
				ConnectionEntry sameNameEntry = null;
				for (int i = 0; i < model.getSize(); i++) {
					ConnectionEntry compareEntry = (ConnectionEntry) model.getElementAt(i);
					if (compareEntry.getName().equals(entry.getName())) {
						sameNameEntry = compareEntry; 
						break;
					}
				}
				if (sameNameEntry == null || (sameNameEntry != null && sameNameEntry.equals(currentlyEditedEntry))) {
					// unique name, overwrite currently edited entry if applicable
					if (currentlyEditedEntry != null) {
						DatabaseConnectionService.deleteConnectionEntry(currentlyEditedEntry);
						model.removeElement(currentlyEditedEntry);
					}
					model.addElement(entry);
					DatabaseConnectionService.addConnectionEntry(entry);
					connectionList.clearSelection();
					connectionList.setSelectedValue(entry, true);
					OPEN_CONNECTION_ACTION.actionPerformed(null);
				} else {
					// name already in use by another connection, ask for overwrite and then remove the overwritten entry
					if (SwingTools.showConfirmDialog("manage_db_connections.overwrite", ConfirmDialog.YES_NO_OPTION, entry.getName()) == ConfirmDialog.YES_OPTION) {
						DatabaseConnectionService.deleteConnectionEntry(sameNameEntry);
						model.removeElement(sameNameEntry);
						if (currentlyEditedEntry != null) {
							DatabaseConnectionService.deleteConnectionEntry(currentlyEditedEntry);
							model.removeElement(currentlyEditedEntry);
						}
						model.addElement(entry);
						DatabaseConnectionService.addConnectionEntry(entry);
						connectionList.clearSelection();
						connectionList.setSelectedValue(entry, true);
						OPEN_CONNECTION_ACTION.actionPerformed(null);
					}
				}
				
			}
		}
	};
	
	private final Action CLONE_CONNECTION_ACTION = new ResourceAction("manage_db_connections.clone") {
		private static final long serialVersionUID = -6286464201049577441L;

		@Override
		public void actionPerformed(ActionEvent e) {
			Object value = connectionList.getSelectedValue();
			if (value instanceof FieldConnectionEntry) {
				FieldConnectionEntry selectedEntry = (FieldConnectionEntry) value;
				
				String alias = "Copy of " + selectedEntry.getName();
				boolean unique = false;
				int copyIndex = 0;
				do {
					for (int i = 0; i < model.getSize(); i++) {
						unique = true;
						ConnectionEntry compareEntry = (ConnectionEntry) model.getElementAt(i);
						if (compareEntry.getName().equals(alias)) {
							unique = false;
							copyIndex++;
							alias = "Copy(" + copyIndex + ") of " + selectedEntry.getName();
							break;
						}
					}
					
				} while(!unique);
				final FieldConnectionEntry newEntry = new FieldConnectionEntry(alias, selectedEntry.getProperties(), selectedEntry.getHost(),selectedEntry.getPort(), selectedEntry.getDatabase(), selectedEntry.getUser(), selectedEntry.getPassword());
				newEntry.setConnectionProperties(selectedEntry.getConnectionProperties());
				model.addElement(newEntry);
				DatabaseConnectionService.addConnectionEntry(newEntry);
				connectionList.setSelectedValue(newEntry, true);
				OPEN_CONNECTION_ACTION.actionPerformed(null);
			}
		}
	};
	
	private final Action NEW_CONNECTION_ACTION = new ResourceAction("manage_db_connections.new") {
		private static final long serialVersionUID = 7979548709619302219L;

		@Override
		public void actionPerformed(ActionEvent e) {
			String alias = "New connection";
			boolean unique = false;
			int appendIndex = 1;
			do {
				for (int i = 0; i < model.getSize(); i++) {
					unique = true;
					ConnectionEntry compareEntry = (ConnectionEntry) model.getElementAt(i);
					if (compareEntry.getName().equals(alias + appendIndex)) {
						unique = false;
						appendIndex++;
						break;
					}
				}
				// do as often as needed until we have a unique name (model must have elements otherwise we have an infinite loop
			} while(!unique && model.getSize() > 0);
			final FieldConnectionEntry newEntry = new FieldConnectionEntry(alias + appendIndex, getJDBCProperties(), "localhost", getJDBCProperties().getDefaultPort(), "", "", "".toCharArray());
			model.addElement(newEntry);
			DatabaseConnectionService.addConnectionEntry(newEntry);
			connectionList.setSelectedValue(newEntry, true);
			OPEN_CONNECTION_ACTION.actionPerformed(null);
		}
	};
	
	private final Action DELETE_CONNECTION_ACTION = new ResourceAction("manage_db_connections.delete") {
		private static final long serialVersionUID = 1155260480975020776L;

		@Override
		public void actionPerformed(ActionEvent e) {
			Object[] selectedValues = connectionList.getSelectedValues();
			boolean applyToAll = false;
			int returnOption = ConfirmDialog.CANCEL_OPTION;
			for (int i = 0; i < selectedValues.length; i++) {
				ConnectionEntry entry = (ConnectionEntry) selectedValues[i];
				if (!applyToAll) {
					MultiConfirmDialog dialog = new MultiConfirmDialog("manage_db_connections.delete", ConfirmDialog.YES_NO_CANCEL_OPTION, entry.getName());
					dialog.setVisible(true);
					applyToAll = dialog.applyToAll();
					returnOption = dialog.getReturnOption();
				}
				if (returnOption == ConfirmDialog.CANCEL_OPTION) {
					break;
				}
				if (returnOption == ConfirmDialog.YES_OPTION) {
					DatabaseConnectionService.deleteConnectionEntry(entry);
					model.removeElement(entry);
					connectionList.clearSelection();
					for (int j = 0; j < selectedValues.length; j++) {
						int index = model.indexOf(selectedValues[j]);
						connectionList.getSelectionModel().addSelectionInterval(index, index);
					}
				}
			}
			if (connectionList.getModel().getSize() > 0) {
				connectionList.setSelectedIndex(0);
				OPEN_CONNECTION_ACTION.actionPerformed(null);
			}
		}
	};
	
	private final Action TEST_CONNECTION_ACTION = new ResourceAction("manage_db_connections.test") {
		private static final long serialVersionUID = -25485375154547037L;

		@Override
		public void actionPerformed(ActionEvent e) {
			ProgressThread t = new ProgressThread("test_database_connection") {
				@Override
				public void run() {
					getProgressListener().setTotal(100);
					getProgressListener().setCompleted(10);
					try {
						ConnectionEntry entry = checkFields(false);
						if (entry == null) {
							return;
						}
			            if (!DatabaseConnectionService.testConnection(entry)) {
			            	throw new SQLException();
			            }
			            testLabel.setText(TEXT_CONNECTION_STATUS_OK);
			            testLabel.setIcon(ICON_CONNECTION_STATUS_OK);
			        } catch (SQLException exception) {
			        	String errorMessage = exception.getLocalizedMessage();
			        	if (errorMessage.length() > 100) {
			        		errorMessage = exception.getLocalizedMessage().substring(0, 100) + "...";
			        	}
			        	testLabel.setText(errorMessage);
			            testLabel.setIcon(ICON_CONNECTION_STATUS_ERROR);
			        } finally {
			        	getProgressListener().complete();
			        }
				}
			};
			t.start();
		}
	};
	
	private final Action SHOW_ADVANCED_PROPERTIES = new ResourceAction("manage_db_connections.advanced") {
		private static final long serialVersionUID = 7641194296960014681L;

		@Override
		public void actionPerformed(ActionEvent e) {
			// no db connection exists
			if (currentlyEditedEntry == null) {
				return;
			}
			// get the properties of the selected driver (not the jdbc properties)
			DriverPropertyInfo[] propInfo = getPropertyInfos();
			if (propInfo == null) {
				SwingTools.showSimpleErrorMessage("db_driver_not_found", "", String.valueOf(databaseTypeComboBox.getSelectedItem()));
				return;
			}
			DatabaseAdvancedConnectionDialog advancedDiag = new DatabaseAdvancedConnectionDialog("db_connection_advanced", propInfo, currentlyEditedEntry.getConnectionProperties());
			advancedDiag.setVisible(true);
			Properties connectionProperties = advancedDiag.getConnectionProperties();
			if (connectionProperties != null) {
				currentlyEditedEntry.setConnectionProperties(connectionProperties);
			}
		}
		
	};
	
	/** this is a clone of the entry which is currently being edited */
	private FieldConnectionEntry currentlyEditedEntry = null;
	
	{
		OPEN_CONNECTION_ACTION.setEnabled(false);
		DELETE_CONNECTION_ACTION.setEnabled(false);
		CLONE_CONNECTION_ACTION.setEnabled(false);
	}
		
	public DatabaseConnectionDialog(String i18nKey, Object ... i18nArgs) {
		super(i18nKey, true, i18nArgs);
	}
	
	public Collection<AbstractButton> makeButtons() {
		Collection<AbstractButton> list = new LinkedList<AbstractButton>();
		list.add(new JButton(SAVE_CONNECTION_ACTION));
		list.add(new JButton(NEW_CONNECTION_ACTION));
		list.add(new JButton(CLONE_CONNECTION_ACTION));
		list.add(new JButton(DELETE_CONNECTION_ACTION));
		return list;
	}
	
	private JPanel makeConnectionPanel() {
		JPanel panel = new JPanel(new GridBagLayout());
		panel.setBorder(createTitledBorder(I18N.getMessage(I18N.getGUIBundle(), "gui.border.manage_db_connections.details")));
		GridBagConstraints c = new GridBagConstraints();
		
		
		c.gridx = 0;
		c.gridy = 0;
		c.weightx = 1;
		c.weighty = 0;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.insets = new Insets(0, GAP, 0, GAP);
		panel.add(new ResourceLabel("manage_db_connections.name"), c);
		
		c.gridx = 1;
		c.gridy = 0;
		c.weightx = 0;
		c.gridwidth = 2;
		c.gridheight = 2;
		c.fill = GridBagConstraints.NONE;
		panel.add(new JButton(SHOW_ADVANCED_PROPERTIES), c);
		
		c.gridx = 0;
		c.gridy = 1;
		c.insets = new Insets(0, GAP, GAP, GAP);
		c.gridwidth = 1;
		c.gridheight = 1;
		c.fill = GridBagConstraints.HORIZONTAL;
		panel.add(aliasTextField, c);
		
		c.gridx = 0;
		c.gridy = 2;
		c.weightx = 1;
		c.gridwidth = 3;
		c.insets = new Insets(GAP, GAP, 0, GAP);
		panel.add(new ResourceLabel("manage_db_connections.system"), c);
		
		c.gridx = 0;
		c.gridy = 3;
		c.insets = new Insets(0, GAP, 0, GAP);
		panel.add(databaseTypeComboBox, c);

		c.gridx = 0;
		c.gridy = 4;
		c.weightx = 1;
		c.gridwidth = 2;
		c.insets = new Insets(GAP, GAP, 0, GAP);
		panel.add(new ResourceLabel("manage_db_connections.host"), c);
		
		c.gridx = 2;
		c.gridy = 4;
		c.gridwidth = 1;
		c.weightx = 0;
		panel.add(new ResourceLabel("manage_db_connections.port"), c);

		c.gridx = 0;
		c.gridy = 5;
		c.weightx = 1;
		c.gridwidth = 2;
		c.insets = new Insets(0, GAP, 0, GAP);
		panel.add(hostTextField, c);
		
		c.gridx = 2;
		c.gridy = 5;
		c.gridwidth = 1;
		c.weightx = 0;
		panel.add(portTextField, c);

		c.gridx = 0;
		c.gridy = 6;
		c.weightx = 1;
		c.insets = new Insets(GAP, GAP, 0, GAP);
		c.gridwidth = 3;
		panel.add(new ResourceLabel("manage_db_connections.database"), c);
		
		c.gridx = 0;
		c.gridy = 7;
		c.insets = new Insets(0, GAP, 0, GAP);
		panel.add(databaseTextField, c);

		c.gridx = 0;
		c.gridy = 8;
		c.insets = new Insets(GAP, GAP, 0, GAP);
		panel.add(new ResourceLabel("manage_db_connections.user"), c);
		
		c.gridx = 0;
		c.gridy = 9;
		c.insets = new Insets(0, GAP, 0, GAP);
		panel.add(userTextField, c);

		c.gridx = 0;
		c.gridy = 10;
		c.insets = new Insets(GAP, GAP, 0, GAP);
		panel.add(new ResourceLabel("manage_db_connections.password"), c);
		
		c.gridx = 0;
		c.gridy = 11;
		c.insets = new Insets(0, GAP, GAP, GAP);
		panel.add(passwordField, c);
		
		c.gridx = 0;
		c.gridy = 12;
		c.insets = new Insets(GAP, GAP, 0, GAP);
		panel.add(new ResourceLabel("manage_db_connections.url"), c);
		
		c.gridx = 0;
		c.gridy = 13;
		c.insets = new Insets(0, GAP, GAP, GAP);
		panel.add(urlField, c);
		
		// adds the connection info label on a panel which is put in a scrollpane
		JPanel scrollPanel = new JPanel();
		scrollPanel.setLayout(new GridBagLayout());
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.fill = GridBagConstraints.BOTH;
		gbc.weightx = 1;
		gbc.weighty = 1;
		gbc.insets = new Insets(GAP, GAP, GAP, GAP);
		scrollPanel.add(testLabel, gbc);
		ExtendedJScrollPane ejsp = new ExtendedJScrollPane(scrollPanel);
		ejsp.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED));
		
		c.gridx = 0;
		c.gridy = 14;
		c.gridwidth = 2;
		c.gridheight = 2;
		c.weightx = 1;
		c.weighty = 1;
		c.fill = GridBagConstraints.BOTH;
		c.insets = new Insets(GAP, 2 * GAP, GAP, GAP);
		panel.add(ejsp, c);
		
		c.gridx = 2;
		c.gridy = 14;
		c.weightx = 0;
		c.gridwidth = 1;
		c.gridheight = 1;
		c.fill = GridBagConstraints.NONE;
		c.anchor = GridBagConstraints.CENTER;
		panel.add(new JButton(TEST_CONNECTION_ACTION), c);
		
		
		updateDefaults();
		updateURL(null);
		return panel;
	}
	
	@Override
	public void setVisible(boolean b) {
		if (connectionList.getModel().getSize() > 0) {
			connectionList.setSelectedIndex(0);
		}
		OPEN_CONNECTION_ACTION.actionPerformed(null);
		super.setVisible(b);
	}
	
	public JPanel makeConnectionManagementPanel() {
		JPanel panel = new JPanel(createGridLayout(1, 2));
		JScrollPane connectionListPane = new ExtendedJScrollPane(connectionList);
		connectionListPane.setBorder(createTitledBorder(I18N.getMessage(I18N.getGUIBundle(), "gui.border.manage_db_connections.connections")));
		panel.add(connectionListPane);
		panel.add(makeConnectionPanel());
		return panel;
	}
		
	private JDBCProperties getJDBCProperties() {
		return DatabaseService.getJDBCProperties((String) databaseTypeComboBox.getSelectedItem());
	}
	
	/**
	 * Returns the driver properties or null if there has been an error.
	 * @return
	 */
	private DriverPropertyInfo[] getPropertyInfos() {
		try {
			String host = hostTextField.getText();
			if (host == null || "".equals(host)) {
				host = "192.168.0.0";
			}
			String port = portTextField.getText();
			if (port == null || "".equals(port)) {
				port = "1234";
			}
			String db = databaseTextField.getText();
			if (db == null || "".equals(db)) {
				db = "test";
			}
			String driverURL = FieldConnectionEntry.createURL(getJDBCProperties(), host, port, db);
			Driver driver = DriverManager.getDriver(driverURL);
			
			// add properties to driver
			Properties givenProperties = currentlyEditedEntry.getConnectionProperties();
			DriverPropertyInfo[] propertyInfo = driver.getPropertyInfo(driverURL, givenProperties);
			if (propertyInfo == null) {
				propertyInfo = new DriverPropertyInfo[0];
			}
			return propertyInfo;
		} catch (SQLException e) {
			//LogService.getGlobal().log("Could not load jdbc driver properties.", LogService.ERROR);
			LogService.getRoot().log(Level.SEVERE, "com.rapidminer.gui.tools.dialogs.DatabaseConnectionDialog.loading_jdbc_driver_properties_error", e);
			return null;
		}
	}

	private void updateDefaults() {
		portTextField.setText(getJDBCProperties().getDefaultPort());
	}
	
	private void updateURL(FieldConnectionEntry entry) {
		if (entry != null && entry.isReadOnly()) {
			urlField.setText(entry.getURL());
		} else {
			urlField.setText(FieldConnectionEntry.createURL(getJDBCProperties(), hostTextField.getText(), portTextField.getText(), databaseTextField.getText()));
		}
		testLabel.setText(TEXT_CONNECTION_STATUS_UNKNOWN);
		testLabel.setIcon(ICON_CONNECTION_STATUS_UNKNOWN);
		fireStateChanged();
	}
	
	protected FieldConnectionEntry checkFields(boolean save) {
		String alias = aliasTextField.getText();
		if (save && (alias == null || "".equals(alias.trim()))) {
			SwingTools.showVerySimpleErrorMessage("manage_db_connections.missing", I18N.getMessage(I18N.getGUIBundle(), "gui.label.manage_db_connections.name.label"));
			aliasTextField.requestFocusInWindow();
			return null;
		}
		String host = hostTextField.getText();
		if (host == null || "".equals(host)) {
			SwingTools.showVerySimpleErrorMessage("manage_db_connections.missing", I18N.getMessage(I18N.getGUIBundle(), "gui.label.manage_db_connections.host.label"));
			hostTextField.requestFocusInWindow();
			return null;
		}
		String port = portTextField.getText();
//		if (port == null || "".equals(port)) {
//			SwingTools.showVerySimpleErrorMessage("manage_db_connections.missing", I18N.getMessage(I18N.getGUIBundle(), "gui.label.manage_db_connections.port.label"));
//			portTextField.requestFocusInWindow();
//			return null;
//		}
		String database = databaseTextField.getText();
		if (database == null) {
			database = "";
		}
//		if (database == null || "".equals(database)) {
//			SwingTools.showVerySimpleErrorMessage("manage_db_connections.missing", I18N.getMessage(I18N.getGUIBundle(), "gui.label.manage_db_connections.database.label"));
//			databaseTextField.requestFocusInWindow();
//			return null;
//		}
		String user = userTextField.getText();
		char[] password = passwordField.getPassword();
		
		// we need to use the connection properties from the current entry
		FieldConnectionEntry entry = new FieldConnectionEntry(alias, getJDBCProperties(), host, port, database, user, password);
		// only add properties if the entry exists
		if (currentlyEditedEntry != null) {
			entry.setConnectionProperties(currentlyEditedEntry.getConnectionProperties());
		}
		return entry;
	}
	
	public FieldConnectionEntry getConnectionEntry(boolean save) {
		String alias = aliasTextField.getText();
		if (save && (alias == null || "".equals(alias.trim()))) {
			return null;
		}
		String host = hostTextField.getText();
		if (host == null || "".equals(host)) {
			return null;
		}
		String port = portTextField.getText();
		if (port == null) { // || "".equals(port)) {
			port = "";
		}
		String database = databaseTextField.getText();
		if (database == null) {
			database = "";
		}
//		if (database == null || "".equals(database)) {
//			return null;
//		}
		String user = userTextField.getText();
		char[] password = passwordField.getPassword();
		return new FieldConnectionEntry(alias, getJDBCProperties(), host, port, database, user, password);
	}

//	private boolean isEntryModified() {
//		Object value = connectionList.getSelectedValue();
//		if (value instanceof FieldConnectionEntry) {
//			FieldConnectionEntry selectedEntry = (FieldConnectionEntry) value;
//			FieldConnectionEntry modifiedEntry = getConnectionEntry(false);
//			return modifiedEntry.equals(selectedEntry);
//		}
//		return true;
//	}
}
