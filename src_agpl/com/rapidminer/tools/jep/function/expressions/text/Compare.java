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
 * Compares the two given texts lexicographically and returns a number smaller than 0 if the first text is smaller, 
 * a number larger than 0 if the first text is larger, or 0 otherwise.
 * 
 * @author Ingo Mierswa
 */
public class Compare extends PostfixMathCommand {

	public Compare() {
		numberOfParameters = 2;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public void run(Stack stack) throws ParseException {
		checkStack(stack);// check the stack

		// initialize the result to the first argument
		Object secondTextObject = stack.pop();
		Object firstTextObject = stack.pop();
		// checking for unknown value
		if (secondTextObject == UnknownValue.UNKNOWN_NOMINAL || firstTextObject == UnknownValue.UNKNOWN_NOMINAL) {
			stack.push(Double.NaN);
			return;
		}
		
		if (!(firstTextObject instanceof String) || !(secondTextObject instanceof String)) {
			throw new ParseException("Invalid argument types, must be (string, string)");
		}
		
		String firstText  = (String) firstTextObject;
		String secondText = (String) secondTextObject;
		
		stack.push(firstText.compareTo(secondText));
	}
}
