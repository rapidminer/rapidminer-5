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

import java.util.Collection;
import java.util.Date;

import javax.swing.Action;

import com.rapid_i.repository.wsimport.EntryResponse;
import com.rapid_i.repository.wsimport.ProcessContentsResponse;
import com.rapid_i.repository.wsimport.Response;
import com.rapidminer.io.process.XMLTools;
import com.rapidminer.repository.ProcessEntry;
import com.rapidminer.repository.RepositoryConstants;
import com.rapidminer.repository.RepositoryException;
/**
 * @author Simon Fischer
 */
public class RemoteProcessEntry extends RemoteDataEntry implements ProcessEntry {

	private String xml;
	private final Object lock = new Object();
	
	public RemoteProcessEntry(EntryResponse response, RemoteFolder container, RemoteRepository repository) {
		super(response, container, repository);
	}

	private void ensureLoaded() throws RepositoryException {
		synchronized (lock) {
			if (xml == null) {
				ProcessContentsResponse response = getRepository().getRepositoryService().getProcessContents(getPath(), 0);
				if (response.getStatus() != RepositoryConstants.OK) {
					xml = "Cannot fetch process: "+response.getErrorMessage();
					getLogger().warning("Cannot fetch process: "+response.getErrorMessage());
					throw new RepositoryException("Cannot fetch process: "+response.getErrorMessage());
				} else {
					xml = response.getContents();
					//setDate(getRepository().getRepositoryService().getEntry(getPath()).getDate());
				}
			}
		}
	}
	
	@Override
	public String retrieveXML() throws RepositoryException {
		ensureLoaded();
		return xml;
	}

	@Override
	public void storeXML(String xml) throws RepositoryException {		
		Response response = getRepository().getRepositoryService().storeProcess(getPath(), xml, XMLTools.getXMLGregorianCalendar(new Date(getDate())));
		if (response.getStatus() != RepositoryConstants.OK) {
			throw new RepositoryException(response.getErrorMessage());
		}
		setDate(getRepository().getRepositoryService().getEntry(getPath()).getDate());
		this.xml = null;
	}

	@Override
	public boolean willBlock() {
		return xml == null;
	}

	@Override
	public Collection<Action> getCustomActions() {
		Collection<Action> actions = super.getCustomActions();
		actions.add(new NewRevisionAction(this));
		return actions;
	}
}
