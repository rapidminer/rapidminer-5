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

import javax.swing.JPopupMenu;

import com.rapidminer.gui.RapidMinerGUI;
import com.rapidminer.gui.flow.ProcessInteractionListener;
import com.rapidminer.gui.flow.ProcessPanel;
import com.rapidminer.gui.tools.components.BubbleToDockable;
import com.rapidminer.gui.tools.components.BubbleToOperator;
import com.rapidminer.gui.tools.components.BubbleWindow;
import com.rapidminer.gui.tools.components.BubbleWindow.AlignedSide;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorChain;
import com.rapidminer.operator.ports.Port;

/**
 * This subclass of {@link Step} will be closed if the a Subprocess was opened.
 * 
 * @author Kersting and Thilo Kamradt
 *
 */

public class OpenSubprocessStep extends Step {

	private AlignedSide alignment;
	private Window owner = RapidMinerGUI.getMainFrame();
	private String i18nKey;
	private Class<? extends OperatorChain> operatorClass;
	private ProcessInteractionListener listener = null;
	private BubbleTo element;
	private String dockableKey = ProcessPanel.PROCESS_PANEL_DOCK_KEY;

	/**
	 * should be used to align to the operator which can be entered or the dockable 
	 * @param preferedAlignment offer for alignment but the Class will calculate by itself whether the position is usable.
	 * @param owner the {@link Window} on which the {@link BubbleWindow} should be shown.
	 * @param i18nKey of the message which will be shown in the {@link BubbleWindow}.
	 * @param docKey i18nKey of the component to which the {@link BubbleWindow} should be placed relative to.
	 * @param operator the class of the Operator which the user should enter.
	 */
	public OpenSubprocessStep(BubbleTo element, AlignedSide preferedAlignment, String i18nKey, Class<? extends OperatorChain> operator) {
		this.alignment = preferedAlignment;
		this.i18nKey = i18nKey;
		this.operatorClass = operator;
		this.element = element;
		if(element == BubbleTo.BUTTON)
			throw new IllegalArgumentException("can not align to a button for entering a subprocess");
	}

	/**
	 * @param preferedAlignment offer for alignment but the Class will calculate by itself whether the position is usable.
	 * @param owner the {@link Window} on which the {@link BubbleWindow} should be shown.
	 * @param i18nKey of the message which will be shown in the {@link BubbleWindow}.
	 * @param attachTo component to which the {@link BubbleWindow} should be placed relative to.
	 */
	public OpenSubprocessStep(BubbleTo element, AlignedSide preferedAlignment, String i18nKey, Class<? extends OperatorChain> operator, Window owner) {
		this.owner = owner;
		this.alignment = preferedAlignment;
		this.i18nKey = i18nKey;
		this.operatorClass = operator;
		this.element = element;
		if(element == BubbleTo.BUTTON)
			throw new IllegalArgumentException("can not align to a button for entering a subprocess");
	}

	@Override
	boolean createBubble() {
		switch(element) {
			case DOCKABLE:
				bubble = new BubbleToDockable(owner, alignment, i18nKey, dockableKey, new Object[] {});
				break;
			case OPERATOR:
				bubble = new BubbleToOperator(owner, alignment, i18nKey, operatorClass, new Object[] {});
				break;
			default:
				throw new IllegalArgumentException("can only align to Dockable or Operator");
		}
		
		listener = new ProcessInteractionListener() {
			
			@Override
			public void portContextMenuWillOpen(JPopupMenu menu, Port port) {
				// do not care
				
			}
			
			@Override
			public void operatorMoved(Operator op) {
				// do not care
				
			}
			
			@Override
			public void operatorContextMenuWillOpen(JPopupMenu menu, Operator operator) {
				// do not care
				
			}
			
			@Override
			public void displayedChainChanged(OperatorChain displayedChain) {
				if (displayedChain != null && (OpenSubprocessStep.this.operatorClass == null || displayedChain.getClass().equals(OpenSubprocessStep.this.operatorClass))) {
					bubble.triggerFire();
					RapidMinerGUI.getMainFrame().getProcessPanel().getProcessRenderer().removeProcessInteractionListener(this);
				}
			}
		};
		RapidMinerGUI.getMainFrame().getProcessPanel().getProcessRenderer().addProcessInteractionListener(listener);
		return true;
	}

	@Override
	protected void stepCanceled() {
		if (listener != null)
			RapidMinerGUI.getMainFrame().getProcessPanel().getProcessRenderer().removeProcessInteractionListener(listener);
	}

	@Override
	public Step[] getPreconditions() {
		return new Step[] { new PerspectivesStep(1), new NotOnScreenStep(dockableKey) };
	}
}
