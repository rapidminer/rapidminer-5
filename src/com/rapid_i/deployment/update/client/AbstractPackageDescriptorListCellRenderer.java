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
package com.rapid_i.deployment.update.client;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.ListCellRenderer;

import com.rapidminer.deployment.client.wsimport.PackageDescriptor;


/**
 * @author Nils Woehler
 *
 */
public abstract class AbstractPackageDescriptorListCellRenderer implements ListCellRenderer {

	private static RenderingHints HI_QUALITY_HINTS = new RenderingHints(null);
	
	static {
		HI_QUALITY_HINTS.put(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		HI_QUALITY_HINTS.put(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		HI_QUALITY_HINTS.put(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
	}
	
	private Map<String, Icon> icons = new HashMap<String, Icon>();
	
	protected Icon getIcon(PackageDescriptor pd) {
		if (pd.getIcon() == null) {
			return null;
		} else {
			Icon result = icons.get(pd.getPackageId());
			if (result == null) {
				result = new ImageIcon(pd.getIcon());
				icons.put(pd.getPackageId(), result);
			}
			return result;
		}
	}

	protected Icon getResizedIcon(Icon originalIcon) {
		if (originalIcon == null)
			return null;
		int width = originalIcon.getIconWidth();
		int height = originalIcon.getIconHeight();
		if (width != 48) {
			double scale = (48d / width);
			BufferedImage bi = new BufferedImage(
					(int) (scale * width),
					(int) (scale * height),
					BufferedImage.TYPE_INT_ARGB);
			Graphics2D g = bi.createGraphics();
			g.setRenderingHints(HI_QUALITY_HINTS);
			g.scale(scale, scale);
			originalIcon.paintIcon(null, g, 0, 0);
			g.dispose();
			return new ImageIcon(bi);
		} else {
			return originalIcon;
		}
	}
	
}
