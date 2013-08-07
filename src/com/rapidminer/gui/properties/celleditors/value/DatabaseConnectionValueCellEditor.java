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
package com.rapidminer.gui.properties.celleditors.value;

import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractCellEditor;
import javax.swing.AbstractListModel;
import javax.swing.Action;
import javax.swing.ComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JTable;

import com.rapidminer.gui.tools.ResourceAction;
import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.gui.tools.dialogs.ConfirmDialog;
import com.rapidminer.gui.tools.dialogs.ManageDatabaseConnectionsDialog;
import com.rapidminer.operator.Operator;
import com.rapidminer.parameter.ParameterTypeDatabaseConnection;
import com.rapidminer.tools.jdbc.connection.ConnectionEntry;
import com.rapidminer.tools.jdbc.connection.DatabaseConnectionService;
import com.rapidminer.tools.jdbc.connection.FieldConnectionEntry;

/**
 * Displays a combobox with all database connections configured in RapidMiner.
 * 
 * @author Simon Fischer, Tobias Malbrecht
 */
public class DatabaseConnectionValueCellEditor extends AbstractCellEditor implements PropertyValueCellEditor {

	private static final long serialVersionUID = -771727412083431607L;

	class DatabaseConnectionComboBoxModel extends AbstractListModel implements ComboBoxModel {

		private static final long serialVersionUID = 5358838374857978178L;

		private ConnectionEntry selectedConnection;
		
		private List<ConnectionEntry> getList() {
			return new ArrayList<ConnectionEntry>(DatabaseConnectionService.getConnectionEntries());
		}
				
		@Override
		public void setSelectedItem(Object anItem) {
			if ((selectedConnection != null && !selectedConnection.getName().equals( (String) anItem )) ||
					selectedConnection == null && anItem != null) {
				selectedConnection = DatabaseConnectionService.getConnectionEntry((String) anItem);
				fireContentsChanged(this, -1, -1);
			}			
		}

		@Override
		public Object getSelectedItem() {
	        return (selectedConnection == null ? null : selectedConnection.getName());
		}

		@Override
		public int getSize() {
			return getList().size();
		}

		@Override
		public Object getElementAt(int index) {
	        if ( index >= 0 && index < getList().size() )
	            return getList().get(index).getName();
	        else
	            return null;
		}
	}

	private DatabaseConnectionComboBoxModel model = new DatabaseConnectionComboBoxModel();

	private JPanel panel = new JPanel();

	private JComboBox comboBox = new JComboBox(model);

	public DatabaseConnectionValueCellEditor(final ParameterTypeDatabaseConnection type) {

		panel.setLayout(new GridBagLayout());
		panel.setToolTipText(type.getDescription());
		comboBox.setToolTipText(type.getDescription());
		comboBox.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				fireEditingStopped();
			}
		});
		comboBox.addFocusListener(new FocusListener() {

			@Override
			public void focusLost(FocusEvent e) {
				if (!e.isTemporary()) {
					fireEditingStopped();
				}
			}

			@Override
			public void focusGained(FocusEvent e) {
//				model.updateModel();
			}
		});

		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.BOTH;
		c.weighty = 1;
		c.weightx = 1;
		panel.add(comboBox, c);

		final JButton button = new JButton(new ResourceAction(true, "manage_db_connections") {

			private static final long serialVersionUID = 3989811306286704326L;
			{
				putValue(Action.NAME, "");
			}

			public void actionPerformed(ActionEvent e) {
				class SetDatabaseConnectionDialog extends ManageDatabaseConnectionsDialog {

					private static final long serialVersionUID = 2306881477330192804L;

					public SetDatabaseConnectionDialog() {
//						super("manage_db_connections");
//						layoutDefault(makeConnectionManagementPanel(), makeOkButton(), makeCancelButton());
						super();
					}

					@Override
					protected void ok() {
						FieldConnectionEntry entry = checkFields(true);
						if (entry != null) {
							boolean existent = false;
							for (ConnectionEntry listEntry : DatabaseConnectionService.getConnectionEntries()) {
								if (listEntry.getName().equals(entry.getName())) {
									existent = true;
									break;
								}
							}
							if (!existent) {
								if (SwingTools.showConfirmDialog("save", ConfirmDialog.YES_NO_OPTION, entry.getName()) == ConfirmDialog.YES_OPTION) {
									DatabaseConnectionService.addConnectionEntry(entry);
								} else {
									fireEditingCanceled();
									return;
								}
							}
//							model.updateModel();
							model.setSelectedItem(entry.getName());
							fireEditingStopped();
							super.ok();
						}
					}
				}
				;
				SetDatabaseConnectionDialog dialog = new SetDatabaseConnectionDialog();
				dialog.setVisible(true);
//				model.updateModel();
			}
		});
		button.setMargin(new Insets(0, 0, 0, 0));
		c.weightx = 0;
		panel.add(button, c);
	}

	@Override
	public boolean rendersLabel() {
		return false;
	}

	@Override
	public boolean useEditorAsRenderer() {
		return true;
	}

	@Override
	public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
		comboBox.setSelectedItem(value);
		return panel;
	}

	@Override
	public Object getCellEditorValue() {
		return comboBox.getSelectedItem();
	}

	@Override
	public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
		comboBox.setSelectedItem(value);
		return panel;
	}

	@Override
	public void setOperator(Operator operator) {}
}
