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
import com.rapidminer.gui.tools.components.BubbleToDockable;
import com.rapidminer.gui.tools.components.BubbleWindow;
import com.rapidminer.gui.tools.components.BubbleWindow.AlignedSide;
import com.vlsolutions.swing.docking.DockableState;
import com.vlsolutions.swing.docking.DockingContext;
import com.vlsolutions.swing.docking.event.DockableStateChangeEvent;
import com.vlsolutions.swing.docking.event.DockableStateChangeListener;


/**
 * 
 * 
 * @author Thilo Kamradt
 *
 */
public class NotOnScreenStep extends Step {
	
	private boolean showMe = false;
	private Window owner = RapidMinerGUI.getMainFrame();
	private String dockableKey;
	private String i18nKey = "lostDockable";
	private DockableStateChangeListener dockListener;
	private DockingContext context = RapidMinerGUI.getMainFrame().getDockableMenu().getDockingContext();
	
	public NotOnScreenStep(String dockableKey) {
		this.dockableKey = dockableKey;
	}
	
	/* (non-Javadoc)
	 * @see com.rapidminer.gui.tour.Step#createBubble()
	 */
	@Override
	boolean createBubble() {
		this.showMe = BubbleWindow.isDockableOnScreen(dockableKey) == -1;
		if(showMe) {
			bubble = new BubbleToDockable(owner, AlignedSide.MIDDLE, i18nKey, null, this.getDockableNameByKey(dockableKey));
			dockListener = new DockableStateChangeListener() {
				
				@Override
				public void dockableStateChanged(DockableStateChangeEvent changed) {
					if(changed.getNewState().getDockable().getDockKey().getKey().equals(dockableKey) && !changed.getNewState().isClosed()) {
						NotOnScreenStep.this.bubble.triggerFire();
						context.removeDockableStateChangeListener(dockListener);
						
					}
				}
			};
			
			context.addDockableStateChangeListener(dockListener);
		}
		return showMe;
	}

	/* (non-Javadoc)
	 * @see com.rapidminer.gui.tour.Step#stepCanceled()
	 */
	@Override
	protected void stepCanceled() {
		context.removeDockableStateChangeListener(dockListener);

	}

	/* (non-Javadoc)
	 * @see com.rapidminer.gui.tour.Step#checkPreconditions()
	 */
	@Override
	public Step[] getPreconditions() {
		return new Step[] {};
	}

	private String getDockableNameByKey(String key) {
		DockableState[] dockables = RapidMinerGUI.getMainFrame().getDockingDesktop().getDockables();
		for (DockableState state : dockables) {
			if(state.getDockable().getDockKey().getKey().equals(key))
				return state.getDockable().getDockKey().getName();
		}
		throw new IllegalArgumentException("Dockable with key: "+key+" does not exists.");
	}
}
