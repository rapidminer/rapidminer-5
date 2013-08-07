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

import java.util.Collection;

import javax.swing.JComboBox;

/**
 * 
 * @author Tobias Malbrecht
 */
public class SelectionInputDialog extends ButtonDialog {

	private static final long serialVersionUID = -5825873580778775409L;
	
	private final JComboBox comboBox = new JComboBox();
	
	public SelectionInputDialog(String key, Object[] selectionValues, Object initialSelectionValue) {
		this(key, selectionValues, initialSelectionValue, (Object[]) null);
	}
	
	public SelectionInputDialog(String key, Object[] selectionValues, Object initialSelectionValue, Object...keyArguments) {
		super("input." + key, true, keyArguments);
		for (Object selectionValue : selectionValues) {
			comboBox.addItem(selectionValue);
		}
		comboBox.setSelectedItem(initialSelectionValue);
		layoutDefault(comboBox, makeOkButton(), makeCancelButton());
	}
	
	/**
	 * This will create a SelectionInputDIalog whose Combobox is editable.
	 */
	public SelectionInputDialog(String key, boolean editable, Collection<?> selectionValues, Object initialSelectionValue, Object...keyArguments) {
		this(key, selectionValues, initialSelectionValue, keyArguments);
		comboBox.setEditable(editable);
	}
	
	public SelectionInputDialog(String key, boolean editable, Object[] selectionValues, Object initialSelectionValue, Object...keyArguments) {
		this(key, selectionValues, initialSelectionValue, keyArguments);
		comboBox.setEditable(editable);
	}
	
	public SelectionInputDialog(String key, Collection<?> selectionValues, Object initialSelectionValue, Object...keyArguments) {
		super("input." + key, true, keyArguments);
		for (Object selectionValue : selectionValues) {
			comboBox.addItem(selectionValue);
		}
		comboBox.setSelectedItem(initialSelectionValue);
		layoutDefault(comboBox, makeOkButton(), makeCancelButton());
	}
	
	public Object getInputSelection() {
		return wasConfirmed() ? comboBox.getSelectedItem() : null;
	}
}
