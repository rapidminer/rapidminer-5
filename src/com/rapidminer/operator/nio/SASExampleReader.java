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
package com.rapidminer.operator.nio;

import java.util.LinkedList;
import java.util.List;

import org.eobjects.sassy.SasColumnType;
import org.eobjects.sassy.SasReaderCallback;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.table.AttributeFactory;
import com.rapidminer.example.table.DataRow;
import com.rapidminer.example.table.DoubleArrayDataRow;
import com.rapidminer.example.table.MemoryExampleTable;
import com.rapidminer.tools.Ontology;

/**
 * This class is the {@link SasReaderCallback} implementation for the {@link SASExampleSource} operator.
 * 
 * @author Marco Boeck
 *
 */
public class SASExampleReader implements SasReaderCallback {
	
	/** the example set which is created by the SAS reader */
	private MemoryExampleTable exSet;
	
	/** list of all {@link Attribute}s in this SAS file */
	private List<Attribute> attList;
	
	
	/**
	 * Standard constructor.
	 */
	public SASExampleReader() {
		attList = new LinkedList<Attribute>();
	}

	@Override
	public void column(int columnIndex, String columnName, String columnLabel, SasColumnType columnType, int columnLength) {
		Attribute attribute = null;
		switch (columnType) {
		case CHARACTER:
			attribute = AttributeFactory.createAttribute(columnName, Ontology.POLYNOMINAL);
			break;
		case NUMERIC:
			attribute = AttributeFactory.createAttribute(columnName, Ontology.REAL);
			break;
		}
		attList.add(attribute);
	}

	@Override
	public boolean readData() {
		return true;
	}

	@Override
	public boolean row(int rowNumber, Object[] rowData) {
		if (exSet == null) {
			exSet = new MemoryExampleTable(attList);
		}
		
		double[] doubleArray = new double[attList.size()];
		for (int i=0; i<rowData.length; i++) {
			Object data = rowData[i];
			Attribute att = attList.get(i);
			
			switch (att.getValueType()) {
			case Ontology.POLYNOMINAL:
				doubleArray[i] = att.getMapping().mapString(String.valueOf(data));
				break;
			case Ontology.REAL:
				doubleArray[i] = Double.parseDouble(String.valueOf(data));
				break;
			}
		}
		DataRow row = new DoubleArrayDataRow(doubleArray);
		exSet.addDataRow(row);
		
		return true;
	}
	
	/**
	 * Returns the {@link ExampleSet} created by the given SAS file.
	 * @return the created {@link ExampleSet} or <code>null</code>.
	 */
	public ExampleSet getExampleSet() {
		if (exSet != null) {
			return exSet.createExampleSet();
		} else {
			return null;
		}
	}

}
