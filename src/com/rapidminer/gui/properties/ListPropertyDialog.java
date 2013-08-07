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

import java.awt.event.ActionEvent;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JScrollPane;

import com.rapidminer.gui.tools.ExtendedJScrollPane;
import com.rapidminer.gui.tools.ResourceAction;
import com.rapidminer.operator.Operator;
import com.rapidminer.parameter.ParameterTypeList;

/**
 * A Dialog displaying a {@link ListPropertyTable}. This can be used to add new
 * values to the parameter list or change current values. Removal of values is
 * also supported.
 * 
 * @see com.rapidminer.gui.properties.ListPropertyTable
 * @author Ingo Mierswa, Simon Fischer, Tobias Malbrecht, Nils Woehler, Marius Helf
 */
public class ListPropertyDialog extends PropertyDialog {

	private static final long serialVersionUID = 1876607848416333390L;

	private boolean ok = false;

	private final ListPropertyTable2 listPropertyTable;

	private final List<String[]> parameterList;

	public ListPropertyDialog(final ParameterTypeList type, List<String[]> parameterList, Operator operator) {
		super(type, "list");
		this.parameterList = parameterList;
		listPropertyTable = new ListPropertyTable2(type, parameterList, operator);
		if (listPropertyTable.isEmpty()) {
			listPropertyTable.addRow();
		}
		JScrollPane scrollPane = new ExtendedJScrollPane(listPropertyTable);
		scrollPane.setBorder(createBorder());
		layoutDefault(scrollPane, NORMAL, new JButton(new ResourceAction("list.add_row") {

			private static final long serialVersionUID = 1L;

			public void actionPerformed(ActionEvent e) {
				listPropertyTable.addRow();
			}
		}), new JButton(new ResourceAction("list.remove_row") {

			private static final long serialVersionUID = 1L;

			public void actionPerformed(ActionEvent e) {
				listPropertyTable.removeSelected();
			}
		}), makeOkButton("list_property_dialog_apply"), makeCancelButton());
		
		listPropertyTable.requestFocusForLastEditableCell();
	}

	@Override
	protected void ok() {
		ok = true;
		listPropertyTable.stopEditing();
		listPropertyTable.storeParameterList(parameterList);
		dispose();
	}

	@Override
	protected void cancel() {
		ok = false;
		dispose();
	}

	@Override
	public boolean isOk() {
		return ok;
	}
}
