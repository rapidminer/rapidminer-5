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
package com.rapidminer.tools;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;

import com.rapidminer.gui.templates.BuildingBlock;
import com.rapidminer.tools.plugin.Plugin;


/**
 * This service class can be used to deliver all building blocks, i.e. both predefined
 * and user defined building blocks.
 * 
 *  @author Ingo Mierswa, Tobias Malbrecht
 */
public class BuildingBlockService {

    private static final String RESOURCE_BUILDINGBLOCK_FOLDER = "/com/rapidminer/resources/buildingblocks/";
    private static final String RESOURCE_BUILDINGBLOCK_LIST = RESOURCE_BUILDINGBLOCK_FOLDER + "buildingblocks.txt";

    /** Returns a sorted list of all building blocks. */
    public static List<BuildingBlock> getBuildingBlocks() {
        List<BuildingBlock> buildingBlocks = getPredefinedBuildingBlocks();
        buildingBlocks.addAll(getPluginBuildingBlocks());
        buildingBlocks.addAll(getUserBuildingBlocks());
        Collections.sort(buildingBlocks);
        return buildingBlocks;
    }

    /** Returns all user defined building blocks. The result is not sorted. */
    public static List<BuildingBlock> getUserBuildingBlocks() {
        File[] userDefinedBuildingBlockFiles = FileSystemService.getUserRapidMinerDir().listFiles(new FileFilter() {

            @Override
            public boolean accept(File file) {
                return file.getName().endsWith(".buildingblock");
            }
        });

        List<BuildingBlock> buildingBlocks = new LinkedList<BuildingBlock>();
        for (File file : userDefinedBuildingBlockFiles) {
            try {
                buildingBlocks.add(new BuildingBlock(file, BuildingBlock.USER_DEFINED));
            } catch (InstantiationException e) {
                //LogService.getRoot().log(Level.WARNING, "Cannot load building block file '" + file + "': " + e.getMessage(), e);
                LogService.getRoot().log(Level.WARNING,
    					I18N.getMessage(LogService.getRoot().getResourceBundle(), 
    					"com.rapidminer.tools.BuildingBlockService.loading_building_block_file_file_error", 
    					file, e.getMessage()),
    					e);            
            }
        }
        return buildingBlocks;
    }

    /** Returns all predefined building blocks. The result is not sorted. */
    public static List<BuildingBlock> getPredefinedBuildingBlocks() {
        InputStream bbListIn = BuildingBlockService.class.getResourceAsStream(RESOURCE_BUILDINGBLOCK_LIST);
        if (bbListIn == null) {
            //LogService.getRoot().warning("Resource "+RESOURCE_BUILDINGBLOCK_LIST+" missing");
        	LogService.getRoot().log(Level.WARNING, "com.rapidminer.tools.BuildingBlockService.ressource_missing", RESOURCE_BUILDINGBLOCK_LIST);
            return Collections.emptyList();
        }
        String[] files = null;
        try {
            files = Tools.readTextFile(new InputStreamReader(bbListIn, "UTF-8")).split("[\\r\\n]+");
        } catch (Exception e1) {
            //LogService.getRoot().log(Level.WARNING, "Cannot read resource "+RESOURCE_BUILDINGBLOCK_LIST+": "+e1 ,e1);
            LogService.getRoot().log(Level.WARNING,
					I18N.getMessage(LogService.getRoot().getResourceBundle(), 
					"com.rapidminer.tools.BuildingBlockService.reading_resource_error", 
					RESOURCE_BUILDINGBLOCK_LIST, e1),
					e1);  
            return Collections.emptyList();
        }
        List<BuildingBlock> buildingBlocks = new LinkedList<BuildingBlock>();
        for (String resourceName : files) {
            if ((resourceName == null) || resourceName.isEmpty()) {
                continue;
            }
            try {
                InputStream in = BuildingBlockService.class.getResourceAsStream(RESOURCE_BUILDINGBLOCK_FOLDER + resourceName);
                buildingBlocks.add(new BuildingBlock(new BufferedReader(new InputStreamReader(in, "UTF-8")), BuildingBlock.PREDEFINED));
            } catch (Exception e) {
                //LogService.getRoot().log(Level.WARNING, "Cannot load building block file '" + resourceName+ "': " + e.getMessage(), e);
                LogService.getRoot().log(Level.WARNING,
    					I18N.getMessage(LogService.getRoot().getResourceBundle(), 
    					"com.rapidminer.tools.BuildingBlockService.loading_building_block_file_resourceName_error", 
    					resourceName, e.getMessage()),
    					e);  
            }
        }
        return buildingBlocks;
    }

    /** Returns all building blocks defined by plugins. */
    public static List<BuildingBlock> getPluginBuildingBlocks() {
        List<BuildingBlock> buildingBlocks = new LinkedList<BuildingBlock>();
        Iterator<Plugin> p = Plugin.getAllPlugins().iterator();
        while (p.hasNext()) {
            buildingBlocks.addAll(p.next().getBuildingBlocks());
        }
        return buildingBlocks;
    }
}
