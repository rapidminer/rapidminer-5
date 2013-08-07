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
package com.rapidminer.gui.templates;

import javax.swing.JButton;


/**
 * The manage templates dialog assists the user in managing his created process templates.
 * Template processes are saved in the local &quot;.rapidminer&quot;
 * directory of the user. The name, description and additional parameters to set
 * can be specified by the user. In this dialog he can also delete one of the templates.
 * 
 * @author Ingo Mierswa, Tobias Malbrecht
 */
public class ManageTemplatesDialog extends TemplatesDialog {

	private static final long serialVersionUID = 1428487062393160289L;

	public ManageTemplatesDialog() {
		super(Template.USER_DEFINED);
		JButton deleteButton = new JButton(DELETE_ACTION);
		layoutDefault(listPane, NORMAL, deleteButton, makeOkButton("manage_templates_dialog_apply"));
	}
}
