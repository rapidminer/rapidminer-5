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
package com.rapidminer.operator.meta.branch;

import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.rapidminer.operator.IOObject;
import com.rapidminer.operator.OperatorChain;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.UserError;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.InputPorts;
import com.rapidminer.operator.ports.MultiInputPortPairExtender;
import com.rapidminer.operator.ports.MultiOutputPortPairExtender;
import com.rapidminer.operator.ports.OutputPort;
import com.rapidminer.operator.ports.OutputPorts;
import com.rapidminer.operator.ports.metadata.SubprocessTransformRule;
import com.rapidminer.operator.preprocessing.filter.ExampleFilter;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeBoolean;
import com.rapidminer.parameter.ParameterTypeCategory;
import com.rapidminer.parameter.ParameterTypeString;
import com.rapidminer.parameter.ParameterTypeStringCategory;
import com.rapidminer.parameter.UndefinedParameterError;
import com.rapidminer.parameter.conditions.EqualStringCondition;
import com.rapidminer.parameter.conditions.NonEqualStringCondition;
import com.rapidminer.tools.OperatorService;
import com.rapidminer.tools.Tools;

/**
 * <p>This operator provides a conditional execution of parts of processes.
 * It has to have two OperatorChains as children. The first chain is processed
 * if the specified condition is true, the second one is processed if it is false
 * (if-then-else). The second chain may be omitted (if-then). In this case, this
 * operator has only one inner operator.</p>
 * 
 * <p>
 * If the condition &quot;attribute_value_filter&quot; is used, the same attribute
 * value conditions already known from the {@link ExampleFilter} operator can be used.
 * In addition to the known attribute value relation format (e.g. &quot;att1&gt;=0.7&quot;),
 * this operator expects an additional definition for the used example which cam be added in 
 * &quot;[&quot; and &quot;]&quot; after the attribute value condition. The following values
 * are possible:
 * <ul>
 * <li>a fixed number, e.g. &quot;att1&gt;0.7 [7]&quot; meaning that the value for attribute 
 *     &quot;att1&quot; for the example 7 must be greater than 0.7</li>
 * <li>the wildcard &quot;*&quot; meaning that the attribute value condition must be
 *     fulfilled for all examples, e.g. &quot;att4&lt;=5 [*]&quot;</li>
 * <li>no example definition, meaning the same as the wildcard definition [*]</li>
 * </ul>
 * </p>
 * 
 * @author Sebastian Land, Ingo Mierswa
 */
public class ProcessBranch extends OperatorChain {

	public static final String PARAMETER_CONDITION_TYPE = "condition_type";

	public static final String PARAMETER_CONDITION_VALUE = "condition_value";

	public static final String PARAMETER_RETURN_INNER_OUTPUT = "return_inner_output";

	public static final String PARAMETER_IO_OBJECT = "io_object";

	// CHECK INDEX OF INPUT_EXISTS BELOW
	public static final String[] CONDITION_NAMES = {
		"attribute_value_filter",
		"attribute_available",
		"min_examples",
		"max_examples",
		"min_attributes",
		"max_attributes",
		"min_fitness",
		"max_fitness",
		"min_performance_value",
		"max_performance_value",
		"file_exists",
		"input_exists",   // <== THIS NEEDS TO BE THE 11'th ENTRY IN THE LIST!!!
		"macro_defined",
		"expression"
	};
	// ONLY TRUE IF INPUT EXISTS IS 11'th ENTRY ABOVE
	public static final String CONDITION_INPUT_EXISTS = CONDITION_NAMES[11];

	public static final Class[]  CONDITION_CLASSES = {
		DataValueCondition.class,
		AttributeAvailableCondition.class,
		MinNumberOfExamplesCondition.class,
		MaxNumberOfExamplesCondition.class,
		MinNumberOfAttributesCondition.class,
		MaxNumberOfAttributesCondition.class,
		MinFitnessCondition.class,
		MaxFitnessCondition.class,
		MinPerformanceValueCondition.class,
		MaxPerformanceValueCondition.class,
		FileExistsCondition.class,
		InputExistsCondition.class,
		MacroDefinedCondition.class,
		ExpressionCondition.class
	};

	


	private String[] objectArray = null;

	private final InputPort conditionInput = getInputPorts().createPort("condition");
	private final OutputPort conditionInnerSourceThen = getSubprocess(0).getInnerSources().createPort("condition");
	private final OutputPort conditionInnerSourceElse = getSubprocess(1).getInnerSources().createPort("condition");
	private final MultiOutputPortPairExtender inputExtender = new MultiOutputPortPairExtender("input", 
			getInputPorts(), 
			new OutputPorts[] { getSubprocess(0).getInnerSources(), getSubprocess(1).getInnerSources() });
	private final MultiInputPortPairExtender outputExtender = new MultiInputPortPairExtender("input",
			getOutputPorts(),
			new InputPorts[] { getSubprocess(0).getInnerSinks(), getSubprocess(1).getInnerSinks() });


	public ProcessBranch(OperatorDescription description) {
		super(description, "Then", "Else");
		inputExtender.start();
		getTransformer().addPassThroughRule(conditionInput, conditionInnerSourceThen);
		getTransformer().addPassThroughRule(conditionInput, conditionInnerSourceElse);	
		getTransformer().addRule(inputExtender.makePassThroughRule());
		getTransformer().addRule(new SubprocessTransformRule(getSubprocess(0)));
		getTransformer().addRule(new SubprocessTransformRule(getSubprocess(1)));
		getTransformer().addRule(outputExtender.makePassThroughRule());
		outputExtender.start();
	}

	@Override
	public void doWork() throws OperatorException {		
		
		// creating condition
		Class conditionClass = null;
		String selectedConditionName = "";
		
		selectedConditionName = getParameterAsString(PARAMETER_CONDITION_TYPE);
		for (int i = 0; i < CONDITION_NAMES.length; i++) {
			if (selectedConditionName.toLowerCase().equals(CONDITION_NAMES[i].toLowerCase())) {
				conditionClass = CONDITION_CLASSES[i];
				break;
			}
		}

		if (conditionClass == null) {
			try {
				conditionClass = Tools.classForName(selectedConditionName);
			} catch (ClassNotFoundException e) {
				throw new UserError(this, e, 904, new Object[] { selectedConditionName, e });
			}
		}
		ProcessBranchCondition condition = null;
		try {
			condition = (ProcessBranchCondition) conditionClass.newInstance();
		} catch (InstantiationException e) {
			throw new UserError(this, e, 904, new Object[] { selectedConditionName, e });
		} catch (IllegalAccessException e) {
			throw new UserError(this, e, 904, new Object[] { selectedConditionName, e });
		}

		clearAllInnerSinks();
		if (condition != null) {
			// checking condition
			String conditionValue;
			if (CONDITION_INPUT_EXISTS.equals(selectedConditionName)) {
				Class<? extends IOObject> selectedConditionClass = getSelectedClass();
				if (selectedConditionClass == null) {
					throw new UserError(this, 904, "'" + getParameter(PARAMETER_IO_OBJECT) + "'", "Class does not exist.");
				}
				conditionValue = null;
			} else {
				conditionValue = getParameterAsString(PARAMETER_CONDITION_VALUE);
			}
			boolean conditionState = condition.check(this, conditionValue);

			// execute
			inputExtender.passDataThrough();
			if (conditionState) {
				conditionInnerSourceThen.deliver(conditionInput.getDataOrNull(IOObject.class));
			} else {
				conditionInnerSourceElse.deliver(conditionInput.getDataOrNull(IOObject.class));
			}
			int chosenProcess = conditionState ? 0 : 1;
			getSubprocess(chosenProcess).execute();
			outputExtender.passDataThrough(chosenProcess);				
		} else {
			outputExtender.passDataThrough(0);
		}
	}

	@Override
	public boolean getAddOnlyAdditionalOutput() {
		return getParameterAsBoolean(PARAMETER_RETURN_INNER_OUTPUT);
	}

	public <T extends IOObject> T getConditionInput(Class<T> cls) throws UserError { 
		return conditionInput.<T>getData(cls);
	}

	public <T extends IOObject> T getConditionInputOrNull(Class<T> cls) throws UserError { 
		return conditionInput.<T>getDataOrNull(cls);
	}

	public Class<? extends IOObject> getSelectedClass() throws UndefinedParameterError {
		int ioType = getParameterAsInt(PARAMETER_IO_OBJECT);
		if (objectArray == null) {
			return null;
		} else {
			if ((ioType >= 0) && (ioType < objectArray.length)) {
				return OperatorService.getIOObjectClass(objectArray[ioType]);			
			} else {
				return null;
			}
		}
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();
		ParameterType type = new ParameterTypeStringCategory(PARAMETER_CONDITION_TYPE, "The condition which is used for the condition check.", CONDITION_NAMES);
		type.setExpert(false);
		types.add(type);

		type = new ParameterTypeString(PARAMETER_CONDITION_VALUE, "A condition parameter which might be desired for some condition checks.", true);
		type.setExpert(false);
		type.registerDependencyCondition(new NonEqualStringCondition(this, PARAMETER_CONDITION_TYPE, true, CONDITION_INPUT_EXISTS));
		types.add(type);

		Set<String> ioObjects = OperatorService.getIOObjectsNames();
		this.objectArray = new String[ioObjects.size()];
		Iterator<String> i = ioObjects.iterator();
		int index = 0;
		while (i.hasNext()) {
			objectArray[index++] = i.next();
		}

		type = new ParameterTypeCategory(PARAMETER_IO_OBJECT, "The class of the object(s) which should be checked for existance.", objectArray, 0);
		type.registerDependencyCondition(new EqualStringCondition(this, PARAMETER_CONDITION_TYPE, true, CONDITION_INPUT_EXISTS));
		type.setExpert(false);
		types.add(type);

		types.add(new ParameterTypeBoolean(PARAMETER_RETURN_INNER_OUTPUT, "Indicates if the output of the inner operators should be delivered.", true));
		return types;
	}
}
