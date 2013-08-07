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

import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

import com.rapid_i.repository.wsimport.EntryResponse;
import com.rapid_i.repository.wsimport.FolderContentsResponse;
import com.rapid_i.repository.wsimport.RepositoryService;
import com.rapid_i.repository.wsimport.Response;
import com.rapidminer.operator.IOObject;
import com.rapidminer.operator.Operator;
import com.rapidminer.repository.BlobEntry;
import com.rapidminer.repository.DataEntry;
import com.rapidminer.repository.Entry;
import com.rapidminer.repository.Folder;
import com.rapidminer.repository.IOObjectEntry;
import com.rapidminer.repository.MalformedRepositoryLocationException;
import com.rapidminer.repository.ProcessEntry;
import com.rapidminer.repository.RepositoryConstants;
import com.rapidminer.repository.RepositoryException;
import com.rapidminer.repository.RepositoryLocation;
import com.rapidminer.tools.I18N;
import com.rapidminer.tools.ProgressListener;

/**
 * @author Simon Fischer
 */
public class RemoteFolder extends RemoteEntry implements Folder {

	private final Comparator<Entry> nameComparator = new Comparator<Entry>() {

		@Override
		public int compare(Entry o1, Entry o2) {
			return o1.getName().compareTo(o2.getName());
		}
	};

	private List<Folder> folders;
	private List<DataEntry> entries;
	private final Object lock = new Object();

	private boolean readOnly = false;
	private boolean forbidden = false;

	private Object childrenLock = new Object();

	RemoteFolder(String location) {
		super(location);
	}

	RemoteFolder(EntryResponse response, RemoteFolder container, RemoteRepository repository) {
		super(response, container, repository);
	}

	@Override
	public Folder createFolder(String name) throws RepositoryException {
		// check for possible invalid name
		if (!RepositoryLocation.isNameValid(name)) {
			throw new RepositoryException(I18N.getMessage(I18N.getErrorBundle(), "repository.illegal_entry_name", name, getLocation()));
		}

		EntryResponse response = getRepository().getRepositoryService().makeFolder(getPath(), name);
		if (response.getStatus() != RepositoryConstants.OK) {
			throw new RepositoryException(response.getErrorMessage());
		}
		RemoteFolder newFolder = new RemoteFolder(response, this, getRepository());
		if (folders != null) {
			folders.add(newFolder);
			Collections.sort(folders, nameComparator);
			getRepository().fireEntryAdded(newFolder, this);
		}
		return newFolder;
	}

	@Override
	public BlobEntry createBlobEntry(String name) throws RepositoryException {
		// check for possible invalid name
		if (!RepositoryLocation.isNameValid(name)) {
			throw new RepositoryException(I18N.getMessage(I18N.getErrorBundle(), "repository.illegal_entry_name", name, getLocation()));
		}

		EntryResponse response = getRepository().getRepositoryService().createBlob(getPath(), name);
		RemoteBlobEntry newBlob = new RemoteBlobEntry(response, this, getRepository());
		if (this.entries != null) {
			entries.add(newBlob);
			Collections.sort(entries, nameComparator);
			getRepository().fireEntryAdded(newBlob, this);
		}
		return newBlob;
	}

	@Override
	public List<DataEntry> getDataEntries() throws RepositoryException {
		ensureLoaded();
		return entries;
	}

	private void ensureLoaded() throws RepositoryException {
		synchronized (lock) {
			if (forbidden) {
				return;
			}
			if ((entries == null) || (folders == null)) {				
				FolderContentsResponse response;
				String path = getPath();
				RemoteRepository repository = getRepository();
				RepositoryService repositoryService = repository.getRepositoryService();
				List<DataEntry> newEntries = new LinkedList<DataEntry>();
				List<Folder> newFolders = new LinkedList<Folder>();
				try {
					if (repositoryService == null) {
						return;
					}
					response = repositoryService.getFolderContents(path);
					if (response.getStatus() != RepositoryConstants.OK) {
						if (response.getStatus() == RepositoryConstants.ACCESS_DENIED) {
							readOnly = true;
							forbidden = true;
						} else {
							getLogger().warning("Cannot get folder: " + response.getErrorMessage());
						}
						return;
					}
					for (EntryResponse entry : response.getEntries()) {
						if (entry.getType().equals(Folder.TYPE_NAME)) {
							newFolders.add(new RemoteFolder(entry, this, repository));
						} else if (entry.getType().equals(ProcessEntry.TYPE_NAME)) {
							newEntries.add(new RemoteProcessEntry(entry, this, repository));
						} else if (entry.getType().equals(IOObjectEntry.TYPE_NAME)) {
							newEntries.add(new RemoteIOObjectEntry(entry, this, repository));
						} else if (entry.getType().equals(BlobEntry.TYPE_NAME)) {
							newEntries.add(new RemoteBlobEntry(entry, this, repository));
						} else {
							getLogger().warning("Unknown entry type: " + entry.getType());
						}
					}
				} finally {
					// atomically set entries and folders
					// lock used in willBlock()
					synchronized (childrenLock) {
						entries = newEntries;
						folders = newFolders;
					}
				}
			}
			return;
		}
	}

	@Override
	public List<Folder> getSubfolders() throws RepositoryException {
		ensureLoaded();
		return folders;
	}

	@Override
	public String getType() {
		return Folder.TYPE_NAME;
	}

	@Override
	public boolean willBlock() {
		synchronized (childrenLock) {
			return (folders == null) || (entries == null);
		}
	}

	@Override
	public void refresh() throws RepositoryException {
		folders = null;
		entries = null;
		readOnly = false;
		forbidden = false;
		ensureLoaded();
		getRepository().fireRefreshed(this);
	}

	@Override
	public boolean containsEntry(String name) throws RepositoryException {
		ensureLoaded();
		for (Folder folder : folders) {
			if (folder.getName().equals(name)) {
				return true;
			}
		}
		for (DataEntry entry : entries) {
			if (entry.getName().equals(name)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public IOObjectEntry createIOObjectEntry(String name, IOObject ioobject, Operator callingOperator, ProgressListener l) throws RepositoryException {
		// check for possible invalid name
		if (!RepositoryLocation.isNameValid(name)) {
			throw new RepositoryException(I18N.getMessage(I18N.getErrorBundle(), "repository.illegal_entry_name", name, getLocation()));
		}

		RepositoryLocation loc;
		try {
			loc = new RepositoryLocation(getLocation(), name);
		} catch (MalformedRepositoryLocationException e) {
			throw new RepositoryException(e);
		}
		RemoteIOObjectEntry.storeData(ioobject, loc.getPath(), getRepository(), l);
		EntryResponse response = getRepository().getRepositoryService().getEntry(loc.getPath());
		if (response.getStatus() != 0) {
			throw new RepositoryException(response.getErrorMessage());
		}
		RemoteIOObjectEntry entry = new RemoteIOObjectEntry(response, this, getRepository());
		if (entries != null) {
			entries.add(entry);
			getRepository().fireEntryAdded(entry, this);
		}
		return entry;
	}

	@Override
	public ProcessEntry createProcessEntry(String name, String processXML) throws RepositoryException {
		// check for possible invalid name
		if (!RepositoryLocation.isNameValid(name)) {
			throw new RepositoryException(I18N.getMessage(I18N.getErrorBundle(), "repository.illegal_entry_name", name, getLocation()));
		}

		RepositoryLocation loc;
		try {
			loc = new RepositoryLocation(getLocation(), name);
		} catch (MalformedRepositoryLocationException e) {
			throw new RepositoryException(e);
		}
		Response response = getRepository().getRepositoryService().storeProcess(loc.getPath(), processXML, null);
		if (response.getStatus() != 0) {
			throw new RepositoryException(response.getErrorMessage());
		}
		EntryResponse entryResponse = getRepository().getRepositoryService().getEntry(loc.getPath());
		if (entryResponse.getStatus() != 0) {
			throw new RepositoryException(entryResponse.getErrorMessage());
		}
		RemoteProcessEntry entry = new RemoteProcessEntry(entryResponse, this, getRepository());
		if (entries != null) {
			entries.add(entry);
		}
		getRepository().fireEntryAdded(entry, this);
		return entry;
	}

	void removeChild(RemoteEntry remoteEntry) {
		if (remoteEntry instanceof Folder) {
			int index = folders.indexOf(remoteEntry);
//			Folder to
//			for (Folder folder : folders) {
//				index++;
//				if (folder.getName().equals(remoteEntry.getName())) {
//					break;
//				}				
//			}
			folders.remove(remoteEntry);
			getRepository().fireEntryRemoved(remoteEntry, this, index);
		} else {
			int index = entries.indexOf(remoteEntry) + folders.size();
			entries.remove(remoteEntry);
			getRepository().fireEntryRemoved(remoteEntry, this, index);
		}
	}

	@Override
	public boolean isReadOnly() {
		return readOnly;
	}

	@Override
	public boolean move(Folder newParent) throws RepositoryException {
		boolean wasLoaded = !willBlock();
		folders = null;
		entries = null;
		readOnly = false;
		forbidden = false;
		boolean success = super.move(newParent);
		if (wasLoaded) {
			ensureLoaded();
			getRepository().fireRefreshed(this);
		}
		return success;
	}

	@Override
	public boolean move(Folder newParent, String newName) throws RepositoryException {
		boolean wasLoaded = !willBlock();
		folders = null;
		entries = null;
		readOnly = false;
		forbidden = false;
		boolean success = super.move(newParent, newName);
		if (wasLoaded) {
			ensureLoaded();
			getRepository().fireRefreshed(this);
		}
		return success;
	}

	@Override
	public boolean rename(String newName) throws RepositoryException {
		boolean wasLoaded = !willBlock();
		folders = null;
		entries = null;
		readOnly = false;
		forbidden = false;
		boolean success = super.rename(newName);
		if (wasLoaded) {
			ensureLoaded();
			getRepository().fireRefreshed(this);
		}
//
//		if (success) {
//			refresh();
//		}
		getRepository().fireRefreshed(this);
		return success;
	}

	@Override
	public boolean canRefreshChild(String childName) throws RepositoryException {
		EntryResponse entryResponse = getRepository().getRepositoryService().getEntry(getPath() + RepositoryLocation.SEPARATOR + childName);
		return entryResponse.getStatus() == RepositoryConstants.OK;
	}
}
