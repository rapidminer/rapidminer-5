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

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import com.rapidminer.gui.tools.dialogs.ButtonDialog;

/** Displays information about all pending {@link ProgressThread}s. 
 * 
 * @author Simon Fischer */
class ProgressThreadDialog extends ButtonDialog {

	private static final long serialVersionUID = 1L;

	private JLabel mainLabel = new JLabel("-");
	private JProgressBar mainProgressBar = new JProgressBar();
	private JList taskList = new JList(ProgressThread.QUEUE_MODEL);
	private JButton stopButton;
	
	private static ProgressThreadDialog INSTANCE = new ProgressThreadDialog();

	public static ProgressThreadDialog getInstance() {
		return INSTANCE;
	}

	private ProgressThreadDialog() {
		super("progress_dialog");
		setModal(true);
		ProgressThread.QUEUE_MODEL.addListDataListener(new ListDataListener() {
			@Override
			public void intervalRemoved(ListDataEvent e) {
				if (ProgressThread.QUEUE_MODEL.getSize() == 0) {
					getInstance().dispose();
				}
			}
			@Override public void intervalAdded(ListDataEvent e) { }
			@Override public void contentsChanged(ListDataEvent e) { }
		});
		mainProgressBar.setMinimum(0);

		JPanel main = new JPanel(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.fill   = GridBagConstraints.HORIZONTAL;
		c.anchor = GridBagConstraints.FIRST_LINE_START;
		c.weightx = 1;
		c.weighty = 0;
		c.insets = new Insets(4,4,4,4);

		c.weightx = 0;
		c.gridwidth = GridBagConstraints.RELATIVE;				
		main.add(new ResourceLabel("progress_dialog.current"), c);
		c.weightx = 1;
		c.gridwidth = GridBagConstraints.REMAINDER;		
		main.add(mainLabel, c);		


		main.add(mainProgressBar, c);
		c.fill    = GridBagConstraints.BOTH;

		c.insets = new Insets(10, 4,4,4);
		main.add(new ResourceLabel("progress_dialog.pending"), c);
		c.insets = new Insets(4,4,4,4);
		c.weighty = 1;
		main.add(new JScrollPane(taskList), c);

		stopButton = new JButton(new ResourceAction("stop") {
			private static final long serialVersionUID = 1L;
			@Override
			public void actionPerformed(ActionEvent e) {
				((ProgressThread)taskList.getSelectedValue()).cancel();
				enableStopButton();
			}			
		});
		
		
		taskList.addListSelectionListener(new ListSelectionListener() {
			@Override
			public void valueChanged(ListSelectionEvent e) {
				enableStopButton();		
			}
		});
		enableStopButton();
		
		layoutDefault(main, stopButton, makeCloseButton());
	}

	private void enableStopButton() {
		stopButton.setEnabled(!taskList.isSelectionEmpty() &&
				!((ProgressThread)taskList.getSelectedValue()).isCancelled());
	}

	public void refreshDialog() {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				final ProgressThread current = ProgressThread.getCurrent();
				if (current != null) {
					final ProgressDisplay display = current.getDisplay();	
					SwingUtilities.invokeLater(new Runnable() {
						@Override
						public void run() {
							String message = display.getMessage();
							if (message == null) {
								mainLabel.setText(current.getName());
							} else {
								mainLabel.setText(current.getName() + ": "+message);
							}
							mainProgressBar.setMaximum(display.getTotal());
							mainProgressBar.setValue(display.getCompleted());							
						}
					});					
				} else {
					mainLabel.setText("-");
					mainProgressBar.setValue(0);
					mainProgressBar.setEnabled(false);
				}
			}			
		});
	}
}
