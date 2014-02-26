/*
 *  RapidMiner
 *
 *  Copyright (C) 2001-2014 by RapidMiner and the contributors
 *
 *  Complete list of developers available at our web site:
 *
 *       http://rapidminer.com
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
package com.rapidminer.operator.meta.branch;

import com.rapidminer.generator.GenerationException;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.tools.expression.parser.AbstractExpressionParser;
import com.rapidminer.tools.expression.parser.AbstractExpressionParser.ExpressionParserException;
import com.rapidminer.tools.expression.parser.ExpressionParserFactory;

/**
 * This condition will parse the condition value as an expression and return
 * it's boolean value if possible. Otherwise an error will be thrown.
 * 
 * @author Sebastian Land
 */
public class ExpressionCondition implements ProcessBranchCondition {

	private AbstractExpressionParser expressionParser = ExpressionParserFactory.getExpressionParser(true);
	
	@Override
	public boolean check(ProcessBranch operator, String value) throws OperatorException {
		//check for errors
		
		try {
			expressionParser.parseExpression(value);
		} catch (ExpressionParserException e) {
			throw new GenerationException(value + ": " + expressionParser.getErrorInfo());
		}

		// create the new attribute from the delivered type 
		Object result;
		try {
			result = expressionParser.getValueAsObject();
		} catch (ExpressionParserException e) {
			result = null;
		}
		
		if (result instanceof Double) {
			double resultValue = (Double) result;
			if (resultValue == 1d || resultValue == 0d){
				return resultValue == 1d;
			}
		} else if (result instanceof Boolean) {
			return ((Boolean)result).booleanValue(); 
		}
		throw new GenerationException("Must return boolean value");
	}
}
