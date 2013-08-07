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
import com.rapidminer.ProcessSetupListener;
import com.rapidminer.gui.RapidMinerGUI;
import com.rapidminer.gui.properties.OperatorPropertyPanel;
import com.rapidminer.gui.tools.components.BubbleToButton;
import com.rapidminer.gui.tools.components.BubbleToDockable;
import com.rapidminer.gui.tools.components.BubbleToOperator;
import com.rapidminer.gui.tools.components.BubbleWindow;
import com.rapidminer.gui.tools.components.BubbleWindow.AlignedSide;
import com.rapidminer.gui.tour.AddBreakpointStep.Position;
import com.rapidminer.operator.ExecutionUnit;
import com.rapidminer.operator.Operator;

/**
 * This subclass of {@link Step} will open a {@link BubbleWindow} which closes if a breakpoint on the given {@link Operator} is removed.
 * @author Kersting and Thilo Kamradt
 *
 */

public class RemoveBreakpointStep extends Step {
	
	private AlignedSide alignment;
	private Window owner = RapidMinerGUI.getMainFrame();
	private String i18nKey;
	private BubbleTo element;
	private Class<? extends Operator> operatorClass;
	private String attachToKey = "breakpoint_after";
	private Position positionOnOperator = Position.DONT_CARE;
	private ProcessSetupListener listener = null;
	private String dockableKey = OperatorPropertyPanel.PROPERTY_EDITOR_DOCK_KEY;
	

	/** chooses the breakpoint_after-Button as default shoould be used to align to the breakpoint-button or the operator
	 * @param preferredAlignment offer for alignment but the Class will calculate by itself whether the position is usable.
	 * @param owner the {@link Window} on which the {@link BubbleWindow} should be shown.
	 * @param i18nKey of the message which will be shown in the {@link BubbleWindow}.
	 * @param operatorClass the Class or Superclass of the {@link Operator} to which the breakpoint should be added.
	 * @param position indicates whether the Step listens to a breakpoint before, after or any breakpoint which will be removed from the given operatorClass.
	 */
	public RemoveBreakpointStep(BubbleTo element, AlignedSide preferredAlignment, String i18nKey, Class<? extends Operator> operatorClass, Position position) {
		super();
		this.alignment = preferredAlignment;
		this.i18nKey = i18nKey;
		this.operatorClass = operatorClass;
		this.positionOnOperator = position;
		this.element = element;
	}
	
	/** should be used to attach the Bubble to a button not the breakpoint-button
	 * @param preferredAlignment offer for alignment but the Class will calculate by itself whether the position is usable.
	 * @param owner the {@link Window} on which the {@link BubbleWindow} should be shown.
	 * @param i18nKey of the message which will be shown in the {@link BubbleWindow}.
	 * @param operatorClass the Class or Superclass of the {@link Operator} to which the breakpoint should be added.
	 * @param attachToKey i18nkey of the Button to which the {@link BubbleWindow} should point to.
	 * @param position indicates whether the Step listens to a breakpoint before, after or any breakpoint which will be removed from the given operatorClass.
	 */
	public RemoveBreakpointStep(AlignedSide preferredAlignment, String i18nKey, Class<? extends Operator> operatorClass, String attachToKey, Position position) {
		super();
		this.alignment = preferredAlignment;
		this.i18nKey = i18nKey;
		this.operatorClass = operatorClass;
		this.positionOnOperator = position;
		this.attachToKey = attachToKey;
	}
	
	/**
	 * @param preferredAlignment offer for alignment but the Class will calculate by itself whether the position is usable.
	 * @param owner the {@link Window} on which the {@link BubbleWindow} should be shown.
	 * @param i18nKey of the message which will be shown in the {@link BubbleWindow}.
	 * @param operatorClass the Class or Superclass of the {@link Operator} to which the breakpoint should be added.
	 * @param attachTo {@link Component} to which the {@link BubbleWindow} should point to.
	 * @param position indicates whether the Step listens to a breakpoint before, after or any breakpoint which will be removed from the given operatorClass.
	 */
	public RemoveBreakpointStep(AlignedSide preferredAlignment, String i18nKey, Class<? extends Operator> operatorClass, String attachToKey, Position position, Window owner) {
		
	}
	

	@Override
	boolean createBubble() {
		switch(element) {
			case BUTTON:
				bubble = new BubbleToButton(owner, dockableKey,alignment, i18nKey, attachToKey, false, new Object[] {});
				break;
			case DOCKABLE:
				bubble = new BubbleToDockable(owner, alignment, i18nKey, (attachToKey.equals("breakpoint_after") ? dockableKey : attachToKey), new Object[] {});
				break;
			case OPERATOR:
				bubble = new BubbleToOperator(owner, alignment, i18nKey, operatorClass, new Object[] {});
		}
		listener = new ProcessSetupListener() {
			
			@Override
			public void operatorRemoved(Operator operator, int oldIndex, int oldIndexAmongEnabled) {
				//don't care
				
			}
			
			@Override
			public void operatorChanged(Operator operator) {
				if (RemoveBreakpointStep.this.operatorClass.isInstance(operator) && !operator.hasBreakpoint()){
					bubble.triggerFire();
					RapidMinerGUI.getMainFrame().getProcess().removeProcessSetupListener(this);
				}
				if (RemoveBreakpointStep.this.operatorClass.isInstance(operator) && operator.hasBreakpoint()) {
					if (positionOnOperator == Position.BEFORE && !operator.hasBreakpoint(BreakpointListener.BREAKPOINT_BEFORE)) {
						bubble.triggerFire();
						RapidMinerGUI.getMainFrame().getProcess().removeProcessSetupListener(this);
					} else if (positionOnOperator == Position.AFTER && !operator.hasBreakpoint(BreakpointListener.BREAKPOINT_AFTER)) {
						bubble.triggerFire();
						RapidMinerGUI.getMainFrame().getProcess().removeProcessSetupListener(this);
					} 
				}
				
			}
			
			@Override
			public void operatorAdded(Operator operator) {
				//don't care
			}
			
			@Override
			public void executionOrderChanged(ExecutionUnit unit) {
				//don't care
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
		return new Step[] {new PerspectivesStep(1), new NotOnScreenStep(dockableKey), new NotViewableStep(alignment, owner, attachToKey, dockableKey)};
	}
}
