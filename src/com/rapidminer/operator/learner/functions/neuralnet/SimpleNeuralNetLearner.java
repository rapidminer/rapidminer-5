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

import java.util.List;

import org.encog.neural.activation.ActivationLinear;
import org.encog.neural.activation.ActivationSigmoid;
import org.encog.neural.data.NeuralDataSet;
import org.encog.neural.data.basic.BasicNeuralDataSet;
import org.encog.neural.networks.BasicNetwork;
import org.encog.neural.networks.Train;
import org.encog.neural.networks.layers.FeedforwardLayer;
import org.encog.neural.networks.training.backpropagation.Backpropagation;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.Statistics;
import com.rapidminer.operator.Model;
import com.rapidminer.operator.OperatorCapability;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.learner.AbstractLearner;
import com.rapidminer.operator.learner.PredictionModel;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeDouble;
import com.rapidminer.parameter.ParameterTypeInt;
import com.rapidminer.tools.RandomGenerator;


/**
 * <p>This operator learns a model by means of a feed-forward neural network. The learning is
 * done via backpropagation. The user can define the structure of the neural network in two
 * different ways according to the setting of the parameter define_different_hidden_layers.
 * If different hidden layers are defined, the parameter hidden_layer_sizes must be set
 * to a comma separated list of the sizes of all hidden layers, e.g. 3,7,5.
 * If no different hidden layers are defined, the parameters for the default hidden layers
 * are used. A size value of -1 or 0 indicates that the layer size should be calculated from
 * the number of attributes of the input example set. In this case, the layer size will be set to
 * (number of attributes + number of classes) / 2 + 1. All layers have a sigmoid activation
 * function.</p>
 * 
 * <p>If the user does not specify any hidden layers, a default hidden layer with
 * size (number of attributes + number of classes) / 2 + 1 will be created and
 * added to the net.</p>
 * 
 * @rapidminer.index Neural Net
 * 
 * @author Ingo Mierswa
 */
public class SimpleNeuralNetLearner extends AbstractLearner {

    public static final String PARAMETER_DEFINE_DIFFERENT_HIDDEN_LAYERS = "define_different_hidden_layers";

    public static final String PARAMETER_HIDDEN_LAYER_SIZES = "hidden_layer_sizes";

    /** The parameter name for &quot;The number of hidden layers. Only used if no layers are defined by the list hidden_layer_types.&quot; */
    public static final String PARAMETER_DEFAULT_NUMBER_OF_HIDDEN_LAYERS = "default_number_of_hidden_layers";

    /** The parameter name for &quot;The default size  of hidden layers. Only used if no layers are defined by the list hidden_layer_types. -1 means size (number of attributes + number of classes) / 2&quot; */
    public static final String PARAMETER_DEFAULT_HIDDEN_LAYER_SIZE = "default_hidden_layer_size";

    /** The parameter name for &quot;The number of training cycles used for the neural network training.&quot; */
    public static final String PARAMETER_TRAINING_CYCLES = "training_cycles";

    /** The parameter name for &quot;The learning rate determines by how much we change the weights at each step.&quot; */
    public static final String PARAMETER_LEARNING_RATE = "learning_rate";

    /** The parameter name for &quot;The momentum simply adds a fraction of the previous weight update to the current one (prevent local maxima and smoothes optimization directions).&quot; */
    public static final String PARAMETER_MOMENTUM = "momentum";

    /** The parameter name for &quot;The optimization is stopped if the training error gets below this epsilon value.&quot; */
    public static final String PARAMETER_ERROR_EPSILON = "error_epsilon";

    private double[] attributeMin;
    private double[] attributeMax;
    private double labelMin;
    private double labelMax;

    public SimpleNeuralNetLearner(OperatorDescription description) {
        super(description);
    }

    @Override
    public Model learn(ExampleSet exampleSet) throws OperatorException {
        BasicNetwork network = getNetwork(exampleSet);
        NeuralDataSet trainingSet = getTraining(exampleSet);
        network = trainNetwork(network, trainingSet, getParameterAsDouble(PARAMETER_LEARNING_RATE), getParameterAsDouble(PARAMETER_MOMENTUM), getParameterAsDouble(PARAMETER_ERROR_EPSILON), getParameterAsInt(PARAMETER_TRAINING_CYCLES));
        return new SimpleNeuralNetModel(exampleSet, network, attributeMin, attributeMax, labelMin, labelMax);
    }

    private BasicNetwork getNetwork(ExampleSet exampleSet) throws OperatorException {
        BasicNetwork network = new BasicNetwork();

        // input layer
        network.addLayer(new FeedforwardLayer(exampleSet.getAttributes().size()));


        // hidden layers
        log("No hidden layers defined. Using default hidden layers.");
        int layerSize = getParameterAsInt(PARAMETER_DEFAULT_HIDDEN_LAYER_SIZE);
        if (layerSize <= 0)
            layerSize = getDefaultLayerSize(exampleSet);
        for (int p = 0; p < getParameterAsInt(PARAMETER_DEFAULT_NUMBER_OF_HIDDEN_LAYERS); p++) {
            network.addLayer(new FeedforwardLayer(layerSize));
        }


        // output layer
        if (exampleSet.getAttributes().getLabel().isNominal()) {
            network.addLayer(new FeedforwardLayer(new ActivationSigmoid(), 1));
        } else {
            network.addLayer(new FeedforwardLayer(new ActivationLinear(), 1));
        }

        network.reset(RandomGenerator.getRandomGenerator(getParameterAsBoolean(RandomGenerator.PARAMETER_USE_LOCAL_RANDOM_SEED), getParameterAsInt(RandomGenerator.PARAMETER_LOCAL_RANDOM_SEED)));

        return network;
    }


    private int getDefaultLayerSize(ExampleSet exampleSet) {
        return (int)Math.round(exampleSet.getAttributes().size() / 2.0d) + 1;
    }

    private NeuralDataSet getTraining(ExampleSet exampleSet) {
        double[][] data   = new double[exampleSet.size()][exampleSet.getAttributes().size()];
        double[][] labels = new double[exampleSet.size()][1];
        int index = 0;
        Attribute label = exampleSet.getAttributes().getLabel();

        this.attributeMin = new double[exampleSet.getAttributes().size()];
        this.attributeMax = new double[attributeMin.length];
        exampleSet.recalculateAllAttributeStatistics();
        int a = 0;
        for (Attribute attribute : exampleSet.getAttributes()) {
            this.attributeMin[a] = exampleSet.getStatistics(attribute, Statistics.MINIMUM);
            this.attributeMax[a] = exampleSet.getStatistics(attribute, Statistics.MAXIMUM);
            a++;
        }

        this.labelMin = exampleSet.getStatistics(label, Statistics.MINIMUM);
        this.labelMax = exampleSet.getStatistics(label, Statistics.MAXIMUM);

        for (Example example : exampleSet) {
            // attributes
            a = 0;
            for (Attribute attribute : exampleSet.getAttributes()) {
                if (attributeMin[a] != attributeMax[a]) {
                    data[index][a] = (example.getValue(attribute) - attributeMin[a]) / (attributeMax[a] - attributeMin[a]);
                } else {
                    data[index][a] = example.getValue(attribute) - attributeMin[a];
                }
                a++;
            }

            // label
            if (label.isNominal()) {
                labels[index][0] = example.getValue(label);
            } else {
                if (labelMax != labelMin) {
                    labels[index][0] = (example.getValue(label) - labelMin) / (labelMax - labelMin);
                } else {
                    labels[index][0] = example.getValue(label) - labelMin;
                }
            }

            index++;
        }

        return new BasicNeuralDataSet(data, labels);
    }

    private BasicNetwork trainNetwork(BasicNetwork network, NeuralDataSet trainingSet, double learningRate, double momentum, double maxError, int maxIteration) {
        final Train train = new Backpropagation(network, trainingSet, learningRate, momentum);

        int epoch = 1;

        do {
            train.iteration();
            epoch++;
        } while ((epoch < maxIteration) && (train.getError() > maxError));

        return (BasicNetwork)train.getNetwork();
    }

    @Override
    public Class<? extends PredictionModel> getModelClass() {
        return SimpleNeuralNetModel.class;
    }

    /**
     * Returns true for all types of attributes and numerical and binominal labels.
     */
    @Override
    public boolean supportsCapability(OperatorCapability lc) {
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

        ParameterType type = new ParameterTypeInt(PARAMETER_DEFAULT_NUMBER_OF_HIDDEN_LAYERS, "The number of hidden layers. Only used if no layers are defined by the list hidden_layer_types.", 1, Integer.MAX_VALUE, 1);
        type.setExpert(false);
        types.add(type);

        types.add(new ParameterTypeInt(PARAMETER_DEFAULT_HIDDEN_LAYER_SIZE, "The default size  of hidden layers. Only used if no layers are defined by the list hidden_layer_types. -1 means size (number of attributes + number of classes) / 2", -1, Integer.MAX_VALUE, -1));

        type = new ParameterTypeInt(PARAMETER_TRAINING_CYCLES, "The number of training cycles used for the neural network training.", 1, Integer.MAX_VALUE, 500);
        type.setExpert(false);
        types.add(type);

        type = new ParameterTypeDouble(PARAMETER_LEARNING_RATE, "The learning rate determines by how much we change the weights at each step.", 0.0d, 1.0d, 0.3d);
        type.setExpert(false);
        types.add(type);

        types.add(new ParameterTypeDouble(PARAMETER_MOMENTUM, "The momentum simply adds a fraction of the previous weight update to the current one (prevent local maxima and smoothes optimization directions).", 0.0d, 1.0d, 0.2d));

        types.add(new ParameterTypeDouble(PARAMETER_ERROR_EPSILON, "The optimization is stopped if the training error gets below this epsilon value.", 0.0d, Double.POSITIVE_INFINITY, 0.01d));

        types.addAll(RandomGenerator.getRandomGeneratorParameters(this));

        return types;
    }
}
