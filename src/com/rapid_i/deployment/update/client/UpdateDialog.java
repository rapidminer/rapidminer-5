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
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Observable;
import java.util.Observer;
import java.util.logging.Level;

import javax.swing.AbstractAction;
import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.xml.ws.WebServiceException;

import com.rapidminer.deployment.client.wsimport.PackageDescriptor;
import com.rapidminer.deployment.client.wsimport.UpdateService;
import com.rapidminer.gui.RapidMinerGUI;
import com.rapidminer.gui.tools.ProgressThread;
import com.rapidminer.gui.tools.ResourceAction;
import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.gui.tools.components.LinkButton;
import com.rapidminer.gui.tools.dialogs.ButtonDialog;
import com.rapidminer.gui.tools.dialogs.ConfirmDialog;
import com.rapidminer.tools.I18N;
import com.rapidminer.tools.LogService;
import com.rapidminer.tools.NetTools;
import com.rapidminer.tools.ProgressListener;
import com.rapidminer.tools.plugin.Dependency;

/**
 * 
 * @author Simon Fischer
 * 
 */
public class UpdateDialog extends ButtonDialog {

	private static final long serialVersionUID = 1L;
	static {
		NetTools.init();
	}
	public static final Action UPDATE_ACTION = new ResourceAction("update_manager") {

		private static final long serialVersionUID = 1L;
		{
			setCondition(EDIT_IN_PROGRESS, DONT_CARE);
		}

		@Override
		public void actionPerformed(ActionEvent arg0) {
			showUpdateDialog(false);
		}
	};

	private WindowListener windowListener = new WindowListener() {

		public void windowActivated(WindowEvent e) {
			UpdateServerAccount account = UpdateManager.getUpdateServerAccount();
			account.updatePurchasedPackages(updateModel);
		}

		@Override
		public void windowOpened(WindowEvent e) {}

		@Override
		public void windowClosing(WindowEvent e) {}

		@Override
		public void windowClosed(WindowEvent e) {}

		@Override
		public void windowIconified(WindowEvent e) {}

		@Override
		public void windowDeiconified(WindowEvent e) {}

		@Override
		public void windowDeactivated(WindowEvent e) {}
	};

	private final UpdatePanel ulp;

	private static UpdatePackagesModel updateModel;

	private static class USAcountInfoButton extends LinkButton implements Observer {

		private static final long serialVersionUID = 1L;

		public USAcountInfoButton() {
			super(new AbstractAction("") {

				private static final long serialVersionUID = 1L;

				@Override
				public void actionPerformed(ActionEvent e) {
					UpdateServerAccount account = UpdateManager.getUpdateServerAccount();
					if ("#register".equals(e.getActionCommand())) {
						try {
							Desktop.getDesktop().browse(new URI(UpdateManager.getBaseUrl() + "/faces/signup.xhtml"));
						} catch (Exception ex) {
							SwingTools.showSimpleErrorMessage("cannot_open_browser", ex);
						}
					} else {
						if (account.isLoggedIn()) {
							account.logout(updateModel);
						} else {
							account.login(updateModel);
						}
					}

				}
			});

			Dimension size = new Dimension(300, 24);
			this.setSize(size);
			this.setMaximumSize(size);
			this.setPreferredSize(size);
		}

		@Override
		public void update(Observable obs, Object arg) {
			if (obs instanceof UpdateServerAccount) {
				UpdateServerAccount account = (UpdateServerAccount) obs;
				if (account.isLoggedIn()) {
					this.setText(I18N.getMessage(I18N.getGUIBundle(), "gui.dialog.update.account_button.logged_in", account.getUserName()));
				} else {
					this.setText(I18N.getMessage(I18N.getGUIBundle(), "gui.dialog.update.account_button.logged_out"));
				}
			}
		}
	}

	private class InstallButton extends JButton implements Observer {

		private static final long serialVersionUID = 1L;

		InstallButton(Action a) {
			super(a);
			updateButton();
		}

		@Override
		public void update(Observable o, Object arg) {
			if (o instanceof UpdatePackagesModel) {
				//UpdatePackagesModel currentModel = (UpdatePackagesModel)o;
				updateButton();
			}
		}

		private void updateButton() {
			UpdatePackagesModel currentModel = updateModel;
			if (currentModel.getInstallationList() != null && currentModel.getInstallationList().size() > 0) {
				this.setText(I18N.getMessage(I18N.getGUIBundle(), "gui.action.update.install.label", currentModel.getInstallationList().size()));
				this.setEnabled(true);
			} else {
				this.setText(I18N.getMessage(I18N.getGUIBundle(), "gui.action.update.install.label", 0));
				this.setEnabled(false);
			}
		}

	}

	private USAcountInfoButton accountInfoButton = new USAcountInfoButton();

	private InstallButton installButton;

	private final PackageDescriptorCache packageDescriptorCache = new PackageDescriptorCache();

	private boolean isConfirmed = false;

	private LinkedList<PackageDescriptor> installablePackageList;

	private JButton closeButton;

	public UpdateDialog(String[] preselectedExtensions) {
		super("update");
		setModal(true);
		UpdateServerAccount usAccount = UpdateManager.getUpdateServerAccount();
		usAccount.addObserver(accountInfoButton);
		updateModel = new UpdatePackagesModel(packageDescriptorCache, usAccount);
		ulp = new UpdatePanel(this, packageDescriptorCache, preselectedExtensions, usAccount, updateModel);
		closeButton = makeCloseButton();
		layoutDefault(ulp, HUGE,  makeOkButton(), closeButton);
		this.addWindowListener(windowListener);
	}

	@Override
	protected JButton makeOkButton(String i18nKey) {

		Action okAction = new ResourceAction(i18nKey) {

			private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed(ActionEvent e) {
				wasConfirmed = true;
				ok();
			}
		};
		installButton = new InstallButton(okAction);
		getRootPane().setDefaultButton(installButton);

		installButton.setEnabled(false);
		updateModel.addObserver(installButton);
		return installButton;
	}

	@Override
	/** Overriding makeButtonPanel in order to display account information. **/
	protected JPanel makeButtonPanel(AbstractButton... buttons) {
		JPanel buttonPanel = new JPanel(new BorderLayout());
		JPanel buttonPanelRight = new JPanel(new FlowLayout(FlowLayout.RIGHT, GAP, GAP));
		for (AbstractButton button : buttons) {
			if (button != null) {
				buttonPanelRight.add(button);
			}
		}
		buttonPanel.add(buttonPanelRight, BorderLayout.CENTER);
		JPanel buttonPanelLeft = new JPanel(new FlowLayout(FlowLayout.LEFT, GAP, 2 * GAP));
		buttonPanelLeft.add(accountInfoButton, false);
		buttonPanel.add(buttonPanelLeft, BorderLayout.WEST);
		return buttonPanel;
	}

	/**
	 * Opens the marketplace dialog and selects the update tab.
	 */
	public static void showUpdateDialog(final boolean selectUpdateTab, final String... preselectedExtensions) {

		new ProgressThread("open_marketplace_dialog", true) {

			@Override
			public void run() {

				getProgressListener().setTotal(100);
				getProgressListener().setCompleted(33);
				try {
					UpdateManager.resetService();
					UpdateManager.getService();
				} catch (WebServiceException e) {
					// thrown when no internet connection is available. Simple error message to not confuse users
					SwingTools.showVerySimpleErrorMessage("failed_update_server_simple");
					LogService.getRoot().log(Level.WARNING, "com.rapid_i.deployment.update.client.UpdateDialog.could_not_connect", e);
					return;
				} catch (Exception e) {
					SwingTools.showSimpleErrorMessage("failed_update_server", e, UpdateManager.getBaseUrl());
					LogService.getRoot().log(Level.WARNING, "com.rapid_i.deployment.update.client.UpdateDialog.could_not_connect", e);
					return;
				}
				getProgressListener().setCompleted(100);

				SwingUtilities.invokeLater(new Runnable() {

					@Override
					public void run() {
						UpdateDialog updateDialog = new UpdateDialog(preselectedExtensions);
						if (selectUpdateTab) {
							updateDialog.showUpdateTab();
						}
						updateDialog.setVisible(true);
					}
				});

			}
		}.start();
	}

	private void showUpdateTab() {
		ulp.selectUpdatesTab();
	}

	public void startUpdate(final List<PackageDescriptor> downloadList) {
		installButton.setEnabled(false);

		new ProgressThread("resolving_dependencies", true) {

			@Override
			public void run() {
				try {
					SwingUtilities.invokeLater(new Runnable() {
						
						@Override
						public void run() {
							closeButton.setEnabled(false);
						}
					});
					getProgressListener().setTotal(100);
					final HashMap<PackageDescriptor, HashSet<PackageDescriptor>> dependency = resolveDependency(downloadList, packageDescriptorCache);
					installablePackageList = getPackagesforInstallation(dependency);
					getProgressListener().setCompleted(30);
					final HashMap<String, String> licenseNameToLicenseTextMap = collectLicenses(installablePackageList,getProgressListener(),100,30,100);

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

											updateModel.clearFromSelectionMap(installedPackages);
											ulp.validate();
											ulp.repaint();

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
											
											getProgressListener().setCompleted(100);

										}
									} catch (Exception e) {
										SwingTools.showSimpleErrorMessage("error_installing_update", e, e.getMessage());
									} finally {
										getProgressListener().complete();
										installButton.setEnabled(true);
									}
								}

							}.start();
						}
					});
					SwingUtilities.invokeLater(new Runnable() {
						
						@Override
						public void run() {
							closeButton.setEnabled(true);
						}
					});
					getProgressListener().complete();
				} catch (Exception e) {
					SwingTools.showSimpleErrorMessage("error_resolving_dependencies", e, e.getMessage());
				} finally {
					getProgressListener().complete();
					installButton.setEnabled(true);
				}
			}
		}.start();

	}

	/**
	 * @param Start, int end 
	 * @param Total 
	 * @param progressListener 
	 * @param installablePackageList2
	 * @return
	 * @throws URISyntaxException 
	 * @throws MalformedURLException 
	 */
	protected static HashMap<String, String> collectLicenses(LinkedList<PackageDescriptor> installablePackageList, ProgressListener progressListener, int total, int start, int end) throws MalformedURLException, URISyntaxException {
		int range = end - start;
		double perIterationPercent = (range * 1.0 / installablePackageList.size());
		int iterCnt = 1;
		HashMap<String, String> licenseNameToLicenseTextMap = null;
		licenseNameToLicenseTextMap = new HashMap<String, String>();
		UpdateService service = UpdateManager.getService();
		for (PackageDescriptor packageDescriptor : installablePackageList) {
			String licenseName = packageDescriptor.getLicenseName();
			String licenseText = service.getLicenseTextHtml(licenseName);
			licenseNameToLicenseTextMap.put(licenseName, licenseText);
			progressListener.setCompleted((int)(start + (iterCnt*perIterationPercent)));
			iterCnt++;

		}
		return licenseNameToLicenseTextMap;
	}

	@Override
	protected void ok() {
		ulp.startUpdate();
	}

	public static LinkedList<PackageDescriptor> getPackagesforInstallation(HashMap<PackageDescriptor, HashSet<PackageDescriptor>> dependency) {
		HashSet<PackageDescriptor> installabledPackages = new HashSet<PackageDescriptor>();
		for (PackageDescriptor packageDescriptor : dependency.keySet()) {
			installabledPackages.add(packageDescriptor);
			installabledPackages.addAll(dependency.get(packageDescriptor));
		}
		LinkedList<PackageDescriptor> installablePackageList = new LinkedList<PackageDescriptor>();
		installablePackageList.addAll(installabledPackages);
		return installablePackageList;
	}

	/**
	 * Recursively collect all the dependent extentions of a given extension			 *
	 * @param desc 
	 * @param pluginsSelectedForDownload 
	 * @return
	 */
	private static HashSet<Dependency> collectDependency(PackageDescriptor desc, HashSet<String> pluginsSelectedForDownload, PackageDescriptorCache packageDescriptorCache) {
		HashSet<Dependency> dependencySet = new HashSet<Dependency>();
		List<Dependency> dependencies = Dependency.parse(desc.getDependencies());
		for (Dependency dependency : dependencies) {
			String packageId = dependency.getPluginExtensionId();
			PackageDescriptor packageInfo = packageDescriptorCache.getPackageInfo(packageId);
			if ((!dependencySet.contains(packageInfo)) && (!pluginsSelectedForDownload.contains(packageId))) {
				dependencySet.add(dependency);
				dependencySet.addAll(collectDependency(packageInfo, pluginsSelectedForDownload, packageDescriptorCache));

			}

		}
		return dependencySet;
	}

	public static HashMap<PackageDescriptor, HashSet<PackageDescriptor>> resolveDependency(final List<PackageDescriptor> downloadList, PackageDescriptorCache packageDescriptorCache) {

		HashMap<PackageDescriptor, HashSet<PackageDescriptor>> dependentPackageMap = new HashMap<PackageDescriptor, HashSet<PackageDescriptor>>();

		HashSet<String> pluginsSelectedForDownload = new HashSet<String>();

		for (PackageDescriptor packageDescriptor : downloadList) {
			pluginsSelectedForDownload.add(packageDescriptor.getPackageId());
			dependentPackageMap.put(packageDescriptor, new HashSet<PackageDescriptor>());
		}

		for (PackageDescriptor desc : downloadList) {
			HashSet<Dependency> dependencySet = collectDependency(desc, pluginsSelectedForDownload, packageDescriptorCache);
			for (Dependency dependency : dependencySet) {
				dependentPackageMap.get(desc).add(packageDescriptorCache.getPackageInfo(dependency.getPluginExtensionId()));
			}

		}
		return dependentPackageMap;
	}
}
