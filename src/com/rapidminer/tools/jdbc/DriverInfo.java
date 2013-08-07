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
package com.rapidminer.tools.jdbc;

import java.sql.Driver;

/** Some basic information about a JDBC driver. 
 * 
 *  @author Ingo Mierswa
 */
public class DriverInfo implements Comparable<DriverInfo> {
	
	private final Driver driver;
	
	private JDBCProperties properties;
	public DriverInfo(Driver driver, JDBCProperties properties) {
		this.driver = driver;
		this.properties = properties;

//		if (driver instanceof DriverAdapter) {
//            this.shortName = driver.toString();                
//        } else {
//        	this.shortName = driver.getClass().getSimpleName();
//        }
//        
//        if (driver instanceof DriverAdapter) {
//            this.longName = ((DriverAdapter)driver).toLongString();             
//        } else {
//        	this.longName = driver.getClass().getName();
//        }
	}
	
//	public DriverInfo(String shortName) {
//		this(shortName, null);
//	}
//	
//	public DriverInfo(String shortName, String longName) {
//		this.driver = null;
//		this.shortName = shortName;
//		this.longName = longName;
//	}
//	
	public Driver getDriver() {
		return this.driver;
	}
	
//	public void setShortName(String shortName) {
//		this.shortName = shortName;
//	}
	
	public String getShortName() {
		if (properties != null) {
			return this.properties.getName();
//		} else if (driver != null) {
//			return driver.getClass().getName();
		} else {
			return "Unknown";
		}
	}
	
	public String getClassName() {
		if (driver != null) {
			if (driver instanceof DriverAdapter) {
				return ((DriverAdapter)this.driver).toLongString();
			} else {
				return this.driver.getClass().getName();
			}
		} else {
			return null;
		}
	}
	
	@Override
	public String toString() {
		return getShortName() + " (" + getClassName() + ")";
	}

	public int compareTo(DriverInfo o) {
		int c = this.getShortName().compareTo(o.getShortName());
		if (c != 0) {
			return c;
		} else {
			String cn1 = this.getClassName();
			String cn2 = o.getClassName();
			if ((cn1 != null) && (cn2 != null)) {
				return cn1.compareTo(cn2);
			} else {
				if (cn1 == null) {
					return 1;
				} else {
					return -1;
				}
			}
		}
	}
	
	@Override
	public boolean equals(Object o) {
		if (!(o instanceof DriverInfo))
			return false;
		DriverInfo a = (DriverInfo) o;
		if (!this.getShortName().equals(a.getShortName()))
			return false;		
		return a.getDriver() == this.getDriver();
	}
	
    @Override
	public int hashCode() {
    	return this.getShortName().hashCode();
    }

	public JDBCProperties getProperties() {
		return properties;
	}
}
