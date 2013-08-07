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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;

import com.rapidminer.tools.Tools;

/**
 * Removes the version tags from all classes.
 * 
 * @author Ingo Mierswa
 */
public class RemoveClassVersionTags {
	
	private char[] readFile(File file) throws IOException {
		StringBuffer contents = new StringBuffer((int) file.length());

		BufferedReader in = new BufferedReader(new FileReader(file));
		try {
			String line = null;
			while ((line = in.readLine()) != null) {
				if ((line.indexOf("@version") < 0) && (line.indexOf("$Id") < 0)) {
					contents.append(line);
					contents.append(Tools.getLineSeparator());
				}
			}
		}
		finally {
			/* Close the stream even if we return early. */
			in.close();
		}
		return contents.toString().toCharArray();
	}

	private void removeVersionTag(File file) throws IOException {
		System.out.print(file + "...");

		char[] fileContents = readFile(file);
		if (fileContents == null)
			return;

		Writer out = new FileWriter(file);
		out.write(fileContents);
		out.close();
		System.out.println("ok");
	}

	private void recurse(File file) {
		if (file.isDirectory()) {
			File[] files = file.listFiles();
			for (int i = 0; i < files.length; i++) {
				recurse(files[i]);
			}
		} else {
			if (file.getName().endsWith(".java")) {
				try {
					removeVersionTag(file);
				} catch (IOException e) {
					System.err.println("failed: " + e.getClass().getName() + ": " + e.getMessage());
				}
			}
		}
	}

	public static void main(String[] argv) throws Exception {
		RemoveClassVersionTags remover = new RemoveClassVersionTags();

		if ((argv.length != 1) || ((argv.length == 1) && argv[0].equals("-help"))) {
			System.out.println("Usage: java " + remover.getClass().getName() + " directory");
			System.exit(1);
		}

		remover.recurse(new File(argv[0]));
	}
}

