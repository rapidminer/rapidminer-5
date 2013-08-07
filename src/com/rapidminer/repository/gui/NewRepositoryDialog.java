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
package com.rapidminer.repository.gui;

import java.awt.Component;
import java.util.HashMap;
import java.util.Map;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JRadioButton;

import com.rapidminer.gui.RapidMinerGUI;
import com.rapidminer.gui.tools.ResourceActionAdapter;
import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.gui.tools.dialogs.MultiPageDialog;
import com.rapidminer.repository.RepositoryException;

/** A dialog to create new remote or local repositories.
 * 
 * @author Simon Fischer
 *
 */
public class NewRepositoryDialog extends MultiPageDialog {

	private static final long serialVersionUID = 1L;

	private final RemoteRepositoryPanel remoteRepositoryPanel = new RemoteRepositoryPanel();
	private final LocalRepositoryPanel localRepositoryPanel = new LocalRepositoryPanel(getFinishButton(), true);

	private final JRadioButton localButton;

	private final JRadioButton remoteButton;

	private NewRepositoryDialog() {
		super(RapidMinerGUI.getMainFrame(), "repositorydialog", true, new Object[]{});

		Box firstPage = new Box(BoxLayout.Y_AXIS);
		ButtonGroup checkBoxGroup = new ButtonGroup();
		localButton = new JRadioButton(new ResourceActionAdapter("new_local_repositiory"));
		remoteButton = new JRadioButton(new ResourceActionAdapter("new_remote_repositiory"));
		checkBoxGroup.add(localButton);
		checkBoxGroup.add(remoteButton);
		firstPage.add(localButton);
		firstPage.add(remoteButton);
		firstPage.add(Box.createVerticalGlue());
		localButton.setSelected(true);

		Map<String,Component> cards = new HashMap<String,Component>();
		cards.put("first", firstPage);		
		cards.put("remote", remoteRepositoryPanel);
		cards.put("local", localRepositoryPanel);
		layoutDefault(cards);
	}

	public static void createNew() {
		NewRepositoryDialog d = new NewRepositoryDialog();
		d.setVisible(true);
	}

	@Override
	protected void finish() {
		try {
			if (localButton.isSelected()) {
				localRepositoryPanel.makeRepository();
			} else {
				remoteRepositoryPanel.makeRepository();
			}
			super.finish();
		} catch (RepositoryException e) {
			SwingTools.showSimpleErrorMessage("cannot_create_repository", e);
		}
	}

	@Override
	protected String getNameForStep(int step) {
		switch (step) {
			case 0: return "first";
			case 1:
				if (localButton.isSelected()) {
					return "local";
				} else {
					return "remote";
				}
			default: throw new IllegalArgumentException("Illegal index: "+step);				
		}		
	}

	@Override
	protected boolean isComplete() {
		return isLastStep(getCurrentStep());
	}

	@Override
	protected boolean isLastStep(int step) {
		return step >= 1;		
	}
}
