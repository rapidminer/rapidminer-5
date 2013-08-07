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
package com.rapidminer.operator.learner.functions.kernel;

import com.rapidminer.datatable.DataTable;
import com.rapidminer.datatable.SimpleDataTable;
import com.rapidminer.datatable.SimpleDataTableRow;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.operator.learner.PredictionModel;
import com.rapidminer.tools.Tools;


/** This is the abstract model class for all kernel models. This class actually only provide 
 *  a common interface for plotting SVM and other kernel method models.
 * 
 *  @author Ingo Mierswa
 */
public abstract class KernelModel extends PredictionModel {

	private static final long serialVersionUID = 7480153570564620067L;

	private String[] attributeConstructions;

	public KernelModel(ExampleSet exampleSet) {
		super(exampleSet);
		this.attributeConstructions = com.rapidminer.example.Tools.getRegularAttributeConstructions(exampleSet);
	}

	public abstract double getBias();

	public abstract double getAlpha(int index);

	public abstract double getFunctionValue(int index);

	public abstract boolean isClassificationModel();

	public abstract String getClassificationLabel(int index);

	public abstract double getRegressionLabel(int index);

	public abstract String getId(int index);

	public abstract SupportVector getSupportVector(int index);

	public abstract int getNumberOfSupportVectors();

	public abstract int getNumberOfAttributes();

	public abstract double getAttributeValue(int exampleIndex, int attributeIndex);


	public String[] getAttributeConstructions() {
		return this.attributeConstructions;
	}

	/** The default implementation returns the classname without package. */
	@Override
	public String getName() {
		return "Kernel Model";
	}

	/** Returns a string representation of this model. */
	@Override
	public String toString() {
		String[] attributeNames = com.rapidminer.example.Tools.getRegularAttributeNames(getTrainingHeader());
		
		StringBuffer result = new StringBuffer();
		result.append("Total number of Support Vectors: " + getNumberOfSupportVectors() + Tools.getLineSeparator());
		result.append("Bias (offset): " + Tools.formatNumber(getBias()) + Tools.getLineSeparators(2));
		if ((!getLabel().isNominal()) || (getLabel().getMapping().size() == 2)) {
			double[] w = new double[getNumberOfAttributes()];
			boolean showWeights = true;
			for (int i = 0; i < getNumberOfSupportVectors(); i++) {
				SupportVector sv = getSupportVector(i);
				if (sv != null) {
					double[] x = sv.getX();
					double alpha = sv.getAlpha();
					double y = sv.getY();
					for (int j = 0; j < w.length; j++) {
						w[j] += y * alpha * x[j];
					}
				} else {
					showWeights = false;
				}
			}
			if (showWeights) {
				for (int j = 0; j < w.length; j++) {
					result.append("w[" + attributeNames[j] + (!attributeNames[j].equals(attributeConstructions[j])? " = " + attributeConstructions[j] : "")+ "] = " + Tools.formatNumber(w[j]) + Tools.getLineSeparator());
				}
			}
		} else {
			result.append("Feature weight calculation only possible for two class learning problems."+Tools.getLineSeparator()+"Please use the operator SVMWeighting instead." + Tools.getLineSeparator());
		}
		return result.toString();
	}

	public DataTable createWeightsTable() {
		String[] attributeNames = com.rapidminer.example.Tools.getRegularAttributeNames(getTrainingHeader());
		
		SimpleDataTable weightTable = new SimpleDataTable("Kernel Model Weights", new String[] { "Attribute", "Weight" } );
		if ((!getLabel().isNominal()) || (getLabel().getMapping().size() == 2)) {
			double[] w = new double[getNumberOfAttributes()];
			boolean showWeights = true;
			for (int i = 0; i < getNumberOfSupportVectors(); i++) {
				SupportVector sv = getSupportVector(i);
				if (sv != null) {
					double[] x = sv.getX();
					double alpha = sv.getAlpha();
					double y = sv.getY();
					for (int j = 0; j < w.length; j++) {
						w[j] += y * alpha * x[j];
					}
				} else {
					showWeights = false;
				}
			}
			if (showWeights) {
				for (int j = 0; j < w.length; j++) {
					int nameIndex = weightTable.mapString(0, attributeNames[j]);
					weightTable.add(new SimpleDataTableRow(new double[] { nameIndex, w[j]}));
				}
				return weightTable;
			} else {
				return null;
			}
		} else {
			return null;
		}
	}
}
