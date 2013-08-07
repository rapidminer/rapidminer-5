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
package com.rapidminer.operator.learner.functions.neuralnet;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.encog.matrix.Matrix;
import org.encog.neural.data.NeuralData;
import org.encog.neural.data.basic.BasicNeuralData;
import org.encog.neural.networks.BasicNetwork;
import org.encog.neural.networks.Layer;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.learner.PredictionModel;
import com.rapidminer.tools.Tools;

/**
 * This is the model for the simple neural net learner.
 * 
 * @author Ingo Mierswa
 */
public class SimpleNeuralNetModel extends PredictionModel {

	private static final long serialVersionUID = 332041465701627316L;

	private BasicNetwork network;

	private String[] attributeNames;

	private double[] attributeMin;
	private double[] attributeMax;
	private double labelMin;
	private double labelMax;

	protected SimpleNeuralNetModel(ExampleSet trainingExampleSet, BasicNetwork network, double[] attributeMin, double[] attributeMax, double labelMin, double labelMax) {
		super(trainingExampleSet);
		this.network = network;
		this.attributeNames = com.rapidminer.example.Tools.getRegularAttributeNames(trainingExampleSet);
		this.attributeMin = attributeMin;
		this.attributeMax = attributeMax;
		this.labelMin = labelMin;
		this.labelMax = labelMax;
	}

	public BasicNetwork getNeuralNet() {
		return this.network;
	}

	public String[] getAttributeNames() {
		return this.attributeNames;
	}

	@Override
	public ExampleSet performPrediction(ExampleSet exampleSet, Attribute predictedLabel) throws OperatorException {		
		for (Example example : exampleSet) {
			double[] data = new double[attributeNames.length];
			for (int i = 0; i < attributeNames.length; i++) {
				if (attributeMin[i] != attributeMax[i]) {
					data[i] = (example.getValue(exampleSet.getAttributes().get(attributeNames[i])) - attributeMin[i]) / (attributeMax[i] - attributeMin[i]);
				} else {
					data[i] = example.getValue(exampleSet.getAttributes().get(attributeNames[i])) - attributeMin[i];
				}
			}
			NeuralData neuralData = new BasicNeuralData(data);
			double prediction = network.compute(neuralData).getData(0);

			if (predictedLabel.isNominal()) {
				double scaled = (prediction - 0.5d) * 2;
				int index = scaled > 0 ? predictedLabel.getMapping().getPositiveIndex() : predictedLabel.getMapping().getNegativeIndex();
				example.setValue(predictedLabel, index);
				example.setConfidence(predictedLabel.getMapping().getPositiveString(), 1.0d / (1.0d + java.lang.Math.exp(-scaled)));
				example.setConfidence(predictedLabel.getMapping().getNegativeString(), 1.0d / (1.0d + java.lang.Math.exp(scaled)));			
			} else {
				example.setValue(predictedLabel, prediction * (labelMax - labelMin) + labelMin);
			}
		}
		return exampleSet;
	}

	@Override
	public String toString() {
		StringBuffer result = new StringBuffer();
		List<Layer> layers = this.network.getLayers();
		Iterator i = layers.iterator();
		int layerIndex = 0;
		while (i.hasNext()) {
			Layer layer = (Layer)i.next();
			String nodeString = layer.getNeuronCount() == 1 ? "1 node" : layer.getNeuronCount() + " nodes";
			String titleString = "Layer " + (layerIndex + 1) + " (" + nodeString + ")";
			result.append(titleString + Tools.getLineSeparator());
			for (int t = 0; t < titleString.length(); t++)
				result.append("-");
			result.append(Tools.getLineSeparator());
			if (layerIndex == 0) {
				result.append(Arrays.asList(this.attributeNames).toString() + Tools.getLineSeparators(2));
				if (layer.hasMatrix()) {
					layerWeightsToString(result, layer.getMatrix(), layerIndex);
				}
			} else {
				if (layer.hasMatrix()) {
					layerWeightsToString(result, layer.getMatrix(), layerIndex);
				}
			}
			result.append(Tools.getLineSeparator());
			layerIndex++;
		}
		return result.toString();
	}

	private void layerWeightsToString(StringBuffer result, Matrix matrix, int currentLayerIndex) {
		result.append("Output Weights:" + Tools.getLineSeparator());

		// number of columns: number of following nodes
		// number of rows: number of this layer's nodes plus 1 for the threshold
		int rows = matrix.getRows();
		int cols = matrix.getCols();

		for (int c = 0; c < cols; c++) {
			result.append(Tools.getLineSeparator() + "* To Layer " + (currentLayerIndex + 2) + " - Node " + (c + 1) + ":" + Tools.getLineSeparator());
			for (int r = 0; r < rows - 1; r++) {
				result.append("From Node " + (r + 1) + ": ");
				result.append(matrix.get(r, c));
				result.append(Tools.getLineSeparator());
			}
			result.append("From Threshold Node: ");
			result.append(matrix.get(rows - 1, c));
			result.append(Tools.getLineSeparator());
		}
	}
}
