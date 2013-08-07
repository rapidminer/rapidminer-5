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
import com.rapidminer.operator.learner.functions.neuralnet.NeuralNetModel;
import com.rapidminer.operator.learner.functions.neuralnet.NeuralNetVisualizer;
import com.rapidminer.report.Reportable;

/**
 * A renderer for the graph view of a neural network.
 * 
 * @author Ingo Mierswa
 */
public class NeuralNetGraphRenderer extends AbstractRenderer {

	public String getName() {
		return "Graph View";
	}
	
	public Reportable createReportable(Object renderable, IOContainer ioContainer, int width, int height) {
		NeuralNetModel neuralNetModel = (NeuralNetModel) renderable;
		NeuralNetVisualizer plotter = new NeuralNetVisualizer(neuralNetModel);
		plotter.setSize(width, height);
		return plotter;
	}

	public Component getVisualizationComponent(Object renderable, IOContainer ioContainer) {
		NeuralNetModel neuralNetModel = (NeuralNetModel) renderable;
		return new ExtendedJScrollPane(new NeuralNetVisualizer(neuralNetModel));
	}
}
