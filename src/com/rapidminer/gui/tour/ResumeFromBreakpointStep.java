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

import java.awt.Component;
import java.awt.Window;

import com.rapidminer.BreakpointListener;
import com.rapidminer.Process;
import com.rapidminer.gui.RapidMinerGUI;
import com.rapidminer.gui.tools.components.BubbleToButton;
import com.rapidminer.gui.tools.components.BubbleWindow;
import com.rapidminer.gui.tools.components.BubbleWindow.AlignedSide;
import com.rapidminer.operator.IOContainer;
import com.rapidminer.operator.Operator;

/**
 * This Subclass of {@link Step} will open a {@link BubbleWindow} when a Process reaches a breakpoint and closes if the user resume the Process.
 * 
 * @author Philipp Kersting and Thilo Kamradt
 *
 */

public class ResumeFromBreakpointStep extends Step {

	public enum Position {
		BEFORE, AFTER, DONT_CARE
	}

	private String i18nKey;
	private AlignedSide alignment;
	private Window owner = RapidMinerGUI.getMainFrame();
	private AddBreakpointStep.Position position;
	Class<? extends Operator> operatorClass;
	private String attachToKey = "run";
	private BreakpointListener listener = null;

	/**
	 * will attach to the run button in the head menu
	 * @param preferredAlignment offer for alignment but the Class will calculate by itself whether the position is usable.
	 * @param owner the {@link Window} on which the {@link BubbleWindow} should be shown.
	 * @param i18nKey of the message which will be shown in the {@link BubbleWindow}.
	 * @param operatorClass Class or Superclass of the {@link Operator} which owns the breakpoint.
	 * @param position indicates to which position of a breakpoint the {@link Step} listens.
	 * @param attachToKey i18nKey of the {@link Component} to which the {@link BubbleWindow} should point to.
	 */
	public ResumeFromBreakpointStep(AlignedSide preferredAlignment, String i18nKey, Class<? extends Operator> operatorClass, AddBreakpointStep.Position position) {
		this.alignment = preferredAlignment;
		this.i18nKey = i18nKey;
		this.operatorClass = operatorClass;
		this.position = position;
	}
	
	
	/**
	 * @param preferredAlignment offer for alignment but the Class will calculate by itself whether the position is usable.
	 * @param owner the {@link Window} on which the {@link BubbleWindow} should be shown.
	 * @param i18nKey of the message which will be shown in the {@link BubbleWindow}.
	 * @param operatorClass Class or Superclass of the {@link Operator} which owns the breakpoint.
	 * @param attachToKey i18nKey of the {@link Component} to which the {@link BubbleWindow} should point to.
	 */
	public ResumeFromBreakpointStep(AlignedSide preferredAlignment, String i18nKey, Class<? extends Operator> operatorClass, String attachToKey, AddBreakpointStep.Position position, Window owner) {
		this.alignment = preferredAlignment;
		this.i18nKey = i18nKey;
		this.operatorClass = operatorClass;
		this.position = position;
		this.owner = owner;
		
	}

	@Override
	boolean createBubble() {
		bubble = new BubbleToButton(owner, null, alignment, i18nKey, attachToKey, false, false, new Object[] {});
		listener = new BreakpointListener() {

			@Override
			public void resume() {
				if (operatorClass.isInstance(RapidMinerGUI.getMainFrame().getProcess().getCurrentOperator())) {
					if (position == AddBreakpointStep.Position.BEFORE && RapidMinerGUI.getMainFrame().getProcess().getCurrentOperator().hasBreakpoint(BreakpointListener.BREAKPOINT_BEFORE)) {
						bubble.triggerFire();
						RapidMinerGUI.getMainFrame().getProcess().removeBreakpointListener(this);
					} else if (position == AddBreakpointStep.Position.AFTER && RapidMinerGUI.getMainFrame().getProcess().getCurrentOperator().hasBreakpoint(BreakpointListener.BREAKPOINT_AFTER)) {
						bubble.triggerFire();
						RapidMinerGUI.getMainFrame().getProcess().removeBreakpointListener(this);
					} else if (position == AddBreakpointStep.Position.DONT_CARE) {
						bubble.triggerFire();
						RapidMinerGUI.getMainFrame().getProcess().removeBreakpointListener(this);
					}
				}

			}

			@Override
			public void breakpointReached(Process process, Operator op, IOContainer iocontainer, int location) {
				if (operatorClass.isInstance(op) && ((location == 1 && position == AddBreakpointStep.Position.AFTER) || (location == 0 && position == AddBreakpointStep.Position.BEFORE) || (position == AddBreakpointStep.Position.DONT_CARE))) {
					bubble.setVisible(true);
				}

			}
		};
		RapidMinerGUI.getMainFrame().getProcess().addBreakpointListener(listener);
		return true;
	}

	@Override
	protected void stepCanceled() {
		if (listener != null)
			RapidMinerGUI.getMainFrame().getProcess().removeBreakpointListener(listener);
	}

	@Override
	public Step[] getPreconditions() {
		return new Step[] {};
	}

}
