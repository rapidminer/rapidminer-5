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
package com.rapidminer.gui.properties.celleditors.value;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.sql.SQLException;
import java.util.logging.Level;

import javax.swing.AbstractCellEditor;
import javax.swing.JButton;
import javax.swing.JTable;

import com.rapidminer.gui.properties.PropertyDialog;
import com.rapidminer.gui.tools.ProgressThread;
import com.rapidminer.gui.tools.ResourceAction;
import com.rapidminer.gui.tools.dialogs.SQLQueryBuilder;
import com.rapidminer.operator.Operator;
import com.rapidminer.parameter.ParameterTypeSQLQuery;
import com.rapidminer.parameter.UndefinedParameterError;
import com.rapidminer.tools.I18N;
import com.rapidminer.tools.LogService;
import com.rapidminer.tools.jdbc.DatabaseHandler;
import com.rapidminer.tools.jdbc.TableMetaDataCache;
import com.rapidminer.tools.jdbc.connection.ConnectionEntry;
import com.rapidminer.tools.jdbc.connection.ConnectionProvider;

/**
 * @author Tobias Malbrecht, Marco Boeck
 */
public class SQLQueryValueCellEditor extends AbstractCellEditor implements PropertyValueCellEditor {

	private static final long serialVersionUID = -771727412083431607L;

	private Operator operator;

	private final JButton button;

	private String sqlQuery;

	public SQLQueryValueCellEditor(final ParameterTypeSQLQuery type) {
		button = new JButton(new ResourceAction(true, "build_sql") {

			private static final long serialVersionUID = -2911499842513746414L;

			public void actionPerformed(ActionEvent e) {
				DatabaseHandler handler = null;
				try {
					if (operator instanceof ConnectionProvider) {
						ConnectionEntry entry = ((ConnectionProvider) operator).getConnectionEntry();
						handler = DatabaseHandler.getConnectedDatabaseHandler(entry);
					}
					//handler = DatabaseHandler.getConnectedDatabaseHandler(operator);
				} catch (Exception e2) {
					//LogService.getRoot().log(Level.WARNING, "Failed to connect to database: "+e2);
					LogService.getRoot().log(Level.WARNING, "com.rapidminer.gui.properties.celleditors.value.SQLQueryValueCellEditor.connecting_to_database_error", e2);
					// we can continue without a db handler
					handler = null;
				}
				try {
					final SQLQueryBuilder queryBuilder = new SQLQueryBuilder(handler);
					class SQLQueryPropertyDialog extends PropertyDialog {

						private static final long serialVersionUID = -5224113818406394872L;
						private JButton resizeButton;
						private JButton clearMetaDataCacheButton;

						private SQLQueryPropertyDialog(boolean editOnly) {
							super(type, "sql");

							ResourceAction resizeAction = new ResourceAction(true, "text_dialog.enlarge") {

								private static final long serialVersionUID = 8857840715142145951L;

								@Override
								public void actionPerformed(ActionEvent event) {

									JButton button = (JButton) event.getSource();
//									Point point = button.getLocation();

									final Point relativeLocation = button.getLocationOnScreen();

									GraphicsEnvironment e
									= GraphicsEnvironment.getLocalGraphicsEnvironment();

									GraphicsDevice[] devices = e.getScreenDevices();

									Rectangle displayBounds = null;
//									Rectangle virtualBounds = new Rectangle();

									//now get the configurations for each device
									for (GraphicsDevice device : devices) {

										GraphicsConfiguration[] configurations =
												device.getConfigurations();
										for (GraphicsConfiguration config : configurations) {
											Rectangle gcBounds = config.getBounds();

											if (gcBounds.contains(relativeLocation)) {
												displayBounds = gcBounds;
											}
										}
									}

									Dimension screenDim;
									if (displayBounds != null) {
										screenDim = new Dimension((int) displayBounds.getWidth(), (int) displayBounds.getHeight());
									}
									else {
										screenDim = Toolkit.getDefaultToolkit().getScreenSize();
									}

									Dimension dim = new Dimension((int) (screenDim.width * 0.9), (int) (screenDim.height * 0.9));
									Dimension currentSize = getSize();
									if (currentSize.getHeight() != dim.getHeight() && currentSize.getWidth() != dim.getWidth()) {
										setSize(dim);

										if (displayBounds != null) {
											int y = displayBounds.y + ((screenDim.height - dim.height)/2);
											int x = displayBounds.x + ((screenDim.width - dim.width)/2);
											setLocation(x,y);
										}
										else {
											setLocationRelativeTo(null);
										}

										resizeButton.setText(I18N.getMessage(I18N.getGUIBundle(), "gui.action.text_dialog.shrink.label"));
										resizeButton.setToolTipText(I18N.getMessage(I18N.getGUIBundle(), "gui.action.text_dialog.shrink.tip"));
										resizeButton.setMnemonic(I18N.getMessage(I18N.getGUIBundle(), "gui.action.text_dialog.shrink.mne").charAt(0));
									} else {
										setSize(getDefaultSize(NORMAL));
										setDefaultLocation();
										resizeButton.setText(I18N.getMessage(I18N.getGUIBundle(), "gui.action.text_dialog.enlarge.label"));
										resizeButton.setToolTipText(I18N.getMessage(I18N.getGUIBundle(), "gui.action.text_dialog.enlarge.tip"));
										resizeButton.setMnemonic(I18N.getMessage(I18N.getGUIBundle(), "gui.action.text_dialog.enlarge.mne").charAt(0));
									}
								}
							};

							resizeButton = new JButton(resizeAction);
							
							ResourceAction clearMetaDataCacheAction = new ResourceAction("clear_db_cache") {

								private static final long serialVersionUID = 8510147303889637712L;

								@Override
								public void actionPerformed(ActionEvent e) {
									ProgressThread t = new ProgressThread("db_clear_cache") {

										@Override
										public void run() {
											TableMetaDataCache.getInstance().clearCache();
											queryBuilder.updateAll();
										}
									};
									t.start();
								}
							};

							clearMetaDataCacheButton = new JButton(clearMetaDataCacheAction);

							layoutDefault(queryBuilder.makeQueryBuilderPanel(editOnly), NORMAL, clearMetaDataCacheButton, resizeButton, makeOkButton(), makeCancelButton());
						}
					}

					boolean connectionProvided = handler != null;

					SQLQueryPropertyDialog dialog = new SQLQueryPropertyDialog(!connectionProvided);
					if (operator != null) {
						String query = null;
						try {
							query = operator.getParameters().getParameter(type.getKey());
						} catch (UndefinedParameterError e1) {}
						if (query != null) {
							queryBuilder.setQuery(query);
						}
					}
					dialog.setVisible(true);
					if (dialog.isOk()) {
						sqlQuery = queryBuilder.getQuery();
						fireEditingStopped();
					} else {
						fireEditingCanceled();
					}
				} finally {
					try {
						if (handler != null) {
							handler.disconnect();
						}
					} catch (SQLException e2) {
						LogService.getRoot().log(Level.WARNING, "com.rapidminer.gui.properties.celleditors.value.SQLQueryValueCellEditor.disconnecting_from_database_error", e2);
					}
				}
			}
		});
		button.setMargin(new Insets(0, 0, 0, 0));

	}

	@Override
	public boolean rendersLabel() {
		return false;
	}

	@Override
	public boolean useEditorAsRenderer() {
		return true;
	}

	@Override
	public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
		return button;
	}

	@Override
	public Object getCellEditorValue() {
		return sqlQuery;
	}

	@Override
	public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
		return button;
	}

	@Override
	public void setOperator(Operator operator) {
		this.operator = operator;
	}
}
