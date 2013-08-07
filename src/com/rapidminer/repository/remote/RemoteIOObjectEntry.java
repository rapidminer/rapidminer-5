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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.ref.SoftReference;
import java.net.HttpURLConnection;
import java.util.logging.Level;

import com.rapid_i.repository.wsimport.EntryResponse;
import com.rapidminer.operator.IOObject;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.ports.metadata.MetaData;
import com.rapidminer.operator.tools.IOObjectSerializer;
import com.rapidminer.repository.IOObjectEntry;
import com.rapidminer.repository.RepositoryException;
import com.rapidminer.repository.remote.RemoteRepository.EntryStreamType;
import com.rapidminer.tools.LogService;
import com.rapidminer.tools.ProgressListener;
import com.rapidminer.tools.WebServiceTools;

/**
 * @author Simon Fischer
 */
public class RemoteIOObjectEntry extends RemoteDataEntry implements IOObjectEntry {

	private SoftReference<MetaData> metaData;
	private final Object metaDatalLock = new Object();
	private String ioObjectClassName;

	RemoteIOObjectEntry(EntryResponse response, RemoteFolder container, RemoteRepository repository) {
		super(response, container, repository);
		ioObjectClassName = response.getIoObjectClassName();
	}

	@Override
	public IOObject retrieveData(ProgressListener l) throws RepositoryException {
		if (l != null) {
			l.setTotal(100);
		}
		InputStream in = null;
		try {
			HttpURLConnection connection = getRepository().getResourceHTTPConnection(getLocation().getPath(), EntryStreamType.IOOBJECT, false);
			WebServiceTools.setURLConnectionDefaults(connection);
			connection.setDoInput(true);
			connection.setDoOutput(false);
			connection.setRequestMethod("GET");
			try {
				in = connection.getInputStream();
			} catch (IOException e) {
				throw new RepositoryException("Cannot download IOObject: " + connection.getResponseCode() + ": " + connection.getResponseMessage(), e);
			}
			Object result = IOObjectSerializer.getInstance().deserialize(in);
			if (result instanceof IOObject) {
				return (IOObject) result;
			} else {
				throw new RepositoryException("Server did not send I/O-Object, but instance of " + result.getClass());
			}
		} catch(RepositoryException e) {
			throw e;
		} catch (Exception e) {
			throw new RepositoryException("Cannot parse I/O-Object: " + e, e);
		} finally {
			if (l != null) {
				l.complete();
			}
			if(in != null) {
				try {
					in.close();
				} catch (IOException e) {}
			}
		}
	}

	@Override
	public MetaData retrieveMetaData() throws RepositoryException {
		synchronized (metaDatalLock) {
			if (metaData != null) {
				MetaData storedData = metaData.get();
				if (storedData != null) {
					return storedData;
				}
			}
			// otherwise metaData == null OR get() == null
			try {
				HttpURLConnection connection = getRepository().getResourceHTTPConnection(getLocation().getPath(), EntryStreamType.METADATA, false);
				WebServiceTools.setURLConnectionDefaults(connection);
				connection.setRequestMethod("GET");
				InputStream in;
				try {
					in = connection.getInputStream();
				} catch (IOException e) {
					throw new RepositoryException("Cannot download meta data: " + connection.getResponseCode() + ": " + connection.getResponseMessage(), e);
				}
				Object result = IOObjectSerializer.getInstance().deserialize(in);
				if (result instanceof MetaData) {
					this.metaData = new SoftReference<MetaData>((MetaData) result);
					return (MetaData) result;
				} else {
					throw new RepositoryException("Server did not send MetaData, but instance of " + result.getClass());
				}
			} catch (IOException e) {
				throw new RepositoryException("Cannot parse I/O-Object: " + e, e);
			}
		}		
	}

	@Override
	public void storeData(IOObject ioobject, Operator callingOperator, ProgressListener l) throws RepositoryException {
		storeData(ioobject, getPath(), getRepository(), l);
	}

	protected static void storeData(IOObject ioobject, String location, RemoteRepository repository, ProgressListener l) throws RepositoryException {
		if (l != null) {
			l.setTotal(100);
			l.setCompleted(0);
		}

		HttpURLConnection conn;
		try {
			conn = repository.getResourceHTTPConnection(location, EntryStreamType.IOOBJECT, false);
		} catch (IOException e1) {
			throw new RepositoryException("Cannot store object at " + location + ": " + e1, e1);
		}
		OutputStream out = null;
		try {
			WebServiceTools.setURLConnectionDefaults(conn);
			conn.setRequestProperty("Content-Type", RMContentType.IOOBJECT.getContentTypeString());
			conn.setRequestMethod("POST");
			conn.setDoOutput(true);
			conn.setDoInput(true);
			try {
				out = conn.getOutputStream();
			} catch (IOException e) {
				throw new RepositoryException("Cannot upload object: " + conn.getResponseCode() + ": " + conn.getResponseMessage(), e);
			}
			IOObjectSerializer.getInstance().serialize(out, ioobject);
			out.close();
			if ((conn.getResponseCode() < 200) || (conn.getResponseCode() > 299)) {
				throw new RepositoryException("Cannot upload object: " + conn.getResponseCode() + ": " + conn.getResponseMessage());
			}
			BufferedReader in = null;
			try {
				try {
					in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
					//LogService.getRoot().fine("BEGIN Reply of server:");
					LogService.getRoot().log(Level.FINE, "com.rapidminer.repository.remote.RemoteIOObjectEntry.begin_reply_server");
					String line;
					while ((line = in.readLine()) != null) {
						LogService.getRoot().fine(line);
					}
					//LogService.getRoot().fine("END Reply of server.");
					LogService.getRoot().log(Level.FINE, "com.rapidminer.repository.remote.RemoteIOObjectEntry.end_reply_server");
				} catch (IOException e) {
				}
			} finally {
				try {
					if (in != null) {
						in.close();
					}
				} catch (IOException e) {
				}
			}
		} catch (IOException e) {
			try {
				throw new RepositoryException("Cannot store object at " + location + ": " + conn.getResponseCode() + ": " + conn.getResponseMessage(), e);
			} catch (IOException e1) {
				throw new RepositoryException("Cannot store object at " + location + ": " + e, e);
			}
		} finally {
			if (out != null) {
				try {
					out.close();
				} catch (IOException e) {
				}
			}
		}

		if (l != null) {
			l.setCompleted(100);
			l.complete();
		}
	}

	@Override
	public boolean willBlock() {
		return (metaData == null) || (metaData.get() == null);
	}

	@SuppressWarnings("unchecked")
	@Override
	public Class<? extends IOObject> getObjectClass() {
		try {
			return (Class<? extends IOObject>) Class.forName(ioObjectClassName);
		} catch (Exception e) {
			return null;
		}
	}
}
