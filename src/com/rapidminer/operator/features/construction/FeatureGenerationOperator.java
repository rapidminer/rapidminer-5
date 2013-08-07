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
package com.rapidminer.operator.features.construction;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.UserError;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeBoolean;
import com.rapidminer.parameter.ParameterTypeFile;
import com.rapidminer.parameter.ParameterTypeList;
import com.rapidminer.parameter.ParameterTypeString;
import com.rapidminer.tools.expression.parser.AbstractExpressionParser;
import com.rapidminer.tools.expression.parser.ExpressionParserFactory;


/**
 * <p>This operator generates new user specified features. The new features are
 * specified by their function names (prefix notation) and their arguments using
 * the names of existing features.<br/> Legal function names include +, -, etc. and 
 * the functions
 * norm, sin, cos, tan, atan, exp, log, min, max, floor, ceil, round, sqrt, abs,
 * and pow. Constant values can be defined by &quot;const[value]()&quot; where
 * value is the desired value. Do not forget the empty round brackets. Example:
 * <code>+(a1, -(a2, a3))</code> will calculate the sum of the attribute
 * <code>a1</code> and the difference of the attributes <code>a2</code> and
 * <code>a3</code>. <br/> Features are generated in the following order:</p>
 * <ol>
 * <li>Features specified by the file referenced by the parameter &quot;filename&quot;
 * are generated</li>
 * <li>Features specified by the parameter list &quot;functions&quot; are generated</li>
 * <li>If &quot;keep_all&quot; is false, all of the old attributes are removed now</li>
 * </ol>
 * 
 * <p>
 * The list of supported functions include +, -, etc. and the functions
 * sin, cos, tan, atan, exp, log, min, max, floor, ceil, round, sqrt, abs, sgn, pow.
 * </p>
 * 
 * @see com.rapidminer.generator.FeatureGenerator
 * @author Simon Fischer, Ingo Mierswa
 */
public class FeatureGenerationOperator extends AbstractFeatureConstruction {

	/** The parameter name for &quot;Create the attributes listed in this file (written by an AttributeConstructionsWriter).&quot; */
	public static final String PARAMETER_FILENAME = "filename";

	/** The parameter name for &quot;List of functions to generate.&quot; */
	public static final String PARAMETER_FUNCTIONS = "functions";

	/** The parameter name for &quot;If set to true, all the original attributes are kept, otherwise they are removed from the example set.&quot; */
	public static final String PARAMETER_KEEP_ALL = "keep_all";

	public FeatureGenerationOperator(OperatorDescription description) {
		super(description);
	}

	@Override
	public ExampleSet apply(ExampleSet exampleSet) throws OperatorException {
		boolean keepAll = getParameterAsBoolean(PARAMETER_KEEP_ALL);
		List<Attribute> oldAttributes = new LinkedList<Attribute>();
		for (Attribute attribute : exampleSet.getAttributes()) {
			oldAttributes.add(attribute);
		}

		//AttributeParser parser = new AttributeParser();
		
		File file = getParameterAsFile(PARAMETER_FILENAME);
		if (file != null) {
			InputStream in = null;
			try {
				in = new FileInputStream(file);
				ExpressionParserFactory.getExpressionParser(true).generateAll(this, exampleSet, in);
			} catch (IOException e) {
				throw new UserError(this, e, 302, new Object[] { file, e.getMessage() });
			} finally {
				if (in != null) {
					try {
						in.close();
					} catch (IOException e) {
						getLogger().warning("Cannot close stream to file " + file);
					}
				}
			}
		}

		AbstractExpressionParser parser = ExpressionParserFactory.getExpressionParser(true);
		Iterator<String[]> j = getParameterList(PARAMETER_FUNCTIONS).iterator();
		while (j.hasNext()) {
			String[] nameFunctionPair = j.next();
			Attribute attribute = parser.addAttribute(exampleSet, nameFunctionPair[0], nameFunctionPair[1]);
			//Attribute attribute = parser.generateAttribute(this, , exampleSet.getExampleTable());
			if (attribute != null) {
				attribute.setName(nameFunctionPair[0]);
				exampleSet.getAttributes().addRegular(attribute);
			} else {
				getLogger().warning("Cannot generate attribute: " + nameFunctionPair[0] + " --> " + nameFunctionPair[1]);
			}
			checkForStop();
		}

		if (!keepAll) {
			for (Attribute oldAttribute : oldAttributes) {
				exampleSet.getAttributes().remove(oldAttribute);
			}
		}

		return exampleSet;
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();
		ParameterType type = new ParameterTypeFile(PARAMETER_FILENAME, "Create the attributes listed in this file (written by an AttributeConstructionsWriter).", "att", true);
		type.setExpert(false);
		types.add(type);
		type = new ParameterTypeList(PARAMETER_FUNCTIONS, "List of functions to generate.", 
				new ParameterTypeString("attribute_name", "The name of the generated attribtue."),
				new ParameterTypeString("function", "Function and arguments to use for generation."));
		type.setExpert(false);
		types.add(type);
		types.add(new ParameterTypeBoolean(PARAMETER_KEEP_ALL, "If set to true, all the original attributes are kept, otherwise they are removed from the example set.", false));
		return types;
	}

}
