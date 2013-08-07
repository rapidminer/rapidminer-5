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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.io.StringReader;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.logging.Level;

import javax.swing.ImageIcon;
import javax.swing.JMenuItem;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.rapidminer.Process;
import com.rapidminer.gui.actions.Actions;
import com.rapidminer.gui.tools.ResourceMenu;
import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.io.process.XMLImporter;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.UnknownParameterInformation;
import com.rapidminer.tools.BuildingBlockService;
import com.rapidminer.tools.I18N;
import com.rapidminer.tools.LogService;
import com.rapidminer.tools.XMLException;

/**
 * This menu contains all building blocks, the predefined and the user defined.
 * 
 * @author Ingo Mierswa, Tobias Malbrecht
 */
public class NewBuildingBlockMenu extends ResourceMenu {

	private static final long serialVersionUID = 316102134905132452L;

    private final Actions actions;

	public NewBuildingBlockMenu(Actions actions) {
		super("new_building_block");
		this.actions = actions;
	}

    public void addAllMenuItems() {
        setMenuItems(BuildingBlockService.getBuildingBlocks());
    }
    
	public void setMenuItems(Collection<BuildingBlock> buildingBlocks) {
        removeAll();
		Iterator<BuildingBlock> i = buildingBlocks.iterator();
		while (i.hasNext()) {
			final BuildingBlock buildingBlock = i.next();
			JMenuItem item = null;
			final String name = buildingBlock.getName();
			ImageIcon icon = buildingBlock.getSmallIcon();
			if (icon == null) {
			    item = new JMenuItem(name);
			} else {
			    item = new JMenuItem(name, icon);
			}
			item.setToolTipText(buildingBlock.getDescription());
			item.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
			        String xmlDescription = buildingBlock.getXML();
			        try {
			            InputSource source = new InputSource(new StringReader(xmlDescription));
			            Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(source);
			            Element element = document.getDocumentElement();
                        Operator operator = Operator.createFromXML(element, actions.getProcess(), new LinkedList<UnknownParameterInformation>(), null, XMLImporter.CURRENT_VERSION);
			            actions.insert(Collections.singletonList(operator));
			        } catch (Exception ex) {
			            SwingTools.showSimpleErrorMessage("cannot_instantiate_building_block", ex, name);
			        }
			    }
			});
			// disable building block which cannot be created, e.g. cause they consist of operators
			// part of a non-loaded plugin
			item.setEnabled(checkBuildingBlock(buildingBlock));
			add(item);
		}
	}
    
    /** Returns true if the building block does not contain errors and can be properly loaded. */
    public static boolean checkBuildingBlock(BuildingBlock buildingBlock) {
        try {
            String xmlDescription = buildingBlock.getXML();
            //Process process = new Process(xmlDescription);
            InputSource source = new InputSource(new StringReader(xmlDescription));
            Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(source);
            Element element = document.getDocumentElement();
            Operator.createFromXML(element, new Process(), new LinkedList<UnknownParameterInformation>(), null, XMLImporter.CURRENT_VERSION);
            //operator.remove();
            return true;
        } catch (IOException ex) {
        	//LogService.getRoot().log(Level.WARNING, "Cannot read building block: "+ex, ex);
			LogService.getRoot().log(Level.WARNING,
					I18N.getMessage(LogService.getRoot().getResourceBundle(), 
					"com.rapidminer.gui.templates.NewBuildingBlockMenu.reading_building_block_error", 
					ex),
					ex);
            return false;
        } catch (SAXException e) {
        	//LogService.getRoot().log(Level.WARNING, "Cannot read building block: "+e, e);
			LogService.getRoot().log(Level.WARNING,
					I18N.getMessage(LogService.getRoot().getResourceBundle(), 
					"com.rapidminer.gui.templates.NewBuildingBlockMenu.reading_building_block_error", 
					e),
					e);
        	return false;
		} catch (ParserConfigurationException e) {
			//LogService.getRoot().log(Level.WARNING, "Cannot read building block: "+e, e);
			LogService.getRoot().log(Level.WARNING,
					I18N.getMessage(LogService.getRoot().getResourceBundle(), 
					"com.rapidminer.gui.templates.NewBuildingBlockMenu.reading_building_block_error", 
					e),
					e);
			return false;
		} catch (XMLException e) {
			//LogService.getRoot().log(Level.WARNING, "Cannot read building block: "+e, e);
			LogService.getRoot().log(Level.WARNING,
					I18N.getMessage(LogService.getRoot().getResourceBundle(), 
					"com.rapidminer.gui.templates.NewBuildingBlockMenu.reading_building_block_error", 
					e),
					e);
			return false;
		}
    }
}
