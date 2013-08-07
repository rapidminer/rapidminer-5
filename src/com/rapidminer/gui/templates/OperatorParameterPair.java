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
package com.rapidminer.gui.templates;


/** A helper class for pairs of operators and their parameters. 
 * 
 * @author Simon Fischer
 * */
public class OperatorParameterPair implements Comparable<OperatorParameterPair> {

	private final String operator;
	private final String parameterKey;

	public OperatorParameterPair(String operator, String parameterKey) {
		this.operator = operator;
		this.parameterKey = parameterKey;
	}

	@Override
	public String toString() {
		return (this.operator + "." + this.parameterKey);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((operator == null) ? 0 : operator.hashCode());
		result = prime * result + ((parameterKey == null) ? 0 : parameterKey.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		OperatorParameterPair other = (OperatorParameterPair) obj;
		if (operator == null) {
			if (other.operator != null)
				return false;
		} else if (!operator.equals(other.operator))
			return false;
		if (parameterKey == null) {
			if (other.parameterKey != null)
				return false;
		} else if (!parameterKey.equals(other.parameterKey))
			return false;
		return true;
	}

	@Override
	public int compareTo(OperatorParameterPair o) {
		int result = this.operator.compareTo(o.operator);
		if (result != 0) {
			return result;
		} else {
			return this.parameterKey.compareTo(o.parameterKey);
		}
	}

	public String getParameter() {		
		return parameterKey;
	}

	public String getOperator() {		
		return operator;
	}
}
