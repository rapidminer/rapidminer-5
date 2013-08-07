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
 * Calculates the suffix of the given string and pushes it on the result stack. If the
 * given string is too short, the complete string will be returned.
 * 
 * @author Ingo Mierswa
 */
public class Suffix extends PostfixMathCommand {

	public Suffix() {
		numberOfParameters = 2;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public void run(Stack stack) throws ParseException {
		checkStack(stack);// check the stack

		// initialize the result to the first argument
		Object lengthObject = stack.pop();
		Object textObject = stack.pop();
		
		// checking for unknown value
		if (textObject  == UnknownValue.UNKNOWN_NOMINAL) {
			stack.push(UnknownValue.UNKNOWN_NOMINAL);
			return;
		}
		
		if (!(textObject instanceof String) || !(lengthObject instanceof Number)) {
			throw new ParseException("Invalid argument type, must be (string, number)");
		}
		int length = ((Number) lengthObject).intValue();
		String text = (String) textObject;
		
		int beginIndex = Math.max(0, text.length() - length);
		try {
			stack.push(text.substring(beginIndex));
		} catch (IndexOutOfBoundsException e) {
			stack.push(text);
		}
	}
}
