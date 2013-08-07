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
package com.rapidminer.operator;

import java.util.Iterator;
import java.util.List;

import com.rapidminer.generator.GenerationException;
import com.rapidminer.operator.ports.DummyPortPairExtender;
import com.rapidminer.operator.ports.PortPairExtender;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeBoolean;
import com.rapidminer.parameter.ParameterTypeExpression;
import com.rapidminer.parameter.ParameterTypeList;
import com.rapidminer.parameter.ParameterTypeString;
import com.rapidminer.tools.expression.parser.AbstractExpressionParser;
import com.rapidminer.tools.expression.parser.ExpressionParserFactory;

/**
 * <p>This operator constructs new macros from expressions which might also 
 * use already existing macros. The names of the new macros and their construction 
 * description are defined in the parameter list &quot;functions&quot;.</p>
 * 
 * <p>The following <em>operators</em> are supported:
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
 * <li>Boolean And: two ampers and</li>
 * <li>Boolean Or: ||</li>
 * </ul>
 * </p>
 * 
 * <p>The following <em>log and exponential functions</em> are supported:
 * <ul>
 * <li>Natural Logarithm: ln(x)</li>
 * <li>Logarithm Base 10: log(x)</li>
 * <li>Logarithm Dualis (Base 2): ld(x)</li>
 * <li>Exponential (e^x): exp(x)</li>
 * <li>Power: pow(x,y)</li>
 * </ul>
 * </p>
 * 
 * <p>The following <em>trigonometric functions</em> are supported:
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
 * <li>Inverse Hyperbolic Sine: asinh(x)</li>
 * <li>Inverse Hyperbolic Cosine: acosh(x)</li>
 * <li>Inverse Hyperbolic Tangent: atanh(x)</li>
 * </ul>
 * </p>
 * 
 * <p>The following <em>statistical functions</em> are supported:
 * <ul>
 * <li>Round: round(x)</li>
 * <li>Round to p decimals: round(x,p)</li>
 * <li>Floor: floor(x)</li>
 * <li>Ceiling: ceil(x)</li>
 * </ul>
 * </p>
 *
 * <p>The following <em>miscellaneous functions</em> are supported:
 * <ul>
 * <li>Average: avg(x,y,z...)</li>
 * <li>Minimum: min(x,y,z...)</li>
 * <li>Maximum: max(x,y,z...)</li>
 * </ul>
 * </p>
 * 
 * <p>The following <em>miscellaneous functions</em> are supported:
 * <ul>
 * <li>If-Then-Else: if(cond,true-evaluation, false-evaluation)</li>
 * <li>Absolute: abs(x)</li>
 * <li>Square Root: sqrt(x)</li>
 * <li>Signum (delivers the sign of a number): sgn(x)</li>
 * <li>Random Number (between 0 and 1): rand()</li>
 * <li>Modulus (x % y): mod(x,y)</li>
 * <li>Sum of k Numbers: sum(x,y,z...)</li>
 * <li>Binomial Coefficients: binom(n, i)</li>
 * <li>Number to String: str(x)</li>
 * </ul>
 * </p> 
 * 
 * <p>Beside those operators and functions, this operator also supports the constants
 * pi and e if this is indicated by the corresponding parameter (default: true). You can
 * also use strings in formulas (for example in a conditioned if-formula) but the string
 * values have to be enclosed in double quotes.</p>
 *  
 * <p>Please note that there are some restrictions for the usage of other macros. 
 * The values of used macros have to fulfill the following in order to let this operator
 * work properly:
 * <ul>
 * <li>If the standard constants are usable, macro values with names like &quot;e&quot; or 
 * &quot;pi&quot; are not allowed.</li>
 * <li>Macro values with function or operator names are also not allowed.</li>
 * <li>Macro values containing parentheses are not allowed.</li>
 * </ul>
 * </p>
 * 
 * <p><br/><em>Examples:</em><br/>
 * 17+sin(%{macro1}*%{macro2})<br/>
 * if (%macro1}>5, %{macro2}*%{macro3}, -abs(%{macro4}))<br/>
 * </p>
 * 
 * @author Ingo Mierswa
 */
public class MacroConstructionOperator extends Operator {

	/** The parameter name for &quot;List of functions to generate.&quot; */
	public static final String PARAMETER_FUNCTIONS = "function_descriptions";

	/** Indicates if standard constants like e or pi should be available. */
	public static final String PARAMETER_USE_STANDARD_CONSTANTS = "use_standard_constants";

	private PortPairExtender dummyPorts = new DummyPortPairExtender("through", getInputPorts(), getOutputPorts());

	public MacroConstructionOperator(OperatorDescription description) {
		super(description);

		dummyPorts.start();

		getTransformer().addRule(dummyPorts.makePassThroughRule());
	}

	@Override
	public void doWork() throws OperatorException {		
		AbstractExpressionParser parser = ExpressionParserFactory.getExpressionParser(getParameterAsBoolean(PARAMETER_USE_STANDARD_CONSTANTS), getProcess());
		Iterator<String[]> j = getParameterList(PARAMETER_FUNCTIONS).iterator();
		while (j.hasNext()) {
			String[] nameFunctionPair = j.next();
			String name = nameFunctionPair[0];
			String function = nameFunctionPair[1];

			try {
				parser.addMacro(getProcess().getMacroHandler(), name, function);
			} catch (GenerationException e) {
				throw new UserError(this, 108, e.getMessage());
			}
		}

		dummyPorts.passDataThrough();
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();

		ParameterType type = new ParameterTypeList(PARAMETER_FUNCTIONS, "The list of macro names together with the expressions which define the new macros", 
				new ParameterTypeString("macro_name", "The name of the constructed macro."),
				new ParameterTypeExpression("functions_expressions", "The expressions which define the new macros.", getInputPorts().getPortByIndex(0), false));
		type.setExpert(false);
		types.add(type);

		types.add(new ParameterTypeBoolean(PARAMETER_USE_STANDARD_CONSTANTS, "Indicates if standard constants like e or pi should be available.", true));

		return types;
	}

}
