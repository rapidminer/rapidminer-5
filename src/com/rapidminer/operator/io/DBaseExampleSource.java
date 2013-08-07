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

import com.rapidminer.operator.OperatorDescription;

/**
 * This operator can read dbase files. It uses Stefan Haustein's kdb tools.
 * 
 * @rapidminer.index dbase
 * @author Ingo Mierswa, Simon Fischer
 *          Exp $
 */
public class DBaseExampleSource extends KDBExampleSource {

	public DBaseExampleSource(OperatorDescription description) {
		super(description);
	}

	@Override
	public String getFormat() {
		return "dbase";
	}
	
	@Override
	public String getExtension() { return "dbf"; }
}
