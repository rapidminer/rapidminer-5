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
import com.rapidminer.MacroHandler;

/**
 * 
 * @author Thilo Kamradt
 *
 */
public class MacroValue extends PostfixMathCommand 
{
	private MacroHandler handler;

	public MacroValue(Process process) {
		numberOfParameters = -1;
		handler = process.getMacroHandler();
	}

	@Override
	public void run(Stack stack) throws ParseException {
		checkStack(stack);// check the stack

		// initialize the result for macro()
		if (curNumberOfParameters==2){
			workWithTwo(stack);
		}
		else if(curNumberOfParameters==1){
			workWithOne(stack);
		}
		else {
			throw new ParseException("Invalid number of arguments for 'macro', must be either 1 or 2.");
		}
		
	}
	
	@SuppressWarnings("unchecked")
	private void workWithTwo(Stack stack) throws ParseException {
		Object parameter2 = stack.pop();
		Object parameter= stack.pop();
	
		if (!(parameter instanceof String)) {
			throw new ParseException("Invalid parameter type, only strings are allowed for 'macroValue'.");
		}
		if (!(parameter2 instanceof String)) {
			throw new ParseException("Invalid parameter type, only strings are allowed for 'macroValue'. Caused by Argument 2");
		}
		// collect Macro from MacroHandler
		String macro=handler.getMacro((String)parameter);
		if(macro==null){
				stack.push((String)parameter2);
		} else{
			stack.push(macro);
		}
	}
	
	@SuppressWarnings("unchecked")
	private void workWithOne(Stack stack) throws ParseException {
				Object parameter= stack.pop();
			
				if (!(parameter instanceof String)) {
					throw new ParseException("Invalid parameter type, only strings are allowed for \"macroValue\".");
				}
				// collect Macro from MacroHandler
				String macro=handler.getMacro((String)parameter);
				if(macro==null)
				{
						
					throw new ParseException("Invalid parameter value, the macro '"+parameter+"' is not defined.");
					}
				else {
					stack.push(macro);
				}
	
	}

}
