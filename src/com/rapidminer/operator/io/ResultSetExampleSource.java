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

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.Attributes;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.table.DataRowFactory;
import com.rapidminer.example.table.DataRowReader;
import com.rapidminer.example.table.ExampleTable;
import com.rapidminer.example.table.MemoryExampleTable;
import com.rapidminer.example.table.ResultSetDataRowReader;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.UserError;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeCategory;
import com.rapidminer.parameter.ParameterTypeString;
import com.rapidminer.tools.jdbc.DatabaseHandler;


/**
 * Abstract superclass for operators that provide access to an
 * {@link ExampleSet} via a {@link ResultSet}.
 * 
 * @author Ingo Mierswa
 */
public abstract class ResultSetExampleSource extends AbstractExampleSource {

	/** The parameter name for &quot;The (case sensitive) name of the label attribute&quot; */
	public static final String PARAMETER_LABEL_ATTRIBUTE = "label_attribute";

	/** The parameter name for &quot;The (case sensitive) name of the id attribute&quot; */
	public static final String PARAMETER_ID_ATTRIBUTE = "id_attribute";

	/** The parameter name for &quot;The (case sensitive) name of the weight attribute&quot; */
	public static final String PARAMETER_WEIGHT_ATTRIBUTE = "weight_attribute";

	/** The parameter name for &quot;Determines, how the data is represented internally.&quot; */
	public static final String PARAMETER_DATAMANAGEMENT = "datamanagement";
	
	
	public ResultSetExampleSource(OperatorDescription description) {
		super(description);
	}

	/** Returns a {@link ResultSet}. */
	public abstract ResultSet getResultSet() throws OperatorException;

	/** This method is invoked at the end of the data query process. Subclasses
	 *  might want to clean up things, e.g. close statements.
	 */
	public abstract void tearDown();

	/**
	 * Since the {@link ResultSet} does not provide information about possible
	 * values of nominal attributes, subclasses must set these by implementing
	 * this method.
	 * 
	 * @param attributeList
	 *            List of {@link Attribute}
	 */
	public abstract void setNominalValues(List<Attribute> attributeList, ResultSet resultSet, Attribute label) throws OperatorException;

	@Override
	public ExampleSet createExampleSet() throws OperatorException {
		int dataRowType = getParameterAsInt(PARAMETER_DATAMANAGEMENT);
		ResultSet resultSet = getResultSet();
		List<Attribute> attributeList = null;
		try {
			attributeList = DatabaseHandler.createAttributes(resultSet);
		} catch (SQLException e) {
			throw new UserError(this, e, 304, e.getMessage());
		}
		setNominalValues(attributeList, resultSet, find(attributeList, getParameterAsString(PARAMETER_LABEL_ATTRIBUTE)));
		DataRowReader reader = new ResultSetDataRowReader(new DataRowFactory(dataRowType, '.'), attributeList, resultSet);
		ExampleTable table = new MemoryExampleTable(attributeList, reader);
		
		// close statements etc.
		tearDown();
		
		return createExampleSet(table, this);
	}

	private static Attribute find(List attributeList, String name) throws OperatorException {
		if (name == null)
			return null;
		Iterator i = attributeList.iterator();
		while (i.hasNext()) {
			Attribute attribute = (Attribute) i.next();
			if (attribute.getName().equals(name))
				return attribute;
		}
		throw new UserError(null, 111, name);
	}

	public static ExampleSet createExampleSet(ExampleTable table, Operator operator) throws OperatorException {
		String labelName = operator.getParameterAsString(PARAMETER_LABEL_ATTRIBUTE);
		String weightName = operator.getParameterAsString(PARAMETER_WEIGHT_ATTRIBUTE);
		String idName = operator.getParameterAsString(PARAMETER_ID_ATTRIBUTE);

		// findAttribute might only be used here, because table is completly new and does not contain any double names!
		Attribute label = table.findAttribute(labelName);
		Attribute weight = table.findAttribute(weightName);
		Attribute id = table.findAttribute(idName);
		
		Map<Attribute, String> specialMap = new HashMap<Attribute, String>();
		if(label != null) {
			specialMap.put(label, Attributes.LABEL_NAME);
		}
		if(weight != null) {
			specialMap.put(weight, Attributes.WEIGHT_NAME);
		}
		if(id != null) {
			specialMap.put(id, Attributes.ID_NAME);
		}
		return table.createExampleSet(specialMap);
	}
	
	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();
		ParameterType type = new ParameterTypeString(PARAMETER_LABEL_ATTRIBUTE, "The (case sensitive) name of the label attribute");
		type.setExpert(false);
		types.add(type);
		types.add(new ParameterTypeString(PARAMETER_ID_ATTRIBUTE, "The (case sensitive) name of the id attribute"));
		types.add(new ParameterTypeString(PARAMETER_WEIGHT_ATTRIBUTE, "The (case sensitive) name of the weight attribute"));
		types.add(new ParameterTypeCategory(PARAMETER_DATAMANAGEMENT, "Determines, how the data is represented internally.", DataRowFactory.TYPE_NAMES, DataRowFactory.TYPE_DOUBLE_ARRAY));
		return types;
	}
}
