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
import java.util.Locale;
import java.util.Stack;

import org.nfunk.jep.ParseException;
import org.nfunk.jep.function.PostfixMathCommand;

import com.rapidminer.tools.expression.parser.UnknownValue;

/**
 * Changes a Calendar object to a String using a custom pattern and a given locale.
 * 
 * @author Marco Boeck
 */
public class Date2StringCustom extends PostfixMathCommand {
	
	public Date2StringCustom() {
		numberOfParameters = -1;
	}
	
	/**
	 * Creates the string result.
	 */
	@SuppressWarnings("unchecked")
	@Override
	public void run(Stack stack) throws ParseException {
		checkStack(stack);
		
		Locale locale = Locale.getDefault();
		
		if (curNumberOfParameters == 3) {
			Object localeObject = stack.pop();
			if (!(localeObject instanceof String)) {
				throw new ParseException("Invalid argument type for 'date_str_custom', third argument must be (String) for locale (e.g. \"en\")");
			}
			locale = new Locale(String.valueOf(localeObject));
		} else if (curNumberOfParameters != 2) {
			throw new ParseException("Invalid number of arguments for 'date_str_custom', must be either 2 or 3.");
		}
		
		Object customFormatObject = stack.pop();
		if (!(customFormatObject instanceof String)) {
			throw new ParseException("Invalid argument type for 'date_str_custom', second argument must be custom formatting string");
		}
		String customFormat = String.valueOf(customFormatObject);
		SimpleDateFormat simpleDateFormatter;
		try{
			simpleDateFormatter = new SimpleDateFormat(customFormat, locale);
		} catch(IllegalArgumentException e) {
			throw new ParseException("Invalid argument for 'date_str_custom', second argument must be valid custom formatting string");
		}
		
		Object customCalObject = stack.pop();
		// check for unknown values
		if (customCalObject == UnknownValue.UNKNOWN_DATE) {
			stack.push(UnknownValue.UNKNOWN_NOMINAL);
			return;
		}
		
		if (!(customCalObject instanceof Calendar)) {
			throw new ParseException("Invalid argument type for 'date_str_custom', first argument must be Calendar");
		}
		Calendar cal = (Calendar)customCalObject;
		String result = simpleDateFormatter.format(cal.getTime());
		
		stack.push(result);
	}
}
