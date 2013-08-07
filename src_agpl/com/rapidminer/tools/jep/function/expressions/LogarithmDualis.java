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
import org.nfunk.jep.type.Complex;

/**
 * Calculates the logarithm to the base 2.
 * 
 * @author Ingo Mierswa
 */
public class LogarithmDualis extends PostfixMathCommand {

	private static final double LOG2 = Math.log(2);
	private static final Complex CLOG2 = new Complex(Math.log(2), 0);

	public LogarithmDualis() {
		numberOfParameters = 1;
	}

	@Override
	@SuppressWarnings("unchecked")
	public void run(Stack inStack) throws ParseException {
		checkStack(inStack);// check the stack
		Object param = inStack.pop();
		inStack.push(log(param));//push the result on the inStack
		return;
	}

	public Object log(Object param) throws ParseException {
		if (param instanceof Complex) {
			return ((Complex) param).log().div(CLOG2);
		} else if (param instanceof Number) {
			double num = ((Number) param).doubleValue();
			if (num >= 0d)
				return new Double(Math.log(num) / LOG2);
			else if (num == 0d)
				return new Double(Double.NaN);
			else {
				Complex temp = new Complex(num);
				return temp.log().div(CLOG2);
			}
		} else {
			throw new ParseException("Invalid parameter type");
		}
	}
}
