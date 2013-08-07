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

package com.rapidminer.repository.remote;

import java.awt.Desktop;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.lang.ref.WeakReference;
import java.net.Authenticator;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.PasswordAuthentication;
import java.net.ProtocolException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Level;

import javax.swing.Action;
import javax.swing.event.EventListenerList;
import javax.xml.namespace.QName;
import javax.xml.ws.BindingProvider;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import com.rapid_i.repository.wsimport.RAInfoService;
import com.rapid_i.repository.wsimport.RAInfoService_Service;
import com.rapid_i.repository.wsimport.RepositoryService;
import com.rapid_i.repository.wsimport.RepositoryService_Service;
import com.rapidminer.gui.actions.BrowseAction;
import com.rapidminer.gui.tools.PasswordDialog;
import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.io.Base64;
import com.rapidminer.io.process.XMLTools;
import com.rapidminer.repository.Entry;
import com.rapidminer.repository.Folder;
import com.rapidminer.repository.Repository;
import com.rapidminer.repository.RepositoryException;
import com.rapidminer.repository.RepositoryListener;
import com.rapidminer.repository.RepositoryLocation;
import com.rapidminer.repository.RepositoryManager;
import com.rapidminer.repository.gui.RemoteRepositoryPanel;
import com.rapidminer.repository.gui.RepositoryConfigurationPanel;
import com.rapidminer.tools.GlobalAuthenticator;
import com.rapidminer.tools.I18N;
import com.rapidminer.tools.LogService;
import com.rapidminer.tools.PasswortInputCanceledException;
import com.rapidminer.tools.WebServiceTools;
import com.rapidminer.tools.XMLException;
import com.rapidminer.tools.cipher.CipherException;
import com.rapidminer.tools.jdbc.connection.DatabaseConnectionService;
import com.rapidminer.tools.jdbc.connection.FieldConnectionEntry;

/**
 * A repository connecting to a RapidAnalytics installation.
 * 
 * @author Simon Fischer, Nils Woehler
 */
public class RemoteRepository extends RemoteFolder implements Repository {

	private static final int CHECK_CONFIG_TIMEOUT = 5000;
	private static CountDownLatch checkConfigCountDownLatch;

	/** Type of object requested from a server.*/
	public static enum EntryStreamType {
		METADATA, IOOBJECT, PROCESS, BLOB
	}

	private URL baseUrl;
	private String alias;
	private String username;
	private char[] password;
	private RepositoryService repositoryService;
	private ProcessServiceFacade processServiceFacade;
	private RAInfoService raInfoService;
	private final EventListenerList listeners = new EventListenerList();

	private static final Map<URI, WeakReference<RemoteRepository>> ALL_REPOSITORIES = new HashMap<URI, WeakReference<RemoteRepository>>();
	private static final Object MAP_LOCK = new Object();

	private boolean offline = true;
	private boolean isHome;
	private boolean passwortInputCanceled = false;

	static {
		GlobalAuthenticator.registerServerAuthenticator(new GlobalAuthenticator.URLAuthenticator() {

			@Override
			public PasswordAuthentication getAuthentication(URL url) throws PasswortInputCanceledException {
				WeakReference<RemoteRepository> reposRef = null;// = ALL_REPOSITORIES.get(url);
				for (Map.Entry<URI, WeakReference<RemoteRepository>> entry : ALL_REPOSITORIES.entrySet()) {
					if (url.toString().startsWith(entry.getKey().toString()) || url.toString().replace("127\\.0\\.0\\.1", "localhost").startsWith(entry.getKey().toString())) {
						reposRef = entry.getValue();
						break;
					}
				}

				if (reposRef == null) {
					return null;
				}
				RemoteRepository repository = reposRef.get();
				if (repository != null) {
					return repository.getAuthentication();
				} else {
					return null;
				}
			}

			@Override
			public String getName() {
				return "Repository authenticator";
			}

			@Override
			public String toString() {
				return getName();
			}
		});
	}

	public RemoteRepository(URL baseUrl, String alias, String username, char[] password, boolean isHome) {
		super("/");
		setRepository(this);
		this.setAlias(alias);
		this.baseUrl = baseUrl;
		this.setUsername(username);
		this.isHome = isHome;
		if ((password != null) && (password.length > 0)) {
			this.setPassword(password);
		} else {
			this.setPassword(null);
		}
		register(this);

		// The line below will cause a stack overflow
		//refreshProcessExecutionQueueNames();
	}

	private static void register(RemoteRepository remoteRepository) {
		synchronized (MAP_LOCK) {
			try {
				ALL_REPOSITORIES.put(remoteRepository.getBaseUrl().toURI(), new WeakReference<RemoteRepository>(remoteRepository));
			} catch (URISyntaxException e) {
				LogService.getRoot().log(Level.WARNING,
						I18N.getMessage(LogService.getRoot().getResourceBundle(),
								"com.rapidminer.repository.remote.RemoteRepository.adding_repository_uri_error",
								remoteRepository.getBaseUrl().toExternalForm()),
						e);
			}
		}
	}

	/**
	 * Checks if the provided configuration works. If it is working, <code>null</code> will be returned.
	 * If it is not working, an error message will be returned.
	 */
	public static synchronized String checkConfiguration(String url, String username, char[] password) {
		URL repositoryServiceURL;
		HttpURLConnection conn = null;
		try {
			if ((username != null) && (username.length() != 0) &&
					(password != null) && (password.length != 0)) {
				
				// create new count down latch with counter of 1
				// because this method is synchronized, it is possible to have only
				// one count down latch with a count of 1 at the same time.
				// This will block all instances of RemoteRepository from creating webservice calls/HttpConnections
				// with RapidAnalytics until this method has finished.
				// This has to be done because we will exchange the default authenticator soon.
				checkConfigCountDownLatch = new CountDownLatch(1);
				
				repositoryServiceURL = getRepositoryServiceWSDLUrl(new URL(url));

				// clear auth cache
				WebServiceTools.clearAuthCache();

				// Set default authenticator to null. 
				// This is actually pretty evil but we have to do it in order to get no PasswordDialogs when authentication fails.
				Authenticator.setDefault(null);

				// create connection
				conn = (HttpURLConnection) repositoryServiceURL.openConnection();

				// set timeout to check config timeout
				conn.setReadTimeout(CHECK_CONFIG_TIMEOUT);
				conn.setConnectTimeout(CHECK_CONFIG_TIMEOUT);
				conn.setRequestProperty("Accept-Charset", "UTF-8");

				// set basic auth
				String userpass = username + ":" + new String(password);
				String basicAuth = "Basic " + new String(Base64.encodeBytes(userpass.getBytes()));
				conn.setRequestProperty("Authorization", basicAuth);
				
				// get response code
				int responseCode = conn.getResponseCode();
				if (200 == responseCode) {
					return null; // works fine, return null
				}
				
				// something is wrong, return according error message
				if (responseCode >= 400) {
					if (responseCode == 401) {
						return I18N.getMessage(I18N.getErrorBundle(), "repository.error.check_connection.authentication_error");
					} else {
						return I18N.getMessage(I18N.getErrorBundle(), "repository.error.check_connection.other_authentication_error");
					}
				} else if (responseCode >= 500 && responseCode < 600) {
					return I18N.getMessage(I18N.getErrorBundle(), "repository.error.check_connection.internal_server_error");
				} else {
					return I18N.getMessage(I18N.getErrorBundle(), "repository.error.check_connection.unkown_error");
				}
			} else {
				
				// username or password not set
				if (username == null || username.length() == 0) {
					return I18N.getMessage(I18N.getErrorBundle(), "repository.error.check_connection.no_user");
				} else {
					return I18N.getMessage(I18N.getErrorBundle(), "repository.error.check_connection.no_password");
				}
			}
		} catch (Throwable t) {
			if (t instanceof UnknownHostException) {
				return I18N.getMessage(I18N.getErrorBundle(), "repository.error.check_connection.unknown_host", t.getLocalizedMessage());
			}
			if (t instanceof ConnectException) {
				return I18N.getMessage(I18N.getErrorBundle(), "repository.error.check_connection.connect_error", url);
			}
			return t.getMessage();
		} finally {
			
			// don't forget to set the instance of GlobalAuthenticator as default authenticator again
			Authenticator.setDefault(GlobalAuthenticator.getInstance());
			
			// disconnect connection
			if (conn != null) {
				conn.disconnect();
			}
			
			// count down the current count down latch so it reaches zero. All instances that have waited for
			// checkConfiguration are now able to proceed. 
			if(checkConfigCountDownLatch != null) {
				checkConfigCountDownLatch.countDown();
			}
		}
	}

	public URL getRepositoryServiceBaseUrl() {
		try {
			return new URL(getBaseUrl(), "RAWS/");
		} catch (MalformedURLException e) {
			// cannot happen
			LogService.getRoot().log(Level.WARNING,
					I18N.getMessage(LogService.getRoot().getResourceBundle(),
							"com.rapidminer.repository.remote.RemoteRepository.creating_webservice_error",
							e),
					e);
			return null;
		}
	}

	private static URL getRepositoryServiceWSDLUrl(URL baseURL) {
		String url = "RAWS/RepositoryService?wsdl";
		try {
			return new URL(baseURL, url);
		} catch (MalformedURLException e) {
			// cannot happen
			LogService.getRoot().log(Level.WARNING,
					I18N.getMessage(LogService.getRoot().getResourceBundle(),
							"com.rapidminer.repository.remote.RemoteRepository.creating_webservice_error",
							url),
					e);
			return null;
		}
	}

	private URL getRepositoryServiceWSDLUrl() {
		return getRepositoryServiceWSDLUrl(getBaseUrl());
	}

	private URL getRAInfoServiceWSDLUrl() {
		String url = "RAWS/RAInfoService?wsdl";
		try {
			return new URL(getBaseUrl(), url);
		} catch (MalformedURLException e) {
			// cannot happen
			LogService.getRoot().log(Level.WARNING,
					I18N.getMessage(LogService.getRoot().getResourceBundle(),
							"com.rapidminer.repository.remote.RemoteRepository.creating_webservice_error",
							url),
					e);
			return null;
		}
	}

	@Override
	public void addRepositoryListener(RepositoryListener l) {
		listeners.add(RepositoryListener.class, l);
	}

	@Override
	public void removeRepositoryListener(RepositoryListener l) {
		listeners.remove(RepositoryListener.class, l);
	}

	@Override
	public boolean rename(String newName) {
		this.setAlias(newName);
		fireEntryChanged(this);
		return true;
	}

	protected void fireEntryChanged(Entry entry) {
		for (RepositoryListener l : listeners.getListeners(RepositoryListener.class)) {
			l.entryChanged(entry);
		}
	}

	protected void fireEntryAdded(Entry newEntry, Folder parent) {
		for (RepositoryListener l : listeners.getListeners(RepositoryListener.class)) {
			l.entryAdded(newEntry, parent);
		}
	}

	protected void fireEntryRemoved(Entry removedEntry, Folder parent, int index) {
		for (RepositoryListener l : listeners.getListeners(RepositoryListener.class)) {
			l.entryRemoved(removedEntry, parent, index);
		}
	}

	protected void fireRefreshed(Folder folder) {
		for (RepositoryListener l : listeners.getListeners(RepositoryListener.class)) {
			l.folderRefreshed(folder);
		}
	}

	private Map<String, RemoteEntry> cachedEntries = new HashMap<String, RemoteEntry>();

	/** Connection entries fetched from server. */
	private Collection<FieldConnectionEntry> connectionEntries;

	private boolean cachedPasswordUsed = false;

	/** Process queue names fetched from server */
	private List<String> processExecutionQueueNames;
	private int protocollExceptionCount;
	private ProtocolException protocolException;

	protected void register(RemoteEntry entry) {
		cachedEntries.put(entry.getPath(), entry);
	}

	@Override
	public Entry locate(String string) throws RepositoryException {
		// check if connection is okay. If user has canceled the password dialog, return null
		if (!checkConnection()) {
			return null;
		}
		return RepositoryManager.getInstance(null).locate(this, string, false);
	}

	@Override
	public String getName() {
		return getAlias();
	}

	@Override
	public String getState() {
		return (isOffline() ? "offline" : (repositoryService != null ? "connected" : "disconnected"));
	}

	@Override
	public String getIconName() {
		return I18N.getMessage(I18N.getGUIBundle(), "gui.repository.remote.icon");
	}

	/** Returns a short HTML description of this repository. Does not include surrounding <html> tags. */
	public String toHtmlString() {
		return getAlias() + "<br/><small style=\"color:gray\">(" + getBaseUrl() + ")</small>";
	}

	/** This function will check if it is possible to connect to RapidAnalytics. If the user is not yet logged in, a password dialog will be shown.
	 *  If the user has canceled the password dialog <code>false</code> will be returned. The function calling this method should just return then without throwing an error itself.
	 *  If the connection can be established <code>true</code> will be returned. If there was a problem connecting to RapidAnalytics a {@link IOException} is thrown.
	 */
	protected synchronized boolean checkConnectionWithIOExpcetion() throws IOException {

		// even if the method is synchronized already, 
		// we still have to check if checkConfiguration is currently running
		if (checkConfigCountDownLatch != null) {
			try {
				checkConfigCountDownLatch.await(); // will wait if checkConfiguration is running
			} catch (InterruptedException e) {
				// do nothing
			}
		}

		boolean checkingConnection = true;
		boolean passwortInputNotCanceled = !isPasswordInputCanceled();

		InputStream inputStream = null;
		// Check if WSDL is reachable
		while (checkingConnection && passwortInputNotCanceled) {
			try {

				// this line will throw exceptions if the WSDL cannot be received
				byte[] temp = new byte[1];
				inputStream = WebServiceTools.openStreamFromURL(getRepositoryServiceWSDLUrl(), 3000);
				inputStream.read(temp);

				// this line can only be reached if the WSDL can be reached. This is only the case if the user is logged in.
				checkingConnection = false;

			} catch (ProtocolException e) {
				setOffline(true);

				// protocol exception means that probably the username and/or the password were wrong. Reset password and show the authentication dialog again
				setProtocollExceptionCount(getProtocollExceptionCount() + 1);
				protocolException = e;
				setPassword(null);
			} catch (IOException e) {
				setOffline(true);

				// only throw a repository exception if the user has not canceled the password input
				if (!isPasswordInputCanceled()) {
					throw e;
				}
			} finally {
				if (inputStream != null) {
					inputStream.close();
					inputStream = null;
				}
			}
			passwortInputNotCanceled = !isPasswordInputCanceled();
		}

		setProtocollExceptionCount(0);

		return passwortInputNotCanceled;
	}

	/** This function will check if it is possible to connect to RapidAnalytics. If the user is not yet logged in, a password dialog will be shown.
	 *  If the user has canceled the password dialog <code>false</code> will be returned. The function calling this method should just return then without throwing an error itself.
	 *  If the connection can be established <code>true</code> will be returned. If there was a problem connecting to RapidAnalytics a RepositoryException is thrown.
	 */
	protected synchronized boolean checkConnection() throws RepositoryException {
		try {
			return checkConnectionWithIOExpcetion();
		} catch (ConnectException e) {
			throw new RepositoryException(I18N.getMessage(I18N.getErrorBundle(), "repository.connection_exception", getName(), getBaseUrl()), e);
		} catch (IOException e) {

			// only throw a repository exception if the user has not canceled the password input
			if (!isPasswordInputCanceled()) {
				throw new RepositoryException(I18N.getMessage(I18N.getErrorBundle(), "repository.cannot_be_reached", getName()), e);
			}

			return false;
		}

	}

	private PasswordAuthentication getAuthentication() throws PasswortInputCanceledException {
		if (password == null) {
			LogService.getRoot().log(Level.INFO, "com.rapidminer.tools.repository.remote.RemoteRepository.authentication_requested", getBaseUrl());
			PasswordAuthentication passwordAuthentication;

			try {
				passwordAuthentication = getPasswordAuthentication();
				if (passwordAuthentication != null && passwordAuthentication.getUserName() != null && passwordAuthentication.getUserName().length() != 0
						&& passwordAuthentication.getPassword() != null && passwordAuthentication.getPassword().length != 0) {
					this.setPassword(passwordAuthentication.getPassword());
					this.setUsername(passwordAuthentication.getUserName());
					RepositoryManager.getInstance(null).save();
				}
				setPasswortInputCanceled(false);
				return passwordAuthentication;
			} catch (PasswortInputCanceledException e) {
				setPasswortInputCanceled(true);
				setPassword(null);
				setOffline(true);
				throw e;
			}
		} else {
			return new PasswordAuthentication(getUsername(), password);
		}
	}

	/**
	 * @return
	 * @throws PasswortInputCanceledException
	 */
	private PasswordAuthentication getPasswordAuthentication() throws PasswortInputCanceledException {
		PasswordAuthentication passwordAuthentication;
		if (cachedPasswordUsed) {
			// if we have used a cached password last time, and we enter this method again,
			// this is probably because the password was wrong, so rather force dialog than
			// using cache again.
			if (getProtocollExceptionCount() > 3) {
				passwordAuthentication = PasswordDialog.getPasswordAuthentication(getAlias(), getBaseUrl().toString(),
						false, false, "authentication.ra.wrong.credentials.protocol.error", getName(), protocolException.getLocalizedMessage());
			} else if (getProtocollExceptionCount() > 0) {
				passwordAuthentication = PasswordDialog.getPasswordAuthentication(getAlias(), getBaseUrl().toString(),
						false, false, "authentication.ra.wrong.credentials", getName());
			} else {
				passwordAuthentication = PasswordDialog.getPasswordAuthentication(getAlias(), getBaseUrl().toString(),
						false, false, "authentication.ra", getName());
			}
			this.cachedPasswordUsed = false;
		} else {
			if (getProtocollExceptionCount() > 3) {
				passwordAuthentication = PasswordDialog.getPasswordAuthentication(getAlias(), getBaseUrl().toString(),
						false, false, "authentication.ra.wrong.credentials.protocol.error", getName(), protocolException.getLocalizedMessage());
			} else if (getProtocollExceptionCount() > 0) {
				passwordAuthentication = PasswordDialog.getPasswordAuthentication(getAlias(), getBaseUrl().toString(),
						false, false, "authentication.ra.wrong.credentials", getName());
			} else {
				passwordAuthentication = PasswordDialog.getPasswordAuthentication(getAlias(), getBaseUrl().toString(),
						false, true, "authentication.ra", getName());
			}
			this.cachedPasswordUsed = true;
		}
		return passwordAuthentication;
	}

	/**
	 * @return the repository service. May return <code>null</code> if connection was refused.
	 */
	public RepositoryService getRepositoryService() throws RepositoryException {
		// check if connection is okay. If user has canceled the password dialog, just return
		if (!checkConnection()) {
			return null;
		}
		installJDBCConnectionEntries();
		if (repositoryService == null) {
			try {
				RepositoryService_Service serviceService = new RepositoryService_Service(getRepositoryServiceWSDLUrl(), new QName("http://service.web.rapidanalytics.de/", "RepositoryService"));
				repositoryService = serviceService.getRepositoryServicePort();
				setupBindingProvider((BindingProvider) repositoryService);

				setOffline(false);
			} catch (Exception e) {
				setOffline(true);
				setPassword(null);
				repositoryService = null;
				throw new RepositoryException("Cannot connect to " + getBaseUrl() + ": " + e, e);
			}
		}
		return repositoryService;
	}
	
	public void resetRepositoryService() throws RepositoryException {
		repositoryService = null;
		getRepositoryService();
	}

	private void setupBindingProvider(BindingProvider bp) {
		WebServiceTools.setCredentials(bp, getUsername(), password);
		WebServiceTools.setTimeout(bp);
	}

	/**
	 * 
	 * @return can return <code>null</code> if connection was refused
	 */
	public ProcessServiceFacade getProcessService() throws RepositoryException {
		// check if connection is okay. If user has canceled the password dialog, just return
		if (!checkConnection()) {
			return null;
		}
		if (processServiceFacade == null) {
			try {

				RAInfoService raInfoService = getRAInfoService();

				// throw error if user has canceled passwort input
				if (raInfoService == null && isPasswordInputCanceled()) {
					throw new RepositoryException(I18N.getMessage(I18N.getErrorBundle(), "repository.login_canceled", getName()));
				}

				processServiceFacade = new ProcessServiceFacade(raInfoService, getBaseUrl(), getUsername(), password);
				setPasswortInputCanceled(false);
				setOffline(false);
			} catch (Exception e) {
				setOffline(true);
				setPassword(null);
				processServiceFacade = null;
				throw new RepositoryException("Cannot connect to " + getBaseUrl() + ": " + e, e);
			}
		}
		return processServiceFacade;
	}

	/**
	 * @return the {@link RAInfoService} if it can be accessed. If the queried RA cannot be reached or has no RAInfoService <code>null</code> is returned.
	 */
	public RAInfoService getRAInfoService() {
		if (raInfoService == null) {
			try {
				checkConnection();
				RAInfoService_Service serviceService = new RAInfoService_Service(getRAInfoServiceWSDLUrl(), new QName("http://service.web.rapidanalytics.de/", "RAInfoService"));  //TODO how to set the namespace uri? 
				raInfoService = serviceService.getRAInfoServicePort();

				setupBindingProvider((BindingProvider) raInfoService);

				setPasswortInputCanceled(false);
				setOffline(false);
			} catch (Exception e) {
				LogService.getRoot().log(Level.INFO,
						I18N.getMessage(LogService.getRoot().getResourceBundle(),
								"com.rapidminer.tools.repository.remote.RemoteRepository.cannot_fetch_info_service"),
						e);
				raInfoService = null;
			}
		}
		return raInfoService;
	}

	@Override
	public String getDescription() {
		return "RapidAnalytics repository at " + getBaseUrl();
	}

	@Override
	public void refresh() throws RepositoryException {
		setPasswortInputCanceled(false);
		setProtocollExceptionCount(0);

		// check if connection is okay. If user has canceled the password dialog, just return
		if (!checkConnection()) {
			return;
		}

		cachedEntries.clear();
		super.refresh();
		if (!isPasswordInputCanceled()) {
			removeJDBCConnectionEntries();
			installJDBCConnectionEntries();
			refreshProcessExecutionQueueNames();
		}
	}

	private void refreshProcessExecutionQueueNames() {
		try {
			refreshProcessQueueNames(getProcessService());
		} catch (RepositoryException e) {
			processExecutionQueueNames = null;
		}
	}

	private void refreshProcessQueueNames(ProcessServiceFacade processService) {
		if (processService.getProcessServiceVersion().isAtLeast(ProcessServiceFacade.VERSION_1_3)) {
			processExecutionQueueNames = processService.getQueueNames();
		} else {
			processExecutionQueueNames = null;
		}
	}

	/**
	 * @return a copy of all process execution queue names. If it is <code>null</code> process queue names are not supported.
	 */
	public List<String> getProcessQueueNames() {
		if (processExecutionQueueNames == null) {
			try {
				ProcessServiceFacade processService = getProcessService();
				refreshProcessQueueNames(processService);
			} catch (RepositoryException e) {
				LogService.getRoot().log(Level.WARNING,
						I18N.getMessage(LogService.getRoot().getResourceBundle(),
								"com.rapidminer.tools.repository.remote.RemoteRepository.error_fetching_process_queue_names"),
						e);
			}
		}

		return processExecutionQueueNames == null ? null : new LinkedList<String>(processExecutionQueueNames);
	}

	/** Returns a connection to a given location in the repository. 
	 * @param preAuthHeader If set, the Authorization: header will be set to basic auth. Otherwise, the {@link GlobalAuthenticator} mechanism
	 *  will be used. 
	 *  @param type can be null*/
	public HttpURLConnection getResourceHTTPConnection(String location, EntryStreamType type, boolean preAuthHeader) throws IOException {
		String split[] = location.split("/");
		StringBuilder encoded = new StringBuilder();
		encoded.append("RAWS/resources");
		for (String fraction : split) {
			if (!fraction.isEmpty()) { // only for non empty to prevent double //
				encoded.append('/');

				// Do not encode the fraction of the location. This will be done in the #getHTTPConnection() method.
				// Furthermore URLEncoder is the wrong class to encode URLs. It is only used to encode URL parameters.
				encoded.append(fraction);
				//encoded.append(URLEncoder.encode(fraction, "UTF-8"));
			}
		}
		String query = null;
		if (type == EntryStreamType.METADATA) {
			query = "?format=" + URLEncoder.encode("binmeta", "UTF-8");
		}
		return getHTTPConnection(encoded.toString(), query, preAuthHeader);
	}

	/** 
	 * Use this function only if there are no query parameters. Use {@link #getHTTPConnection(String, String, boolean)} otherwise.
	 * 
	 * @param pathInfo should look like 'RAWS/...' without a '/' in front. Furthermore the pathInfo should NOT be encoded. This will be done by this function. 
	 */
	public HttpURLConnection getHTTPConnection(String pathInfo, boolean preAuthHeader) throws IOException {
		return getHTTPConnection(pathInfo, null, preAuthHeader);
	}

	/** 
	 * @param pathInfo should look like 'RAWS/...' without a '/' in front. Furthermore the pathInfo should NOT be encoded. This will be done by this function. 
	 * @param query should look like this '?format=PARAM1'. The query parameters should be encoded with URLEncoder before passing them to this function <br/>(e.g. String query = "?format="+URLEncoder.encode("binmeta", "UTF-8");).
	 *
	 * @return can return <code>null</code> if user cancels the password dialog
	 */
	public HttpURLConnection getHTTPConnection(String pathInfo, String query, boolean preAuthHeader) throws IOException {
		pathInfo = RepositoryLocation.SEPARATOR + pathInfo;
		URI uri;
		try {
			uri = new URI(getBaseUrl().getProtocol(), null, getBaseUrl().getHost(), getBaseUrl().getPort(), pathInfo, null, null);
		} catch (URISyntaxException e) {
			throw new IOException("Cannot connect to " + getBaseUrl() + RepositoryLocation.SEPARATOR + pathInfo + ". Location is malformed!", e);
		}
		URL url;
		if (query != null) {
			url = new URL(uri.toASCIIString() + query);
		} else {
			url = new URL(uri.toASCIIString());
		}

		if (!preAuthHeader) {
			if (!checkConnectionWithIOExpcetion()) {
				return null;
			}
		}

		final HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		WebServiceTools.setURLConnectionDefaults(conn);
		conn.setRequestProperty("Accept-Charset", "UTF-8");
		if (preAuthHeader && (username != null) && (password != null)) {
			String userpass = username + ":" + new String(password);
			String basicAuth = "Basic " + new String(Base64.encodeBytes(userpass.getBytes()));
			conn.setRequestProperty("Authorization", basicAuth);
		}
		return conn;
	}

	@Override
	public Element createXML(Document doc) {
		Element repositoryElement = doc.createElement("remoteRepository");

		Element url = doc.createElement("url");
		url.appendChild(doc.createTextNode(this.getBaseUrl().toString()));
		repositoryElement.appendChild(url);

		Element alias = doc.createElement("alias");
		alias.appendChild(doc.createTextNode(this.getAlias()));
		repositoryElement.appendChild(alias);

		Element user = doc.createElement("user");
		user.appendChild(doc.createTextNode(this.getUsername()));
		repositoryElement.appendChild(user);

		return repositoryElement;
	}

	public static RemoteRepository fromXML(Element element) throws XMLException {
		String url = XMLTools.getTagContents(element, "url", true);
		try {
			return new RemoteRepository(new URL(url), XMLTools.getTagContents(element, "alias", true), XMLTools.getTagContents(element, "user", true), null, false);
		} catch (MalformedURLException e) {
			throw new XMLException("Illegal url '" + url + "': " + e, e);
		}
	}

	@Override
	public void delete() {
		RepositoryManager.getInstance(null).removeRepository(this);
	}

	/**
	 *  @deprecated do not use, as it returns wrong results in case repositories are removed. 
	 *  Use {@link RepositoryManager#getRemoteRepositories()} instead.
	 */
	@Deprecated
	public static List<RemoteRepository> getAll() {
		List<RemoteRepository> result = new LinkedList<RemoteRepository>();
		for (WeakReference<RemoteRepository> ref : ALL_REPOSITORIES.values()) {
			if (ref != null) {
				RemoteRepository rep = ref.get();
				if(rep != null) {
					result.add(rep);
				}
			}
		}
		return result;
	}

	public boolean isConnected() {
		return !isOffline();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((getAlias() == null) ? 0 : getAlias().hashCode());
		int uriModificator = 0;
		try {
			uriModificator = (getBaseUrl() == null) ? 0 : getBaseUrl().toURI().hashCode();
		} catch (URISyntaxException e) {}
		result = prime * result + uriModificator;
		result = prime * result + ((getUsername() == null) ? 0 : getUsername().hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		RemoteRepository other = (RemoteRepository) obj;
		if (getAlias() == null) {
			if (other.getAlias() != null)
				return false;
		} else if (!getAlias().equals(other.getAlias()))
			return false;
		if (getBaseUrl() == null) {
			if (other.getBaseUrl() != null)
				return false;
		} else
			try {
				if (!getBaseUrl().toURI().equals(other.getBaseUrl().toURI()))
					return false;
			} catch (URISyntaxException e) {
				// this cannot happen, since we already had have a valid URL: no possible problem when converting to uri
				return false;
			}
		if (getUsername() == null) {
			if (other.getUsername() != null)
				return false;
		} else if (!getUsername().equals(other.getUsername()))
			return false;
		return true;
	}

	/** Returns the URI to which a browser can be pointed to browse a given entry. */
	public URI getURIForResource(String path) {
		try {
			return getBaseUrl().toURI().resolve("/RA/faces/restricted/browse.xhtml?location=" + URLEncoder.encode(path, "UTF-8"));
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		} catch (URISyntaxException e) {
			throw new RuntimeException(e);
		}
	}

	/** Returns the URI to which a browser can be pointed to access the RA web interface. */
	private URI getURIWebInterfaceURI() {
		try {
			return getBaseUrl().toURI().resolve("RA/faces/restricted/index.xhtml");
		} catch (URISyntaxException e) {
			throw new RuntimeException(e);
		}
	}

	public void browse(String location) {
		try {
			Desktop.getDesktop().browse(getURIForResource(location));
		} catch (Exception e) {
			SwingTools.showSimpleErrorMessage("cannot_open_browser", e);
		}
	}

	public void showLog(int id) {
		try {
			Desktop.getDesktop().browse(getProcessLogURI(id));
		} catch (Exception e) {
			SwingTools.showSimpleErrorMessage("cannot_open_browser", e);
		}
	}

	public URI getProcessLogURI(int id) {
		try {
			return getBaseUrl().toURI().resolve("/RA/processlog?id=" + id);
		} catch (URISyntaxException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public Collection<Action> getCustomActions() {
		Collection<Action> actions = super.getCustomActions();
		actions.add(new BrowseAction("remoterepository.administer", getRepository().getURIWebInterfaceURI()));
		return actions;
	}

	@Override
	public boolean shouldSave() {
		return !isHome;
	}

	// JDBC entries provided by server
	private Collection<FieldConnectionEntry> fetchJDBCEntries() throws XMLException, CipherException, SAXException, IOException {
		URL xmlURL = new URL(getBaseUrl(), "RAWS/jdbc_connections.xml");
		Document doc = XMLTools.parse(WebServiceTools.openStreamFromURL(xmlURL));
		final Collection<FieldConnectionEntry> result = DatabaseConnectionService.parseEntries(doc.getDocumentElement());
		for (FieldConnectionEntry entry : result) {
			entry.setRepository(getAlias());
		}
		return result;
	}

	@Override
	public void postInstall() {}

	private void installJDBCConnectionEntries() throws RepositoryException {
		if (this.connectionEntries != null) {
			return;
		}
		try {
			this.connectionEntries = fetchJDBCEntries();
			for (FieldConnectionEntry entry : connectionEntries) {
				DatabaseConnectionService.addConnectionEntry(entry);
			}
			LogService.getRoot().log(Level.CONFIG, "com.rapidminer.repository.remote.RemoteRepository.added_jdbc_connections_exported_by",
					new Object[] { connectionEntries.size(), getName() });
		} catch (Exception e) {
			LogService.getRoot().log(Level.WARNING,
					I18N.getMessage(LogService.getRoot().getResourceBundle(),
							"com.rapidminer.repository.remote.RemoteRepository.fetching_jdbc_connections_entries_error",
							getName()),
					e);
			setPassword(null);
			setOffline(true);
			throw new RepositoryException(I18N.getMessage(I18N.getErrorBundle(), "repository.cannot_be_reached", getName()), e);
		}
	}

	private void removeJDBCConnectionEntries() {
		if (this.connectionEntries != null) {
			for (FieldConnectionEntry entry : connectionEntries) {
				DatabaseConnectionService.deleteConnectionEntry(entry);
			}
			this.connectionEntries = null;
		}
	}

	@Override
	public void preRemove() {}

	public URL getBaseUrl() {
		return baseUrl;
	}

	public void setBaseUrl(URL url) {
		baseUrl = url;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getUsername() {
		return username;
	}

	public void setPassword(char[] password) {
		if (password != null && password.length == 0) {
			this.password = null;
		} else {
			this.password = password;
		}
		WebServiceTools.clearAuthCache();
	}

	@Override
	public boolean isConfigurable() {
		return true;
	}

	@Override
	public RepositoryConfigurationPanel makeConfigurationPanel() {
		return new RemoteRepositoryPanel();
	}

	public void setAlias(String alias) {
		this.alias = alias;
	}

	public String getAlias() {
		return alias;
	}

	private boolean isOffline() {
		return offline;
	}

	/** If value changes, notifies {@link #connectionListeners}. */
	private void setOffline(boolean offline) {
		if (offline == this.offline) {
			return;
		}
		this.offline = offline;
		for (ConnectionListener l : connectionListeners) {
			if (isConnected()) {
				l.connectionEstablished(this);
			} else {
				l.connectionLost(this);
			}
		}
	}

	private List<ConnectionListener> connectionListeners = new LinkedList<ConnectionListener>();

	public void addConnectionListener(ConnectionListener l) {
		connectionListeners.add(l);
	}

	public void removeConnectionListener(ConnectionListener l) {
		connectionListeners.remove(l);
	}

	/**
	 * @return the passwortInputCanceled
	 */
	public boolean isPasswordInputCanceled() {
		return this.passwortInputCanceled;
	}

	/**
	 * @param passwortInputCanceled the passwortInputCanceled to set
	 */
	public void setPasswortInputCanceled(boolean passwortInputCanceled) {
		this.passwortInputCanceled = passwortInputCanceled;
	}

	/**
	 * @return the protocollExceptionCount
	 */
	public int getProtocollExceptionCount() {
		return this.protocollExceptionCount;
	}

	/**
	 * @param protocollExceptionCount the protocollExceptionCount to set
	 */
	public void setProtocollExceptionCount(int protocollExceptionCount) {
		this.protocollExceptionCount = protocollExceptionCount;
	}

}
