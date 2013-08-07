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

import com.rapidminer.Process;
import com.rapidminer.ProcessStorageListener;
import com.rapidminer.gui.RapidMinerGUI;
import com.rapidminer.gui.tools.components.BubbleToButton;
import com.rapidminer.gui.tools.components.BubbleWindow;
import com.rapidminer.gui.tools.components.BubbleWindow.AlignedSide;

/**
 * This subclass of {@link Step} will open a {@link BubbleWindow} which closes if the user has opened a process.
 * 
 * @author Philipp Kersting and Thilo Kamradt
 *
 */

public class OpenProcessStep extends Step {

	private AlignedSide alignment;
	private Window owner = RapidMinerGUI.getMainFrame();
	private String i18nKey;
	private String attachToKey;
	private ProcessStorageListener listener = null;

	/**
	 * 
	 * @param preferedAlignment offer for alignment but the Class will calculate by itself whether the position is usable.
	 * @param owner the {@link Window} on which the {@link BubbleWindow} should be shown.
	 * @param i18nKey of the message which will be shown in the {@link BubbleWindow}.
	 * @param attachTo Component to which the {@link BubbleWindow} should point to.
	 */
	public OpenProcessStep(AlignedSide preferedAlignment, String i18nKey, String attachToKey) {
		this.alignment = preferedAlignment;
		this.i18nKey = i18nKey;
		this.attachToKey = attachToKey;
	}

	/**
	 * 
	 * @param preferedAlignment offer for alignment but the Class will calculate by itself whether the position is usable.
	 * @param owner the {@link Window} on which the {@link BubbleWindow} should be shown.
	 * @param i18nKey of the message which will be shown in the {@link BubbleWindow}.
	 * @param attachToKey key of the Component to which the {@link BubbleWindow} should point to.
	 */
	public OpenProcessStep(AlignedSide preferedAlignment, String i18nKey, String attachToKey, Window owner) {
		this.alignment = preferedAlignment;
		this.owner = owner;
		this.i18nKey = i18nKey;
		this.attachToKey = attachToKey;
	}

	@Override
	boolean createBubble() {
		if (attachToKey == null)
			throw new IllegalArgumentException("no component to attach !");
		bubble = new BubbleToButton(owner, null, alignment, i18nKey, attachToKey, false, new Object[] {});
		listener = new ProcessStorageListener() {
			@Override
			public void stored(Process process) { 
				
			}

			@Override
			public void opened(Process process) {				
				bubble.triggerFire();
				RapidMinerGUI.getMainFrame().removeProcessStorageListener(listener);
			}
		};
		RapidMinerGUI.getMainFrame().addProcessStorageListener(listener);
		return true;
	}
	
	@Override
	protected void stepCanceled() {
		if(listener != null)
			RapidMinerGUI.getMainFrame().getProcess().removeProcessStorageListener(listener);
	}

	@Override
	public Step[] getPreconditions() {
		return new Step[] {};
	}
}
