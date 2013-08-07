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
 * Allows to set a custom value for a portion of a given Calendar, e.g. set the month to 4 or the day to 23.
 * 
 * @author Marco Boeck
 */
public class DateSet extends PostfixMathCommand {
	
	public DateSet() {
		// variable numer of parameters
		numberOfParameters = -1;
	}
	
	/**
	 * Creates the new Calendar.
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
				throw new ParseException("Invalid argument type for 'date_set', fifth argument must be (String) for TimeZone (e.g. America/Los_Angeles)");
			}
			zone = TimeZone.getTimeZone(String.valueOf(timezoneObject));
			
			Object localeObject = stack.pop();
			if (!(localeObject instanceof String)) {
				throw new ParseException("Invalid argument type for 'date_set', fourth argument must be (String) for locale (e.g. \"en\")");
			}
			locale = new Locale(String.valueOf(localeObject));
		} else if (curNumberOfParameters != 3) {
			throw new ParseException("Invalid number of arguments for 'date_set', must be either 3 or 5.");
		}
		Object unitConstantObject = stack.pop();
		if (!(unitConstantObject instanceof String)) {
			throw new ParseException("Invalid argument type for 'date_set', third argument must be unit constant (e.g. DATE_UNIT_HOUR)");
		}
		String unitConstant = String.valueOf(unitConstantObject);
		
		Object setValueObject = stack.pop();
		// check for unknown values
		if (setValueObject instanceof Double && ((Double) setValueObject).isNaN()) {
			stack.push(UnknownValue.UNKNOWN_DATE);
			return;
		}
		if (!(setValueObject instanceof Double)) {
			throw new ParseException("Invalid argument type for 'date_set', second argument must be a number");
		}
		double setValue = (Double)setValueObject;
		
		Object calObject = stack.pop();
		// check for unknown values
		if (calObject == UnknownValue.UNKNOWN_DATE) {
			stack.push(UnknownValue.UNKNOWN_DATE);
			return;
		}
		
		if (!(calObject instanceof Calendar)) {
			throw new ParseException("Invalid argument type for 'date_set', first argument must be Calendar");
		}
		Calendar calOld = (Calendar)calObject;
		Date date = calOld.getTime();
		Calendar cal = GregorianCalendar.getInstance(zone, locale);
		cal.setTime(date);
		
		if (unitConstant.equals(ExpressionParserConstants.DATE_UNIT_YEAR)) {
			cal.set(Calendar.YEAR, (int)setValue);
		} else if (unitConstant.equals(ExpressionParserConstants.DATE_UNIT_MONTH)) {
			cal.set(Calendar.MONTH, (int)setValue);
		} else if (unitConstant.equals(ExpressionParserConstants.DATE_UNIT_WEEK)) {
			cal.set(Calendar.WEEK_OF_YEAR, (int)setValue);
		} else if (unitConstant.equals(ExpressionParserConstants.DATE_UNIT_DAY)) {
			cal.set(Calendar.DAY_OF_MONTH, (int)setValue);
		} else if (unitConstant.equals(ExpressionParserConstants.DATE_UNIT_HOUR)) {
			cal.set(Calendar.HOUR_OF_DAY, (int)setValue);
		} else if (unitConstant.equals(ExpressionParserConstants.DATE_UNIT_MINUTE)) {
			cal.set(Calendar.MINUTE, (int)setValue);
		} else if (unitConstant.equals(ExpressionParserConstants.DATE_UNIT_SECOND)) {
			cal.set(Calendar.SECOND, (int)setValue);
		} else if (unitConstant.equals(ExpressionParserConstants.DATE_UNIT_MILLISECOND)) {
			cal.set(Calendar.MILLISECOND, (int)setValue);
		} else {
			throw new ParseException("Invalid argument type for 'date_set', third argument must be unit constant (e.g. DATE_UNIT_HOUR)");
		}
		
		stack.push(cal);
	}
}
