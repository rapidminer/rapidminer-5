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

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;

import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;

import com.rapidminer.Process;
import com.rapidminer.gui.dialog.TemplateWizardDialog;
import com.rapidminer.gui.templates.OperatorParameterPair;
import com.rapidminer.operator.Operator;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.UndefinedParameterError;


/**
 * With a WizardPropertyTable one can edit (a subset of) parameters of all
 * operators of the process in one place. This property table is used in the
 * {@link TemplateWizardDialog}.
 * 
 * @author Ingo Mierswa, Simon Fischer
 *          Exp $
 */
public class WizardPropertyTable extends DefaultPropertyTable {

	private static final long serialVersionUID = 1510360561085238281L;

	private transient Operator[] operators;

	private transient ParameterType[] parameterTypes;

	public WizardPropertyTable() {
		super();
		setProcess(null, null);
	}

	/**
	 * Sets the process and the editable parameters.
	 * 
	 * @param parameters
	 *            A list of String[2] where the first String is the name of the
	 *            operator and the second is the name of the parameter.
	 */
	public boolean setProcess(Process process, Collection<OperatorParameterPair> parameters) {
		if (process == null) {
			parameters = new LinkedList<OperatorParameterPair>(); // enforce arraylengths = 0
		}
		updateTableData(parameters.size());

		operators = new Operator[parameters.size()];
		parameterTypes = new ParameterType[parameters.size()];

		Iterator<OperatorParameterPair> i = parameters.iterator();
		int j = 0;
		while (i.hasNext()) {
			OperatorParameterPair parameter = i.next();
			Operator operator = process.getOperator(parameter.getOperator());
			operators[j] = operator;
			ParameterType parameterType = getParameterType(operator, parameter.getParameter());
			if(operator == null || parameterType == null) {
				updateTableData(0); //enforce size of 0
				return false;
			}
			parameterTypes[j] = parameterType;
			getModel().setValueAt(operator.getName() + "." + parameterTypes[j].getKey(), j, 0);
			Object value = parameterTypes[j].getDefaultValue();
			try {
				value = operator.getParameters().getParameter(parameterTypes[j].getKey());
			} catch (UndefinedParameterError e) {
				// tries non default value. Fail --> default
			} 
			getModel().setValueAt(value, j, 1);
			j++;
		}
		updateEditorsAndRenderers();

		getModel().addTableModelListener(new TableModelListener() {
			public void tableChanged(TableModelEvent e) {
				setValue(e.getFirstRow(), getModel().getValueAt(e.getFirstRow(), 1));
			}
		});
		return true;
	}

	private static ParameterType getParameterType(Operator operator, String key) {
		Iterator i = operator.getParameters().getParameterTypes().iterator();
		while (i.hasNext()) {
			ParameterType type = (ParameterType) i.next();
			if (type.getKey().equals(key))
				return type;
		}
		return null;
	}

	@Override
	public ParameterType getParameterType(int row) {
		return parameterTypes[row];
	}

	@Override
	public Operator getOperator(int row) {
		return operators[row];
	}
}
