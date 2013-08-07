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

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;

import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.rapid_i.deployment.update.client.listmodels.AbstractPackageListModel;
import com.rapidminer.deployment.client.wsimport.PackageDescriptor;
import com.rapidminer.deployment.client.wsimport.UpdateService;
import com.rapidminer.gui.RapidMinerGUI;
import com.rapidminer.gui.tools.ExtendedJScrollPane;
import com.rapidminer.gui.tools.ProgressThread;
import com.rapidminer.gui.tools.ResourceAction;
import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.gui.tools.dialogs.ButtonDialog;
import com.rapidminer.gui.tools.dialogs.ConfirmDialog;
import com.rapidminer.io.process.XMLTools;
import com.rapidminer.tools.FileSystemService;
import com.rapidminer.tools.I18N;
import com.rapidminer.tools.LogService;
import com.rapidminer.tools.ParameterService;
import com.rapidminer.tools.XMLException;

/**
 * The Dialog is eventually shown at the start of RapidMiner, if the user purchased extensions online but haven't installed them yet.
 * 
 * @author Dominik Halfkann
 */
public class PendingPurchasesInstallationDialog extends ButtonDialog {

	private static final long serialVersionUID = 1L;
	private final PackageDescriptorCache packageDescriptorCache = new PackageDescriptorCache();
	private AbstractPackageListModel purchasedModel = new PurchasedNotInstalledModel(packageDescriptorCache);
	JCheckBox neverAskAgain = new JCheckBox(I18N.getMessage(I18N.getGUIBundle(), "gui.dialog.purchased_not_installed.not_check_on_startup"));

	private final List<String> packages;
	private boolean isConfirmed;
	private LinkedList<PackageDescriptor> installablePackageList;
	private JButton remindNeverButton;
	private JButton remindLaterButton;
	private JButton okButton;
	
	private class PurchasedNotInstalledModel extends AbstractPackageListModel {

		private static final long serialVersionUID = 1L;

		public PurchasedNotInstalledModel(PackageDescriptorCache cache) {
			super(cache, "gui.dialog.update.tab.no_packages");
		}

		@Override
		public List<String> handleFetchPackageNames() {
			return packages;
		}
	}

	public PendingPurchasesInstallationDialog(List<String> packages) {
		super("purchased_not_installed");
		this.packages = packages;
		remindNeverButton = remindNeverButton();
		remindLaterButton = remindLaterButton();
		okButton = makeOkButton("install_purchased");
		layoutDefault(makeContentPanel(), NORMAL, okButton, remindNeverButton, remindLaterButton);
		this.setPreferredSize(new Dimension(404, 430));
		this.setMaximumSize(new Dimension(404, 430));
		this.setMinimumSize(new Dimension(404, 300));
		this.setSize(new Dimension(404, 430));
	}

	private JPanel makeContentPanel() {
		BorderLayout layout = new BorderLayout(12, 12);
		JPanel panel = new JPanel(layout);
		panel.setBorder(new EmptyBorder(0, 12, 8, 12));
		panel.add(createExtensionListScrollPane(purchasedModel), BorderLayout.CENTER);
		purchasedModel.update();
		JPanel southPanel = new JPanel(new BorderLayout(0, 7));
		JLabel question = new JLabel(I18N.getMessage(I18N.getGUIBundle(), "gui.dialog.purchased_not_installed.should_install"));

		southPanel.add(question, BorderLayout.CENTER);
		southPanel.add(neverAskAgain, BorderLayout.SOUTH);
		panel.add(southPanel, BorderLayout.SOUTH);		
		
		return panel;
	}

	private JScrollPane createExtensionListScrollPane(AbstractPackageListModel model) {
		final JList updateList = new JList(model);
		updateList.setCellRenderer(new UpdateListCellRenderer(true));
		JScrollPane extensionListScrollPane = new ExtendedJScrollPane(updateList);
		extensionListScrollPane.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED));
		return extensionListScrollPane;
	}

	private JButton remindLaterButton() {
		Action Action = new ResourceAction("ask_later") {

			private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed(ActionEvent e) {
				wasConfirmed = false;
				checkNeverAskAgain();
				close();
			}
		};
		getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0, false), "CLOSE");
		getRootPane().getActionMap().put("CLOSE", Action);
		JButton button = new JButton(Action);
		getRootPane().setDefaultButton(button);
		return button;
	}

	private JButton remindNeverButton() {
		Action Action = new ResourceAction("ask_never") {

			private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed(ActionEvent e) {
				wasConfirmed = false;
				checkNeverAskAgain();
				neverRemindAgain();
				close();
			}
		};
		JButton button = new JButton(Action);
		getRootPane().setDefaultButton(button);
		return button;
	}

	@Override
	protected void ok() {
		checkNeverAskAgain();
		startUpdate(getPackageDescriptorList());
		dispose();
	}

	public List<PackageDescriptor> getPackageDescriptorList() {
		List<PackageDescriptor> packageList = new ArrayList<PackageDescriptor>();
		for (int a = 0; a < purchasedModel.getSize(); a++) {
			Object listItem = purchasedModel.getElementAt(a);
			if (listItem instanceof PackageDescriptor) {
				packageList.add((PackageDescriptor) listItem);
			}
		}
		return packageList;
	}

	public void startUpdate(final List<PackageDescriptor> downloadList) {
		final UpdateService service;
		try {
			service = UpdateManager.getService();
		} catch (Exception e) {
			SwingTools.showSimpleErrorMessage("failed_update_server", e, UpdateManager.getBaseUrl());
			return;
		}

		new ProgressThread("resolving_dependencies", true) {

			@Override
			public void run() {
				try {
					getProgressListener().setTotal(100);
					remindLaterButton.setEnabled(false);
					remindNeverButton.setEnabled(false);
					final HashMap<PackageDescriptor, HashSet<PackageDescriptor>> dependency = UpdateDialog.resolveDependency(downloadList, packageDescriptorCache);
					getProgressListener().setCompleted(30);
					installablePackageList = UpdateDialog.getPackagesforInstallation(dependency);
					final HashMap<String, String> licenseNameToLicenseTextMap = UpdateDialog.collectLicenses(installablePackageList,getProgressListener(),100,30,100);
					SwingUtilities.invokeLater(new Runnable() {

						@Override
						public void run() {
							isConfirmed = ConfirmLicensesDialog.confirm(dependency, licenseNameToLicenseTextMap);
							new ProgressThread("installing_updates", true) {

								@Override
								public void run() {
									try {
										if (isConfirmed) {

											getProgressListener().setTotal(100);
											getProgressListener().setCompleted(20);

											UpdateService service = UpdateManager.getService();
											UpdateManager um = new UpdateManager(service);
											List<PackageDescriptor> installedPackages = um.performUpdates(installablePackageList, getProgressListener());
											getProgressListener().setCompleted(40);

											if (installedPackages.size() > 0) {
												int confirmation = SwingTools.showConfirmDialog((installedPackages.size() == 1 ? "update.complete_restart" : "update.complete_restart1"),
														ConfirmDialog.YES_NO_OPTION, installedPackages.size());
												if (confirmation == ConfirmDialog.YES_OPTION) {
													RapidMinerGUI.getMainFrame().exit(true);
												} else if (confirmation == ConfirmDialog.NO_OPTION) {
													if (installedPackages.size() == installablePackageList.size()) {
														dispose();
													}
												}
											}

											getProgressListener().complete();

										}
									} catch (Exception e) {
										SwingTools.showSimpleErrorMessage("error_installing_update", e, e.getMessage());
									} finally {
										getProgressListener().complete();
									}
								}

							}.start();
						}
					});
					remindLaterButton.setEnabled(true);
					remindNeverButton.setEnabled(true);
					getProgressListener().complete();
				} catch (Exception e) {
					SwingTools.showSimpleErrorMessage("error_resolving_dependencies", e, e.getMessage());
				}

			}

		}.start();

	}

	private void checkNeverAskAgain() {
		if (neverAskAgain.isSelected()) {
			ParameterService.setParameterValue(RapidMinerGUI.PROPERTY_RAPIDMINER_GUI_PURCHASED_NOT_INSTALLED_CHECK, "false");
			ParameterService.saveParameters();
		}
	}

	private void neverRemindAgain() {
		LogService.getRoot().log(Level.CONFIG, "com.rapid_i.deployment.update.client.PurchasedNotInstalledDialog.saving_ignored_extensions_file");
		Document doc;
		try {
			doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
		} catch (ParserConfigurationException e) {
			LogService.getRoot().log(Level.WARNING,
					I18N.getMessage(LogService.getRoot().getResourceBundle(),
							"com.rapid_i.deployment.update.client.PurchasedNotInstalledDialog.creating_xml_document_error",
							e),
					e);
			return;
		}
		Element root = doc.createElement(UpdateManager.NEVER_REMIND_INSTALL_EXTENSIONS_FILE_NAME);
		doc.appendChild(root);
		for (String i : purchasedModel.fetchPackageNames()) {
			Element entryElem = doc.createElement("extension_name");
			entryElem.setTextContent(i);
			root.appendChild(entryElem);
		}
		File file = FileSystemService.getUserConfigFile(UpdateManager.NEVER_REMIND_INSTALL_EXTENSIONS_FILE_NAME);
		try {
			XMLTools.stream(doc, file, null);
		} catch (XMLException e) {
			LogService.getRoot().log(Level.WARNING,
					I18N.getMessage(LogService.getRoot().getResourceBundle(),
							"com.rapid_i.deployment.update.client.PurchasedNotInstalledDialog.saving_ignored_extensions_file_error",
							e),
					e);
		}
	}
}
