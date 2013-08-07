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
package com.rapidminer.gui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;

import com.rapidminer.ObjectVisualizer;
import com.rapidminer.gui.tools.ExtendedJScrollPane;
import com.rapidminer.gui.tools.ExtendedJTable;
import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.tools.LoggingHandler;
import com.rapidminer.tools.Tools;
import com.rapidminer.tools.jdbc.DatabaseHandler;

/**
 * Queries the database table for the row with the requested ID
 * and creates a generic example visualizer. This visualizer simply 
 * displays the attribute values of the example. Adding this operator 
 * is might be necessary to enable the visualization of single examples 
 * in the provided plotter or graph components. In contrast to the 
 * usual example visualizer, this version does not load the complete
 * data set into memory but simply queries the information from the
 * database and just shows the single row.
 * 
 * @author Ingo Mierswa
 */
public class DatabaseExampleVisualization implements ObjectVisualizer {
	
	private DatabaseHandler handler;
	
	private PreparedStatement statement;
	
	public DatabaseExampleVisualization(String databaseURL, String userName, String password, int databaseSystem, String tableName, String columnName, LoggingHandler logging) {		
		try {
			this.handler = DatabaseHandler.getConnectedDatabaseHandler(databaseURL, userName, password);

			String query = 
				"SELECT * FROM " + 
				handler.getStatementCreator().makeIdentifier(tableName) + 
				" WHERE " + 
				handler.getStatementCreator().makeIdentifier(columnName) + 
				" = ?";

			this.statement = this.handler.createPreparedStatement(query, false);
		} catch (OperatorException e) {
			logging.logError("Cannot connect to database: " + e.getMessage());
		} catch (SQLException e) {
			logging.logError("Cannot connect to database: " + e.getMessage());
		}
	}

	@Override
	public void finalize() throws Throwable {
		super.finalize();
		
		if (statement != null) {
			try {
				statement.close();
			} catch (SQLException e) {
				// do nothing
			}
		}
		if (handler != null) {
			try {
				handler.disconnect();
			} catch (SQLException e) {
				// do nothing
			}
		}
	}
	
	public void startVisualization(Object objId) {
		if ((handler == null) || (statement == null))
			return;
		
		try {
			statement.setObject(1, objId);
			ResultSet rs = statement.executeQuery();
			
			// show data
			final JDialog dialog = new JDialog(RapidMinerGUI.getMainFrame(), "Example: " + objId, false);
			dialog.getContentPane().setLayout(new BorderLayout());

	        if (rs != null) {
	        	boolean dataAvailable = rs.next();
	        	if (dataAvailable) {
	        		ResultSetMetaData metaData = rs.getMetaData();
	        		String[] columnNames = new String[] { "Attribute", "Value" };
	        		String[][] data = new String[metaData.getColumnCount()][2];
	        		for (int c = 1; c <= data.length; c++) {
	        			data[c - 1][0] = metaData.getColumnName(c);
	        			Object result = rs.getObject(c);
	        			String value = "?";
	        			if (result != null) {
	        				if (result instanceof Number) {
	        					value = Tools.formatIntegerIfPossible(((Number)result).doubleValue());
	        				} else {
	        					value = result.toString();
	        				}
	        			}
	        			data[c - 1][1] = value;
	        		}            
	        		JTable table = new ExtendedJTable();
	        		table.setDefaultEditor(Object.class, null);
	        		TableModel tableModel = new DefaultTableModel(data, columnNames);
	        		table.setModel(tableModel);
	        		JScrollPane scrollPane = new ExtendedJScrollPane(table);
	        		dialog.getContentPane().add(scrollPane, BorderLayout.CENTER);
	        	} else {
		            JLabel noInfoLabel = new JLabel("No information available for object '" + objId + "'.");
		            dialog.getContentPane().add(noInfoLabel, BorderLayout.CENTER);	        		
	        	}
	        } else {
	            JLabel noInfoLabel = new JLabel("No information available for object '" + objId + "'.");
	            dialog.getContentPane().add(noInfoLabel, BorderLayout.CENTER);
	        }

			JPanel buttons = new JPanel(new FlowLayout());
			JButton okButton = new JButton("Ok");
			okButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
				    dialog.dispose();
				}
			});
			buttons.add(okButton);
			dialog.getContentPane().add(buttons, BorderLayout.SOUTH);
			dialog.pack();
			dialog.setLocationRelativeTo(RapidMinerGUI.getMainFrame());
			dialog.setVisible(true);
			
			// clean up
			if (rs != null)
				rs.close();
		} catch (SQLException e) {
			SwingTools.showSimpleErrorMessage("cannot_retrieve_obj_inf", e, objId);
		}	
	}
	
	public String getDetailData(Object objId, String fieldName) { 		
		if ((handler == null) || (statement == null))
			return null;
		
		try {
			statement.setObject(1, objId);
			ResultSet rs = statement.executeQuery();
			
			String resultString = null;
			
	        if (rs != null) {
	        	boolean dataAvailable = rs.next();
	        	if (dataAvailable) {
	        		Object result = rs.getObject(fieldName);
	        		String value = "?";
	        		if (result != null) {
	        			if (result instanceof Number) {
	        				value = Tools.formatIntegerIfPossible(((Number)result).doubleValue());
	        			} else {
	        				value = result.toString();
	        			}
	        		}
	        		resultString = value;
	        	}
	        }
	        
	        // clean up
			if (rs != null)
				rs.close();
			
			return resultString;
		} catch (SQLException e) {
			return null;
		}
	}
	
	public String[] getFieldNames(Object objId) {
		if ((handler == null) || (statement == null))
			return new String[0];
		
		try {
			statement.setObject(1, objId);
			ResultSet rs = statement.executeQuery();
			
			// show data
			List<String> result = new LinkedList<String>();
	        if (rs != null) {
	        	boolean dataAvailable = rs.next();
	        	if (dataAvailable) {
	        		ResultSetMetaData metaData = rs.getMetaData();
	        		for (int c = 1; c <= metaData.getColumnCount(); c++) {
	        			result.add(metaData.getColumnName(c));
	        		}            
	 
	        	}
	        }
			
			// clean up
			if (rs != null)
				rs.close();
			
			String[] resultArray = new String[result.size()];
			result.toArray(resultArray);
			return resultArray;
		} catch (SQLException e) {
			return new String[0];
		}	
	}
	
	public String getTitle(Object objId) {
		return (objId instanceof String) ? (String)objId : ((Double)objId).toString();
	}

	public boolean isCapableToVisualize(Object id) {
		return true;
	}

	public void stopVisualization(Object objId) {}
}
