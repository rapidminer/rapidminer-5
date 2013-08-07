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
package com.rapidminer.parameter;

import org.w3c.dom.Element;

import com.rapidminer.tools.XMLException;



/**
 * A parameter type for categories. These are several Strings and one of these
 * is the default value. Operators ask for the index of the selected value with
 * {@link com.rapidminer.operator.Operator#getParameterAsInt(String)}.
 * 
 * @author Ingo Mierswa, Simon Fischer, Tobias Malbrecht
 */
public class ParameterTypeDatabaseConnection extends ParameterTypeSingle {

    private static final long serialVersionUID = 5747692587025691591L;

    public ParameterTypeDatabaseConnection(Element element) throws XMLException {
        super(element);
    }

    public ParameterTypeDatabaseConnection(String key, String description, boolean expert) {
        this(key, description);
        setExpert(expert);
    }

    public ParameterTypeDatabaseConnection(String key, String description) {
        super(key, description);
    }

    /** Returns false. */
    @Override
    public boolean isNumerical() {
        return false;
    }

    @Override
    public Object getDefaultValue() {
        return null;
    }

    @Override
    public String getRange() {
        return null;
    }

    @Override
    public void setDefaultValue(Object defaultValue) {
    }

    @Override
    protected void writeDefinitionToXML(Element typeElement) {
    }
}
