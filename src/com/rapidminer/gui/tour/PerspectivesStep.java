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
package com.rapidminer.gui.tour;

import java.awt.Window;

import com.rapidminer.gui.RapidMinerGUI;
import com.rapidminer.gui.tools.components.BubbleToButton;
import com.rapidminer.gui.tools.components.BubbleWindow.AlignedSide;


/**
 * This subclass of Step lets the user open a specific perspective.
 * @author Thilo Kamradt
 *
 */
public class PerspectivesStep extends Step {

	private String i18nKey, buttonKey;
	private Window owner = RapidMinerGUI.getMainFrame();
	private AlignedSide alignment = AlignedSide.BOTTOM;
	private boolean showMe;
	private int perspectiveIndex;
	
	/**
	 * @param perspective the perspective which should be shown at the end of the Step. (0 for WelcomePerspective, 1 for WorkingPerspective, 2 for ResultPerspective)
	 */
	public PerspectivesStep(int perspective) {
		if(perspective < 0 || perspective > 2)
			throw new IllegalArgumentException("the parameter perspective must be bigger than -1 and smaller than 3");
		this.perspectiveIndex = perspective;
		switch (perspective) {
			case 0:
				i18nKey = "changeToWelcome";
				buttonKey = "workspace_welcome";
				break;
			case 2:
				i18nKey = "changeToResult";
				buttonKey = "workspace_result";
				break;
			case 1:
			default:
				i18nKey = "changeToWork";
				buttonKey = "workspace_design";
		}
	}
	
	/**
	 * @see com.rapidminer.gui.tour.Step#createBubble()
	 */
	@Override
	boolean createBubble() {
		switch (perspectiveIndex) {
			case 0:
				showMe = !(RapidMinerGUI.getMainFrame().getPerspectives().getCurrentPerspective().getName().equals("welcome"));
				break;
			case 2:
				showMe = !(RapidMinerGUI.getMainFrame().getPerspectives().getCurrentPerspective().getName().equals("result"));
				break;
			case 1:
			default:
				showMe = !(RapidMinerGUI.getMainFrame().getPerspectives().getCurrentPerspective().getName().equals("design"));
		}
		if(showMe)
			bubble = new BubbleToButton(owner, null, alignment, i18nKey, buttonKey);
		return showMe;
	}

	@Override
	protected void stepCanceled() {
		//we don't need to do anything because the only have a ButtonListener which will be removed by the BubbleWindow
	}

	@Override
	public Step[] getPreconditions() {
		return new Step[] {};
	}

}
