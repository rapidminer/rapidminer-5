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
package com.rapidminer;

import java.io.IOException;
import java.util.logging.Level;

import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.repository.Entry;
import com.rapidminer.repository.Folder;
import com.rapidminer.repository.ProcessEntry;
import com.rapidminer.repository.RepositoryException;
import com.rapidminer.repository.RepositoryLocation;
import com.rapidminer.tools.LogService;
import com.rapidminer.tools.ProgressListener;
import com.rapidminer.tools.XMLException;
/**
 * @author Simon Fischer
 */
public class RepositoryProcessLocation implements ProcessLocation {
	
	private final RepositoryLocation repositoryLocation;

	public RepositoryProcessLocation(RepositoryLocation location) {
		super();
		this.repositoryLocation = location;
	}

	private ProcessEntry getEntry() throws IOException {
		Entry entry;
		try {
			entry = repositoryLocation.locateEntry();
		} catch (RepositoryException e) {
			throw new IOException("Cannot locate entry '"+repositoryLocation+"': "+e, e);
		}
		if (entry == null) {
			throw new IOException("No such entry: "+repositoryLocation);			
		} else if (!(entry instanceof ProcessEntry)) {
			throw new IOException("No process entry: "+repositoryLocation);
		} else {
			return (ProcessEntry) entry;
		}
	}
	
	@Override
	public String getRawXML() throws IOException {
		try {
			return getEntry().retrieveXML();
		} catch (RepositoryException e) {
			throw new IOException("Cannot access entry '"+repositoryLocation+"': "+e, e);
		}
	}

	@Override
	public Process load(ProgressListener listener) throws IOException, XMLException {
		if (listener != null) {
			listener.setCompleted(60);
		}
		final String xml = getRawXML();
		Process process = new Process(xml);
		process.setProcessLocation(this);
		if (listener != null) {
			listener.setCompleted(80);
		}
		return process;
	}

	@Override
	public String toHistoryFileString() {
		return "repository "+repositoryLocation.toString();
	}

	@Override
	public void store(Process process, ProgressListener listener) throws IOException {		
		try {
			Entry entry = repositoryLocation.locateEntry();
			if (entry == null) {
				Folder folder = repositoryLocation.parent().createFoldersRecursively();
				folder.createProcessEntry(repositoryLocation.getName(), process.getRootOperator().getXML(false));
			} else {
				if (entry instanceof ProcessEntry) {
					boolean isReadOnly = repositoryLocation.getRepository().isReadOnly();
					if (isReadOnly) {
						SwingTools.showSimpleErrorMessage("save_to_read_only_repo", "", repositoryLocation.toString());
					} else {
						((ProcessEntry) entry).storeXML(process.getRootOperator().getXML(false));
					}
				} else {
					throw new RepositoryException("Entry "+repositoryLocation+" is not a process entry.");
				}
			}
			//LogService.getRoot().info("Saved process definition at '" + repositoryLocation + "'.");
			LogService.getRoot().log(Level.INFO, "com.rapidminer.RepositoryProcessLocation.saved_process_definition", repositoryLocation);
		} catch (RepositoryException e) {
			throw new IOException("Cannot store process at "+repositoryLocation+": "+e.getMessage(), e);
		}
	}

	public RepositoryLocation getRepositoryLocation() {
		return repositoryLocation;
	}

	@Override
	public String toMenuString() {
		return repositoryLocation.toString();
	}
	
	@Override
	public String toString() {
		return toMenuString();	
	}
	
	
	@Override
	public boolean equals(Object o) {
		if (!(o instanceof RepositoryProcessLocation)) {
			return false;
		} else {
			return ((RepositoryProcessLocation)o).repositoryLocation.equals(this.repositoryLocation);
		}
	}
	
	@Override
	public int hashCode() {
		return repositoryLocation.hashCode();
	}

	@Override
	public String getShortName() {
		return repositoryLocation.getName();
	}
}
