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
package com.rapidminer.gui.processeditor;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.util.List;

import javax.swing.JPanel;
import javax.swing.JTextArea;

import com.rapidminer.Process;
import com.rapidminer.gui.MainFrame;
import com.rapidminer.gui.tools.ExtendedJScrollPane;
import com.rapidminer.gui.tools.ResourceDockKey;
import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.gui.tools.syntax.TextAreaDefaults;
import com.rapidminer.operator.Operator;
import com.vlsolutions.swing.docking.DockKey;
import com.vlsolutions.swing.docking.Dockable;


/**
 * This editor is used to edit the description (comment) for the currently selected
 * operator.
 * 
 * @author Ingo Mierswa
 */
public class CommentEditor extends JPanel implements ProcessEditor, Dockable {
	private static final long serialVersionUID = -2661346182983330754L;
	
	private transient Operator currentOperator;
	
	//private final JEditTextArea editor;
	private JTextArea editor = new JTextArea(20, 80);
	
	public CommentEditor() {
		super(new BorderLayout());
		TextAreaDefaults textAreaDefaults = SwingTools.getTextAreaDefaults();
		textAreaDefaults.eolMarkers = false;
		textAreaDefaults.paintInvalid = false;
		//editor = new JEditTextArea(textAreaDefaults);
		editor.addFocusListener(new FocusListener() {
			public void focusGained(FocusEvent e) {}
			public void focusLost(FocusEvent e) {
				saveComment();
			}			
		});
		//editor.setTokenMarker(new HTMLTokenMarker());
		editor.setLineWrap(true);
		editor.setWrapStyleWord(true);
		editor.setBorder(null);
		
//		JToolBar toolBar = new ExtendedJToolBar();
//		toolBar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Color.LIGHT_GRAY));
//		toolBar.add(new ResourceAction(true, "xml_editor.apply_changes") {
//			private static final long serialVersionUID = 1L;
//			@Override
//			public void actionPerformed(ActionEvent e) {
//				try {
//					validateProcess();
//				} catch (IOException e1) {
//					e1.printStackTrace();
//				} catch (XMLException e1) {
//					e1.printStackTrace();
//				}
//			}			
//		});
//		toolBar.addSeparator();
//		toolBar.add(editor.COPY_ACTION);
//		toolBar.add(editor.CUT_ACTION);
//		toolBar.add(editor.PASTE_ACTION);
//		toolBar.add(editor.DELETE_ACTION);
//		toolBar.addSeparator();
//		toolBar.add(editor.SEARCH_AND_REPLACE_ACTION);
//		add(toolBar, BorderLayout.NORTH);
		add(new ExtendedJScrollPane(editor), BorderLayout.CENTER);
	}
	
	public void setSelection(List<Operator> selection) {
		Operator operator = selection.isEmpty() ? null : selection.get(0);
		if (operator == this.currentOperator) {
			return;
		}
		saveComment();
		this.currentOperator = operator;
		if (this.currentOperator != null) {
			String description = this.currentOperator.getUserDescription();
			if (description != null) {
				String text = description;
				editor.setText(text);
			} else {
				editor.setText(null);
			}
		} else {
			editor.setText(null);
		}
	}

	public void processChanged(Process proc) {}
	
	public void processUpdated(Process proc) {}

	private void saveComment() {		
		if (this.currentOperator != null) {
			this.currentOperator.setUserDescription(editor.getText());
		}
	}

//	/**
//	 * Overwrites the super method in order to save the typed text.
//	 */
//	@Override
//	public void processKeyEvent(KeyEvent evt) {
//		editor.processKeyEvent(evt);
//		switch (evt.getID()) {
//		case KeyEvent.KEY_RELEASED:
//			saveComment();
//			break;
//		}
//	}	

	public static final String COMMENT_EDITOR_DOCK_KEY = "comment_editor";
	private final DockKey DOCK_KEY = new ResourceDockKey(COMMENT_EDITOR_DOCK_KEY);
	{
		DOCK_KEY.setDockGroup(MainFrame.DOCK_GROUP_ROOT);
	}
	@Override
	public Component getComponent() {
		return this;
	}

	@Override
	public DockKey getDockKey() {
		return DOCK_KEY;
	}
}
