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
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.Stack;
import java.util.TimeZone;

import org.nfunk.jep.ParseException;
import org.nfunk.jep.function.PostfixMathCommand;

import com.rapidminer.tools.expression.parser.ExpressionParserConstants;
import com.rapidminer.tools.expression.parser.UnknownValue;

/**
 * Allows to add a custom amount of time to a given Calendar.
 * 
 * @author Marco Boeck
 */
public class DateAdd extends PostfixMathCommand {
	
	public DateAdd() {
		// variable number of parameters
		numberOfParameters = -1;
	}
	
	/**
	 * Creates the new Date.
	 */
	@SuppressWarnings("unchecked")
	@Override
	public void run(Stack stack) throws ParseException {
		checkStack(stack);
		
		Locale locale = Locale.getDefault();
		TimeZone zone = TimeZone.getDefault();
		
		if (curNumberOfParameters == 5) {
			Object timezoneObject = stack.pop();
			if (!(timezoneObject instanceof String)) {
				throw new ParseException("Invalid argument type for 'date_add', fifth argument must be (String) for TimeZone (e.g. America/Los_Angeles)");
			}
			zone = TimeZone.getTimeZone(String.valueOf(timezoneObject));
			
			Object localeObject = stack.pop();
			if (!(localeObject instanceof String)) {
				throw new ParseException("Invalid argument type for 'date_add', fourth argument must be (String) for locale (e.g. \"en\")");
			}
			locale = new Locale(String.valueOf(localeObject));
		} else if (curNumberOfParameters != 3) {
			throw new ParseException("Invalid number of arguments for 'date_add', must be either 3 or 5.");
		}
		Object unitConstantObject = stack.pop();
		if (!(unitConstantObject instanceof String)) {
			throw new ParseException("Invalid argument type for 'date_add', third argument must be unit constant (e.g. DATE_UNIT_HOUR)");
		}
		String unitConstant = String.valueOf(unitConstantObject);
		
		Object addedValueObject = stack.pop();
		if (!(addedValueObject instanceof Double)) {
			throw new ParseException("Invalid argument type for 'date_add', second argument must be a number");
		}
		double addedValue = (Double)addedValueObject;
		
		Object dateObject = stack.pop();
		// check for unknown values
		if (dateObject == UnknownValue.UNKNOWN_DATE) {
			stack.push(UnknownValue.UNKNOWN_NOMINAL);
			return;
		}
		if (!(dateObject instanceof Calendar)) {
			throw new ParseException("Invalid argument type for 'date_add', first argument must be Calendar");
		}
		Calendar dateCal = (Calendar)dateObject;
		Calendar cal = GregorianCalendar.getInstance(zone, locale);
		cal.setTime(dateCal.getTime());
		
		if (unitConstant.equals(ExpressionParserConstants.DATE_UNIT_YEAR)) {
			cal.add(Calendar.YEAR, (int)addedValue);
		} else if (unitConstant.equals(ExpressionParserConstants.DATE_UNIT_MONTH)) {
			cal.add(Calendar.MONTH, (int)addedValue);
		} else if (unitConstant.equals(ExpressionParserConstants.DATE_UNIT_WEEK)) {
			cal.add(Calendar.WEEK_OF_YEAR, (int)addedValue);
		} else if (unitConstant.equals(ExpressionParserConstants.DATE_UNIT_DAY)) {
			cal.add(Calendar.DAY_OF_MONTH, (int)addedValue);
		} else if (unitConstant.equals(ExpressionParserConstants.DATE_UNIT_HOUR)) {
			cal.add(Calendar.HOUR_OF_DAY, (int)addedValue);
		} else if (unitConstant.equals(ExpressionParserConstants.DATE_UNIT_MINUTE)) {
			cal.add(Calendar.MINUTE, (int)addedValue);
		} else if (unitConstant.equals(ExpressionParserConstants.DATE_UNIT_SECOND)) {
			cal.add(Calendar.SECOND, (int)addedValue);
		} else if (unitConstant.equals(ExpressionParserConstants.DATE_UNIT_MILLISECOND)) {
			cal.add(Calendar.MILLISECOND, (int)addedValue);
		} else {
			throw new ParseException("Invalid argument type for 'date_add', third argument must be unit constant (e.g. DATE_UNIT_HOUR)");
		}
		
		stack.push(cal);
	}
}
