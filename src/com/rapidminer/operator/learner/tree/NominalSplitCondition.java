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
package com.rapidminer.operator.learner.tree;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.Example;
import com.rapidminer.tools.Tools;

/**
 * A split condition for nominal values (equals).
 * 
 * @author Ingo Mierswa
 */
public class NominalSplitCondition extends AbstractSplitCondition {
    
    private static final long serialVersionUID = 3883155435836330171L;
    
	private double value;
    
    private String valueString;
    
    public NominalSplitCondition(Attribute attribute, String valueString) {
        super(attribute.getName());
        this.value = attribute.getMapping().getIndex(valueString);
        this.valueString = valueString;
    }
    
    public boolean test(Example example) {
        double currentValue = example.getValue(example.getAttributes().get(getAttributeName()));
        return Tools.isEqual(currentValue, value);
    }

	public String getRelation() {
		return "=";
	}

	public String getValueString() {
		return this.valueString;
	}
}
