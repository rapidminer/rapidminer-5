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
package com.rapidminer.tools.jep.function.expressions;

import java.util.Stack;

import org.nfunk.jep.ParseException;
import org.nfunk.jep.function.PostfixMathCommand;

import com.rapidminer.Process;
import com.rapidminer.parameter.UndefinedParameterError;

/**
 * Retrieves the value of an operator's parameter and pushes it onto the result stack.
 * 
 * @author Sebastian Land
 */
public class ParameterValue extends PostfixMathCommand {

	private Process process;

	public ParameterValue(Process process) {
		numberOfParameters = 2;
		this.process = process;
	}

	@Override
	@SuppressWarnings("unchecked")
	public void run(Stack stack) throws ParseException {
		checkStack(stack);// check the stack

		// initialize the result to the first argument
		Object parameter = stack.pop();
		if (!(parameter instanceof String)) {
			throw new ParseException("Invalid parameter type, only strings are allowed for 'parameterValue'.");
		}
		Object operator = stack.pop();
		if (!(operator instanceof String)) {
			throw new ParseException("Invalid parameter type, only strings are allowed for 'parameterValue'.");
		}

		String operatorName = (String) operator;
		String parameterName = (String) parameter;
		
		try {
			stack.push(process.getOperator(operatorName).getParameter(parameterName));
		} catch (UndefinedParameterError e) {
			throw new ParseException("Unknown parameter as argument for 'parameterValue'.");
		} catch (NullPointerException e) {
			throw new ParseException("Unknown operator as argument for 'parameterValue'.");
		}
		
	}

}
