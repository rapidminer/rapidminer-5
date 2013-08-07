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
package com.rapidminer.operator.generator;

import java.util.Iterator;
import java.util.List;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.Attributes;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.table.DoubleArrayDataRow;
import com.rapidminer.example.table.MemoryExampleTable;
import com.rapidminer.generator.GenerationException;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.ProcessSetupError.Severity;
import com.rapidminer.operator.SimpleProcessSetupError;
import com.rapidminer.operator.UserError;
import com.rapidminer.operator.io.AbstractExampleSource;
import com.rapidminer.operator.ports.metadata.AttributeMetaData;
import com.rapidminer.operator.ports.metadata.ExampleSetMetaData;
import com.rapidminer.operator.ports.metadata.MetaData;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeExpression;
import com.rapidminer.parameter.ParameterTypeList;
import com.rapidminer.parameter.ParameterTypeString;
import com.rapidminer.parameter.ParameterTypeStringCategory;
import com.rapidminer.parameter.UndefinedParameterError;
import com.rapidminer.tools.expression.parser.AbstractExpressionParser;
import com.rapidminer.tools.expression.parser.ExpressionParserFactory;

/**
 * This operator produces an {@link ExampleSet} containing only
 * one example whose Attribute values are derived from a user specified list.
 * 
 * @author Sebastian Land
 */
public class UserSpecificationDataGenerator extends AbstractExampleSource {

    public static final String PARAMETER_VALUES = "attribute_values";
    public static final String PARAMETER_ATTRIBUTE_NAME = "attribute_name";
    public static final String PARAMETER_ATTRIBUTE_VALUE = "attribute_value";

    /** The parameter name for &quot;The name of the attribute of which the type should be changed.&quot; */
    public static final String PARAMETER_NAME = "name";

    /** The parameter name for &quot;The target type of the attribute (only changed if parameter change_attribute_type is true).&quot; */
    public static final String PARAMETER_TARGET_ROLE = "target_role";

    public static final String PARAMETER_ROLES = "set_additional_roles";

    private static final String REGULAR_NAME = "regular";

    private static final String[] TARGET_ROLES = new String[] { REGULAR_NAME, Attributes.ID_NAME, Attributes.LABEL_NAME, Attributes.PREDICTION_NAME, Attributes.CLUSTER_NAME, Attributes.WEIGHT_NAME, Attributes.BATCH_NAME };


    public UserSpecificationDataGenerator(OperatorDescription description) {
        super(description);

    }

    @Override
    public MetaData getGeneratedMetaData() throws OperatorException {
        ExampleSetMetaData emd = new ExampleSetMetaData();
        emd.setNumberOfExamples(1);
        emd.attributesAreKnown();

		AbstractExpressionParser parser = ExpressionParserFactory.getExpressionParser(true);

        try {
            Iterator<String[]> j = getParameterList(PARAMETER_VALUES).iterator();
            while (j.hasNext()) {
                String[] nameFunctionPair = j.next();
                String name = nameFunctionPair[0];
                String function = nameFunctionPair[1];
                parser.addAttributeMetaData(emd, name, function);
            }

            // now proceed with Attribute rules
            if (isParameterSet(PARAMETER_ROLES)) {
                List<String[]> list = getParameterList(PARAMETER_ROLES);
                for (String[] pairs: list) {
                    setRoleMetaData(emd, pairs[0], pairs[1]);
                }
            }
        } catch (UndefinedParameterError e) {}

        return emd;
    }

    private void setRoleMetaData(ExampleSetMetaData metaData, String name, String targetRole) {
        AttributeMetaData amd = metaData.getAttributeByName(name);
        if (amd != null) {
            if (targetRole != null) {
                if (REGULAR_NAME.equals(targetRole)) {
                    amd.setRegular();
                } else {
                    AttributeMetaData oldRole = metaData.getAttributeByRole(targetRole);
                    if (oldRole != null && oldRole != amd) {
                        addError(new SimpleProcessSetupError(Severity.WARNING, this.getPortOwner(), "already_contains_role", targetRole));
                        metaData.removeAttribute(oldRole);
                    }
                    amd.setRole(targetRole);
                }
            }
        }
    }

    @Override
    public ExampleSet createExampleSet() throws OperatorException {
		AbstractExpressionParser parser = ExpressionParserFactory.getExpressionParser(true);

        MemoryExampleTable table = new MemoryExampleTable();
        table.addDataRow(new DoubleArrayDataRow(new double[0]));

        ExampleSet exampleSet = table.createExampleSet();
        Iterator<String[]> j = getParameterList(PARAMETER_VALUES).iterator();
        while (j.hasNext()) {
            String[] nameFunctionPair = j.next();
            String name = nameFunctionPair[0];
            String function = nameFunctionPair[1];
            try {
                parser.addAttribute(exampleSet, name, function);
            } catch (GenerationException e) {
                throw new UserError(this, e, 108, e.getMessage());
            }

            checkForStop();
        }

        // now set roles
        if (isParameterSet(PARAMETER_ROLES)) {
            List<String[]> list = getParameterList(PARAMETER_ROLES);
            for (String[] pairs: list) {
                setRole(exampleSet, pairs[0], pairs[1]);
            }
        }
        return exampleSet;
    }

    private void setRole(ExampleSet exampleSet, String name, String newRole) throws UserError {
        Attribute attribute = exampleSet.getAttributes().get(name);

        if (attribute == null) {
            throw new UserError(this, 111, name);
        }

        exampleSet.getAttributes().remove(attribute);
        if ((newRole == null) || (newRole.trim().length() == 0))
            throw new UserError(this, 205, PARAMETER_TARGET_ROLE);
        if (newRole.equals(REGULAR_NAME)) {
            exampleSet.getAttributes().addRegular(attribute);
        } else {
            exampleSet.getAttributes().setSpecialAttribute(attribute, newRole);
        }
    }

    @Override
    public List<ParameterType> getParameterTypes() {
        List<ParameterType> types = super.getParameterTypes();
        types.add(new ParameterTypeList(PARAMETER_VALUES, "This parameter defines the attributes and their values in the single example returned.",
                new ParameterTypeString(PARAMETER_ATTRIBUTE_NAME, "This is the name of the generated attribute.", false),
                new ParameterTypeExpression(PARAMETER_ATTRIBUTE_VALUE, "An expression that is parsed to derive the value of this attribute."), false));

        types.add(new ParameterTypeList(PARAMETER_ROLES, "This parameter defines additional attribute role combinations.",
                new ParameterTypeString(PARAMETER_NAME, "The name of the attribute whose role should be changed.", false, false),
                new ParameterTypeStringCategory(PARAMETER_TARGET_ROLE, "The target role of the attribute (only changed if parameter change_attribute_type is true).", TARGET_ROLES, TARGET_ROLES[0]),
                false));
        return types;
    }

}
