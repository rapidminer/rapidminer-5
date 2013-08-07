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
import org.nfunk.jep.function.Add;
import org.nfunk.jep.function.Divide;
import org.nfunk.jep.function.PostfixMathCommand;

/**
 * Calculates the average of an arbitrary number of arguments.
 * 
 * @author Ingo Mierswa
 */
public class Average extends PostfixMathCommand {

	private Add addFun = new Add();
	private Divide divideFun = new Divide();

	public Average() {
		// Use a variable number of arguments
		numberOfParameters = -1;
	}

	/**
	 * Calculates the result of summing up all parameters, which are assumed to
	 * be of the Double type.
	 */
	@Override
	@SuppressWarnings("unchecked")
	public void run(Stack stack) throws ParseException {
		checkStack(stack);// check the stack

		// initialize the result to the first argument
		Object sum = stack.pop();
		if (!(sum instanceof Double)) {
			throw new ParseException(
					"Invalid parameter type, only numbers are allowed for 'min'.");
		}

		int i = 1;

		// repeat summation for each one of the current parameters
		while (i < curNumberOfParameters) {
			// get the parameter from the stack
			Object param = stack.pop();

			if (param instanceof Double) {
				// add it to the sum (order is important for String arguments)
				sum = addFun.add(param, sum);
			} else {
				throw new ParseException(
						"Invalid parameter type, only numbers are allowed for 'avg'.");
			}

			i++;
		}

		// calc average
		sum = divideFun.div(sum, curNumberOfParameters);

		// push the result on the inStack
		stack.push(sum);
	}
}
