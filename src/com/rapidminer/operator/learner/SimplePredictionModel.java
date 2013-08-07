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
package com.rapidminer.operator.learner;

import java.util.Iterator;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.operator.OperatorException;


/**
 * A model that can be applied to an example set by applying it to each example
 * separately. Just as for the usual prediction model, subclasses must provide 
 * a constructor getting a label attribute which will be used to invoke the 
 * super one-argument constructor.
 * 
 * @author Ingo Mierswa, Simon Fischer
 *          ingomierswa Exp $
 */
public abstract class SimplePredictionModel extends PredictionModel {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 6275902545494306001L;

	protected SimplePredictionModel(ExampleSet exampleSet) {
		super(exampleSet);
	}
	
	/**
	 * Applies the model to a single example and returns the predicted class
	 * value.
	 */
	public abstract double predict(Example example) throws OperatorException;

	/** Iterates over all examples and applies the model to them. */
	@Override
	public ExampleSet performPrediction(ExampleSet exampleSet, Attribute predictedLabel) throws OperatorException {
		Iterator<Example> r = exampleSet.iterator();
		while (r.hasNext()) {
			Example example = r.next();
			example.setValue(predictedLabel, predict(example));
		}
		return exampleSet;
	}
}
