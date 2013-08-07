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

import com.rapidminer.ProcessSetupListener;
import com.rapidminer.gui.RapidMinerGUI;
import com.rapidminer.gui.tools.components.BubbleToDockable;
import com.rapidminer.gui.tools.components.BubbleWindow;
import com.rapidminer.gui.tools.components.BubbleWindow.AlignedSide;
import com.rapidminer.operator.ExecutionUnit;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorChain;

/**
 * This Subclass of {@link Step} will open a {@link BubbleWindow} which closes if the given {@link Operator} was dragged to the Process and is wired.
 *  
 * @author Philipp Kersting and Thilo Kamradt
 *
 */

public class AddOperatorStep extends Step {

	public interface AddOperatorStepListener {

		public void operatorAvailable(Operator op, Step callingStep);
	}

	private String i18nKey;
	private AlignedSide alignment;
	private Window owner = RapidMinerGUI.getMainFrame();
	private Class operatorClass;
	private String targetDockKey;
	private boolean checkForChain = true;
	private Class<? extends OperatorChain> targetEnclosingOperatorChain = OperatorChain.class;
	private ProcessSetupListener listener = null;
	
	/**
	 * @param preferredAlignment offer for alignment but the Class will calculate by itself whether the position is usable.
	 * @param owner the {@link Window} on which the {@link BubbleWindow} should be shown.
	 * @param i18nKey of the message which will be shown in the {@link BubbleWindow}.
	 * @param operatorClass the Class or Superclass of the Operator which should be added to the Process.
	 * @param targetDockKey the i18nKey of the Dockable to which we bubble should point to.
	 */
	public AddOperatorStep(AlignedSide preferredAlignment, String i18nKey, Class<? extends Operator> operatorClass, String targetDockKey) {
		this.alignment = preferredAlignment;
		this.i18nKey = i18nKey;
		this.operatorClass = operatorClass;
		this.targetDockKey = targetDockKey;
	}

	/**
	 * @param preferredAlignment offer for alignment but the Class will calculate by itself whether the position is usable.
	 * @param owner the {@link Window} on which the {@link BubbleWindow} should be shown.
	 * @param i18nKey of the message which will be shown in the {@link BubbleWindow}.
	 * @param operatorClass the Class or Superclass of the Operator which should be added to the Process.
	 * @param targetDockKey the i18nKey of the Dockable to which we bubble should point to.
	 * @param checkForEnclosingOperatorChain indicates whether the {@link BubbleWindow} closes only if the Operator is also wired or not
	 */
	public AddOperatorStep(AlignedSide preferredAlignment, String i18nKey, Class<? extends Operator> operatorClass, String targetDockKey, boolean checkForEnclosingOperatorChain) {
		this.alignment = preferredAlignment;
		this.i18nKey = i18nKey;
		this.operatorClass = operatorClass;
		this.targetDockKey = targetDockKey;
		this.checkForChain = checkForEnclosingOperatorChain;
	}

	/**
	 * @param i18nKey of the message which will be shown in the {@link BubbleWindow}.
	 * @param preferredAlignment offer for alignment but the Class will calculate by itself whether the position is usable.
	 * @param owner the {@link Window} on which the {@link BubbleWindow} should be shown.
	 * @param operatorClass the Class or Superclass of the Operator which should be added to the Process.
	 * @param targetDockKey the i18nKey of the dockable to which we bubble should point to.
	 * @param targetEnclosingOperatorChain target OperatorChain
	 */
	public AddOperatorStep(AlignedSide preferredAlignment, String i18nKey, Class<? extends Operator> operatorClass, String targetDockKey, Class<? extends OperatorChain> targetEnclosingOperatorChain) {
		this.i18nKey = i18nKey;
		this.alignment = preferredAlignment;
		this.operatorClass = operatorClass;
		this.targetDockKey = targetDockKey;
		this.targetEnclosingOperatorChain = targetEnclosingOperatorChain;
	}

	@Override
	boolean createBubble() {
		bubble = new BubbleToDockable(owner, alignment, i18nKey, targetDockKey);
		listener = new ProcessSetupListener() {

			@Override
			public void operatorRemoved(Operator operator, int oldIndex, int oldIndexAmongEnabled) {}

			@Override
			public void operatorChanged(Operator operator) {
				if (operatorClass.isInstance(operator)) {
					if (checkForChain) {
						if ((targetEnclosingOperatorChain == null || targetEnclosingOperatorChain.isInstance(operator.getExecutionUnit().getEnclosingOperator())) && (operator.getOutputPorts().getNumberOfConnectedPorts() != 0)) {

							bubble.triggerFire();
							RapidMinerGUI.getMainFrame().getProcess().removeProcessSetupListener(this);
						}
					} else {
						if (operator.getOutputPorts().getNumberOfConnectedPorts() != 0) {

							bubble.triggerFire();
							RapidMinerGUI.getMainFrame().getProcess().removeProcessSetupListener(this);
						}
					}
				}
			}

			@Override
			public void operatorAdded(Operator operator) {}

			@Override
			public void executionOrderChanged(ExecutionUnit unit) {}
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
		return new Step[] {new PerspectivesStep(1), new NotOnScreenStep(targetDockKey)};
	}

}
