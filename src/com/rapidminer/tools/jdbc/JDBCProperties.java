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

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.AccessController;
import java.security.PrivilegedExceptionAction;
import java.sql.Driver;
import java.sql.DriverManager;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.rapidminer.tools.I18N;
import com.rapidminer.tools.LogService;
import com.rapidminer.tools.Tools;

/**
 * This class encapsulates the necessary information to build a JDBC connection string (url) for a specific database
 * system.
 * 
 * @author Ingo Mierswa
 */
public class JDBCProperties {

	private String name;
	private String defaultPort;
	private String urlPrefix;
	private String dbNameSeperator;
	// private String integerName;
	// private String realName;
	// private String varcharName;
	// private String textName;
	// private String dateTimeName;
	// private String timeName;
	// private String dateName;
	// private String identifierQuoteOpen;
	// private String identifierQuoteClose;
	// private String valueQuoteOpen;
	// private String valueQuoteClose;
	private String[] drivers;
	private String driverJarFile;
	private boolean userDefined;

	private JDBCProperties() {
		this(false);
	}

	public JDBCProperties(boolean userDefined) {
		setName("unknown");
		setDefaultPort("port");
		setUrlPrefix("urlprefix://");
		this.dbNameSeperator = "/";
		this.userDefined = userDefined;
		// varcharName = "VARCHAR";
		// textName = "BLOB";
		// integerName = "INTEGER";
		// realName = "REAL";
		// timeName = "TIME";
		// dateName = "DATE";
		// dateTimeName = "TIMESTAMP";
		// identifierQuoteOpen = "\"";
		// identifierQuoteClose = "\"";
		// valueQuoteOpen = "'";
		// valueQuoteClose = "'";
	}

	/**
	 * @param driverElement
	 * @throws Exception
	 */
	public JDBCProperties(Element driverElement, boolean userDefined) throws Exception {
		this.userDefined = userDefined;
		Attr nameAttr = driverElement.getAttributeNode("name");
		Attr driversAttr = driverElement.getAttributeNode("drivers");
		Attr portAttr = driverElement.getAttributeNode("defaultport");
		Attr urlAttr = driverElement.getAttributeNode("urlprefix");
		Attr dbNameAttr = driverElement.getAttributeNode("dbnameseparator");
		if (dbNameAttr == null) {
			// old version with typo
			dbNameAttr = driverElement.getAttributeNode("dbnameseperator");
		}
		// Attr varcharNameAttr = driverElement.getAttributeNode("type_varchar");
		// Attr textNameAttr = driverElement.getAttributeNode("type_text");
		// Attr integerNameAttr = driverElement.getAttributeNode("type_integer");
		// Attr realNameAttr = driverElement.getAttributeNode("type_real");
		// Attr timeNameAttr = driverElement.getAttributeNode("type_time");
		// Attr dateNameAttr = driverElement.getAttributeNode("type_date");
		// Attr dateTimeNameAttr = driverElement.getAttributeNode("type_dateTime");
		// Attr identifierQuoteOpenAttr = driverElement.getAttributeNode("identifier_quote_open");
		// Attr identifierQuoteCloseAttr = driverElement.getAttributeNode("identifier_quote_close");
		// Attr valueQuoteOpenAttr = driverElement.getAttributeNode("value_quote_open");
		// Attr valueQuoteCloseAttr = driverElement.getAttributeNode("value_quote_close");
		Attr driverJarAttr = driverElement.getAttributeNode("driver_jar");

		if (nameAttr == null)
			throw new Exception("Missing name for <driver> tag");

		setName(nameAttr.getValue());

		if (portAttr == null)
			throw new Exception("Missing defaultport for <driver> tag for driver '" + getName() + "'");
		if (urlAttr == null)
			throw new Exception("Missing urlprefix for <driver> tag for driver '" + getName() + "'");
		if (driversAttr == null) {
			//LogService.getRoot().warning("Missing database driver class name for '" + getName() + "'");
			LogService.getRoot().log(Level.WARNING, "com.rapidminer.tools.jdbc.JDBCProperties.missing_database_driver_class", getName());
		}

		// if (varcharNameAttr != null) {
		// this.varcharName = varcharNameAttr.getValue();
		// } else {
		// this.varcharName = "VARCHAR";
		// LogService.getRoot().warning("No definition of 'type_varchar' found for driver " + nameAttr.getValue() +
		// ", using default (VARCHAR)...");
		// }
		//
		// if (textNameAttr != null) {
		// this.textName= textNameAttr.getValue();
		// } else {
		// this.textName = "BLOB";
		// LogService.getRoot().warning("No definition of 'type_text' found for driver " + nameAttr.getValue() +
		// ", using default (BLOB)...");
		// }
		//
		// if (integerNameAttr != null) {
		// this.integerName = integerNameAttr.getValue();
		// } else {
		// this.integerName= "INTEGER";
		// LogService.getRoot().warning("No definition of 'type_integer' found for driver " + nameAttr.getValue() +
		// ", using default (INTEGER)...");
		// }

		// if (realNameAttr != null) {
		// this.realName = realNameAttr.getValue();
		// } else {
		// this.realName= "REAL";
		// LogService.getRoot().warning("No definition of 'type_real' found for driver " + nameAttr.getValue() +
		// ", using default (REAL)...");
		// }
		//
		// if (timeNameAttr != null) {
		// this.timeName= timeNameAttr.getValue();
		// } else {
		// this.timeName = "TIME";
		// LogService.getRoot().warning("No definition of 'type_time' found for driver " + nameAttr.getValue() +
		// ", using default (TIME)...");
		// }
		//
		// if (dateNameAttr != null) {
		// this.dateName = dateNameAttr.getValue();
		// } else {
		// this.dateName= "DATE";
		// LogService.getRoot().warning("No definition of 'type_date' found for driver " + nameAttr.getValue() +
		// ", using default (DATE)...");
		// }
		// if (dateTimeNameAttr != null) {
		// this.dateTimeName = dateTimeNameAttr.getValue();
		// } else {
		// this.dateTimeName = "TIMESTAMP";
		// LogService.getRoot().warning("No definition of 'type_dateTime' found for driver " + nameAttr.getValue() +
		// ", using default (TIMESTAMP)...");
		// }

		// if (identifierQuoteOpenAttr != null) {
		// this.identifierQuoteOpen = identifierQuoteOpenAttr.getValue();
		// } else {
		// this.identifierQuoteOpen = "\"";
		// LogService.getRoot().warning("No definition of 'identifier_quote_open' found for driver " +
		// nameAttr.getValue() + ", using default (\")...");
		// }
		//
		// if (identifierQuoteCloseAttr != null) {
		// this.identifierQuoteClose = identifierQuoteCloseAttr.getValue();
		// } else {
		// this.identifierQuoteClose = "\"";
		// LogService.getRoot().warning("No definition of 'identifier_quote_close' found for driver " +
		// nameAttr.getValue() + ", using default (\")...");
		// }

		// if (valueQuoteOpenAttr != null) {
		// this.valueQuoteOpen = valueQuoteOpenAttr.getValue();
		// } else {
		// this.valueQuoteOpen = "'";
		// LogService.getRoot().warning("No definition of 'value_quote_open' found for driver " + nameAttr.getValue() +
		// ", using default (')...");
		// }

		// this.valueQuoteClose = "'";
		// if (valueQuoteCloseAttr != null) {
		// this.valueQuoteClose = valueQuoteCloseAttr.getValue();
		// } else {
		// LogService.getRoot().warning("No definition of 'value_quote_close' found for driver " + nameAttr.getValue() +
		// ", using default (')...");
		// }

		setDefaultPort(portAttr.getValue());
		setUrlPrefix(urlAttr.getValue());
		dbNameSeperator = dbNameAttr != null ? dbNameAttr.getValue() : "/";

		if (driversAttr != null) {
			final String value = driversAttr.getValue();
			setDriverClasses(value);
		} else {
			this.drivers = new String[0];
		}
		if (driverJarAttr != null) {
			this.setDriverJarFile(driverJarAttr.getValue());
		} else {
			this.setDriverJarFile(null);
		}
	}

	public void setDriverClasses(String value) {
		if (value == null) {
			this.drivers = new String[0];
		} else {
			this.drivers = value.split("\\s*,\\s*");
		}
	}

	/** Overrides all fields specified by other. */
	public void merge(JDBCProperties other) {
		if (other.getDefaultPort() != null)
			this.setDefaultPort(other.getDefaultPort());
		if (other.getUrlPrefix() != null)
			this.setUrlPrefix(other.getUrlPrefix());
		if (other.dbNameSeperator != null)
			this.dbNameSeperator = other.dbNameSeperator;
		this.userDefined = this.userDefined || other.userDefined;
		if (other.getDriverJarFile() != null) {
			if (this.getDriverJarFile() == null) {
				this.setDriverJarFile(other.getDriverJarFile());
			} else {
				this.setDriverJarFile(other.getDriverJarFile() + "," + this.getDriverJarFile());
			}
		}
		if (other.drivers != null) {
			if (this.drivers == null) {
				this.drivers = other.drivers;
			} else {
				Set<String> merged = new HashSet<String>();
				merged.addAll(Arrays.asList(this.drivers));
				merged.addAll(Arrays.asList(other.drivers));
				this.drivers = merged.toArray(new String[merged.size()]);
			}
		}
	}

	public String getDbNameSeperator() {
		return dbNameSeperator;
	}

	public String getDefaultPort() {
		return defaultPort;
	}

	public String getName() {
		return name;
	}

	//
	public String getUrlPrefix() {
		return urlPrefix;
	}

	//
	// public String getIntegerName() {
	// return integerName;
	// }
	//
	// public String getTextName() {
	// return textName;
	// }
	//
	// public String getRealName() {
	// return realName;
	// }
	//
	// public String getVarcharName() {
	// return varcharName;
	// }

	// public String getIdentifierQuoteOpen() {
	// return this.identifierQuoteOpen;
	// }
	//
	// public String getIdentifierQuoteClose() {
	// return this.identifierQuoteClose;
	// }

	// @Deprecated
	// public String getValueQuoteOpen() {
	// return this.valueQuoteOpen;
	// }
	//
	// @Deprecated
	// public String getValueQuoteClose() {
	// return this.valueQuoteClose;
	// }

	// public String getDateTimeName() {
	// return dateTimeName;
	// }
	//
	// public String getTimeName() {
	// return timeName;
	// }
	//
	// public String getDateName() {
	// return dateName;
	// }

	public static JDBCProperties createDefaultJDBCProperties() {
		return new JDBCProperties();
	}

	public void registerDrivers() {
		for (String driverName : drivers) {
			try {
				ClassLoader loader;
				if (getDriverJarFile() != null) {
					String[] jarNames = getDriverJarFile().split(",");
					final URL urls[] = new URL[jarNames.length];
					for (int i = 0; i < jarNames.length; i++) {
						File jarFile = new File(jarNames[i]);
						if (!jarFile.exists()) {
							//LogService.getRoot().warning("Driver jar file '" + jarFile.getAbsolutePath() + "' referenced for JDBC driver " + getName() + " does not exist.");
							LogService.getRoot().log(Level.WARNING, "com.rapidminer.tools.jdbc.JDBCProperties.driver_jar_file_does_not_exist",
									new Object[] {jarFile.getAbsolutePath(), getName()});						
						}
						urls[i] = jarFile.toURI().toURL();
					}
					loader = AccessController.doPrivileged(new PrivilegedExceptionAction<ClassLoader>() {
						public ClassLoader run() throws Exception {
							return new URLClassLoader(urls);
						}
					});
				} else {
					loader = getClass().getClassLoader();
				}

				// Normally, forName() is sufficient, but when loaded dynamically, the DriverManager
				// does not accept it, so we use the DriverAdapter, see http://www.kfu.com/~nsayer/Java/dyn-jdbc.html
				if (getDriverJarFile() == null) {
					Class.forName(driverName, true, loader);
				} else {
					DriverManager.registerDriver(new DriverAdapter((Driver) Class.forName(driverName, true, loader).newInstance()));
				}
				// Class.forName(driverName, true, loader).newInstance();

				if (getDriverJarFile() != null) {
					//LogService.getRoot().config("Loaded JDBC driver " + driverName + " from " + getDriverJarFile());
					LogService.getRoot().log(Level.CONFIG, "com.rapidminer.tools.jdbc.JDBCProperties.loaded_jdbc_driver_from_driverjarfile", 
							new Object[] {driverName, getDriverJarFile()});
				} else {
					//LogService.getRoot().config("Loaded JDBC driver " + driverName);
					LogService.getRoot().log(Level.CONFIG, "com.rapidminer.tools.jdbc.JDBCProperties.loaded_jdbc_driver", driverName);
				}

			} catch (ClassNotFoundException e) {
				if (getDriverJarFile() != null) {
					//LogService.getRoot().info("JDBC driver " + driverName + " not found in " + getDriverJarFile());
					LogService.getRoot().log(Level.INFO, "com.rapidminer.tools.jdbc.JDBCProperties.jdbc_driver_not_found", 
							new Object[] {driverName, getDriverJarFile()});
				} else {
					//LogService.getRoot().info("JDBC driver " + driverName + " not found. Probably the driver is not installed.");
					LogService.getRoot().log(Level.INFO, "com.rapidminer.tools.jdbc.JDBCProperties.jdbc_driver_not_found_not_installed", driverName);
				}
			} catch (Exception e) {
				//LogService.getRoot().log(Level.WARNING, "Failed to register JDBC driver " + driverName + ": " + e, e);
				LogService.getRoot().log(Level.WARNING,
						I18N.getMessage(LogService.getRoot().getResourceBundle(), 
						"com.rapidminer.tools.jdbc.JDBCProperties.jdbc_driver_not_registered", 
						driverName, e),
						e);

			}
		}
	}

	public String[] getDriverClasses() {
		return drivers;
	}

	public String getDriverJarFile() {
		return driverJarFile;
	}

	public boolean isUserDefined() {
		return userDefined;
	}

	@Override
	public String toString() {
		return getName();
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setDefaultPort(String defaultPort) {
		this.defaultPort = defaultPort;
	}

	public void setUrlPrefix(String urlPrefix) {
		this.urlPrefix = urlPrefix;
	}

	public void setDriverJarFile(String driverJarFile) {
		this.driverJarFile = driverJarFile;
	}

	public void setDbNameSeperator(String dbNameSeperator) {
		this.dbNameSeperator = dbNameSeperator;
	}

	public Element getXML(Document doc) {
		Element element = doc.createElement("driver");
		element.setAttribute("name", getName());
		element.setAttribute("drivers", Tools.toString(drivers, ","));
		element.setAttribute("driver_jar", getDriverJarFile());
		element.setAttribute("defaultport", getDefaultPort());
		element.setAttribute("urlprefix", getUrlPrefix());
		element.setAttribute("dbnameseparator", getDbNameSeperator());
		return element;
	}
}
