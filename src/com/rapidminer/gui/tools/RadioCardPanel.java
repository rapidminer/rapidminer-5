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
package com.rapidminer.gui.tools;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.ButtonGroup;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.SwingUtilities;

import com.rapidminer.gui.RapidMinerGUI;
import com.rapidminer.gui.actions.CloseAllResultsAction;
import com.rapidminer.gui.actions.StoreInRepositoryAction;
import com.rapidminer.gui.tools.components.DropDownButton;
import com.rapidminer.operator.IOObject;
import com.rapidminer.operator.ResultObject;

/**
 * This is a helper component consisting of a a set of radio buttons together
 * with a main panel with a card layout. If more than one component is added, 
 * the viewable component can be selected via the radio buttons. Hence, this
 * component works similar to a tabbed pane but with radio buttons instead.
 * 
 * @author Sebastian Land, Ingo Mierswa
 */
public class RadioCardPanel extends JPanel {

	private static final long serialVersionUID = 2929637220390538982L;

	private final CardLayout layout = new CardLayout();

	private final JPanel mainPanel = new JPanel(layout);

	private final ViewToolBar toolBar = new ViewToolBar();

	private final ButtonGroup buttonGroup = new ButtonGroup();

	private Runnable delayedAddition = null;

	private int counter = 0;

	public RadioCardPanel(String name, IOObject object) {
		this(name, object, true, true, false);
	}
	
	public RadioCardPanel(String name, IOObject object, boolean showRepositoryStorageButton, boolean showExportMenuButton) {
		setLayout(new BorderLayout());
		add(mainPanel, BorderLayout.CENTER);
		add(toolBar, BorderLayout.NORTH);

		if (showRepositoryStorageButton) {
			toolBar.add(new StoreInRepositoryAction(object), ViewToolBar.RIGHT);
		}
		if (showExportMenuButton) {
			DropDownButton exportButton = PrintingTools.makeExportPrintDropDownButton(mainPanel, (object instanceof ResultObject ? ((ResultObject)object).getName() : "result") + " " + object.getSource());
			exportButton.addToToolBar(toolBar, ViewToolBar.RIGHT);
		}
	}
	
	public RadioCardPanel(String name, IOObject object, boolean showRepositoryStorageButton, boolean showExportMenuButton, boolean showCloseAllResultsButton) {
		setLayout(new BorderLayout());
		add(mainPanel, BorderLayout.CENTER);
		add(toolBar, BorderLayout.NORTH);

		if (showCloseAllResultsButton) {
			toolBar.add(new CloseAllResultsAction(RapidMinerGUI.getMainFrame()), ViewToolBar.RIGHT);
		}
		if (showRepositoryStorageButton) {
			toolBar.add(new StoreInRepositoryAction(object), ViewToolBar.RIGHT);
		}
		if (showExportMenuButton) {
			DropDownButton exportButton = PrintingTools.makeExportPrintDropDownButton(mainPanel, (object instanceof ResultObject ? ((ResultObject)object).getName() : "result") + " " + object.getSource());
			exportButton.addToToolBar(toolBar, ViewToolBar.RIGHT);
		}
	}

	/**
	 * This adds the given component to the card layout and adds a radio button for selecting this card
	 */
	public void addCard(final String name, final Component component) {
		if (component instanceof JComponent)
			((JComponent) component).setBorder(null);

		if (counter == 0) {
			// adding component
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					mainPanel.add(component, name);
				}
			});
			// storing delayed addition of button for selection
			delayedAddition = new Runnable() {
				public void run() {
					JRadioButton viewButton = new JRadioButton(name);
					viewButton.addActionListener(new ActionListener() {
						public void actionPerformed(ActionEvent e) {
							showCard(name);
						}
					});
					toolBar.add(viewButton);
					buttonGroup.add(viewButton);
					viewButton.setSelected(true);
				}
			};
		} else {
			if (counter == 1) {
				// execute addition of first if a second occurs
				SwingUtilities.invokeLater(delayedAddition);
				delayedAddition = null;
			}
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					mainPanel.add(component, name);
					JRadioButton viewButton = new JRadioButton(name);
					viewButton.addActionListener(new ActionListener() {
						public void actionPerformed(ActionEvent e) {
							showCard(name);
						}
					});
					toolBar.add(viewButton);
					buttonGroup.add(viewButton);
				}
			});
		}
		counter++;
	}

	private void showCard(String name) {
		layout.show(mainPanel, name);
	}
}

