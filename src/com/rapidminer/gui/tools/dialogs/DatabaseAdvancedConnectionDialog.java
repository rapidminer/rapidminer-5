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

import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.sql.DriverPropertyInfo;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.swing.AbstractButton;
import javax.swing.DefaultCellEditor;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableModel;

import com.rapidminer.gui.tools.ExtendedJScrollPane;
import com.rapidminer.tools.I18N;

/**
 * In this dialog, the user can edit the advanvced properties of the given JDBC driver.
 * Note: These are not the jdbc properties!
 * 
 * @author Marco Boeck
 */
public class DatabaseAdvancedConnectionDialog extends ButtonDialog {
	
	private static final long serialVersionUID = -3287030968059122084L;
	

	/**
	 * This {@link TableModel} can work with a {@link DriverPropertyInfo} array.
	 *
	 */
	private class DriverPropertyInfoTableModel extends AbstractTableModel {
		private static final long serialVersionUID = -6521100131706498109L;
		
		/** the driver propinfos */
		private DriverPropertyInfo[] propInfo;
		
		/** a list indicating for each propInfo index if the default value should be overriden or not */
		private List<Boolean> override;
		
		
		/**
		 * Creates a new {@link DriverPropertyInfoTableModel}.
		 * @param propInfo all propertyInfos of a JDBC driver (not the jdbc properties)
		 * @param currentProperties the current edited custom properties as of before this dialog
		 */
		private DriverPropertyInfoTableModel(DriverPropertyInfo[] propInfo, Properties currentProperties) {
			this.propInfo = propInfo;
			this.override = new ArrayList<Boolean>(propInfo.length);
			for (int i=0; i<this.propInfo.length; i++) {
				// only set override for the rows to true where the key exists in the currentProperties
				if (currentProperties.get(propInfo[i].name) != null) {
					override.add(true);
				} else {
					override.add(false);
				}
			}
		}

		@Override
		public int getRowCount() {
			return propInfo.length;
		}

		@Override
		public int getColumnCount() {
			return 3;
		}
		
		@Override
		public boolean isCellEditable(int row, int col) {
			// only values are editable
			if (col == 1 || col == 2) {
				return true;
			}
			return false;
	    }
		
		@Override
		public Class<?> getColumnClass(int columnIndex) {
			if (columnIndex == 0) {
				return String.class;
			} else if (columnIndex == 1) {
				return Object.class;
			} else {
				return Boolean.class;
			}
		}
		
		@Override
		public String getColumnName(int col) {
			if (col == 0) {
				return "Key";
			} else if (col == 2) {
				return "Override";
			} else {
				return "Value";
			}
	    }
		
		/**
		 * Returns the current value of a combo item.
		 * @param row
		 * @return
		 */
		public String getComboValue(int row) {
			return propInfo[row].value;
		}
		
		/**
		 * Returns the tooltip for the given row.
		 * @param row
		 * @return
		 */
		public String getTooltip(int row) {
			return propInfo[row].description;
		}

		@Override
		public Object getValueAt(int rowIndex, int columnIndex) {
			if (columnIndex == 0) {
				return propInfo[rowIndex].name;
			} else if (columnIndex == 2) {
				return override.get(rowIndex);
			} else {
				if (propInfo[rowIndex].choices == null) {
					return propInfo[rowIndex].value;
				} else {
					return propInfo[rowIndex].choices;
				}
			}
		}
		
		@Override
		public void setValueAt(Object value, int row, int col) {
			// nothing but the value column is editable
			if (col != 1 && col != 2) {
				return;
			}
			if (col == 1) {
				// check if choices are available that the value is one of the choices
				if (propInfo[row].choices != null) {
					boolean found = false;
					for (String choice : propInfo[row].choices) {
						if (choice.equals(value)) {
							found = true;
							break;
						}
					}
					if (!found) {
						return;
					}
				}
				propInfo[row].value = String.valueOf(value);
				override.set(row, Boolean.TRUE);
				fireTableCellUpdated(row, 2);
			} else if (col == 2) {
				override.set(row, Boolean.parseBoolean(String.valueOf(value)));
			}
	    }
		
		/**
		 * Returns the user edited properties.
		 * @return
		 */
		public Properties getProperties() {
			Properties props = new Properties();
			for (int i=0; i<getRowCount(); i++) {
				// empty values mean field not set, don't add to properties
				// and only add if manual override has been checked
				if (propInfo[i].value != null && !"".equals(propInfo[i].value) && override.get(i)) {
					props.put(propInfo[i].name, propInfo[i].value);
				}
			}
			
			return props;
		}
		
	}
	
	
	/**
	 * This {@link TableCellRenderer} can render the arrays from a {@link DriverPropertyInfo} array.
	 *
	 */
	private class DriverPropertyInfoTableDefaultCellRenderer extends DefaultTableCellRenderer {
		private static final long serialVersionUID = -3376218040898856384L;

		/** the combo box for array values */
		JComboBox box;
		
		
		/**
		 * Creates a new {@link DriverPropertyInfoTableDefaultCellRenderer}.
		 */
		private DriverPropertyInfoTableDefaultCellRenderer() {
			box = new JComboBox();
		}


		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
			if (value instanceof String[]) {
				box.setModel(new DefaultComboBoxModel((String[])value));
				box.setSelectedItem(((DriverPropertyInfoTableModel)table.getModel()).getComboValue(table.convertRowIndexToModel(row)));
				return box;
			} else {
				Component component = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
				// add tooltips for key column
				if (component instanceof DefaultTableCellRenderer && table.convertColumnIndexToModel(column) == 0) {
					String tooltip = ((DriverPropertyInfoTableModel)table.getModel()).getTooltip(table.convertRowIndexToModel(row));
					tooltip = "<html><div width = 300px>" + tooltip + "</div></html>";
					((DefaultTableCellRenderer)component).setToolTipText(tooltip);
				}
				return component;
			}
		}
	}
	
	/** the JTable displaying the properties */
	private JTable table;
	
	private int returnValue = ConfirmDialog.CANCEL_OPTION;
	
	
	/**
	 * Creates a new advanced connection properties dialog.
	 * @param i18nKey
	 * @param propertyInfos the propertyInfo[] array as returned by the driver
	 * @param currentProperties the currently set properties
	 * @param i18nArgs
	 */
	public DatabaseAdvancedConnectionDialog(String i18nKey, DriverPropertyInfo[] propertyInfos, Properties currentProperties, Object ... i18nArgs) {
		super(i18nKey, true, i18nArgs);
		setupGUI(propertyInfos, currentProperties);
	}
	
	
	private void setupGUI(final DriverPropertyInfo[] propInfo, final Properties currentProperties) {
		table = new JTable(new DriverPropertyInfoTableModel(propInfo, currentProperties)) {
			private static final long serialVersionUID = 1L;
			
			/** a map containing the JComboBoxes for each row where one is needed */
			private Map<Integer, JComboBox> mapOfBoxes = new HashMap<Integer, JComboBox>(propInfo.length);;

			@Override
			public TableCellEditor getCellEditor(int row, int col) {
				if (getModel().getValueAt(convertRowIndexToModel(row), convertColumnIndexToModel(col)) instanceof String[]) {
					// this needs a JComboBox as editor
					JComboBox box = mapOfBoxes.get(convertRowIndexToModel(row));
					if (box == null) {
						mapOfBoxes.put(convertRowIndexToModel(row), new JComboBox((String[])getModel().getValueAt(convertRowIndexToModel(row), convertColumnIndexToModel(col))));
						box = mapOfBoxes.get(convertRowIndexToModel(row));
					}
					box.setSelectedItem(((DriverPropertyInfoTableModel)table.getModel()).getComboValue(table.convertRowIndexToModel(row)));
					return new DefaultCellEditor(box);
				} else if (getModel().getValueAt(convertRowIndexToModel(row), convertColumnIndexToModel(col)) instanceof Boolean) {
					// override checkbox
					return getDefaultEditor(Boolean.class);
				} else {
					// normal string text editor
					return getDefaultEditor(String.class);
				}
			}
			
			@Override
		    protected JTableHeader createDefaultTableHeader() {
		        return new JTableHeader(columnModel) {
					private static final long serialVersionUID = 1L;

					public String getToolTipText(MouseEvent e) {
						// add table header tooltips
		                Point p = e.getPoint();
		                int index = columnModel.getColumnIndexAtX(p.x);
		                int realIndex = columnModel.getColumn(index).getModelIndex();
		                switch (realIndex) {
						case 0:
							return I18N.getMessage(I18N.getGUIBundle(), "gui.dialog.db_connection_advanced.table.key.tooltip");
						case 1:
							return I18N.getMessage(I18N.getGUIBundle(), "gui.dialog.db_connection_advanced.table.value.tooltip");
						case 2:
							return I18N.getMessage(I18N.getGUIBundle(), "gui.dialog.db_connection_advanced.table.override.tooltip");
						default:
							return null;
						}
		            }
		        };
		    }

		};
		table.setAutoCreateRowSorter(true);
		table.setRowHeight(table.getRowHeight() + 4);
		table.setAutoResizeMode(JTable.AUTO_RESIZE_NEXT_COLUMN);
		table.getColumnModel().getColumn(0).setPreferredWidth(200);
		table.getColumnModel().getColumn(1).setPreferredWidth(200);
		table.getColumnModel().getColumn(2).setPreferredWidth(10);
		table.setDefaultRenderer(Object.class, new DriverPropertyInfoTableDefaultCellRenderer());
		table.getTableHeader().setReorderingAllowed(false);
		
		JPanel panel = new JPanel();
		panel.setLayout(new GridBagLayout());
		GridBagConstraints gbc = new GridBagConstraints();
		
		ExtendedJScrollPane scrollPane = new ExtendedJScrollPane(table);
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.weightx = 1;
		gbc.weighty = 1;
		gbc.fill = GridBagConstraints.BOTH;
		panel.add(scrollPane, gbc);
		
		Collection<AbstractButton> list = new LinkedList<AbstractButton>();
		JButton okButton = makeOkButton();
		okButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				returnValue = ConfirmDialog.OK_OPTION;
			}
			
		});
		list.add(okButton);
		list.add(makeCancelButton());
		
		layoutDefault(panel, NORMAL, list);
	}
	
	/**
	 * Returns the user edited connection properties or {@code null} if the user pressed cancel.
	 * @return
	 */
	public Properties getConnectionProperties() {
		if (returnValue == ConfirmDialog.OK_OPTION) {
			return ((DriverPropertyInfoTableModel)table.getModel()).getProperties();
		} else {
			return null;
		}
	}

}
