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
package com.rapidminer.test;

import java.io.File;
import java.io.FileInputStream;
import java.net.URL;
import java.util.Properties;
import java.util.logging.Level;

import com.rapidminer.RapidMiner;
import com.rapidminer.RapidMiner.ExecutionMode;
import com.rapidminer.repository.Repository;
import com.rapidminer.repository.RepositoryLocation;
import com.rapidminer.repository.RepositoryManager;
import com.rapidminer.repository.remote.RemoteRepository;
import com.rapidminer.tools.FileSystemService;
import com.rapidminer.tools.LogService;

/**
 * <p>The test context (singleton) is in charge for initializing RapidMiner and determining a
 * test repository.</p>
 * 
 * <p>You can specify the test repository by setting the following system properties for this repository:
 * 
 *  <ul>
 *  	<li>rapidminer.test.repository.url</li>
 *      <li>rapidminer.test.repository.location</li>
 *  	<li>rapidminer.test.repository.user</li>
 *  	<li>rapidminer.test.repository.password</li>
 *  	<li>rapidminer.test.repository.exclude</li>
 *  <ul>
 *  </p>
 *  
 *  <p>Alternatively a file 'test.properties' with this properties can be saved in the home directory
 *  of RapidMiner.</p>
 *  
 *  <p>The alias for the repository will be 'junit'.</p>
 * 
 * @author Marcin Skirzynski, Nils Woehler
 *
 */
public class TestContext {


	/**
	 * Singleton instance
	 */
	private static volatile TestContext INSTANCE = null;
	
	/**
	 * File name for the properties
	 */
	public static String PROPERTY_TEST_FILE = 					"test.properties";
    
	/**
	 * Property name for the URL to the test repository
	 */
    public static String PROPERTY_TEST_REPOSITORY_URL = 		"rapidminer.test.repository.url";
    
    /**
     *Property name for the location to the test repository
     */
    public static String PROPERTY_TEST_REPOSITORY_LOCATION = 	"rapidminer.test.repository.location";
    
    /**
     * Property name for the user name for the test repository 
     */
    public static String PROPERTY_TEST_REPOSITORY_USER = 		"rapidminer.test.repository.user";
    
    /**
     * Property name for the password for the test repository 
     */
    public static String PROPERTY_TEST_REPOSITORY_PASSWORD = 	"rapidminer.test.repository.password";
    
    /**
     * A regular expression. It is matched against the process location (including the process name) relative 
     * to {@value #PROPERTY_TEST_REPOSITORY_LOCATION}.
     * If it matches, the process is NOT tested. Please note that the entire string must be matched.
     * That means if you wanted to exclude any processes which contain the substring "ignore" the expression
     * must be something like ".*ignore.*" .
     */
    private static final String PROPERTY_TEST_REPOSITORY_EXCLUDE = "rapidminer.test.repository.exclude";    
    
    /**
     * Displayed repostiory alias.
     */
    public static String REPOSITORY_ALIAS = "junit";
	
	private boolean initialized = false;
	
	private boolean repositoryPresent = false;
	
	private Repository repository;
	
	private RepositoryLocation repositoryLocation;

	private String processExclusionPattern = null;
	
	/**
	 * Does not allow external instantiation
	 */
	private TestContext() {}
	
	/**
	 * Returns the singleton instance of the test context
	 * 
	 * @return	the test context
	 */
    public static TestContext get() {
        if (INSTANCE == null) {
            synchronized(TestContext.class) {
                if (INSTANCE == null)
                	INSTANCE = new TestContext(); 
            }
        }
        return INSTANCE;
    }
	
    /**
     * <p>Initializes RapidMiner and tries to fetch the information for the test repository.</p>
     * 
	 * <p>You can specify the test repository by setting the following system properties for this repository:
	 * 
	 *  <ul>
	 *  	<li>rapidminer.test.repository.url</li>
	 *      <li>rapidminer.test.repository.location</li>
	 *  	<li>rapidminer.test.repository.user</li>
	 *  	<li>rapidminer.test.repository.password</li>
	 *  <ul>
	 *  </p>
	 *  
	 *  <p>Alternatively a file 'test.properties' with this properties can be saved in the home directory
	 *  of RapidMiner.</p>
	 *  
	 *  <p>The alias for the repository will be 'junit'.</p>
     */
	public void initRapidMiner() {
		
		 if (!isInitialized()) {
	            File testConfigFile = FileSystemService.getUserConfigFile(PROPERTY_TEST_FILE);
	            
	            Properties properties = new Properties();
	            if (testConfigFile.exists()) {
	                FileInputStream in;
	                try {
	                    in = new FileInputStream(testConfigFile);
	                    properties.load(in);
	                    in.close();
	                } catch (Exception e) {
	                    throw new RuntimeException("Failed to read " + testConfigFile,e);
	                }
	            } else {
	            	properties = System.getProperties();
	            }

	            String repositoryUrl = properties.getProperty(PROPERTY_TEST_REPOSITORY_URL);
	            String repositoryLocation = properties.getProperty(PROPERTY_TEST_REPOSITORY_LOCATION);
	            String repositoryUser= properties.getProperty(PROPERTY_TEST_REPOSITORY_USER);
	            String repositoryPassword = properties.getProperty(PROPERTY_TEST_REPOSITORY_PASSWORD);
	            
	            this.processExclusionPattern = properties.getProperty(PROPERTY_TEST_REPOSITORY_EXCLUDE);
	            

	            RapidMiner.setExecutionMode(ExecutionMode.TEST);
	            RapidMiner.init();
	            RapidMiner.initAsserters();
	            
	            try {
	            	if (repositoryUrl!=null&&repositoryLocation!=null&&repositoryUser!=null&&repositoryPassword!=null) {
						setRepository(new RemoteRepository(new URL(repositoryUrl), REPOSITORY_ALIAS, repositoryUser, repositoryPassword.toCharArray(), true));
	            		setRepositoryLocation(new RepositoryLocation(repositoryLocation));
	            		RepositoryManager.getInstance(null).addRepository(getRepository());
	            		setRepositoryPresent(true);
	            	} else {
//	            		LogService.getRoot().log(Level.WARNING,
//	            				"In order to run repository tests, please define system property "
//	            				+PROPERTY_TEST_REPOSITORY_URL+", "
//	            				+PROPERTY_TEST_REPOSITORY_LOCATION+", "
//	            				+PROPERTY_TEST_REPOSITORY_USER+" and "
//	            				+PROPERTY_TEST_REPOSITORY_PASSWORD+
//	            				" in your run configuration or create a property file called "+PROPERTY_TEST_FILE+" with this values which point to the test repository.");
	            		LogService.getRoot().log(Level.WARNING,
	            		"com.rapidminer.test.TestContext.define_system_property",
        				new Object [] {PROPERTY_TEST_REPOSITORY_URL,
        				PROPERTY_TEST_REPOSITORY_LOCATION,
        				PROPERTY_TEST_REPOSITORY_USER,
        				PROPERTY_TEST_REPOSITORY_PASSWORD,
        				PROPERTY_TEST_FILE});
	            	}
	            } catch (Exception e) {
	            	setRepositoryPresent(false);
//	            	System.out.println("url: " + repositoryUrl);
//	            	System.out.println("user: " + repositoryUser);
//	            	System.out.println("pass: " + repositoryPassword);
//	            	System.out.println("loca: " + repositoryLocation);
	                throw new RuntimeException("Failed to intialize test repository", e);
	            }

	            setInitialized(true);
	        }
		
	}

	/**
	 * @param initialized the initialized to set
	 */
	public void setInitialized(boolean initialized) {
		this.initialized = initialized;
	}

	/**
	 * @return the initialized
	 */
	public boolean isInitialized() {
		return initialized;
	}

	/**
	 * @param repository the repository to set
	 */
	public void setRepository(Repository repository) {
		this.repository = repository;
	}

	/**
	 * @return the repository
	 */
	public Repository getRepository() {
		return repository;
	}

	/**
	 * @param repositoryLocation the repositoryLocation to set
	 */
	public void setRepositoryLocation(RepositoryLocation repositoryLocation) {
		this.repositoryLocation = repositoryLocation;
	}

	/**
	 * @return the repositoryLocation
	 */
	public RepositoryLocation getRepositoryLocation() {
		return repositoryLocation;
	}

	/**
	 * @param repositoryPresent the repositoryPresent to set
	 */
	public void setRepositoryPresent(boolean repositoryPresent) {
		this.repositoryPresent = repositoryPresent;
	}

	/**
	 * @return the repositoryPresent
	 */
	public boolean isRepositoryPresent() {
		return repositoryPresent;
	}

	public String getProcessExclusionPattern() {
		return processExclusionPattern;
	}
}
