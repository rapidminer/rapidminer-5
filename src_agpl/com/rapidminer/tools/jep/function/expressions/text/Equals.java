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
 * Compares the two given texts. The result is true if and only if the first argument represents 
 * the same sequence of characters as the second argument.
 * 
 * @author Ingo Mierswa
 */
public class Equals extends PostfixMathCommand {

	public Equals() {
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
		if (firstTextObject == UnknownValue.UNKNOWN_NOMINAL || secondTextObject == UnknownValue.UNKNOWN_NOMINAL) {
			stack.push(UnknownValue.UNKNOWN_BOOLEAN);
			return;
		}
		
		if (!(firstTextObject instanceof String) || !(secondTextObject instanceof String)) {
			throw new ParseException("Invalid argument types, must be (string, string)");
		}
		
		String firstText  = (String) firstTextObject;
		String secondText = (String) secondTextObject;
		
		stack.push(firstText.equals(secondText));
	}
}
