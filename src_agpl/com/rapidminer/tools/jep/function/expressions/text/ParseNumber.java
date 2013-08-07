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
package com.rapidminer.tools.jep.function.expressions.text;

import java.util.Stack;

import org.nfunk.jep.ParseException;
import org.nfunk.jep.function.PostfixMathCommand;

import com.rapidminer.tools.expression.parser.UnknownValue;

/**
 * Parses the given string and puts the number on the result stack.
 * 
 * @author Sebastian Land
 */
public class ParseNumber extends PostfixMathCommand {

	public ParseNumber() {
		numberOfParameters = 1;
	}

	@Override
	@SuppressWarnings("unchecked")
	public void run(Stack stack) throws ParseException {
		checkStack(stack);// check the stack

		// initialize the result to the first argument
		Object value = stack.pop();
		// checking if value is unknown
		if (value  == UnknownValue.UNKNOWN_NOMINAL) {
			stack.push(Double.NaN);
			return;
		}
		// if known, parse it
		if (!(value instanceof String)) {
			throw new ParseException("Invalid argument type, only strings are supported.");
		}
		try {
			stack.push(Double.parseDouble((String) value));
		} catch (NumberFormatException e) {
			throw new ParseException("String '" + value + "' is not a number.");
		}
	}
}
