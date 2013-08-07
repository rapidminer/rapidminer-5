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
package com.rapidminer.operator.preprocessing.normalization;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import com.rapidminer.example.Attribute;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.OutputPort;

/**
 * This operator will transform a given Normalization Model
 * into a model that will effectively revert the normalization.
 * 
 * @author Sebastian Land
 */
public class DenormalizationOperator extends Operator {

    /**
     * This saves the coefficients of a linear transformation a*x + b of attributes.
     * 
     * @author Sebastian Land
     */
    public static class LinearTransformation implements Serializable {
        private static final long serialVersionUID = 1L;

        public LinearTransformation(double a, double b) {
            this.a = a;
            this.b = b;
        }
        public double a;
        public double b;
    }

    private InputPort modelInput = getInputPorts().createPort("model input", AbstractNormalizationModel.class);
    private OutputPort modelOutput = getOutputPorts().createPort("model output");
    private OutputPort originalModelOutput = getOutputPorts().createPort("original model output");

    public DenormalizationOperator(OperatorDescription description) {
        super(description);

        getTransformer().addPassThroughRule(modelInput, originalModelOutput);
        getTransformer().addGenerationRule(modelOutput, AbstractNormalizationModel.class);
    }

    @Override
    public void doWork() throws OperatorException {
        AbstractNormalizationModel model = modelInput.getData(AbstractNormalizationModel.class);

        Map<String, LinearTransformation> attributeTransformations = new HashMap<String, DenormalizationOperator.LinearTransformation>();
        for (Attribute attribute: model.getTrainingHeader().getAttributes()) {
            double b = model.computeValue(attribute, 0);
            double a = model.computeValue(attribute, 1) - b;

            attributeTransformations.put(attribute.getName(), new LinearTransformation(a, b));
        }

        modelOutput.deliver(new DenormalizationModel(model.getTrainingHeader(), attributeTransformations, model));
        originalModelOutput.deliver(model);
    }
}
