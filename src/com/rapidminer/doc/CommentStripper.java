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
package com.rapidminer.doc;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import com.rapidminer.tools.Tools;


/**
 * Delivers the class comments of the Javadoc comments of an operator class.
 * 
 * @author Simon Fischer
 */
public class CommentStripper {

	private File sourceDir;

	public CommentStripper(File sourceDir) {
		this.sourceDir = sourceDir;
	}

	@SuppressWarnings("fallthrough")
	public String stripClassComment(Class clazz) throws IOException {
		File sourceFile = new File(sourceDir, clazz.getName().replaceAll("\\.", File.separator) + ".java");
		if (!sourceFile.exists())
			throw new FileNotFoundException("No source file found for class " + clazz.getName() + "; source was expected in '" + sourceFile + "'.");
		StringBuffer comment = null;
		BufferedReader reader = new BufferedReader(new FileReader(sourceFile));
		try {
			String line = null;

			final int SCANNING_FOR_COMMENT = 0;
			final int READING_COMMENT = 1;
			final int FINISHED_COMMENT = 2;
			int currentState = SCANNING_FOR_COMMENT;
			while ((line = reader.readLine()) != null) {
				line = line.trim();
				switch (currentState) {
					case SCANNING_FOR_COMMENT:
						if (line.startsWith("/**")) {
							comment = new StringBuffer();
							line = line.substring("/**".length());
							currentState = READING_COMMENT;
							// ATTENTION (in case someone wants to read this code): there is no break here!
							// Hence the fallthrough is ok and we add a warning suppression
						} else {
							break;
						}
					case READING_COMMENT:
						int end = line.indexOf("*/");
						if (end != -1) {
							line = line.substring(0, end);
							currentState = FINISHED_COMMENT;
						}
						line = stripAsteriks(line);
						if (line.startsWith("@"))
							continue;
						comment.append(line + " ");
						break;
					case FINISHED_COMMENT:
						if (line.length() > 0) {
							int classIndex = line.indexOf("class");
							int nameIndex = line.indexOf(clazz.getSimpleName());
							if ((classIndex != -1) && (nameIndex > classIndex + "class".length())) {
								return comment.toString().trim();
							} else {
								// this is not what we want
								comment = null;
							}
						}
						break;
				}
			}
		}
		finally {
			/* Close the stream even if we return early. */
			reader.close();
		}
		return null;
	}

	private static String stripAsteriks(String string) {
		while (string.startsWith("*"))
			string = string.substring(1).trim();
		return string.trim();
	}

}
