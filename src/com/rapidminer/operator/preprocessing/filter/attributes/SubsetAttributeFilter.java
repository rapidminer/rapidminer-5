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
package com.rapidminer.operator.preprocessing.filter.attributes;

import java.util.LinkedList;
import java.util.List;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.set.ConditionCreationException;
import com.rapidminer.operator.UserError;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.metadata.AttributeMetaData;
import com.rapidminer.operator.ports.metadata.MetaDataInfo;
import com.rapidminer.parameter.ParameterHandler;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeAttributes;


/**
 * A filter condition for subsets of attributes.
 * 
 * @author Tobias Malbrecht
 */
public class SubsetAttributeFilter extends AbstractAttributeFilterCondition {

	public static final String PARAMETER_ATTRIBUTES = "attributes";

	private String attributeNames;

	@Override
	public void init(ParameterHandler operator) throws UserError, ConditionCreationException {
		attributeNames = operator.getParameterAsString(PARAMETER_ATTRIBUTES);
	}

	public MetaDataInfo isFilteredOutMetaData(AttributeMetaData attribute, ParameterHandler handler) throws ConditionCreationException {
		if ((attributeNames == null) || (attributeNames.length() == 0)) {
			return MetaDataInfo.YES;
		}
		boolean found = false;
		for (String attributeName : attributeNames.split("\\|")) {
			if (attribute.getName().equals(attributeName)) {
				found = true;
			}
		}
		return found ? MetaDataInfo.NO : MetaDataInfo.YES;
	}

	public ScanResult beforeScanCheck(Attribute attribute) throws UserError {
		if(attributeNames == null || attributeNames.length() == 0) {
			return ScanResult.REMOVE;
		}
		for (String attributeName : attributeNames.split("\\|")) {
			if (attribute.getName().equals(attributeName)) {
				return ScanResult.KEEP;
			}
		}
		return ScanResult.REMOVE;
	}

	@Override
	public List<ParameterType> getParameterTypes(ParameterHandler operator, final InputPort inPort, int...valueTypes) {
		List<ParameterType> types = new LinkedList<ParameterType>();
		ParameterType type = new ParameterTypeAttributes(PARAMETER_ATTRIBUTES, "The attribute which should be chosen.", inPort, valueTypes);
		type.setExpert(false);
		types.add(type);
		return types;
	}
}
