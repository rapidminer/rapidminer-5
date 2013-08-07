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

import java.util.Properties;

import com.rapidminer.tools.ParameterService;

/**
 * This change listener listens for settings changes, i.e. for changes
 * of the global program settings.
 * 
 * This change listener has been replaced by the ParameterChangeListener
 * @author Ingo Mierswa, Sebastian Land
 */
@Deprecated
public interface SettingsChangeListener {

    /** This method will be called after a settings change.
     * 
     * Attention! Please take into account that this method signature only remains
     * for compatibility reasons. Please don't use the given properties but the
     * {@link ParameterService} instead to see which settings have been changed.
     * 
     * The given properties (the system properties) will reflect the changes, too, but
     * only for ensuring compatibility and this might be removed soon.
     * */
    public void settingsChanged(Properties properties);

}
