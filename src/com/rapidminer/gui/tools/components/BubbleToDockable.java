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
package com.rapidminer.gui.tools.components;

import java.awt.Point;
import java.awt.Window;

/**
 * This class creates a speech bubble-shaped JDialog, which can be attache to
 * Buttons, either by using its ID or by passing a reference. 
 * The bubble triggers two events which are obserable by the {@link BubbleListener};
 * either if the close button was clicked, or if the corresponding button was used.
 * The keys for the title and the text must be of format gui.bubble.XXX.body or gui.bubble.XXX.title .
 * 
 * @author Thilo Kamradt
 *
 */

public class BubbleToDockable extends BubbleWindow {

	private static final long serialVersionUID = 3888050226315317727L;

	/**
	 * @param owner the {@link Window} on which this {@link BubbleWindow} should be shown.
	 * @param preferredAlignment offer for alignment but the Class will calculate by itself whether the position is usable.
	 * @param i18nKey of the message which should be shown
	 * @param docKey key of the Dockable to which this {@link BubbleWindow} should be placed relative to. 
	 * @param arguments arguments to pass thought to the I18N Object 
	 */
	public BubbleToDockable(Window owner, final AlignedSide preferredAlignment, String i18nKey, String docKey, Object... arguments) {
		super(owner, preferredAlignment, i18nKey, docKey, arguments);
		if(preferredAlignment != AlignedSide.MIDDLE) {
			if(docKey == null) {
				throw new IllegalArgumentException("key of Dockable can not be null if Alignment is not MIDDLE");
			}
		}
		super.paint(false);
	}

	@Override
	protected void registerMovementListener() {
		super.registerMovementListener();
		this.registerSpecificListener();
	}

	@Override
	protected void registerSpecificListener() {

	}
	
	@Override
	protected void unregisterMovementListener() {
		super.unregisterMovementListener();
		this.unregisterSpecificListeners();
	}
	
	@Override
	protected void unregisterSpecificListeners() {

	}

	@Override
	protected Point getObjectLocation() {
		if (dockable != null) {
			return dockable.getComponent().getLocationOnScreen();
		} else {
			return null;
		}
	}

	@Override
	protected int getObjectWidth() {
		if (dockable != null) {
			return dockable.getComponent().getWidth();
		} else {
			return 0;
		}
	}

	@Override
	protected int getObjectHeight() {
		if (dockable != null) {
			return dockable.getComponent().getHeight();
		} else {
			return 0;
		}
	}

	@Override
	protected void reloadComponent() {
		super.reloadComponent();
	}

}
