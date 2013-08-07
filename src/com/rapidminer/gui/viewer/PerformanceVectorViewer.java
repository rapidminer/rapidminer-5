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
package com.rapidminer.gui.viewer;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.util.LinkedList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import com.rapidminer.gui.processeditor.results.ResultDisplayTools;
import com.rapidminer.gui.tools.ExtendedJScrollPane;
import com.rapidminer.operator.IOContainer;
import com.rapidminer.operator.performance.PerformanceCriterion;
import com.rapidminer.operator.performance.PerformanceVector;


/**
 * Can be used to display the criteria of a {@link PerformanceVector}.
 * 
 * @author Ingo Mierswa
 */
public class PerformanceVectorViewer extends JPanel {

	private static final long serialVersionUID = -5848837142789453985L;

	public PerformanceVectorViewer(final PerformanceVector performanceVector, final IOContainer container) {
		setLayout(new BorderLayout());

		// all criteria
		final CardLayout cardLayout = new CardLayout();
		final JPanel mainPanel = new JPanel(cardLayout);
		add(mainPanel, BorderLayout.CENTER);
		List<String> criteriaNameList = new LinkedList<String>();
		for (int i = 0; i < performanceVector.getSize(); i++) {
			PerformanceCriterion criterion = performanceVector.getCriterion(i);
			criteriaNameList.add(criterion.getName());
			Component component = ResultDisplayTools.createVisualizationComponent(criterion, container, "Performance Criterion");
			JScrollPane criterionPane = new ExtendedJScrollPane(component);
			criterionPane.setBorder(null);
			mainPanel.add(criterionPane, criterion.getName());
		}
		String[] criteriaNames = new String[criteriaNameList.size()];
		criteriaNameList.toArray(criteriaNames);
		final JList criteriaList = new JList(criteriaNames) {

			private static final long serialVersionUID = 3031125186920370793L;

			@Override
			public Dimension getPreferredSize() {
				Dimension dim = super.getPreferredSize();
				dim.width = Math.max(150, dim.width);
				return dim;
			}
		};


		// selection list
		criteriaList.setBorder(BorderFactory.createTitledBorder("Criterion Selector"));
		criteriaList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

		criteriaList.addListSelectionListener(new ListSelectionListener() {
			public void valueChanged(ListSelectionEvent e) {
				String selected = (String)criteriaList.getSelectedValue();
				cardLayout.show(mainPanel, selected);
			}
		});

		JScrollPane listScrollPane = new ExtendedJScrollPane(criteriaList);
		listScrollPane.setBorder(null);
		add(listScrollPane, BorderLayout.WEST);

		// select first criterion
		criteriaList.setSelectedIndices(new int[] { 0 });
	}
}
