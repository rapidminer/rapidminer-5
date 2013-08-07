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
package com.rapidminer.gui.plotter.conditions;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import com.rapidminer.datatable.DataTable;


/**
 * This condition accepts data tables which fulfills all child conditions.
 * 
 * @author Ingo Mierswa
 */
public class CombinedPlotterCondition implements PlotterCondition {

    private List<PlotterCondition> conditions = new LinkedList<PlotterCondition>();
  
    public void addPlotterCondition(PlotterCondition condition) {
    	conditions.add(condition);
    }
    
    public boolean acceptDataTable(DataTable dataTable) {
    	Iterator<PlotterCondition> i = conditions.iterator();
    	while (i.hasNext()) {
    		PlotterCondition condition = i.next();
    		if (!condition.acceptDataTable(dataTable))
    			return false;
    	}
    	return true;
    }
    
    public String getRejectionReason(DataTable dataTable) {
    	Iterator<PlotterCondition> i = conditions.iterator();
    	while (i.hasNext()) {
    		PlotterCondition condition = i.next();
    		if (!condition.acceptDataTable(dataTable))
    			return condition.getRejectionReason(dataTable);
    	}
    	return "";
    }
}
