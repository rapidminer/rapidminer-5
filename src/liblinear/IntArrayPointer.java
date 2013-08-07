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


final class IntArrayPointer {

   private final int[] _array;
   private int         _offset;


   public void setOffset( int offset ) {
      if ( offset < 0 || offset >= _array.length ) throw new IllegalArgumentException("offset must be between 0 and the length of the array");
      _offset = offset;
   }

   public IntArrayPointer( final int[] array, final int offset ) {
      _array = array;
      setOffset(offset);
   }

   public int get( final int index ) {
      return _array[_offset + index];
   }

   public void set( final int index, final int value ) {
      _array[_offset + index] = value;
   }
}
