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
import java.util.Map;
import java.util.Map.Entry;

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
import com.rapidminer.tools.ProgressListener;
import com.rapidminer.tools.jdbc.ColumnIdentifier;
import com.rapidminer.tools.jdbc.TableName;
import com.rapidminer.tools.jdbc.connection.ConnectionEntry;

/** Represents all tables in a single {@link ConnectionEntry} converted to {@link IOObject}s
 *  using a particular {@link DBConnectionToIOObjectConverter}.
 * 
 * @author Simon Fischer
 *
 */
public class DBConnectionConverterFolder implements Folder {

	private final ConnectionEntry entry;
	private final DBConnectionToIOObjectConverter converter;
	private final DBRepository repository;
	private final DBConnectionFolder parent;
	private final Map<TableName, List<ColumnIdentifier>> allTableMetaData;
	
	private List<DataEntry> entries;	
	
	public DBConnectionConverterFolder(DBRepository dbRepository,
			DBConnectionFolder parent,
			ConnectionEntry dbConEntry, 
			DBConnectionToIOObjectConverter converter,
			Map<TableName, List<ColumnIdentifier>> allTableMetaData) throws RepositoryException {
		this.repository = dbRepository;
		this.parent = parent;
		this.entry = dbConEntry;
		this.converter = converter;
		this.allTableMetaData = allTableMetaData;
		this.ensureLoaded();
	}

	@Override
	public String getName() {
		return  converter.getSuffix();
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
		throw new RepositoryException("Cannot rename connection entry.");
	}

	@Override
	public boolean move(Folder newParent) throws RepositoryException {
		throw new RepositoryException("Cannot move connection entry.");
	}
	
	@Override
	public boolean move(Folder newParent, String newName) throws RepositoryException {
		throw new RepositoryException("Cannot move connection entry.");
	}

	@Override
	public Folder getContainingFolder() {
		return repository;
	}

	@Override
	public boolean willBlock() {
		return false;
	}

	@Override
	public RepositoryLocation getLocation() {
		try {
			return new RepositoryLocation(this.parent.getLocation(), getName());
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
	public List<DataEntry> getDataEntries() throws RepositoryException {
		return entries;
	}

	@Override
	public List<Folder> getSubfolders() throws RepositoryException {
		return Collections.emptyList();
	}

	@Override
	public void refresh() throws RepositoryException {
		entries = null;
		ensureLoaded();
		repository.fireRefreshed(this);		
	}

	@Override
	public boolean containsEntry(String name) throws RepositoryException {
		for (DataEntry entry : entries) {
			if (entry.getName().equals(name)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public Folder createFolder(String name) throws RepositoryException {
		throw new RepositoryException("Cannot create folder in connection entry.");
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
	
	protected ConnectionEntry getConnectionEntry() {
		return entry;
	}

	private void ensureLoaded() throws RepositoryException {
		if (entries == null) {
			entries = new LinkedList<DataEntry>();
			for (Entry<TableName, List<ColumnIdentifier>> tableEntry : allTableMetaData.entrySet()) {
				entries.add(new DBConnectionEntry(this, converter, tableEntry.getKey(), tableEntry.getValue()));
			}
		}		
	}
	
	@Override
	public boolean canRefreshChild(String childName) throws RepositoryException {
		return containsEntry(childName);
	}

}
