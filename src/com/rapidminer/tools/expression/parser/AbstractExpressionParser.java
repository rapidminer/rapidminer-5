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

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.rapidminer.MacroHandler;
import com.rapidminer.Process;
import com.rapidminer.example.Attribute;
import com.rapidminer.example.AttributeRole;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.table.AttributeFactory;
import com.rapidminer.generator.GenerationException;
import com.rapidminer.operator.ports.metadata.AttributeMetaData;
import com.rapidminer.operator.ports.metadata.ExampleSetMetaData;
import com.rapidminer.operator.ports.metadata.SetRelation;
import com.rapidminer.tools.LoggingHandler;
import com.rapidminer.tools.Ontology;
import com.rapidminer.tools.RandomGenerator;
import com.rapidminer.tools.Tools;

/**
 * @author Venkatesh Umaashankar
 *
 */
abstract public class AbstractExpressionParser {

	private static final List<String> FUNCTION_GROUPS = new LinkedList<String>(Arrays.asList(new String[] { "Basic Operators", "Log and Exponential", "Trigonometric", "Statistical", "Text", "Date", "Process", "Miscellaneous" }));

	private static final Map<String, List<FunctionDescription>> FUNCTION_DESCRIPTIONS = new HashMap<String, List<FunctionDescription>>();

	static {
		// basic operators
		List<FunctionDescription> operatorFunctions = new LinkedList<FunctionDescription>();
		operatorFunctions.add(new FunctionDescription("+", "Addition", "Calculates the addition of the two terms surrounding this operator; example: att1 + 7", 2));
		operatorFunctions.add(new FunctionDescription("-", "Subtraction", "Calculates the subtraction of the first term by the second one; example: 42 - att2", 2));
		operatorFunctions.add(new FunctionDescription("*", "Multiplication", "Calculates the multiplication of the two terms surrounding this operator; example: 5 * att3", 2));
		operatorFunctions.add(new FunctionDescription("/", "Division", "Calculates the division of the first term by the second one; example: 12 / 4", 2));
		operatorFunctions.add(new FunctionDescription("^", "Power", "Calculates the first term to the power of the second one; example: 2^3", 2));
		operatorFunctions.add(new FunctionDescription("%", "Modulus", "Calculates the modulus of the first term by the second one; example: 11 % 2", 2));
		operatorFunctions.add(new FunctionDescription("<", "Less Than", "Delivers true if the first term is less than the second; example: att1 < 4", 2));
		operatorFunctions.add(new FunctionDescription(">", "Greater Than", "Delivers true if the first term is greater than the second; example: att2 > 3", 2));
		operatorFunctions.add(new FunctionDescription("<=", "Less Equals", "Delivers true if the first term is less than or equal to the second; example: att3 <= 5", 2));
		operatorFunctions.add(new FunctionDescription(">=", "Greater Equals", "Delivers true if the first term is greater than or equal to the second; example: att4 >= 4", 2));
		operatorFunctions.add(new FunctionDescription("==", "Equals", "Delivers true if the first term is equal to the second; example: att1 == att2", 2));
		operatorFunctions.add(new FunctionDescription("!=", "Not Equals", "Delivers true if the first term is not equal to the second; example: att1 != att2", 2));
		operatorFunctions.add(new FunctionDescription("!", "Boolean Not", "Delivers true if the following term is false or vice versa; example: !(att1 > 2)", 1));
		operatorFunctions.add(new FunctionDescription("&&", "Boolean And", "Delivers true if both surrounding terms are true; example: (att1 > 2) && (att2 < 4)", 2));
		operatorFunctions.add(new FunctionDescription("||", "Boolean Or", "Delivers true if at least one of the surrounding terms is true; example: (att1 < 3) || (att2 > 1)", 2));
		FUNCTION_DESCRIPTIONS.put(FUNCTION_GROUPS.get(0), operatorFunctions);

		// log and exponential functions
		List<FunctionDescription> logFunctions = new LinkedList<FunctionDescription>();
		logFunctions.add(new FunctionDescription("ln()", "Natural Logarithm", "Calculates the logarithm of the argument to the base e; example: ln(5)", 1));
		logFunctions.add(new FunctionDescription("log()", "Logarithm Base 10", "Calculates the logarithm of the argument to the base 10; example: log(att1)", 1));
		logFunctions.add(new FunctionDescription("ld()", "Logarithm Base 2", "Calculates the logarithm of the argument to the base 2; example: ld(att2)", 1));
		logFunctions.add(new FunctionDescription("exp()", "Exponential", "Calculates the value of the constant e to the power of the argument; example: exp(att3)", 1));
		logFunctions.add(new FunctionDescription("pow()", "Power", "Calculates the first term to the power of the second one; example: pow(att1, 3)", 2));
		FUNCTION_DESCRIPTIONS.put(FUNCTION_GROUPS.get(1), logFunctions);

		// trigonometric functions
		List<FunctionDescription> trigonometricFunctions = new LinkedList<FunctionDescription>();
		trigonometricFunctions.add(new FunctionDescription("sin()", "Sine", "Calculates the sine of the given argument; example: sin(att1)", 1));
		trigonometricFunctions.add(new FunctionDescription("cos()", "Cosine", "Calculates the cosine of the given argument; example: cos(att2)", 1));
		trigonometricFunctions.add(new FunctionDescription("tan()", "Tangent", "Calculates the tangent of the given argument; example: tan(att3)", 1));
		trigonometricFunctions.add(new FunctionDescription("asin()", "Arc Sine", "Calculates the inverse sine of the given argument; example: asin(att1)", 1));
		trigonometricFunctions.add(new FunctionDescription("acos()", "Arc Cos", "Calculates the inverse cosine of the given argument; example: acos(att2)", 1));
		trigonometricFunctions.add(new FunctionDescription("atan()", "Arc Tangent", "Calculates the inverse tangent of the given argument; example: atan(att3)", 1));
		trigonometricFunctions.add(new FunctionDescription("atan2()", "Arc Tangent 2", "Calculates the inverse tangent based on the two given arguments; example: atan(att1, 0.5)",
				2));
		trigonometricFunctions.add(new FunctionDescription("sinh()", "Hyperbolic Sine", "Calculates the hyperbolic sine of the given argument; example: sinh(att2)", 1));
		trigonometricFunctions.add(new FunctionDescription("cosh()", "Hyperbolic Cosine", "Calculates the hyperbolic cosine of the given argument; example: cosh(att3)", 1));
		trigonometricFunctions.add(new FunctionDescription("tanh()", "Hyperbolic Tangent", "Calculates the hyperbolic tangent of the given argument; example: tanh(att1)", 1));
		trigonometricFunctions.add(new FunctionDescription("asinh()", "Inverse Hyperbolic Sine",
				"Calculates the inverse hyperbolic sine of the given argument; example: asinh(att2)", 1));
		trigonometricFunctions.add(new FunctionDescription("acosh()", "Inverse Hyperbolic Cosine",
				"Calculates the inverse hyperbolic cosine of the given argument; example: acosh(att3)", 1));
		trigonometricFunctions.add(new FunctionDescription("atanh()", "Inverse Hyperbolic Tangent",
				"Calculates the inverse hyperbolic tangent of the given argument; example: atanh(att1)", 1));
		FUNCTION_DESCRIPTIONS.put(FUNCTION_GROUPS.get(2), trigonometricFunctions);

		// statistical functions
		List<FunctionDescription> statisticalFunctions = new LinkedList<FunctionDescription>();
		statisticalFunctions
				.add(new FunctionDescription(
						"round()",
						"Round",
						"Rounds the given number to the next integer. If two arguments are given, the first one is rounded to the number of digits indicated by the second argument; example: round(att1) or round(att2, 3)",
						2));
		statisticalFunctions.add(new FunctionDescription("floor()", "Floor", "Calculates the next integer less than the given argument; example: floor(att3)", 1));
		statisticalFunctions.add(new FunctionDescription("ceil()", "Ceil", "Calculates the next integer greater than the given argument; example: ceil(att1)", 1));
		statisticalFunctions.add(new FunctionDescription("avg()", "Average", "Calculates the average of the given arguments; example: avg(att1, att3)",
				FunctionDescription.UNLIMITED_NUMBER_OF_ARGUMENTS));
		statisticalFunctions.add(new FunctionDescription("min()", "Minimum", "Calculates the minimum of the given arguments; example: min(0, att2, att3)",
				FunctionDescription.UNLIMITED_NUMBER_OF_ARGUMENTS));
		statisticalFunctions.add(new FunctionDescription("max()", "Maximum", "Calculates the maximum of the given arguments; example: max(att1, att2)",
				FunctionDescription.UNLIMITED_NUMBER_OF_ARGUMENTS));
		FUNCTION_DESCRIPTIONS.put(FUNCTION_GROUPS.get(3), statisticalFunctions);

		// text functions
		List<FunctionDescription> textFunctions = new LinkedList<FunctionDescription>();
		textFunctions.add(new FunctionDescription("str()", "To String", "Transforms the given number into a string (nominal value); example: str(17)", 1));
		textFunctions.add(new FunctionDescription("parse()", "To Number", "Transforms the given string (nominal value) into a number by parsing it; example: parse(att2)", 1));
		textFunctions.add(new FunctionDescription("cut()", "Cut",
				"Cuts the substring of given length at the given start out of a string; example: cut(\"Text\", 1, 2) delivers \"ex\"", 3));
		textFunctions.add(new FunctionDescription("concat()", "Concatenation",
				"Concatenates the given arguments (the + operator can also be used for this); <br>example: both concat(\"At\", \"om\") and \"At\" + \"om\" deliver \"Atom\"",
				FunctionDescription.UNLIMITED_NUMBER_OF_ARGUMENTS));
		textFunctions
				.add(new FunctionDescription(
						"replace()",
						"Replace",
						"Replaces all occurences of a search string by the defined replacement; <br>example: replace(att1, \"am\", \"pm\") replaces all occurences of \"am\" in each value of attribute att1 by \"pm\"",
						3));
		textFunctions
				.add(new FunctionDescription(
						"replaceAll()",
						"Replace All",
						"Evaluates the first argument as regular expression and replaces all matches by the defined replacement; <br>example: replaceAll(att1, \"[abc]\", \"X\") replaces all occurences of \"a\", \"b\" or \"c\" by \"X\" in each value of attribute att1",
						3));
		textFunctions.add(new FunctionDescription("lower()", "Lower", "Transforms the given argument into lower case characters; example: lower(att2)", 1));
		textFunctions.add(new FunctionDescription("upper()", "Upper", "Transforms the given argument into upper case characters; example: upper(att3)", 1));
		textFunctions.add(new FunctionDescription("index()", "Index",
				"Delivers the first position of the given search string in the text; example: index(\"Text\", \"e\") delivers 1", 2));
		textFunctions.add(new FunctionDescription("length()", "Length", "Delivers the length of the given argument; example: length(att1)", 1));
		textFunctions.add(new FunctionDescription("char()", "Character At", "Delivers the character at the specified position; example: char(att2, 3)", 2));
		textFunctions.add(new FunctionDescription("compare()", "Compare",
				"Compares the two arguments and deliver a negative value, if the first argument is lexicographically smaller; example: compare(att2, att3)", 2));
		textFunctions.add(new FunctionDescription("contains()", "Contains", "Delivers true if the second argument is part of the first one; example: contains(att1, \"pa\")", 2));
		textFunctions.add(new FunctionDescription("equals()", "Equals",
				"Delivers true if the two arguments are lexicographically equal to each other; example: equals(att1, att2)", 2));
		textFunctions.add(new FunctionDescription("starts()", "Starts With", "Delivers true if the first argument starts with the second; example: starts(att1, \"OS\")", 2));
		textFunctions.add(new FunctionDescription("ends()", "Ends With", "Delivers true if the first argument ends with the second; example: ends(att2, \"AM\")", 2));
		textFunctions.add(new FunctionDescription("matches()", "Matches",
				"Delivers true if the first argument matches the regular expression defined by the second argument; example: matches(att3, \".*mm.*\"", 2));
		textFunctions.add(new FunctionDescription("finds()", "Finds",
				"Delivers true if, and only if, a subsequence of the first matches the regular expression defined by the second argument; example: finds(att3, \".*AM.*|.*PM.*\"",
				2));
		textFunctions.add(new FunctionDescription("suffix()", "Suffix", "Delivers the suffix of the specified length; example: suffix(att1, 2)", 2));
		textFunctions.add(new FunctionDescription("prefix()", "Prefix", "Delivers the prefix of the specified length; example: prefix(att2, 3)", 2));
		textFunctions.add(new FunctionDescription("trim()", "Trim", "Removes all leading and trailing white space characters; example: trim(att3)", 1));
		textFunctions.add(new FunctionDescription("escape_html()", "Escape HTML", "Escapes the given string with HTML entities; example: escape_html(att1)", 1));
		FUNCTION_DESCRIPTIONS.put(FUNCTION_GROUPS.get(4), textFunctions);

		// date functions
		List<FunctionDescription> dateFunctions = new LinkedList<FunctionDescription>();
		dateFunctions.add(new FunctionDescription("date_parse()", "Parse Date", "Parses the given string or double to a date; example: date_parse(att1)", 1));
		dateFunctions.add(new FunctionDescription("date_parse_loc()", "Parse Date with Locale",
				"Parses the given string or double to a date with the given locale (via lowercase two-letter ISO-639 code); <br>example: date_parse(att1, en)", 2));
		dateFunctions
				.add(new FunctionDescription(
						"date_parse_custom()",
						"Parse Custom Date",
						"Parses the given date string to a date using a custom pattern and the given locale (via lowercase two-letter ISO-639 code); <br>example: date_parse_custom(att1, \"dd|MM|yy\", \"de\")",
						3));
		dateFunctions.add(new FunctionDescription("date_before()", "Date Before",
				"Determines if the first date is strictly earlier than the second date; example: date_before(att1, att2)", 2));
		dateFunctions.add(new FunctionDescription("date_after()", "Date After",
				"Determines if the first date is strictly later than the second date; example: date_after(att1, att2)", 2));
		dateFunctions.add(new FunctionDescription("date_str()", "Date to String",
				"Changes a date to a string using the specified format; example: date_str(att1, DATE_FULL, DATE_SHOW_DATE_AND_TIME)", 3));
		dateFunctions
				.add(new FunctionDescription(
						"date_str_loc()",
						"Date to String with Locale",
						"Changes a date to a string using the specified format and the given locale (via lowercase two-letter ISO-639 code); <br>example: date_str_loc(att1, DATE_MEDIUM, DATE_SHOW_TIME_ONLY, \"us\")",
						4));
		dateFunctions
				.add(new FunctionDescription(
						"date_str_custom()",
						"Date to String with custom pattern",
						"Changes a date to a string using the specified custom format pattern and the (optional) given locale (via lowercase two-letter ISO-639 code); <br>example: date_str_custom(att1, \"dd|MM|yy\", \"us\")",
						4));
		dateFunctions.add(new FunctionDescription("date_now()", "Create Date", "Creates the current date; example: date_now()", 0));
		dateFunctions
				.add(new FunctionDescription(
						"date_diff()",
						"Date Difference",
						"Calculates the elapsed time between two dates. Locale and time zone arguments are optional; example: date_diff(timeStart, timeEnd, \"us\", \"America/Los_Angeles\")",
						4));
		dateFunctions
				.add(new FunctionDescription(
						"date_add()",
						"Add Time",
						"Allows to add a custom amount of time to a given date. Note that only the integer portion of a given value will be used! <br>Locale and Timezone arguments are optional; example: date_add(date, value, DATE_UNIT_DAY, \"us\", \"America/Los_Angeles\")",
						5));
		dateFunctions
				.add(new FunctionDescription(
						"date_set()",
						"Set Time",
						"Allows to set a custom value for a portion of a given date, e.g. set the day to 23. Note that only the integer portion of a given value will be used! <br>Locale and Timezone arguments are optional; example: date_set(date, value, DATE_UNIT_DAY, \"us\", \"America/Los_Angeles\")",
						5));
		dateFunctions
				.add(new FunctionDescription(
						"date_get()",
						"Get Time",
						"Allows to get a portion of a given date, e.g. get the day of a month only. Locale and Timezone arguments are optional; example: date_get(date, DATE_UNIT_DAY, \"us\", \"America/Los_Angeles\")",
						4));
		FUNCTION_DESCRIPTIONS.put(FUNCTION_GROUPS.get(5), dateFunctions);

		// process functions
		List<FunctionDescription> processFunctions = new LinkedList<FunctionDescription>();
		processFunctions.add(new FunctionDescription("param()", "Parameter",
				"Delivers the specified parameter of the specified operator; example: param(\"Read Excel\", \"file\")", 2));
		processFunctions.add(new FunctionDescription("macro()", "Macro", "Delivers the value of the macro with the name specified by the first argument as string; example: macro(\"myMacro\"). Optionally a default value can be specified, which is delivered if the macro is not defined: macro(\"myMacro\", \"default value\")", -1));
		FUNCTION_DESCRIPTIONS.put(FUNCTION_GROUPS.get(6), processFunctions);

		// miscellaneous functions
		List<FunctionDescription> miscellaneousFunctions = new LinkedList<FunctionDescription>();
		miscellaneousFunctions
				.add(new FunctionDescription(
						"if()",
						"If-Then-Else",
						"Delivers the result of the second argument if the first one is evaluated to true and the result of the third argument otherwise; <br>example: if(att1 > 5, 7 * att1, att2 / 2)",
						3));
		miscellaneousFunctions.add(new FunctionDescription("const()", "Constant", "Delivers the argument as numerical constant value; example: const(att1)", 1));
		miscellaneousFunctions.add(new FunctionDescription("sqrt()", "Square Root", "Delivers the square root of the given argument; example: sqrt(att2)", 1));
		miscellaneousFunctions.add(new FunctionDescription("sgn()", "Signum", "Delivers -1 or +1 depending on the signum of the argument; example: sgn(-5)", 1));
		miscellaneousFunctions.add(new FunctionDescription("rand()", "Random", "Delivers a random number between 0 and 1; example: rand()", 0));
		miscellaneousFunctions.add(new FunctionDescription("mod()", "Modulus", "Calculates the modulus of the first term by the second one; example: 11 % 2", 2));
		miscellaneousFunctions.add(new FunctionDescription("sum()", "Sum", "Calculates the sum of all arguments; example: sum(att1, att3, 42)",
				FunctionDescription.UNLIMITED_NUMBER_OF_ARGUMENTS));
		miscellaneousFunctions.add(new FunctionDescription("binom()", "Binomial", "Calculates the binomial coefficients; example: binom(5, 2)", 2));
		miscellaneousFunctions.add(new FunctionDescription("missing()", "Missing", "Checks if the given number is missing; example: missing(att1)", 1));
		miscellaneousFunctions.add(new FunctionDescription("bit_or()", "Bitwise OR", "Calculate the bitwise OR of two integer arguments; example: bit_or(att1, att2)", 2));
		miscellaneousFunctions.add(new FunctionDescription("bit_and()", "Bitwise AND", "Calculate the bitwise AND of two integer arguments; example: bit_and(att2, att3)", 2));
		miscellaneousFunctions.add(new FunctionDescription("bit_xor()", "Bitwise XOR", "Calculate the bitwise XOR of two integer arguments; example: bit_xor(att1, att3)", 2));
		miscellaneousFunctions.add(new FunctionDescription("bit_not()", "Bitwise NOT", "Calculate the bitwise NOT of the integer argument; example: bit_not(att2)", 1));
		FUNCTION_DESCRIPTIONS.put(FUNCTION_GROUPS.get(7), miscellaneousFunctions);

	}
	
	private static final ArrayList<Function> CUSTOM_FUNCTIONS = new ArrayList<Function>();

	public abstract void setAllowUndeclared(boolean value);

	public abstract void parseExpression(String expression) throws ExpressionParserException;

	public abstract String getErrorInfo();

	public abstract void addVariable(String name, Object object) throws ExpressionParserException;

	public abstract Object getValueAsObject() throws ExpressionParserException;

	public String[] getFunctionGroups() {
		return FUNCTION_GROUPS.toArray(new String[FUNCTION_GROUPS.size()]);
	}

	public List<FunctionDescription> getFunctions(String functionGroup) {
		return FUNCTION_DESCRIPTIONS.get(functionGroup);
	}

	public abstract void addStandardConstants();

	protected void addCustomConstants() {
		addConstant("TRUE", Boolean.valueOf(true));
		addConstant("FALSE", Boolean.valueOf(false));
		addConstant("NaN", Double.NaN);
		addConstant("NAN", Double.NaN);

		addConstant("DATE_SHORT", ExpressionParserConstants.DATE_FORMAT_SHORT);
		addConstant("DATE_MEDIUM", ExpressionParserConstants.DATE_FORMAT_MEDIUM);
		addConstant("DATE_LONG", ExpressionParserConstants.DATE_FORMAT_LONG);
		addConstant("DATE_FULL", ExpressionParserConstants.DATE_FORMAT_FULL);
		addConstant("DATE_SHOW_DATE_ONLY", ExpressionParserConstants.DATE_SHOW_DATE_ONLY);
		addConstant("DATE_SHOW_TIME_ONLY", ExpressionParserConstants.DATE_SHOW_TIME_ONLY);
		addConstant("DATE_SHOW_DATE_AND_TIME", ExpressionParserConstants.DATE_SHOW_DATE_AND_TIME);
		addConstant("DATE_UNIT_YEAR", ExpressionParserConstants.DATE_UNIT_YEAR);
		addConstant("DATE_UNIT_MONTH", ExpressionParserConstants.DATE_UNIT_MONTH);
		addConstant("DATE_UNIT_WEEK", ExpressionParserConstants.DATE_UNIT_WEEK);
		addConstant("DATE_UNIT_DAY", ExpressionParserConstants.DATE_UNIT_DAY);
		addConstant("DATE_UNIT_HOUR", ExpressionParserConstants.DATE_UNIT_HOUR);
		addConstant("DATE_UNIT_MINUTE", ExpressionParserConstants.DATE_UNIT_MINUTE);
		addConstant("DATE_UNIT_SECOND", ExpressionParserConstants.DATE_UNIT_SECOND);
		addConstant("DATE_UNIT_MILLISECOND", ExpressionParserConstants.DATE_UNIT_MILLISECOND);
	}

	public abstract void addConstant(String constantName, Object value);

	public abstract void addFunction(String functionName, Object value);

	protected abstract void addCustomFunctions();

	public abstract void initParser(boolean useStandardConstants);

	public abstract void initParser(boolean useStandardConstants, Process process);

	/**
	 * This method allows to derive a value from the given function and store it
	 * as a macro in the macroHandler under the given name.
	 */
	public void addMacro(MacroHandler macroHandler, String name, String expression) throws GenerationException {
		// parse expression
		// check for errors
		Object result = null;
		try {
			parseExpression(expression);
			result = getValueAsObject();
		} catch (ExpressionParserException e1) {
			throw new GenerationException(e1.getMessage());
		}

		// set result as macro
		if (result != null) {
			if (result instanceof Calendar) {
				Calendar calendar = (Calendar) result;
				macroHandler.addMacro(name, Tools.formatDateTime(new Date(calendar.getTimeInMillis())));
			} else {
				try {
					macroHandler.addMacro(name, Tools.formatIntegerIfPossible(Double.parseDouble(result.toString())));
				} catch (NumberFormatException e) {
					macroHandler.addMacro(name, result.toString());
				}
			}
		}
	}

	public abstract boolean hasError();

	/** Subclasses must implement this method to provide Jep's respective implementation of SymbolTable values
	 *  to {@link #addAttributeMetaData(ExampleSetMetaData, String, String)}. */
	protected abstract Collection getSymbolTableValues();

	/** Adds attribute meta data to the example set meta data that represent the attributes that will be generated
	 *  by this instance for the given expression. 
	 * @throws ExpressionParserException */
	public void addAttributeMetaData(ExampleSetMetaData emd, String name, String expression) throws GenerationException {
		Collection symbolTableValues = getSymbolTableValues();
		try {
			setAllowUndeclared(true); //set to true before parsing, otherwise parseExpression will throw an exception 
			parseExpression(expression);

			Map<String, AttributeMetaData> name2attributes = new HashMap<String, AttributeMetaData>();
			for (Object variableObj : symbolTableValues) {
				if (!isConstant(variableObj)) {
					AttributeMetaData attribute = emd.getAttributeByName(getVariableName(variableObj));
					if (attribute != null) {
						name2attributes.put(getVariableName(variableObj), attribute);
						if (attribute.isNominal()) {
							addVariable(attribute.getName(), "");
						} else {
							addVariable(attribute.getName(), Double.NaN);
						}
					}
				}
			}

			// create the new attribute from the delivered type
			Object result = getValueAsObject();

			AttributeMetaData newAttribute = null;
			if (result instanceof Boolean) {
				newAttribute = new AttributeMetaData(name, Ontology.BINOMINAL);
				HashSet<String> values = new HashSet<String>();
				values.add("false");
				values.add("true");
				newAttribute.setValueSet(values, SetRelation.EQUAL);
			} else if (result instanceof Number) {
				newAttribute = new AttributeMetaData(name, Ontology.REAL);
			} else if (isComplex(result)) {
				newAttribute = new AttributeMetaData(name, Ontology.REAL);
			} else if (result instanceof Date) {
				newAttribute = new AttributeMetaData(name, Ontology.DATE_TIME);
			} else if (result instanceof Calendar) {
				newAttribute = new AttributeMetaData(name, Ontology.DATE_TIME);
			} else {
				newAttribute = new AttributeMetaData(name, Ontology.NOMINAL);
			}
			emd.addAttribute(newAttribute);

		} catch (ExpressionParserException e) {
			emd.addAttribute(new AttributeMetaData(name, Ontology.ATTRIBUTE_VALUE));
		}

	}

	protected abstract boolean isComplex(Object result);

	protected abstract String getVariableName(Object variableObj);

	protected abstract boolean isConstant(Object variableObj);

	protected abstract double getDoubleValueofComplex(Object result);

	/**
	 * Iterates over the {@link ExampleSet}, interprets attributes as variables,
	 * evaluates the function and creates a new attribute with the given name
	 * that takes the expression's value. The type of the attribute depends on
	 * the expression type and is {@link Ontology#NOMINAL} for strings,
	 * {@link Ontology#NUMERICAL} for reals and complex numbers,
	 * {@link Ontology#DATE_TIME} for Dates and Calendars and
	 * {@link Ontology#BINOMINAL} with values &quot;true&quot; and
	 * &quot;false&quot; for booleans.
	 * 
	 * @return The generated attribute
	 * */
	public Attribute addAttribute(ExampleSet exampleSet, String name, String expression) throws GenerationException {

		Object result = null;
		Map<String, Attribute> name2attributes = null;

		// expression parse only need to be called if there a examples present
		if (exampleSet.size() != 0) {
			setAllowUndeclared(true);

			try {
				parseExpression(expression);
				name2attributes = deriveVariablesFromExampleSet(exampleSet);
				result = getValueAsObject();
			} catch (ExpressionParserException e) {
				throw new GenerationException("Offending attribute: '" + name + "', Expression: '" + expression + "', Error: '" + getErrorInfo() + "'");
			}

		}

		Attribute newAttribute = null;
		// if != null this needs to be overriden
		Attribute existingAttribute = exampleSet.getAttributes().get(name);
		StringBuffer appendix = new StringBuffer();
		String targetName = name;
		if (existingAttribute != null) {
			// append a random string to the attribute's name until it's a unique attribute name
			do {
				appendix.append(RandomGenerator.getGlobalRandomGenerator().nextString(5));
			} while (exampleSet.getAttributes().get(name + appendix.toString()) != null);
			name = name + appendix.toString();
		}

		if (result instanceof Boolean || result == UnknownValue.UNKNOWN_BOOLEAN) {
			newAttribute = AttributeFactory.createAttribute(name, Ontology.BINOMINAL);
			newAttribute.getMapping().mapString("false");
			newAttribute.getMapping().mapString("true");
		} else if (result instanceof Number) {
			newAttribute = AttributeFactory.createAttribute(name, Ontology.REAL);
		} else if (isComplex(result)) {
			newAttribute = AttributeFactory.createAttribute(name, Ontology.REAL);
		} else if (result instanceof Date || result == UnknownValue.UNKNOWN_DATE) {
			newAttribute = AttributeFactory.createAttribute(name, Ontology.DATE_TIME);
		} else if (result instanceof Calendar || result == UnknownValue.UNKNOWN_DATE) {
			newAttribute = AttributeFactory.createAttribute(name, Ontology.DATE_TIME);
		} else {
			newAttribute = AttributeFactory.createAttribute(name, Ontology.NOMINAL);
		}

		// set construction description
		newAttribute.setConstruction(expression);

		// add new attribute to table and example set
		exampleSet.getExampleTable().addAttribute(newAttribute);
		exampleSet.getAttributes().addRegular(newAttribute);

		// create attribute of correct type and all values
		for (Example example : exampleSet) {

			// assign values to the variables
			assignVariableValuesFromExample(example, name2attributes);

			// calculate result
			try {
				result = getValueAsObject();
			} catch (ExpressionParserException e) {
				throw new GenerationException("Offending attribute: '" + name + "', Expression: '" + expression + "', Error: '" + getErrorInfo() + "'");
			}

			// store result
			if (result instanceof Boolean) {
				if ((Boolean) result) {
					example.setValue(newAttribute, newAttribute.getMapping().mapString("true"));
				} else {
					example.setValue(newAttribute, newAttribute.getMapping().mapString("false"));
				}
			} else if (result instanceof Number) {
				example.setValue(newAttribute, ((Number) result).doubleValue());
			} else if (isComplex(result)) {
				example.setValue(newAttribute, getDoubleValueofComplex(result));
			} else if (result instanceof Date) {
				example.setValue(newAttribute, ((Date) result).getTime());
			} else if (result instanceof Calendar) {
				example.setValue(newAttribute, ((Calendar) result).getTimeInMillis());
			} else if (result instanceof UnknownValue) {
				example.setValue(newAttribute, Double.NaN);
			} else if (result == null) {
				throw new GenerationException("Offending attribute: '" + name + "', Expression: '" + expression + "', Error: '" + getErrorInfo() + "'");
			} else {
				example.setValue(newAttribute, newAttribute.getMapping().mapString(result.toString()));
			}
		}

		// remove existing attribute (if necessary)
		if (existingAttribute != null) {
			/* FIXME: The following line cannot be used, as the attribute might
			 * occur in other example sets, or other attribute instances might use the same 
			 * ExampleTable's column.
			 * 
			 * exampleSet.getExampleTable().removeAttribute(existingAttribute);
			 */
			AttributeRole oldRole = exampleSet.getAttributes().getRole(existingAttribute);
			exampleSet.getAttributes().remove(existingAttribute);
			newAttribute.setName(targetName);
			// restore role from old attribute to new attribute
			exampleSet.getAttributes().setSpecialAttribute(newAttribute, oldRole.getSpecialName());
		}

		return newAttribute;
	}

	/**
	 * Make the exampleSet's attributes available to the parser as variables.
	 * @param parser
	 * @param exampleSet
	 * @return
	 * @throws GenerationException
	 */
	public Map<String, Attribute> deriveVariablesFromExampleSet(ExampleSet exampleSet) throws GenerationException {
		Map<String, Attribute> name2attributes;
		try {
			// new iterator to prevent ConcurrentModificationException
			Collection symbolTableValues = new LinkedList(getSymbolTableValues());
			name2attributes = new HashMap<String, Attribute>();
			for (Object variableObj : symbolTableValues) {
				if (!isConstant(variableObj)) {
					Attribute attribute = exampleSet.getAttributes().get(getVariableName(variableObj));
					if (attribute == null) {
						throw new GenerationException("No such attribute: '" + getVariableName(variableObj) + "'");
					} else {
						name2attributes.put(getVariableName(variableObj), attribute);
						// retrieve test example with real values (needed to
						// compliance checking!)
						if (exampleSet.size() > 0) {
							Example example = exampleSet.iterator().next();
							if (attribute.isNominal()) {
								if (Double.isNaN(example.getValue(attribute))) {
									addVariable(attribute.getName(), UnknownValue.UNKNOWN_NOMINAL); // ExpressionParserConstants.MISSING_VALUE);
								} else {
									addVariable(attribute.getName(), example.getValueAsString(attribute));
								}
							} else if (Ontology.ATTRIBUTE_VALUE_TYPE.isA(attribute.getValueType(), Ontology.DATE_TIME)) {
								Calendar cal = Calendar.getInstance();
								cal.setTime(new Date((long) example.getValue(attribute)));
								addVariable(attribute.getName(), cal);
							} else {
								addVariable(attribute.getName(), example.getValue(attribute));
							}
						} else {
							// nothing will be done later: no compliance to data
							// must be met
							if (attribute.isNominal()) {
								addVariable(attribute.getName(), UnknownValue.UNKNOWN_NOMINAL);
							} else {
								addVariable(attribute.getName(), Double.NaN);
							}
						}
					}
				}
			}
		} catch (ExpressionParserException e) {
			throw new GenerationException(e.getMessage());
		}
		return name2attributes;
	}

	/**
	 * Make the variable values from the example available to the parser.
	 * @param parser
	 * @param example
	 * @param name2attributes
	 */
	public void assignVariableValuesFromExample(Example example, Map<String, Attribute> name2attributes) {
		// assign variable values
		for (Map.Entry<String, Attribute> entry : name2attributes.entrySet()) {
			String variableName = entry.getKey();
			Attribute attribute = entry.getValue();
			double value = example.getValue(attribute);
			if (attribute.isNominal()) {
				if (Double.isNaN(value)) {
					setVarValue(variableName, UnknownValue.UNKNOWN_NOMINAL);
				} else {
					setVarValue(variableName, example.getValueAsString(attribute));
				}
			} else if (Ontology.ATTRIBUTE_VALUE_TYPE.isA(attribute.getValueType(), Ontology.DATE_TIME)) {
				if (Double.isNaN(value)) {
					setVarValue(variableName, UnknownValue.UNKNOWN_DATE);
				} else {
					Calendar cal = Calendar.getInstance();
					cal.setTime(new Date((long) value));
					setVarValue(variableName, cal);
				}
			} else {
				setVarValue(variableName, value);
			}
		}
	}

	public abstract void setVarValue(String variableName, Object value);

	public abstract void setImplicitMul(boolean b);

	/**
	 * Parses all lines of the AttributeConstruction file and returns a list
	 * containing all newly generated attributes.
	 */
	public List<Attribute> generateAll(LoggingHandler logging, ExampleSet exampleSet, InputStream in) throws IOException, GenerationException {
		this.addStandardConstants();
		LinkedList<Attribute> generatedAttributes = new LinkedList<Attribute>();
		Document document = null;
		try {
			document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(in);
		} catch (SAXException e1) {
			throw new IOException(e1.getMessage());
		} catch (ParserConfigurationException e1) {
			throw new IOException(e1.getMessage());
		}

		Element constructionsElement = document.getDocumentElement();
		if (!constructionsElement.getTagName().equals("constructions")) {
			throw new IOException("Outer tag of attribute constructions file must be <constructions>");
		}

		NodeList constructions = constructionsElement.getChildNodes();
		for (int i = 0; i < constructions.getLength(); i++) {
			Node node = constructions.item(i);
			if (node instanceof Element) {
				Element constructionTag = (Element) node;
				String tagName = constructionTag.getTagName();
				if (!tagName.equals("attribute"))
					throw new IOException("Only <attribute> tags are allowed for attribute description files, but found " + tagName);
				String attributeName = constructionTag.getAttribute("name");
				String attributeConstruction = constructionTag.getAttribute("construction");
				if (attributeName == null) {
					throw new IOException("<attribute> tag needs 'name' attribute.");
				}
				if (attributeConstruction == null) {
					throw new IOException("<attribute> tag needs 'construction' attribute.");
				}
				if (attributeConstruction.equals(attributeName)) {
					Attribute presentAttribute = exampleSet.getAttributes().get(attributeName);
					if (presentAttribute != null) {
						generatedAttributes.add(presentAttribute);
						continue;
					} else {
						throw new GenerationException("No such attribute: " + attributeName);
					}
				} else {
					generatedAttributes.add(this.addAttribute(exampleSet, attributeName, attributeConstruction));
				}
			}
		}
		return generatedAttributes;
	}

	public class ExpressionParserException extends Exception {

		private static final long serialVersionUID = 6824584915569094846L;

		public ExpressionParserException(String msg) {
			super(msg);
		}
	}

	/** 
	 * Registers a custom function that is described by a {@link Function}.
	 * 
	 * @param groupName the group name to include the function in.
	 * @param customFD custom function to include.
	 */
	public static void registerFunction(String groupName,  Function function){
		
		if(!FUNCTION_GROUPS.contains(groupName)){
			FUNCTION_GROUPS.add(groupName);
			FUNCTION_DESCRIPTIONS.put(groupName, new LinkedList<FunctionDescription>());
		}
		FUNCTION_DESCRIPTIONS.get(groupName).add(function.getFunctionDescription());
		CUSTOM_FUNCTIONS.add(function);
	}
	
	public static final List<Function> getCustomFunctions(){
		return CUSTOM_FUNCTIONS;
	}
}
