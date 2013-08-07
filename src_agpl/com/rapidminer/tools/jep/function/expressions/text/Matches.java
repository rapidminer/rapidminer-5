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
 * Returns true if and only if the given text matches the given expression.
 * 
 * @author Ingo Mierswa
 */
public class Matches extends PostfixMathCommand {

	public Matches() {
		numberOfParameters = 2;
	}

	@SuppressWarnings("unchecked")
	@Override
	public void run(Stack stack) throws ParseException {
		checkStack(stack);// check the stack

		// initialize the result to the first argument
		Object expressionObject = stack.pop();
		Object textObject = stack.pop();

		// checking for unknown value
		if (expressionObject == UnknownValue.UNKNOWN_NOMINAL || textObject == UnknownValue.UNKNOWN_NOMINAL) {
			stack.push(UnknownValue.UNKNOWN_BOOLEAN);
			return;
		}
		if (!(textObject instanceof String) || !(expressionObject instanceof String)) {
			throw new ParseException("Invalid argument types, must be (string, string)");
		}

		String text = (String) textObject;
		String expression = (String) expressionObject;

		stack.push(text.matches(expression));
	}
}
