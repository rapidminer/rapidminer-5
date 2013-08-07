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

import java.util.Comparator;

import com.rapidminer.operator.OperatorDescription;

/** Sorts operators such that frequently used operators are first.
 * 
 * @author Simon Fischer
 *
 */
public class OperatorStatisticsComparator implements Comparator<OperatorDescription> {

	private UsageStatistics.StatisticsScope scope;
	private OperatorStatisticsValue type;
	
	public OperatorStatisticsComparator(UsageStatistics.StatisticsScope scope, OperatorStatisticsValue type) {
		this.scope = scope;
		this.type = type;
	}
	
	@Override
	public int compare(OperatorDescription d1, OperatorDescription d2) {
		return UsageStatistics.getInstance().getOperatorStatistics(scope, d2).getStatistics(type) -
			UsageStatistics.getInstance().getOperatorStatistics(scope, d1).getStatistics(type);
	}	
}
