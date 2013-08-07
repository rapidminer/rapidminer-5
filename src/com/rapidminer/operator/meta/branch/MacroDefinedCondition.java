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
package com.rapidminer.operator.meta.branch;

import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.UserError;

/**
 * This condition tests if a macro with the given name is defined.
 * 
 * @author Tobias Malbrecht
 */
public class MacroDefinedCondition implements ProcessBranchCondition {
	
	/**
	 * Constructor used by reflection.
	 */
	public MacroDefinedCondition() {}
	
	public boolean check(ProcessBranch operator, String value) throws OperatorException {
		if (value == null) {
			throw new UserError(operator, 205, ProcessBranch.PARAMETER_CONDITION_VALUE);
		}
		
		String macroValue = operator.getProcess().getMacroHandler().getMacro(value);
		return macroValue != null;
	}
}



