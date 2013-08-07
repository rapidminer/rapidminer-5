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
package com.rapidminer.operator.preprocessing.filter;

import java.util.List;

import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.set.Condition;
import com.rapidminer.example.set.ConditionCreationException;
import com.rapidminer.example.set.ConditionedExampleSet;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.UserError;
import com.rapidminer.operator.annotation.ResourceConsumptionEstimator;
import com.rapidminer.operator.ports.metadata.AttributeMetaData;
import com.rapidminer.operator.ports.metadata.ExampleSetMetaData;
import com.rapidminer.operator.ports.metadata.MDInteger;
import com.rapidminer.operator.preprocessing.AbstractDataProcessing;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeBoolean;
import com.rapidminer.parameter.ParameterTypeString;
import com.rapidminer.parameter.ParameterTypeStringCategory;
import com.rapidminer.parameter.UndefinedParameterError;
import com.rapidminer.parameter.conditions.EqualStringCondition;
import com.rapidminer.tools.OperatorResourceConsumptionHandler;


/**
 * <p>This operator takes an {@link ExampleSet} as input and returns a new
 * {@link ExampleSet} including only the {@link Example}s that fulfill a
 * condition.</p>
 * 
 * <p>By specifying an implementation of
 * {@link com.rapidminer.example.set.Condition} and a parameter string, arbitrary
 * filters can be applied. Users can implement their own conditions by writing a
 * subclass of the above class and implementing a two argument constructor
 * taking an {@link ExampleSet} and a parameter string. This parameter string is
 * specified by the parameter <code>parameter_string</code>. Instead of using
 * one of the predefined conditions users can define their own implementation
 * with the fully qualified class name.</p>
 * 
 * <p>For &quot;attribute_value_condition&quot; the parameter string must have the form
 * <code>attribute op value</code>, where attribute is a name of an
 * attribute, value is a value the attribute can take and op is one of the
 * binary logical operators similar to the ones known from Java, e.g. greater
 * than or equals. Please note your can define a logical OR of several conditions
 * with || and a logical AND of two conditions with two ampers and - or 
 * simply by applying several ExampleFilter operators in a row. Please note also 
 * that for nominal attributes you can define a regular expression for value of the 
 * possible equal and not equal checks.</p>
 * 
 * <p>For &quot;unknown_attributes&quot; the parameter string
 * must be empty. This filter removes all examples containing attributes that
 * have missing or illegal values. For &quot;unknown_label&quot; the parameter
 * string must also be empty. This filter removes all examples with an unknown
 * label value.</p>
 * 
 * @author Ingo Mierswa, Simon Fischer
 */
public class ExampleFilter extends AbstractDataProcessing {

	/** The parameter name for &quot;Implementation of the condition.&quot; */
	public static final String PARAMETER_CONDITION_CLASS = "condition_class";

	/** The parameter name for &quot;Parameter string for the condition, e.g. 'attribute=value' for the AttributeValueFilter.&quot; */
	public static final String PARAMETER_PARAMETER_STRING = "parameter_string";

	/** The parameter name for &quot;Indicates if only examples should be accepted which would normally filtered.&quot; */
	public static final String PARAMETER_INVERT_FILTER = "invert_filter";
	

	public ExampleFilter(OperatorDescription description) {
		super(description);
	}

	@Override
	public ExampleSetMetaData modifyMetaData(ExampleSetMetaData emd) {
		emd.getNumberOfExamples().reduceByUnknownAmount();
		try {
			if (getParameterAsString(PARAMETER_CONDITION_CLASS).equals(
					ConditionedExampleSet.KNOWN_CONDITION_NAMES[ConditionedExampleSet.CONDITION_NO_MISSING_ATTRIBUTES])) {
				for (AttributeMetaData amd : emd.getAllAttributes()) {
					amd.setNumberOfMissingValues(new MDInteger(0));
				}
			}
		} catch (UndefinedParameterError e) {
		}
		return emd;
	}
	
	@Override
	public ExampleSet apply(ExampleSet inputSet) throws OperatorException {
		getLogger().fine(getName() + ": input set has " + inputSet.size() + " examples.");

		String className = getParameterAsString(PARAMETER_CONDITION_CLASS);
		String parameter = getParameterAsString(PARAMETER_PARAMETER_STRING);
		getLogger().fine("Creating condition '" + className + "' with parameter '" + parameter + "'");
		Condition condition = null;
		try {
			condition = ConditionedExampleSet.createCondition(className, inputSet, parameter);
		} catch (ConditionCreationException e) {
			throw new UserError(this, e, 904, className, e.getMessage());
		}
		ExampleSet result = new ConditionedExampleSet(inputSet, condition, getParameterAsBoolean(PARAMETER_INVERT_FILTER));
		return result;
	}
	
	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();
		
		ParameterType type  = new ParameterTypeStringCategory(PARAMETER_CONDITION_CLASS, "Implementation of the condition.", ConditionedExampleSet.KNOWN_CONDITION_NAMES, ConditionedExampleSet.KNOWN_CONDITION_NAMES[ConditionedExampleSet.CONDITION_ALL], false);
		type.setExpert(false);
		types.add(type);
		
		type = new ParameterTypeString(PARAMETER_PARAMETER_STRING, "Parameter string for the condition, e.g. 'attribute=value' for the AttributeValueFilter.", true);
		type.registerDependencyCondition(new EqualStringCondition(this, PARAMETER_CONDITION_CLASS, true, ConditionedExampleSet.KNOWN_CONDITION_NAMES[7]));
		type.setExpert(false);
		types.add(type);
		
        type = new ParameterTypeBoolean(PARAMETER_INVERT_FILTER, "Indicates if only examples should be accepted which would normally filtered.", false);
        type.setExpert(false);
        types.add(type);
        
		return types;
	}
	
	@Override
	public boolean writesIntoExistingData() {
		return false;
	}
	
	@Override
	public ResourceConsumptionEstimator getResourceConsumptionEstimator() {
		return OperatorResourceConsumptionHandler.getResourceConsumptionEstimator(getInputPort(), ExampleFilter.class, null);
	}
}
