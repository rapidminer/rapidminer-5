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
package com.rapidminer.operator.io;

import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.AttributeRole;
import com.rapidminer.example.Attributes;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.tools.Tools;
import com.rapidminer.tools.io.Encoding;

/**
 * <p>
 * Writes values of all examples into an XRFF file which can be used by the
 * machine learning library Weka. The XRFF format is described in the
 * {@link XrffExampleSource} operator which is able to read XRFF files to make
 * them usable with RapidMiner.
 * </p>
 * 
 * <p>
 * Please note that writing attribute weights is not supported, please use the
 * other RapidMiner operators for attribute weight loading and writing for this
 * purpose.
 * </p>
 * 
 * @rapidminer.index xrff
 * @author Ingo Mierswa
 */
public class XrffExampleSetWriter extends AbstractStreamWriter {

	/** The parameter name for &quot;File to save the example set to.&quot; */
	public static final String PARAMETER_EXAMPLE_SET_FILE = "example_set_file";

	/**
	 * The parameter name for &quot;Indicates if the data file should be
	 * compressed.&quot;
	 */
	public static final String PARAMETER_COMPRESS = "compress";

	public XrffExampleSetWriter(OperatorDescription description) {
		super(description);
	}

	@Override
	void writeStream(ExampleSet exampleSet, OutputStream outputStream) throws OperatorException {
		final Charset encoding = Encoding.getEncoding(this);
		writeXrff(exampleSet, outputStream, encoding);
	}

	/*
	@Override
	public ExampleSet write(ExampleSet exampleSet) throws OperatorException {
		try {
			File xrffFile = getParameterAsFile(PARAMETER_EXAMPLE_SET_FILE, true);
			final Charset encoding = Encoding.getEncoding(this);
			final FileOutputStream outputStream = new FileOutputStream(xrffFile);
			writeXrff(exampleSet, outputStream, encoding);
		} catch (IOException e) {
			throw new UserError(this, e, 303, new Object[] {
					getParameterAsString(PARAMETER_EXAMPLE_SET_FILE),
					e.getMessage() });
		}
		return exampleSet;
	}*/

	public static void writeXrff(ExampleSet exampleSet, final OutputStream outputStream, final Charset encoding) {
		PrintWriter out = null;
		try {
			out = new PrintWriter(new OutputStreamWriter(outputStream, encoding));
			out.println("<?xml version=\"1.0\" encoding=\"" + encoding + "\"?>");
			out.println("<dataset name=\"RapidMinerData\" version=\"3.5.4\">");

			out.println("  <header>");
			out.println("    <attributes>");

			Iterator<AttributeRole> a = exampleSet.getAttributes().allAttributeRoles();
			while (a.hasNext()) {
				AttributeRole role = a.next();
				// ignore weight attribute in order to use instance weights
				// directly later
				if ((role.getSpecialName() != null) && (role.getSpecialName().equals(Attributes.WEIGHT_NAME)))
					continue;
				Attribute attribute = role.getAttribute();
				boolean label = (role.getSpecialName() != null) && (role.getSpecialName().equals(Attributes.LABEL_NAME));
				printAttribute(attribute, out, label);
			}
			out.println("    </attributes>");
			out.println("  </header>");

			out.println("  <body>");
			out.println("    <instances>");

			Attribute weightAttribute = exampleSet.getAttributes().getWeight();
			for (Example example : exampleSet) {
				String weightString = "";
				if (weightAttribute != null) {
					weightString = " weight=\"" + example.getValue(weightAttribute) + "\"";
				}
				out.println("      <instance" + weightString + ">");
				a = exampleSet.getAttributes().allAttributeRoles();
				while (a.hasNext()) {
					AttributeRole role = a.next();
					// ignore weight attribute in order to use instance weights
					// directly later
					if ((role.getSpecialName() != null) && (role.getSpecialName().equals(Attributes.WEIGHT_NAME)))
						continue;
					Attribute attribute = role.getAttribute();
					out.println("        <value>" + Tools.escapeXML(example.getValueAsString(attribute)) + "</value>");
				}
				out.println("      </instance>");
			}

			out.println("    </instances>");
			out.println("  </body>");
			out.println("</dataset>");
		} finally {
			if (out != null) {
				out.close();
			}
		}
	}

	private static void printAttribute(Attribute attribute, PrintWriter out, boolean isClass) {
		String classString = isClass ? "class=\"yes\" " : "";
		if (attribute.isNominal()) {
			out.println("      <attribute name=\"" + Tools.escapeXML(attribute.getName()) + "\" " + classString + "type=\"nominal\">");
			out.println("        <labels>");
			for (String s : attribute.getMapping().getValues()) {
				out.println("          <label>" + Tools.escapeXML(s) + "</label>");
			}
			out.println("        </labels>");
			out.println("      </attribute>");
		} else {
			out.println("      <attribute name=\"" + Tools.escapeXML(attribute.getName()) + "\" " + classString + "type=\"numeric\"/>");
		}
	}

	@Override
	protected boolean supportsEncoding() {
		return true;
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = new LinkedList<ParameterType>();
		types.add(makeFileParameterType());
		//types.add(new ParameterTypeFile(PARAMETER_EXAMPLE_SET_FILE,
		//		"File to save the example set to.", "xrff", false));
		// types.add(new ParameterTypeBoolean(PARAMETER_COMPRESS,
		// "Indicates if the data file should be compressed.", false));
		types.addAll(super.getParameterTypes());
		return types;
	}

	@Override
	String[] getFileExtensions() {
		return new String[] { "xrff" };
	}

	@Override
	String getFileParameterName() {
		return PARAMETER_EXAMPLE_SET_FILE;
	}

}
