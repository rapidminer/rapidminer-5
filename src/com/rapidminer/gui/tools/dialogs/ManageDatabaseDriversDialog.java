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
package com.rapidminer.gui.tools.dialogs;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.LinkedList;
import java.util.List;
import java.util.jar.JarFile;
import java.util.logging.Level;

import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import com.rapidminer.gui.RapidMinerGUI;
import com.rapidminer.gui.tools.ProgressThread;
import com.rapidminer.gui.tools.ResourceAction;
import com.rapidminer.gui.tools.ResourceLabel;
import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.tools.I18N;
import com.rapidminer.tools.LogService;
import com.rapidminer.tools.Tools;
import com.rapidminer.tools.XMLException;
import com.rapidminer.tools.jdbc.DatabaseService;
import com.rapidminer.tools.jdbc.JDBCProperties;

/**
 * 
 * @author Simon Fischer
 * 
 */
public class ManageDatabaseDriversDialog extends ButtonDialog {
	private static final long serialVersionUID = 1L;
	private JList availableDrivers;

	public static final Action SHOW_DIALOG_ACTION = new ResourceAction("manage_database_drivers") {
		private static final long serialVersionUID = 1L;

		@Override
		public void actionPerformed(ActionEvent e) {
			new ManageDatabaseDriversDialog().setVisible(true);
		}
	};

	private class DriverPane extends JPanel {
		private static final long serialVersionUID = 1L;
		private JDBCProperties properties;
		private JTextField nameField = new JTextField(20);
		private JTextField urlprefixField = new JTextField(20);
		private JTextField portField = new JTextField(20);
		private JTextField jarFileField = new JTextField(20);
		private JTextField dbseparatorField = new JTextField(20);
		private JComboBox classNameCombo = new JComboBox();

		public DriverPane() {
			setLayout(new GridBagLayout());
			classNameCombo.setEditable(true);

			JButton fileButton = new JButton(new ResourceAction(true, "manage_database_drivers.jarfile") {
				private static final long serialVersionUID = 1L;

				@Override
				public void actionPerformed(ActionEvent e) {
					File file = SwingTools.chooseFile(DriverPane.this, null, true, "jar", "JDBC driver jar file");
					if (file != null) {
						jarFileField.setText(file.getAbsolutePath());
						((DefaultComboBoxModel) classNameCombo.getModel()).removeAllElements();
						for (String driver : findDrivers(file)) {
							((DefaultComboBoxModel) classNameCombo.getModel()).addElement(driver);
						}
					}
				}
			});

			add("name", nameField, null);
			add("urlprefix", urlprefixField, null);
			add("port", portField, null);
			add("dbseparator", dbseparatorField, null);
			add("jarfile", jarFileField, fileButton);
			add("classname", classNameCombo, null);
		}

		private void add(String labelKey, JComponent component, JComponent button) {
			GridBagConstraints c = new GridBagConstraints();
			c.anchor = GridBagConstraints.FIRST_LINE_START;
			c.weightx = 0.5;
			c.weighty = 1;
			c.fill = GridBagConstraints.BOTH;
			c.gridheight = 1;
			ResourceLabel label = new ResourceLabel("manage_database_drivers." + labelKey);
			label.setLabelFor(component);
			c.gridwidth = GridBagConstraints.REMAINDER;
			add(label, c);

			c.insets = new Insets(0, 0, 5, 0);
			if (button == null) {
				c.gridwidth = GridBagConstraints.REMAINDER;
				add(component, c);
			} else {
				c.gridwidth = GridBagConstraints.RELATIVE;
				c.weightx = 1;
				add(component, c);

				c.gridwidth = GridBagConstraints.REMAINDER;
				c.weightx = 0;
				add(button, c);
			}
		}

		private void setProperties(JDBCProperties props) {
			if (this.properties != props) {
				save();
			}
			this.properties = props;
			((DefaultComboBoxModel) classNameCombo.getModel()).removeAllElements();
			if (props == null) {
				SwingTools.setEnabledRecursive(this, false);
				nameField.setText("");
				urlprefixField.setText("");
				portField.setText("");
				classNameCombo.setSelectedItem("");
				jarFileField.setText("");
				dbseparatorField.setText("/");
			} else {
				nameField.setText(props.getName());
				urlprefixField.setText(props.getUrlPrefix());
				portField.setText(props.getDefaultPort());
				classNameCombo.setSelectedItem(Tools.toString(props.getDriverClasses(), ","));
				jarFileField.setText(props.getDriverJarFile());
				dbseparatorField.setText(props.getDbNameSeperator());
				if (props.isUserDefined()) {
					SwingTools.setEnabledRecursive(this, true);
				} else {
					SwingTools.setEnabledRecursive(this, false);
				}
			}
			deleteButton.setEnabled((props != null) && props.isUserDefined());
		}

		private void save() {
			if ((properties != null) && properties.isUserDefined()) {
				properties.setName(nameField.getText());
				properties.setUrlPrefix(urlprefixField.getText());
				properties.setDefaultPort(portField.getText());
				properties.setDriverJarFile(jarFileField.getText());
				properties.setDbNameSeperator(dbseparatorField.getText());
				final String className = (String) classNameCombo.getSelectedItem();
				if (className != null) {
					properties.setDriverClasses(className);
				}
			}
		}
	}

	private boolean needsRestart = false;
	private DriverPane driverPane = new DriverPane();
	private AbstractButton deleteButton = new JButton(new ResourceAction("manage_database_drivers.delete") {
		private static final long serialVersionUID = 1L;

		@Override
		public void actionPerformed(ActionEvent e) {
			JDBCProperties props = driverPane.properties;
			if ((props != null) && props.isUserDefined()) {
				((DefaultListModel) availableDrivers.getModel()).removeElement(props);
				DatabaseService.removeJDBCProperties(props);
			}
		}
	});

	public ManageDatabaseDriversDialog() {
		super("manage_database_drivers");
		JPanel main = new JPanel(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.ipadx = c.ipady = 5;
		c.insets = new Insets(5, 5, 5, 5);
		DefaultListModel model = new DefaultListModel();
		for (JDBCProperties props : DatabaseService.getJDBCProperties()) {
			model.addElement(props);
		}
		availableDrivers = new JList(model);
		availableDrivers.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		availableDrivers.addListSelectionListener(new ListSelectionListener() {
			@Override
			public void valueChanged(ListSelectionEvent e) {
				final JDBCProperties selected = (JDBCProperties) availableDrivers.getSelectedValue();
				driverPane.setProperties(selected);
			}
		});

		c.anchor = GridBagConstraints.FIRST_LINE_START;
		c.weightx = 0.5;
		c.weighty = 1;
		c.fill = GridBagConstraints.BOTH;
		c.gridheight = 1;
		c.gridwidth = GridBagConstraints.RELATIVE;
		main.add(new JScrollPane(availableDrivers), c);
		c.gridwidth = GridBagConstraints.REMAINDER;
		main.add(driverPane, c);

		AbstractButton addButton = new JButton(new ResourceAction("manage_database_drivers.add") {
			private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed(ActionEvent e) {
				final JDBCProperties newProps = new JDBCProperties(true);
				((DefaultListModel) availableDrivers.getModel()).addElement(newProps);
				availableDrivers.setSelectedValue(newProps, true);
				DatabaseService.addJDBCProperties(newProps);
			}
		});
		AbstractButton saveButton = new JButton(new ResourceAction("manage_database_drivers.save") {
			private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed(ActionEvent e) {
				try {
					driverPane.save();
					DatabaseService.saveUserDefinedProperties();
					needsRestart = true;
					ManageDatabaseDriversDialog.this.dispose();
				} catch (XMLException e1) {
					SwingTools.showSimpleErrorMessage("manage_database_drivers.error_saving", e1, e1.getMessage());
				}
			}
		});

		layoutDefault(main, addButton, deleteButton, saveButton,
				makeCloseButton());

		driverPane.setProperties(null);
	}

	@Override
	public void close() {
		if (needsRestart && SwingTools.showConfirmDialog("manage_database_drivers.restart", ConfirmDialog.YES_NO_OPTION) == ConfirmDialog.YES_OPTION) {
			super.close();
			RapidMinerGUI.getMainFrame().exit(true);
		} else {
			super.close();
		}
	}

	private List<String> findDrivers(final File file) {
		final List<String> driverNames = new LinkedList<String>();
		new ProgressThread("manage_database_drivers.scan_jar", true) {
			public void run() {
				try {
					ClassLoader ucl = AccessController.doPrivileged(new PrivilegedExceptionAction<ClassLoader>() {
						public ClassLoader run() throws Exception {
							try {
								return new URLClassLoader(new URL[] { file.toURI().toURL() });
							} catch (MalformedURLException e) {
								throw new RuntimeException("Cannot create class loader for file '" + file + "': " + e.getMessage(), e);
							}
						};
					});

					try {
						JarFile jarFile = new JarFile(file);
						Tools.findImplementationsInJar(ucl, jarFile, java.sql.Driver.class, driverNames);

					} catch (Exception e) {
						//LogService.getRoot().log(Level.WARNING, "Cannot scan jar file '" + file + "' for drivers: " + e.getMessage(), e);
						LogService.getRoot().log(Level.WARNING,
								I18N.getMessage(LogService.getRoot().getResourceBundle(), 
								"com.rapidminer.gui.tools.dialogs.ManageDatabaseDriversDialog.scanning_jar_file_error", 
								file, e.getMessage()),
								e);

					}
				} catch (PrivilegedActionException e) {
					throw new RuntimeException("Cannot create class loader for file '" + file + "': " + e.getMessage(), e);
				}
			}
		}.startAndWait();
		return driverNames;
	}
}
