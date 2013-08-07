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
package com.rapidminer.gui.tools;


/**
 * Contains information about the different parts of a version number.
 *
 * @author Ingo Mierswa
 */
public class VersionNumber implements Comparable<VersionNumber> {

	private int majorNumber;
	
	private int minorNumber;
	
	private int patchLevel;
	
	private boolean alpha;
	
	private int alphaNumber;
	
	private boolean beta;
	
	private int betaNumber;
	
	public VersionNumber(String versionString) {		
		String version = versionString.toLowerCase().trim();
		int alphaIndex = version.indexOf("alpha");
		if (alphaIndex >= 0) { // alpha
			String[] numbers = version.substring(0, alphaIndex).split("\\.");
			if (numbers.length > 0)
				majorNumber = Integer.parseInt(numbers[0]);
			if (numbers.length > 1)
				minorNumber = Integer.parseInt(numbers[1]);
			if (numbers.length > 2)
				patchLevel = Integer.parseInt(numbers[2]);
			alpha = true;
			String alphaNumberString = version.substring(alphaIndex + "alpha".length());
			if (alphaNumberString.length() > 0) {
				alphaNumber = Integer.parseInt(alphaNumberString);
			}
		} else { // no alpha
			int betaIndex = version.indexOf("beta");
			if (betaIndex >= 0) { // beta
				String[] numbers = version.substring(0, betaIndex).split("\\.");
				if (numbers.length > 0)
					majorNumber = Integer.parseInt(numbers[0]);
				if (numbers.length > 1)
					minorNumber = Integer.parseInt(numbers[1]);
				if (numbers.length > 2)
					patchLevel = Integer.parseInt(numbers[2]);
				beta = true;
				String betaNumberString = version.substring(betaIndex + "beta".length());
				if (betaNumberString.length() > 0) {
					betaNumber = Integer.parseInt(betaNumberString);
				}
			} else { // no beta
				String[] numbers = version.split("\\.");
				if (numbers.length > 0)
					majorNumber = Integer.parseInt(numbers[0]);
				if (numbers.length > 1)
					minorNumber = Integer.parseInt(numbers[1]);
				if (numbers.length > 2)
					patchLevel = Integer.parseInt(numbers[2]);
			}
		}
	}
	
	public VersionNumber(int majorNumber, int minorNumber) {
		this(majorNumber, minorNumber, 0, false, 0, false, 0);
	}
	
	public VersionNumber(int majorNumber, int minorNumber, int patchLevel) {
		this(majorNumber, minorNumber, patchLevel, false, 0, false, 0);
	}
	
	public VersionNumber(int majorNumber, int minorNumber, int patchLevel, boolean alpha, int alphaNumber, boolean beta, int betaNumber) {
		this.majorNumber = majorNumber;
		this.minorNumber = minorNumber;
		this.patchLevel  = patchLevel;
		this.alpha       = alpha;
		this.alphaNumber = alphaNumber;
		this.beta        = beta;
		this.betaNumber  = betaNumber;
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof VersionNumber))
			return false;
		VersionNumber other = (VersionNumber)o;
		return 
		   this.majorNumber == other.majorNumber &&
		   this.minorNumber == other.minorNumber &&
		   this.patchLevel  == other.patchLevel  &&
		   this.alpha       == other.alpha       &&
		   this.alphaNumber == other.alphaNumber &&
		   this.beta        == other.beta        &&
		   this.betaNumber  == other.betaNumber;
	}
	
	@Override
	public int hashCode() {
		return 
		    Double.valueOf(this.majorNumber).hashCode() ^
		    Double.valueOf(this.minorNumber).hashCode() ^
		    Double.valueOf(this.patchLevel).hashCode() ^
		    Boolean.valueOf(alpha).hashCode() ^
		    Double.valueOf(this.alphaNumber).hashCode() ^
		    Boolean.valueOf(beta).hashCode() ^
		    Double.valueOf(this.betaNumber).hashCode();
	}
	
	/**
	 * Returns if this number is at least as high as the given arguments.
	 */
	public boolean isAtLeast(int major, int minor, int buildNumber) {
		return this.compareTo(new VersionNumber(major, minor, buildNumber)) >= 0;
	}

	public boolean isAtLeast(VersionNumber other) {
		return this.compareTo(other) >= 0;
	}

	/**
	 * Returns if this number is at most as high as the given arguments.
	 */
	public boolean isAtMost(int major, int minor, int buildNumber) {
		return this.compareTo(new VersionNumber(major, minor, buildNumber)) <= 0;
	}

	public boolean isAtMost(VersionNumber other) {
		return this.compareTo(other) <= 0;
	}
	
	
	@Override
	public int compareTo(VersionNumber o) {
		int index = Double.compare(this.majorNumber, o.majorNumber);
		if (index != 0) {
			return index;
		} else {
			index = Double.compare(this.minorNumber, o.minorNumber);
			if (index != 0) {
				return index;
			} else {
				index = Double.compare(this.patchLevel, o.patchLevel);
				if (index != 0) {
					return index;
				} else {
					if (this.alpha) {
						if (o.alpha) {
							return Double.compare(this.alphaNumber, o.alphaNumber);	
						} else {
							return -1;
						}
					} else if (this.beta) {
						if (o.alpha) {
							return 1;
						} else if (o.beta) {
							return Double.compare(this.betaNumber, o.betaNumber);	
						} else {
							return -1;
						}
					} else {
						if (o.alpha) {
							return 1;
						} else if (o.beta) {
							return 1;
						} else {
							return 0;
						}
					}
				}
			}
		}
	}
	
	
	
	@Override
	public String toString() {
		String alphaBetaString = "";
		if (alpha) {
			alphaBetaString = "alpha" + (alphaNumber >= 2 ? alphaNumber : "");
		} else if (beta) {
			alphaBetaString = "beta" + (betaNumber >= 2 ? betaNumber : "");
		}
		return majorNumber + "." + minorNumber + "." + "000".substring((patchLevel + "").length()) + patchLevel +  alphaBetaString; 
	}

	/**
	 * Returns the RapidMiner version in the format major.minor.patchlevel, with 3 digits for patchlevel. Example: 5.3.001
	 */
	public String getLongVersion() {
		return toString();
	}

	public String getShortVersion() {
		return majorNumber + "." + minorNumber;
	}
	
	public int getMajorNumber() {
		return majorNumber;
	}
	
	public int getMinorNumber() {
		return minorNumber;
	}
	
	public int getPatchLevel() {
		return patchLevel;
	}
}
