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

import com.rapid_i.repository.wsimport.EntryResponse;
import com.rapidminer.repository.DataEntry;
/**
 * @author Simon Fischer
 */
public abstract class RemoteDataEntry extends RemoteEntry implements DataEntry {
		
	private int revision;
	private String type;
	private long size;
	private long date;
	
	RemoteDataEntry(EntryResponse response, RemoteFolder container, RemoteRepository repository) {
		super(response, container, repository);
		extractData(response);
	}

	/**
	 * @param response
	 */
	@Override
	protected void extractData(EntryResponse response) {
		super.extractData(response);
		this.revision = response.getLatestRevision();
		this.type = response.getType();
		this.size = response.getSize();
		this.date = response.getDate();
	}
	
	@Override
	public int getRevision() {
		return revision;
	}	

	@Override
	public String getType() {
		return type;
	}

	@Override
	public long getSize() {
		return size;
	}

	public long getDate() {
		return date;
	}
	
	public void setDate(long date) {
		this.date = date;
	}
}
