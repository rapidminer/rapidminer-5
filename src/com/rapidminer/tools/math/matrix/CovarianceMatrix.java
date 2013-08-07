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
package com.rapidminer.tools.math.matrix;

import Jama.Matrix;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;

/**
 * This helper class can be used to calculate a covariance matrix from given
 * matrices or example sets.
 * 
 * @author Regina Fritsch, Ingo Mierswa
 */
public class CovarianceMatrix {


    /** Transforms the example set into a double matrix (doubling the amount of used memory)
     *  and invokes {@link #getCovarianceMatrix(double[][])}. */
    public static Matrix getCovarianceMatrix(ExampleSet exampleSet) {
        double[][] data = new double[exampleSet.size()][exampleSet.getAttributes().size()];
        int r = 0;
        for (Example example : exampleSet) {
            int c = 0;
            for (Attribute attribute : exampleSet.getAttributes()) {
                data[r][c] = example.getValue(attribute);
                c++;
            }
            r++;
        }

        return getCovarianceMatrix(data);
    }

    /** Returns the covariance matrix from the given double matrix. */
    public static Matrix getCovarianceMatrix(double[][] data) {
        // checks
        if (data.length == 0) {
            throw new IllegalArgumentException("Calculation of covariance matrices not possible for data sets with zero rows.");
        }

        int numberOfColumns = -1;
        for (int r = 0; r < data.length; r++) {
            if (numberOfColumns < 0) {
                numberOfColumns = data[r].length;
            } else {
                if (numberOfColumns != data[r].length) {
                    throw new IllegalArgumentException("Calculation of covariance matrices not possible for data sets with different numbers of columns.");
                }
            }
        }

        if (numberOfColumns <= 0) {
            throw new IllegalArgumentException("Calculation of covariance matrices not possible for data sets with zero columns.");
        }

        // subtract column-averages
        for (int c = 0; c < numberOfColumns; c++) {
            double average = getAverageForColumn(data, c);
            for (int r = 0; r < data.length; r++) {
                data[r][c] -= average;
            }
        }

        // create covariance matrix
        double[][] covarianceMatrixEntries = new double[numberOfColumns][numberOfColumns];

        // fill the covariance matrix
        for (int i = 0; i < covarianceMatrixEntries.length; i++) {
            for (int j = i; j < covarianceMatrixEntries.length; j++) {
                double covariance = getCovariance(data, i, j);
                covarianceMatrixEntries[i][j] = covariance;
                covarianceMatrixEntries[j][i] = covariance;
            }
        }

        return new Matrix(covarianceMatrixEntries);
    }

    /** Returns the average for the column with the given index. */
    private static double getAverageForColumn(double[][] data, int column) {
        double sum = 0.0d;
        for (int r = 0; r < data.length; r++) {
            sum += data[r][column];
        }
        return sum / data.length;
    }

    /** Returns the covariance between the given columns. */
    private static double getCovariance(double[][] data, int x, int y) {
        double cov = 0;
        for (int i = 0; i < data.length; i++) {
            cov += data[i][x] * data[i][y];
        }
        return cov / (data.length - 1);
    }
}
