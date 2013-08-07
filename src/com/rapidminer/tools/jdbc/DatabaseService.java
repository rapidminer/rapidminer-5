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
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.Charset;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.rapidminer.RapidMiner;
import com.rapidminer.io.process.XMLTools;
import com.rapidminer.tools.FileSystemService;
import com.rapidminer.tools.I18N;
import com.rapidminer.tools.LogService;
import com.rapidminer.tools.ParameterService;
import com.rapidminer.tools.Tools;
import com.rapidminer.tools.XMLException;

/**
 * This service class dynamically registers (additional) JDBC drivers. Please note that drivers
 * cannot be created by Class.forName() but will just be instantiated automatically via
 * DriverManager.getConnection(...).
 * 
 * @author Ingo Mierswa
 *
 */
public class DatabaseService {

	private static final String LOCALHOST_TEST_SUFFIX = "localhost/test";
	private static List<JDBCProperties> jdbcProperties = new ArrayList<JDBCProperties>();

	public static void init() {
		// use the delivered default properties in the resources (e.g. in the jar file)
		URL propertyURL = Tools.getResource("jdbc_properties.xml");
		if (propertyURL != null) {
			InputStream in = null;
			try {
				in = propertyURL.openStream();
				loadJDBCProperties(in, "resource jdbc_properties.xml", false);
			} catch (IOException e) {
				//LogService.getRoot().log(Level.WARNING, "Cannot load JDBC properties from program resources.", e);
				LogService.getRoot().log(Level.WARNING,
						I18N.getMessage(LogService.getRoot().getResourceBundle(),
								"com.rapidminer.tools.jdbc.DatabaseService.loading_jdbc_properties_error_from_program_resources"),
						e);
			} finally {
				if (in != null) {
					try {
						in.close();
					} catch (IOException e) {
						//LogService.getRoot().log(Level.WARNING, "Cannot close connection for JDBC properties file in the resources.", e);
						LogService.getRoot().log(Level.WARNING,
								I18N.getMessage(LogService.getRoot().getResourceBundle(),
										"com.rapidminer.tools.jdbc.DatabaseService.closing_jdbc_connection_error_in_resources"),
								e);
					}
				}
			}
		}

		if (RapidMiner.getExecutionMode().canAccessFilesystem()) {
			File globalJDBCFile = ParameterService.getGlobalConfigFile("jdbc_properties.xml");
			if (globalJDBCFile != null) {
				loadJDBCProperties(globalJDBCFile, false);
			}

			File userProperties = getUserJDBCPropertiesFile();
			if ((userProperties != null) && userProperties.exists()) {
				loadJDBCProperties(userProperties, true);
			}
		} else {
			//LogService.getRoot().config("Ignoring jdbc_properties.xml files in execution mode "+RapidMiner.getExecutionMode()+".");
			LogService.getRoot().log(Level.CONFIG, "com.rapidminer.tools.jdbc.DatabaseService.ignoring_jdbc_properties_xml_file", RapidMiner.getExecutionMode());
		}
	}

	private static File getUserJDBCPropertiesFile() {
		return FileSystemService.getUserConfigFile("jdbc_properties.xml");
	}

	public static void loadJDBCProperties(File jdbcProperties, boolean userDefined) {
		InputStream in = null;
		try {
			in = new FileInputStream(jdbcProperties);
			loadJDBCProperties(in, jdbcProperties.getAbsolutePath(), userDefined);
		} catch (IOException e) {
			//LogService.getRoot().log(Level.WARNING, "Cannot load JDBC properties from etc directory.", e);
			LogService.getRoot().log(Level.WARNING,
					I18N.getMessage(LogService.getRoot().getResourceBundle(),
							"com.rapidminer.tools.jdbc.DatabaseService.loading_jdbc_properties_error_from_directory"),
					e);
		} finally {
			if (in != null) {
				try {
					in.close();
				} catch (IOException e) {
					//LogService.getRoot().log(Level.WARNING, "Cannot close connection for JDBC properties file in the etc directory.", e);
					LogService.getRoot().log(Level.WARNING,
							I18N.getMessage(LogService.getRoot().getResourceBundle(),
									"com.rapidminer.tools.jdbc.DatabaseService.closing_jdbc_connection_error_in_directory"),
							e);
				}
			}
		}
	}

	/*
	private static void registerAllJDBCDrivers(File jdbcDirectory, boolean searchForJDBDriversInLibDirectory, boolean searchForJDBCDriversInClasspath) {
		if (!RapidMiner.getExecutionMode().canAccessFilesystem()) {
			LogService.getRoot().config("Skipping JDBC driver registration in execution mode "+RapidMiner.getExecutionMode()+".");
			return;
		}
		if (searchForJDBDriversInLibDirectory) {
			if ((jdbcDirectory != null) && (jdbcDirectory.exists())) {
				File[] allFiles = jdbcDirectory.listFiles();
				if (allFiles != null) {
					for (File f : allFiles) {
						if ((f.getName().endsWith(".jar")) || ((f.getName().endsWith(".zip")))) {
							registerDynamicJDBCDrivers(f);
						}
					}
				}
			}
		}

	    if (searchForJDBCDriversInClasspath) {
	    	String classpath = System.getProperty("java.class.path");
	    	String pathComponents[] = classpath.split(File.pathSeparator);
	    	for (int i = 0; i < pathComponents.length; i++) {
	    		String path = pathComponents[i].trim();
	    		if ((path.endsWith(".jar")) || ((path.endsWith(".zip")))) {
	    			registerClasspathJDBCDrivers(new File(path).getAbsoluteFile());
	    		}
	    	}
	    }
	}
	 */
	/*
	private static void registerDynamicJDBCDrivers(File file) {
		URLClassLoader ucl = null;
		try {
			URL u = new URL("jar:file:" + file.getAbsolutePath() + "!/");
			ucl = new URLClassLoader(new URL[] { u });
		} catch (MalformedURLException e) {
			LogService.getRoot().log(Level.WARNING, "DatabaseService: cannot create class loader for file '" + file + "': " + e.getMessage(), e);
		}

		if (ucl != null) {
			try {
				JarFile jarFile = new JarFile(file);
				List<String> driverNames = new LinkedList<String>();
				Tools.findImplementationsInJar(ucl, jarFile, java.sql.Driver.class, driverNames);
				Iterator<String> i = driverNames.iterator();
				while (i.hasNext()) {
					String driverName = i.next();
					registerDynamicJDBCDriver(ucl, driverName);
				}
			} catch (Exception e) {
				LogService.getRoot().log(Level.WARNING, "DatabaseService: cannot register drivers for file '" + file + "': " + e.getMessage(), e);
			}
		}
	}
	 */
	/*
	private static void registerDynamicJDBCDriver(ClassLoader ucl, String driverName) throws ClassNotFoundException, InstantiationException, IllegalAccessException, SQLException {
	    if (!driverName.equals(DriverAdapter.class.getName())) {
	        Driver d = (Driver)Class.forName(driverName, true, ucl).newInstance();
	        DriverManager.registerDriver(new DriverAdapter(d));
	    }
	}
	 */
	/*
	private static void registerClasspathJDBCDrivers(File file) {
	    JarFile jarFile = null;
	    try {
	        jarFile = new JarFile(file);
	    } catch (IOException e) {
	        LogService.getRoot().log(Level.WARNING, "DatabaseService: cannot register drivers from file '" + file + "': " + e.getMessage(), e);
	        return;
	    }
	    List<String> driverNames = new LinkedList<String>();
	    Tools.findImplementationsInJar(jarFile, java.sql.Driver.class, driverNames);
	    Iterator<String> i = driverNames.iterator();
	    while (i.hasNext()) {
	        String driverName = i.next();
	        try {
	            Class.forName(driverName);
	        } catch (Exception e) {
	            LogService.getRoot().log(Level.WARNING, "DatabaseService: cannot register driver '"+driverName+"' from file '" + file + "': " + e.getMessage(), e);
	        }
	    }

	}
	 */
	public static void loadJDBCProperties(InputStream in, String name, boolean userDefined) {
		//jdbcProperties.clear();
		//LogService.getRoot().config("Loading JDBC driver information from '" + name + "'.");
		LogService.getRoot().log(Level.CONFIG, "com.rapidminer.tools.jdbc.DatabaseService.loading_jdbc_driver_information", name);
		Document document = null;
		try {
			document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(in);
		} catch (Exception e) {
			//LogService.getRoot().log(Level.WARNING, "Cannot read JDBC driver description file '" + name + "': no valid XML: " + e.getMessage(), e);
			LogService.getRoot().log(Level.WARNING,
					I18N.getMessage(LogService.getRoot().getResourceBundle(),
							"com.rapidminer.tools.jdbc.DatabaseService.reading_jdbc_driver_description_file_error",
							name, e.getMessage()),
					e);
		}
		if (document != null) {
			if (!document.getDocumentElement().getTagName().toLowerCase().equals("drivers")) {
				//LogService.getRoot().log(Level.WARNING, "JDBC driver description file '" + name + "': outermost tag must be <drivers>!");
				LogService.getRoot().log(Level.WARNING, "com.rapidminer.tools.jdbc.DatabaseService.reading_jdbc_driver_description_file_outermost_tag_error", name);
				return;
			}

			NodeList driverTags = document.getDocumentElement().getElementsByTagName("driver");
			for (int i = 0; i < driverTags.getLength(); i++) {
				Element currentElement = (Element) driverTags.item(i);
				try {
					addDriverInformation(currentElement, userDefined);
				} catch (Exception e) {
					Attr currentNameAttr = currentElement.getAttributeNode("name");
					if (currentNameAttr != null) {
						//LogService.getRoot().log(Level.WARNING, "JDBC driver description: cannot register '" + currentNameAttr.getValue() + "': " + e, e);
						LogService.getRoot().log(Level.WARNING,
								I18N.getMessage(LogService.getRoot().getResourceBundle(),
										"com.rapidminer.tools.jdbc.DatabaseService.registering_jdbc_driver_description_error",
										currentNameAttr.getValue(), e),
								e);
					} else {
						//LogService.getRoot().log(Level.WARNING, "JDBC driver registration: cannot register '" + currentElement + "': " + e, e);
						LogService.getRoot().log(Level.WARNING,
								I18N.getMessage(LogService.getRoot().getResourceBundle(),
										"com.rapidminer.tools.jdbc.DatabaseService.registering_jdbc_driver_description_error",
										currentElement, e),
								e);
					}
				}
			}
		}
	}

	private static void addDriverInformation(Element driverElement, boolean userDefined) throws Exception {
		JDBCProperties properties = new JDBCProperties(driverElement, userDefined);
		properties.registerDrivers();
		for (JDBCProperties other : jdbcProperties) {
			if (other.getName().equals(properties.getName())) {
				//LogService.getRoot().config("Merging jdbc driver information for "+other.getName());            	
				LogService.getRoot().log(Level.CONFIG, "com.rapidminer.tools.jdbc.DatabaseService.merging_jdbc_driver_information", other.getName());
				other.merge(properties);
				return;
			}
		}
		jdbcProperties.add(properties);
	}

	private static Enumeration<Driver> getAllDrivers() {
		return DriverManager.getDrivers();
	}

	public static DriverInfo[] getAllDriverInfos() {
		List<DriverInfo> predefinedDriverList = new LinkedList<DriverInfo>();
		// Find driver for all defined JDBCProperties
		for (JDBCProperties properties : getJDBCProperties()) {
			Enumeration<Driver> drivers = getAllDrivers();
			boolean accepted = false;
			while (drivers.hasMoreElements()) {
				Driver driver = drivers.nextElement();
				try {
					if (driver.acceptsURL(properties.getUrlPrefix() + LOCALHOST_TEST_SUFFIX)) {
						DriverInfo info = new DriverInfo(driver, properties);
						predefinedDriverList.add(info);
						accepted = true;
						break;
					}
				} catch (SQLException e) {
					// do nothing
				}
			}
			// found no driver, add empty info
			if (!accepted) {
				predefinedDriverList.add(new DriverInfo(null, properties));
			}
		}

		// now, find drivers for which we don't have JDBCProperties
		List<DriverInfo> driverList = new LinkedList<DriverInfo>();
		Enumeration<Driver> drivers = getAllDrivers();
		while (drivers.hasMoreElements()) {
			Driver driver = drivers.nextElement();
			boolean accepted = true;
			for (DriverInfo predefinedInfo : predefinedDriverList) {
				if ((predefinedInfo.getDriver() != null) && (predefinedInfo.getDriver().equals(driver))) {
					accepted = false;
					break;
				}
			}
			if (accepted) {
				//		    	if ((!info.getShortName().startsWith("NonRegistering")) &&
				//		    			(!info.getShortName().startsWith("Replication"))) {
				driverList.add(new DriverInfo(driver, null));
			}
		}

		driverList.addAll(predefinedDriverList);
		Collections.sort(driverList);

		DriverInfo[] driverArray = new DriverInfo[driverList.size()];
		driverList.toArray(driverArray);
		return driverArray;
	}

	public static JDBCProperties getJDBCProperties(String name) {
		for (JDBCProperties properties : jdbcProperties) {
			if (properties.getName().equals(name)) {
				return properties;
			}
		}
		return null;
	}

	//	@Deprecated
	//	public static JDBCProperties getJProperties(String systemName) {
	//		JDBCProperties result = null;
	//		for (JDBCProperties properties : jdbcProperties) {
	//			if (properties.getName().equalsIgnoreCase(systemName)) {
	//				result = properties;
	//				break;
	//			}
	//		}
	//		return result;
	//	}

	public static List<JDBCProperties> getJDBCProperties() {
		return jdbcProperties;
	}

	public static void addJDBCProperties(JDBCProperties newProps) {
		jdbcProperties.add(newProps);
	}

	public static void removeJDBCProperties(JDBCProperties newProps) {
		jdbcProperties.remove(newProps);
	}

	public static String[] getDBSystemNames() {
		String[] names = new String[jdbcProperties.size()];
		int counter = 0;
		Iterator<JDBCProperties> i = jdbcProperties.iterator();
		while (i.hasNext()) {
			names[counter++] = i.next().getName();
		}
		return names;
	}

	public static void saveUserDefinedProperties() throws XMLException {
		Document doc;
		try {
			doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
		} catch (ParserConfigurationException e) {
			throw new XMLException("Failed to create document: " + e, e);
		}
		Element root = doc.createElement("drivers");
		doc.appendChild(root);
		for (JDBCProperties props : getJDBCProperties()) {
			if (props.isUserDefined()) {
				root.appendChild(props.getXML(doc));
			}
		}
		XMLTools.stream(doc, getUserJDBCPropertiesFile(), Charset.forName("UTF-8"));
	}

	//    /** Sets whether the lib directory is scanned for JDBC drivers. */
	//    public static void setScanLibForJDBCDrivers(boolean scan) {
	//    	System.setProperty(RapidMiner.PROPERTY_RAPIDMINER_INIT_JDBC_LIB, Boolean.toString(scan));
	//    }
	//
	//    /** Sets whether the entire classpath is scanned for JDBC drivers (very time consuming). */
	//    public static void setScanClasspathForJDBCDrivers(boolean scan) {
	//    	System.setProperty(RapidMiner.PROPERTY_RAPIDMINER_INIT_JDBC_CLASSPATH, Boolean.toString(scan));
	//    }
	//
	//    /** Sets the firectory to scan for JDBC drivers. */
	//    public static void setJDBCDriversLibDirectory(String directory) {
	//    	System.setProperty(RapidMiner.PROPERTY_RAPIDMINER_INIT_JDBC_LIB_LOCATION, directory);
	//    }
}
