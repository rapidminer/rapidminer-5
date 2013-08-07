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
package com.rapidminer.operator.io;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.UserError;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.OutputPort;
import com.rapidminer.operator.ports.metadata.ExampleSetMetaData;
import com.rapidminer.operator.ports.metadata.ExampleSetPassThroughRule;
import com.rapidminer.operator.ports.metadata.SetRelation;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeBoolean;
import com.rapidminer.parameter.ParameterTypeFile;
import com.rapidminer.parameter.UndefinedParameterError;
import com.rapidminer.tools.expression.parser.ExpressionParserFactory;


/**
 * Loads an attribute set from a file and constructs the desired features. If
 * keep_all is false, original attributes are deleted before the new ones are
 * created. This also means that a feature selection is performed if only a
 * subset of the original features was given in the file.
 * 
 * @author Ingo Mierswa
 */
public class AttributeConstructionsLoader extends Operator {

	private InputPort exampleSetInput = getInputPorts().createPort("example set", ExampleSet.class);
	private OutputPort exampleSetOutput = getOutputPorts().createPort("example set");

	/** The parameter name for &quot;Filename for the attribute constructions file.&quot; */
	public static final String PARAMETER_ATTRIBUTE_CONSTRUCTIONS_FILE = "attribute_constructions_file";

	/** The parameter name for &quot;If set to true, all the original attributes are kept, otherwise they are removed from the example set.&quot; */
	public static final String PARAMETER_KEEP_ALL = "keep_all";

	public AttributeConstructionsLoader(OperatorDescription description) {
		super(description);
		getTransformer().addRule(new ExampleSetPassThroughRule(exampleSetInput, exampleSetOutput, SetRelation.SUPERSET) {
			@Override
			public ExampleSetMetaData modifyExampleSet(ExampleSetMetaData metaData) throws UndefinedParameterError {
				metaData.clearRegular();
				return metaData;
			}
		});
	}

	/** Loads the attribute set from a file and constructs desired features. */
	@Override
	public void doWork() throws OperatorException {
		ExampleSet exampleSet = exampleSetInput.getData(ExampleSet.class);

		boolean keepAll = getParameterAsBoolean(PARAMETER_KEEP_ALL);
		List<Attribute> oldAttributes = new LinkedList<Attribute>();
		for (Attribute attribute : exampleSet.getAttributes()) {
			oldAttributes.add(attribute);
		}

		File file = getParameterAsFile(PARAMETER_ATTRIBUTE_CONSTRUCTIONS_FILE);
		List<Attribute> generatedAttributes = new LinkedList<Attribute>();
		if (file != null) {
			InputStream in = null;
			try {
				in = new FileInputStream(file);
				generatedAttributes = ExpressionParserFactory.getExpressionParser(true).generateAll(this, exampleSet, in);
			} catch (java.io.IOException e) {
				throw new UserError(this, e, 302, new Object[] { file.getName(), e.getMessage() });
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

		if (!keepAll) {
			for (Attribute oldAttribute : oldAttributes) {
				if (!generatedAttributes.contains(oldAttribute))
					exampleSet.getAttributes().remove(oldAttribute);
			}
		}

		exampleSetOutput.deliver(exampleSet);
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();
		types.add(new ParameterTypeFile(PARAMETER_ATTRIBUTE_CONSTRUCTIONS_FILE, "Filename for the attribute constructions file.", "att", false, false));
		types.add(new ParameterTypeBoolean(PARAMETER_KEEP_ALL, "If set to true, all the original attributes are kept, otherwise they are removed from the example set.", false, false));
		return types;
	}
}
