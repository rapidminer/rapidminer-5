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
package com.rapidminer.repository.db;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import javax.swing.Action;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.rapidminer.operator.IOObject;
import com.rapidminer.operator.Operator;
import com.rapidminer.repository.BlobEntry;
import com.rapidminer.repository.DataEntry;
import com.rapidminer.repository.Entry;
import com.rapidminer.repository.Folder;
import com.rapidminer.repository.IOObjectEntry;
import com.rapidminer.repository.MalformedRepositoryLocationException;
import com.rapidminer.repository.ProcessEntry;
import com.rapidminer.repository.Repository;
import com.rapidminer.repository.RepositoryException;
import com.rapidminer.repository.RepositoryListener;
import com.rapidminer.repository.RepositoryLocation;
import com.rapidminer.repository.RepositoryManager;
import com.rapidminer.repository.gui.RepositoryConfigurationPanel;
import com.rapidminer.tools.I18N;
import com.rapidminer.tools.ProgressListener;
import com.rapidminer.tools.jdbc.connection.DatabaseConnectionService;
import com.rapidminer.tools.jdbc.connection.FieldConnectionEntry;

/** A repository to make database tables in defined connections accessible
 *  directly as IOObjects.
 * 
 * @author Simon Fischer
 *
 */
public class DBRepository implements Repository {

	private List<Folder> folders = null;
	private List<RepositoryListener> repositoryListeners = new LinkedList<RepositoryListener>();
	private String name = "DB";
	
	private static final List<DBConnectionToIOObjectConverter> CONVERTERS = new LinkedList<DBConnectionToIOObjectConverter>(); 
	static {
		CONVERTERS.add(new StandardDBConnectionToIOObjectConverter());
	}
	
	/** Adds a converter to the converter registry that will be used by all entries. */
	public static void registerConverter(DBConnectionToIOObjectConverter converter) {
		CONVERTERS.add(converter);
	}
	
	@Override
	public List<DataEntry> getDataEntries() throws RepositoryException {
		return Collections.emptyList();
	}

	@Override
	public List<Folder> getSubfolders() throws RepositoryException {
		ensureLoaded();
		return folders;
	}

	@Override
	public void refresh() throws RepositoryException {
		folders = null;
		fireRefreshed(this);
	}

	@Override
	public boolean containsEntry(String name) throws RepositoryException {
		ensureLoaded();
		for (Folder entry : folders) {
			if (entry.getName().equals(name)) {
				return false;
			}
		}
		return false;
	}

	@Override
	public Folder createFolder(String name) throws RepositoryException {
		throw new RepositoryException("Cannot create folders in connection entry.");
	}

	@Override
	public IOObjectEntry createIOObjectEntry(String name, IOObject ioobject, Operator callingOperator, ProgressListener progressListener) throws RepositoryException {
		throw new RepositoryException("Cannot create items in connection entry.");
	}

	@Override
	public ProcessEntry createProcessEntry(String name, String processXML) throws RepositoryException {
		throw new RepositoryException("Cannot create items in connection entry.");
	}

	@Override
	public BlobEntry createBlobEntry(String name) throws RepositoryException {
		throw new RepositoryException("Cannot create items in connection entry.");
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public String getType() {
		return Repository.TYPE_NAME;
	}

	@Override
	public String getOwner() {
		return null;
	}

	@Override
	public String getDescription() {
		return "List of defined database connections";
	}

	@Override
	public boolean isReadOnly() {
		return true;
	}

	@Override
	public boolean rename(String newName) throws RepositoryException {
		throw new RepositoryException("Cannot rename items in connection entry.");
	}

	@Override
	public boolean move(Folder newParent) throws RepositoryException {
		throw new RepositoryException("Cannot move items in connection entry.");
	}
	
	@Override
	public boolean move(Folder newParent, String newName) throws RepositoryException {
		throw new RepositoryException("Cannot move items in connection entry.");
	}

	@Override
	public Folder getContainingFolder() {
		return null;
	}

	@Override
	public boolean willBlock() {
		return folders == null;
	}

	@Override
	public RepositoryLocation getLocation() {
		try {
			return new RepositoryLocation(name , new String[0]);
		} catch (MalformedRepositoryLocationException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void delete() throws RepositoryException {
		throw new RepositoryException("Cannot delete connection entry.");
	}

	@Override
	public Collection<Action> getCustomActions() {
		return Collections.emptyList();
	}

	@Override
	public void addRepositoryListener(RepositoryListener l) {
		repositoryListeners.add(l);
	}

	@Override
	public void removeRepositoryListener(RepositoryListener l) {
		repositoryListeners.remove(l);
	}

	@Override
	public Entry locate(String entry) throws RepositoryException {
		return RepositoryManager.getInstance(null).locate(this, entry, false);
	}

	@Override
	public String getState() {
		return null;
	}
	
	@Override
	public String getIconName() {
		return I18N.getMessage(I18N.getGUIBundle(), "gui.repository.db.icon");
	}

	@Override
	public Element createXML(Document doc) {
		return null;
	}

	@Override
	public boolean shouldSave() {
		return false;
	}

	@Override
	public void postInstall() {
	}

	@Override
	public void preRemove() {
	}

	@Override
	public boolean isConfigurable() {
		return false;
	}

	@Override
	public RepositoryConfigurationPanel makeConfigurationPanel() {
		throw new RuntimeException("DB connection repository is not configurable.");
	}



	private void ensureLoaded() {
		if (folders == null) {
			folders = new LinkedList<Folder>();
			for (FieldConnectionEntry dbConEntry : DatabaseConnectionService.getConnectionEntries()) {
				folders.add(new DBConnectionFolder(this, dbConEntry));
			}
		}
	}

	protected void fireRefreshed(Folder folder) {
		for (RepositoryListener l : this.repositoryListeners) {
			l.folderRefreshed(folder);
		}		
	}

	protected List<DBConnectionToIOObjectConverter> getConverters() {
		return CONVERTERS;
	}

	@Override
	public boolean canRefreshChild(String childName) throws RepositoryException {
		return containsEntry(childName);
	}

}
