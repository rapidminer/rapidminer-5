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

import javax.swing.JTextField;

/**
 * 
 * @author Tobias Malbrecht
 */
public class InputDialog extends ButtonDialog {

	private static final long serialVersionUID = -5825873580778775409L;
	
	private final JTextField textField = new JTextField();
	
	public InputDialog(String key) {
		this(key, null);
	}
	
	public InputDialog(String key, String text, Object...arguments) {
		super("input." + key, true, arguments);		
		if (text != null) {
			textField.setText(text);
		}
		layoutDefault(textField, makeOkButton(), makeCancelButton());
	}
	
	public String getInputText() {
		return textField.getText();
	}
}
