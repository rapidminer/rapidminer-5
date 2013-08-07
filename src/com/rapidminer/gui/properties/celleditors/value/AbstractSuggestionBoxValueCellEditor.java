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
import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.util.List;

import javax.swing.AbstractCellEditor;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.swing.plaf.basic.BasicComboPopup;

import com.rapidminer.gui.tools.ProgressThread;
import com.rapidminer.operator.Operator;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.tools.I18N;
import com.rapidminer.tools.ProgressListener;

/** 
 *  Renders a combo box which can be filled with suggestions.
 * 
 * @author Marcin Skirzynski, Nils Woehler
 */
public abstract class AbstractSuggestionBoxValueCellEditor extends AbstractCellEditor implements PropertyValueCellEditor {

	private static final long serialVersionUID = -771727412083431607L;

	/**
	 * The model of the combo box which consist of the suggestions
	 */
	private final SuggestionComboBoxModel model;

	/**
	 * The GUI element
	 */
	private final JComboBox comboBox;
	private final JPanel container;

	private Operator operator;

	private ParameterType type;

	private final String LOADING;

	public AbstractSuggestionBoxValueCellEditor(final ParameterType type) {
		this.type = type;
		this.model = new SuggestionComboBoxModel();
		this.comboBox = new SuggestionComboBox(model);
		this.comboBox.setToolTipText(type.getDescription());
		this.comboBox.setRenderer(new SuggestionComboBoxModelCellRenderer());

		LOADING = I18N.getGUILabel("parameters.loading");

		this.container = new JPanel(new GridBagLayout());
		this.container.setToolTipText(type.getDescription());

		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.BOTH;
		c.weighty = 1;
		c.weightx = 1;
		container.add(comboBox, c);
	}

	public abstract List<Object> getSuggestions(Operator operator, ProgressListener progressListener);

	private String getValue() {
		String value = null;
		value = operator.getParameters().getParameterOrNull(type.getKey());
		return value;
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
		return container;
	}

	@Override
	public Object getCellEditorValue() {
		return comboBox.getSelectedItem();
	}

	@Override
	public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
		comboBox.setSelectedItem(value);
		return container;
	}

	@Override
	public void setOperator(Operator operator) {
		this.operator = operator;
	}

	public class SuggestionComboBoxModelCellRenderer extends DefaultListCellRenderer {

		private static final long serialVersionUID = 1L;

		@Override
		public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
			Component listCellRendererComponent = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
			if (LOADING.equals(value)) {
				listCellRendererComponent.setBackground(list.getBackground());
				listCellRendererComponent.setForeground(UIManager.getColor("Label.disabledForeground"));
				listCellRendererComponent.setEnabled(false);
			}
			return listCellRendererComponent;
		}

	}

	class SuggestionComboBoxModel extends DefaultComboBoxModel {

		private static final long serialVersionUID = -2984664300141879731L;

		private Object lock = new Object();

		public void updateModel(final SuggestionComboBox comboBox) {
			final Object selected = getValue();

			ProgressThread t = new ProgressThread("fetching_suggestions") {

				@Override
				public void run() {
					try {
						getProgressListener().setTotal(100);
						getProgressListener().setCompleted(0);

						synchronized (lock) {
							removeAllElements();

							insertElementAt(LOADING, 0);

							// fill list with stuff
							List<Object> suggestions = getSuggestions(operator, getProgressListener());

							removeAllElements();

							int index = 0;
							for (Object suggestion : suggestions) {
								insertElementAt(suggestion, index);
								++index;
							}

							// resize popup
							Object child = comboBox.getAccessibleContext().getAccessibleChild(0);
							BasicComboPopup popup = (BasicComboPopup) child;
							JList list = popup.getList();
							Dimension preferred = list.getPreferredSize();
							preferred.width += 25;
							int itemCount = comboBox.getItemCount();
							int rowHeight = 10;
							if(itemCount > 0) {
								rowHeight = preferred.height / itemCount;
							} 
							int maxHeight = comboBox.getMaximumRowCount() * rowHeight;
							preferred.height = Math.min(preferred.height, maxHeight);

							Container c = SwingUtilities.getAncestorOfClass(JScrollPane.class, list);
							JScrollPane scrollPane = (JScrollPane) c;

							scrollPane.setPreferredSize(preferred);
							scrollPane.setMaximumSize(preferred);

							Dimension popupSize = popup.getSize();
							popupSize.width = preferred.width;
							popupSize.height = preferred.height + 5;
							Component parent = popup.getParent();
							if (parent != null) {
								parent.setSize(popupSize);
								parent.validate();
								parent.repaint();
							}
						}

						getProgressListener().setCompleted(100);
						if (getSelectedItem() == null) {
							if (model.getSize() == 0) {
								setSelectedItem(null);
							} else if (selected != null) {
								setSelectedItem(selected);
							}
						}
					} finally {
						getProgressListener().complete();
					}
				}
			};
			t.start();
		}
	}

	class SuggestionComboBox extends JComboBox {

		private static final long serialVersionUID = 4000279412600950101L;

		private SuggestionComboBox(final SuggestionComboBoxModel model) {
			super(model);
			setEditable(true);
			addActionListener(new ActionListener() {

				public void actionPerformed(ActionEvent e) {
					fireEditingStopped();
				}
			});
			getEditor().getEditorComponent().addFocusListener(new FocusListener() {

				@Override
				public void focusLost(FocusEvent e) {
					if (!e.isTemporary()) {
						fireEditingStopped();
					}
				}

				@Override
				public void focusGained(FocusEvent e) {}
			});
			
			// add popup menu listener
			Object child = getAccessibleContext().getAccessibleChild(0);
			BasicComboPopup popup = (BasicComboPopup) child;
			popup.addPopupMenuListener(new PopupMenuListener() {

				@Override
				public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
					model.updateModel(SuggestionComboBox.this);
				}

				@Override
				public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {}

				@Override
				public void popupMenuCanceled(PopupMenuEvent e) {}
			});
		}

		@Override
		public void setSelectedItem(Object anObject) {
			if (!LOADING.equals(anObject)) {
				super.setSelectedItem(anObject);
			}
		}

		@Override
		public void setSelectedIndex(int anIndex) {
			if (!LOADING.equals(getModel().getElementAt(anIndex))) {
				super.setSelectedIndex(anIndex);
			}
		}
	}

	/**
	 * @param button adds a button the the right side of the ComboBox.
	 */
	protected void addConfigureButton(JButton button) {
		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.BOTH;
		c.weighty = 1;
		c.weightx = 0;
		container.add(button, c);
	}

}
