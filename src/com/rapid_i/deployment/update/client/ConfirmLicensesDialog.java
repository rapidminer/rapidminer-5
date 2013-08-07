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
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JEditorPane;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import com.itextpdf.text.Font;
import com.rapidminer.deployment.client.wsimport.PackageDescriptor;
import com.rapidminer.deployment.client.wsimport.UpdateService;
import com.rapidminer.gui.tools.ExtendedJScrollPane;
import com.rapidminer.gui.tools.ResourceAction;
import com.rapidminer.gui.tools.ResourceLabel;
import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.gui.tools.dialogs.ButtonDialog;
import com.rapidminer.tools.I18N;
import com.rapidminer.tools.RMUrlHandler;

/**
 * @author Venkatesh Umaashankar
 *
 */
public class ConfirmLicensesDialog extends ButtonDialog {

	private static final long serialVersionUID = 4276757146820898347L;
	private JButton okButton;
	private JEditorPane licensePane = new JEditorPane("text/html", "");
	private static final int LIST_WIDTH = 330;
	private JList selectedForInstallList;
	private JList dependentPackages;
	private ResourceLabel licenseLabel;
	private Map<String, String> licenseNameToLicenseTextMap;

	private static String LOADING_LICENSE_TEXT = "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\">"
			+ "<html xmlns=\"http://www.w3.org/1999/xhtml\" dir=\"ltr\" lang=\"en\" xml:lang=\"en\">" + "<head>" + "<table cellpadding=0 cellspacing=0>" + "<tr><td>"
			+ "<img src=\"" + SwingTools.getIconPath("48/hourglass.png") + "\" /></td>" + "<td width=\"5\">" + "</td>" + "<td>" + I18N.getGUILabel("loading_license") + "</td></tr>" + "</table>" + "</head>" + "</html>";

	private static String ERROR_LOADING_LICENSE_TEXCT = "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\">"
			+ "<html xmlns=\"http://www.w3.org/1999/xhtml\" dir=\"ltr\" lang=\"en\" xml:lang=\"en\">" + "<head>" + "<table cellpadding=0 cellspacing=0>" + "<tr><td>"
			+ "<img src=\"" + SwingTools.getIconPath("48/error.png") + "\" /></td>" + "<td width=\"5\">" + "</td>" + "<td>" + I18N.getGUILabel("error_loading_license") + "</td></tr>" + "</table>" + "</head>" + "</html>";

	// this variable checks if license loading has failed. 
	// If so we cannot allow the user to install the packages because he hasn't seen the license
	private boolean licenseLoadingFailed = true;
	private JCheckBox acceptReject;

	public ConfirmLicensesDialog(HashMap<PackageDescriptor, HashSet<PackageDescriptor>> dependency, HashMap<String, String> licenseNameToLicenseTextMap) {
		super("confirm_licenses", "updates");
		setModal(true);

		if (licenseNameToLicenseTextMap != null) {
			this.licenseNameToLicenseTextMap = licenseNameToLicenseTextMap;
			licenseLoadingFailed = false;
		} else {
			this.licenseNameToLicenseTextMap = new HashMap<String, String>();
		}

		JPanel main = new JPanel(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.BOTH;
		c.anchor = GridBagConstraints.FIRST_LINE_START;
		c.weightx = 0;
		c.weighty = 1;
		c.gridwidth = GridBagConstraints.RELATIVE;

		c.gridx = 0;
		c.weighty = 0;
		c.insets = new Insets(0, 0, 2, 0);
		ResourceLabel label = new ResourceLabel("selected_packages");
		label.setFont(label.getFont().deriveFont(Font.BOLD));
		main.add(label, c);

		Set<PackageDescriptor> selectedPackages = dependency.keySet();
		selectedForInstallList = new JList(selectedPackages.toArray());
		label.setLabelFor(selectedForInstallList);

		HashMap<PackageDescriptor, HashSet<PackageDescriptor>> invertedDependency = new HashMap<PackageDescriptor, HashSet<PackageDescriptor>>();

		for (PackageDescriptor source : selectedPackages) {
			for (PackageDescriptor dep : dependency.get(source)) {
				if (!invertedDependency.containsKey(dep)) {
					invertedDependency.put(dep, new HashSet<PackageDescriptor>());
					invertedDependency.get(dep).add(source);
				} else {
					invertedDependency.get(dep).add(source);
				}
			}
		}

		Set<PackageDescriptor> depPackages = invertedDependency.keySet();
		dependentPackages = new JList(depPackages.toArray());

		c.gridx = 0;
		c.gridy = GridBagConstraints.RELATIVE;
		c.weighty = 1;
		c.insets = new Insets(0, 0, 0, 0);
		JScrollPane selectedForInstallPane = new ExtendedJScrollPane(selectedForInstallList);
		selectedForInstallPane.setMinimumSize(new Dimension(LIST_WIDTH, 100));
		selectedForInstallPane.setPreferredSize(new Dimension(LIST_WIDTH, 100));
		selectedForInstallPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		selectedForInstallPane.setBorder(BorderFactory.createMatteBorder(1, 1, 1, 1, Color.LIGHT_GRAY));
		selectedForInstallList.addListSelectionListener(new LicenseListSelectionListener(dependentPackages));
		PackageListCellRenderer sourceCellRenderer = new PackageListCellRenderer(null);
		selectedForInstallList.setCellRenderer(sourceCellRenderer);
		main.add(selectedForInstallPane, c);

		c.gridx = 0;
		c.gridy = 2;
		c.weighty = 0;
		c.insets = new Insets(10, 0, 0, 0);
		ResourceLabel dependentLabel = new ResourceLabel("dependent_packages");
		dependentLabel.setFont(dependentLabel.getFont().deriveFont(Font.BOLD));
		dependentLabel.setLabelFor(dependentPackages);
		main.add(dependentLabel, c);

		c.gridx = 0;
		c.gridy = GridBagConstraints.RELATIVE;
		c.weighty = 1;
		c.insets = new Insets(0, 0, 0, 0);
		JScrollPane dependentPackagesPane = new ExtendedJScrollPane(dependentPackages);
		dependentPackagesPane.setMinimumSize(new Dimension(LIST_WIDTH, 100));
		dependentPackagesPane.setPreferredSize(new Dimension(LIST_WIDTH, 100));
		dependentPackagesPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		dependentPackagesPane.setBorder(BorderFactory.createMatteBorder(1, 1, 1, 1, Color.LIGHT_GRAY));
		dependentPackages.addListSelectionListener(new LicenseListSelectionListener(selectedForInstallList));
		PackageListCellRenderer depCellRenderer = new PackageListCellRenderer(invertedDependency);
		dependentPackages.setCellRenderer(depCellRenderer);
		main.add(dependentPackagesPane, c);

		c.gridx = 1;
		c.gridy = 0;
		c.weighty = 0;
		c.weightx = 1;
		c.insets = new Insets(0, 10, 1, 0);
		;
		licenseLabel = new ResourceLabel("license_label");
		main.add(licenseLabel, c);

		c.gridx = 1;
		c.gridy = GridBagConstraints.RELATIVE;
		c.gridheight = 4;
		c.insets = new Insets(0, 10, 1, 0);
		licensePane.setEditable(false);
		JScrollPane scrollPane = new JScrollPane(licensePane);
		scrollPane.setPreferredSize(new Dimension(400, 400));
		scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		scrollPane.setBorder(BorderFactory.createMatteBorder(1, 1, 1, 1, Color.LIGHT_GRAY));
		main.add(scrollPane, c);
		licensePane.addHyperlinkListener(new HyperlinkListener() {

			@Override
			public void hyperlinkUpdate(HyperlinkEvent e) {
				if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
					RMUrlHandler.handleUrl(e.getDescription());
				}				
			}
		});

		acceptReject = new JCheckBox(new ResourceAction("accept_license") {

			private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed(ActionEvent e) {
				if (acceptReject.isSelected()) {
					enableButtons();
				} else {
					okButton.setEnabled(false);
				}
			}
		});

		okButton = this.makeOkButton("update.install");
		okButton.setText(I18N.getMessage(I18N.getGUIBundle(), "gui.action.update.install.label", selectedPackages.size() + depPackages.size()));
		okButton.setEnabled(false);

		layoutDefault(main, HUGE, acceptReject, okButton, makeCancelButton("skip_install"));

		enableButtons();
	}

	private void setInitialSelection() {
		selectedForInstallList.setSelectedIndex(0);
	}

	private void enableButtons() {
		okButton.setEnabled(acceptReject.isSelected() && !licenseLoadingFailed);
		acceptReject.setEnabled(!licenseLoadingFailed);
	}

	private void setLicensePaneContent(final PackageDescriptor desc) {
		licenseLabel.setText(I18N.getMessage(I18N.getGUIBundle(), "gui.label.license_label.label", desc.getName()));

		final String licenseName = desc.getLicenseName();

		String licenseText = licenseNameToLicenseTextMap.get(licenseName);
		if (licenseText != null) {
			setLicenseText(licenseText);
		} else {
			licensePane.setText(LOADING_LICENSE_TEXT);
			new Thread("fetching-license") { //TODO change to progress thread if running more than one progress thread at a time is possible

				@Override
				public void run() {
					UpdateService service = null;
					try {
						service = UpdateManager.getService();
						String licenseText = service.getLicenseTextHtml(licenseName);
						licenseNameToLicenseTextMap.put(licenseName, licenseText);
						licenseLoadingFailed = false;
						setLicenseText(licenseText);
					} catch (Exception e) {
						licenseLoadingFailed = true;
						setLicenseText(ERROR_LOADING_LICENSE_TEXCT);
					}
					enableButtons();
				}

			}.start();
		}

	}

	private void setLicenseText(final String licenseText) {
		SwingUtilities.invokeLater(new Runnable() {

			@Override
			public void run() {
				licensePane.setText(licenseText);
				licensePane.setCaretPosition(0);
			}
		});
	}

	/** Returns true iff the user chooses to confirm the license. 
	 * @param licenseNameToLicenseTextMap2 
	 * @param numberOfTotalPackages 
	 * @param updateModel */
	public static boolean confirm(HashMap<PackageDescriptor, HashSet<PackageDescriptor>> dependency, HashMap<String, String> licenseNameToLicenseTextMap) {
		ConfirmLicensesDialog d = new ConfirmLicensesDialog(dependency, licenseNameToLicenseTextMap);
		d.setInitialSelection();
		d.setVisible(true);
		return d.wasConfirmed();
	}

	private class LicenseListSelectionListener implements ListSelectionListener {

		private JList otherList;

		public LicenseListSelectionListener(JList otherList) {
			this.otherList = otherList;
		}

		@Override
		public void valueChanged(ListSelectionEvent e) {
			if (!e.getValueIsAdjusting()) {
				JList source = (JList) e.getSource();
				if (!source.isSelectionEmpty()) {
					/* 
					* clear selection in the other list 
					 */
					if (!otherList.isSelectionEmpty()) {
						otherList.clearSelection();
					}

					PackageDescriptor desc = null;
					Object selectedValue = ((JList) source).getSelectedValue();
					desc = (PackageDescriptor) selectedValue;

					try {
						setLicensePaneContent(desc);
					} catch (Exception e1) {
						SwingTools.showSimpleErrorMessage("error_installing_update", e1, e1.getMessage());
					}

				}

			}
		}
	}
}
