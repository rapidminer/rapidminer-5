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

package com.rapidminer.repository.gui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;

import javax.swing.AbstractButton;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

import com.rapidminer.gui.security.UserCredential;
import com.rapidminer.gui.security.Wallet;
import com.rapidminer.gui.tools.ProgressThread;
import com.rapidminer.gui.tools.ResourceAction;
import com.rapidminer.gui.tools.ResourceLabel;
import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.gui.tools.components.LinkButton;
import com.rapidminer.repository.Repository;
import com.rapidminer.repository.RepositoryException;
import com.rapidminer.repository.RepositoryManager;
import com.rapidminer.repository.remote.RemoteRepository;
import com.rapidminer.tools.I18N;

/** Panel to add remote repositories
 * 
 * @author Simon Fischer, Nils Woehler
 *
 */
public class RemoteRepositoryPanel extends JPanel implements RepositoryConfigurationPanel {

	private static final long serialVersionUID = 1L;

	private final JTextField urlField = new JTextField("http://localhost:8080/", 30);
	private final JTextField aliasField = new JTextField("NewRepository", 30);
	private final JTextField userField = new JTextField(System.getProperty("user.name"), 20);
	private final JPasswordField passwordField = new JPasswordField(20);

	private static final String UNKNOWN_STATUS_LABEL = I18N.getGUILabel("check_connection_settings.unknown");
	private static final String SUCCESS_STATUS_LABEL = I18N.getGUILabel("check_connection_settings.success");
	private static final String CHECKING_STATUS_LABEL = I18N.getGUILabel("check_connection_settings.checking");

	private static final ImageIcon SUCCESS_ICON = SwingTools.createIcon("16/ok.png");
	private static final ImageIcon FAILURE_ICON = SwingTools.createIcon("16/error.png");
	private static final ImageIcon UNKOWN_ICON = SwingTools.createIcon("16/unknown.png");

	private static final Color UNKOWN_STATUS_COLOR = Color.GRAY;
	private static final Color FAILURE_STATUS_COLOR = Color.RED;
	private static final Color SUCCESS_STATUS_COLOR = Color.GREEN.darker().darker();
	private static final Color CHECKING_STATUS_COLOR = Color.BLACK;

	private JButton okButton;
	private JLabel checkLabel = new JLabel(UNKNOWN_STATUS_LABEL);

	private final ResourceAction checkConnectionSettingsAction = new ResourceAction(false, "check_connection_settings") {

		private static final long serialVersionUID = 1L;

		@Override
		public void actionPerformed(ActionEvent e) {
			enableComponents(false);
			new ProgressThread("check_connection_settings", false) {

				@Override
				public void run() {
					getProgressListener().setTotal(100);

					setCheckButtonVisible(false);

					adaptConnectionLabel(CHECKING_STATUS_LABEL, null, CHECKING_STATUS_COLOR);

					getProgressListener().setCompleted(43);

					String errorMessage = RemoteRepository.checkConfiguration(urlField.getText(), userField.getText(), passwordField.getPassword());

					if (errorMessage == null) {
						adaptConnectionLabel(SUCCESS_STATUS_LABEL, SUCCESS_ICON, SUCCESS_STATUS_COLOR);
					} else {
						adaptConnectionLabel(I18N.getMessage(I18N.getGUIBundle(), "gui.label.check_connection_settings.failure", errorMessage)
								, FAILURE_ICON, FAILURE_STATUS_COLOR);
					}

					enableComponents(true);
					getProgressListener().complete();
				}
			}.start();
		}

	};

	private final LinkButton checkButton = new LinkButton(checkConnectionSettingsAction, true);

	private KeyListener resetCheckButtonKeyListener = new KeyListener() {

		@Override
		public void keyTyped(KeyEvent e) {}

		@Override
		public void keyReleased(KeyEvent e) {
			resetConnectionStatusLabel();
		}

		@Override
		public void keyPressed(KeyEvent e) {}
	};

	public RemoteRepositoryPanel() {
		GridBagLayout gbl = new GridBagLayout();
		setLayout(gbl);
		GridBagConstraints c = new GridBagConstraints();
		c.anchor = GridBagConstraints.FIRST_LINE_START;
		c.weighty = 1;
		c.weightx = .5;
		c.insets = new Insets(4, 4, 4, 4);

		// ALIAS
		c.gridwidth = GridBagConstraints.RELATIVE;
		JLabel label = new ResourceLabel("repositorydialog.alias");
		label.setLabelFor(aliasField);
		gbl.setConstraints(label, c);
		add(label);

		c.gridwidth = GridBagConstraints.REMAINDER;
		gbl.setConstraints(aliasField, c);
		add(aliasField);

		// URL
		c.gridwidth = GridBagConstraints.RELATIVE;
		label = new ResourceLabel("repositorydialog.url");
		label.setLabelFor(urlField);
		gbl.setConstraints(label, c);
		add(label);

		c.gridwidth = GridBagConstraints.REMAINDER;
		gbl.setConstraints(urlField, c);
		add(urlField);

		// USERNAME
		c.gridwidth = GridBagConstraints.RELATIVE;
		c.insets = new Insets(10, 4, 4, 4);
		label = new ResourceLabel("repositorydialog.user");
		label.setLabelFor(userField);
		gbl.setConstraints(label, c);
		add(label);

		c.gridwidth = GridBagConstraints.REMAINDER;
		gbl.setConstraints(userField, c);
		add(userField);

		// Password
		c.insets = new Insets(4, 4, 4, 4);
		c.gridwidth = GridBagConstraints.RELATIVE;
		label = new ResourceLabel("repositorydialog.password");
		label.setLabelFor(passwordField);
		gbl.setConstraints(label, c);
		add(label);

		c.gridwidth = GridBagConstraints.REMAINDER;
		gbl.setConstraints(passwordField, c);
		add(passwordField);

		// connection status
		c.insets = new Insets(4, 4, 4, 4);
		c.gridwidth = GridBagConstraints.RELATIVE;
		c.fill = GridBagConstraints.BOTH;
		label = new ResourceLabel("connection_status");
		gbl.setConstraints(label, c);
		add(label);

		c.gridwidth = GridBagConstraints.REMAINDER;
		c.fill = GridBagConstraints.BOTH;
		JPanel checkPanel = new JPanel(new GridBagLayout());
		checkPanel.setPreferredSize(new Dimension(200,25));
		gbl.setConstraints(checkPanel, c);
		add(checkPanel);

		c.gridwidth = GridBagConstraints.RELATIVE;
		checkPanel.add(checkLabel, c);
		c.insets = new Insets(0, 0, 2, 0);
		checkPanel.add(checkButton, c);

		aliasField.selectAll();
		urlField.selectAll();
		userField.selectAll();

		userField.addKeyListener(resetCheckButtonKeyListener);
		passwordField.addKeyListener(resetCheckButtonKeyListener);
		urlField.addKeyListener(resetCheckButtonKeyListener);

		resetConnectionStatusLabel();
	}

	@Override
	public void makeRepository() throws RepositoryException {
		final URL url;
		try {
			url = new URL(urlField.getText());
		} catch (MalformedURLException e) {
			SwingTools.showSimpleErrorMessage("illegal_url", e);
			return;
		}
		String alias = aliasField.getText().trim();
		if (alias.length() == 0) {
			alias = url.toString();
		}
		final String finalAlias = alias;
		
		checkConfiguration(alias);

		ProgressThread pt = new ProgressThread("add_repository") {

			@Override
			public void run() {
				getProgressListener().setTotal(100);
				getProgressListener().setCompleted(10);
				Repository repository = new RemoteRepository(url, finalAlias, userField.getText(), passwordField.getPassword(), false);
				getProgressListener().setCompleted(90);
				RepositoryManager.getInstance(null).addRepository(repository);
				UserCredential authenticationCredentials = new UserCredential(urlField.getText(), userField.getText(), passwordField.getPassword());
				// use alias as ID to store credentials
				Wallet.getInstance().registerCredentials(finalAlias, authenticationCredentials);
				Wallet.getInstance().saveCache();
				getProgressListener().setCompleted(100);
				getProgressListener().complete();
			}
		};
		pt.start();
	}

	private void setCheckButtonVisible(boolean visible) {
		checkButton.setVisible(visible);
		checkLabel.setVisible(!visible);
	}

	private void enableComponents(boolean enabled) {
		aliasField.setEditable(enabled);
		urlField.setEditable(enabled);
		passwordField.setEditable(enabled);
		userField.setEditable(enabled);
		if (okButton != null) {
			okButton.setEnabled(enabled);
		}

	}

	@Override
	public void configureUIElementsFrom(Repository remote) {
		aliasField.setText(((RemoteRepository) remote).getAlias());
		urlField.setText(((RemoteRepository) remote).getBaseUrl().toString());
		userField.setText(((RemoteRepository) remote).getUsername());
		UserCredential credentials = Wallet.getInstance().getEntry(aliasField.getText(), urlField.getText());
		if (credentials != null) {
			passwordField.setText(new String(credentials.getPassword()));
		}
	}

	@Override
	public boolean configure(final Repository repository) {
		URL url;
		try {
			url = new URL(urlField.getText());
		} catch (MalformedURLException e) {
			SwingTools.showSimpleErrorMessage("illegal_url", e);
			return false;
		}

		String userName = userField.getText();
		char[] password = passwordField.getPassword();
		
		String alias = aliasField.getText();
		try {
			// only check if Alias is different
			if (((RemoteRepository) repository).getAlias().equals(alias)) {
				alias = null;
			}
			checkConfiguration(alias);
		} catch (RepositoryException e) {
			SwingTools.showSimpleErrorMessage("cannot_configure_repository", e);
			return false;
		}

		if (alias != null) {
			((RemoteRepository) repository).rename(alias);
		}
		((RemoteRepository) repository).setBaseUrl(url);
		((RemoteRepository) repository).setUsername(userName);
		((RemoteRepository) repository).setPassword(password);

		UserCredential authenticationCredentials = new UserCredential(urlField.getText(), userName, password);
		// use alias as ID to store credentials
		String id = ((RemoteRepository) repository).getAlias();
		Wallet.getInstance().registerCredentials(id, authenticationCredentials);
		Wallet.getInstance().saveCache();
		
		try {
			// this needs to be called after changing the credentials,
			// otherwise the old webservice will keep using the previous credentials
			((RemoteRepository) repository).resetRepositoryService();
		} catch (RepositoryException e) {
			SwingTools.showSimpleErrorMessage("error_connecting_to_server", e);
		}
		
		return true;
	}

	@Override
	public JComponent getComponent() {
		return this;
	}

	@Override
	public void setOkButton(JButton okButton) {
		this.okButton = okButton;
	}

	@Override
	public List<AbstractButton> getAdditionalButtons() {
		LinkedList<AbstractButton> buttons = new LinkedList<AbstractButton>();
//		buttons.add(checkButton);
		return buttons;
	}

	private void resetConnectionStatusLabel() {
		adaptConnectionLabel(UNKNOWN_STATUS_LABEL, UNKOWN_ICON, UNKOWN_STATUS_COLOR);
		setCheckButtonVisible(true);
	}

	private void adaptConnectionLabel(String text, Icon icon, Color color) {
		checkLabel.setText(text);
		checkLabel.setIcon(icon);
		checkLabel.setBackground(color);
		checkLabel.setForeground(color);

	}
	
	/**
	 * Throws a {@link RepositoryException} if the given alias is invalid.
	 * @param alias
	 * @throws RepositoryException
	 */
	private void checkConfiguration(String alias) throws RepositoryException {
		// make sure that it's not possible to create multiple repositories in the same location or with the same alias
		for (Repository repo : RepositoryManager.getInstance(null).getRepositories()) {
			if (repo.getName().equals(alias)) {
				throw new RepositoryException(I18N.getMessage(I18N.getErrorBundle(), "repository.repository_creation_duplicate_alias"));
			}
		}
	}

}
