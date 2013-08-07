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
 * Calculates the signum of the given argument.
 * 
 * @author Ingo Mierswa
 */
public class Signum extends PostfixMathCommand {

	public Signum() {
		numberOfParameters = 1;
	}

	@Override
	@SuppressWarnings("unchecked")
	public void run(Stack inStack) throws ParseException {
		checkStack(inStack);// check the stack
		Object param = inStack.pop();
		inStack.push(sgn(param));//push the result on the inStack
		return;
	}

	public Object sgn(Object param) throws ParseException {
		double num = 0.0d;
		if (param instanceof Complex) {
			num = ((Complex) param).doubleValue(); 
		} else if (param instanceof Number) {
			num = ((Number) param).doubleValue();
		} else {
			throw new ParseException("Invalid parameter type");
		}
		if (num >= 0) {
			return Double.valueOf(1);
		} else {
			return Double.valueOf(-1);
		}
	}
}
