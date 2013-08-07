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
package com.rapidminer.gui.dialog;

import javax.swing.JEditorPane;
import javax.swing.JScrollPane;

import com.rapidminer.gui.tools.ExtendedHTMLJEditorPane;
import com.rapidminer.gui.tools.ExtendedJScrollPane;
import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.gui.tools.dialogs.ButtonDialog;


/**
 * This dialog is shown after loading a process definition if the root operator has a
 * user comment (description tag). The text of this description is shown in this
 * dialog.
 * 
 * @author Ingo Mierswa, Tobias Malbrecht
 */
public class ProcessInfoScreen extends ButtonDialog {

	private static final long serialVersionUID = 7687035897010730802L;

	public ProcessInfoScreen(String file, String text) {
		super("process_info", true, new Object[] {file});

		JEditorPane description = new ExtendedHTMLJEditorPane("text/html", SwingTools.text2DisplayHtml(text));
		description.setToolTipText("A short description of this process");
		description.setEditable(false);
		// so it does not steel ENTER from the default button
		description.setFocusable(false);
		description.setBackground(this.getBackground());
		description.setCaretPosition(0);
		JScrollPane textScrollPane = new ExtendedJScrollPane(description);
		textScrollPane.setBorder(createBorder());

		layoutDefault(textScrollPane, NORMAL, makeCloseButton());
	}
}
