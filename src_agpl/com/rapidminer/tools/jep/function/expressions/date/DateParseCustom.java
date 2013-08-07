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

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.Stack;

import org.nfunk.jep.ParseException;
import org.nfunk.jep.function.PostfixMathCommand;

import com.rapidminer.tools.expression.parser.UnknownValue;

/**
 * Parses a Calendar object from a custom string.
 * 
 * @author Marco Boeck
 */
public class DateParseCustom extends PostfixMathCommand {
	
	public DateParseCustom() {
		numberOfParameters = -1;
	}
	
	/**
	 * Create the resulting Calendar object.
	 */
	@SuppressWarnings("unchecked")
	@Override
	public void run(Stack stack) throws ParseException {
		checkStack(stack);// check the stack
		
		Locale locale = Locale.getDefault();
		
		if (curNumberOfParameters == 3) {
			Object localeObject = stack.pop();
			if (!(localeObject instanceof String)) {
				throw new ParseException("Invalid argument type for 'date_parse_custom', third argument must be (String) for locale (e.g. \"en\")");
			}
			locale = new Locale(String.valueOf(localeObject));
		} else if (curNumberOfParameters != 2) {
			throw new ParseException("Invalid number of arguments for 'date_parse_custom', must be either 2 or 3.");
		}
		
		Object customFormatObject = stack.pop();
		if (!(customFormatObject instanceof String)) {
			throw new ParseException("Invalid argument type for 'date_parse_custom', second argument must be custom formatting string");
		}
		String customFormat = String.valueOf(customFormatObject);
		SimpleDateFormat simpleDateFormatter;
		try{
			simpleDateFormatter = new SimpleDateFormat(customFormat, locale);
		} catch(IllegalArgumentException e) {
			throw new ParseException("Invalid argument type for 'date_parse_custom', second argument must be valid custom formatting string");
		}
		
		Object customDateObject = stack.pop();
		// check for unknown values
		if (customDateObject == UnknownValue.UNKNOWN_NOMINAL) {
			stack.push(UnknownValue.UNKNOWN_DATE);
			return;
		}

		if (!(customDateObject instanceof String)) {
			throw new ParseException("Invalid argument type for 'date_parse_custom', first argument must be date string");
		}
		String customDate = String.valueOf(customDateObject);
		Date parsedDate;
		try {
			parsedDate = simpleDateFormatter.parse(customDate);
		} catch(java.text.ParseException e) {
			throw new ParseException("Invalid argument type for 'date_parse_custom', first argument must be valid date string");
		}
		Calendar cal = GregorianCalendar.getInstance(locale);
		cal.setTime(parsedDate);
		
		stack.push(cal);
	}
}
