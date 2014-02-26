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
package com.rapidminer.tools.expression.parser;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.logging.Level;

import com.rapidminer.Process;
import com.rapidminer.tools.LogService;
import com.rapidminer.tools.plugin.Plugin;

/**
 * @author Venkatesh Umaashankar
 *
 */
public class ExpressionParserFactory {

	private static Class<AbstractExpressionParser> parserProviderClass = null;
	public static boolean isParserRegistered = false;

	static {
		try {
			parserProviderClass = (Class<AbstractExpressionParser>) Class.forName("com.rapidminer.tools.jep.function.ExpressionParser", true, Plugin.getMajorClassLoader());
			ExpressionParserFactory.isParserRegistered = true;
			LogService.getRoot().info("Default version of expression parser registered successfully");
		} catch (ClassNotFoundException e) {		
			LogService.getRoot().info("Could not register the default version of expression parser");
			parserProviderClass = null;
			ExpressionParserFactory.isParserRegistered = false;
		}
	}

	public static void registerParser(String parserClass) {
		try {
			parserProviderClass = (Class<AbstractExpressionParser>) Class.forName(parserClass, true, Plugin.getMajorClassLoader());
			ExpressionParserFactory.isParserRegistered = true;
		} catch (ClassNotFoundException e) {
			LogService.getRoot().log(Level.WARNING, "Failed to register expression parser implementation: "+e, e);
			parserProviderClass = null;
			ExpressionParserFactory.isParserRegistered = false;
		}
	}

	public static boolean isParserRegistered() {
		return isParserRegistered;
	}

	public static AbstractExpressionParser getExpressionParser(boolean useStandardConstants) {

		if (ExpressionParserFactory.parserProviderClass == null) {
			LogService.getRoot().log(Level.WARNING, "A valid expression Parser is not registered with the factory");
			return null;
		}

		AbstractExpressionParser parser = null;
		try {
			Method parserProviderMethod = parserProviderClass.getDeclaredMethod("getExpressionParser", boolean.class);
			parser = (AbstractExpressionParser) parserProviderMethod.invoke(parserProviderClass, useStandardConstants);
		} catch (NoSuchMethodException e) {
			LogService.getRoot().log(Level.WARNING, "Could not instantiate expression parser", e);
		} catch (SecurityException e) {
			LogService.getRoot().log(Level.WARNING, "Could not instantiate expression parser", e);
		} catch (IllegalAccessException e) {
			LogService.getRoot().log(Level.WARNING, "Could not instantiate expression parser", e);
		} catch (IllegalArgumentException e) {
			LogService.getRoot().log(Level.WARNING, "Could not instantiate expression parser", e);
		} catch (InvocationTargetException e) {
			LogService.getRoot().log(Level.WARNING, "Could not instantiate expression parser", e);
		}

		return parser;

	}

	public static AbstractExpressionParser getExpressionParser(boolean useStandardConstants, Process process) {

		if (ExpressionParserFactory.parserProviderClass == null) {
			LogService.getRoot().log(Level.WARNING, "A valid expression Parser is not registered with the factory");
			return null;
		}

		AbstractExpressionParser parser = null;
		try {
			Method parserProviderMethod = parserProviderClass.getDeclaredMethod("getExpressionParser", boolean.class, Process.class);
			parser = (AbstractExpressionParser) parserProviderMethod.invoke(parserProviderClass, useStandardConstants, process);
		} catch (NoSuchMethodException e) {
			LogService.getRoot().log(Level.WARNING, "Could not instantiate expression parser", e);
		} catch (SecurityException e) {
			LogService.getRoot().log(Level.WARNING, "Could not instantiate expression parser", e);
		} catch (IllegalAccessException e) {
			LogService.getRoot().log(Level.WARNING, "Could not instantiate expression parser", e);
		} catch (IllegalArgumentException e) {
			LogService.getRoot().log(Level.WARNING, "Could not instantiate expression parser", e);
		} catch (InvocationTargetException e) {
			LogService.getRoot().log(Level.WARNING, "Could not instantiate expression parser", e);
		}

		return parser;
	}

	private ExpressionParserFactory() {}

}
