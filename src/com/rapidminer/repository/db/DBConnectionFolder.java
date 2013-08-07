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

import java.sql.SQLException;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import javax.swing.Action;

import com.rapidminer.operator.IOObject;
import com.rapidminer.operator.Operator;
import com.rapidminer.repository.BlobEntry;
import com.rapidminer.repository.DataEntry;
import com.rapidminer.repository.Folder;
import com.rapidminer.repository.IOObjectEntry;
import com.rapidminer.repository.MalformedRepositoryLocationException;
import com.rapidminer.repository.ProcessEntry;
import com.rapidminer.repository.RepositoryException;
import com.rapidminer.repository.RepositoryLocation;
import com.rapidminer.tools.I18N;
import com.rapidminer.tools.LogService;
import com.rapidminer.tools.ProgressListener;
import com.rapidminer.tools.jdbc.ColumnIdentifier;
import com.rapidminer.tools.jdbc.DatabaseHandler;
import com.rapidminer.tools.jdbc.TableName;
import com.rapidminer.tools.jdbc.connection.ConnectionEntry;
import com.rapidminer.tools.jdbc.connection.FieldConnectionEntry;

/** Represents all tables in a single {@link ConnectionEntry}. This folder will have
 *  one subfolder per {@link DBConnectionToIOObjectConverter} instance that is registered.
 * 
 * @author Simon Fischer
 *
 */
public class DBConnectionFolder implements Folder {

	private ConnectionEntry entry;
	private DBRepository repository;

	private List<Folder> folders;
	
	public DBConnectionFolder(DBRepository dbRepository, FieldConnectionEntry dbConEntry) {
		this.repository = dbRepository;
		this.entry = dbConEntry;
	}

	@Override
	public String getName() {
		return entry.getName();
	}

	@Override
	public String getType() {
		return Folder.TYPE_NAME;
	}

	@Override
	public String getOwner() {
		return null;
	}

	@Override
	public String getDescription() {
		return getName() + " ("+entry.getURL()+")";
	}

	@Override
	public boolean isReadOnly() {
		return true;
	}

	@Override
	public boolean rename(String newName) throws RepositoryException {
		throw new RepositoryException("This is a read-only view on a database. Cannot rename entries.");
	}

	@Override
	public boolean move(Folder newParent) throws RepositoryException {
		throw new RepositoryException("This is a read-only view on a database. Cannot move entries.");
	}
	
	@Override
	public boolean move(Folder newParent, String newName) throws RepositoryException {
		throw new RepositoryException("This is a read-only view on a database. Cannot move or rename entries");
	}

	@Override
	public Folder getContainingFolder() {
		return repository;
	}

	@Override
	public boolean willBlock() {
		return folders == null;
	}

	@Override
	public RepositoryLocation getLocation() {
		try {
			return new RepositoryLocation(this.repository.getLocation(), getName());
		} catch (MalformedRepositoryLocationException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void delete() throws RepositoryException {
		throw new RepositoryException("This is a read-only view on a database. Cannot delete entries.");		
	}

	@Override
	public Collection<Action> getCustomActions() {
		return Collections.emptyList();
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
		ensureLoaded();
		repository.fireRefreshed(this);		
	}

	@Override
	public boolean containsEntry(String name) throws RepositoryException {
		ensureLoaded();
		for (Folder entry : folders) {
			if (entry.getName().equals(name)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public Folder createFolder(String name) throws RepositoryException {
		throw new RepositoryException("This is a read-only view on a database. Cannot create folders.");
	}

	@Override
	public IOObjectEntry createIOObjectEntry(String name, IOObject ioobject, Operator callingOperator, ProgressListener progressListener) throws RepositoryException {
		throw new RepositoryException("This is a read-only view on a database. Cannot create new entries.");
	}

	@Override
	public ProcessEntry createProcessEntry(String name, String processXML) throws RepositoryException {
		throw new RepositoryException("This is a read-only view on a database. Cannot create new entries.");		
	}

	@Override
	public BlobEntry createBlobEntry(String name) throws RepositoryException {
		throw new RepositoryException("This is a read-only view on a database. Cannot create new entries.");
	}
	
	protected ConnectionEntry getConnectionEntry() {
		return entry;
	}

	private void ensureLoaded() throws RepositoryException {
		if (folders == null) {
			folders = new LinkedList<Folder>();
			DatabaseHandler handler = null;
			try {
				handler = DatabaseHandler.getConnectedDatabaseHandler(entry);
				Map<TableName, List<ColumnIdentifier>> allTableMetaData = handler.getAllTableMetaData();				
				for (DBConnectionToIOObjectConverter converter : repository.getConverters()) {
					folders.add(new DBConnectionConverterFolder(repository, this, entry, converter, allTableMetaData)); 
				}
			} catch (SQLException e) {
				throw new RepositoryException("Failed to load table data: "+e ,e);
			} finally {
				if ((handler != null) && (handler.getConnection() != null)) {
					try {
						handler.getConnection().close();
					} catch (SQLException e) {
						//LogService.getRoot().log(Level.WARNING, "Failed to close connection: "+e, e);
						LogService.getRoot().log(Level.WARNING,
								I18N.getMessage(LogService.getRoot().getResourceBundle(), 
								"com.rapidminer.repository.db.DBConnectionFolder.closing_connection_error", 
								e),
								e);
					}
				}
			}
		}		
	}
	
	@Override
	public boolean canRefreshChild(String childName) throws RepositoryException {
		return containsEntry(childName);
	}

}
