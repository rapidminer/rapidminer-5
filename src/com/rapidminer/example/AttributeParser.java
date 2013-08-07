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
package com.rapidminer.example;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Stack;

import com.rapidminer.example.table.ExampleTable;
import com.rapidminer.generator.ConstantGenerator;
import com.rapidminer.generator.FeatureGenerator;
import com.rapidminer.generator.GenerationException;
import com.rapidminer.tools.LoggingHandler;
import com.rapidminer.tools.expression.parser.AbstractExpressionParser;
import com.rapidminer.tools.expression.parser.ExpressionParserFactory;


/**
 * Parses a file containing construction descriptions and adds the new
 * attributes to the example set.
 * 
 * @author Ingo Mierswa
 * @deprecated Use {@link ExpressionParser}
 */
@Deprecated
public class AttributeParser {

	private static class Construction {

		String function;
		Construction[] arguments;

		public Construction(String function) {
			this.function = function;
		}

		public Construction(String function, Construction[] arguments) {
			this.function = function;
			this.arguments = arguments;
		}

		public int getNumberOfArguments() {
			if (arguments == null) {
				return 0;
			} else {
				return arguments.length;
			}
		}

		public String getFunction() {
			return this.function;
		}

		@Override
		public String toString() {
			if (arguments == null) {
				return this.function;
			} else {
				StringBuffer result = new StringBuffer(this.function + "(");
				boolean first = true;
				for (Construction construction : arguments) {
					if (!first)
						result.append(", ");
					result.append(construction.toString());
					first = false;
				}
				result.append(")");
				return result.toString();
			}
		}
	}


	/** The example table to which the attributes should be added. */
	//private ExampleTable exampleTable;

	public AttributeParser() {}

	/** Parses all lines. 
	 * @deprecated Use {@link ExpressionParser#generateAll(LoggingHandler, ExampleSet, InputStream)} */
	@Deprecated
	public static void generateAll(LoggingHandler logging, ExampleSet exampleSet, InputStream in) throws IOException, GenerationException {
		AbstractExpressionParser expressionParser = ExpressionParserFactory.getExpressionParser(true);
		expressionParser.generateAll(logging, exampleSet, in);
	}

	public Attribute generateAttribute(LoggingHandler logging, String constructionDescription, ExampleTable table) throws GenerationException {
		Queue<Construction> toConstruct = new LinkedList<Construction>();
		parseAttributes(constructionDescription, toConstruct);
		return generate(logging, toConstruct, table);
	}


	// ===========================================================================

	private static int getClosingBracketIndex(String string, int startIndex) throws GenerationException {
		int openCount = 1;
		while (true) {
			int nextOpen = string.indexOf("(", startIndex + 1);
			int nextClosing = string.indexOf(")", startIndex + 1);
			if (nextClosing == -1)
				throw new GenerationException("Malformed attribute description: mismatched parantheses");
			if ((nextOpen != -1) && (nextOpen < nextClosing)) {
				openCount++;
				startIndex = nextOpen;
			} else {
				openCount--;
				startIndex = nextClosing;
			}
			if (openCount == 0) {
				return nextClosing;
			}
		}
	}

	/** Recursively parses the string starting at the current position. */
	private List<Construction> parseAttributes(String constructionString, Queue<Construction> toConstruct) throws GenerationException {
		int start = 0;
		List<Construction> constructedList = new LinkedList<Construction>();
		while (start < constructionString.length()) {
			int leftBr = constructionString.indexOf("(", start);
			int comma = constructionString.indexOf(",", start);
			if ((comma == -1) && (leftBr == -1)) { // no comma and left bracket --> simple the attribute or a constant
				int end = constructionString.length();
				String name = constructionString.substring(start, end).trim();
				if (name.startsWith(ConstantGenerator.FUNCTION_NAME)) {
					throw new GenerationException("The function name '" + ConstantGenerator.FUNCTION_NAME + "' must be used with empty arguments, for example 'const[5]()'!");
				} else {
					toConstruct.add(new Construction(name));
					constructedList.add(toConstruct.peek());
					start = constructionString.length();
				}
			} else if ((leftBr == -1) || ((comma < leftBr) && (comma != -1))) { // inner part with at least two arguments, left part
				int end = comma;
				String name = constructionString.substring(start, end).trim();
				if (name.startsWith(ConstantGenerator.FUNCTION_NAME)) {
					throw new GenerationException("The function name '" + ConstantGenerator.FUNCTION_NAME + "' must be used with empty arguments, for example 'const[5]()'!");
				} else {
					toConstruct.add(new Construction(name));
					constructedList.add(toConstruct.peek());
					start = end + 1;
				}
			} else { // right part 
				int rightBr = getClosingBracketIndex(constructionString, leftBr);
				String functionName = constructionString.substring(start, leftBr).trim();

				List<Construction> argumentList = parseAttributes(constructionString.substring(leftBr + 1, rightBr).trim(), toConstruct);
				Construction[] argumentDescriptions = new Construction[argumentList.size()];
				for (int i = 0; i < argumentDescriptions.length; i++) {
					argumentDescriptions[i] = argumentList.get(i);
				}

				Construction generated = new Construction(functionName, argumentDescriptions); 
				toConstruct.add(generated);
				constructedList.add(generated);

				start = constructionString.indexOf(",", rightBr) + 1;
				if (start <= 0)
					start = constructionString.length();
			}
		}
		return constructedList;
	}

	private Attribute findInTable(String constructionDescription, ExampleTable table) {
		for (int i = 0; i < table.getNumberOfAttributes(); i++) {
			Attribute a = table.getAttribute(i);
			if ((a != null) && (a.getConstruction().equals(constructionDescription)))
				return a;
		}
		return null;
	}

	/** Generates the new attribute described by the given stack. */
	private Attribute generate(LoggingHandler logging, Queue<Construction> toConstruct, ExampleTable table) throws GenerationException {
		// construct all attributes from stack
		List<Attribute> allGeneratedAttributes = new LinkedList<Attribute>();
		Attribute currentAttribute = null;
		Stack<Attribute> resultStack = new Stack<Attribute>();
		while (toConstruct.size() > 0) {
			Construction construction = toConstruct.remove();

			if (construction.getNumberOfArguments() == 0) { // no arguments --> existing attribute or non-argument function
				FeatureGenerator generator = FeatureGenerator.createGeneratorForFunction(construction.getFunction());
				if (generator == null) {
					// try to find attribute
					Attribute attribute = findInTable(construction.getFunction(), table);
					if (attribute == null) {
						throw new GenerationException("No such attribute: " + construction.getFunction());
					} else {
						resultStack.push(attribute);
					}
				} else {
					List<FeatureGenerator> generatorList = new LinkedList<FeatureGenerator>();
					generatorList.add(generator);
					List<Attribute> currentResultList = FeatureGenerator.generateAll(table, generatorList);
					currentAttribute = currentResultList.get(0);
					resultStack.push(currentAttribute);

					if (toConstruct.size() > 0) {
						allGeneratedAttributes.add(currentAttribute);
					}
				}
			} else {
				int numberOfArguments = construction.getNumberOfArguments();
				Attribute[] inputAttributes = new Attribute[numberOfArguments];
				for (int i = 0; i < numberOfArguments; i++) {
					inputAttributes[inputAttributes.length - 1 - i] = resultStack.pop();
				}

				FeatureGenerator generator = FeatureGenerator.createGeneratorForFunction(construction.getFunction());
				generator.setArguments(inputAttributes);
				List<FeatureGenerator> generatorList = new LinkedList<FeatureGenerator>();
				generatorList.add(generator);
				List<Attribute> currentResultList = FeatureGenerator.generateAll(table, generatorList);
				currentAttribute = currentResultList.get(0);
				resultStack.push(currentAttribute);

				if (toConstruct.size() > 0) {
					allGeneratedAttributes.add(currentAttribute);
				}
			}
		}

		// delete intermediate results
		for (Attribute attribute : allGeneratedAttributes) {
			table.removeAttribute(attribute);
		}

		return currentAttribute;
	}
}
