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
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.Stack;

import org.nfunk.jep.ParseException;
import org.nfunk.jep.function.PostfixMathCommand;

import com.rapidminer.tools.expression.parser.UnknownValue;

/**
 * Parses a Calendar object from either a long value or a string using the given locale.
 * 
 * @author Marco Boeck
 */
public class DateParseWithLocale extends PostfixMathCommand {
	
	public DateParseWithLocale() {
		numberOfParameters = 2;
	}
	
	/**
	 * Create the resulting Calendar object.
	 */
	@SuppressWarnings("unchecked")
	@Override
	public void run(Stack stack) throws ParseException {
		checkStack(stack);// check the stack
		Locale locale = Locale.getDefault();
		
		Object localeObject = stack.pop();
		if (!(localeObject instanceof String)) {
			throw new ParseException("Invalid argument type for 'date_parse_loc', second argument must be (String) for locale (e.g. \"en\")");
		}
		locale = new Locale(String.valueOf(localeObject));
		
		Object dateObject = stack.pop();

		// check for unknown values
		if (dateObject == UnknownValue.UNKNOWN_NOMINAL || (dateObject instanceof Double && ((Double) dateObject).isNaN())) {
			stack.push(UnknownValue.UNKNOWN_DATE);
			return;
		}

		if (dateObject instanceof Double) {
			try {
				dateObject = (long)(double)(Double)dateObject;
			} catch (ClassCastException e) {
				throw new ParseException("Invalid argument for 'date_parse_loc', cannot convert to Date");
			}
		}
		if (!(dateObject instanceof String) && !(dateObject instanceof Long)) {
			throw new ParseException("Invalid argument type for 'date_parse_loc', first argument must be (string) or (long)");
		}
		
		Date date;
		if (dateObject instanceof String) {
			String dateString = (String)dateObject;
			try {
				date = DateFormat.getDateInstance(DateFormat.SHORT, locale).parse(dateString);
				Calendar cal = GregorianCalendar.getInstance(locale);
				cal.setTime(date);
				stack.push(cal);
			} catch (java.text.ParseException e) {
				throw new ParseException("Bad string argument for 'date_parse_loc' (" + e.getMessage() + ") and given locale (" + locale + ")");
			}
		} else if (dateObject instanceof Long) {
			long dateLong = (Long)dateObject;
			date = new Date(dateLong);
			Calendar cal = GregorianCalendar.getInstance(locale);
			cal.setTime(date);
			stack.push(cal);
		}
	}
}
