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
package com.rapidminer.operator.nio;

import java.util.LinkedList;
import java.util.List;

import org.eobjects.sassy.SasReader;

import com.rapidminer.example.ExampleSet;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.UserError;
import com.rapidminer.operator.io.AbstractExampleSource;
import com.rapidminer.operator.io.AbstractReader;
import com.rapidminer.operator.nio.file.FileInputPortHandler;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.Port;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.PortProvider;

/**
 * This operator can be used to load SAS (.sas7bdat) data files.
 * 
 * @author Marco Boeck
 *
 */
public class SASExampleSource extends AbstractExampleSource {
	
	private InputPort fileInputPort = getInputPorts().createPort("file");
	private FileInputPortHandler filePortHandler = new FileInputPortHandler(this, fileInputPort, this.getFileParameterName());

	public static final String PARAMETER_SAS_FILE = "sas_file";

	static {
		AbstractReader.registerReaderDescription(new ReaderDescription("sas7bdat", SASExampleSource.class, PARAMETER_SAS_FILE));
	}
	

	/**
	 * Standard constructor.
	 * @param description
	 */
	public SASExampleSource(OperatorDescription description) {
		super(description);
	}

	protected String getFileParameterName() {
		return PARAMETER_SAS_FILE;
	}

	protected String getFileExtension() {
		return "sas7bdat";
	}
	
	@Override
    public List<ParameterType> getParameterTypes() {
        LinkedList<ParameterType> types = new LinkedList<ParameterType>();
        
        types.add(makeFileParameterType());
        
        types.addAll(super.getParameterTypes());
        return types;
    }

	private ParameterType makeFileParameterType() {
		return FileInputPortHandler.makeFileParameterType(this, getFileParameterName(), "Name of the file to read the data from.", new PortProvider() {
			@Override
			public Port getPort() {
				return fileInputPort;
			}
		}, getFileExtension());
	}

	@Override
	public ExampleSet createExampleSet() throws OperatorException {
		SASExampleReader sasCallbackReader = new SASExampleReader();
		SasReader reader = new SasReader(filePortHandler.getSelectedFile());
		reader.read(sasCallbackReader);
		ExampleSet exSet = sasCallbackReader.getExampleSet();
		if (exSet == null) {
			throw new UserError(this, 957);
		}
		
		return exSet;
	}

}
