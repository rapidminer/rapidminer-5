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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.logging.Level;

import javax.swing.ImageIcon;

import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.tools.I18N;
import com.rapidminer.tools.LogService;


/**
 * A building block consisting of a name, a short description, and the XML description 
 * for the building block. Templates must look like this:
 * 
 * <pre>
 *   one line for the name
 *   one line of html description
 *   one line for the XML description
 * </pre>
 * 
 * @author Ingo Mierswa, Tobias Malbrecht
 */
public class BuildingBlock implements Comparable<BuildingBlock> {
	
	public static final int ALL = 0;
	
	public static final int PREDEFINED = 1;
	
	public static final int USER_DEFINED = 2;
	
	public static final int PLUGIN_DEFINED = 3;
	
	private String name = "unnamed";

	private String description = "none";

	private String xmlDescription;

	private File buildingBlockFile;
	
	private String iconName;
	
	private int source = PREDEFINED; 
	
	@Deprecated
	public BuildingBlock(File file) throws InstantiationException {
		this(file, PREDEFINED);
	}

	public BuildingBlock(File file, int source) throws InstantiationException {
		this.source = source;
		BufferedReader in = null;
		try {
			in = new BufferedReader(new FileReader(file));
			init(in);
			this.buildingBlockFile = file;
		} catch (IOException e) {
			//LogService.getRoot().log(Level.WARNING, "Cannot read building block file: " + e.getMessage(), e);
			LogService.getRoot().log(Level.WARNING,
					I18N.getMessage(LogService.getRoot().getResourceBundle(), 
					"com.rapidminer.gui.templatesBuildingBlock.reading_building_block_file_error", 
					e.getMessage()),
					e);
			throw new InstantiationException("Cannot instantiate building block: " + e.getMessage());
		} finally {
			if (in != null) {
				try {
					in.close();
				} catch (IOException e) {
					//LogService.getRoot().log(Level.WARNING, "Cannot close stream to building block file: " + e.getMessage(), e);
					LogService.getRoot().log(Level.WARNING,
							I18N.getMessage(LogService.getRoot().getResourceBundle(), 
							"com.rapidminer.gui.templatesBuildingBlock.closing_building_block_file_error", 
							e.getMessage()),
							e);
				}
			}
		}
	}

	@Deprecated
	public BuildingBlock(BufferedReader in) throws IOException {
		this(in, PREDEFINED);
	}
	
	public BuildingBlock(BufferedReader in, int source) throws IOException {
		this.source = source;
		init(in);
	}
	
	@Deprecated
	public BuildingBlock(String name, String description, String iconPath, String xmlDescription) {
		this(name, description, iconPath, xmlDescription, PREDEFINED);
	}
	
	public BuildingBlock(String name, String description, String iconName, String xmlDescription, int source) {
		this.name = name;
		this.description = description;
		this.iconName = iconName;
		this.xmlDescription = xmlDescription;
		this.source = source;
	}

	private void init(BufferedReader in) throws IOException {
		this.name = in.readLine();
		this.description = in.readLine();
		this.iconName = in.readLine();
		// rest is XML
		String line = null;
		StringBuffer result = new StringBuffer();
		while ((line = in.readLine()) != null) {
			result.append(line);
		}
		this.xmlDescription = result.toString();		
	}
	
	public File getFile() {
		return buildingBlockFile;
	}

	public String getName() {
		return name;
	}

	public String getDescription() {
		return description;
	}

	public String getIconName() {
		return iconName;
	}

	public ImageIcon getSmallIcon() {
		return SwingTools.createIcon("16/" + getIconName());
	}

	public ImageIcon getLargeIcon() {
		return SwingTools.createIcon("24/" + getIconName());
	}
	
	public String getXML() {
		return xmlDescription;
	}
	
	public int getSource() {
		return source;
	}
	
	public boolean isUserDefined() {
		return (source == USER_DEFINED);
	}
	
	public boolean isPredefined() {
		return (source == PREDEFINED);
	}
	
	public void save(File file) throws IOException {
		PrintWriter out = null;
		try {
			out = new PrintWriter(new FileWriter(file));
			out.println(name);
			out.println(description);
			out.println(iconName);
			out.println(xmlDescription);
		} catch (IOException e) {
			throw e;
		} finally {
			if (out != null) {
				out.close();		
			}
		}
	}
	
	@Override
	public String toString() {
		return name;
	}
	
	public int compareTo(BuildingBlock buildingBlock) {
		return name.compareTo(buildingBlock.name);
	}
	
	@Override
	public boolean equals(Object o) {
		if (!(o instanceof BuildingBlock)) {
			return false;
		} else {
			return this.name.equals(((BuildingBlock)o).name);
		}
	}
	
	@Override
	public int hashCode() {
		return this.name.hashCode();
	}
}
