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

import java.util.Calendar;
import java.util.Date;
import java.util.Stack;

import org.nfunk.jep.ParseException;
import org.nfunk.jep.function.PostfixMathCommand;

import com.rapidminer.tools.expression.parser.UnknownValue;

/**
 * Determines if the first Calendar is strictly earlier than the second Calendar.
 * 
 * @author Marco Boeck
 */
public class DateBefore extends PostfixMathCommand {
	
	public DateBefore() {
		numberOfParameters = 2;
	}
	
	/**
	 * Creates the boolean result.
	 * True if the first date is strictly earlier than the second date; false otherwise (includes same date).
	 */
	@SuppressWarnings("unchecked")
	@Override
	public void run(Stack stack) throws ParseException {
		checkStack(stack);
		
		Object calObjectTwo = stack.pop();
		Object calObjectOne = stack.pop();
		// check for unknown values
		if (calObjectTwo == UnknownValue.UNKNOWN_DATE || calObjectOne == UnknownValue.UNKNOWN_DATE) {
			stack.push(UnknownValue.UNKNOWN_BOOLEAN);
			return;
		}
		if (!(calObjectOne instanceof Calendar) || !(calObjectTwo instanceof Calendar)) {
			throw new ParseException("Invalid argument type for 'date_before', must both be Calendar");
		}
		Calendar calOne = (Calendar)calObjectOne;
		Calendar calTwo = (Calendar)calObjectTwo;
		Date dateOne = calOne.getTime();
		Date dateTwo = calTwo.getTime();
		boolean result = dateOne.before(dateTwo);
		stack.push(result);
	}
}
