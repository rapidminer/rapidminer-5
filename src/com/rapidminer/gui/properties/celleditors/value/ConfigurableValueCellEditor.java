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
import java.util.List;

import javax.swing.AbstractCellEditor;
import javax.swing.Action;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JTable;

import com.rapidminer.gui.tools.ResourceAction;
import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.operator.Operator;
import com.rapidminer.tools.config.Configurable;
import com.rapidminer.tools.config.ConfigurationException;
import com.rapidminer.tools.config.ConfigurationManager;
import com.rapidminer.tools.config.Configurator;
import com.rapidminer.tools.config.ParameterTypeConfigurable;
import com.rapidminer.tools.config.gui.ConfigurationDialog;

/**
 * Provides a selection field for {@link Configurable}s.
 * @author Dominik Halfkann
 *
 */
public class ConfigurableValueCellEditor extends AbstractCellEditor implements PropertyValueCellEditor {

	private static final long serialVersionUID = -771727412083431607L;

	private String typeId = "";

	class ConfigurableComboBoxModel extends DefaultComboBoxModel {

		private static final long serialVersionUID = -2984664300141879731L;

		public void updateModel() {
			Object selected = getSelectedItem();
			removeAllElements();

			List<String> entryNames = ConfigurationManager.getInstance().getAllConfigurableNames(typeId);
			for (String entryName : entryNames) {
				addElement(entryName);
			}

			if (model.getSize() == 0) {
				setSelectedItem(null);
			} else {
				if (selected != null) {
					setSelectedItem(selected);
				} else {
					if (model.getSize() > 0) {
						setSelectedItem(model.getElementAt(0));
					}
				}
			}
		}
	}

	private ConfigurableComboBoxModel model = new ConfigurableComboBoxModel();

	private JPanel panel = new JPanel();

	private JComboBox comboBox = new JComboBox(model);

	public ConfigurableValueCellEditor(final ParameterTypeConfigurable type) {
		this.typeId = type.getTypeId();
		if (!ConfigurationManager.getInstance().hasTypeId(typeId)) {
			throw new IllegalArgumentException("Unknown configurable type: " + typeId);
		}
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
				// The event is only fired if the focus loss is permanently,
				// i.e. it is not fired if the user just switched to another window.
				if (!e.isTemporary()) {
					fireEditingStopped();
				}
			}

			@Override
			public void focusGained(FocusEvent e) {
				model.updateModel();
			}
		});

		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.BOTH;
		c.weighty = 1;
		c.weightx = 1;
		panel.add(comboBox, c);

		final JButton button = new JButton(new ResourceAction(true, "configuration." + ConfigurationManager.getInstance().getConfigurator(typeId).getI18NBaseKey()) {

			private static final long serialVersionUID = 3989811306286704326L;
			{
				putValue(Action.NAME, "");
			}

			public void actionPerformed(ActionEvent e) {
				class SetConfigurableDialog extends ConfigurationDialog {

					private static final long serialVersionUID = 2306881477330192804L;

					@SuppressWarnings("unchecked")
					public SetConfigurableDialog() {
						super((Configurator<Configurable>) ConfigurationManager.getInstance().getConfigurator(typeId));
					}

					@Override
					protected void close() {
						try {
							if (checkUnsavedChanges()) {
								Configurable entry;
								entry = getConfigurableFromInputFields();

								if (entry != null) {
									model.updateModel();
									model.setSelectedItem(entry.getName());
									fireEditingStopped();
								}
								super.close();
							}
						} catch (ConfigurationException e1) {
							SwingTools.showSimpleErrorMessage("configuration.dialog.general", e1, e1.getMessage());
							super.close();
						}
					}
				}
				;
				SetConfigurableDialog dialog = new SetConfigurableDialog();
				dialog.setVisible(true);
				model.updateModel();
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
		model.updateModel();
		comboBox.setSelectedItem(value);
		return panel;
	}

	@Override
	public Object getCellEditorValue() {
		return comboBox.getSelectedItem();
	}

	@Override
	public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
		model.updateModel();
		comboBox.setSelectedItem(value);
		return panel;
	}

	@Override
	public void setOperator(Operator operator) {}
}
