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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.util.logging.Level;

import com.rapid_i.repository.wsimport.EntryResponse;
import com.rapidminer.repository.BlobEntry;
import com.rapidminer.repository.RepositoryException;
import com.rapidminer.repository.remote.RemoteRepository.EntryStreamType;
import com.rapidminer.tools.LogService;
import com.rapidminer.tools.WebServiceTools;

/**
 * Reference on BLOB entries in the repository, if using a remote repository.
 * 
 * @author Simon Fischer
 */
public class RemoteBlobEntry extends RemoteDataEntry implements BlobEntry {

	/**
	 * TODO: Retrieve from Web service
	 */
	private String mimeType = "application/octet-stream";

	RemoteBlobEntry(EntryResponse response, RemoteFolder container, RemoteRepository repository) {
		super(response, container, repository);
	}
	
	@Override
	public String getMimeType() {
		//TODO: This mimetype should be retrieved from somewhere
		return mimeType;
	}

	@Override
	public InputStream openInputStream() throws RepositoryException {
		try {
			HttpURLConnection conn = getRepository().getResourceHTTPConnection(getLocation().getPath(), EntryStreamType.BLOB, false);
			WebServiceTools.setURLConnectionDefaults(conn);
			conn.setDoOutput(false);
			conn.setDoInput(true);
			try {
				mimeType = conn.getContentType();				
				return conn.getInputStream();
			} catch (IOException e) {
				throw new RepositoryException("Cannot download object: " + conn.getResponseCode()+": "+conn.getResponseMessage(), e);	
			}
		} catch (IOException e) {
			throw new RepositoryException("Cannot open connection to '"+getLocation()+"': "+e, e);
		}		
	}

	@Override
	public OutputStream openOutputStream(String mimeType) throws RepositoryException {
		this.mimeType = mimeType;
		try {
			final HttpURLConnection conn = getRepository().getResourceHTTPConnection(getLocation().getPath(), EntryStreamType.BLOB, false);
			WebServiceTools.setURLConnectionDefaults(conn);
			conn.setDoOutput(true);
			conn.setDoInput(true);
			conn.setRequestProperty("Content-Type", mimeType);
			final OutputStream out;
			try {
				out = conn.getOutputStream();
			} catch (IOException e) {
				throw new RepositoryException("Cannot upload object: " + conn.getResponseCode()+": "+conn.getResponseMessage(), e);
			}
			//return out;
			return new OutputStream() {
				@Override
				public void flush() throws IOException {
					out.flush();
				}
				@Override
				public void write(byte[] b) throws IOException {
					out.write(b);
				}
				@Override
				public void write(byte[] b, int off, int len) throws IOException {
					out.write(b, off, len);
				}
				@Override
				public void write(int b) throws IOException {
					out.write(b);
				}
				@Override
				public void close() throws IOException {
					super.close();
					out.close();
					
					int code = conn.getResponseCode();
					String error = conn.getResponseMessage();
					if ((code < 200) || (code >= 300)) {						
						throw new IOException("Upload failed. Server responded with code "+code+": "+error);
					} else {
						//LogService.getRoot().info("Uploaded blob. ("+code+": "+error+")");
						LogService.getRoot().log(Level.INFO, "com.rapidminer.repository.remote.RemoteBlobEntry.uploaded_blob", new Object[] {code, error});
						try {
							EntryResponse entryResponse = getRepository().getRepositoryService().getEntry(getPath());
							extractData(entryResponse);
							RemoteBlobEntry.this.getRepository().fireEntryChanged(RemoteBlobEntry.this);
						} catch (RepositoryException e) {
							throw new IOException("Failed to refresh data after upload: "+e, e);
						}					
					}
				}
				
			};
		} catch (IOException e) {
			throw new RepositoryException("Cannot open connection to '"+getLocation()+"': "+e, e);
		}		
	}

	@Override
	public boolean willBlock() {
		return false;
	}
}
