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
package com.rapidminer.gui.actions;

import java.awt.event.ActionEvent;

import javax.swing.JScrollPane;

import com.rapidminer.gui.tools.JDBCDriverTable;
import com.rapidminer.gui.tools.ResourceAction;
import com.rapidminer.gui.tools.dialogs.ButtonDialog;
import com.rapidminer.tools.jdbc.DatabaseService;
import com.rapidminer.tools.jdbc.DriverInfo;


/**
 * Start the corresponding action.
 * 
 * @author Ingo Mierswa
 */
public class CheckForJDBCDriversAction extends ResourceAction {

    private static final long serialVersionUID = -3497263063489866721L;
	
    public CheckForJDBCDriversAction() {
        super("show_database_drivers");
    }

    public void actionPerformed(ActionEvent e) {
    	final ButtonDialog dialog = new ButtonDialog("jdbc_drivers", true, new Object[]{}) {
			private static final long serialVersionUID = 8300330464175246257L;
			
			{
    			DriverInfo[] drivers = DatabaseService.getAllDriverInfos();
    			JDBCDriverTable driverTable = new JDBCDriverTable(drivers);
    			driverTable.setBorder(null);
    			JScrollPane driverTablePane = new JScrollPane(driverTable);
    			driverTablePane.setBorder(createBorder());
    			layoutDefault(driverTablePane, NORMAL, makeCloseButton());
			}				
    	};
        dialog.setVisible(true);
    }
}    
