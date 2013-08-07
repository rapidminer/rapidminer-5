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

import com.rapidminer.Process;
import com.rapidminer.tools.RandomGenerator;

/**
 * Returns a random number between 0 and 1.
 * 
 * @author Dominik Halfkann
 */
public class Random extends PostfixMathCommand {

	private Process process;
	
	public Random(Process process) {
		// Use a variable number of arguments
		numberOfParameters = 0;
		this.process = process;
	}

	/**
	 * Calculates the result of summing up all parameters, which are assumed to
	 * be of the Double type.
	 */
	@Override
	@SuppressWarnings("unchecked")
	public void run(Stack stack) throws ParseException {
		checkStack(stack);// check the stack
		
		RandomGenerator randomGenerator = RandomGenerator.getRandomGenerator(process, -1);
		double randomValue = randomGenerator.nextDouble();
		
		// push the result on the inStack
		stack.push(randomValue);
	}
}
