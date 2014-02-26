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
package com.rapidminer.tools.jep.function;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

import org.nfunk.jep.JEP;
import org.nfunk.jep.ParseException;
import org.nfunk.jep.SymbolTable;
import org.nfunk.jep.Variable;
import org.nfunk.jep.function.PostfixMathCommand;
import org.nfunk.jep.function.PostfixMathCommandI;
import org.nfunk.jep.type.Complex;

import com.rapidminer.Process;
import com.rapidminer.operator.preprocessing.filter.ChangeAttributeName;
import com.rapidminer.tools.expression.parser.AbstractExpressionParser;
import com.rapidminer.tools.expression.parser.Function;
import com.rapidminer.tools.expression.parser.JEPFunctionException;
import com.rapidminer.tools.jep.function.expressions.Average;
import com.rapidminer.tools.jep.function.expressions.BitwiseAnd;
import com.rapidminer.tools.jep.function.expressions.BitwiseNot;
import com.rapidminer.tools.jep.function.expressions.BitwiseOr;
import com.rapidminer.tools.jep.function.expressions.BitwiseXor;
import com.rapidminer.tools.jep.function.expressions.Constant;
import com.rapidminer.tools.jep.function.expressions.LogarithmDualis;
import com.rapidminer.tools.jep.function.expressions.MacroValue;
import com.rapidminer.tools.jep.function.expressions.Maximum;
import com.rapidminer.tools.jep.function.expressions.Minimum;
import com.rapidminer.tools.jep.function.expressions.Missing;
import com.rapidminer.tools.jep.function.expressions.ParameterValue;
import com.rapidminer.tools.jep.function.expressions.Random;
import com.rapidminer.tools.jep.function.expressions.Signum;
import com.rapidminer.tools.jep.function.expressions.date.Date2String;
import com.rapidminer.tools.jep.function.expressions.date.Date2StringCustom;
import com.rapidminer.tools.jep.function.expressions.date.Date2StringWithLocale;
import com.rapidminer.tools.jep.function.expressions.date.DateAdd;
import com.rapidminer.tools.jep.function.expressions.date.DateAfter;
import com.rapidminer.tools.jep.function.expressions.date.DateBefore;
import com.rapidminer.tools.jep.function.expressions.date.DateCreate;
import com.rapidminer.tools.jep.function.expressions.date.DateDiff;
import com.rapidminer.tools.jep.function.expressions.date.DateGet;
import com.rapidminer.tools.jep.function.expressions.date.DateParse;
import com.rapidminer.tools.jep.function.expressions.date.DateParseCustom;
import com.rapidminer.tools.jep.function.expressions.date.DateParseWithLocale;
import com.rapidminer.tools.jep.function.expressions.date.DateSet;
import com.rapidminer.tools.jep.function.expressions.number.Str;
import com.rapidminer.tools.jep.function.expressions.text.CharAt;
import com.rapidminer.tools.jep.function.expressions.text.Compare;
import com.rapidminer.tools.jep.function.expressions.text.Concat;
import com.rapidminer.tools.jep.function.expressions.text.Contains;
import com.rapidminer.tools.jep.function.expressions.text.EndsWith;
import com.rapidminer.tools.jep.function.expressions.text.Equals;
import com.rapidminer.tools.jep.function.expressions.text.EscapeHTML;
import com.rapidminer.tools.jep.function.expressions.text.Finds;
import com.rapidminer.tools.jep.function.expressions.text.IndexOf;
import com.rapidminer.tools.jep.function.expressions.text.Length;
import com.rapidminer.tools.jep.function.expressions.text.LowerCase;
import com.rapidminer.tools.jep.function.expressions.text.Matches;
import com.rapidminer.tools.jep.function.expressions.text.ParseNumber;
import com.rapidminer.tools.jep.function.expressions.text.Prefix;
import com.rapidminer.tools.jep.function.expressions.text.Replace;
import com.rapidminer.tools.jep.function.expressions.text.ReplaceRegex;
import com.rapidminer.tools.jep.function.expressions.text.StartsWith;
import com.rapidminer.tools.jep.function.expressions.text.Substring;
import com.rapidminer.tools.jep.function.expressions.text.Suffix;
import com.rapidminer.tools.jep.function.expressions.text.Trim;
import com.rapidminer.tools.jep.function.expressions.text.UpperCase;

/**
 * <p>
 * This class can be used as expression parser in order to generate new
 * attributes. The parser constructs new attributes from the attributes of the
 * input example set.
 * </p>
 * 
 * <p>
 * The following <em>operators</em> are supported:
 * <ul>
 * <li>Addition: +</li>
 * <li>Subtraction: -</li>
 * <li>Multiplication: *</li>
 * <li>Division: /</li>
 * <li>Power: ^</li>
 * <li>Modulus: %</li>
 * <li>Less Than: &lt;</li>
 * <li>Greater Than: &gt;</li>
 * <li>Less or Equal: &lt;=</li>
 * <li>More or Equal: &gt;=</li>
 * <li>Equal: ==</li>
 * <li>Not Equal: !=</li>
 * <li>Boolean Not: !</li>
 * <li>Boolean And: &&</li>
 * <li>Boolean Or: ||</li>
 * </ul>
 * </p>
 * 
 * <p>
 * The following <em>log and exponential functions</em> are supported:
 * <ul>
 * <li>Natural Logarithm: ln(x)</li>
 * <li>Logarithm Base 10: log(x)</li>
 * <li>Logarithm Dualis (Base 2): ld(x)</li>
 * <li>Exponential (e^x): exp(x)</li>
 * <li>Power: pow(x,y)</li>
 * </ul>
 * </p>
 * 
 * <p>
 * The following <em>trigonometric functions</em> are supported:
 * <ul>
 * <li>Sine: sin(x)</li>
 * <li>Cosine: cos(x)</li>
 * <li>Tangent: tan(x)</li>
 * <li>Arc Sine: asin(x)</li>
 * <li>Arc Cosine: acos(x)</li>
 * <li>Arc Tangent: atan(x)</li>
 * <li>Arc Tangent (with 2 parameters): atan2(x,y)</li>
 * <li>Hyperbolic Sine: sinh(x)</li>
 * <li>Hyperbolic Cosine: cosh(x)</li>
 * <li>Hyperbolic Tangent: tanh(x)</li>
 * <li>Inverse Hyperbolic Sine: asinh(x)</li></li>
 * <li>Inverse Hyperbolic Cosine: acosh(x)</li></li>
 * <li>Inverse Hyperbolic Tangent: atanh(x)</li></li>
 * </ul>
 * </p>
 * 
 * <p>
 * The following <em>statistical functions</em> are supported:
 * <ul>
 * <li>Round: round(x)</li>
 * <li>Round to p decimals: round(x,p)</li>
 * <li>Floor: floor(x)</li>
 * <li>Ceiling: ceil(x)</li>
 * </ul>
 * </p>
 * 
 * <p>
 * The following <em>aggregation functions</em> are supported:
 * <ul>
 * <li>Average: avg(x,y,z...)</li>
 * <li>Minimum: min(x,y,z...)</li>
 * <li>Maximum: max(x,y,z...)</li>
 * </ul>
 * </p>
 * 
 * <p>
 * The following <em>text functions</em> are supported:
 * <ul>
 * <li>Number to string: str(x)</li>
 * <li>String to number: parse(text)</li>
 * <li>Substring: cut(text, start, length)</li>
 * <li>Concatenation (also possible by &quot+&quot;): concat(text1, text2,
 * text3...)</li>
 * <li>Replace: replace(text, what, by)</li>
 * <li>Replace All: replaceAll(text, what, by)</li>
 * <li>To lower case: lower(text)</li>
 * <li>To upper case: upper(text)</li>
 * <li>First position of string in text: index(text, string)</li>
 * <li>Length: length(text)</li>
 * <li>Character at position pos in text: char(text, pos)</li>
 * <li>Compare: compare(text1, text2)</li>
 * <li>Contains string in text: contains(text, string)</li>
 * <li>Equals: equals(text1, text2)</li>
 * <li>Starts with string: starts(text, string)</li>
 * <li>Ends with string: ends(text, string)</li>
 * <li>Matches with regular expression exp: matches(text, exp)</li>
 * <li>Suffix of length: suffix(text, length)</li>
 * <li>Prefix of length: prefix(text, length)</li>
 * <li>Trim (remove leading and trailing whitespace): trim(text)</li>
 * </ul>
 * </p>
 * 
 * <p>
 * The following <em>date functions</em> are supported:
 * <ul>
 * <li>Parse date: date_parse(x)</li>
 * <li>Parse date using locale: date_parse_loc(x, code)</li>
 * <li>Parse date using custom format: date_parse_custom(x, format, code)</li>
 * <li>Date before: date_before(x, y)</li>
 * <li>Date after: date_after(x, y)</li>
 * <li>Date to string: date_str(x)</li>
 * <li>Date to string using locale: date_str_loc(x, code)</li>
 * <li>Date to string with custom pattern: date_str_custom(x, pattern, code)</li>
 * <li>Current date: date_now()</li>
 * <li>Date difference: date_diff(x, y)</li>
 * <li>Date add: date_add(x, y, unit)</li>
 * <li>Date set: date_set(x, y, unit)</li>
 * <li>Date get: date_get(x, unit)</li>
 * </ul>
 * 
 * <p>
 * The following <em>process related functions</em> are supported:
 * <ul>
 * <li>Retrieving a parameter value: param("operator", "parameter")</li>
 * <li>Retrieving a macro value: macro("macro", "default Value")</li>
 * </ul>
 * </p>
 * 
 * <p>
 * The following <em>miscellaneous functions</em> are supported:
 * <ul>
 * <li>If-Then-Else: if(cond,true-evaluation, false-evaluation)</li>
 * <li>Absolute: abs(x)</li>
 * <li>Constant: const(x)</li>
 * <li>Square Root: sqrt(x)</li>
 * <li>Signum (delivers the sign of a number): sgn(x)</li>
 * <li>Random Number (between 0 and 1): rand()</li>
 * <li>Modulus (x % y): mod(x,y)</li>
 * <li>Sum of k Numbers: sum(x,y,z...)</li>
 * <li>Binomial Coefficients: binom(n, i)</li>
 * <li>Check for Missing: missing(x)</li>
 * <li>Bitwise OR: bit_or(x, y)</li>
 * <li>Bitwise AND: bit_and(x, y)</li>
 * <li>Bitwise XOR: bit_xor(x, y)</li>
 * <li>Bitwise NOT: bit_not(x)</li>
 * </ul>
 * </p>
 * 
 * 
 * <p>
 * Beside those operators and functions, this operator also supports the
 * constants pi and e if this is indicated by the corresponding parameter
 * (default: true). You can also use strings in formulas (for example in a
 * conditioned if-formula) but the string values have to be enclosed in double
 * quotes.
 * </p>
 * 
 * <p>
 * Please note that there are some restrictions for the attribute names in order
 * to let this operator work properly:
 * <ul>
 * <li>If the standard constants are usable, attribute names with names like
 * &quot;e&quot; or &quot;pi&quot; are not allowed.</li>
 * <li>Attribute names with function or operator names are also not allowed.</li>
 * <li>Attribute names containing parentheses are not allowed.</li>
 * </ul>
 * If these conditions are not fulfilled, the names must be changed beforehand,
 * for example with the {@link ChangeAttributeName} operator.
 * </p>
 * 
 * <p>
 * <br/>
 * <em>Examples:</em><br/>
 * a1+sin(a2*a3)<br/>
 * if (att1>5, att2*att3, -abs(att1))<br/>
 * </p>
 * 
 * @author Ingo Mierswa
 */
public class ExpressionParser extends AbstractExpressionParser {

	private JEP parser;
	
	/** Static map to remember already created {@link PostfixMathCommand}s. This prevents from creating new ones every time the JEP is instanciated. */
	private static Map<String, PostfixMathCommand> REGISTERED_CUSTOM_FUNCTIONS = new HashMap<String, PostfixMathCommand>();

	private ExpressionParser(boolean useStandardConstants) {
		this(useStandardConstants, null);
	}

	/**
	 * This constructor allows additional functions if called within a process.
	 */
	private ExpressionParser(boolean useStandardConstants, Process process) {
		initParser(useStandardConstants, process);

	}

	public static AbstractExpressionParser getExpressionParser(boolean useStandardConstants) {
		return new ExpressionParser(useStandardConstants);
	}

	public static AbstractExpressionParser getExpressionParser(boolean useStandardConstants, Process process) {
		return new ExpressionParser(useStandardConstants, process);
	}

	@Override
	public void setAllowUndeclared(boolean value) {
		getParser().setAllowUndeclared(value);
	}

	@Override
	protected void addCustomConstants() {
		addConstant("true", Boolean.valueOf(true));
		addConstant("false", Boolean.valueOf(false));
		super.addCustomConstants();
	}

	@Override
	protected void addCustomFunctions() {

		addFunction("const", new Constant());

		addFunction("str", new Str());
		addFunction("avg", new Average());
		addFunction("min", new Minimum());
		addFunction("max", new Maximum());
		addFunction("ld", new LogarithmDualis());
		addFunction("sgn", new Signum());
		addFunction("missing", new Missing());
		addFunction("bit_or", new BitwiseOr());
		addFunction("bit_and", new BitwiseAnd());
		addFunction("bit_xor", new BitwiseXor());
		addFunction("bit_not", new BitwiseNot());

		// text functions
		addFunction("parse", new ParseNumber());
		addFunction("cut", new Substring());
		addFunction("concat", new Concat());
		addFunction("replace", new Replace());
		addFunction("replaceAll", new ReplaceRegex());
		addFunction("lower", new LowerCase());
		addFunction("upper", new UpperCase());
		addFunction("index", new IndexOf());
		addFunction("length", new Length());
		addFunction("char", new CharAt());
		addFunction("compare", new Compare());
		addFunction("equals", new Equals());
		addFunction("contains", new Contains());
		addFunction("starts", new StartsWith());
		addFunction("ends", new EndsWith());
		addFunction("matches", new Matches());
		addFunction("finds", new Finds());
		addFunction("prefix", new Prefix());
		addFunction("suffix", new Suffix());
		addFunction("trim", new Trim());
		addFunction("escape_html", new EscapeHTML());

		// date functions
		addFunction("date_parse", new DateParse());
		addFunction("date_parse_loc", new DateParseWithLocale());
		addFunction("date_parse_custom", new DateParseCustom());
		addFunction("date_before", new DateBefore());
		addFunction("date_after", new DateAfter());
		addFunction("date_str", new Date2String());
		addFunction("date_str_loc", new Date2StringWithLocale());
		addFunction("date_str_custom", new Date2StringCustom());
		addFunction("date_now", new DateCreate());
		addFunction("date_diff", new DateDiff());
		addFunction("date_add", new DateAdd());
		addFunction("date_set", new DateSet());
		addFunction("date_get", new DateGet());
		
		for (final Function function : getCustomFunctions()) {
			String functionName = function.getFunctionName();
			PostfixMathCommand postfixMathCommand = REGISTERED_CUSTOM_FUNCTIONS.get(functionName);
			
			// if function has not yet been created, create and save it now
			if(postfixMathCommand == null) {
				postfixMathCommand = new PostfixMathCommand() {

					{
						numberOfParameters = function.getFunctionDescription().getNumberOfArguments();
					}

					@Override
					public void run(Stack stack) throws ParseException {
						int numParams = numberOfParameters == -1 ? curNumberOfParameters : numberOfParameters;
						ArrayList<Object> arguments = new ArrayList<Object>();
						for (int i = 0; i < numParams; i++) {
							arguments.add(stack.pop());
						}
						Object result;
						try {
							result = function.compute(arguments.toArray());
						} catch (JEPFunctionException e) {
							throw new ParseException(e.getMessage());
						}
						stack.push(result);
					}
				};
				REGISTERED_CUSTOM_FUNCTIONS.put(functionName, postfixMathCommand);
			}
			addFunction(functionName, postfixMathCommand);
		}
	}
	
	public boolean hasError() {
		return getParser().hasError();
	}

	@Override
	public void parseExpression(String expression) throws ExpressionParserException {
		getParser().parseExpression(expression);
		if (hasError()) {
			throw new ExpressionParserException(getErrorInfo());
		}

	}

	@Override
	public String getErrorInfo() {
		if (getParser().hasError()) {
			return getParser().getErrorInfo();
		}
		return "";
	}

	@Override
	public void addVariable(String name, Object object) {
		getParser().addVariable(name, object);
	}

	@Override
	public Object getValueAsObject() {
		return getParser().getValueAsObject();
	}

	public void addConstant(String constantName, Object value) {
		getParser().addConstant(constantName, value);
	}

	@Override
	public void initParser(boolean useStandardConstants) {
		initParser(useStandardConstants, null);
	}

	@Override
	public void initParser(boolean useStandardConstants, Process process) {
		parser = new JEP();
		parser.addStandardFunctions();
		if (useStandardConstants)
			parser.addStandardConstants();

		addCustomFunctions();
		addCustomConstants();

		setAllowUndeclared(false);
		setImplicitMul(false);

		if (process != null) {
			parser.addFunction("param", new ParameterValue(process));
			parser.addFunction("macro", new MacroValue(process));
			parser.removeFunction("rand");
			parser.addFunction("rand", new Random(process));
		}
	}

	@Override
	public void setVarValue(String variableName, Object value) {
		getParser().setVarValue(variableName, value);

	}

	@Override
	public void setImplicitMul(boolean b) {
		getParser().setImplicitMul(b);

	}

	@Override
	public Collection getSymbolTableValues() {
		SymbolTable symbolTable = parser.getSymbolTable();
		return symbolTable.values();
	}

	private JEP getParser() {
		return parser;
	}

	@Override
	public void addStandardConstants() {
		getParser().addStandardConstants();

	}

	@Override
	public boolean isComplex(Object result) {
		if (result instanceof Complex) {
			return true;
		}
		return false;
	}

	@Override
	public String getVariableName(Object variableObj) {
		Variable variable = (Variable) variableObj;
		return variable.getName();
	}

	@Override
	public boolean isConstant(Object variableObj) {
		Variable variable = (Variable) variableObj;
		return variable.isConstant();
	}

	@Override
	public double getDoubleValueofComplex(Object result) {
		Complex cmplx = (Complex) result;
		return cmplx.doubleValue();
	}

	@Override
	public void addFunction(String functionName, Object value) {
		getParser().addFunction(functionName, (PostfixMathCommandI) value);
	}

}
