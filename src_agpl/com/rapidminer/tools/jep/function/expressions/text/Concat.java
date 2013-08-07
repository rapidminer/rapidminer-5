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

import java.util.ArrayList;
import java.util.Stack;

import org.nfunk.jep.ParseException;
import org.nfunk.jep.function.PostfixMathCommand;

import com.rapidminer.tools.expression.parser.UnknownValue;

/**
 * This command realizes String concatenation.
 * 
 * @author Sebastian Land
 */
public class Concat extends PostfixMathCommand {
	public Concat() {
		numberOfParameters = -1;
	}

	@SuppressWarnings("unchecked")
	@Override
	public void run(Stack stack) throws ParseException {
		checkStack(stack);// check the stack

		// initialize the result to the first argument
		ArrayList<String> strings = new ArrayList<String>();
		for (int i = 0; i < curNumberOfParameters; i++) {
			Object string = stack.pop();
			if (string == UnknownValue.UNKNOWN_NOMINAL) {
				// do nothing
			} else {
				if (!(string instanceof String)) {
					throw new ParseException("Invalid argument type, only strings are allowed for 'concat'.");
				}
				strings.add((String) string);
			}
		}

		StringBuilder builder = new StringBuilder();
		for (int i = strings.size() - 1; i >= 0; i--) {
			builder.append(strings.get(i));
		}
		stack.push(builder.toString());
	}
}
