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
package com.rapidminer.tools.usagestats;
/**
 * @author Simon Fischer
 */
public enum OperatorStatisticsValue {
	/** Counts how often a process containing this operator was executed.
	 *  (Not how often the operator was executed). */
	EXECUTION("run"),
	
//	/** Counts how often the operator was created. */
//	CREATION("add"),
	
//	/** Counts how often this operator threw an exception. */
//	THROWN("exc"),
	
	/** Counts how often this operator was contained in a process throwing an exception. */
	FAILURE("fail"),
	
	/** Counts how often this operator has thrown a UserError. */
	USER_ERROR("user_err"),
	
	/** Counts how often this operator was contained in a process that has thrown a Non-OperatorException. */
	RUNTIME_EXCEPTION("runtime_err"),
	
	/** Counts how often this operator was contained in a process that has thrown an OperatorException that is not a UserError. */
	OPERATOR_EXCEPTION("op_err"),
	
	/** Counts how often this operator was stopped from the GUI. */
	STOPPED("stop");
	
	private String xmlKey;
	private OperatorStatisticsValue(String xmlKey) {
		this.xmlKey = xmlKey;
	}
	protected String getTagName() {
		return xmlKey;
	}
}
