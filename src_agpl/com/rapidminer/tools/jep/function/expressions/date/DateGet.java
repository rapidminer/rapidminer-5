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
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.Stack;
import java.util.TimeZone;

import org.nfunk.jep.ParseException;
import org.nfunk.jep.function.PostfixMathCommand;

import com.rapidminer.tools.expression.parser.ExpressionParserConstants;
import com.rapidminer.tools.expression.parser.UnknownValue;

/**
 * Gets a specified portion of a given Calendar, e.g. the month.
 * 
 * @author Marco Boeck
 */
public class DateGet extends PostfixMathCommand {
	
	public DateGet() {
		numberOfParameters = -1;
	}
	
	/**
	 * Returns the specified portion as a double.
	 */
	@SuppressWarnings("unchecked")
	@Override
	public void run(Stack stack) throws ParseException {
		checkStack(stack);
		
		Locale locale = Locale.getDefault();
		TimeZone zone = TimeZone.getDefault();
		
		if (curNumberOfParameters == 4) {
			Object timezoneObject = stack.pop();
			if (!(timezoneObject instanceof String)) {
				throw new ParseException("Invalid argument type for 'date_get', fourth argument must be (String) for TimeZone (e.g. America/Los_Angeles)");
			}
			zone = TimeZone.getTimeZone(String.valueOf(timezoneObject));
			
			Object localeObject = stack.pop();
			if (!(localeObject instanceof String)) {
				throw new ParseException("Invalid argument type for 'date_get', third argument must be (String) for locale (e.g. \"en\")");
			}
			locale = new Locale(String.valueOf(localeObject));
		} else if (curNumberOfParameters != 2) {
			throw new ParseException("Invalid number of arguments for 'date_get', must be 2 or 4.");
		}
		Object unitConstantObject = stack.pop();
		if (!(unitConstantObject instanceof String)) {
			throw new ParseException("Invalid argument type for 'date_get', second argument must be unit constant (e.g. DATE_UNIT_HOUR)");
		}
		String unitConstant = String.valueOf(unitConstantObject);
		
		Object calObject = stack.pop();
		// check for unknown values
		if (calObject == UnknownValue.UNKNOWN_DATE) {
			stack.push(Double.NaN);
			return;
		}

		if (!(calObject instanceof Calendar)) {
			throw new ParseException("Invalid argument type for 'date_get', first argument must be Calendar");
		}
		Calendar calOld = (Calendar)calObject;
		Date date = calOld.getTime();
		Calendar cal = GregorianCalendar.getInstance(zone, locale);
		cal.setTime(date);
		double result;
		
		if (unitConstant.equals(ExpressionParserConstants.DATE_UNIT_YEAR)) {
			result = cal.get(Calendar.YEAR);
		} else if (unitConstant.equals(ExpressionParserConstants.DATE_UNIT_MONTH)) {
			result = cal.get(Calendar.MONTH);
		} else if (unitConstant.equals(ExpressionParserConstants.DATE_UNIT_WEEK)) {
			result = cal.get(Calendar.WEEK_OF_MONTH);
		} else if (unitConstant.equals(ExpressionParserConstants.DATE_UNIT_DAY)) {
			result = cal.get(Calendar.DAY_OF_MONTH);
		} else if (unitConstant.equals(ExpressionParserConstants.DATE_UNIT_HOUR)) {
			result = cal.get(Calendar.HOUR_OF_DAY);
		} else if (unitConstant.equals(ExpressionParserConstants.DATE_UNIT_MINUTE)) {
			result = cal.get(Calendar.MINUTE);
		} else if (unitConstant.equals(ExpressionParserConstants.DATE_UNIT_SECOND)) {
			result = cal.get(Calendar.SECOND);
		} else if (unitConstant.equals(ExpressionParserConstants.DATE_UNIT_MILLISECOND)) {
			result = cal.get(Calendar.MILLISECOND);
		} else {
			throw new ParseException("Invalid argument type for 'date_get', second argument must be unit constant (e.g. DATE_UNIT_HOUR)");
		}
		
		stack.push(result);
	}
}
