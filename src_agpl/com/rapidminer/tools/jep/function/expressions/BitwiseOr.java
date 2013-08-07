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

/**
 * Calculates the logical OR of two arguments which must be of integer type.
 * 
 * @author Ingo Mierswa
 */
public class BitwiseOr extends PostfixMathCommand {

	public BitwiseOr() {
		// Use a variable number of arguments
		numberOfParameters = 2;
	}

	/**
	 * Calculates the result of the function, which are assumed to
	 * be of the Integer type.
	 */
	@Override
	@SuppressWarnings("unchecked")
	public void run(Stack stack) throws ParseException {
		checkStack(stack);// check the stack

		Object firstO = stack.pop();
		if ((firstO == null) || (!(firstO instanceof Number))) {
			throw new ParseException(
					"Invalid parameter type, only integer numbers are allowed for 'bit_or'.");
		}
		double first = ((Number) firstO).doubleValue();
		if ((int)first != first) {
			throw new ParseException(
			"Invalid parameter type, only integer numbers are allowed for 'bit_or'.");
		}
		
		Object secondO = stack.pop();
		if ((secondO == null) || (!(secondO instanceof Number))) {
			throw new ParseException(
					"Invalid parameter type, only integer numbers are allowed for 'bit_or'.");
		}
		double second = ((Number) secondO).doubleValue();
		if ((int)second != second) {
			throw new ParseException(
			"Invalid parameter type, only integer numbers are allowed for 'bit_or'.");
		}

		// push the result on the inStack
		stack.push((int)first | (int)second);
	}
}
