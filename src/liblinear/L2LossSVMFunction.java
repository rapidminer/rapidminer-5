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

public class L2LossSVMFunction implements Function {

   private final Problem  prob;
   private final double[] C;
   private final int[]    I;
   private final double[] z;

   private int            sizeI;

   public L2LossSVMFunction( Problem prob, double Cp, double Cn ) {
      int i;
      int l = prob.l;
      int[] y = prob.y;

      this.prob = prob;

      z = new double[l];
      C = new double[l];
      I = new int[l];

      for ( i = 0; i < l; i++ ) {
         if ( y[i] == 1 )
            C[i] = Cp;
         else
            C[i] = Cn;
      }
   }

   public double fun( double[] w ) {
      int i;
      double f = 0;
      int[] y = prob.y;
      int l = prob.l;
      int n = prob.n;

      Xv(w, z);
      for ( i = 0; i < l; i++ ) {
         z[i] = y[i] * z[i];
         double d = 1 - z[i];
         if ( d > 0 ) f += C[i] * d * d;
      }
      f = 2 * f;
      for ( i = 0; i < n; i++ )
         f += w[i] * w[i];
      f /= 2.0;

      return (f);
   }

   public int get_nr_variable() {
      return prob.n;
   }

   public void grad( double[] w, double[] g ) {
      int i;
      int[] y = prob.y;
      int l = prob.l;
      int n = prob.n;

      sizeI = 0;
      for ( i = 0; i < l; i++ ) {
         if ( z[i] < 1 ) {
            z[sizeI] = C[i] * y[i] * (z[i] - 1);
            I[sizeI] = i;
            sizeI++;
         }
      }
      subXTv(z, g);

      for ( i = 0; i < n; i++ )
         g[i] = w[i] + 2 * g[i];
   }

   public void Hv( double[] s, double[] Hs ) {
      int i;
      int l = prob.l;
      int n = prob.n;
      double[] wa = new double[l];

      subXv(s, wa);
      for ( i = 0; i < sizeI; i++ )
         wa[i] = C[I[i]] * wa[i];

      subXTv(wa, Hs);
      for ( i = 0; i < n; i++ )
         Hs[i] = s[i] + 2 * Hs[i];
   }

   private void subXTv( double[] v, double[] XTv ) {
      int i;
      int n = prob.n;

      for ( i = 0; i < n; i++ )
         XTv[i] = 0;

      for ( i = 0; i < sizeI; i++ ) {
         for ( FeatureNode s : prob.x[I[i]] ) {
            XTv[s.index - 1] += v[i] * s.value;
         }
      }
   }

   private void subXv( double[] v, double[] Xv ) {

      for ( int i = 0; i < sizeI; i++ ) {
         Xv[i] = 0;
         for ( FeatureNode s : prob.x[I[i]] ) {
            Xv[i] += v[s.index - 1] * s.value;
         }
      }
   }

   private void Xv( double[] v, double[] Xv ) {

      for ( int i = 0; i < prob.l; i++ ) {
         Xv[i] = 0;
         for ( FeatureNode s : prob.x[i] ) {
            Xv[i] += v[s.index - 1] * s.value;
         }
      }
   }

}
