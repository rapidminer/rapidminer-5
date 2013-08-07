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
import java.awt.Color;
import java.awt.Component;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Observable;
import java.util.Observer;
import java.util.logging.Level;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JToggleButton;
import javax.swing.ScrollPaneConstants;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.StyleSheet;

import com.rapid_i.deployment.update.client.listmodels.AbstractPackageListModel;
import com.rapid_i.deployment.update.client.listmodels.BookmarksPackageListModel;
import com.rapid_i.deployment.update.client.listmodels.LicencedPackageListModel;
import com.rapidminer.RapidMiner;
import com.rapidminer.deployment.client.wsimport.PackageDescriptor;
import com.rapidminer.gui.tools.ExtendedHTMLJEditorPane;
import com.rapidminer.gui.tools.ExtendedJScrollPane;
import com.rapidminer.gui.tools.ResourceAction;
import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.gui.tools.VersionNumber;
import com.rapidminer.gui.tools.components.LinkButton;
import com.rapidminer.tools.I18N;
import com.rapidminer.tools.LogService;

/**
 * 
 * @author Dominik Halfkann
 *
 */
public class UpdatePanelTab extends JPanel {

	private JPanel extensionButtonPane;

	private static final long serialVersionUID = 1L;

	private static final int LIST_WIDTH = 330;

	protected UpdatePackagesModel updateModel;
	protected AbstractPackageListModel listModel;
	protected UpdateServerAccount usAccount;

	private ExtendedHTMLJEditorPane displayPane;
	private final SelectForInstallationButton installButton;
	private LinkButton loginForInstallHint;
	private LinkButton extensionHomepageLink;
	private PackageDescriptor lastSelected = null;

	private JList packageList;

	private class SelectForInstallationButton extends JToggleButton implements Observer {

		private boolean purchaseFirst = false;

		private static final long serialVersionUID = 1L;

		public SelectForInstallationButton(Action a) {
			super(a);
		}

		public void setPurchaseFirst(boolean purchaseFirst) {
			this.purchaseFirst = purchaseFirst;
		}

		public boolean getPurchaseFirst() {
			return purchaseFirst;
		}

		@Override
		public void update(Observable o, Object arg) {
			if (o instanceof UpdatePackagesModel) {
				UpdatePackagesModel currentModel = (UpdatePackagesModel) o;
				if (arg != null && arg instanceof PackageDescriptor) {
					PackageDescriptor desc = (PackageDescriptor) arg;
					Object selectedObject = getPackageList().getSelectedValue();
					if (selectedObject instanceof PackageDescriptor) {
						PackageDescriptor selectedDescriptor = (PackageDescriptor) selectedObject;
						if (desc.getPackageId().equals(selectedDescriptor.getPackageId())) {
							this.setSelected(currentModel.isSelectedForInstallation(desc));
							if (this.isSelected()) {
								this.setIcon(SwingTools.createIcon("16/checkbox.png"));
							} else {
								this.setIcon(SwingTools.createIcon("16/checkbox_unchecked.png"));
							}

						}
					}
					listModel.updateView(desc);
				}
			}
		}
	}

	public UpdatePanelTab(final UpdatePackagesModel updateModel, AbstractPackageListModel model, final UpdateServerAccount usAccount) {
		super(new GridBagLayout());

		this.updateModel = updateModel;
		this.listModel = model;
		this.usAccount = usAccount;
		this.usAccount.addObserver(new Observer() {

			@Override
			public void update(Observable o, Object arg) {
				updateDisplayPane();
			}
		});

		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.BOTH;
		c.gridheight = GridBagConstraints.REMAINDER;
		c.gridx = 0;
		c.gridy = 0;
		c.weightx = 0;
		c.weighty = 1;
		c.insets = new Insets(0, 0, 0, 0);

		installButton = new SelectForInstallationButton(new ResourceAction(true, "update.select") {

			private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed(ActionEvent e) {
				markForInstallation((PackageDescriptor) getPackageList().getSelectedValue(), true, true);
			}
		});
		installButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				if (!installButton.getPurchaseFirst()) {
					if (installButton.isSelected()) {
						installButton.setIcon(SwingTools.createIcon("16/checkbox.png"));
					} else {
						installButton.setIcon(SwingTools.createIcon("16/checkbox_unchecked.png"));
					}
				}
			}
		});
		installButton.setEnabled(false);
		updateModel.addObserver(installButton);

		displayPane = new ExtendedHTMLJEditorPane("text/html", "");
		displayPane.installDefaultStylesheet();
		((HTMLEditorKit) displayPane.getEditorKit()).getStyleSheet().addRule("a  {text-decoration:underline; color:blue;}");

		setDefaultDescription();

		displayPane.setEditable(false);

		displayPane.addHyperlinkListener(new HyperlinkListener() {

			@Override
			public void hyperlinkUpdate(HyperlinkEvent e) {
				if (HyperlinkEvent.EventType.ACTIVATED.equals(e.getEventType())) {
					try {
						Desktop.getDesktop().browse(e.getURL().toURI());
					} catch (Exception e1) {
						SwingTools.showVerySimpleErrorMessage("cannot_open_browser");
					}
				}
			}
		});

		loginForInstallHint = new LinkButton(new AbstractAction() {

			private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed(ActionEvent e) {
				usAccount.login(updateModel);
			}

		});

		extensionHomepageLink = new LinkButton(new AbstractAction() {

			private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed(ActionEvent e) {

				PackageDescriptor selectedDescriptor = (PackageDescriptor) getPackageList().getSelectedValue();
				if (selectedDescriptor != null) {
					String url = updateModel.getExtensionURL(selectedDescriptor);
					// open link
					Desktop desktop = Desktop.getDesktop();

					if (desktop.isSupported(Desktop.Action.BROWSE)) {
						URI uri;
						try {
							uri = new java.net.URI(url);
							desktop.browse(uri);
						} catch (URISyntaxException e1) {
							LogService.getRoot().log(Level.WARNING, "Malformed extension URI.");
							return;
						} catch (IOException e2) {
							LogService.getRoot().log(Level.WARNING, "Error opening extension URI in the default browser.");
							return;
						}

					}
				}
			}

		});

		packageList = createUpdateList();
		JScrollPane updateListScrollPane = new ExtendedJScrollPane(packageList);
		updateListScrollPane.setMinimumSize(new Dimension(LIST_WIDTH, 100));
		updateListScrollPane.setPreferredSize(new Dimension(LIST_WIDTH, 100));
		updateListScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		Component topPanel = makeTopPanel();
		Component bottomPanel = makeBottomPanel();

		JPanel leftPanel = new JPanel(new BorderLayout());
		leftPanel.add(updateListScrollPane, BorderLayout.CENTER);

		if (topPanel != null) {
			leftPanel.add(topPanel, BorderLayout.NORTH);
			add(leftPanel, c);
		}
		if (bottomPanel != null) {
			leftPanel.add(bottomPanel, BorderLayout.SOUTH);
			add(leftPanel, c);
		}

		c.gridx = 1;
		c.gridy = 0;
		c.weightx = 1;
		c.weighty = 1;
		c.insets = new Insets(0, 0, 0, 0);

		JScrollPane jScrollPane = new ExtendedJScrollPane(displayPane);
		jScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		JPanel descriptionPanel = new JPanel(new BorderLayout());
		descriptionPanel.add(jScrollPane, BorderLayout.CENTER);

		extensionButtonPane = new JPanel(new BorderLayout());
		extensionButtonPane.setMinimumSize(new Dimension(100, 35));
		extensionButtonPane.setPreferredSize(new Dimension(100, 35));

		JPanel extensionButtonPaneLeft = new JPanel(new FlowLayout(FlowLayout.LEFT));

		extensionButtonPaneLeft.add(installButton);
		extensionButtonPaneLeft.add(loginForInstallHint);
		extensionButtonPane.add(extensionButtonPaneLeft, BorderLayout.WEST);

		JPanel extensionButtonPaneRight = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		extensionHomepageLink.setText(I18N.getMessage(I18N.getGUIBundle(), "gui.label.update.extension_homepage.label"));
		extensionButtonPaneRight.add(extensionHomepageLink);
		extensionButtonPane.add(extensionButtonPaneRight, BorderLayout.CENTER);

		for (Component component : extensionButtonPane.getComponents()) {
			component.setVisible(false);
		}

		extensionButtonPane.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, Color.LIGHT_GRAY));

		descriptionPanel.setBorder(BorderFactory.createMatteBorder(0, 1, 0, 0, Color.LIGHT_GRAY));
		descriptionPanel.add(extensionButtonPane, BorderLayout.SOUTH);

		add(descriptionPanel, c);
	}

	private void showProductPage(PackageDescriptor desc) {
		try {
			String url = UpdateManager.getBaseUrl() + "/faces/product_details.xhtml?productId=" + desc.getPackageId();
			Desktop.getDesktop().browse(new URI(url));
		} catch (Exception e1) {
			SwingTools.showVerySimpleErrorMessage("cannot_open_browser");
		}
	}

	protected Component makeTopPanel() {
		return null;
	}

	protected Component makeBottomPanel() {
		JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		panel.setMinimumSize(new Dimension(100, 35));
		panel.setPreferredSize(new Dimension(100, 35));
		panel.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, Color.LIGHT_GRAY));
		return panel;
	}

	private JList createUpdateList() {
		JList updateList = new JList(listModel);
		updateList.addListSelectionListener(new ListSelectionListener() {

			@Override
			public void valueChanged(ListSelectionEvent e) {
				if (!e.getValueIsAdjusting()) {
					updateDisplayPane();
				}
			}

		});
		updateList.addMouseListener(new MouseAdapter() {

			@Override
			public void mouseClicked(MouseEvent e) {
				if (e.getClickCount() == 2) {
					markForInstallation((PackageDescriptor) getPackageList().getSelectedValue(), true, true);
				}
			}
		});
		updateList.setCellRenderer(new UpdateListCellRenderer(updateModel));
		return updateList;
	}

	/**
	 * 
	 * @param selectedDescriptor
	 * @param loginForRestricted shows login dialog if restricted package found but not logged in 
	 * @param showProductPage shows product page if not yet purchased package selected
	 */
	protected void markForInstallation(final PackageDescriptor selectedDescriptor, final boolean loginForRestricted, final boolean showProductPage) {
		// check if extension is installed and up to date
		if (updateModel.isUpToDate(selectedDescriptor)) {
			return;
		}

		// check if selected descriptor is restricted
		if (selectedDescriptor.isRestricted()) {
			if (usAccount.isLoggedIn()) {
				if (updateModel.isPurchased(selectedDescriptor)) {
					UpdatePanelTab.this.updateModel.toggleSelectionForInstallation(selectedDescriptor);
					getModel().updateView(selectedDescriptor);
				} else {
					if (showProductPage) {
						showProductPage(selectedDescriptor);
					}
				}
			} else {
				if (loginForRestricted) {
					usAccount.login(updateModel, false, new Runnable() {

						@Override
						public void run() {
							if (usAccount.isLoggedIn()) {
								if (updateModel.isPurchased(selectedDescriptor)) {
									UpdatePanelTab.this.updateModel.toggleSelectionForInstallation(selectedDescriptor);
									getModel().updateView(selectedDescriptor);
								} else {
									if (showProductPage) {
										showProductPage(selectedDescriptor);
									}
								}
							}
						}
					}, null);
				}
			}
		} else {
			UpdatePanelTab.this.updateModel.toggleSelectionForInstallation(selectedDescriptor);
			getModel().updateView(selectedDescriptor);
		}
	}

	protected JList getPackageList() {
		return packageList;
	}

	public void selectNotify() {
		if (listModel instanceof BookmarksPackageListModel || listModel instanceof LicencedPackageListModel) {
			usAccount.login(updateModel);
		}
		listModel.update();
	}

	public AbstractPackageListModel getModel() {
		return listModel;
	}

	private void setDefaultDescription() {
		new Thread("Load Default Description") {

			@Override
			public void run() {

				try {
					displayPane.setPage("http://rapid-i.com/marketplace_news");
				} catch (Exception e) {
					displayPane.setText(I18N.getMessage(I18N.getGUIBundle(), "gui.dialog.update_welcome_message.text", UpdateManager.getBaseUrl()));
				}

			}
		}.start();
	}

	private void updateDisplayPane() {
		Object selectedValue = packageList.getSelectedValue();
		PackageDescriptor desc = null;
		if (selectedValue instanceof PackageDescriptor) {
			desc = (PackageDescriptor) selectedValue;
			lastSelected = desc;
		} else {
			packageList.clearSelection();
			if (lastSelected != null) {
				desc = lastSelected;
			}
		}
		if (desc != null) {

			for (Component component : extensionButtonPane.getComponents()) {
				component.setVisible(true);
			}
			installButton.setVisible(false);
			extensionButtonPane.setVisible(true);
			StyleSheet css = new StyleSheet();//.makeDefaultStylesheet();
			css.addRule("a  {text-decoration:underline; color:blue;}");
			css.addRule("h1 {font-size: 14px;}");
			css.addRule("h2 {font-size: 11px;font-weight:bold;}");
			css.addRule("div, p, hr { margin-bottom:8px }");
			css.addRule("div.changes-section{padding-left:10px;font-size:9px;color:#444444;}");
			css.addRule(".changes-header-version {margin-top:10px;margin-bottom:5px;color:#111111;}");
			css.addRule("ul {padding-left:10px;}");
			css.addRule("ul li {margin-left:0px;padding-left:0px;}");

			HTMLDocument doc = new HTMLDocument(css);
			displayPane.setDocument(doc);
			displayPane.setText(updateModel.toString(desc, listModel.getChanges(desc.getPackageId())));

			displayPane.setCaretPosition(0);

			installButton.setSelected(updateModel.isSelectedForInstallation(desc));

			boolean isInstalled = false;
			boolean isUpToDate = false;

			boolean isRapidMiner = "STAND_ALONE".equals(desc.getPackageTypeName());
			if (isRapidMiner) {
				isUpToDate = RapidMiner.getVersion().isAtLeast(new VersionNumber(desc.getVersion()));
				isInstalled = true;
			} else {
				//updatesExist = !RapidMiner.getVersion().isAtLeast(new VersionNumber(getService().getLatestVersion("rapidminer", TARGET_PLATFORM)));
				ManagedExtension ext = ManagedExtension.get(desc.getPackageId());
				if (ext != null) {
					isInstalled = true;
					String installed = ext.getLatestInstalledVersion();
					if (installed != null) {
						boolean upToDate = installed.compareTo(desc.getVersion()) >= 0;
						if (upToDate) {
							isUpToDate = true;
						} else {
							isUpToDate = false;
						}
					}
				}
			}

			if (desc.isRestricted() && !isInstalled) {
				if (!usAccount.isLoggedIn()) {
					// restricted, uninstalled, not logged in
					installButton.setVisible(false);
					loginForInstallHint.setText(I18N.getMessage(I18N.getGUIBundle(), "gui.label.update.need_to_log_in.label"));
				} else if (updateModel.isPurchased(desc)) {
					// restricted, purchased but not installed yet
					installButton.setText(I18N.getMessage(I18N.getGUIBundle(), "gui.action.install.select.label"));
					installButton.getAction().putValue(Action.MNEMONIC_KEY, (int) I18N.getMessage(I18N.getGUIBundle(), "gui.action.install.select.mne").toUpperCase().charAt(0));
					extensionHomepageLink.setText(I18N.getMessage(I18N.getGUIBundle(), "gui.label.update.extension_homepage.label"));
					installButton.setPurchaseFirst(false);
					installButton.setVisible(true);
					installButton.setEnabled(true);
					loginForInstallHint.setText("");

					if (updateModel.isSelectedForInstallation(desc)) {
						installButton.setIcon(SwingTools.createIcon("16/checkbox.png"));
					} else {
						installButton.setIcon(SwingTools.createIcon("16/checkbox_unchecked.png"));
					}
				} else {
					// restricted, not purchased
					installButton.setText(I18N.getMessage(I18N.getGUIBundle(), "gui.action.update.purchase.label"));
					installButton.setIcon(SwingTools.createIcon("16/shopping_cart_empty.png"));
					installButton.getAction().putValue(Action.MNEMONIC_KEY, (int) I18N.getMessage(I18N.getGUIBundle(), "gui.action.update.purchase.mne").toUpperCase().charAt(0));
					extensionHomepageLink.setText(I18N.getMessage(I18N.getGUIBundle(), "gui.label.update.extension_homepage.label"));
					installButton.setVisible(true);
					loginForInstallHint.setText("");

					installButton.setPurchaseFirst(true);
				}
			}
			else {
				if (isInstalled) {
					extensionHomepageLink.setText(I18N.getMessage(I18N.getGUIBundle(), "gui.label.update.extension_homepage.label"));
					if (!isUpToDate) {
						// not restricted / restricted and installed but not updated
						installButton.setText(I18N.getMessage(I18N.getGUIBundle(), "gui.action.update.select.label"));
						installButton.getAction().putValue(Action.MNEMONIC_KEY, (int) I18N.getMessage(I18N.getGUIBundle(), "gui.action.update.select.mne").toUpperCase().charAt(0));
						installButton.setPurchaseFirst(false);
						installButton.setEnabled(true);
						installButton.setVisible(true);
						loginForInstallHint.setText("");

						if (updateModel.isSelectedForInstallation(desc)) {
							installButton.setIcon(SwingTools.createIcon("16/checkbox.png"));
						} else {
							installButton.setIcon(SwingTools.createIcon("16/checkbox_unchecked.png"));
						}
					} else {
						// Installed and updated. So showing nothing.
						installButton.setVisible(false);
						loginForInstallHint.setText("");
					}

				} else {
					// not restricted / restricted not installed
					installButton.setText(I18N.getMessage(I18N.getGUIBundle(), "gui.action.install.select.label"));
					installButton.getAction().putValue(Action.MNEMONIC_KEY, (int) I18N.getMessage(I18N.getGUIBundle(), "gui.action.install.select.mne").toUpperCase().charAt(0));
					extensionHomepageLink.setText(I18N.getMessage(I18N.getGUIBundle(), "gui.label.update.extension_homepage.label"));
					installButton.setPurchaseFirst(false);
					installButton.setVisible(true);
					installButton.setEnabled(true);
					loginForInstallHint.setText("");

					if (updateModel.isSelectedForInstallation(desc)) {
						installButton.setIcon(SwingTools.createIcon("16/checkbox.png"));
					} else {
						installButton.setIcon(SwingTools.createIcon("16/checkbox_unchecked.png"));
					}
				}
			}
			if (isRapidMiner) {
				extensionHomepageLink.setText(I18N.getMessage(I18N.getGUIBundle(), "gui.label.update.product_homepage.label"));
			}
		}
	}

	@Override
	public void removeNotify() {
		super.removeNotify();
		usAccount.deleteObservers();
		updateModel.deleteObservers();
	}
}
