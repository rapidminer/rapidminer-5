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

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import com.rapidminer.RapidMiner;
import com.rapidminer.deployment.client.wsimport.PackageDescriptor;
import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.tools.I18N;

/**
 * Renders a cell of the update list. This contains icons for the type of extension or update.
 * 
 * @author Simon Fischer
 */
final class UpdateListCellRenderer extends AbstractPackageDescriptorListCellRenderer {

	private final UpdatePackagesModel updateModel;

	private static String MARKED_FOR_INSTALL_COLOR = "#0066CC";
	private static String MARKED_FOR_UPDATE__COLOR = "#3399FF";
	private static String NOT_INSTALLED_COLOR = "#666666";
	private static String UP_TO_DATE_COLOR = "#006600";
	private static String UPDATES_AVAILABLE_COLOR = "#CC9900";

	public UpdateListCellRenderer(UpdatePackagesModel updateModel) {
		this.updateModel = updateModel;
	}

	public UpdateListCellRenderer(boolean allPurchased) {
		this.updateModel = null;
	}

	private String getFirstSentence(String text) {
		if (text != null && text.contains(".")) {
			String[] sentences = text.split("\\.");
			return sentences[0].trim() + ".";
		} else {
			return text;
		}
	}

	@Override
	public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
		JPanel panel = new JPanel() {

			private static final long serialVersionUID = 6409307403021306689L;

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
			boolean selectedForInstallation = updateModel != null ? updateModel.isSelectedForInstallation(desc) : true;
			Icon packageIcon = getResizedIcon(getIcon(desc));

			text = "<html><body style='width: " + (packageIcon != null ? (300 - packageIcon.getIconWidth()) : 314) + ";" +
					(packageIcon == null ? "margin-left:40px;" : "") + "'>";

			// add name and version
			text += "<div><strong>" + desc.getName() + "</strong> " + desc.getVersion();

			if (desc.isRestricted()) {
				text += "&nbsp;&nbsp;<img src='icon:///16/currency_euro.png' style='vertical-align:middle;'/>";
			}

			text += "</div>";

			// add description
			text += "<div>" + getFirstSentence(desc.getDescription()) + "</div>";
			ManagedExtension ext = ManagedExtension.get(desc.getPackageId());
			boolean upToDate = false;
			if (desc.getPackageTypeName().equals("RAPIDMINER_PLUGIN")) {
				if (ext == null) {
					if (selectedForInstallation) {
						text += getMarkedForInstallationHtml();
					} else {
						text += getNotInstalledHtml();
					}
				} else {
					String installed = ext.getLatestInstalledVersion();
					if (installed != null) {
						upToDate = installed.compareTo(desc.getVersion()) >= 0;
						if (upToDate) {
							text += getUpToDateHtml();
						} else {
							if (selectedForInstallation) {
								text += getMarkedForUpdateHtml();
							} else {
								text += getUpdatesAvailableHtml(ext.getLatestInstalledVersion());
							}
						}
					} else {
						if (selectedForInstallation) {
							text += getMarkedForInstallationHtml();
						} else {
							text += getNotInstalledHtml();
						}
					}
				}
			} else if (desc.getPackageTypeName().equals("STAND_ALONE")) {
				String myVersion = RapidMiner.getLongVersion();
				upToDate = ManagedExtension.normalizeVersion(myVersion).compareTo(ManagedExtension.normalizeVersion(desc.getVersion())) >= 0;
				if (selectedForInstallation) {
					text += getMarkedForUpdateHtml();
				} else if (upToDate) {
					text += getUpToDateHtml();
				} else {
					text += getUpdatesAvailableHtml(myVersion);
				}
			}
			text += "</body></html>";

			label.setIcon(packageIcon);
			label.setVerticalTextPosition(SwingConstants.TOP);
			label.setForeground(Color.BLACK);
		} else {
			text = "<html><div style=\"width:250px;\">" + value.toString() + "</div></html>";
		}
		label.setText(text);

		return panel;
	}

	private String getMarkedForInstallationHtml() {
		return "<div style='" + getActionStyle(MARKED_FOR_INSTALL_COLOR) + "'><img src='icon:///16/nav_down_blue.png'/>&nbsp;" + I18N.getGUILabel("marked.for.installation") + "</div>";
	}

	private String getUpToDateHtml() {
		return "<div style='" + getActionStyle(UP_TO_DATE_COLOR) + "'><img src=\"icon:///16/nav_plain_green.png\"/>&nbsp;" + I18N.getGUILabel("package.up.to.date") + "</div>";
	}

	private String getNotInstalledHtml() {
		return "<div style='" + getActionStyle(NOT_INSTALLED_COLOR) + "'>" + I18N.getGUILabel("not.installed") + "</div>";
	}

	private String getMarkedForUpdateHtml() {
		return "<div style='" + getActionStyle(MARKED_FOR_UPDATE__COLOR) + "'><img src=\"icon:///16/nav_refresh_blue.png\"/>&nbsp;" + I18N.getGUILabel("marked.for.update") + "</div>";
	}

	private String getUpdatesAvailableHtml(String installedVersion) {
		return "<div style='" + getActionStyle(UPDATES_AVAILABLE_COLOR) + "'><img src=\"icon:///16/nav_refresh_yellow.png\"/>&nbsp;" + I18N.getGUILabel("installed.version", installedVersion) + "</div>";
	}

	private String getActionStyle(String color) {
		return "height:18px;min-height:18px;line-height:18px;vertical-align:middle;color:" + color + ";margin-top:3px;";
	}
}
