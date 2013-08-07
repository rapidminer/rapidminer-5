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
import java.util.List;

import javax.swing.Action;

import com.rapidminer.operator.Annotations;
import com.rapidminer.operator.IOObject;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.ports.metadata.ExampleSetMetaData;
import com.rapidminer.operator.ports.metadata.MetaData;
import com.rapidminer.repository.Folder;
import com.rapidminer.repository.IOObjectEntry;
import com.rapidminer.repository.MalformedRepositoryLocationException;
import com.rapidminer.repository.RepositoryException;
import com.rapidminer.repository.RepositoryLocation;
import com.rapidminer.tools.ProgressListener;
import com.rapidminer.tools.jdbc.ColumnIdentifier;
import com.rapidminer.tools.jdbc.TableName;

/**
 * Entry representing an Database Connection.
 * 
 * @author Simon Fischer
 *
 */
public class DBConnectionEntry implements IOObjectEntry {

	private TableName tableName;
	private DBConnectionConverterFolder folder;
	private MetaData metaData;
	private DBConnectionToIOObjectConverter converter;
	
	public DBConnectionEntry(DBConnectionConverterFolder parent, DBConnectionToIOObjectConverter converter, TableName tableName, List<ColumnIdentifier> columns) {
		this.folder = parent;
		this.converter = converter;
		this.tableName = tableName;
		metaData = converter.convertMetaData(folder.getConnectionEntry(), tableName, columns);
		if (tableName.getComment() != null) {
			metaData.getAnnotations().setAnnotation(Annotations.KEY_COMMENT, tableName.getComment());
		}
		metaData.getAnnotations().setAnnotation(Annotations.KEY_SOURCE, tableName.toString());
	}

	@Override
	public int getRevision() {
		return 1;
	}

	@Override
	public long getSize() {
		return -1;
	}

	@Override
	public long getDate() {
		return -1;
	}

	@Override
	public String getName() {	
		return tableName.toString();
	}

	@Override
	public String getType() {
		return IOObjectEntry.TYPE_NAME;
	}

	@Override
	public String getOwner() {
		return folder.getConnectionEntry().getUser();
	}

	@Override
	public String getDescription() {
		return "Table "+getName()+" in "+folder.getConnectionEntry().getURL();
	}

	@Override
	public boolean isReadOnly() {
		return true;
	}

	@Override
	public boolean rename(String newName) throws RepositoryException {
		throw new RepositoryException("This is a read-only view on a database table. Cannot rename entry.");
	}

	@Override
	public boolean move(Folder newParent) throws RepositoryException {
		throw new RepositoryException("This is a read-only view on a database table. Cannot move entry.");
	}
	
	@Override
	public boolean move(Folder newParent, String newName) throws RepositoryException {
		throw new RepositoryException("This is a read-only view on a database table. Cannot move or rename entry.");
	}

	@Override
	public Folder getContainingFolder() {
		return folder;
	}

	@Override
	public boolean willBlock() {
		return metaData == null;
	}

	@Override
	public RepositoryLocation getLocation() {
		try {
			return new RepositoryLocation(this.folder.getLocation(), getName());
		} catch (MalformedRepositoryLocationException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void delete() throws RepositoryException {
		throw new RepositoryException("Cannot delete items in connection entry.");
	}

	@Override
	public Collection<Action> getCustomActions() {
		return Collections.emptyList();
	}

	@Override
	public IOObject retrieveData(ProgressListener l) throws RepositoryException {		
		try {
			final IOObject result = converter.convert(folder.getConnectionEntry(), tableName);
			result.getAnnotations().setAnnotation(Annotations.KEY_SOURCE, tableName.toString());
			if (tableName.getComment() != null) {
				result.getAnnotations().setAnnotation(Annotations.KEY_COMMENT, tableName.getComment());
			}			
			return result;
		} catch (Exception e) {
			throw new RepositoryException("Failed to read data: "+e, e);
		}
	}

	@Override
	public MetaData retrieveMetaData() throws RepositoryException {
		if (metaData == null) { // cannot happen since assigned in constructor
			metaData = new ExampleSetMetaData();
		}
		return metaData;
	}

	@Override
	public void storeData(IOObject data, Operator callingOperator, ProgressListener l) throws RepositoryException {
		throw new RepositoryException("This is a read-only view on a database table. Cannot store data here.");
	}

	@Override
	public Class<? extends IOObject> getObjectClass() {
		return metaData.getObjectClass();
	}
}
