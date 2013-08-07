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
package com.rapidminer.gui.plotter;

import java.awt.Component;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import com.rapidminer.datatable.DataTable;
import com.rapidminer.gui.plotter.PlotterConfigurationModel.PlotterChangedListener;
import com.rapidminer.gui.plotter.PlotterConfigurationModel.PlotterSettingsChangedListener;
import com.rapidminer.gui.plotter.PlotterPanel.LineStyleCellRenderer;
import com.rapidminer.gui.plotter.settings.ListeningJCheckBox;
import com.rapidminer.gui.plotter.settings.ListeningJComboBox;
import com.rapidminer.gui.plotter.settings.ListeningJSlider;
import com.rapidminer.gui.plotter.settings.ListeningListSelectionModel;
import com.rapidminer.gui.tools.ExtendedJList;
import com.rapidminer.gui.tools.ExtendedJScrollPane;
import com.rapidminer.gui.tools.ExtendedListModel;
import com.rapidminer.gui.tools.ResourceAction;
import com.rapidminer.parameter.ParameterTypeEnumeration;
import com.rapidminer.tools.LogService;

/** Panel containing control elements for a  {@link Plotter}. Depending on
 *  the selected plotter type the options panel part is created or adapted. The
 *  option panel usually contains selectors for up to three axis and other
 *  options depending on the plotter like a plot amount slider or option buttons.
 * 
 * @see PlotterPanel
 * @author Simon Fischer
 *
 */
public class PlotterControlPanel extends JPanel implements PlotterChangedListener {

	private static final long serialVersionUID = 1L;

	private PlotterConfigurationModel plotterSettings;

	/** The plotter selection combo box. */
	private final JComboBox plotterCombo = new JComboBox();
	
	private List<PlotterSettingsChangedListener> changeListenerElements = new LinkedList<PlotterSettingsChangedListener>();

	
	private transient final ItemListener plotterComboListener = new ItemListener() {
		@Override
		public void itemStateChanged(ItemEvent e) {
			plotterSettings.setPlotter((plotterCombo.getSelectedItem().toString()));	
		}			
	};

	public PlotterControlPanel(PlotterConfigurationModel plotterSettings) {
		this.plotterSettings = plotterSettings;
		this.setLayout(new GridBagLayout());
        updatePlotterCombo();
        updateControls();
	}


	private void updateControls() {
		final Plotter plotter = plotterSettings.getPlotter();
		DataTable dataTable = plotterSettings.getDataTable();
		changeListenerElements = new LinkedList<PlotterSettingsChangedListener>();
		
		// 0. Clear GUI
		removeAll();

		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.BOTH;
		c.gridwidth = GridBagConstraints.REMAINDER;
		c.insets = new Insets(2, 2, 2, 2);
		c.weightx = 1;

		// 1. register mouse listener on plotter		

		final JLabel coordinatesLabel = new JLabel("                      ");
		PlotterMouseHandler mouseHandler = new PlotterMouseHandler(plotter, plotterSettings.getDataTable(), new CoordinatesHandler() {
			@Override
			public void updateCoordinates(String coordinateInfo) {
				coordinatesLabel.setText(coordinateInfo);
			}
		});  
		plotter.addMouseMotionListener(mouseHandler);
		plotter.addMouseListener(mouseHandler);

		// 2. Construct Plotter list
		JLabel label = null;
		String toolTip = null;
		if (plotterSettings.getAvailablePlotters().size() > 1) {
			toolTip = "The plotter which should be used for displaying data.";
			label = new JLabel("Plotter");
			label.setToolTipText(toolTip);
			this.add(label, c);
			this.add(plotterCombo, c);
		}

		List<Integer> plottedDimensionList = new LinkedList<Integer>();
		if (plotter != null) {
			for (int i = 0; i < dataTable.getNumberOfColumns(); i++) {
				if (plotter.getPlotColumn(i)) {
					plottedDimensionList.add(i);
				}
			}
		}

		// 3b. Setup axes selection panel (main)
		final List<JComboBox> axisCombos = new LinkedList<JComboBox>();
		for (int axisIndex = 0; axisIndex < plotter.getNumberOfAxes(); axisIndex++) {
			toolTip = "Select a column for " + plotter.getAxisName(axisIndex);
			label = new JLabel(plotter.getAxisName(axisIndex));
			label.setToolTipText(toolTip);
			this.add(label, c);
			final int finalAxisIndex = axisIndex;
			final ListeningJComboBox axisCombo = new ListeningJComboBox(PlotterAdapter.PARAMETER_SUFFIX_AXIS + PlotterAdapter.transformParameterName(plotter.getAxisName(finalAxisIndex)), 200) {
				private static final long serialVersionUID = 1L;
				@Override
				public void settingChanged(String generalKey, String specificKey, String value) {
					super.settingChanged(generalKey, specificKey, value);
				}
			}; 
				axisCombo.setToolTipText(toolTip);
				axisCombo.addItem("None");
				for (int j = 0; j < dataTable.getNumberOfColumns(); j++) {
					axisCombo.addItem(dataTable.getColumnName(j));
				}
			changeListenerElements.add(axisCombo);

			axisCombo.addItemListener(new ItemListener() {
				public void itemStateChanged(ItemEvent e) {
					plotterSettings.setParameterAsString(PlotterAdapter.PARAMETER_SUFFIX_AXIS + PlotterAdapter.transformParameterName(plotter.getAxisName(finalAxisIndex)), axisCombo.getSelectedItem().toString());
				}
			});

			this.add(axisCombo, c);
			axisCombos.add(axisCombo);

			// log scale
			if (plotter.isSupportingLogScale(axisIndex)) {
				final ListeningJCheckBox logScaleBox = new ListeningJCheckBox(PlotterAdapter.PARAMETER_SUFFIX_AXIS + PlotterAdapter.transformParameterName(plotter.getAxisName(finalAxisIndex))+ PlotterAdapter.PARAMETER_SUFFIX_LOG_SCALE, "Log Scale", false);
				changeListenerElements.add(logScaleBox);
				logScaleBox.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						plotterSettings.setParameterAsBoolean(PlotterAdapter.PARAMETER_SUFFIX_AXIS + PlotterAdapter.transformParameterName(plotter.getAxisName(finalAxisIndex))+ PlotterAdapter.PARAMETER_SUFFIX_LOG_SCALE, logScaleBox.isSelected());
					}
				});
				this.add(logScaleBox, c);
			}
		}

		// 4. Specific settings (colors, values, etc.)
		if (plotter.getValuePlotSelectionType() != Plotter.NO_SELECTION) {
			JLabel plotLabel;
			if (plotter.getPlotName() == null) {
				plotLabel = new JLabel("Plots");
				toolTip = "Select the column which should be plotted.";
			} else {
				plotLabel = new JLabel(plotter.getPlotName());
				toolTip = "Select a column for " + plotter.getPlotName();
			}
			plotLabel.setToolTipText(toolTip);
			this.add(plotLabel, c);
		}

		switch (plotter.getValuePlotSelectionType()) {
		case Plotter.MULTIPLE_SELECTION:
			final ExtendedListModel model = new ExtendedListModel();
			for (String name : dataTable.getColumnNames()) {
				model.addElement(name, "Select column '" + name + "' for plotting.");	
			}
			final JList plotList = new ExtendedJList(model, 200);
			ListeningListSelectionModel selectionModel = new ListeningListSelectionModel(PlotterAdapter.PARAMETER_PLOT_COLUMNS, plotList);
			changeListenerElements.add(selectionModel);
			plotList.setSelectionModel(selectionModel);
			plotList.setToolTipText(toolTip);
			plotList.setBorder(BorderFactory.createLoweredBevelBorder());
			plotList.setCellRenderer(new LineStyleCellRenderer(plotter));
			plotList.addListSelectionListener(new ListSelectionListener() {

				public void valueChanged(ListSelectionEvent e) {
					if (!e.getValueIsAdjusting()) {
						List<String> list = new LinkedList<String>();
						for (int i = 0; i < plotList.getModel().getSize(); i++) {
							if (plotList.isSelectedIndex(i)) {
								list.add(model.get(i).toString());
							}
						}
						String result = ParameterTypeEnumeration.transformEnumeration2String(list);

						plotterSettings.setParameterAsString(PlotterAdapter.PARAMETER_PLOT_COLUMNS, result);
					}
				}
			});
			plotList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

			JScrollPane listScrollPane = new ExtendedJScrollPane(plotList);
			listScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
			c.weighty = 1.0;
//			c.weightx = 0;
			this.add(listScrollPane, c);
			c.weighty = 0.0;
//			c.weightx = 1;
			break;
		case Plotter.SINGLE_SELECTION:
			final ListeningJComboBox plotCombo = new ListeningJComboBox(PlotterAdapter.PARAMETER_PLOT_COLUMN, 200);
				plotCombo.setToolTipText(toolTip);
				plotCombo.addItem("None");
				
			changeListenerElements.add(plotCombo);
			for (int j = 0; j < dataTable.getNumberOfColumns(); j++) {
				plotCombo.addItem(dataTable.getColumnName(j));
			}
			plotCombo.addItemListener(new ItemListener() {

				public void itemStateChanged(ItemEvent e) {
					plotterSettings.setParameterAsString(PlotterAdapter.PARAMETER_PLOT_COLUMN, plotCombo.getSelectedItem().toString());
				}
			});

			this.add(plotCombo, c);
			break;
		case Plotter.NO_SELECTION:
			// do nothing
			break;
		}

		// log scale
		if (plotter.isSupportingLogScaleForPlotColumns()) {
			final ListeningJCheckBox logScaleBox = new ListeningJCheckBox(PlotterAdapter.PARAMETER_PLOT_COLUMNS + PlotterAdapter.PARAMETER_SUFFIX_LOG_SCALE, "Log Scale", false);
			changeListenerElements.add(logScaleBox);
			logScaleBox.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					plotterSettings.setParameterAsBoolean(PlotterAdapter.PARAMETER_PLOT_COLUMNS + PlotterAdapter.PARAMETER_SUFFIX_LOG_SCALE, logScaleBox.isSelected());
				}
			});
			this.add(logScaleBox, c);
		}

		// sorting
		if (plotter.isSupportingSorting()) {
			final ListeningJCheckBox sortingBox = new ListeningJCheckBox(PlotterAdapter.PARAMETER_SUFFIX_SORTING, "Sorting", false);
			changeListenerElements.add(sortingBox);
			sortingBox.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					plotterSettings.setParameterAsBoolean(PlotterAdapter.PARAMETER_SUFFIX_SORTING, sortingBox.isSelected());
				}
			});
			this.add(sortingBox, c);
		}

		// sorting
		if (plotter.isSupportingAbsoluteValues()) {
			final ListeningJCheckBox absoluteBox = new ListeningJCheckBox(PlotterAdapter.PARAMETER_SUFFIX_ABSOLUTE_VALUES, "Absolute Values", false);
			changeListenerElements.add(absoluteBox);
			absoluteBox.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					plotterSettings.setParameterAsBoolean(PlotterAdapter.PARAMETER_SUFFIX_ABSOLUTE_VALUES, absoluteBox.isSelected());
				}
			});
			this.add(absoluteBox, c);
		}

		// zooming
		if (plotter.canHandleZooming()) {
			label = new JLabel("Zooming");
			toolTip = "Set a new zooming factor.";
			label.setToolTipText(toolTip);
			this.add(label, c);
			final ListeningJSlider zoomingSlider = new ListeningJSlider(PlotterAdapter.PARAMETER_SUFFIX_ZOOM_FACTOR, 1, 100, plotter.getInitialZoomFactor());
			changeListenerElements.add(zoomingSlider);
			zoomingSlider.setToolTipText(toolTip);
			this.add(zoomingSlider, c);
			zoomingSlider.addChangeListener(new ChangeListener() {
				public void stateChanged(ChangeEvent e) {
					plotterSettings.setParameterAsInt(PlotterAdapter.PARAMETER_SUFFIX_ZOOM_FACTOR, zoomingSlider.getValue());					
				}
			});
		}

		// jitter
		if (plotter.canHandleJitter()) {
			label = new JLabel("Jitter");
			toolTip = "Select the amount of jittering (small perturbation of data points).";
			label.setToolTipText(toolTip);
			this.add(label, c);
			final ListeningJSlider jitterSlider = new ListeningJSlider(PlotterAdapter.PARAMETER_JITTER_AMOUNT, 0, 100, 0);
			changeListenerElements.add(jitterSlider);
			jitterSlider.setToolTipText(toolTip);
			this.add(jitterSlider, c);
			jitterSlider.addChangeListener(new ChangeListener() {
				public void stateChanged(ChangeEvent e) {
					plotterSettings.setParameterAsInt(PlotterAdapter.PARAMETER_JITTER_AMOUNT, jitterSlider.getValue());
				}
			});
		}

		// option dialog
		if (plotter.hasOptionsDialog()) {
			toolTip = "Opens a dialog with further options for this plotter.";
			JButton optionsButton = new JButton("Options");
			optionsButton.setToolTipText(toolTip);
			this.add(optionsButton, c);
			optionsButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					plotter.showOptionsDialog();
				}
			});
		}

		// Add the plotter options components for user interaction, if provided
		int componentCounter = 0;
		while (plotter.getOptionsComponent(componentCounter) != null) {
			Component options = plotter.getOptionsComponent(componentCounter);			
			this.add(options, c);
			componentCounter++;
		}

		// Save image button for the plotter
		// no longer needed because export now works for old plotters out of the box
//		if (!plotter.hasSaveImageButton()) {
//			JButton imageButton = new JButton(new ResourceAction(true, "save_image") {
//				private static final long serialVersionUID = -6568814929011124484L;
//
//				@Override
//				public void actionPerformed(ActionEvent e) {
//					Component tosave = plotter.getPlotter();
//					ExportDialog exportDialog = new ExportDialog("RapidMiner");
//					exportDialog.showExportDialog(ApplicationFrame.getApplicationFrame(), "Save Image...", tosave, "plot");
//				}
//			});
//			this.add(imageButton, c);
//		}

		// check if savable (for data)
		if (plotter.isSaveable()) {
			toolTip = "Saves the data underlying this plot into a file.";
			JButton saveButton = new JButton(new ResourceAction("save_result") {
				private static final long serialVersionUID = 1L;
				public void actionPerformed(ActionEvent e) {
					plotter.save();
				}
			});
			saveButton.setToolTipText(toolTip);
			this.add(saveButton, c);
		}

		// coordinates
		if (plotter.isProvidingCoordinates()) {
			toolTip = "The current coordinates of the mouese cursor with respect to the data dimensions.";
			coordinatesLabel.setToolTipText(toolTip);
			coordinatesLabel.setBorder(BorderFactory.createEtchedBorder());
			coordinatesLabel.setFont(new Font("Monospaced", Font.PLAIN, coordinatesLabel.getFont().getSize()));
			this.add(coordinatesLabel, c);
		}

		// add fill component if necessary (glue)
		if (plotter.getValuePlotSelectionType() != Plotter.MULTIPLE_SELECTION) {
			c.weighty = 1.0;
			JPanel fillPanel = new JPanel();
			this.add(fillPanel, c);
			c.weighty = 0.0;
		}

		this.setAlignmentX(LEFT_ALIGNMENT);

		revalidate();
		repaint();
	}


	public void updatePlotterCombo() {
		plotterCombo.removeItemListener(plotterComboListener);
		plotterCombo.removeAllItems();
		Iterator<String> n = plotterSettings.getAvailablePlotters().keySet().iterator();
		while (n.hasNext()) {
			String plotterName = n.next();
			try {
				Class<? extends Plotter> plotterClass = plotterSettings.getAvailablePlotters().get(plotterName);
				if (plotterClass != null) {
					//TODO: Make this more elegant...
					plotterCombo.addItem(plotterName);
				}
			} catch (IllegalArgumentException e) {
				//LogService.getGlobal().log("Plotter control panel: cannot instantiate plotter '" + plotterName + "'. Skipping...", LogService.WARNING);
				LogService.getRoot().log(Level.WARNING, "com.rapidminer.gui.plotter.PlotterControlPanel.instatiating_plotter_error", plotterName);
			} catch (SecurityException e) {
				//LogService.getGlobal().log("Plotter control panel: cannot instantiate plotter '" + plotterName + "'. Skipping...", LogService.WARNING);
				LogService.getRoot().log(Level.WARNING, "com.rapidminer.gui.plotter.PlotterControlPanel.instatiating_plotter_error", plotterName);
			}
		}
		plotterCombo.setToolTipText("The plotter which should be used for displaying data.");
		plotterCombo.addItemListener(plotterComboListener);
	}

	@Override
	public List<PlotterSettingsChangedListener> getListeningObjects() {
		return changeListenerElements;
	}
	
	@Override
	public void plotterChanged(String plotterName) {
		plotterCombo.setSelectedItem(plotterName);
		updateControls();
	}
}
