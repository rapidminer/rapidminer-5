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
package com.rapidminer.gui.properties;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.Properties;

import com.rapidminer.operator.Operator;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.tools.ParameterService;
/**
 * This class is somehow missnamed, because it's original purpose is to
 * display defined Parameters of the {@link ParameterService}.
 * It also holds a method for applying changes in the value back to the {@link ParameterService}.
 * @author Sebastian Land, Simon Fischer
 */
public class SettingsPropertyPanel extends PropertyPanel {

    private static final long serialVersionUID = 313811558626370370L;

    private final Collection<ParameterType> shownParameterTypes;

    private final Properties parameterValues;

    public SettingsPropertyPanel(Collection<ParameterType> shownParameterTypes) {
        this.shownParameterTypes = shownParameterTypes;
        this.parameterValues = new Properties();

        for (ParameterType type: shownParameterTypes) {
            String key = type.getKey();
            parameterValues.put(key, ParameterService.getParameterValue(key));
        }

        setupComponents();
    }


    @Override
    protected Collection<ParameterType> getProperties() {
        return shownParameterTypes;
    }

    @Override
    protected String getValue(ParameterType type) {
        String value = parameterValues.getProperty(type.getKey());
        if (value == null) {
            return null;
        } else {
            return type.transformNewValue(value);
        }
    }

    @Override
    protected void setValue(Operator operator, ParameterType type, String value) {
        parameterValues.put(type.getKey(), value);
    }

    /** Applies the properties without saving them. */
    public void applyProperties() {
        for (ParameterType type: shownParameterTypes) {
            String value = parameterValues.getProperty(type.getKey());
            ParameterService.setParameterValue(type, value);
        }
    }


    @Override
    protected Operator getOperator() {
        return null;
    }


    /**
     * This method remains for compatibility. But the settings edited in this pane
     * will now be directly saved by in the ParameterService.
     */
    @Deprecated
    public void applyProperties(Properties properties) {
        applyProperties();
        //        for (ParameterType type : propertyTypes) {
        //            String value = getValue(type);
        //            if (value != null) {
        //                properties.setProperty(type.getKey(), value);
        //            } else {
        //                properties.remove(type.getKey());
        //            }
        //        }
    }

    /**
     * This method is deprecated and won't affect any parameters in the {@link ParameterService}.
     * Applies and write the properties in the system dependent config file in
     * the user directory.
     * 
     */
    @Deprecated
    public void writeProperties(PrintWriter out) throws IOException {
        for (ParameterType type : shownParameterTypes) {
            String value = getValue(type);
            if (value != null) {
                System.setProperty(type.getKey(), value);
                out.println(type.getKey() + " = " + value);
            }
        }
    }

}
