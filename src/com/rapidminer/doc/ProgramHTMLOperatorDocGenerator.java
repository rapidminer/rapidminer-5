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

import java.io.PrintWriter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.rapidminer.operator.Operator;
import com.rapidminer.tools.LogService;
import com.sun.javadoc.ClassDoc;
import com.sun.javadoc.RootDoc;

/**
 * Formats operator documentation in HTML style.
 *
 * @author Ingo Mierswa
 */
public class ProgramHTMLOperatorDocGenerator implements OperatorDocGenerator {

	public void generateDoc(Operator op, RootDoc rootDoc, PrintWriter out) {
		ClassDoc opDoc = rootDoc.classNamed(op.getClass().getName());

		// name
		out.println(op.getOperatorDescription().getName());

		// Description
		out.println(transformHTMLJavadocComment(opDoc.commentText(), op.getClass(), op.getOperatorDescription().getName()));

		out.println("#####");
	}

	public String transformHTMLJavadocComment(String comment, final Class clazz, final String operatorName) {
		try {
			// Links
			StringBuffer linksReplaced = new StringBuffer();
			Pattern linkPattern = Pattern.compile("\\{@link (.*?)\\}");
			Matcher linkMatcher = linkPattern.matcher(comment);
			while (linkMatcher.find()) {
				String classname = linkMatcher.group(1);
				int period = classname.lastIndexOf(".");
				if (period != -1)
					classname = classname.substring(period + 1);
				linkMatcher.appendReplacement(linksReplaced, "<i>" + classname + "</i>");
			}
			linkMatcher.appendTail(linksReplaced);
			
			// RapidMiner Ref
			StringBuffer refReplaced = new StringBuffer();
			Pattern refPattern = Pattern.compile("\\{@rapidminer.ref (.*?)\\}");
			Matcher refMatcher = refPattern.matcher(linksReplaced.toString());
			while (refMatcher.find()) {
				String refName = refMatcher.group(1);
				int period = refName.lastIndexOf("|");
				if (period != -1)
					refName = refName.substring(period + 1);
				refMatcher.appendReplacement(refReplaced, "<i>" + refName + "</i>");
			}
			refMatcher.appendTail(refReplaced);
			
			// RapidMiner Math
			StringBuffer mathReplaced = new StringBuffer();
			Pattern mathPattern = Pattern.compile("\\{@rapidminer.math (.*?)\\}");
			Matcher mathMatcher = mathPattern.matcher(refReplaced.toString());
			while (mathMatcher.find()) {
				String mathName = mathMatcher.group(1);
				mathMatcher.appendReplacement(mathReplaced, "<i>" + mathName + "</i>");
			}
			mathMatcher.appendTail(mathReplaced);
			
			return mathReplaced.toString();
		} catch (Throwable e) {
			LogService.getGlobal().log(operatorName + " (" + clazz.getName() + "): " + e, LogService.ERROR);
			return "Cannot parse class comment: " + e;
		}
	}

	/** Does nothing. */
	public void beginGroup(String groupName, PrintWriter out) {}

	/** Does nothing. */
	public void endGroup(String groupName, PrintWriter out) {}
}
