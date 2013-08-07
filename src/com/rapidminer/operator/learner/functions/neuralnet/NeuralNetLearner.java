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

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.joone.engine.FullSynapse;
import org.joone.engine.GaussianLayer;
import org.joone.engine.Layer;
import org.joone.engine.LinearLayer;
import org.joone.engine.LogarithmicLayer;
import org.joone.engine.Monitor;
import org.joone.engine.NeuralNetEvent;
import org.joone.engine.NeuralNetListener;
import org.joone.engine.SigmoidLayer;
import org.joone.engine.SineLayer;
import org.joone.engine.TanhLayer;
import org.joone.engine.learning.TeachingSynapse;
import org.joone.engine.listeners.ErrorBasedTerminator;
import org.joone.io.MemoryInputSynapse;
import org.joone.io.MemoryOutputSynapse;
import org.joone.net.NeuralNet;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.operator.Model;
import com.rapidminer.operator.OperatorCapability;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.UserError;
import com.rapidminer.operator.learner.AbstractLearner;
import com.rapidminer.operator.learner.PredictionModel;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeCategory;
import com.rapidminer.parameter.ParameterTypeDouble;
import com.rapidminer.parameter.ParameterTypeInt;
import com.rapidminer.parameter.ParameterTypeList;
import com.rapidminer.parameter.UndefinedParameterError;


/**
 * <p>This operator learns a model by means of a feed-forward neural network. The learning is
 * done via backpropagation. The user can define the structure of the neural network with the
 * parameter list &quot;hidden_layer_types&quot;. Each list entry describes a new hidden
 * layer. The key of each entry must correspond to the layer type which must be one out of</p>
 * 
 * <ul>
 * <li>linear</li>
 * <li>sigmoid (default)</li>
 * <li>tanh</li>
 * <li>sine</li>
 * <li>logarithmic</li>
 * <li>gaussian</li>
 * </ul>
 * 
 * <p>The key of each entry must be a number defining the size of the hidden layer. A size value
 * of -1 or 0 indicates that the layer size should be calculated from the number of attributes
 * of the input example set. In this case, the layer size will be set to 
 * (number of attributes + number of classes) / 2 + 1.</p>
 * 
 * <p>If the user does not specify any hidden layers, a default hidden layer with 
 * sigmoid type and size (number of attributes + number of classes) / 2 + 1 will be created and 
 * added to the net.</p>
 * 
 * <p>The type of the input nodes is sigmoid. The type of the output node is sigmoid is the 
 * learning data describes a classification task and linear for numerical regression tasks.</p>
 * 
 * @rapidminer.index Neural Net
 * 
 * @author Ingo Mierswa
 */
public class NeuralNetLearner extends AbstractLearner implements NeuralNetListener {

	/** The parameter name for &quot;The default layer type for the input layers.&quot; */
	public static final String PARAMETER_INPUT_LAYER_TYPE = "input_layer_type";

	/** The parameter name for &quot;The default layer type for the output layers.&quot; */
	public static final String PARAMETER_OUTPUT_LAYER_TYPE = "output_layer_type";

	/** The parameter name for &quot;The number of hidden layers. Only used if no layers are defined by the list hidden_layer_types.&quot; */
	public static final String PARAMETER_DEFAULT_NUMBER_OF_HIDDEN_LAYERS = "default_number_of_hidden_layers";

	/** The parameter name for &quot;The default size  of hidden layers. Only used if no layers are defined by the list hidden_layer_types. -1 means size (number of attributes + number of classes) / 2&quot; */
	public static final String PARAMETER_DEFAULT_HIDDEN_LAYER_SIZE = "default_hidden_layer_size";

	/** The parameter name for &quot;The default layer type for the hidden layers. Only used if the parameter list hidden_layer_types is not defined.&quot; */
	public static final String PARAMETER_DEFAULT_HIDDEN_LAYER_TYPE = "default_hidden_layer_type";

	/** The parameter name for &quot;Describes the name, the size, and the type of all hidden layers&quot; */
	public static final String PARAMETER_HIDDEN_LAYER_TYPES = "hidden_layer_types";

	/** The parameter name for &quot;The number of training cycles used for the neural network training.&quot; */
	public static final String PARAMETER_TRAINING_CYCLES = "training_cycles";

	/** The parameter name for &quot;The learning rate determines by how much we change the weights at each step.&quot; */
	public static final String PARAMETER_LEARNING_RATE = "learning_rate";

	/** The parameter name for &quot;The momentum simply adds a fraction of the previous weight update to the current one (prevent local maxima and smoothes optimization directions).&quot; */
	public static final String PARAMETER_MOMENTUM = "momentum";

	/** The parameter name for &quot;The optimization is stopped if the training error gets below this epsilon value.&quot; */
	public static final String PARAMETER_ERROR_EPSILON = "error_epsilon";

	private static final String[] LAYER_TYPES = new String[] {
		"linear",
		"sigmoid",
		"tanh",
		"sine",
		"logarithmic",
		"gaussian"
	};

	private static final int LINEAR      = 0;
	private static final int SIGMOID     = 1;
	private static final int TANH        = 2;
	private static final int SINE        = 3;
	private static final int LOGARITHMIC = 4;
	private static final int GAUSSIAN    = 5;

	private NeuralNet neuralNet;

	private MemoryInputSynapse inputSynapse;

	private MemoryInputSynapse desiredOutputSynapse;

	private double minLabel, maxLabel;


	/** Creates a new Neural Network learner. */
	public NeuralNetLearner(OperatorDescription description) {
		super(description);
	}

	private int getDefaultLayerSize(ExampleSet exampleSet) {
		return (int)Math.round(exampleSet.getAttributes().size() / 2.0d) + 1;
	}

	/** Learns and returns a model. */
	public Model learn(ExampleSet exampleSet) throws OperatorException {
		Attribute label = exampleSet.getAttributes().getLabel();
		if (label.isNominal()) {
			if (label.getMapping().size() != 2) {
				throw new UserError(this, 114, getName(), label.getName());
			}
		}
		initNeuralNet(exampleSet);
		train(exampleSet);
		return new NeuralNetModel(exampleSet, neuralNet, exampleSet.getAttributes().size(), this.minLabel, this.maxLabel);
	}

	private Layer createLayer(String layerTypeName, int size, int counter) {
		return createLayer("Hidden", layerTypeName, size, counter);
	}

	private Layer createLayer(String layerName, String layerTypeName, int size, int counter) {
		Layer layer = null;
		if (LAYER_TYPES[LINEAR].equals(layerTypeName.toLowerCase())) {
			layer = new LinearLayer(); 
		} else if (LAYER_TYPES[SIGMOID].equals(layerTypeName.toLowerCase())) {
			layer = new SigmoidLayer();
		} else if (LAYER_TYPES[TANH].equals(layerTypeName.toLowerCase())) {
			layer = new TanhLayer();
		} else if (LAYER_TYPES[SINE].equals(layerTypeName.toLowerCase())) {
			layer = new SineLayer();
		} else if (LAYER_TYPES[LOGARITHMIC].equals(layerTypeName.toLowerCase())) {
			layer = new LogarithmicLayer();
		} else if (LAYER_TYPES[GAUSSIAN].equals(layerTypeName.toLowerCase())) {
			layer = new GaussianLayer();
		} else {
			logWarning("Cannot create layer of type '" + layerTypeName + "', using sigmoid layer instead.");
			layer = new SigmoidLayer();
		}
		layer.setRows(size);
		String name = layerName;
		if (counter >= 0)
			name += "-" + counter;
		name += " [" + layerTypeName + "]";
		layer.setLayerName(name);
		return layer;
	}

	private void initNeuralNet(ExampleSet exampleSet) throws UndefinedParameterError {
		// First create the layers
		Layer input = createLayer("Input", LAYER_TYPES[getParameterAsInt(PARAMETER_INPUT_LAYER_TYPE)], exampleSet.getAttributes().size(), -1);		
		Layer output = createLayer("Output", LAYER_TYPES[getParameterAsInt(PARAMETER_OUTPUT_LAYER_TYPE)], 1, -1);

		// create hidden layers
		LinkedList<Layer> allHiddenLayers = new LinkedList<Layer>();
		List<String[]> hiddenLayerList = getParameterList(PARAMETER_HIDDEN_LAYER_TYPES);
		Iterator<String[]> i = hiddenLayerList.iterator();
		int counter = 1;
		while (i.hasNext()) {
			String[] typeSizePair = i.next();
			String layerType = typeSizePair[0];
			Integer layerSizeObject = Integer.valueOf(typeSizePair[1]);
			int layerSize = layerSizeObject;
			if (layerSize <= 0)
				layerSize = getDefaultLayerSize(exampleSet);
			Layer hiddenLayer = createLayer(layerType, layerSize, counter);
			allHiddenLayers.add(hiddenLayer);
			counter++;
		}

		// create at least one hidden layer if no other layers were created
		if (allHiddenLayers.size() == 0) {
			log("No hidden layers defined. Using default hidden layers.");
			String layerType = LAYER_TYPES[getParameterAsInt(PARAMETER_DEFAULT_HIDDEN_LAYER_TYPE)];
			int layerSize = getParameterAsInt(PARAMETER_DEFAULT_HIDDEN_LAYER_SIZE);
			if (layerSize <= 0)
				layerSize = getDefaultLayerSize(exampleSet);
			for (int p = 0; p < getParameterAsInt(PARAMETER_DEFAULT_NUMBER_OF_HIDDEN_LAYERS); p++) {
				allHiddenLayers.add(createLayer(layerType, layerSize, (p+1)));
			}
		}

		// now create the synapses between all hidden layers
		Layer last = null;
		Iterator<Layer> l = allHiddenLayers.iterator();
		while (l.hasNext()) {
			Layer current = l.next();
			if (last != null) {
				FullSynapse synapse_HH = new FullSynapse();
				last.addOutputSynapse(synapse_HH);
				current.addInputSynapse(synapse_HH);
			}
			last = current;
		}

		// Connect the input layer with the first hidden layer
		FullSynapse synapse_IH = new FullSynapse();
		input.addOutputSynapse(synapse_IH);
		allHiddenLayers.getFirst().addInputSynapse(synapse_IH);

		// Connect the last hidden layer with the output layer
		FullSynapse synapse_HO = new FullSynapse();
		allHiddenLayers.getLast().addOutputSynapse(synapse_HO);
		output.addInputSynapse(synapse_HO);

		// the input to the neural net
		inputSynapse = new MemoryInputSynapse();
		input.addInputSynapse(inputSynapse);

		// the output of the neural net
		MemoryOutputSynapse outputSynapse = new MemoryOutputSynapse();
		output.addOutputSynapse(outputSynapse);

		// the trainer and its desired output
		TeachingSynapse trainer = new TeachingSynapse();
		desiredOutputSynapse = new MemoryInputSynapse();
		trainer.setDesired(desiredOutputSynapse);

		// now we add the complete structure to a NeuralNet object
		neuralNet = new NeuralNet();

		neuralNet.addLayer(input, NeuralNet.INPUT_LAYER);
		Iterator<Layer> h = allHiddenLayers.iterator();
		while (h.hasNext()) {
			neuralNet.addLayer(h.next(), NeuralNet.HIDDEN_LAYER);	
		}
		neuralNet.addLayer(output, NeuralNet.OUTPUT_LAYER);
		neuralNet.setTeacher(trainer);
		output.addOutputSynapse(trainer);

		ErrorBasedTerminator terminator = new ErrorBasedTerminator(getParameterAsDouble(PARAMETER_ERROR_EPSILON));
		terminator.setNeuralNet(neuralNet);
		neuralNet.getMonitor().addNeuralNetListener(terminator);
	}

	public void train(ExampleSet exampleSet) throws UndefinedParameterError {
		double[][] inputArray = createInputData(exampleSet);

		// set the inputs
		inputSynapse.setInputArray(inputArray);
		inputSynapse.setAdvancedColumnSelector("1-" + exampleSet.getAttributes().size());

		// set the desired outputs
		desiredOutputSynapse.setInputArray(inputArray);
		desiredOutputSynapse.setAdvancedColumnSelector((exampleSet.getAttributes().size() + 1) + "");

		// get the monitor object to train or feed forward
		Monitor monitor = neuralNet.getMonitor();

		// set the monitor parameters
		monitor.setLearningRate(getParameterAsDouble(PARAMETER_LEARNING_RATE));
		monitor.setMomentum(getParameterAsDouble(PARAMETER_MOMENTUM));
		monitor.setTrainingPatterns(inputArray.length);
		monitor.setTotCicles(getParameterAsInt(PARAMETER_TRAINING_CYCLES));
		monitor.setLearning(true);
		neuralNet.getMonitor().addNeuralNetListener(this);
		neuralNet.start();
		neuralNet.getMonitor().Go();
		neuralNet.join();
	}

	public void cicleTerminated(NeuralNetEvent e) {}

	public void errorChanged(NeuralNetEvent e) {
	}

	public void netStarted(NeuralNetEvent e) {
		log("learning started.");
	}

	public void netStopped(NeuralNetEvent e) {
		log("learning finished.");
	}

	public void netStoppedError(NeuralNetEvent e, String error) {
		logError("learning stopped, error: " + error);
	}

	private double[][] createInputData(ExampleSet exampleSet) {
		double[][] result = null;
		result = new double[exampleSet.size()][exampleSet.getAttributes().size() + 1];

		int counter = 0;
		Iterator<Example> i = exampleSet.iterator();
		this.maxLabel = Double.NEGATIVE_INFINITY;
		this.minLabel = Double.POSITIVE_INFINITY;
		Attribute label = exampleSet.getAttributes().getLabel();
		while (i.hasNext()) {
			Example example = i.next();
			int a = 0;
			for (Attribute attribute : example.getAttributes()) {
				result[counter][a++] = example.getValue(attribute);
			}
			double labelValue = example.getValue(label);
			if (label.isNominal()) {
				result[counter][exampleSet.getAttributes().size()] = (label.getMapping().getPositiveIndex() == labelValue ? 1.0d : 0.0d);
			} else {
				result[counter][exampleSet.getAttributes().size()] = labelValue;
				this.maxLabel = Math.max(this.maxLabel, labelValue);
				this.minLabel = Math.min(this.minLabel, labelValue);
			}
			counter++;
		}
		if (!label.isNominal()) {
			for (int l = 0; l < result.length; l++) {
				result[l][exampleSet.getAttributes().size()] = (result[l][exampleSet.getAttributes().size()] - this.minLabel) / (this.maxLabel - this.minLabel);
			}
		}
		return result;
	}

	@Override
	public Class<? extends PredictionModel> getModelClass() {
		return NeuralNetModel.class;
	}
	
	/**
	 * Returns true for all types of attributes and numerical and binominal labels.
	 */
	public boolean supportsCapability(OperatorCapability lc) {
		if (lc == OperatorCapability.POLYNOMINAL_ATTRIBUTES)
			return true;
		if (lc == OperatorCapability.BINOMINAL_ATTRIBUTES)
			return true;
		if (lc == OperatorCapability.NUMERICAL_ATTRIBUTES)
			return true;
		if (lc == OperatorCapability.BINOMINAL_LABEL)
			return true;
		if (lc == OperatorCapability.NUMERICAL_LABEL)
			return true;
		return false;
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();
		ParameterType type = new ParameterTypeCategory(PARAMETER_INPUT_LAYER_TYPE, "The default layer type for the input layers.", LAYER_TYPES, LINEAR);
		type.setExpert(false);
		types.add(type);
		type = new ParameterTypeCategory(PARAMETER_OUTPUT_LAYER_TYPE, "The default layer type for the output layers.", LAYER_TYPES, SIGMOID);
		type.setExpert(false);
		types.add(type);
		type = new ParameterTypeInt(PARAMETER_DEFAULT_NUMBER_OF_HIDDEN_LAYERS, "The number of hidden layers. Only used if no layers are defined by the list hidden_layer_types.", 1, Integer.MAX_VALUE, 1);
		type.setExpert(false);
		types.add(type);
		types.add(new ParameterTypeInt(PARAMETER_DEFAULT_HIDDEN_LAYER_SIZE, "The default size  of hidden layers. Only used if no layers are defined by the list hidden_layer_types. -1 means size (number of attributes + number of classes) / 2", -1, Integer.MAX_VALUE, -1));
		types.add(new ParameterTypeCategory(PARAMETER_DEFAULT_HIDDEN_LAYER_TYPE, "The default layer type for the hidden layers. Only used if the parameter list hidden_layer_types is not defined.", LAYER_TYPES, SIGMOID));
		types.add(new ParameterTypeList(PARAMETER_HIDDEN_LAYER_TYPES, "Describes the type and size of all hidden layers", 
				new ParameterTypeCategory("hidden_layer_type", "The type of this hidden layer", LAYER_TYPES, 0),
				new ParameterTypeInt("hidden_layer_sizes", "The the size of this hidden layer. A size of <= 0 leads to a layer size of (number_of_attributes + number of classes) / 2.", -1, Integer.MAX_VALUE, -1)));
		type = new ParameterTypeInt(PARAMETER_TRAINING_CYCLES, "The number of training cycles used for the neural network training.", 1, Integer.MAX_VALUE, 200);
		type.setExpert(false);
		types.add(type);
		type = new ParameterTypeDouble(PARAMETER_LEARNING_RATE, "The learning rate determines by how much we change the weights at each step.", 0.0d, 1.0d, 0.3d);
		type.setExpert(false);
		types.add(type);
		types.add(new ParameterTypeDouble(PARAMETER_MOMENTUM, "The momentum simply adds a fraction of the previous weight update to the current one (prevent local maxima and smoothes optimization directions).", 0.0d, 1.0d, 0.2d));
		types.add(new ParameterTypeDouble(PARAMETER_ERROR_EPSILON, "The optimization is stopped if the training error gets below this epsilon value.", 0.0d, Double.POSITIVE_INFINITY, 0.05d));
		return types;
	}
}
