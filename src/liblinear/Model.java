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
package liblinear;

import java.io.Serializable;
import java.util.Arrays;

import static liblinear.Linear.copyOf;


/**
 * use {@link Linear#loadModel(String)} and {@link Linear#saveModel(String, Model)} to load/save it
 */
public final class Model implements Serializable {

   private static final long serialVersionUID = -6456047576741854834L;

   public double                    bias;

   /** label of each class (label[n]) */
   public int[]                     label;

   /** number of classes */
   public int                       nr_class;

   public int                       nr_feature;

   public SolverType                solverType;

   public double[]                  w;

   public int getNrClass() {
      return nr_class;
   }

   public int getNrFeature() {
      return nr_feature;
   }

   public int[] getLabels() {
      return copyOf(label, nr_class);
   }

   @Override
   public String toString() {
      StringBuilder sb = new StringBuilder("Model");
      sb.append(" bias=").append(bias);
      sb.append(" nr_class=").append(nr_class);
      sb.append(" nr_feature=").append(nr_feature);
      sb.append(" solverType=").append(solverType);
      return sb.toString();
   }

   @Override
   public int hashCode() {
      final int prime = 31;
      int result = 1;
      long temp;
      temp = Double.doubleToLongBits(bias);
      result = prime * result + (int)(temp ^ (temp >>> 32));
      result = prime * result + Arrays.hashCode(label);
      result = prime * result + nr_class;
      result = prime * result + nr_feature;
      result = prime * result + ((solverType == null) ? 0 : solverType.hashCode());
      result = prime * result + Arrays.hashCode(w);
      return result;
   }

   @Override
   public boolean equals( Object obj ) {
      if ( this == obj ) return true;
      if ( obj == null ) return false;
      if ( getClass() != obj.getClass() ) return false;
      Model other = (Model)obj;
      if ( Double.doubleToLongBits(bias) != Double.doubleToLongBits(other.bias) ) return false;
      if ( !Arrays.equals(label, other.label) ) return false;
      if ( nr_class != other.nr_class ) return false;
      if ( nr_feature != other.nr_feature ) return false;
      if ( solverType == null ) {
         if ( other.solverType != null ) return false;
      } else if ( !solverType.equals(other.solverType) ) return false;
      if ( !Arrays.equals(w, other.w) ) return false;
      return true;
   }
}
