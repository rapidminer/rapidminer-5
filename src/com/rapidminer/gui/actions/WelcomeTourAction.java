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
import java.util.LinkedList;

import javax.swing.AbstractAction;
import javax.swing.Icon;

import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.gui.tools.dialogs.ConfirmDialog;
import com.rapidminer.gui.tour.IntroductoryTour;
import com.rapidminer.gui.tour.IntroductoryTour.TourListener;
import com.rapidminer.gui.tour.TourChooser;
import com.rapidminer.gui.tour.TourManager;
import com.rapidminer.gui.tour.TourState;
import com.rapidminer.tools.I18N;
import com.rapidminer.tools.ParameterService;

/**
 * Start the corresponding action.
 * 
 * @author Marco Boeck
 */
public class WelcomeTourAction extends AbstractAction {

	private static final long serialVersionUID = 1L;

	private static Icon icon = null;

	private TourManager tourManager;

	private LinkedList<String> newTours;

	static {
		icon = SwingTools.createIcon("48/" + I18N.getMessage(I18N.getGUIBundle(), "gui.action.welcome.tour.icon"));
	}

	public WelcomeTourAction() {
		super(I18N.getMessage(I18N.getGUIBundle(), "gui.action.welcome.tour.label"), icon);
		putValue(SHORT_DESCRIPTION, I18N.getMessage(I18N.getGUIBundle(), "gui.action.welcome.tour.tip"));
		tourManager = TourManager.getInstance();
	}

	public void actionPerformed(ActionEvent e) {
		new TourChooser().setVisible(true);
	}

	/**
	 * This Method will check if any Tour is new or wasn't performed until now and will ask the user whether he wants to perform the Tour right now.
	 */
	public void checkTours() {
		// look for the new tours
		String[] keys = tourManager.getTourkeys();
		newTours = new LinkedList<String>();
		for (int i = 0; i < keys.length; i++) {
			if (tourManager.getTourState(keys[i]).equals(TourState.NEW_ONE) && tourManager.getAskState(keys[i])) {
				newTours.add(keys[i]);
			}
		}
		//start asking for the execution of the tours
		this.startNext();
	}

	/**
	 * Starts the next Tour in the tourList
	 */
	private void startNext() {
		if (!newTours.isEmpty()) {
			String currentTourKey = newTours.remove();
			String propertyKey = "DontAskAgainTourChoosen";
			ParameterService.setParameterValue(propertyKey, "null");
			int returnValue = ConfirmDialog.showConfirmDialogWithOptionalCheckbox("new_tour_found", ConfirmDialog.YES_NO_OPTION, propertyKey, ConfirmDialog.NO_OPTION, true, currentTourKey);
			// save whether we will ask again
			if (!Boolean.parseBoolean(ParameterService.getParameterValue(propertyKey))) {
				tourManager.setTourState(currentTourKey, TourState.NEVER_ASK);
			}
			//handle returnValue
			if (returnValue == ConfirmDialog.YES_OPTION) {
				IntroductoryTour currentTour = tourManager.get(currentTourKey);
				currentTour.addListener(new TourListener() {

					@Override
					public void tourClosed() {
						WelcomeTourAction.this.startNext();
					}
				});
				currentTour.startTour();
			}
			if (returnValue == ConfirmDialog.NO_OPTION) {
				this.startNext();
			}

		}
	}
}
