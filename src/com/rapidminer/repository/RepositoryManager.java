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

package com.rapidminer.repository;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.rapidminer.RapidMiner;
import com.rapidminer.io.process.XMLTools;
import com.rapidminer.operator.IOObject;
import com.rapidminer.operator.Operator;
import com.rapidminer.repository.db.DBRepository;
import com.rapidminer.repository.local.LocalRepository;
import com.rapidminer.repository.remote.RemoteRepository;
import com.rapidminer.repository.resource.ResourceRepository;
import com.rapidminer.tools.AbstractObservable;
import com.rapidminer.tools.FileSystemService;
import com.rapidminer.tools.I18N;
import com.rapidminer.tools.LogService;
import com.rapidminer.tools.Observer;
import com.rapidminer.tools.ProgressListener;
import com.rapidminer.tools.Tools;
import com.rapidminer.tools.XMLException;

/** Keeps static references to registered repositories and provides helper methods.
 * 
 *  Observers will be notified when repositories are added (with the repository passed as an argument to the 
 *  {@link Observer#update(com.rapidminer.tools.Observable, Object)} method and when they are removed,
 *  in which case null is passed.
 *
 * @author Simon Fischer
 *
 */
public class RepositoryManager extends AbstractObservable<Repository> {

	public static final String SAMPLE_REPOSITORY_NAME = "Samples";

	private static final Logger LOGGER = Logger.getLogger(RepositoryManager.class.getName());

	private static RepositoryManager instance;
	private static final Object INSTANCE_LOCK = new Object();
	private static Repository sampleRepository;
	private static final Map<RepositoryAccessor, RepositoryManager> CACHED_MANAGERS = new HashMap<RepositoryAccessor, RepositoryManager>();
	private static final List<RepositoryFactory> FACTORIES = new LinkedList<RepositoryFactory>();

	private final List<Repository> repositories = new LinkedList<Repository>();

	public static RepositoryManager getInstance(RepositoryAccessor repositoryAccessor) {
		synchronized (INSTANCE_LOCK) {
			if (instance == null) {
				init();
			}
			if (repositoryAccessor != null) {
				RepositoryManager manager = CACHED_MANAGERS.get(repositoryAccessor);
				if (manager == null) {
					manager = new RepositoryManager(instance);
					for (RepositoryFactory factory : FACTORIES) {
						for (Repository repos : factory.createRepositoriesFor(repositoryAccessor)) {
							manager.repositories.add(repos);
						}
					}
					CACHED_MANAGERS.put(repositoryAccessor, manager);
				}
				return manager;
			}
		}
		return instance;
	}

	private RepositoryManager(RepositoryManager cloned) {
		this.repositories.addAll(cloned.repositories);
	}

	private RepositoryManager() {
		if (sampleRepository == null) {
			sampleRepository = new ResourceRepository(SAMPLE_REPOSITORY_NAME, "samples");
		}
		repositories.add(sampleRepository);
		repositories.add(new DBRepository());

		final String homeUrl = System.getProperty(RapidMiner.PROPERTY_HOME_REPOSITORY_URL);
		if (homeUrl != null) {
			try {
				String username = System.getProperty(RapidMiner.PROPERTY_HOME_REPOSITORY_USER);
				String password = System.getProperty(RapidMiner.PROPERTY_HOME_REPOSITORY_PASSWORD);
				char[] passwordChars = null;
				if (password != null) {
					passwordChars = password.toCharArray();
				}
				RemoteRepository homeRepository = new RemoteRepository(new URL(homeUrl), "Home", username, passwordChars, true);
				repositories.add(homeRepository);
				//LogService.getRoot().config("Adding home repository " + homeUrl + ".");
				LogService.getRoot().log(Level.CONFIG, "com.rapidminer.repository.RepositoryManager.adding_home_repository", homeUrl);
			} catch (MalformedURLException e) {
				//LogService.getRoot().log(Level.WARNING, "Illegal repository URL " + homeUrl + ": " + e, e);
				LogService.getRoot().log(Level.WARNING,
						I18N.getMessage(LogService.getRoot().getResourceBundle(),
								"com.rapidminer.repository.RepositoryManager.illegal_repository_url",
								homeUrl, e),
						e);

			}
		}
		load();
	}

	public static void init() {
		synchronized (INSTANCE_LOCK) {
			instance = new RepositoryManager();
			instance.postInstall();
		}
	}

	private void postInstall() {
		for (Repository repository : getRepositories()) {
			repository.postInstall();
		}
	}

	public static void registerFactory(RepositoryFactory factory) {
		synchronized (INSTANCE_LOCK) {
			FACTORIES.add(factory);
		}
	}

	/** Registers a repository. 
	 * 
	 * @see #removeRepository(Repository) */
	public void addRepository(Repository repository) {
		LOGGER.config("Adding repository " + repository.getName());
		repositories.add(repository);
		if (instance != null) {
			// we cannot call post install during init(). The reason is that
			// post install may access RepositoryManager.getInstance() which will be null and hence
			// trigger further recursive, endless calls to init()
			repository.postInstall();
			save();
		}
		fireUpdate(repository);
	}

	/** Removes a registered repository. 
	 * 
	 * @see #addRepository(Repository) */
	public void removeRepository(Repository repository) {
		repository.preRemove();
		repositories.remove(repository);
		fireUpdate(null);
	}

	public List<Repository> getRepositories() {
		return Collections.unmodifiableList(repositories);
	}

	/** Gets a registered ({@link #addRepository(Repository)} repository by {@link Repository#getName()}*/
	public Repository getRepository(String name) throws RepositoryException {
		for (Repository repos : repositories) {
			if (repos.getName().equals(name)) {
				return repos;
			}
		}
		throw new RepositoryException("Requested repository " + name + " does not exist.");
	}

	/** Gets a list of all registered repositories inheriting from {@link RemoteRepository}. */
	public List<RemoteRepository> getRemoteRepositories() {
		List<RemoteRepository> result = new LinkedList<RemoteRepository>();
		for (Repository repos : getRepositories()) {
			if (repos instanceof RemoteRepository) {
				result.add((RemoteRepository) repos);
			}
		}
		return result;
	}

	private File getConfigFile() {
		return FileSystemService.getUserConfigFile("repositories.xml");
	}

	/** Loads the XML configuration file.
	 * 
	 * @see #save() */
	public void load() {
		if (!RapidMiner.getExecutionMode().canAccessFilesystem()) {
			LOGGER.info("Cannot access file system in execution mode " + RapidMiner.getExecutionMode() + ". Not loading repositories.");
			return;
		}
		File file = getConfigFile();
		if (file.exists()) {
			LOGGER.config("Loading repositories from " + file);
			try {
				Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file);
				if (!doc.getDocumentElement().getTagName().equals("repositories")) {
					LOGGER.warning("Broken repositories file. Root element must be <reposities>.");
					return;
				}
				NodeList list = doc.getDocumentElement().getChildNodes();
				for (int i = 0; i < list.getLength(); i++) {
					if (list.item(i) instanceof Element) {
						Element element = (Element) list.item(i);
						if ("localRepository".equals(element.getTagName())) {
							addRepository(LocalRepository.fromXML(element));
						} else if ("remoteRepository".equals(element.getTagName())) {
							addRepository(RemoteRepository.fromXML(element));
						} else {
							LOGGER.warning("Unknown tag: " + element.getTagName());
						}
					}
				}
			} catch (Exception e) {
				LOGGER.log(Level.WARNING, "Cannot read repository configuration file '" + file + "': " + e, e);
			}
		}
	}

	public void createRepositoryIfNoneIsDefined() {
		boolean empty = true;
		// check if we have at least one repository that is not pre-defined
		for (Repository repository : repositories) {
			if (!(repository instanceof ResourceRepository) && !(repository instanceof DBRepository)) {
				empty = false;
				break;
			}
		}
		if (empty) {
//			SwingTools.showMessageDialog("please_create_repository");
//			NewRepositoryDialog.createNew();
			try {
				LocalRepository defaultRepo = new LocalRepository("Local Repository");
				RepositoryManager.getInstance(null).addRepository(defaultRepo);
				defaultRepo.createFolder("data");
				defaultRepo.createFolder("processes");
			} catch (RepositoryException e) {
				LogService.getRoot().log(Level.WARNING, "com.rapidminer.repository.RepositoryManager.failed_to_create_default", e);
			}
		}
	}

	/** Stores the XML configuration file. 
	 * @see #load() 
	 */
	public void save() {
		if (!RapidMiner.getExecutionMode().canAccessFilesystem()) {
			LOGGER.config("Cannot access file system in execution mode " + RapidMiner.getExecutionMode() + ". Not saving repositories.");
			return;
		}
		Document doc;
		try {
			doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
		} catch (ParserConfigurationException e) {
			LOGGER.log(Level.WARNING, "Cannot save repositories: " + e, e);
			return;
		}
		Element root = doc.createElement("repositories");
		doc.appendChild(root);
		for (Repository repository : getRepositories()) {
			if (repository.shouldSave()) {
				Element repositoryElement = repository.createXML(doc);
				if (repositoryElement != null) {
					root.appendChild(repositoryElement);
				}
			}
		}
		try {
			XMLTools.stream(doc, getConfigFile(), null);
		} catch (XMLException e) {
			LOGGER.log(Level.WARNING, "Cannot save repositories: " + e, e);
		}
	}

	/** Stores an IOObject at the given location. Creates entries if they don't exist. */
	public IOObject store(IOObject ioobject, RepositoryLocation location, Operator callingOperator) throws RepositoryException {
		return store(ioobject, location, callingOperator, null);
	}

	/** Stores an IOObject at the given location. Creates entries if they don't exist. */
	public IOObject store(IOObject ioobject, RepositoryLocation location, Operator callingOperator, ProgressListener progressListener) throws RepositoryException {
		Entry entry = location.locateEntry();
		if (entry == null) {
			RepositoryLocation parentLocation = location.parent();
			if (parentLocation != null) {
				String childName = location.getName();

				Entry parentEntry = parentLocation.locateEntry();
				Folder parentFolder;
				if (parentEntry != null) {
					if (parentEntry instanceof Folder) {
						parentFolder = (Folder) parentEntry;
					} else {
						throw new RepositoryException("Parent '" + parentLocation + "' of '" + location + "' is not a folder.");
					}
				} else {
					parentFolder = parentLocation.createFoldersRecursively();
				}
				parentFolder.createIOObjectEntry(childName, ioobject, callingOperator, progressListener);
				return ioobject;
			} else {
				throw new RepositoryException("Entry '" + location + "' does not exist.");
			}
		} else if (entry instanceof IOObjectEntry) {
			((IOObjectEntry) entry).storeData(ioobject, callingOperator, null);
			return ioobject;
		} else {
			throw new RepositoryException("Entry '" + location + "' is not a data entry, but " + entry.getType());
		}
	}

	/** Gets the referenced blob entry. Creates a new one if it does not exist. */
	public BlobEntry getOrCreateBlob(RepositoryLocation location) throws RepositoryException {
		Entry entry = location.locateEntry();
		if (entry == null) {
			RepositoryLocation parentLocation = location.parent();
			if (parentLocation != null) {
				String childName = location.getName();
				Entry parentEntry = parentLocation.locateEntry();
				Folder parentFolder;
				if (parentEntry != null) {
					if (parentEntry instanceof Folder) {
						parentFolder = (Folder) parentEntry;
					} else {
						throw new RepositoryException("Parent '" + parentLocation + "' of '" + location + "' is not a folder.");
					}
				} else {
					parentFolder = parentLocation.createFoldersRecursively();
				}
				return parentFolder.createBlobEntry(childName);
			} else {
				throw new RepositoryException("Entry '" + location + "' does not exist.");
			}
		} else if (entry instanceof BlobEntry) {
			return (BlobEntry) entry;
		} else {
			throw new RepositoryException("Entry '" + location + "' is not a blob entry, but a " + entry.getType());
		}
	}

	/** Saves the configuration file. */
	public static void shutdown() {
		if (instance != null) {
			instance.save();
		}
	}

	/** Copies an entry to a given destination folder. */
	public void copy(RepositoryLocation source, Folder destination, ProgressListener listener) throws RepositoryException {
		copy(source, destination, null, listener);
	}

	/** Copies an entry to a given destination folder with the name newName. If newName is null the old name will be kept. */
	public void copy(RepositoryLocation source, Folder destination, String newName, ProgressListener listener) throws RepositoryException {
		if (listener != null) {
			listener.setTotal(100000);
			listener.setCompleted(0);
		}
		try {
			copy(source, destination, newName, listener, 0, 100000);
		} finally {
			if (listener != null) {
				listener.complete();
			}
		}
	}

	private void copy(RepositoryLocation source, Folder destination, String newName, ProgressListener listener, int minProgress, int maxProgress) throws RepositoryException {
		Entry entry = source.locateEntry();
		if (entry == null) {
			throw new RepositoryException("No such entry: " + source);
		}
		copy(entry, destination, newName, listener, minProgress, maxProgress);
	}

	private void copy(Entry entry, Folder destination, String newName, ProgressListener listener, int minProgress, int maxProgress) throws RepositoryException {
		if (listener != null) {
			listener.setMessage(entry.getName());
		}

		if (newName == null) {
			newName = entry.getName();
		}

		String originalName = newName;
		if (destination.containsEntry(newName)) {
			newName = "Copy of " + newName;
			int i = 2;
			while (destination.containsEntry(newName)) {
				newName = "Copy " + (i++) + " of " + originalName;
			}
		}
		if (entry instanceof ProcessEntry) {
			ProcessEntry pe = (ProcessEntry) entry;
			String xml = pe.retrieveXML();
			if (listener != null) {
				listener.setCompleted((minProgress + maxProgress) / 2);
			}
			destination.createProcessEntry(newName, xml);
			if (listener != null) {
				listener.setCompleted(maxProgress);
			}
		} else if (entry instanceof IOObjectEntry) {
			IOObjectEntry iooe = (IOObjectEntry) entry;
			IOObject original = iooe.retrieveData(null);
			if (listener != null) {
				listener.setCompleted((minProgress + maxProgress) / 2);
			}
			destination.createIOObjectEntry(newName, original, null, null);
			if (listener != null) {
				listener.setCompleted(maxProgress);
			}
		} else if (entry instanceof BlobEntry) {
			BlobEntry blob = (BlobEntry) entry;
			BlobEntry target = destination.createBlobEntry(newName);
			try {
				InputStream in = blob.openInputStream();
				String mimeType = blob.getMimeType();
				OutputStream out = target.openOutputStream(mimeType);
				Tools.copyStreamSynchronously(in, out, true);
				if (listener != null) {
					listener.setCompleted(maxProgress);
				}
			} catch (IOException e) {
				throw new RepositoryException(e);
			}
		} else if (entry instanceof Folder) {
			String sourceAbsolutePath = entry.getLocation().getAbsoluteLocation();
			String destinationAbsolutePath = destination.getLocation().getAbsoluteLocation();
			// make sure same folder moves are forbidden
			if (sourceAbsolutePath.equals(destinationAbsolutePath)) {
				throw new RepositoryException(I18N.getMessage(I18N.getErrorBundle(), "repository.repository_copy_same_folder"));
			}
			// make sure moving parent folder into subfolder is forbidden
			if (destinationAbsolutePath.contains(sourceAbsolutePath)) {
				throw new RepositoryException(I18N.getMessage(I18N.getErrorBundle(), "repository.repository_copy_into_subfolder"));
			}
			Folder destinationFolder = destination.createFolder(newName);
			List<Entry> allChildren = new LinkedList<Entry>();
			allChildren.addAll(((Folder) entry).getSubfolders());
			allChildren.addAll(((Folder) entry).getDataEntries());
			final int count = allChildren.size();
			int progressStart = minProgress;
			int progressDiff = maxProgress - minProgress;
			int i = 0;
			for (Entry child : allChildren) {
				copy(child, destinationFolder, null, listener, progressStart + i * progressDiff / count, progressStart + (i + 1) * progressDiff / count);
				i++;
			}
		} else {
			throw new RepositoryException("Cannot copy entry of type " + entry.getType());
		}
	}

	/** Moves an entry to a given destination folder. */
	public void move(RepositoryLocation source, Folder destination, ProgressListener listener) throws RepositoryException {
		move(source, destination, null, listener);
	}

	/** Moves an entry to a given destination folder with the name newName. */
	public void move(RepositoryLocation source, Folder destination, String newName, ProgressListener listener) throws RepositoryException {
		Entry entry = source.locateEntry();
		if (entry == null) {
			throw new RepositoryException("No such entry: " + source);
		} else {
			String sourceAbsolutePath = source.getAbsoluteLocation();
			String destinationAbsolutePath;
			if (!(entry instanceof Folder)) {
				destinationAbsolutePath = destination.getLocation().getAbsoluteLocation() + RepositoryLocation.SEPARATOR + source.getName();
			} else {
				destinationAbsolutePath = destination.getLocation().getAbsoluteLocation();
			}
			// make sure same folder moves are forbidden
			if (sourceAbsolutePath.equals(destinationAbsolutePath)) {
				throw new RepositoryException(I18N.getMessage(I18N.getErrorBundle(), "repository.repository_move_same_folder"));
			}
			// make sure moving parent folder into subfolder is forbidden
			if (destinationAbsolutePath.contains(sourceAbsolutePath)) {
				throw new RepositoryException(I18N.getMessage(I18N.getErrorBundle(), "repository.repository_move_into_subfolder"));
			}
			if (destination.getLocation().getRepository() != source.getRepository()) {
				copy(source, destination, newName, listener);
				entry.delete();
			} else {
				String effectiveNewName = newName != null ? newName : entry.getName();
				Entry toDeleteEntry = null;
				for (Folder folderEntry : destination.getSubfolders()) {
					if (folderEntry.getName().equals(effectiveNewName)) {
						throw new RepositoryException(I18N.getMessage(I18N.getErrorBundle(), "repository.repository_folder_already_exists", effectiveNewName));
					}
				}
				if (destination.containsEntry(effectiveNewName)) {
					for (DataEntry dataEntry : destination.getDataEntries()) {
						if (dataEntry.getName().equals(effectiveNewName)) {
							toDeleteEntry = dataEntry;
						}
					}
					if (toDeleteEntry != null) {
						toDeleteEntry.delete();
					}
				}
				if (listener != null) {
					listener.setTotal(100);
					listener.setCompleted(10);
				}
				if (newName == null) {
					entry.move(destination);
				} else {
					entry.move(destination, newName);
				}
				if (listener != null) {
					listener.setCompleted(100);
					listener.complete();
				}
			}
		}
	}

	/** Looks up the entry with the given path in the given repository.
	 *  This method will return null when it finds a folder that blocks (has not yet loaded
	 *  all its data) AND failIfBlocks is true.
	 * 
	 *  This method can be used as a first approach to locate an entry and fall back
	 *  to a more expensive solution when this fails.
	 * 
	 */
	public Entry locate(Repository repository, String path, boolean failIfBlocks) throws RepositoryException {
		if (path.startsWith("" + RepositoryLocation.SEPARATOR)) {
			path = path.substring(1);
		}
		if (path.equals("")) {
			return repository;
		}
		String[] splitted = path.split("" + RepositoryLocation.SEPARATOR);
		Folder folder = repository;
		int index = 0;
		while (true) {
			if (failIfBlocks && folder.willBlock()) {
				return null;
			}
			if (index == splitted.length - 1) {
				int retryCount = 0;
				while (retryCount <= 1) {
					List<Entry> all = new LinkedList<Entry>();
					all.addAll(folder.getSubfolders());
					all.addAll(folder.getDataEntries());
					for (Entry child : all) {
						if (child.getName().equals(splitted[index])) {
							return child;
						}
					}
					// missed entry -> refresh and try again
					if (folder.canRefreshChild(splitted[index])) {
						folder.refresh();
					} else {
						break;
					}
					retryCount++;
				}

				return null;
			} else {
				int retryCount = 0;
				boolean found = false;
				while (retryCount <= 1) {
					for (Folder subfolder : folder.getSubfolders()) {
						if (subfolder.getName().equals(splitted[index])) {
							folder = subfolder;
							found = true;
							break;
						}
					}
					if (found) {
						// found in 1st round
						break;
					} else {
						// missed entry -> refresh and try again
						if (folder.canRefreshChild(splitted[index])) {
							folder.refresh();
						} else {
							break;
						}
						retryCount++;
					}

				}
				if (!found) {
					return null;
				}
			}
			index++;
		}
	}

	/** Returns the repository containing the RapidMiner sample processes. */
	public Repository getSampleRepository() {
		return sampleRepository;
	}

	/** Visitor pattern for repositories. Callbacks to the visitor will be made
	 *  only for matching types. (Recursion happens also if the type is not
	 *  a Folder.
	 * @throws RepositoryException
	 * */
	public <T extends Entry> void walk(Entry start, RepositoryVisitor<T> visitor, Class<T> visitedType) throws RepositoryException {
		boolean continueChildren = true;
		if (visitedType.isInstance(start)) {
			continueChildren &= visitor.visit(visitedType.cast(start));
		}
		if (continueChildren && (start instanceof Folder)) {
			Folder folder = (Folder) start;
			for (Entry child : folder.getDataEntries()) {
				walk(child, visitor, visitedType);
			}
			for (Folder childFolder : folder.getSubfolders()) {
				walk(childFolder, visitor, visitedType);
			}
		}
	}
}
