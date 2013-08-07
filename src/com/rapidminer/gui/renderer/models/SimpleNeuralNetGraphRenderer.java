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
package com.rapidminer.gui.renderer.models;

import java.awt.Component;

import com.rapidminer.gui.renderer.AbstractRenderer;
import com.rapidminer.gui.tools.ExtendedJScrollPane;
import com.rapidminer.operator.IOContainer;
import com.rapidminer.operator.learner.functions.neuralnet.SimpleNeuralNetModel;
import com.rapidminer.operator.learner.functions.neuralnet.SimpleNeuralNetVisualizer;
import com.rapidminer.report.Reportable;

/**
 * A renderer for the graph view of the new simpler neural network implementation.
 * 
 * @author Ingo Mierswa
 */
public class SimpleNeuralNetGraphRenderer extends AbstractRenderer {

	public String getName() {
		return "Graph View";
	}
	
	public Reportable createReportable(Object renderable, IOContainer ioContainer, int width, int height) {
		SimpleNeuralNetModel neuralNetModel = (SimpleNeuralNetModel) renderable;
		SimpleNeuralNetVisualizer plotter = new SimpleNeuralNetVisualizer(neuralNetModel);
		plotter.setSize(width, height);
		return plotter;
	}

	public Component getVisualizationComponent(Object renderable, IOContainer ioContainer) {
		SimpleNeuralNetModel neuralNetModel = (SimpleNeuralNetModel) renderable;
		return new ExtendedJScrollPane(new SimpleNeuralNetVisualizer(neuralNetModel));
	}
}
