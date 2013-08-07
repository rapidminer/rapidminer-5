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

import java.util.EnumMap;
import java.util.Map;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.rapidminer.io.process.XMLTools;

/** Collects statistics abut a single operator.
 * 
 * @author Simon Fischer
 *
 */
public class OperatorUsageStatistics {

	private final EnumMap<OperatorStatisticsValue, Integer> statistics = new EnumMap<OperatorStatisticsValue,Integer>(OperatorStatisticsValue.class);
	
	protected void parse(Element element) {
		for (OperatorStatisticsValue type : OperatorStatisticsValue.values()) {
			String stats = XMLTools.getTagContents(element, type.getTagName());
			if (stats != null) {
				statistics.put(type, Integer.parseInt(stats));
			}
		}
	}
	
	public  void reset() {
		statistics.clear();
	}

	protected void count(OperatorStatisticsValue type) {
		Integer value = statistics.get(type);
		if (value == null) {
			value = 1;
		} else {
			value = value+1;
		}
		statistics.put(type, value);
	}

	protected Node getXML(String key, Document doc) {
		Element element = doc.createElement(key);
		mapToXML(element, statistics);	
		return element;
	}

	private void mapToXML(Element element, EnumMap<OperatorStatisticsValue,Integer> map) {
		for (Map.Entry<OperatorStatisticsValue, Integer> entry : map.entrySet()) {
			XMLTools.setTagContents(element, entry.getKey().getTagName(), entry.getValue().toString());
		}
	}

	public int getStatistics(OperatorStatisticsValue type) {
		Integer val = statistics.get(type);
		if (val == null) {
			return 0;
		} else {
			return val;
		}
	}
}
