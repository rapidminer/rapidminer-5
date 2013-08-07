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
package com.rapidminer.gui;

import com.rapidminer.ObjectVisualizer;
import com.rapidminer.gui.tools.SwingTools;


/**
 * A dummy visualizer, capable of visualizing anything, but actually doing
 * nothing.
 * 
 * @author Michael Wurst, Ingo Mierswa
 */
public class DummyObjectVisualizer implements ObjectVisualizer {

	public void startVisualization(Object objId) {
		SwingTools.showVerySimpleErrorMessage("no_visual_for_obj", objId);
	}

	public void stopVisualization(Object objId) {}

	public String getTitle(Object objId) {
		return objId.toString();
	}

	public boolean isCapableToVisualize(Object id) {
		return true;
	}

	public String getDetailData(Object id, String fieldName) { 
		return null;
	}
	
	public String[] getFieldNames(Object id) {
		return new String[0];
	}
}
