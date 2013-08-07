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
package com.rapidminer.gui.templates;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.rapidminer.Process;
import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.io.process.XMLImporter;
import com.rapidminer.io.process.XMLTools;
import com.rapidminer.tools.FileSystemService;
import com.rapidminer.tools.I18N;
import com.rapidminer.tools.LogService;
import com.rapidminer.tools.Tools;
import com.rapidminer.tools.XMLException;

/**
 * A template process consisting of name, short description, a name for an
 * process file and a list parameters given as String pairs (operator, key).
 * Templates must look like this:
 * 
 * <pre>
 *   one line for the name
 *   one line of html description
 *   one line for the process file name
 *   Rest of the file: some important parameters in the form operatorname.parametername
 * </pre>
 * 
 * @author Ingo Mierswa, Simon Fischer
 */
public class Template {

    public static final int PREDEFINED = 0;

    public static final int USER_DEFINED = 1;

    public static final int ALL = 2;

    private String name = "unnamed";

    private String description = "none";

    private String processResourceName;

    private Set<OperatorParameterPair> parameters = new TreeSet<OperatorParameterPair>();

    private File templateFile = null;

    /** Indicates whether the template is read from a file (and hence can be deleted) or from a resource stream. */
    private boolean readFromFile = false;

    /** Indicates whether templates was read from the old format where template description and process come in two separate files. */
    private boolean oldFormat;

    private String group = "General";

    private String templateDefinition;

    public Template() {}


    public Template(File file) throws IOException, SAXException {
        this(new FileInputStream(file));
        readFromFile = true;
        this.templateFile = file;

        // uncomment to convert old format files to new format
        //		try {
        //			if (oldFormat) {
        //				saveAsUserTemplate(getProcess());
        //			}
        //		} catch (XMLException e) {
        //			throw new IOException(e);
        //		}
    }

    public Template(InputStream ins) throws IOException, SAXException {
        if (!ins.markSupported()) {
            ins = new BufferedInputStream(ins);
        }
        ins.mark(7);
        String first = new String(new char[] {
                (char)ins.read(),
                (char)ins.read(),
                (char)ins.read(),
                (char)ins.read(),
                (char)ins.read()
        });
        ins.reset();
        if ("<?xml".equals(first)) {
            parseNewFormat(ins);
        } else {
            parseOldFormat(ins);
        }
    }

    private void parseNewFormat(InputStream ins) throws IOException, SAXException {
        this.oldFormat = false;
        templateDefinition = Tools.readTextFile(ins);
        Document doc = XMLTools.parse(new ByteArrayInputStream(templateDefinition.getBytes(XMLImporter.PROCESS_FILE_CHARSET)));
        this.name = XMLTools.getTagContents(doc.getDocumentElement(), "title");
        this.description = XMLTools.getTagContents(doc.getDocumentElement(), "description");
        this.group = XMLTools.getTagContents(doc.getDocumentElement(), "template-group");
        NodeList freeParameterElements = doc.getDocumentElement().getElementsByTagName("template-parameter");
        for (int i = 0; i < freeParameterElements.getLength(); i++) {
            Element freeParameterElement = (Element) freeParameterElements.item(i);
            String operator = XMLTools.getTagContents(freeParameterElement, "operator");
            String parameterKey= XMLTools.getTagContents(freeParameterElement, "parameter");
            parameters.add(new OperatorParameterPair(operator, parameterKey));
        }
    }

    private void parseOldFormat(InputStream ins) throws IOException {
        this.oldFormat = true;
        BufferedReader in = null;
        try {
            in = new BufferedReader(new InputStreamReader(ins, "UTF-8"));
            name = in.readLine();
            description = in.readLine();
            processResourceName = in.readLine();
            String line = null;
            while ((line = in.readLine()) != null) {
                String[] split = line.split("\\.");
                if (split.length == 2) {
                    parameters.add(new OperatorParameterPair(split[0], split[1]));
                } else {
                    throw new IOException("Malformed operator parameter pair: "+line);
                }
            }
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    //LogService.getRoot().log(Level.WARNING, "Cannot close stream to template file: " + e.getMessage(), e);
        			LogService.getRoot().log(Level.WARNING,
        					I18N.getMessage(LogService.getRoot().getResourceBundle(), 
        					"com.rapidminer.gui.templates.Template.closing_stream_error", 
        					e.getMessage()),
        					e);

                }
            }
        }
    }

    public Template(String name, String group, String description, String configFile, Set<OperatorParameterPair> parameters) {
        this.name = name;
        this.group = group;
        this.description = description;
        this.processResourceName = configFile;
        this.parameters = parameters;
    }

    private InputStream getProcessStream() throws IOException {
        if (readFromFile) {
            return new FileInputStream(getProcessFile());
        } else {
            String resource = "/com/rapidminer/resources/templates/"+getProcessResource();
            InputStream resourceAsStream = Template.class.getResourceAsStream(resource);
            if (resourceAsStream == null) {
                throw new IOException("Resource "+resource+" not found.");
            } else {
                return resourceAsStream;
            }
        }
    }

    private File getProcessFile() {
        return new File(templateFile.getParent(), getProcessResource());
    }

    private String getProcessResource() {
        return processResourceName;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getGroup() {
        return group;
    }

    public Collection<OperatorParameterPair> getParameters() {
        return parameters;
    }

    public void saveAsUserTemplate(Process process) throws IOException, XMLException {
        String name = getName();
        if (!Tools.canFileBeStoredOnCurrentFilesystem(name)) {
			SwingTools.showVerySimpleErrorMessage("name_contains_illegal_chars", name);
			return;
		}
        File outputFile = FileSystemService.getUserConfigFile(name + ".template");

        Document doc = process.getRootOperator().getDOMRepresentation();
        XMLTools.setTagContents(doc.getDocumentElement(), "title", getName());
        XMLTools.setTagContents(doc.getDocumentElement(), "description", getDescription());
        XMLTools.setTagContents(doc.getDocumentElement(), "template-group", getGroup());
        Element opps = doc.createElement("template-parameters");
        doc.getDocumentElement().appendChild(opps);
        for (OperatorParameterPair opp : parameters) {
            Element oppElement = doc.createElement("template-parameter");
            opps.appendChild(oppElement);
            XMLTools.setTagContents(oppElement, "operator", opp.getOperator());
            XMLTools.setTagContents(oppElement, "parameter", opp.getParameter());
        }
        XMLTools.stream(doc, outputFile, XMLImporter.PROCESS_FILE_CHARSET);

        //		PrintWriter out = null;
        //		try {
        //			out = new PrintWriter(new FileWriter(outputFile));
        //			out.println(name);
        //			out.println(description);
        //			out.println(processResourceName);
        //			Iterator<OperatorParameterPair> i = parameters.iterator();
        //			while (i.hasNext()) {
        //				OperatorParameterPair pair = i.next();
        //				out.println(pair.toString());
        //			}
        //
        //			File templateXmlFile = ParameterService.getUserConfigFile(name + ".xml");
        //			process.save(templateXmlFile);
        //		} catch (IOException e) {
        //			throw e;
        //		} finally {
        //			if (out != null) {
        //				out.close();
        //			}
        //		}
    }


    public void delete() {
        File expFile = getProcessFile();
        boolean deleteResult = templateFile.delete();
        if (!deleteResult)
            //LogService.getGlobal().logWarning("Unable to delete template file: " + templateFile);
        	LogService.getRoot().log(Level.WARNING, "com.rapidminer.gui.templates.Template.deleting_template_file_error", templateFile);
        deleteResult = expFile.delete();
        if (!deleteResult)
            //LogService.getGlobal().logWarning("Unable to delete template experiment file: " + expFile);
        	LogService.getRoot().log(Level.WARNING, "com.rapidminer.gui.templates.Template.deleting_template_experiment_file_error", expFile);
    }


    public Process getProcess() throws IOException, XMLException {
        Process process;
        if (oldFormat) {
            final InputStream in = getProcessStream();
            process = new Process(in);
        } else {
            process = new Process(templateDefinition);
        }
        final String desc = process.getRootOperator().getUserDescription();
        if ((desc == null) || desc.isEmpty()) {
            process.getRootOperator().setUserDescription(getDescription());
        }
        return process;
    }


    public String getHTMLDescription() {
        return "<html><strong>" + getName() +"</strong>"+(readFromFile ? " <small>(user defined)</small>" : "")+"<div width=\"600\">" + getDescription() + "</div></html>";
    }
}
