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
package com.rapidminer.gui.renderer.data;

import java.awt.Component;
import java.util.LinkedList;
import java.util.List;

import javax.swing.table.TableModel;

import com.rapidminer.example.ExampleSet;
import com.rapidminer.gui.plotter.PlotterAdapter;
import com.rapidminer.gui.renderer.AbstractTableModelTableRenderer;
import com.rapidminer.gui.viewer.MetaDataViewer;
import com.rapidminer.gui.viewer.MetaDataViewerTableModel;
import com.rapidminer.operator.IOContainer;
import com.rapidminer.operator.UserError;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.tools.AttributeSubsetSelector;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeBoolean;
import com.rapidminer.parameter.UndefinedParameterError;

/**
 * A renderer for the meta data view of example sets.
 * 
 * @author Ingo Mierswa
 */
public class ExampleSetMetaDataRenderer extends AbstractTableModelTableRenderer {

	private AttributeSubsetSelector subsetSelector = null;
	
	@Override
	public String getName() {
		return "Meta Data View";
	}
	
	@Override
	public Component getVisualizationComponent(Object renderable, IOContainer ioContainer) {
		ExampleSet exampleSet = (ExampleSet)renderable;
		return new MetaDataViewer(exampleSet, true);
	}

	@Override
	public TableModel getTableModel(Object renderable, IOContainer ioContainer, boolean isReporting) {
		ExampleSet exampleSet = (ExampleSet)renderable;
		
		if (isReporting && subsetSelector != null) {
			try {
				exampleSet = subsetSelector.getSubset(exampleSet, false);
			} catch (UndefinedParameterError e) {
			} catch (UserError e) {
			}
		}
		MetaDataViewerTableModel model = new MetaDataViewerTableModel(exampleSet);
		
		for (int i = 0; i < MetaDataViewerTableModel.COLUMN_NAMES.length; i++) {
			model.setShowColumn(i, getParameterAsBoolean("show_" + PlotterAdapter.transformParameterName(MetaDataViewerTableModel.COLUMN_NAMES[i])));
		}
		return model;
	}
	
	@Override
	public List<ParameterType> getParameterTypes(InputPort inputPort) {
		List<ParameterType> types = new LinkedList<ParameterType>();
		
		subsetSelector = new AttributeSubsetSelector(this, inputPort);
		types.addAll(subsetSelector.getParameterTypes());	
		for (int i = 0; i < MetaDataViewerTableModel.COLUMN_NAMES.length; i++) {
			ParameterType type = new ParameterTypeBoolean("show_" + PlotterAdapter.transformParameterName(MetaDataViewerTableModel.COLUMN_NAMES[i]), MetaDataViewerTableModel.COLUMN_TOOL_TIPS[i], true, false);
			types.add(type);
		}
		
		return types;
	}
}
