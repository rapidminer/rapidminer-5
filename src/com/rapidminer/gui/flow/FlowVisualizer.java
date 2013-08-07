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
package com.rapidminer.gui.flow;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Stroke;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.swing.Action;
import javax.swing.JPopupMenu;
import javax.swing.JToggleButton;

import com.rapidminer.gui.actions.ToggleAction;
import com.rapidminer.gui.tools.ResourceAction;
import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.operator.ExecutionUnit;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.ports.InputPort;

/** This class lets the user view and edit the execution order of a process.
 * 
 * @author Simon Fischer
 *
 */
public class FlowVisualizer {

	private static final Stroke FLOW_STROKE = new BasicStroke(10f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
	private static final Font   FLOW_FONT   = new Font("Dialog", Font.BOLD, 18);
	private static final Stroke LINE_STROKE = new BasicStroke(1f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
	private static final Stroke HIGHLIGHT_STROKE = new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
	private static final Color  PASSIVE_COLOR = new Color(0, 0, 0, 50);
	private static final Color  FLOW_COLOR = new Color(SwingTools.RAPID_I_ORANGE.getRed(), 
			SwingTools.RAPID_I_ORANGE.getGreen(), 
			SwingTools.RAPID_I_ORANGE.getBlue(), 150);
	
//	protected JToggleButton SHOW_ORDER_TOGGLEBUTTON = new JToggleButton(new ResourceAction(true, "render_execution_order") {
//		private static final long serialVersionUID = 1L;
//		@Override
//		public void actionPerformed(ActionEvent e) {
//			setActive(SHOW_ORDER_TOGGLEBUTTON.isSelected());
//			processRenderer.repaint();
//		}		
//	});
	
	public final ToggleAction ALTER_EXECUTION_ORDER = new ToggleAction(true, "render_execution_order") {
		private static final long serialVersionUID = -8333670355512143502L;

		@Override
		public void actionToggled(ActionEvent e) {
			setActive(isSelected());
			processRenderer.repaint();
		}
		
	};
	
	protected JToggleButton SHOW_ORDER_TOGGLEBUTTON = ALTER_EXECUTION_ORDER.createToggleButton();

	public final Action SHOW_EXECUTION_ORDER = new ResourceAction("show_execution_order") {
		private static final long serialVersionUID = 3932329413268066576L;
		@Override
		public void actionPerformed(ActionEvent e) {
			setActive(true);
			processRenderer.repaint();
			StringBuilder b = new StringBuilder();			
			for (ExecutionUnit unit : processRenderer.getDisplayedChain().getSubprocesses()) {
				b.append("<strong>").append(unit.getName()).append("</strong><br/><ol>");
				for (Operator op : unit.topologicalSort()) {
					b.append("<li>").append(op.getName()).append("</li>");						
				}
				b.append("</ol>");
			}
			SwingTools.showLongMessage("execution_order_info", b.toString());
			setActive(ALTER_EXECUTION_ORDER.isSelected());
			processRenderer.repaint();
		}		
	};

	private final Action BRING_TO_FRONT = new ResourceAction("bring_operator_to_front") {
		private static final long serialVersionUID = 1L;
		@Override
		public void actionPerformed(ActionEvent e) {
			if (hoveringOperator != null) {
				hoveringOperator.getExecutionUnit().moveToIndex(hoveringOperator, 0);
			}
		}		
	};
	
	private boolean active = false;
	private final ProcessRenderer processRenderer;

	private Operator startOperator;
	private Operator endOperator;
	private Operator hoveringOperator;
	private Collection<Operator> dependentOps;	
	
	public FlowVisualizer(ProcessRenderer processRenderer2) {
		this.processRenderer = processRenderer2;
		installListeners();		
		SHOW_ORDER_TOGGLEBUTTON.setText(null);
	}

	private void installListeners() {
		processRenderer.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {				
				if (isActive()) {	
					if (showPopupMenu(e)) {
						return;
					}
					Operator op = findOperator(e.getPoint());
					switch (e.getButton()) {
					case MouseEvent.BUTTON1:
						if (startOperator == null) {
							if (op != startOperator) {
								startOperator = op;
								dependentOps = null;
								recomputeDependentOperators();
								processRenderer.repaint();
							}
						} else if (dependentOps != null) {							
							startOperator.getExecutionUnit().bringToFront(dependentOps, startOperator);
							startOperator = endOperator = null;
							dependentOps = null;
							processRenderer.repaint();
						}
						break;
					case MouseEvent.BUTTON3:
						startOperator = endOperator = null;
						dependentOps = null;
						processRenderer.repaint();
						break;
					} 
					//e.consume();
				}
			}

			@Override
			public void mouseClicked(MouseEvent e) {
				if (isActive()) {
					if (showPopupMenu(e)) {
						return;
					}
				}
			}

			@Override
			public void mouseReleased(MouseEvent e) {
				if (isActive()) {
					if (showPopupMenu(e)) {
						return;
					}
				}
			}
		});

		processRenderer.addMouseMotionListener(new MouseMotionAdapter() {
			@Override
			public void mouseMoved(MouseEvent e) {
				if (isActive()) {					
					hoveringOperator = findOperator(e.getPoint());
					if (startOperator != null) {
						if (hoveringOperator != startOperator) {
							endOperator = hoveringOperator;
							recomputeDependentOperators();
							processRenderer.repaint();						
						}
					}
					//e.consume();
				}				
			}

		});
	}

	public void setActive(boolean active) {
		this.active = active;
	}

	public boolean isActive() {
		return active;
	}

	public void render(Graphics2D g, ExecutionUnit process) {
		if (active) {
			// Re-Arrange operators
			List<Operator> operators = new LinkedList<Operator>(process.getOperators());
			if (dependentOps != null) {
				operators.removeAll(dependentOps);
				int insertionIndex = operators.indexOf(startOperator) + 1;
				for (Operator depOp : dependentOps) {
					operators.add(insertionIndex++, depOp);
				}
			}

			// they should be sorted already.
			//GeneralPath p = new GeneralPath();
			Point2D lastPoint = null;						
			g.setStroke(FLOW_STROKE);
			for (Operator op : operators) {
				if (!op.isEnabled()) continue;
				Rectangle2D r = processRenderer.getOperatorRect(op, true);
				
				if ((startOperator == null) || ((dependentOps != null) && dependentOps.contains(op))) {
					g.setColor(FLOW_COLOR);
				} else {
					g.setColor(PASSIVE_COLOR);
				}
				
				if (lastPoint != null) {
					g.draw(new Line2D.Double(lastPoint.getX(), lastPoint.getY(), r.getCenterX(), r.getCenterY()));
				}
				lastPoint = new Point2D.Double(r.getCenterX(), r.getCenterY());
			}			
			
			int i = 0;
			g.setStroke(LINE_STROKE);
			g.setFont(FLOW_FONT);
			boolean illegalStart = operators.indexOf(endOperator) < operators.indexOf(startOperator);
			for (Operator op : operators) {
				if (!op.isEnabled()) continue;
				i++;				
				Rectangle2D r = processRenderer.getOperatorRect(op, true);
				int size = 30;
				double y = (r.getMinY() + ProcessRenderer.HEADER_HEIGHT) + (r.getHeight() - ProcessRenderer.HEADER_HEIGHT - 10)/2;
				Ellipse2D circle = new Ellipse2D.Double(r.getCenterX() - size/2, y - size/2, size, size);

				// Fill circle
				if (illegalStart && (op == endOperator)) {
					g.setColor(Color.red);
				} else if ((op == startOperator) || (op == endOperator)) {
					g.setColor(SwingTools.LIGHT_BLUE);
				} else if ((dependentOps != null) && dependentOps.contains(op)) {
					g.setColor(SwingTools.LIGHT_BLUE);
				} else {
					g.setColor(Color.WHITE);
				}
				g.fill(circle);
				
				// Draw circle
				if ((op == hoveringOperator) || (startOperator == null) || (startOperator == op) || ((dependentOps != null) && dependentOps.contains(op))) {
					g.setColor(Color.BLACK);
				} else {
					g.setColor(Color.LIGHT_GRAY);
				}
				if (op == hoveringOperator) {
					g.setStroke(HIGHLIGHT_STROKE);
				} else {
					g.setStroke(LINE_STROKE);
				}
				g.draw(circle);
				
				String label = ""+i;
				Rectangle2D bounds = FLOW_FONT.getStringBounds(label, g.getFontRenderContext());
				g.drawString(label, (float)(r.getCenterX()-bounds.getWidth()/2), (float)(y - (bounds.getHeight())/2 - bounds.getY()));
			}
		}
	}

	private Collection<Operator> getDependingOperators(Operator enclosingOperator,
			int startIndex, int endIndex,
			List<Operator> topologicallySortedCandidates) {

		if (endIndex <= startIndex) {
			return Collections.emptyList();
		}

		Set<Operator> foundDependingOperators = new HashSet<Operator>();
		Set<Operator> completedOperators = new HashSet<Operator>();

		Operator stopWhenReaching = topologicallySortedCandidates.get(startIndex);

		foundDependingOperators.add(topologicallySortedCandidates.get(endIndex));

		for (int opIndex = endIndex; opIndex > startIndex; opIndex--) {
			Operator op = topologicallySortedCandidates.get(opIndex);

			// remember that we are already working on this one
			completedOperators.add(op);

			// Do we depend on that one? Otherwise, we can contine with the next.
			// (The startIndex-th operator is always in this set, so we actually start doing something.)
			if (!foundDependingOperators.contains(op)) {
				continue;
			}
			for (InputPort in : op.getInputPorts().getAllPorts()) {				
				if (in.isConnected()) {
					Operator predecessor = in.getSource().getPorts().getOwner().getOperator();
					// Skip if connected to inner sink
					if (predecessor == enclosingOperator) {
						continue;
					} else {						
						// Skip if working on it already
						if (completedOperators.contains(predecessor)) { 
							continue;
							// Skip when reaching end of the range
						} else if (predecessor == stopWhenReaching) { // did we reach the end?
							continue;
						} else {
							// Skip when beyond bounds
							int predecessorIndex = topologicallySortedCandidates.indexOf(predecessor);
							if (predecessorIndex <= startIndex) {
								continue;
							} else {
								// Otherwise, add to set of depending operators
								foundDependingOperators.add(predecessor);
							}
						}
					}
				}
			}
		}		

		List<Operator> orderedResult = new LinkedList<Operator>();
		for (Operator op : topologicallySortedCandidates) {
			if (foundDependingOperators.contains(op)) {
				orderedResult.add(op);
			}
		}
		return orderedResult;
	}

	public Operator findOperator(Point point) {
		int processIndex = processRenderer.getProcessIndexUnder(point);
		if (processIndex != -1) {
			Point mousePositionRelativeToProcess = processRenderer.toProcessSpace(point, processIndex);

			for (Operator op : processRenderer.getDisplayedChain().getSubprocess(processIndex).getOperators()) {
				Rectangle2D rect = processRenderer.getOperatorRect(op, true);					
				if (rect.contains(new Point2D.Double(mousePositionRelativeToProcess.x, mousePositionRelativeToProcess.y))) {
					return op;
				}
			}
		}
		return null;
	}

	private void recomputeDependentOperators() {
		if ((startOperator == null) || (endOperator == null)) {
			dependentOps = null;
		} else {
			ExecutionUnit unit = startOperator.getExecutionUnit();
			if (endOperator.getExecutionUnit() != unit) {
				dependentOps = null;
				return;
			} else {
				List<Operator> operators = unit.getOperators(); 
				dependentOps = getDependingOperators(processRenderer.getDisplayedChain(), 
						operators.indexOf(startOperator), 
						operators.indexOf(endOperator), 
						operators);
			}
		}
	}

	private boolean showPopupMenu(MouseEvent e) {
		if (e.isPopupTrigger()) {
			if (hoveringOperator != null) {
				JPopupMenu menu = new JPopupMenu();
				menu.add(BRING_TO_FRONT);
				menu.show(processRenderer, e.getX(), e.getY());
				return true;
			} else {
				return false;	
			}	
		} else {
			return false;
		}
	}
}
