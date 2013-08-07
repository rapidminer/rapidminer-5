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
package com.rapidminer.gui.properties;

import java.awt.Dimension;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.TreeMap;

import javax.swing.ScrollPaneConstants;

import com.rapidminer.gui.tools.ExtendedJScrollPane;
import com.rapidminer.gui.tools.ExtendedJTabbedPane;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.tools.ParameterService;


/**
 * The tabs for the different groups of RapidMiner settings. Each tab contains a
 * {@link SettingsPropertyPanel} for the settings in this group.
 * 
 * @author Sebastian Land, Ingo Mierswa
 */
public class SettingsTabs extends ExtendedJTabbedPane {

    private static final long serialVersionUID = -229446448782516589L;

    private final List<SettingsPropertyPanel> parameterPanels = new LinkedList<SettingsPropertyPanel>();

    public SettingsTabs() {
        this(null);
    }

    public SettingsTabs(String initialSelectedTab) {
        Collection<String> definedParameterKeys = ParameterService.getDefinedParameterKeys();
        SortedMap<String, List<ParameterType>> groups = new TreeMap<String, List<ParameterType>>();

        for (String key: definedParameterKeys) {
            String group = ParameterService.getGroupKey(key);

            List<ParameterType> groupTypeList = groups.get(group);
            if (groupTypeList == null) {
                groupTypeList = new LinkedList<ParameterType>();
                groups.put(group, groupTypeList);
            }
            groupTypeList.add(ParameterService.getParameterType(key));
        }

        for (Entry<String, List<ParameterType>> entry: groups.entrySet()) {
            List<ParameterType> lists = entry.getValue();
            Collections.sort(lists, new Comparator<ParameterType>() {
                @Override
                public int compare(ParameterType o1, ParameterType o2) {
                    return o1.getKey().compareTo(o2.getKey());
                }
            });
            SettingsPropertyPanel table = new SettingsPropertyPanel(lists);
            parameterPanels.add(table);

            ExtendedJScrollPane scrollPane = new ExtendedJScrollPane(table);
            scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
            scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
            scrollPane.setPreferredSize(new Dimension(600, 300));

            String group = entry.getKey();
            String name = new String(new char[] { group.charAt(0) }).toUpperCase() + group.substring(1, group.length());
            addTab(name, scrollPane);
        }
    }

    public void applyProperties() {
        for (SettingsPropertyPanel panel: parameterPanels) {
            panel.applyProperties();
        }
    }

    /**
     * This method will save the parameters defined in this tab
     */
    public void save() throws IOException {
        applyProperties();
        ParameterService.saveParameters();
    }
}
