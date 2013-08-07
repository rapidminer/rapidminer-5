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
package com.rapidminer.gui.processeditor;

import java.awt.Color;
import java.awt.Component;

import javax.swing.JLabel;
import javax.swing.JTree;
import javax.swing.tree.DefaultTreeCellRenderer;

import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.tools.GroupTree;
import com.rapidminer.tools.usagestats.OperatorStatisticsValue;
import com.rapidminer.tools.usagestats.OperatorUsageStatistics;
import com.rapidminer.tools.usagestats.UsageStatistics;
import com.rapidminer.tools.usagestats.UsageStatistics.StatisticsScope;


/**
 * The renderer for the group tree (displays a small group icon).
 * 
 * @author Ingo Mierswa
 */
public class NewOperatorGroupTreeRenderer extends DefaultTreeCellRenderer {

    private static final long serialVersionUID = -6092290820461444236L;
	private int maxVisibleUsage;
	
	private static final Color maxUsedColor = new Color(0, 125, 0);
	private static final Color minUsedColor = new Color(75, 25, 25);
	
    public NewOperatorGroupTreeRenderer() {
		setLeafIcon(getDefaultClosedIcon());
    }
    
    @Override
	public Component getTreeCellRendererComponent(JTree tree, 
                                                  Object value, 
                                                  boolean isSelected,
                                                  boolean expanded,
                                                  boolean leaf,
                                                  int row,
                                                  boolean hasFocus) {

        
        if (value instanceof GroupTree) {
        	GroupTree groupTree = (GroupTree)value;
        	setToolTipText("This group contains all operators of the group '" + groupTree.getName() + "'.");
        	return super.getTreeCellRendererComponent(tree, groupTree.toString(), isSelected, expanded, leaf, row, hasFocus);
        } else {
        	OperatorDescription op = (OperatorDescription)value;
        	OperatorUsageStatistics operatorStatistics = UsageStatistics.getInstance().getOperatorStatistics(StatisticsScope.ALL_TIME, op);
        	int usageCount = operatorStatistics.getStatistics(OperatorStatisticsValue.EXECUTION);
        	
        	setToolTipText(null);
        	String labelText = op.getName();
        	
        	// define text color
        	Color foregroundColor;
        	if (hasFocus || (maxVisibleUsage == 0)) {
            	foregroundColor = Color.BLACK;
        	} else {
        		// fade text color respective to usage count
        		
        		// use log for gentler descent
            	double usageFactor = Math.min(1, Math.log(usageCount+1) / Math.log(maxVisibleUsage+1d));

            	// merge colors
            	
            	int r = (int) (maxUsedColor.getRed() * (usageFactor) + minUsedColor.getRed() * (1-usageFactor));
            	int g = (int) (maxUsedColor.getGreen() * (usageFactor) + minUsedColor.getGreen() * (1-usageFactor));
            	int b = (int) (maxUsedColor.getBlue() * (usageFactor) + minUsedColor.getBlue() * (1-usageFactor));
            	foregroundColor = new  Color(r, g, b);
        	}
			
        	
			JLabel label = (JLabel)super.getTreeCellRendererComponent(tree, labelText, isSelected, expanded, leaf, row, hasFocus);
        	label.setIcon(op.getSmallIcon());
        	if (isSelected) {
        		label.setForeground(Color.WHITE);
        	} else {
        		label.setForeground(foregroundColor);
        	}
        	if (op.getDeprecationInfo() != null) {
        		label.setEnabled(false);
        	}
        	
        	return label;
        }        
    }
    
    public void setMaxVisibleUsageCount(int count) {
    	this.maxVisibleUsage = count;
    }

}
