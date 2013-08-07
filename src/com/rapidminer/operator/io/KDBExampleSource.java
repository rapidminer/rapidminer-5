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
import java.sql.ResultSet;
import java.util.Iterator;
import java.util.List;

import org.kobjects.jdbc.TableManager;
import org.kobjects.jdbc.util.AbstractResultSet;

import com.rapidminer.example.Attribute;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.nio.file.FileInputPortHandler;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.Port;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.PortProvider;


/**
 * This class can read arff, comma separated values (csv), dbase and bibtex
 * files. It uses Stefan Haustein's kdb tools.
 * 
 * @author Simon Fischer, Ingo Mierswa
 *          Exp $
 */
public abstract class KDBExampleSource extends ResultSetExampleSource {


	/** The parameter name for &quot;The file containing the data&quot; */
	public static final String PARAMETER_DATA_FILE = "data_file";
	public abstract String getFormat();

	public abstract String getExtension();
	
	private InputPort fileInputPort = getInputPorts().createPort("file");
	private FileInputPortHandler filePortHandler = new FileInputPortHandler(this, fileInputPort, PARAMETER_DATA_FILE);
	
	public KDBExampleSource(OperatorDescription description) {
		super(description);
	}

	/** Does nothing. */
	@Override
	public void tearDown() {}
	
	@Override
	public ResultSet getResultSet() throws OperatorException {
		File dataFile = filePortHandler.getSelectedFile();
		String dataFileAbsolutePath = dataFile.getAbsolutePath();
		return TableManager.getResultSet(getFormat() + ":" + dataFileAbsolutePath, TableManager.READ);
	}

	@Override
	public void setNominalValues(List attributeList, ResultSet resultSet, Attribute label) throws OperatorException {
		if (resultSet instanceof AbstractResultSet) {
			AbstractResultSet ars = (AbstractResultSet) resultSet;
			Iterator i = attributeList.iterator();
			int j = 0;
			while (i.hasNext()) {
				j++;
				Attribute attribute = (Attribute) i.next();
				Object[] values = ars.getColumnSet().getColumn(j).getValues();
				if (attribute.isNominal()) {
					if (values == null) {
						logWarning("Information about class values is null!");
					} else {
						for (int k = 0; k < values.length; k++) {
							attribute.getMapping().mapString(values[k].toString());
						}
					}
				}
			}
		} else {
			logWarning("Result set does not provide information about class values!");
		}
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();
		types.add(FileInputPortHandler.makeFileParameterType(this, PARAMETER_DATA_FILE, "Name of the XRFF file to read the data from.", "xrff", new PortProvider() {
			@Override
			public Port getPort() {			
				return fileInputPort;
			}
		}));
		return types;
	}

}
