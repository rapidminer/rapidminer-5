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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.AbstractButton;

import com.rapidminer.gui.tour.Step;

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

public class BubbleToButton extends BubbleWindow {

	private static final long serialVersionUID = 8601169454504964237L;

	private ActionListener buttonListener;
	private AbstractButton button = null;
	private String buttonKey;

	/**
	 * @param owner the {@link Window} on which this {@link BubbleWindow} should be shown.
	 * @param preferredAlignment offer for alignment but the Class will calculate by itself whether the position is usable
	 * @param i18nKey of the message which should be shown
	 * @param buttonKeyToAttach i18nKey of the Button to which this {@link BubbleWindow} should be placed relative to. 
	 */
	public BubbleToButton(Window owner, String nextDockableKey, AlignedSide preferredAlignment, String i18nKey, String buttonKeyToAttach, Object... arguments) {
		this(owner, nextDockableKey, preferredAlignment, i18nKey, buttonKeyToAttach, true, arguments);
	}

	/**
	 * @param owner the {@link Window} on which this {@link BubbleWindow} should be shown.
	 * @param preferredAlignment offer for alignment but the Class will calculate by itself whether the position is usable
	 * @param i18nKey of the message which should be shown
	 * @param buttonKeyToAttach i18nKey of the Button to which this {@link BubbleWindow} should be placed relative to. 
	 * @param addListener indicates whether the {@link BubbleWindow} closes if the Button was pressed or when another Listener added by a subclass of {@link Step} is fired.
	 */
	public BubbleToButton(Window owner, String nextDockableKey, AlignedSide preferredAlignment, String i18nKey, String buttonKeyToAttach, boolean addListener, Object... arguments) {
		this(owner, nextDockableKey, preferredAlignment, i18nKey, buttonKeyToAttach, addListener, true, arguments);
	}
	
	/**
	 * @param owner the {@link Window} on which this {@link BubbleWindow} should be shown.
	 * @param preferredAlignment offer for alignment but the Class will calculate by itself whether the position is usable
	 * @param i18nKey of the message which should be shown
	 * @param buttonKeyToAttach i18nKey of the Button to which this {@link BubbleWindow} should be placed relative to. 
	 * @param addListener indicates whether the {@link BubbleWindow} closes if the Button was pressed or when another Listener added by a subclass of {@link Step} is fired.
	 * @param listenToPerspective if true the {@link BubbleWindow} is only in one Perspective viewable else the Bubble will keep their position doesn't matter whether the perspective changes 
	 */
	public BubbleToButton(Window owner, String nextDockableKey, AlignedSide preferredAlignment, String i18nKey, String buttonKeyToAttach, boolean addListener, boolean listenToPerspective, Object... arguments) {
		super(owner, preferredAlignment, i18nKey, nextDockableKey, arguments);
		if (preferredAlignment != AlignedSide.MIDDLE) {
			//Bubble will be bind to a Component
			this.buttonKey = buttonKeyToAttach;
			if (buttonKey == null || buttonKey.equals("")) {
				throw new IllegalArgumentException("key of the Button can not be null if the Alignment is not MIDDLE");
			} else {
				this.button = BubbleWindow.findButton(buttonKey, owner);
				if (addListener) {
					this.addListenerToButton(button);
				}
			}
		}
		setAddPerspectiveListener(listenToPerspective);
		super.paint(false);
	}

	/** Positions the window such that the pointer points to the given button. 
	 *  In addition to that, adds an {@link ActionListener} to the button which
	 *  closes the BubbleWindow as soon as the button is pressed and one that 
	 *  makes sure that the pointer always points at the right position. */
	private void addListenerToButton(AbstractButton button) {
		buttonListener = new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				BubbleToButton.this.dispose();
				fireEventActionPerformed();
				unregisterListenerFromButton();
			}
		};
		button.addActionListener(buttonListener);

	}

	private void unregisterListenerFromButton() {
		button.removeActionListener(buttonListener);
	}

	private void rearrangeButton() {
		AbstractButton localButton = BubbleWindow.findButton(buttonKey, owner);
		if (!(this.button.equals(localButton))) {
			this.unregisterListenerFromButton();
			this.addListenerToButton(localButton);
		}
	}

	@Override
	protected void registerMovementListener() {
		super.registerMovementListener();
		this.registerSpecificListener();
		
	}

	@Override
	protected void unregisterMovementListener() {
		super.unregisterMovementListener();
		this.unregisterSpecificListeners();
	}

	@Override
	protected Point getObjectLocation() {
		return button.getLocationOnScreen();
	}

	@Override
	protected int getObjectWidth() {
		return button.getWidth();
	}

	@Override
	protected int getObjectHeight() {
		return button.getHeight();
	}

	@Override
	protected void reloadComponent() {
		super.reloadComponent();
		this.rearrangeButton();
	}

	@Override
	protected void unregisterSpecificListeners() {

	}

	@Override
	protected void registerSpecificListener() {
	
	}
	
	
}
