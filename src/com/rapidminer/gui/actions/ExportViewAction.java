/*
 *  RapidMiner
 *
 *  Copyright (C) 2001-2014 by RapidMiner and the contributors
 *
 *  Complete list of developers available at our web site:
 *
 *       http://rapidminer.com
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
package com.rapidminer.gui.actions;

import java.awt.CardLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;

import javax.swing.JPanel;

import org.freehep.util.export.ExportDialog;

import com.rapidminer.gui.RapidMinerGUI;
import com.rapidminer.gui.new_plotter.engine.jfreechart.link_and_brush.LinkAndBrushChartPanel;
import com.rapidminer.gui.new_plotter.gui.ChartConfigurationPanel;
import com.rapidminer.gui.plotter.PlotterPanel;
import com.rapidminer.gui.tools.ResourceAction;


/**
 * Start the corresponding action.
 * 
 * @author Ingo Mierswa
 */
public class ExportViewAction extends ResourceAction {

	private static final long serialVersionUID = 2438568343977468901L;

	private final Component component;
	private final String componentName;
	
	public ExportViewAction(Component component, String componentName) {
		super("export", componentName);
		this.component = component;
		this.componentName = componentName;
	}

	public void actionPerformed(ActionEvent e) {
		ExportDialog exportDialog = new ExportDialog("RapidMiner");
		
		// special handling for charts as we only want to export the chart but not the control panel
		// chart cannot be scaled to size of component because otherwise we would break the chart aspect-ratio
		if (component.getClass().isAssignableFrom(JPanel.class)) {
			JPanel panel = (JPanel) component;
			if (panel.getLayout().getClass().isAssignableFrom(CardLayout.class)) {
				for (final Component comp : panel.getComponents()) {
					if (comp.isVisible() && ChartConfigurationPanel.class.isAssignableFrom(comp.getClass())) {
						final ChartConfigurationPanel chartConfigPanel = (ChartConfigurationPanel) comp;
						
						JPanel outerPanel = new JPanel() {
							
							private static final long serialVersionUID = 7315234075649335574L;

							@Override
							public void paintComponent(Graphics g) {
								Graphics2D g2 = (Graphics2D) g;
								// create new LinkAndBrushChartPanel with double buffering set to false to get vector graphic export
								// The real chart has to use double buffering for a) performance and b) zoom rectangle drawing
								LinkAndBrushChartPanel newLaBPanel = new LinkAndBrushChartPanel(chartConfigPanel.getPlotEngine().getChartPanel().getChart(), chartConfigPanel.getPlotEngine().getChartPanel().getWidth(), chartConfigPanel.getPlotEngine().getChartPanel().getHeight(), chartConfigPanel.getPlotEngine().getChartPanel().getMinimumDrawWidth(), chartConfigPanel.getPlotEngine().getChartPanel().getMinimumDrawHeight(), false, false);
								newLaBPanel.setSize(chartConfigPanel.getPlotEngine().getChartPanel().getSize());
								newLaBPanel.setOverlayList(chartConfigPanel.getPlotEngine().getChartPanel().getOverlayList());
								newLaBPanel.print(g2);
							}
						};
						outerPanel.setSize(new Dimension(chartConfigPanel.getPlotEngine().getChartPanel().getWidth(), chartConfigPanel.getPlotEngine().getChartPanel().getHeight()));
						
						exportDialog.showExportDialog(RapidMinerGUI.getMainFrame(), "Export", outerPanel, componentName);
						return;
					} else if (comp.isVisible() && PlotterPanel.class.isAssignableFrom(comp.getClass())) {
						// special case for PlotterPanel as the Panel itself is wider than the plotter
						// not having a special case here results in the exported image being too wide (empty space to the left)
						final PlotterPanel plotterPanel = (PlotterPanel) comp;
						
						JPanel outerPanel = new JPanel() {
							
							private static final long serialVersionUID = 7315234075649335574L;

							@Override
							public void paintComponent(Graphics g) {
								Graphics2D g2 = (Graphics2D) g;
								plotterPanel.print(g2);
							}
						};
						outerPanel.setSize(plotterPanel.getPlotterComponent().getSize());
						
						exportDialog.showExportDialog(RapidMinerGUI.getMainFrame(), "Export", outerPanel, componentName);
						return;
					}
				}
			}
		}
		exportDialog.showExportDialog(RapidMinerGUI.getMainFrame(), "Export", component, componentName);
	}
}
