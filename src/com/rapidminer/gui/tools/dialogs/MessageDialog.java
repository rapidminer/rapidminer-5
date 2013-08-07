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

import javax.swing.Icon;
import javax.swing.JComponent;

import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.tools.I18N;

/**
 * 
 * @author Tobias Malbrecht
 */
public class MessageDialog extends ButtonDialog {

	private static final long serialVersionUID = -5825873580778775409L;
	
	public MessageDialog(String i18nKey, Object...i18nArgs) {
		this(i18nKey, null, i18nArgs);
	}
	
	public MessageDialog(String i18nKey, JComponent c, Object...i18nArgs) {
		super("message." + i18nKey, true, i18nArgs);
		layoutDefault(c, makeOkButton());		
	}
	
	@Override
	protected Icon getInfoIcon() {
		String iconName = I18N.getMessageOrNull(I18N.getGUIBundle(), getKey() + ".icon");
		if (iconName == null || "".equals(iconName)) {
			return SwingTools.createIcon("48/" + I18N.getMessage(I18N.getGUIBundle(), "gui.dialog.message.icon"));			
		} else {
			return SwingTools.createIcon("48/" + iconName);
		}
	}
}
