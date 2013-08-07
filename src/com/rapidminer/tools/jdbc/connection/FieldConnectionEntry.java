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
package com.rapidminer.tools.jdbc.connection;

import java.security.Key;
import java.util.Properties;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.rapidminer.io.process.XMLTools;
import com.rapidminer.tools.XMLException;
import com.rapidminer.tools.cipher.CipherException;
import com.rapidminer.tools.cipher.CipherTools;
import com.rapidminer.tools.jdbc.DatabaseService;
import com.rapidminer.tools.jdbc.JDBCProperties;

/**
 * This class is a ConnectionEntry specifying additional fields for storing the host, port and database.
 * 
 * @author Tobias Malbrecht, Sebastian Land
 */
public class FieldConnectionEntry extends ConnectionEntry {

	static final String XML_TAG_NAME = "field-entry";

	private String host;

	private String port;

	private String database;
	
	/** the connection properties (not the JDBCProperties) */
	private Properties connectionProperties;

	/** the name of the repository where this connection comes from or null if it's not a repository connection */
	private String repository = null;

	public String getPort() {
		return port;
	}

	public void setPort(String port) {
		this.port = port;
	}

	public String getDatabase() {
		return database;
	}

	public void setDatabase(String database) {
		this.database = database;
	}

	@Override
	public String getUser() {
		return user;
	}

	public void setUser(String user) {
		this.user = user;
	}
	
	/**
	 * Sets the additional connection properties (not the jdbc properties).
	 * @param connectionProperties
	 */
	public void setConnectionProperties(Properties connectionProperties) {
		Properties newProps = new Properties();
		for (Object key : connectionProperties.keySet()) {
			newProps.put(key, connectionProperties.get(key));
		}
		this.connectionProperties = newProps;
	}
	
	/**
	 * Gets the additional connection properties (not the jdbc properties).
	 * @return
	 */
	public Properties getConnectionProperties() {
		Properties newProps = new Properties();
		for (Object key : connectionProperties.keySet()) {
			newProps.put(key, connectionProperties.get(key));
		}
		return newProps;
	}

	@Override
	public char[] getPassword() {
		return password;
	}

	public void setPassword(char[] password) {
		this.password = password;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public FieldConnectionEntry() {
		super();
	}

	public FieldConnectionEntry(String name, JDBCProperties properties, String host, String port, String database, String user, char[] password) {
		super(name, properties);
		this.host = host;
		this.port = port;
		this.database = database;
		this.user = user;
		this.password = password;
		this.connectionProperties = new Properties();
	}

	@Override
	public String getURL() {
		return createURL(properties, host, port, database);
	}

	public static String createURL(JDBCProperties properties, String host, String port, String database) {
		StringBuffer urlBuffer = new StringBuffer();
		if (properties != null) {
			urlBuffer.append(properties.getUrlPrefix());
		} else {
			urlBuffer.append("unknown:prefix://");
		}
		if (host != null && !"".equals(host)) {
			urlBuffer.append(host);
			if (port != null && !"".equals(port)) {
				urlBuffer.append(":" + port);
			}
			if (database != null && !"".equals(database)) {
				if (properties != null) {
					urlBuffer.append(properties.getDbNameSeperator());
				} else {
					urlBuffer.append("/");
				}
				urlBuffer.append(database);
			}
		}
		return urlBuffer.toString();
	}

	public String getHost() {
		return host;
	}

	@Override
	public boolean equals(Object object) {
		if (object instanceof FieldConnectionEntry) {
			FieldConnectionEntry entry = (FieldConnectionEntry) object;
			boolean equals = true;
			equals &= name.equals(entry.name);
			equals &= host.equals(entry.host);
			equals &= port.equals(entry.port);
			equals &= database.equals(entry.database);
			equals &= user.equals(entry.user);
			equals &= password.length == entry.password.length;
			if (equals) {
				for (int i = 0; i < password.length; i++) {
					equals &= password[i] == entry.password[i];
				}
			}
			return equals;
		}
		return false;
	}

	public Element toXML(Document doc, Key key, String replacementForLocalhost) throws CipherException {
		Element element = doc.createElement(XML_TAG_NAME);
		XMLTools.setTagContents(element, "name", name);
		if (properties != null) {
			XMLTools.setTagContents(element, "system", properties.getName());
		}
		String host = this.host;
		if (replacementForLocalhost != null) {
			host = host.replace("localhost", replacementForLocalhost);
		}
		XMLTools.setTagContents(element, "host", host);
		XMLTools.setTagContents(element, "port", port);
		XMLTools.setTagContents(element, "database", database);
		XMLTools.setTagContents(element, "user", user);
		XMLTools.setTagContents(element, "password", CipherTools.encrypt(new String(password), key));
		
		// add connection properties
		Element propertiesElement = doc.createElement("properties");
		element.appendChild(propertiesElement);
		for (Object propKey : connectionProperties.keySet()) {
			Element singlePropElement = doc.createElement(String.valueOf(propKey));
			singlePropElement.setTextContent(String.valueOf(connectionProperties.get(propKey)));
			propertiesElement.appendChild(singlePropElement);
		}
		
		return element;
	}

	public FieldConnectionEntry(Element element, Key key) throws CipherException {
		this.name = XMLTools.getTagContents(element, "name");
		this.host = XMLTools.getTagContents(element, "host");
		this.port = XMLTools.getTagContents(element, "port");
		this.database = XMLTools.getTagContents(element, "database");
		this.user = XMLTools.getTagContents(element, "user");
		this.password = CipherTools.decrypt(XMLTools.getTagContents(element, "password"), key).toCharArray();
		String system = XMLTools.getTagContents(element, "system");
		if (system != null) {
			properties = DatabaseService.getJDBCProperties(system);
		}
		try {
			Element propertiesElement = XMLTools.getChildElement(element, "properties", true);
			Properties props = new Properties();
			for (Element singlePropElement : XMLTools.getChildElements(propertiesElement)) {
				props.put(singlePropElement.getTagName(), singlePropElement.getTextContent());
			}
			this.connectionProperties = props;
		} catch (XMLException e) {
			// old connections may not yet have properties element
			this.connectionProperties = new Properties();
		}
	}
	
	public void setRepository(String repository) {
		this.repository = repository;
	}
	
	@Override
	public String getRepository() {
		return repository;
	}
	
	@Override
	public boolean isReadOnly() {
		// read only if it's a connection created by a RapidAnalytics remote repository
		if (repository != null) {
			return true;
		} else {
			return false;
		}
	}
}
