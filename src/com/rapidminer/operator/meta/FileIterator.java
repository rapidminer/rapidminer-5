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
package com.rapidminer.operator.meta;

import java.io.File;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;

import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.nio.file.FileObject;
import com.rapidminer.operator.nio.file.SimpleFileObject;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeDirectory;


/**
 * This operator iterates over the files in the specified directory (and
 * subdirectories if the corresponding parameter is set to true).
 * 
 * @author Sebastian Land, Ingo Mierswa, Marius Helf
 */
public class FileIterator extends AbstractFileIterator {
    public static final String PARAMETER_DIRECTORY = "directory";

    private File directory;
    
    
	public FileIterator(OperatorDescription description) {
		super(description);
	}
	
	@Override
	public void doWork() throws OperatorException {
        directory = getParameterAsFile(PARAMETER_DIRECTORY);

		super.doWork();
	}

	@Override
    protected void iterate(Object currentParent, Pattern filter, boolean iterateSubDirs, boolean iterateFiles, boolean recursive) throws OperatorException {
		if (currentParent == null) {
			currentParent = directory;
		}
		File dir = (File)currentParent;
        if (dir.isDirectory()) {
            for (File child : dir.listFiles()) {
                if (iterateSubDirs && child.isDirectory() || iterateFiles && child.isFile()) {
                	String fileName = child.getName();
                	String fullPath = child.getAbsolutePath();
                	String parentPath = child.getParent();
                    if (matchesFilter(filter, fileName, fullPath, parentPath)) {
                    	FileObject fileObject = new SimpleFileObject(child);
                    	doWorkForSingleIterationStep(fileName, fullPath, parentPath, fileObject);
                    }
                }

                if (recursive && child.isDirectory()) {
                    iterate(child, filter, iterateSubDirs, iterateFiles, recursive);
                }
            }
        }
    }
	
	
	@Override
	public List<ParameterType> getParameterTypes() {
        List<ParameterType> types = new LinkedList<ParameterType>();

        ParameterType type = new ParameterTypeDirectory(PARAMETER_DIRECTORY, "Specifies the directory to iterate over.", false);
        type.setExpert(false);
        types.add(type);
        
        types.addAll(super.getParameterTypes());
        
        return types;
	}

}
