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
import java.util.Vector;

import org.joone.engine.Layer;
import org.joone.engine.Matrix;
import org.joone.engine.Synapse;
import org.joone.io.MemoryInputSynapse;
import org.joone.io.MemoryOutputSynapse;
import org.joone.net.NeuralNet;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.learner.PredictionModel;
import com.rapidminer.tools.Tools;


/**
 * This is the model for the neural net learner.
 * 
 * @author Ingo Mierswa
 */
public class NeuralNetModel extends PredictionModel {

	private static final long serialVersionUID = 776221623930869372L;

	private NeuralNet neuralNet;

	private String[] attributeNames;

	private int numberOfInputAttributes;

	private double minLabel;

	private double maxLabel;

	public NeuralNetModel(ExampleSet exampleSet, NeuralNet neuralNet, int numberOfInputAttributes, double minLabel, double maxLabel) {
		super(exampleSet);
		this.attributeNames = com.rapidminer.example.Tools.getRegularAttributeNames(exampleSet);
		this.neuralNet = neuralNet;
		this.numberOfInputAttributes = numberOfInputAttributes;
		this.minLabel = minLabel;
		this.maxLabel = maxLabel;
	}

	public NeuralNet getNeuralNet() {
		return this.neuralNet;
	}

	public String[] getAttributeNames() {
		return this.attributeNames;
	}

	@Override
	public ExampleSet performPrediction(ExampleSet exampleSet, Attribute predictedLabel) throws OperatorException {
		// remove input layer inputs
		Layer input = this.neuralNet.getInputLayer();
		input.removeAllInputs();

		MemoryInputSynapse memInp = new MemoryInputSynapse();
		memInp.setFirstRow(1);
		memInp.setAdvancedColumnSelector("1-" + this.numberOfInputAttributes);

		input.addInputSynapse(memInp);
		memInp.setInputArray(createInputData(exampleSet));

		// remove output layer outputs
		Layer output = this.neuralNet.getOutputLayer();
		output.removeAllOutputs();

		// Now we interrogate the net once with the input patterns
		this.neuralNet.getMonitor().setTotCicles(1);
		this.neuralNet.getMonitor().setTrainingPatterns(exampleSet.size());
		this.neuralNet.getMonitor().setLearning(false);

		double[] predictions = recall(this.neuralNet);

		Iterator<Example> i = exampleSet.iterator();
		int counter = 0;
		while (i.hasNext()) {
			Example example = i.next();
			double prediction = predictions[counter];
			if (predictedLabel.isNominal()) {
				double scaled = (prediction - 0.5d) * 2;
				int index = scaled > 0 ? predictedLabel.getMapping().getPositiveIndex() : predictedLabel.getMapping().getNegativeIndex();
				example.setValue(predictedLabel, index);
				example.setConfidence(predictedLabel.getMapping().getPositiveString(), 1.0d / (1.0d + java.lang.Math.exp(-scaled)));
				example.setConfidence(predictedLabel.getMapping().getNegativeString(), 1.0d / (1.0d + java.lang.Math.exp(scaled)));			
			} else {
				example.setValue(predictedLabel, prediction * (this.maxLabel - this.minLabel) + this.minLabel);
			}
			counter++;
		}

		return exampleSet;
	}

	private double[] recall(NeuralNet net) {
		MemoryOutputSynapse output = new MemoryOutputSynapse();

		// inject the input and get the output
		neuralNet.addOutputSynapse(output);
		neuralNet.start(); // init layers
		neuralNet.getMonitor().Go();
		neuralNet.join();
		int cc = neuralNet.getMonitor().getTrainingPatterns();
		double[] result = new double[cc];
		for (int i = 0; i < cc; i++) {
			double[] pattern = output.getNextPattern();
			result[i] = pattern[0];
		}
		neuralNet.stop();
		return result;
	}

	private double[][] createInputData(ExampleSet exampleSet) {
		double[][] result = new double[exampleSet.size()][exampleSet.getAttributes().size()];
		int counter = 0;
		Iterator<Example> i = exampleSet.iterator();
		while (i.hasNext()) {
			Example example = i.next();
			int a = 0; 
			for (Attribute attribute : exampleSet.getAttributes())
				result[counter][a++] = example.getValue(attribute);
			counter++;
		}
		return result;
	}

	@Override
	public String toString() {
		StringBuffer result = new StringBuffer();
		Vector layers = this.neuralNet.getLayers();
		Iterator i = layers.iterator();
		int layerIndex = 0;
		while (i.hasNext()) {
			Layer layer = (Layer)i.next();
			String nodeString = layer.getRows() == 1 ? "1 node" : layer.getRows() + " nodes";
			String titleString = "Layer '" + layer.getLayerName() + "' (" + nodeString + ")";
			result.append(titleString + Tools.getLineSeparator());
			for (int t = 0; t < titleString.length(); t++)
				result.append("-");
			result.append(Tools.getLineSeparator());
			if (layerIndex == 0) {
				result.append(Arrays.asList(this.attributeNames).toString() + Tools.getLineSeparator());
			} else {
				result.append("Input Weights:" + Tools.getLineSeparator());
				Vector inputs = layer.getAllInputs();
				Iterator o = inputs.iterator();
				while (o.hasNext()) {
					Object object = o.next();
					if (object instanceof Synapse) {
						Synapse synapse = (Synapse)object;
						Matrix weights = synapse.getWeights();
						// #rows --> input nodes
						// #columns --> output nodes
						if (weights != null) {
							int inputRows  = weights.getM_rows();
							int outputRows = weights.getM_cols();
							for (int y = 0; y < outputRows; y++) {
								result.append("Node " + (y + 1) + Tools.getLineSeparator());
								for (int x = 0; x < inputRows; x++) {
									result.append(weights.value[x][y] + Tools.getLineSeparator());
								}
							}
						}
					}
				}
			}
			result.append(Tools.getLineSeparator());
			layerIndex++;
		}
		return result.toString();
	}
}
