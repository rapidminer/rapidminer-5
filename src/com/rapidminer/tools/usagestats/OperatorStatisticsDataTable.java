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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.rapidminer.datatable.AbstractDataTable;
import com.rapidminer.datatable.DataTable;
import com.rapidminer.datatable.DataTableRow;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.tools.OperatorService;
import com.rapidminer.tools.usagestats.UsageStatistics.StatisticsScope;

/** Presents {@link UsageStatistics} as a {@link DataTable}.
 * 
 * @author Simon Fischer
 *
 */
class OperatorStatisticsDataTable extends AbstractDataTable {

	/**
	 * The statistics reflected by this table. 
	 */
	private final UsageStatistics usageStatistics;
	
	/**
	 *  The scope to use.
	 */
	private final StatisticsScope scope;
	
	/**
	 *  The operator names. Will be extracted in the constructor.
	 */
	private final List<String> operatorKeys;

	OperatorStatisticsDataTable(UsageStatistics usageStatistics, StatisticsScope scope) {
		super("Operator Statistics for "+scope);
		this.usageStatistics = usageStatistics;
		this.scope = scope;
		operatorKeys = new ArrayList<String>(usageStatistics.getOperatorKeys(scope));
	}

	@Override
	public void add(DataTableRow row) {
		throw new UnsupportedOperationException("Statistics table is immutable");
	}

	@Override
	public int getColumnIndex(String name) {
		if ("Operator".equals(name))  {
			return 0;
		} else {
			for (OperatorStatisticsValue type : OperatorStatisticsValue.values()) {
				if (type.toString().equals(name)) {
					return type.ordinal();
				}
			}
			return -1;
		} 
	}

	@Override
	public String getColumnName(int i) {
		if (i == 0) {
			return "Operator";
		} else {
			return OperatorStatisticsValue.values()[i-1].toString();
		}
	}

	@Override
	public double getColumnWeight(int i) {
		return 1;
	}

	@Override
	public int getNumberOfColumns() {
		return OperatorStatisticsValue.values().length+1;
	}

	@Override
	public int getNumberOfRows() {
		return operatorKeys.size();
	}

	@Override
	public int getNumberOfSpecialColumns() {
		return 0;
	}

	@Override
	public int getNumberOfValues(int column) {
		if (column == 0) {
			return this.operatorKeys.size();
		} else {
			return -1;
		}
	}

	@Override
	public DataTableRow getRow(final int row) {
		return new DataTableRow() {
			@Override
			public String getId() {
				return ""+row;
			}
			@Override
			public int getNumberOfValues() {
				return OperatorStatisticsValue.values().length;
			}
			@Override
			public double getValue(int col) {
				if (col == 0) {
					return row;
				} else {
					OperatorStatisticsValue colType = OperatorStatisticsValue.values()[col-1];
					String opKey = operatorKeys.get(row);
					return usageStatistics.getOperatorStatistics(scope, opKey).getStatistics(colType);	
				}				
			}			
		};
	}

	@Override
	public boolean isDate(int index) {
		return false;
	}

	@Override
	public boolean isDateTime(int index) {
		return false;
	}

	@Override
	public boolean isNominal(int index) {
		return index == 0;
	}

	@Override
	public boolean isNumerical(int index) {
		return index != 0;
	}

	@Override
	public boolean isSpecial(int column) {
		return false;
	}

	@Override
	public boolean isSupportingColumnWeights() {				
		return false;
	}

	@Override
	public boolean isTime(int index) {
		return false;
	}

	@Override
	public Iterator<DataTableRow> iterator() {
		return new Iterator<DataTableRow>() {
			private int row;
			@Override
			public boolean hasNext() {
				return row < operatorKeys.size();
			}
			@Override
			public DataTableRow next() {
				return getRow(row++);
			}
			@Override
			public void remove() {
				throw new UnsupportedOperationException("Removing statistics is not supported.");
			}			
		};
	}

	@Override
	public String mapIndex(int column, int index) {
		if (column == 0) {
			String key = operatorKeys.get(index); 
			OperatorDescription desc = OperatorService.getOperatorDescription(key);
			if (desc != null) {
				return desc.getName();
			} else {
				return key;
			}
		} else {
			return ""+index;
		}
	}

	@Override
	public int mapString(int column, String value) {
		if (column == 0) {
			return operatorKeys.indexOf(value);
		} else {
			return -1;
		}
	}

	@Override
	public DataTable sample(int newSize) {
		throw new UnsupportedOperationException("Sampling not supported for statistics tables.");
	}
}
