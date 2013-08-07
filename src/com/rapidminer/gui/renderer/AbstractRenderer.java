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
package com.rapidminer.gui.renderer;

import java.awt.Color;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;

import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeCategory;
import com.rapidminer.parameter.ParameterTypeColor;
import com.rapidminer.parameter.ParameterTypeList;
import com.rapidminer.parameter.ParameterTypeTupel;
import com.rapidminer.parameter.Parameters;
import com.rapidminer.parameter.UndefinedParameterError;
import com.rapidminer.tools.WebServiceTools;
import com.rapidminer.tools.math.StringToMatrixConverter;

/**
 * This is the abstract renderer superclass for all renderers which
 * provide some basic methods for parameter handling.
 * 
 * @author Ingo Mierswa
 */
public abstract class AbstractRenderer implements Renderer {

	private Parameters parameters;

	public AbstractRenderer() {}

	public final List<ParameterType> getParameterTypes() {
		return getParameterTypes(null);
	}

	public List<ParameterType> getParameterTypes(InputPort inputPort) {
		return new LinkedList<ParameterType>();
	}
	
	public String getParameter(String key) throws UndefinedParameterError {
		return getParameters().getParameter(key);
	}

	@Override
	public String toString() {
		return getName();
	}

	public boolean getParameterAsBoolean(String key) {
		try {
			return Boolean.valueOf(getParameter(key));
		} catch (UndefinedParameterError e) {
			return false;
		}
	}
	
	public char getParameterAsChar(String key) throws UndefinedParameterError {
		String parameterValue = getParameter(key);
		if (parameterValue.length() > 0) {
			return parameterValue.charAt(0);
		}
		return 0;
	}

	public Color getParameterAsColor(String key) throws UndefinedParameterError {
		return ParameterTypeColor.string2Color(getParameter(key));
	}

	public double getParameterAsDouble(String key) throws UndefinedParameterError {
		return Double.valueOf(getParameter(key));
	}

	public InputStream getParameterAsInputStream(String key) throws UndefinedParameterError, IOException {
		String urlString = getParameter(key);
		if (urlString == null)
			return null;

		try {
			URL url = new URL(urlString);
			InputStream stream = WebServiceTools.openStreamFromURL(url);
			return stream;
		} catch (MalformedURLException e) {
			// URL did not work? Try as file...
			File file = getParameterAsFile(key);
			if (file != null) {
				return new FileInputStream(file);
			} else {
				return null;
			}
		}
	}

	public File getParameterAsFile(String key) throws UndefinedParameterError {
		return getParameterAsFile(key, false);
	}

	public File getParameterAsFile(String key, boolean createMissingDirectories) throws UndefinedParameterError {
		return new File(getParameter(key));
	}

	public int getParameterAsInt(String key) throws UndefinedParameterError {
		ParameterType type = parameters.getParameterType(key);
		String value = getParameter(key);
		if (type != null) {
			if (type instanceof ParameterTypeCategory) {
				String parameterValue = value;
				try {
					return Integer.valueOf(parameterValue);
				} catch (NumberFormatException e) {
					ParameterTypeCategory categoryType = (ParameterTypeCategory)type;
					return categoryType.getIndex(parameterValue);	
				}
			} 
		}
		try {
			return Integer.valueOf(value);
		} catch (NumberFormatException e) {
			throw new UndefinedParameterError(key, "Expected integer but found '"+value+"'.");
		}		
	}
	
	 /** Returns a single named parameter and casts it to long. */
    @Override
    public long getParameterAsLong(String key) throws UndefinedParameterError {
        ParameterType type = this.getParameters().getParameterType(key);
        String value = getParameter(key);
        if (type != null) {
            if (type instanceof ParameterTypeCategory) {
                String parameterValue = value;
                try {
                    return Long.valueOf(parameterValue);
                } catch (NumberFormatException e) {
                    ParameterTypeCategory categoryType = (ParameterTypeCategory)type;
                    return categoryType.getIndex(parameterValue);
                }
            }
        }
        try {
            return Long.valueOf(value);
        } catch (NumberFormatException e) {
            throw new UndefinedParameterError(key, "Expected integer but found '"+value+"'.");
        }
    }

	public double[][] getParameterAsMatrix(String key) throws UndefinedParameterError {
		String matrixLine = getParameter(key);
		try {
			return StringToMatrixConverter.parseMatlabString(matrixLine);
		} catch (OperatorException e) {
			throw new UndefinedParameterError(e.getMessage());
		}
	}

	public String getParameterAsString(String key) throws UndefinedParameterError {
		return getParameter(key);
	}

	public List<String[]> getParameterList(String key) throws UndefinedParameterError {
		return ParameterTypeList.transformString2List(getParameter(key));
	}

	public String[] getParameterTupel(String key) throws UndefinedParameterError {
		return ParameterTypeTupel.transformString2Tupel(getParameter(key));
	}

	public boolean isParameterSet(String key) throws UndefinedParameterError {
		return getParameter(key) != null;
	}

	public void setListParameter(String key, List<String[]> list) {
		this.parameters.setParameter(key, ParameterTypeList.transformList2String(list));
	}

	public void setParameter(String key, String value) {
		getParameters().setParameter(key, value);
	}

	/** Do nothing. */
	public void setParameters(Parameters parameters) {
		this.parameters = parameters;
	}

	/** Returns null. */
	public Parameters getParameters() {
		if (this.parameters == null) {
			this.parameters = new Parameters(getParameterTypes());
		}
		return this.parameters;
	}
	
	/** Returns null. */
	public Parameters getParameters(InputPort inputPort) {
		if (this.parameters == null) {
			updateParameters(inputPort);
		}
		return this.parameters;
	}
	
	/** This method overrides all existing parameters. It must be used to ensure, that 
	 * input Port referencing attributes are connected to the correct port, since they are only created once and might 
	 * be initialized from another operator.
	 */
	public void updateParameters(InputPort inputPort) {
		this.parameters = new Parameters(getParameterTypes(inputPort));
	}
}
