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

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.util.HashMap;
import java.util.HashSet;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import com.rapidminer.deployment.client.wsimport.PackageDescriptor;
import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.tools.I18N;

/**
 * @author Venkatesh Umaashankar
 *
 */
public class PackageListCellRenderer extends AbstractPackageDescriptorListCellRenderer {

	private int textPixelSize = 0;
	private HashMap<PackageDescriptor, HashSet<PackageDescriptor>> dependecyMap = null;

	public PackageListCellRenderer(HashMap<PackageDescriptor, HashSet<PackageDescriptor>> dependecyMap) {
		this.dependecyMap = dependecyMap;
	}

	@Override
	public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {

		JPanel panel = new JPanel() {

			private static final long serialVersionUID = 1L;

			@Override
			/*
			 * Overriding this method causes the correct computation
			 * of the width with no overlapping if the scrollbar
			 * is displayed.
			 */
			public Dimension getPreferredSize() {
				Dimension d = super.getPreferredSize();
				if (d == null) {
					return d;
				}
				d.width = 10;
				return d;
			}
		};

		JLabel label = new JLabel();

		panel.setLayout(new FlowLayout(FlowLayout.LEFT));

		panel.add(label);

		panel.setOpaque(true);

		if (isSelected && (value instanceof PackageDescriptor)) {
			panel.setBackground(SwingTools.DARKEST_BLUE);
			panel.setBorder(BorderFactory.createLineBorder(Color.black));
		} else {
			if (index % 2 == 0) {
				panel.setBackground(Color.WHITE);
				panel.setBorder(BorderFactory.createLineBorder(Color.WHITE));

			} else {
				panel.setBackground(SwingTools.LIGHTEST_BLUE);
				panel.setBorder(BorderFactory.createLineBorder(SwingTools.LIGHTEST_BLUE));
			}
		}

		String text = "";
		if (value instanceof PackageDescriptor) {
			PackageDescriptor desc = (PackageDescriptor) value;

			Icon packageIcon = getResizedIcon(getIcon(desc));

			text = "<html><body style='width: " + (packageIcon != null ? (300 - packageIcon.getIconWidth()) : 314) + ((textPixelSize > 0) ? ";font-size:" : "") + textPixelSize + "px;" +
					(packageIcon == null ? "margin-left:40px;" : "") + "'>";

			// add name and version
			text += "<div><strong>" + desc.getName() + "</strong> " + desc.getVersion();

			if (desc.isRestricted()) {
				text += "&nbsp;&nbsp;<img src='icon:///16/currency_euro.png' style='vertical-align:middle;'/>";
			}

			text += "</div>";
			text += "<div style='margin-top:5px;'>" + getLicenseType(desc.getLicenseName()) + "</div>";

			if (dependecyMap != null && dependecyMap.get(desc).size() > 0) {
				text += "<div style='margin-top:5px;'>" + getSourcePackages(desc) + "</div>";
			}

			text += "</body></html>";

			label.setIcon(packageIcon);
			label.setVerticalTextPosition(SwingConstants.TOP);
			label.setForeground(Color.BLACK);
			label.setText(text);

		}

		return panel;
	}

	private String getSourcePackages(PackageDescriptor desc) {
		StringBuffer text = new StringBuffer("");
		boolean first = true;
		for (PackageDescriptor dep : dependecyMap.get(desc)) {
			if (!first) {
				text.append(", ");
			} else {
				first = false;
			}
			text.append(dep.getName());
		}
		return I18N.getMessage(I18N.getGUIBundle(), "gui.label.required_by", text.toString());
	}

	private String getLicenseType(String licenseName) {
		return I18N.getMessage(I18N.getGUIBundle(), "gui.label.license_type", licenseName);
	}

}
