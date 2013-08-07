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
package com.rapid_i.deployment.update.client;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * 
 * @author Simon Fischer
 *
 */
public class InMemoryZipFile {
	
	private Map<String,byte[]> contents = new HashMap<String,byte[]>();
	
	public InMemoryZipFile(byte[] zipBuffer) throws IOException {
		ZipInputStream zin = new ZipInputStream(new ByteArrayInputStream(zipBuffer));
		ZipEntry entry;
		while ((entry = zin.getNextEntry()) != null) {
			if (entry.isDirectory()) {
				continue;
			}
			int length;
			ByteArrayOutputStream buffer = new ByteArrayOutputStream();
			byte[] buf = new byte[10*1024];
			while ((length = zin.read(buf)) != -1) {
				buffer.write(buf, 0, length);
			}
			zin.closeEntry();
			contents.put(entry.getName(), buffer.toByteArray());
		}
		zin.close();
	}
	
	public Set<String> entryNames() {
		return contents.keySet();
	}
	
	public byte[] getContents(String name) {
		return contents.get(name);
	}

	public boolean containsEntry(String name) {
		return contents.containsKey(name);
	}
}
