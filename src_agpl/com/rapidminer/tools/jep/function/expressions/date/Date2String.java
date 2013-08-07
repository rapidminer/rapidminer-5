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
package com.rapidminer.tools.jep.function.expressions.date;

import java.text.DateFormat;
import java.util.Calendar;
import java.util.Stack;

import org.nfunk.jep.ParseException;
import org.nfunk.jep.function.PostfixMathCommand;

import com.rapidminer.tools.expression.parser.ExpressionParserConstants;
import com.rapidminer.tools.expression.parser.UnknownValue;

/**
 * Changes a Calendar object to a String.
 * 
 * @author Marco Boeck
 */
public class Date2String extends PostfixMathCommand {
	
	public Date2String() {
		numberOfParameters = 3;
	}
	
	/**
	 * Creates the string result.
	 */
	@SuppressWarnings("unchecked")
	@Override
	public void run(Stack stack) throws ParseException {
		checkStack(stack);
		
		Object constantShowObject = stack.pop();
		if (!(constantShowObject instanceof String)) {
			throw new ParseException("Invalid argument type for 'date_str', third argument must be show constant (e.g. DATE_SHOW_DATE_ONLY)");
		}
		String constantShow = (String)constantShowObject;
		
		Object constantFormatObject = stack.pop();
		if (!(constantFormatObject instanceof String)) {
			throw new ParseException("Invalid argument type for 'date_str', second argument must be formatting constant (e.g. DATE_FULL)");
		}
		String constantFormat = (String)constantFormatObject;
		
		Object dateObject = stack.pop();
		// checking for unknown value
		if (dateObject ==  UnknownValue.UNKNOWN_DATE) {
			stack.push(UnknownValue.UNKNOWN_NOMINAL);
			return;
		}
		
		if (!(dateObject instanceof Calendar)) {
			throw new ParseException("Invalid argument type for 'date_str', first argument must be Calendar");
		}
		Calendar dateCal = (Calendar)dateObject;
		
		String result;
		DateFormat dateFormat;
		int formatting;
		if (constantFormat.equals(ExpressionParserConstants.DATE_FORMAT_FULL)) {
			formatting = DateFormat.FULL;
		} else if (constantFormat.equals(ExpressionParserConstants.DATE_FORMAT_LONG)) {
			formatting = DateFormat.LONG;
		} else if (constantFormat.equals(ExpressionParserConstants.DATE_FORMAT_MEDIUM)) {
			formatting = DateFormat.MEDIUM;
		} else if (constantFormat.equals(ExpressionParserConstants.DATE_FORMAT_SHORT)) {
			formatting = DateFormat.SHORT;
		} else {
			throw new ParseException("Invalid format constant for 'date_to_string'");
		}
		if (constantShow.equals(ExpressionParserConstants.DATE_SHOW_DATE_ONLY)) {
			dateFormat = DateFormat.getDateInstance(formatting);
		} else if (constantShow.equals(ExpressionParserConstants.DATE_SHOW_TIME_ONLY)) {
			dateFormat = DateFormat.getTimeInstance(formatting);
		} else if (constantShow.equals(ExpressionParserConstants.DATE_SHOW_DATE_AND_TIME)) {
			dateFormat = DateFormat.getDateTimeInstance(formatting, formatting);
		} else {
			throw new ParseException("Invalid show constant for 'date_str'");
		}
		result = dateFormat.format(dateCal.getTime());
		stack.push(result);
	}
}
