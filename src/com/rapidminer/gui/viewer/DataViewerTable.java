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
package com.rapidminer.gui.viewer;

import java.awt.Color;
import java.awt.event.MouseEvent;
import java.util.Iterator;

import javax.swing.table.JTableHeader;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.AttributeRole;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.gui.tools.CellColorProvider;
import com.rapidminer.gui.tools.ExtendedJTable;
import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.tools.Ontology;


/**
 * Can be used to display (parts of) the data by means of a JTable.
 * 
 * @author Ingo Mierswa
 */
public class DataViewerTable extends ExtendedJTable {
    
	private static final int MAXIMAL_CONTENT_LENGTH = 200;
	
    private static final long serialVersionUID = 5535239693801265693L;
    
    private int numberOfSpecialAttributes = 0;
    
    private int[] dateColumns;
    
    
    public DataViewerTable() { 
        setAutoResizeMode(AUTO_RESIZE_OFF);   
        setFixFirstColumnForRearranging(true);
        installToolTip();
    }

    public void setExampleSet(ExampleSet exampleSet) {
        setModel(new DataViewerTableModel(exampleSet));
        this.numberOfSpecialAttributes = exampleSet.getAttributes().specialSize();

        dateColumns = new int[exampleSet.getAttributes().allSize() + 1];
        dateColumns[0] = NO_DATE_FORMAT;
        int index = 1;
        Iterator<AttributeRole> s = exampleSet.getAttributes().specialAttributes();
        while (s.hasNext()) {
        	Attribute attribute = s.next().getAttribute();
        	if (Ontology.ATTRIBUTE_VALUE_TYPE.isA(attribute.getValueType(), Ontology.DATE)) {
        		dateColumns[index] = DATE_FORMAT;
        	} else if (Ontology.ATTRIBUTE_VALUE_TYPE.isA(attribute.getValueType(), Ontology.TIME)) {
        		dateColumns[index] = TIME_FORMAT;
        	} else if (Ontology.ATTRIBUTE_VALUE_TYPE.isA(attribute.getValueType(), Ontology.DATE_TIME)) {
        		dateColumns[index] = DATE_TIME_FORMAT;
        	} else {
        		dateColumns[index] = NO_DATE_FORMAT;
        	}
        	index++;
        }
        
        Iterator<Attribute> a = exampleSet.getAttributes().iterator();
        while (a.hasNext()) {
        	Attribute attribute = a.next();
        	if (Ontology.ATTRIBUTE_VALUE_TYPE.isA(attribute.getValueType(), Ontology.DATE)) {
        		dateColumns[index] = DATE_FORMAT;
        	} else if (Ontology.ATTRIBUTE_VALUE_TYPE.isA(attribute.getValueType(), Ontology.TIME)) {
        		dateColumns[index] = TIME_FORMAT;
        	} else if (Ontology.ATTRIBUTE_VALUE_TYPE.isA(attribute.getValueType(), Ontology.DATE_TIME)) {
        		dateColumns[index] = DATE_TIME_FORMAT;
        	} else {
        		dateColumns[index] = NO_DATE_FORMAT;
        	}
        	index++;
        }
        
        setCellColorProvider(new CellColorProvider() {
            public Color getCellColor(int row, int column) {
            	int col = convertColumnIndexToModel(column);
                if ((col > 0) && (col < (numberOfSpecialAttributes + 1))) {
                    if (row % 2 == 0) {
                        return Color.WHITE;
                    } else {
                    	return SwingTools.LIGHTEST_YELLOW;
                    }
                } else {
                    if (row % 2 == 0) {
                    	return Color.WHITE;
                    } else {
                    	return SwingTools.LIGHTEST_BLUE;
                    }
                }
            }
        });
        
    	setCutOnLineBreak(true);
    	setMaximalTextLength(MAXIMAL_CONTENT_LENGTH);
    }
    
    /** This method ensures that the correct tool tip for the current column is delivered. */
    @Override
	protected JTableHeader createDefaultTableHeader() {
      return new JTableHeader(columnModel) {
    	  
		private static final long serialVersionUID = 1L;

		@Override
		public String getToolTipText(MouseEvent e) {
          java.awt.Point p = e.getPoint();
          int index = columnModel.getColumnIndexAtX(p.x);
          int realColumnIndex = convertColumnIndexToModel(index);
          return DataViewerTable.this.getHeaderToolTipText(realColumnIndex);
        }
      };
    }
    
    private String getHeaderToolTipText(int realColumnIndex) {
        if (realColumnIndex == 0) {
        	return "The position of the example in the (filtered) view on the example table.";
        } else {
        	return "The data for the attribute '" + 
        	  getModel().getColumnName(realColumnIndex) + "'.";
        }	
    }
    
    @Override
	public int getDateFormat(int row, int column) {
    	return dateColumns[column];
    }
}
