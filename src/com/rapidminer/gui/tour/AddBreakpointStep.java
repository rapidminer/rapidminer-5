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

import com.rapidminer.BreakpointListener;
import com.rapidminer.ProcessSetupListener;
import com.rapidminer.gui.RapidMinerGUI;
import com.rapidminer.gui.properties.OperatorPropertyPanel;
import com.rapidminer.gui.tools.components.BubbleToButton;
import com.rapidminer.gui.tools.components.BubbleToDockable;
import com.rapidminer.gui.tools.components.BubbleToOperator;
import com.rapidminer.gui.tools.components.BubbleWindow;
import com.rapidminer.gui.tools.components.BubbleWindow.AlignedSide;
import com.rapidminer.operator.ExecutionUnit;
import com.rapidminer.operator.Operator;

/**
 * 
 * This subclass of {@link Step} will open a {@link BubbleWindow} which closes if a Breakpoint is set.
 * 
 * @author Philipp Kersting and Thilo Kamradt
 *
 */

public class AddBreakpointStep extends Step {

	/**indicates on which position of a Breakpoint*/
	public enum Position {
		BEFORE, AFTER, DONT_CARE
	}

	private Class<? extends Operator> operator;
	private String i18nKey;
	private AlignedSide alignment;
	private Window owner = RapidMinerGUI.getMainFrame();
	private Position position;
	private String elementKey = "breakpoint_after";
	private ProcessSetupListener listener = null;
	private BubbleTo element;
	private String dockableKey = OperatorPropertyPanel.PROPERTY_EDITOR_DOCK_KEY;

	/**
	 * should be called if you want to align to the add-breakpoint-button or the operator or the operator-porperty-dockable
	 * The Bubble Window will be attached to the Button with the breakpoint_after key
	 * @param preferredAlignment offer for alignment but the Class will calculate by itself whether the position is usable.
	 * @param owner the {@link Window} on which the {@link BubbleWindow} should be shown.
	 * @param i18nKey of the message which will be shown in the {@link BubbleWindow}.
	 * @param operator the class of the Operator at which the Breakpoint should be added.
	 * @param position position on which the Breakpoint should be set.
	 */
	public AddBreakpointStep(BubbleTo element,AlignedSide preferredAlignment, String i18nKey, Class<? extends Operator> operator, Position position) {
		this.alignment = preferredAlignment;
		this.i18nKey = i18nKey;
		this.position = position;
		this.operator = operator;
		this.element = element; 
	}

	/**
	 * should be called if you want to align to another Button or another Dockable
	 * @param preferredAlignment offer for alignment but the Class will calculate by itself whether the position is usable.
	 * @param owner the {@link Window} on which the {@link BubbleWindow} should be shown.
	 * @param i18nKey of the message which will be shown in the {@link BubbleWindow}.
	 * @param operator the class of the Operator at which the Breakpoint should be added.
	 * @param i18nButtonKey key of the Button to which the {@link BubbleWindow} should be attached to.
	 * @param position position on which the Breakpoint should be set.
	 */
	public AddBreakpointStep(BubbleTo element, AlignedSide preferredAlignment, String i18nKey, Class<? extends Operator> operator, String elementKey, Position position) {
		this.alignment = preferredAlignment;
		this.i18nKey = i18nKey;
		this.position = position;
		this.operator = operator;
		this.elementKey = elementKey;
		this.element = element;
		if(element == BubbleTo.DOCKABLE)
			dockableKey = elementKey;
	}

	public AddBreakpointStep(BubbleTo element, AlignedSide preferredAlignment, String i18nKey, Class<? extends Operator> operator, String i18nButtonKey, Position position, Window owner) {
		this.alignment = preferredAlignment;
		this.i18nKey = i18nKey;
		this.position = position;
		this.operator = operator;
		this.elementKey = i18nButtonKey;
		this.owner = owner;
		this.element = element;
	}
	
	@Override
	boolean createBubble() {
			switch(element) {
				case BUTTON:
					bubble = new BubbleToButton(owner, dockableKey, alignment, i18nKey, elementKey, false, new Object[] {});
					break;
				case DOCKABLE:
					bubble = new BubbleToDockable(owner, alignment, i18nKey, dockableKey);
					break;
				case OPERATOR:
					bubble = new BubbleToOperator(owner, alignment, i18nKey, operator);
			}
			listener = new ProcessSetupListener() {

			@Override
			public void operatorRemoved(Operator operator, int oldIndex, int oldIndexAmongEnabled) {
				// do not care

			}

			@Override
			public void operatorChanged(Operator operator) {
				if (AddBreakpointStep.this.operator.isInstance(operator) && operator.hasBreakpoint()) {
					if (position == Position.BEFORE && operator.hasBreakpoint(BreakpointListener.BREAKPOINT_BEFORE)) {
						bubble.triggerFire();
						RapidMinerGUI.getMainFrame().getProcess().removeProcessSetupListener(this);
					} else if (position == Position.AFTER && operator.hasBreakpoint(BreakpointListener.BREAKPOINT_AFTER)) {
						bubble.triggerFire();
						RapidMinerGUI.getMainFrame().getProcess().removeProcessSetupListener(this);
					} else if (position == Position.DONT_CARE) {
						bubble.triggerFire();
						RapidMinerGUI.getMainFrame().getProcess().removeProcessSetupListener(this);
					}
				}

			}

			@Override
			public void operatorAdded(Operator operator) {
				// do not care

			}

			@Override
			public void executionOrderChanged(ExecutionUnit unit) {
				// do not care
			}
		};
				RapidMinerGUI.getMainFrame().getProcess().addProcessSetupListener(listener);
		return true;
	}
	
	@Override
	protected void stepCanceled() {
		if(listener != null)
			RapidMinerGUI.getMainFrame().getProcess().removeProcessSetupListener(listener);
	}

	@Override
	public Step[] getPreconditions() {
		return new Step[] {new PerspectivesStep(1), new NotOnScreenStep(dockableKey), new NotViewableStep(alignment, owner, elementKey, dockableKey)};
	}
}
