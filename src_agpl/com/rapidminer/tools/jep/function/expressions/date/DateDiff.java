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

import com.rapidminer.tools.expression.parser.UnknownValue;

/**
 * Determines the elapsed time between the first Date and the second Date.
 * 
 * @author Marco Boeck
 */
public class DateDiff extends PostfixMathCommand {
	
	public DateDiff() {
		// variable numer of parameters
		numberOfParameters = -1;
	}
	
	/**
	 * Creates the boolean result.
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
				throw new ParseException("Invalid argument type for 'date_diff', fourth argument must be (String) for TimeZone (e.g. America/Los_Angeles)");
			}
			zone = TimeZone.getTimeZone(String.valueOf(timezoneObject));
			
			Object localeObject = stack.pop();
			if (!(localeObject instanceof String)) {
				throw new ParseException("Invalid argument type for 'date_diff', third argument must be (String) for locale (e.g. \"en\")");
			}
			locale = new Locale(String.valueOf(localeObject));
		} else if (curNumberOfParameters != 2) {
			throw new ParseException("Invalid number of arguments for 'date_diff', must be either 2 or 4.");
		}
		
		Object calObjectTwo = stack.pop();
		Object calObjectOne = stack.pop();
		// check for unknown values
		if (calObjectTwo == UnknownValue.UNKNOWN_DATE || calObjectOne == UnknownValue.UNKNOWN_DATE) {
			stack.push(Double.NaN);
			return;
		}

		if (!(calObjectOne instanceof Calendar) || !(calObjectTwo instanceof Calendar)) {
			throw new ParseException("Invalid argument type for 'date_diff', first and second argument must both be Calendar");
		}
		Calendar calOldOne = (Calendar)calObjectOne;
		Calendar calOldTwo = (Calendar)calObjectTwo;
		Date dateOne = calOldOne.getTime();
		Date dateTwo = calOldTwo.getTime();
		Calendar calOne = GregorianCalendar.getInstance(zone, locale);
		calOne.setTime(dateOne);
		Calendar calTwo = GregorianCalendar.getInstance(zone, locale);
		calTwo.setTime(dateTwo);
		// difference including offsets like daylight saving time (specific to timezones)
		long timeTwo = calTwo.getTimeInMillis() + calTwo.getTimeZone().getOffset(calTwo.getTimeInMillis());
        long timeOne = calOne.getTimeInMillis() + calOne.getTimeZone().getOffset(calOne.getTimeInMillis());
		stack.push(timeTwo - timeOne);
	}
}
