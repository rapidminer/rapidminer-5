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

import java.util.Map;

import com.sun.javadoc.Tag;
import com.sun.tools.doclets.Taglet;

/**
 * A taglet with name &quot;@rapidminer.ref&quot; can be used in the Javadoc comments of an operator to produce textual
 * references. Example: &quot;@rapidminer.ref figure1|A figure for this&quot;. This will include a LaTeX reference to your
 * documentation.
 * 
 * @author Simon Fischer, Ingo Mierswa
 */
public class RefTaglet implements TexTaglet {

	private static final String NAME = "rapidminer.ref";

	public String getName() {
		return NAME;
	}

	public boolean inField() {
		return true;
	}

	public boolean inConstructor() {
		return true;
	}

	public boolean inMethod() {
		return true;
	}

	public boolean inOverview() {
		return true;
	}

	public boolean inPackage() {
		return true;
	}

	public boolean inType() {
		return true;
	}

	public boolean isInlineTag() {
		return true;
	}

	public static void register(Map<String, Taglet> tagletMap) {
		RefTaglet tag = new RefTaglet();
		Taglet t = tagletMap.get(tag.getName());
		if (t != null) {
			tagletMap.remove(tag.getName());
		}
		tagletMap.put(tag.getName(), tag);
	}

	private String[] split(Tag tag) {
		String[] splitted = tag.text().split("\\|");
		if (splitted.length != 2) {
			System.err.println("Usage: {@" + getName() + " latexref|html_human_readable_ref} (" + tag.position() + ")");
			return new String[] { tag.text(), tag.text() };
		} else {
			return splitted;
		}
	}

	public String toString(Tag tag) {
		return split(tag)[1];
	}

	public String toString(Tag[] tags) {
		return null;
	}

	public String toTex(Tag tag) {
		return "\\ref{" + split(tag)[0] + "}";
	}

	public String toTex(Tag[] tag) {
		return null;
	}
}
