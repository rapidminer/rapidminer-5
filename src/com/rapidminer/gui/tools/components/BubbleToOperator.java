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
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JPopupMenu;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import com.rapidminer.gui.RapidMinerGUI;
import com.rapidminer.gui.flow.ProcessInteractionListener;
import com.rapidminer.gui.flow.ProcessPanel;
import com.rapidminer.gui.flow.ProcessRenderer;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorChain;
import com.rapidminer.operator.ports.Port;
import com.vlsolutions.swing.docking.event.DockingActionEvent;
import com.vlsolutions.swing.docking.event.DockingActionListener;

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

public class BubbleToOperator extends BubbleWindow {

	private static final long serialVersionUID = 7404582361212798730L;

	private int split;

	private Class<? extends Operator> operatorClass;
	private Operator onDisplay = null;
	private OperatorChain homeChain = null;
	private BubbleToDockable dockBubble = null;
	
	private ProcessRenderer renderer = RapidMinerGUI.getMainFrame().getProcessPanel().getProcessRenderer();
	private ProcessInteractionListener rendererListener;
	private ChangeListener viewPortListener;
	private DockingActionListener assistanceDockingListener;
	private ProcessInteractionListener assistanceRendererListener;

	
	public BubbleToOperator(Window owner, AlignedSide preferredAlignment, String i18nKey, Class<? extends Operator> toAttach, Object ... arguments) {
		this(owner, preferredAlignment, i18nKey, toAttach, 1, arguments);
	}
	
	public BubbleToOperator(Window owner, AlignedSide preferredAlignment, String i18nKey, Class<? extends Operator> toAttach, int split, Object ... arguments) {
		super(owner, preferredAlignment, i18nKey, ProcessPanel.PROCESS_PANEL_DOCK_KEY, arguments);
		operatorClass = toAttach;
		this.split = split;
		Operator[] matchingOperators = this.getMatchingOperatorsInChain(operatorClass, renderer.getDisplayedChain());
		if(matchingOperators.length == 0) {
			//TODO add information for user to enter the wanted chain with operator
			this.registerMovementListener();
			this.changeToAssistanceListener();
			this.getBubbleToProcesspanel().setVisible(true);
		} else {
			onDisplay = matchingOperators[(matchingOperators.length <= split ? matchingOperators.length - 1 : split - 1)];
			homeChain = renderer.getDisplayedChain();
			renderer.scrollRectToVisible(renderer.getOperatorRect(onDisplay, false).getBounds());
		}
		super.paint(false);
	}
	
	private Operator[] getMatchingOperatorsInChain(Class<? extends Operator> operatorClass,OperatorChain displayedChain) {
		ArrayList<Operator> matching = new ArrayList<Operator>();
		List<Operator> operatorsInChain = displayedChain.getAllInnerOperators();
		for(Operator operatorInChain : operatorsInChain) {
			if(operatorClass.isAssignableFrom(operatorInChain.getClass())) {
				matching.add(operatorInChain);
			}
		}
		return matching.toArray(new Operator[0]);
	}
	
	
	@Override
	protected void registerMovementListener() {
		super.registerMovementListener();
		this.registerSpecificListener();
	}

	@Override
	protected void registerSpecificListener() {
		viewPortListener = new ChangeListener() {
			
			@Override
			public void stateChanged(ChangeEvent e) {
				BubbleToOperator.this.paint(false);
			}
		};
		RapidMinerGUI.getMainFrame().getProcessPanel().getViewPort().addChangeListener(viewPortListener);
		rendererListener = new ProcessInteractionListener() {
			
			@Override
			public void portContextMenuWillOpen(JPopupMenu menu, Port port) {
				// do not care
			}
			
			@Override
			public void operatorMoved(Operator op) {
				if(op.equals(BubbleToOperator.this.onDisplay))
					BubbleToOperator.this.paint(false);
			}
			
			@Override
			public void operatorContextMenuWillOpen(JPopupMenu menu, Operator operator) {
				// do not care
				
			}
			
			@Override
			public void displayedChainChanged(OperatorChain displayedChain) {
				if(onDisplay == null) {
					//TODO outline because unreachable ...
					Operator[] matching = BubbleToOperator.this.getMatchingOperatorsInChain(operatorClass, displayedChain);
					if (matching.length != 0) {
						onDisplay = matching[(matching.length <= split ? matching.length - 1 : split - 1)];
						homeChain = renderer.getDisplayedChain();
						if(dockBubble != null) {
							triggerfireForBubbleToDockable();
						}
					}
				} else if (homeChain.equals(displayedChain)) {
						//will be handled by the the assistance listener
				} else {
					BubbleToOperator.this.setVisible(false);
					BubbleToOperator.this.unregisterMovementListener();
					BubbleToOperator.this.changeToAssistanceListener();
					getBubbleToProcesspanel().setVisible(true);
				}
			}
		};
		renderer.addProcessInteractionListener(rendererListener);
	}
	
	private void changeToAssistanceListener() {
		this.unregisterMovementListener();
		assistanceRendererListener = new ProcessInteractionListener() {
			
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
				if(onDisplay == null) {
					Operator[] matching = BubbleToOperator.this.getMatchingOperatorsInChain(operatorClass, displayedChain);
					if (matching.length != 0) {
						onDisplay = matching[(matching.length <= split ? matching.length - 1 : split - 1)];
						homeChain = renderer.getDisplayedChain();
						triggerfireForBubbleToDockable();
						BubbleToOperator.this.changeToMainListeners();
					}
				} else if (homeChain.equals(displayedChain)) {
					BubbleToOperator.this.paint(true);
					BubbleToOperator.this.setVisible(true);
					triggerfireForBubbleToDockable();
					BubbleToOperator.this.changeToMainListeners();
					//TODO: check this for mistakes
				}
			}
		};
		renderer.addProcessInteractionListener(assistanceRendererListener);
		if(dockable != null) {
			assistanceDockingListener = new DockingActionListener() {

				int dockingCounter = 0;
				@Override
				public void dockingActionPerformed(DockingActionEvent event) {
					// actionType 2 indicates that a Dockable was splitted
					// actionType 3 indicates that the Dockable has created his own position
					// actionType 5 indicates that the Dockable was docked to another position
					// actionType 6 indicates that the Dockable was separated
					if (event.getActionType() == 5 || event.getActionType() == 3) {
						if ((++dockingCounter) % 2 == 0) {
							//get the new component of the Dockable because the current component is disabled
							BubbleToOperator.this.reloadComponent(true);
						}
					}
					if (event.getActionType() == 6 || event.getActionType() == 2) {
						//get the new component of the Dockable because the current component is disabled
						BubbleToOperator.this.reloadComponent(true);
					}
				}

				@Override
				public boolean acceptDockingAction(DockingActionEvent arg0) {
					// no need to deny anything
					return true;
				}
			};
			desktop.addDockingActionListener(assistanceDockingListener);
		}
	}
	
	
	private void changeToMainListeners() {
		if(dockable == null) {
			//remove assistance listener
			renderer.removeProcessInteractionListener(assistanceRendererListener);
		} else {
			//remove assistance listeners
			desktop.removeDockingActionListener(assistanceDockingListener);
			renderer.removeProcessInteractionListener(assistanceRendererListener);
		}
		//add the correct listeners again
		this.registerMovementListener();
	}
	
	@Override
	protected void unregisterMovementListener() {
		super.unregisterMovementListener();
		this.unregisterSpecificListeners();
	}

	@Override
	protected void unregisterSpecificListeners() {
		renderer.removeProcessInteractionListener(rendererListener);
		RapidMinerGUI.getMainFrame().getProcessPanel().getViewPort().removeChangeListener(viewPortListener);
	}

	@Override
	protected Point getObjectLocation() {
		//get all necessary parameters
		
		int xDockable = dockable.getComponent().getLocationOnScreen().x;
		int yDockable = dockable.getComponent().getLocationOnScreen().y;
		Rectangle2D rec = renderer.getOperatorRect(onDisplay, false);
		double width = rec.getWidth();
		double height = rec.getHeight();
		double xOperator = rec.getMinX();
		double yOperator = rec.getMinY();
		Point view = RapidMinerGUI.getMainFrame().getProcessPanel().getProcessRenderer().getVisibleRect().getLocation();
		
		return new Point((int) (xDockable + (width*0.3) +xOperator - view.x),(int) (yDockable + height*0.85 + yOperator - view.y));
		
	}
	
	@Override
	protected int getObjectWidth() {
		return (int) Math.round(renderer.getOperatorRect(onDisplay, false).getWidth());
	}

	@Override
	protected int getObjectHeight() {
		return (int) Math.round(renderer.getOperatorRect(onDisplay, false).getHeight());
	}

	/** returns the current instance of the BubbleToDockable Object or a new one*/
	private BubbleToDockable getBubbleToProcesspanel() {
		if(dockable == null) {
			dockBubble = new BubbleToDockable(owner, AlignedSide.RIGHT, "operatorNotDisplayed", super.docKey, operatorClass.getName());; 
		}
		return dockBubble;
	}
	
	/**disposes the BubbleToDockable object*/
	private void triggerfireForBubbleToDockable() {
		if(dockBubble != null)
			dockBubble.triggerFire();
		dockBubble = null;
	}
	
	protected void reloadComponent(boolean assistanceActive) {
		super.reloadComponent();
		if(assistanceActive && dockable != null){
			dockable.getComponent().removeComponentListener(compListener);
			super.removeComponentListener(compListener);
			desktop.removeDockingActionListener(dockListener);
			desktop.addDockingActionListener(assistanceDockingListener);
		}
	}
	
	@Override
	protected void reloadComponent() {
		super.reloadComponent();
//		dockable = BubbleWindow.getDockableByKey(docKey);
//		Operator[] matchingOperators = this.getMatchingOperatorsInChain(operatorClass, renderer.getDisplayedChain());
//		if(matchingOperators.length == 0) {
//			//TODO show dialog
//			dockBubble = new BubbleToDockable(owner, AlignedSide.RIGHT, "operatorNotDisplayed", docKey, operatorClass.getName());
//			dockBubble.setVisible(true);
//		} else if(matchingOperators.length <= split) {
//			onDisplay = matchingOperators[split - 1];
//		} else {
//			onDisplay = matchingOperators[matchingOperators.length - 1];
//		}
//	dockable = BubbleWindow.getDockableByKey(docKey);
	}

}

