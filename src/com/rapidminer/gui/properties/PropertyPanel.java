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
package com.rapidminer.gui.properties;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.lang.reflect.Constructor;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;

import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.event.CellEditorListener;
import javax.swing.event.ChangeEvent;

import com.rapidminer.gui.properties.celleditors.value.AttributeFileValueCellEditor;
import com.rapidminer.gui.properties.celleditors.value.AttributeOrderingCellEditor;
import com.rapidminer.gui.properties.celleditors.value.AttributeValueCellEditor;
import com.rapidminer.gui.properties.celleditors.value.AttributesValueCellEditor;
import com.rapidminer.gui.properties.celleditors.value.ColorValueCellEditor;
import com.rapidminer.gui.properties.celleditors.value.ConfigurableValueCellEditor;
import com.rapidminer.gui.properties.celleditors.value.ConfigurationWizardValueCellEditor;
import com.rapidminer.gui.properties.celleditors.value.CronExpressionCellEditor;
import com.rapidminer.gui.properties.celleditors.value.DatabaseConnectionValueCellEditor;
import com.rapidminer.gui.properties.celleditors.value.DatabaseTableValueCellEditor;
import com.rapidminer.gui.properties.celleditors.value.DateFormatValueCellEditor;
import com.rapidminer.gui.properties.celleditors.value.DateValueCellEditor;
import com.rapidminer.gui.properties.celleditors.value.DefaultPropertyValueCellEditor;
import com.rapidminer.gui.properties.celleditors.value.EnumerationValueCellEditor;
import com.rapidminer.gui.properties.celleditors.value.ExpressionValueCellEditor;
import com.rapidminer.gui.properties.celleditors.value.InnerOperatorValueCellEditor;
import com.rapidminer.gui.properties.celleditors.value.ListValueCellEditor;
import com.rapidminer.gui.properties.celleditors.value.MatrixValueCellEditor;
import com.rapidminer.gui.properties.celleditors.value.OperatorValueValueCellEditor;
import com.rapidminer.gui.properties.celleditors.value.ParameterTupelCellEditor;
import com.rapidminer.gui.properties.celleditors.value.PreviewValueCellEditor;
import com.rapidminer.gui.properties.celleditors.value.PropertyValueCellEditor;
import com.rapidminer.gui.properties.celleditors.value.RegexpValueCellEditor;
import com.rapidminer.gui.properties.celleditors.value.RepositoryLocationValueCellEditor;
import com.rapidminer.gui.properties.celleditors.value.SQLQueryValueCellEditor;
import com.rapidminer.gui.properties.celleditors.value.SimpleFileValueCellEditor;
import com.rapidminer.gui.properties.celleditors.value.TextValueCellEditor;
import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.operator.Operator;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeAttribute;
import com.rapidminer.parameter.ParameterTypeAttributeFile;
import com.rapidminer.parameter.ParameterTypeAttributeOrderingRules;
import com.rapidminer.parameter.ParameterTypeAttributes;
import com.rapidminer.parameter.ParameterTypeBoolean;
import com.rapidminer.parameter.ParameterTypeCategory;
import com.rapidminer.parameter.ParameterTypeChar;
import com.rapidminer.parameter.ParameterTypeColor;
import com.rapidminer.parameter.ParameterTypeConfiguration;
import com.rapidminer.parameter.ParameterTypeCronExpression;
import com.rapidminer.parameter.ParameterTypeDatabaseConnection;
import com.rapidminer.parameter.ParameterTypeDatabaseSchema;
import com.rapidminer.parameter.ParameterTypeDatabaseTable;
import com.rapidminer.parameter.ParameterTypeDate;
import com.rapidminer.parameter.ParameterTypeDateFormat;
import com.rapidminer.parameter.ParameterTypeDouble;
import com.rapidminer.parameter.ParameterTypeEnumeration;
import com.rapidminer.parameter.ParameterTypeExpression;
import com.rapidminer.parameter.ParameterTypeFile;
import com.rapidminer.parameter.ParameterTypeInnerOperator;
import com.rapidminer.parameter.ParameterTypeInt;
import com.rapidminer.parameter.ParameterTypeList;
import com.rapidminer.parameter.ParameterTypeMatrix;
import com.rapidminer.parameter.ParameterTypePassword;
import com.rapidminer.parameter.ParameterTypePreview;
import com.rapidminer.parameter.ParameterTypeRegexp;
import com.rapidminer.parameter.ParameterTypeRepositoryLocation;
import com.rapidminer.parameter.ParameterTypeSQLQuery;
import com.rapidminer.parameter.ParameterTypeStringCategory;
import com.rapidminer.parameter.ParameterTypeText;
import com.rapidminer.parameter.ParameterTypeTupel;
import com.rapidminer.parameter.ParameterTypeValue;
import com.rapidminer.tools.I18N;
import com.rapidminer.tools.LogService;
import com.rapidminer.tools.config.ParameterTypeConfigurable;

/**
 * @author Simon Fischer
 *
 */
public abstract class PropertyPanel extends JPanel {

	private static final long serialVersionUID = -3478661102690417293L;

	private final GridBagLayout layout = new GridBagLayout();

	/** Maps parameter type keys to currently displayed editors. */
	private final Map<String, PropertyValueCellEditor> currentEditors = new LinkedHashMap<String, PropertyValueCellEditor>();

	/** Types currently displayed by editors. */
	private Collection<ParameterType> currentTypes;

	private Color fontColor = Color.BLACK;

	public static final int VALUE_CELL_EDITOR_HEIGHT = 28;

	private static Map<Class<? extends ParameterType>, Class<? extends PropertyValueCellEditor>> knownValueEditors =
			new HashMap<Class<? extends ParameterType>, Class<? extends PropertyValueCellEditor>>();

	static {
		registerPropertyValueCellEditor(ParameterTypePassword.class, DefaultPropertyValueCellEditor.class);
		registerPropertyValueCellEditor(ParameterTypeConfiguration.class, ConfigurationWizardValueCellEditor.class);
		registerPropertyValueCellEditor(ParameterTypePreview.class, PreviewValueCellEditor.class);
		registerPropertyValueCellEditor(ParameterTypeColor.class, ColorValueCellEditor.class);
		registerPropertyValueCellEditor(ParameterTypeCategory.class, DefaultPropertyValueCellEditor.class);
		registerPropertyValueCellEditor(ParameterTypeStringCategory.class, DefaultPropertyValueCellEditor.class);
		registerPropertyValueCellEditor(ParameterTypeBoolean.class, DefaultPropertyValueCellEditor.class);
		registerPropertyValueCellEditor(ParameterTypeChar.class, DefaultPropertyValueCellEditor.class);
		registerPropertyValueCellEditor(ParameterTypeInt.class, DefaultPropertyValueCellEditor.class);
		registerPropertyValueCellEditor(ParameterTypeDouble.class, DefaultPropertyValueCellEditor.class);
		registerPropertyValueCellEditor(ParameterTypeAttributeFile.class, AttributeFileValueCellEditor.class);
		registerPropertyValueCellEditor(ParameterTypeFile.class, SimpleFileValueCellEditor.class);
		registerPropertyValueCellEditor(ParameterTypeRepositoryLocation.class, RepositoryLocationValueCellEditor.class);
		registerPropertyValueCellEditor(ParameterTypeValue.class, OperatorValueValueCellEditor.class);
		registerPropertyValueCellEditor(ParameterTypeInnerOperator.class, InnerOperatorValueCellEditor.class);
		registerPropertyValueCellEditor(ParameterTypeList.class, ListValueCellEditor.class);
		registerPropertyValueCellEditor(ParameterTypeMatrix.class, MatrixValueCellEditor.class);
		registerPropertyValueCellEditor(ParameterTypeExpression.class, ExpressionValueCellEditor.class);
		registerPropertyValueCellEditor(ParameterTypeText.class, TextValueCellEditor.class);
		registerPropertyValueCellEditor(ParameterTypeAttribute.class, AttributeValueCellEditor.class);
		registerPropertyValueCellEditor(ParameterTypeTupel.class, ParameterTupelCellEditor.class);
		registerPropertyValueCellEditor(ParameterTypeRegexp.class, RegexpValueCellEditor.class);
		registerPropertyValueCellEditor(ParameterTypeAttributes.class, AttributesValueCellEditor.class);
		registerPropertyValueCellEditor(ParameterTypeEnumeration.class, EnumerationValueCellEditor.class);
		registerPropertyValueCellEditor(ParameterTypeDatabaseConnection.class, DatabaseConnectionValueCellEditor.class);
		registerPropertyValueCellEditor(ParameterTypeDateFormat.class, DateFormatValueCellEditor.class);
		registerPropertyValueCellEditor(ParameterTypeSQLQuery.class, SQLQueryValueCellEditor.class);
		registerPropertyValueCellEditor(ParameterTypeDatabaseTable.class, DatabaseTableValueCellEditor.class);
		registerPropertyValueCellEditor(ParameterTypeDatabaseSchema.class, DatabaseTableValueCellEditor.class);
		registerPropertyValueCellEditor(ParameterTypeConfigurable.class, ConfigurableValueCellEditor.class);
		registerPropertyValueCellEditor(ParameterTypeAttributeOrderingRules.class, AttributeOrderingCellEditor.class);
		registerPropertyValueCellEditor(ParameterTypeCronExpression.class, CronExpressionCellEditor.class);
		registerPropertyValueCellEditor(ParameterTypeDate.class, DateValueCellEditor.class);
	}

	/**
	 * This method allows extensions to register own ParameterTypes and their editors. Please keep in mind, 
	 * that this method has to be called before any operator creation! That means, it has to be performed
	 * during init of the extension. 
	 * This method will register this value cell editor as well in the PropertyTable.
	 * 
	 * @param typeClass The class of the new ParameterType for which the editor should be used
	 * @param editor    The class of the PropertyValueCellEditor
	 */
	public static void registerPropertyValueCellEditor(Class<? extends ParameterType> typeClass, Class<? extends PropertyValueCellEditor> editor) {
		knownValueEditors.put(typeClass, editor);
		PropertyTable.registerPropertyValueCellEditor(typeClass, editor);
	}

	private PropertyValueCellEditor instantiateValueCellEditor(final ParameterType type) {
		return instantiateValueCellEditor(type, getOperator());
	}

	public PropertyPanel() {
		setLayout(layout);
	}

	public void setupComponents() {
		if (SwingUtilities.isEventDispatchThread()) {
			setupComponentsNow();
		} else {
			SwingUtilities.invokeLater(new Runnable() {

				@Override
				public void run() {
					setupComponentsNow();
				}
			});
		}
	}

	public void fireEditingStoppedEvent() {
		Map<String, PropertyValueCellEditor> currentEditors = new LinkedHashMap<String, PropertyValueCellEditor>();
		currentEditors.putAll(this.currentEditors);
		if (currentEditors.size() > 0) {
			for (String key : currentEditors.keySet()) {
				currentEditors.get(key).stopCellEditing();
			}
		}
	}

	private void setupComponentsNow() {
		removeAll();
		currentEditors.clear();

		currentTypes = getProperties();
		if (currentTypes == null) {
			revalidate();
			repaint();
			return;
		}
		GridBagConstraints c = new GridBagConstraints();
		c.anchor = GridBagConstraints.FIRST_LINE_START;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.insets = new Insets(4, 4, 4, 4);

		int row = 0;
		for (final ParameterType type : currentTypes) {
			StringBuilder toolTip = new StringBuilder(type.getDescription());
			if ((!(type instanceof ParameterTypeCategory)) && (!(type instanceof ParameterTypeStringCategory))) {
				String range = type.getRange();
				if ((range != null) && (range.trim().length() > 0)) {
					toolTip.append(" (");
					toolTip.append(type.getRange());
					toolTip.append(")");
				}
			}
			final PropertyValueCellEditor editor = instantiateValueCellEditor(type);
			currentEditors.put(type.getKey(), editor);
			Object value;
			value = getValue(type);
			if (value == null) {
				value = type.getDefaultValue();
			}

			Component editorComponent = editor.getTableCellEditorComponent(null, value, false, row, 1);
			if (!isEnabled()) {
				SwingTools.setEnabledRecursive(editorComponent, false);
			}
			if (editorComponent instanceof JComponent) {
				((JComponent) editorComponent).setToolTipText(toolTip.toString());
			}
			final Operator typesOperator = getOperator();
			editor.addCellEditorListener(new CellEditorListener() {

				@Override
				public void editingCanceled(ChangeEvent e) {}

				@Override
				public void editingStopped(ChangeEvent e) {
					Object valueObj = editor.getCellEditorValue();
					String value = type.toString(valueObj);
					String last;
					last = getValue(type);
					if (((value != null) && (last == null)) ||
							((last == null) && (value != null)) ||
							((value != null) && (last != null) && !value.equals(last))) {
						setValue(typesOperator, type, value, false);
					}
				}
			});

			c.gridx = 0;
			c.gridy = row;
			c.weightx = 1;
			c.weighty = 0;

			JPanel parameterPanel = null;
			if (!editor.rendersLabel()) {
				parameterPanel = new JPanel(new GridLayout(1, 2));
				parameterPanel.setOpaque(isOpaque());
				parameterPanel.setBackground(getBackground());
				parameterPanel.setPreferredSize(new Dimension((int) parameterPanel.getPreferredSize().getWidth(), VALUE_CELL_EDITOR_HEIGHT));
				JLabel label = new JLabel(type.getKey().replace('_', ' ') + " ");
				label.setOpaque(isOpaque());
				label.setFont(getFont());
				label.setForeground(fontColor);
				label.setBackground(getBackground());
				label.setToolTipText(toolTip.toString());
				int style = Font.PLAIN;
				if (!type.isOptional()) {
					style |= Font.BOLD;
				}
				if (type.isExpert()) {
					style |= Font.ITALIC;
				}
				label.setFont(label.getFont().deriveFont(style));
				label.setLabelFor(editorComponent);
				if (!isEnabled()) {
					SwingTools.setEnabledRecursive(label, false);
				}
				parameterPanel.add(label);
				parameterPanel.add(editorComponent);
				add(parameterPanel, c);
			} else {
				parameterPanel = new JPanel(new BorderLayout());
				parameterPanel.setOpaque(isOpaque());
				parameterPanel.setBackground(getBackground());
				parameterPanel.setPreferredSize(new Dimension((int) parameterPanel.getPreferredSize().getWidth(), VALUE_CELL_EDITOR_HEIGHT));
				parameterPanel.add(editorComponent, editorComponent instanceof JCheckBox ? BorderLayout.WEST : BorderLayout.CENTER);
				add(parameterPanel, c);
			}
			row++;
		}

		c.gridx = 0;
		c.gridy = row;
//		JComponent message = getMessage();
//		message.setOpaque(isOpaque());
//		add(message, c);

		JPanel dummyPanel = new JPanel(new GridLayout(1, 2));
		//dummyPanel.setBackground(getBackground());
		dummyPanel.setOpaque(false);
		c.weightx = 1;
		c.weighty = 1;
		c.fill = GridBagConstraints.BOTH;
		layout.setConstraints(dummyPanel, c);
		add(dummyPanel);

		revalidate();
		repaint();
	}

//	protected JComponent getMessage() {
//		JPanel panel = new JPanel();
//		panel.setBackground(getBackground());
//		return panel;
//	}

	protected boolean hasEditorFor(ParameterType type) {
		return currentEditors.containsKey(type.getKey());
	}

	protected int getNumberOfEditors() {
		return currentEditors.size();
	}

	protected PropertyValueCellEditor getEditorForKey(String key) {
		return currentEditors.get(key);
	}

	protected abstract String getValue(ParameterType type);

	protected abstract void setValue(Operator operator, ParameterType type, String value);

	protected abstract Collection<ParameterType> getProperties();

	protected abstract Operator getOperator();

	/** Subclasses of PropertyPanel (e.g. GenericParameterPanel) can overwrite this method in order to specify if GUI elements should be updated after setting the Value. **/
	protected void setValue(Operator operator, ParameterType type, String value, boolean updateComponents) {
		setValue(operator, type, value);
	}

	public static PropertyValueCellEditor instantiateValueCellEditor(final ParameterType type, Operator operator) {
		PropertyValueCellEditor editor;
		Class<?> typeClass = type.getClass();
		do {
			Class<? extends PropertyValueCellEditor> editorClass = knownValueEditors.get(typeClass);
			if (editorClass != null) {
				try {
					Constructor<? extends PropertyValueCellEditor> constructor = editorClass.getConstructor(new Class[] { typeClass });
					editor = constructor.newInstance(new Object[] { type });
				} catch (Exception e) {
					//LogService.getRoot().log(Level.WARNING, "Cannot construct property editor: " + e, e);
					LogService.getRoot().log(Level.WARNING,
							I18N.getMessage(LogService.getRoot().getResourceBundle(),
									"com.rapidminer.gui.properties.PropertyPanel.construct_property_editor_error",
									e),
							e);

					editor = new DefaultPropertyValueCellEditor(type);
				}
				break;
			} else {
				typeClass = typeClass.getSuperclass();
				editor = new DefaultPropertyValueCellEditor(type);
			}
		} while (typeClass != null);
		editor.setOperator(operator);
		return editor;
	}

	/**
	 * This sets the color of the labels of properties. Not used if lable is replaced by editor
	 */
	public void setLabelTextColor(Color color) {
		fontColor = color;
	}
}
