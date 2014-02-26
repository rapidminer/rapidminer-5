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
package com.rapidminer.tools.expression.parser;

/**
 * 
 * @author Ingo Mierswa
 *
 */
public class FunctionDescription {

	public static final int UNLIMITED_NUMBER_OF_ARGUMENTS = -1;
	
	private String displayName;

	private String helpTextName;

	private String functionDescription;

	private int numberOfArguments;

	public FunctionDescription(String functionName, String helpTextName, String description, int numberOfArguments) {
		this.displayName = functionName;
		this.helpTextName = helpTextName;
		this.functionDescription = description;
		this.numberOfArguments = numberOfArguments;
	}

	public String getDisplayName() {
		return this.displayName;
	}

	public String getHelpTextName() {
		return this.helpTextName;
	}

	public String getDescription() {
		return this.functionDescription;
	}

	public int getNumberOfArguments() {
		return this.numberOfArguments;
	}
}
