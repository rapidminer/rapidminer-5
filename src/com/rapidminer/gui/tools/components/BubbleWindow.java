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

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.Window;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;

import javax.swing.AbstractButton;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;

import com.rapidminer.gui.Perspective;
import com.rapidminer.gui.PerspectiveChangeListener;
import com.rapidminer.gui.RapidMinerGUI;
import com.rapidminer.gui.tools.ResourceAction;
import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.gui.tour.Step;
import com.rapidminer.tools.I18N;
import com.rapidminer.tools.LogService;
import com.rapidminer.tools.Tools;
import com.sun.awt.AWTUtilities;
import com.vlsolutions.swing.docking.Dockable;
import com.vlsolutions.swing.docking.DockableState;
import com.vlsolutions.swing.docking.DockingDesktop;
import com.vlsolutions.swing.docking.event.DockableStateChangeEvent;
import com.vlsolutions.swing.docking.event.DockableStateChangeListener;
import com.vlsolutions.swing.docking.event.DockingActionEvent;
import com.vlsolutions.swing.docking.event.DockingActionListener;

/**
 * This class creates a speech bubble-shaped JDialog, which can be attache to
 * Buttons, either by using its ID or by passing a reference. 
 * The bubble triggers two events which are obserable by the {@link BubbleListener};
 * either if the close button was clicked, or if the corresponding button was used.
 * The keys for the title and the text must be of format gui.bubble.XXX.body or gui.bubble.XXX.title .
 * 
 * @author Philipp Kersting and Thilo Kamradt
 *
 */

public abstract class BubbleWindow extends JDialog {

	private static final long serialVersionUID = -6369389148455099450L;

	public static interface BubbleListener {

		public void bubbleClosed(BubbleWindow bw);

		public void actionPerformed(BubbleWindow bw);
	}

	private List<BubbleListener> listeners = new LinkedList<BubbleListener>();

	/** indicates on which side the Bubble will appear*/
	public enum AlignedSide {
		RIGHT, LEFT, TOP, BOTTOM, MIDDLE
	}

	/** Used to define the position of the pointer of the bubble 
	 * (Describes the corner which points to the component).
	 * CENTER places the Bubble inside the Component. MIDDLE places the BubbleWindow in the middle of the mainframe( won't be checked by the BubbleWindow if chosen).
	 */
	private enum Alignment {
		TOPLEFT, TOPRIGHT, BOTTOMLEFT, BOTTOMRIGHT, LEFTTOP, LEFTBOTTOM, RIGHTTOP, RIGHTBOTTOM, INNERRIGHT, INNERLEFT, MIDDLE;
	}

	private static RenderingHints HI_QUALITY_HINTS = new RenderingHints(null);
	static {
		HI_QUALITY_HINTS.put(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		HI_QUALITY_HINTS.put(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
	}

	private static final int CORNER_RADIUS = 20;
	private static final int WINDOW_WIDTH = 200;

	/** Shape used for setting the shape of the window and for rendering the outline. */
	private Shape shape;
	protected Alignment realAlignment;
	protected AlignedSide preferredAlignment;
	private JPanel bubble;
	private ImageIcon background;
	private JButton close;
	private GridBagConstraints constraints = null;
	private ImageIcon passiveCloseIcon, activCloseIcon;
	private ActionListener listener;
	
	private JLabel headline;
	private JLabel mainText;

	private DockableStateChangeListener stateChangeListener;
	private PerspectiveChangeListener perspectiveListener = null;
	private WindowAdapter windowListener;
	protected Window owner;
	private String myPerspective;
	/** indicates whether the listeners currently are added or not */
	private boolean listenersAdded = false;
	private boolean addPerspective = true;
	protected String docKey = null;
	protected Dockable dockable;
	protected ComponentListener compListener;
	protected DockingActionListener dockListener = null;
	protected final DockingDesktop desktop = RapidMinerGUI.getMainFrame().getDockingDesktop();
	private int dockingCounter = 0;

	/**
	 * @param owner the {@link Window} on which this {@link BubbleWindow} should be shown.
	 * @param preferredAlignment offer for alignment but the Class will calculate by itself whether the position is usable.
	 * @param i18nKey of the message which should be shown
	 * @param ToAttach {@link Component} to which this {@link BubbleWindow} should be placed relative to. 
	 * @param addListener indicates whether the {@link BubbleWindow} closes if the Button was pressed or when another Listener added by a subclass of {@link Step} is fired.
	 */
	public BubbleWindow(Window owner, final AlignedSide preferredAlignment, String i18nKey, String docKey, Object... arguments) {
		super(owner);
		this.owner = owner;
		this.myPerspective = RapidMinerGUI.getMainFrame().getPerspectives().getCurrentPerspective().getName();
		this.preferredAlignment = preferredAlignment;
		if (docKey != null) {
			this.docKey = docKey;
			dockable = desktop.getContext().getDockableByKey(docKey);
		}
		//load image for background
		background = new ImageIcon(Tools.getResource("/images/comic-pattern.png"));
		//headline label
		{
			headline = new JLabel(I18N.getGUIBundle().getString("gui.bubble." + i18nKey + ".title"));
			headline.setFont(new Font("AlterEgoBB", Font.PLAIN, 14).deriveFont(Font.BOLD));
			headline.setMinimumSize(new Dimension(WINDOW_WIDTH, 12));
			headline.setPreferredSize(new Dimension(WINDOW_WIDTH, 12));
		}
		//mainText label
		{
			mainText = new JLabel("<html><div style=\"line-height: 150%;width:" + WINDOW_WIDTH + "px \">" + I18N.getMessage(I18N.getGUIBundle(), "gui.bubble." + i18nKey + ".body", arguments) + "</div></html>");
			mainText.setOpaque(false);
			mainText.setFont(new Font("AlterEgoBB", Font.PLAIN, 12));
			mainText.setMinimumSize(new Dimension(150, 20));
			mainText.setMaximumSize(new Dimension(WINDOW_WIDTH, 800));
		}
		
	}

	/**
	 * should be used to update the Bubble. Call this instead of repaint and similar. Update the Alignment, shape and location. Also this method builds the Bubble by the first call.
	 * @param reregisterListerns
	 */
	public void paint(boolean reregisterListerns) {
		if(constraints == null) {
			this.buildBubble();
		} else {
			this.paintAgain(reregisterListerns);
		}
	}
	/**
	 * builds the Bubble for the first time
	 */
	private void buildBubble() {
		this.realAlignment = this.calculateAlignment(this.realAlignment);
		setLayout(new BorderLayout());
		setUndecorated(true);

		addComponentListener(new ComponentAdapter() {

			@Override
			public void componentResized(ComponentEvent e) {
				shape = createShape(realAlignment);

				String version = System.getProperty("java.version").substring(0, 3).replace(".", "");
				try {
					Integer versionNumber = Integer.valueOf(version);
					if (versionNumber == 16) {
						// Java SE 6 Update 10
						AWTUtilities.setWindowShape(BubbleWindow.this, shape);
					} else if (versionNumber >= 17) {
						// Java 7+
						setShape(shape);
					}
				} catch (Throwable t) {
					LogService.getRoot().log(Level.WARNING, "Could not create shaped Bubble Windows. Error: " + t.getLocalizedMessage(), t);
				}
			}
		});

		GridBagLayout gbl = new GridBagLayout();
		bubble = new JPanel(gbl) {

			private static final long serialVersionUID = 1L;

			@Override
			protected void paintComponent(Graphics gr) {
				super.paintComponent(gr);
				Graphics2D g = (Graphics2D) gr;
				g.setColor(SwingTools.RAPID_I_BROWN);
				g.setStroke(new BasicStroke(6));
				g.setRenderingHints(HI_QUALITY_HINTS);
                g.drawImage(background.getImage(), 0, 0, this);
				g.draw(AffineTransform.getTranslateInstance(-.5, -.5).createTransformedShape(getShape()));
			}
		};
		bubble.setBackground(SwingTools.LIGHTEST_BLUE);
		bubble.setSize(getSize());
		getContentPane().add(bubble, BorderLayout.CENTER);

		constraints = new GridBagConstraints();
		Insets insetsLabel = new Insets(10, 10, 10, 10);
		Insets insetsMainText = new Insets(0, 10, 10, 10);
		switch (realAlignment) {
			case TOPLEFT:
				insetsLabel = new Insets(CORNER_RADIUS + 15, 10, 10, 10);
				break;
			case TOPRIGHT:
				insetsLabel = new Insets(CORNER_RADIUS + 15, 10, 10, 10);
				break;
			case INNERLEFT:
			case LEFTTOP:
				insetsLabel = new Insets(10, CORNER_RADIUS + 15, 10, 10);
				insetsMainText = new Insets(0, CORNER_RADIUS + 15, 10, 10);
				break;
			case LEFTBOTTOM:
				insetsLabel = new Insets(10, CORNER_RADIUS + 15, 10, 10);
				insetsMainText = new Insets(0, CORNER_RADIUS + 15, 10, 10);
				break;
			case BOTTOMRIGHT:
				insetsLabel = new Insets(10, 10, 10, 10);
				insetsMainText = new Insets(0, 10, CORNER_RADIUS + 15, 10);
				break;
			case BOTTOMLEFT:
				insetsLabel = new Insets(10, 10, 10, 10);
				insetsMainText = new Insets(0, 10, CORNER_RADIUS + 15, 10);
				break;
			case INNERRIGHT:
			case RIGHTTOP:
				insetsLabel = new Insets(10, 10, 10, CORNER_RADIUS + 15);
				insetsMainText = new Insets(0, 10, 10, CORNER_RADIUS + 15);
				break;
			case RIGHTBOTTOM:
				insetsLabel = new Insets(10, 10, 10, CORNER_RADIUS + 15);
				insetsMainText = new Insets(0, 10, 10, CORNER_RADIUS + 15);
				break;
			default:
		}
		//add the headline
		constraints.insets = insetsLabel;
		constraints.fill = GridBagConstraints.BOTH;
		constraints.anchor = GridBagConstraints.FIRST_LINE_START;
		constraints.weightx = 1;
		constraints.weighty = 0;
		constraints.gridwidth = GridBagConstraints.RELATIVE;
		bubble.add(headline, constraints);

		//create and add close Button for the Bubble
		constraints.weightx = 0;
		constraints.gridwidth = GridBagConstraints.REMAINDER;
		constraints.insets = insetsLabel;
		passiveCloseIcon = new ImageIcon(DockingDesktop.class.getResource("/com/vlsolutions/swing/docking/close16v2.png"));
		activCloseIcon = new ImageIcon(DockingDesktop.class.getResource("/com/vlsolutions/swing/docking/close16v2rollover.png"));
		close = new JButton(passiveCloseIcon);
		close.setBorderPainted(false);
		close.setOpaque(false);
		// change Icons and set close operation
		close.addMouseListener(new MouseListener() {

			@Override
			public void mouseReleased(MouseEvent e) {
				// don't care
			}

			@Override
			public void mousePressed(MouseEvent e) {
				//don't care
			}

			@Override
			public void mouseExited(MouseEvent e) {
				BubbleWindow.this.close.setIcon(BubbleWindow.this.passiveCloseIcon);

			}

			@Override
			public void mouseEntered(MouseEvent e) {
				BubbleWindow.this.close.setIcon(BubbleWindow.this.activCloseIcon);

			}

			@Override
			public void mouseClicked(MouseEvent e) {
				BubbleWindow.this.dispose();
				fireEventCloseClicked();
			}
		});
		close.setMargin(new Insets(0, 5, 0, 5));
		bubble.add(close, constraints);

		//add the main Text
		constraints.insets = insetsMainText;
		constraints.gridwidth = GridBagConstraints.REMAINDER;
		constraints.weightx = 1;
		constraints.weighty = 1;
		bubble.add(mainText, constraints);

		pack();

		if (this.calculateAlignment(this.realAlignment) == this.realAlignment) {
			positionRelative();
		} else {
			this.paintAgain(false);
		}
	}

	/**
	 * updates the Alignment and Position and repaints the Bubble
	 * @param reregisterListeners if true the listeners will be removed and added again after the repaint
	 */
	private void paintAgain(boolean reregisterListeners) {
		Alignment newAlignment = this.calculateAlignment(realAlignment);
		if(realAlignment.equals(newAlignment)) {
			this.pointAtComponent();
			return;
		} else {
			realAlignment = newAlignment;
		}
		shape = createShape(realAlignment);
		if (reregisterListeners) {
			this.unregisterMovementListener();
		}
		//choose the right call for the right version
		String version = System.getProperty("java.version").substring(0, 3).replace(".", "");
		try {
			Integer versionNumber = Integer.valueOf(version);
			if (versionNumber == 16) {
				// Java SE 6 Update 10
				AWTUtilities.setWindowShape(this, shape);
			} else if (versionNumber >= 17) {
				// Java 7+
				setShape(shape);
			}
		} catch (Throwable t) {
			LogService.getRoot().log(Level.WARNING, "Could not create shaped Bubble Windows. Error: " + t.getLocalizedMessage(), t);
		}

		bubble.removeAll();
		Insets insetsLabel = new Insets(10, 10, 10, 10);
		Insets insetsMainText = new Insets(0, 10, 10, 10);
		switch (realAlignment) {
			case TOPLEFT:
				insetsLabel = new Insets(CORNER_RADIUS + 15, 10, 10, 10);
				break;
			case TOPRIGHT:
				insetsLabel = new Insets(CORNER_RADIUS + 15, 10, 10, 10);
				break;
			case INNERLEFT:
			case LEFTTOP:
				insetsLabel = new Insets(10, CORNER_RADIUS + 15, 10, 10);
				insetsMainText = new Insets(0, CORNER_RADIUS + 15, 10, 10);
				break;
			case LEFTBOTTOM:
				insetsLabel = new Insets(10, CORNER_RADIUS + 15, 10, 10);
				insetsMainText = new Insets(0, CORNER_RADIUS + 15, 10, 10);
				break;
			case BOTTOMRIGHT:
				insetsLabel = new Insets(10, 10, 10, 10);
				insetsMainText = new Insets(0, 10, CORNER_RADIUS + 15, 10);
				break;
			case BOTTOMLEFT:
				insetsLabel = new Insets(10, 10, 10, 10);
				insetsMainText = new Insets(0, 10, CORNER_RADIUS + 15, 10);
				break;
			case INNERRIGHT:
			case RIGHTTOP:
				insetsLabel = new Insets(10, 10, 10, CORNER_RADIUS + 15);
				insetsMainText = new Insets(0, 10, 10, CORNER_RADIUS + 15);
				break;
			case RIGHTBOTTOM:
				insetsLabel = new Insets(10, 10, 10, CORNER_RADIUS + 15);
				insetsMainText = new Insets(0, 10, 10, CORNER_RADIUS + 15);
				break;
			default:
		}
		//add headline
		constraints.insets = insetsLabel;
		constraints.fill = GridBagConstraints.BOTH;
		constraints.anchor = GridBagConstraints.FIRST_LINE_START;
		constraints.weightx = 1;
		constraints.weighty = 0;
		constraints.gridwidth = GridBagConstraints.RELATIVE;
		bubble.add(headline, constraints);

		//add close-Button
		constraints.weightx = 0;
		constraints.gridwidth = GridBagConstraints.REMAINDER;
		constraints.insets = insetsLabel;
		bubble.add(close, constraints);

		//add main text
		constraints.insets = insetsMainText;
		constraints.gridwidth = GridBagConstraints.REMAINDER;
		constraints.weightx = 1;
		constraints.weighty = 1;
		bubble.add(mainText, constraints);

		pack();

		positionRelative();
		
	}

	/**
	 * 
	 * Adds a {@link BubbleListener}.
	 * 
	 * @param l The listener
	 */
	public void addBubbleListener(BubbleListener l) {
		listeners.add(l);
	}

	/**
	 * removes the given {@link BubbleListener}.
	 * @param l {@link BubbleListener} to remove.
	 */
	public void removeBubbleListener(BubbleListener l) {
		listeners.remove(l);
	}

	/**
	 * Creates a speech bubble-shaped Shape.
	 * 
	 * @param alignment The alignment of the pointer.
	 * 
	 * @return A speech-bubble <b>Shape</b>.
	 */
	public Shape createShape(Alignment alignment) {
		int w = getSize().width - 2 * CORNER_RADIUS;
		int h = getSize().height - 2 * CORNER_RADIUS;
		int o = CORNER_RADIUS;

		GeneralPath gp = new GeneralPath();
		switch (alignment) {
			case TOPLEFT:
				gp.moveTo(0, 0);
				gp.lineTo(0, h + o);
				gp.quadTo(0, h + (2 * o), o, h + (2 * o));
				gp.lineTo(w + o, h + (2 * o));
				gp.quadTo(w + (2 * o), h + (2 * o), w + (2 * o), h + o);
				gp.lineTo(w + (2 * o), (2 * o));
				gp.quadTo(w + (2 * o), o, w + o, o);
				gp.lineTo(o, o);
				gp.lineTo(0, 0);
				break;
			case TOPRIGHT:
				gp.moveTo(0, 2 * o);
				gp.lineTo(0, h + o);
				gp.quadTo(0, h + (2 * o), o, h + (2 * o));
				gp.lineTo(w + o, h + (2 * o));
				gp.quadTo(w + (2 * o), h + (2 * o), w + (2 * o), h + o);
				gp.lineTo(w + (2 * o), 0);
				gp.lineTo((w + o), o);
				gp.lineTo(o, o);
				gp.quadTo(0, o, 0, (2 * o));
				break;
			case BOTTOMLEFT:
				gp.moveTo(0, o);
				gp.lineTo(0, h + (2 * o));
				gp.lineTo(o, h + o);
				gp.lineTo(w + o, h + o);
				gp.quadTo(w + (2 * o), h + o, w + (2 * o), h);
				gp.lineTo(w + (2 * o), o);
				gp.quadTo(w + (2 * o), 0, w + o, 0);
				gp.lineTo(o, 0);
				gp.quadTo(0, 0, 0, o);
				break;
			case BOTTOMRIGHT:
				gp.moveTo(0, o);
				gp.lineTo(0, h);
				gp.quadTo(0, (h + o), o, (h + o));
				gp.lineTo(w + o, (h + o));
				gp.lineTo(w + (2 * o), h + (2 * o));
				gp.lineTo(w + (2 * o), o);
				gp.quadTo(w + (2 * o), 0, w + o, 0);
				gp.lineTo(o, 0);
				gp.quadTo(0, 0, 0, o);
				break;
			case LEFTBOTTOM:
				gp.moveTo(0, h + (2 * o));
				gp.lineTo(w + o, h + (2 * o));
				gp.quadTo(w + (2 * o), h + (2 * o), w + (2 * o), h + o);
				gp.lineTo(w + (2 * o), o);
				gp.quadTo(w + (2 * o), 0, w + o, 0);
				gp.lineTo((2 * o), 0);
				gp.quadTo(o, 0, o, o);
				gp.lineTo(o, h + o);
				gp.closePath();
				break;
			case INNERLEFT:
			case LEFTTOP:
				gp.moveTo(0, 0);
				gp.lineTo(o, o);
				gp.lineTo(o, (h + o));
				gp.quadTo(o, h + (2 * o), (2 * o), h + (2 * o));
				gp.lineTo(w + o, h + (2 * o));
				gp.quadTo(w + (2 * o), h + (2 * o), w + (2 * o), h + o);
				gp.lineTo(w + (2 * o), o);
				gp.quadTo(w + (2 * o), 0, w + o, 0);
				gp.lineTo(0, 0);
				break;
			case RIGHTBOTTOM:
				gp.moveTo(0, h + o);
				gp.quadTo(0, h + (2 * o), o, h + (2 * o));
				gp.lineTo(w + (2 * o), h + (2 * o));
				gp.lineTo(w + o, h + o);
				gp.lineTo(w + o, o);
				gp.quadTo(w + o, 0, w, 0);
				gp.lineTo(o, 0);
				gp.quadTo(0, 0, 0, o);
				gp.lineTo(0, h + o);
				break;
			case INNERRIGHT:
			case RIGHTTOP:
				gp.moveTo(o, 0);
				gp.quadTo(0, 0, 0, o);
				gp.lineTo(0, (h + o));
				gp.quadTo(0, h + (2 * o), o, h + (2 * o));
				gp.lineTo(w, h + (2 * o));
				gp.quadTo((w + o), h + (2 * o), (w + o), (h + o));
				gp.lineTo((w + o), o);
				gp.lineTo(w + (2 * o), 0);
				gp.lineTo(o, 0);
				break;
			case MIDDLE:
				gp.moveTo(o, 0);
				gp.quadTo(0, 0, 0, o);
				gp.lineTo(0, (h + o));
				gp.quadTo(0, h + (2 * o), o, h + (2 * o));
				gp.lineTo(w + o, h + (2 * o));
				gp.quadTo(w + (2 * o), h + (2 * o), w + (2 * o), h + o);
				gp.lineTo(w + (2 * o), o);
				gp.quadTo(w + (2 * o), 0, w + o, 0);
				gp.lineTo(o, 0);
				break;
			default:
		}
		AffineTransform tx = new AffineTransform();
		return gp.createTransformedShape(tx);
	}

	/**
	 * places the {@link BubbleWindow} relative to the Component which was given and adds the listeners.
	 */
	private void positionRelative() {

		pointAtComponent();

		registerMovementListener();
	}

	/**
	 * places the Bubble-speech so that it points to the Component 
	 * @param component component to point to
	 */
	protected void pointAtComponent() {
		double targetx = 0;
		double targety = 0;
		Point target = new Point(0, 0);
		if (realAlignment == Alignment.MIDDLE) {
			targetx = owner.getWidth() * 0.5 - getWidth() * 0.5;
			targety = owner.getHeight() * 0.5 - getHeight() * 0.5;
		} else {
			Point location = this.getObjectLocation();
			int x = (int) location.getX();
			int y = (int) location.getY();
			int h = this.getObjectHeight();
			int w = this.getObjectWidth();
			switch (realAlignment) {
				case TOPLEFT:
					targetx = x + 0.5 * w;
					targety = y + h;
					break;
				case TOPRIGHT:
					targetx = (x + 0.5 * w) - getWidth();
					targety = y + h;
					break;
				case LEFTBOTTOM:
					targetx = x + w;
					targety = (y + 0.5 * h) - getHeight();
					break;
				case LEFTTOP:
					targetx = x + w;
					targety = (y + 0.5 * h);
					break;
				case RIGHTBOTTOM:
					targetx = x - getWidth();
					targety = (y + 0.5 * h) - getHeight();
					break;
				case RIGHTTOP:
					targetx = x - getWidth();
					targety = (y + 0.5 * h);
					break;
				case BOTTOMLEFT:
					targetx = x + 0.5 * w;
					targety = y - getHeight();
					break;
				case BOTTOMRIGHT:
					targetx = x + 0.5 * w - getWidth();
					targety = y - getHeight();
					break;
				case INNERLEFT:
					targetx = x + w - 0.5 * getWidth();
					double xShift = (targetx + getWidth()) - (owner.getX() + owner.getWidth());
					if (xShift > 0) {
						targetx -= xShift;
					}
					targety = y + h - 0.5 * getHeight();
					double yShift = (targety + getHeight()) - (owner.getY() + owner.getHeight());
					if (yShift > 0) {
						targetx -= yShift;
					}
					break;
				case INNERRIGHT:
					targetx = x - 0.5 * getWidth();
					xShift = owner.getX() - targetx;
					if (xShift > 0) {
						targetx += xShift;
					}
					targety = y + h - 0.5 * getHeight();
					yShift = (targety + getHeight()) - (owner.getY() + owner.getHeight());
					if (yShift > 0) {
						targetx -= yShift;
					}
				default:
			}
		}

		target = new Point((int) Math.round(targetx), (int) Math.round(targety));
		setLocation(target);
	}

	/**
	 * method to get to know whether the dockable with the given key is on Screen
	 * @param dockableKey i18nKey of the wanted Dockable
	 * @return returns 1 if the Dockable is on the Screen and -1 if the Dockable is not on the Screen. 
	 */
	public static int isDockableOnScreen(String dockableKey) {
		Dockable onScreen = RapidMinerGUI.getMainFrame().getDockingDesktop().getContext().getDockableByKey(dockableKey);
		if (onScreen == null)
			return -1;
		return 1;

	}

	/**
	 * method to get to know whether the AbstractButton with the given key is on Screen
	 * @param dockableKey i18nKey of the wanted AbstractButton
	 * @return returns 1 if the AbstractButton is on the Screen, 0 if the AbstractButton is on Screen but the user can not see it with the current settings of the perspective and -1 if the AbstractButton is not on the Screen. 
	 */
	public static int isButtonOnScreen(String buttonKey) {
		// find the Button and return -1 if we can not find it
		Component onScreen;
		try {
			onScreen = BubbleWindow.findButton(buttonKey, RapidMinerGUI.getMainFrame());
		} catch (NullPointerException e) {
			return -1;
		}
		if (onScreen == null)
			return -1;
		// detect whether the Button is viewable
		int xposition = onScreen.getLocationOnScreen().x;
		int yposition = onScreen.getLocationOnScreen().y;
		int otherXposition = xposition + onScreen.getWidth();
		int otherYposition = yposition + onScreen.getHeight();
		Window frame = RapidMinerGUI.getMainFrame();
		if (otherXposition <= frame.getWidth() && otherYposition <= frame.getHeight() && xposition > 0 && yposition > 0) {
			return 1;
		} else {
			return 0;
		}
	}

	/**
	 * @param name i18nKey of the Button
	 * @param searchRoot {@link Component} to search in for the Button
	 * @return returns the {@link AbstractButton} if found or null if the Button was not found. 
	 */
	public static AbstractButton findButton(String name, Component searchRoot) {
		if (searchRoot instanceof AbstractButton) {

			AbstractButton b = (AbstractButton) searchRoot;
			if (b.getAction() instanceof ResourceAction) {
				String id = (String) b.getAction().getValue("rm_id");
				if (name.equals(id)) {
					return b;
				}
			}
		}
		if (searchRoot instanceof Container) {
			Component[] all = ((Container) searchRoot).getComponents();
			for (Component child : all) {
				AbstractButton result = findButton(name, child);
				if (result != null) {
					return result;

				}
			}
		}
		return null;
	}

	/**
	 * Returns the {@link Shape} of this {@link BubbleWindow}
	 */
	public Shape getShape() {
		if (shape == null) {
			shape = createShape(realAlignment);
		}
		return shape;
	}

	protected void registerMovementListener() {
		if (!listenersAdded) {
			if (addPerspective) {
				perspectiveListener = new PerspectiveChangeListener() {

					@Override
					public void perspectiveChangedTo(Perspective perspective) {
						if ((BubbleWindow.this.myPerspective).equals(perspective.getName())) {
							BubbleWindow.this.reloadComponent();
							BubbleWindow.this.setVisible(true);
						} else {
							BubbleWindow.this.setVisible(false);
						}
					}
				};
			}
			compListener = new ComponentListener() {

				@Override
				public void componentShown(ComponentEvent e) {
					BubbleWindow.this.pointAtComponent();
					BubbleWindow.this.setVisible(true);
				}

				@Override
				public void componentResized(ComponentEvent e) {
					if (BubbleWindow.this.realAlignment.equals(BubbleWindow.this.calculateAlignment(realAlignment))) {
						BubbleWindow.this.pointAtComponent();
					} else {
						BubbleWindow.this.paintAgain(false);
					}
					BubbleWindow.this.setVisible(true);
				}

				@Override
				public void componentMoved(ComponentEvent e) {
					if (BubbleWindow.this.realAlignment.equals(BubbleWindow.this.calculateAlignment(realAlignment))) {
						BubbleWindow.this.pointAtComponent();
					} else {
						BubbleWindow.this.paintAgain(true);
					}
					BubbleWindow.this.setVisible(true);
				}

				@Override
				public void componentHidden(ComponentEvent e) {
					BubbleWindow.this.setVisible(false);
				}
			};
			if(docKey == null) {
				//no component was attached but possible there are some side effects
				RapidMinerGUI.getMainFrame().addComponentListener(compListener);
			} else {
				BubbleWindow.this.dockable.getComponent().addComponentListener(compListener);
				dockListener = new DockingActionListener() {

					@Override
					public void dockingActionPerformed(DockingActionEvent event) {
						//TODO: use constants instead of integers and check for name first
						// actionType 2 indicates that a Dockable was splitted
						// actionType 3 indicates that the Dockable has created his own position
						// actionType 5 indicates that the Dockable was docked to another position
						// actionType 6 indicates that the Dockable was separated
						if (event.getActionType() == 5 || event.getActionType() == 3) {
							if ((++dockingCounter) % 2 == 0) {
								//get the new component of the Dockable because the current component is disabled
								BubbleWindow.this.dockable.getComponent().removeComponentListener(compListener);
								BubbleWindow.this.reloadComponent();
								//repaint
								BubbleWindow.this.paintAgain(false);
								BubbleWindow.this.setVisible(true);
							}
						}
						if (event.getActionType() == 6 || event.getActionType() == 2) {
							//get the new component of the Dockable because the current component is disabled
							BubbleWindow.this.dockable.getComponent().removeComponentListener(compListener);
							BubbleWindow.this.reloadComponent();
							//repaint
							BubbleWindow.this.paintAgain(false);
							BubbleWindow.this.setVisible(true);
						}
					}

					@Override
					public boolean acceptDockingAction(DockingActionEvent arg0) {
						// no need to deny anything
						return true;
					}
				};
				desktop.addDockingActionListener(dockListener);
				stateChangeListener = new DockableStateChangeListener() {
					
					@Override
					public void dockableStateChanged(DockableStateChangeEvent arg0) {
						DockableState state = arg0.getNewState();
						if (state.isClosed()) {
							//TODO: try to reload
							System.out.println("---dock closed");
						} else if (state.isDocked()) {
							//TODO: do nothing
							System.out.println("---dock docked");
						} else if (state.isFloating()) {
							//TODO: re attach
							System.out.println("---dock floating");
						} else if (state.isMaximized()) {
							//TODO: set reload bubble (paint(true))
							System.out.println("---dock maximized");
						}
						if(arg0.getNewState().getDockable().getDockKey().getKey().equals(BubbleWindow.this.docKey)) {
							state = arg0.getNewState();
							if (state.isClosed()) {
								//TODO: try to reload
								System.out.println("dock closed");
							} else if (state.isDocked()) {
								//TODO: do nothing
								System.out.println("dock docked");
							} else if (state.isFloating()) {
								//TODO: re attach
								System.out.println("dock floating");
							} else if (state.isMaximized()) {
								//TODO: set reload bubble (paint(true))
								System.out.println("dock maximized");
							}
							switch (state.getLocation()) {
								default:
									break;
								
							}
						}
						
					}
				};
				desktop.getContext().addDockableStateChangeListener(stateChangeListener);
			}
			windowListener = new WindowAdapter() {

				@Override
				public void windowIconified(WindowEvent e) {
					super.windowIconified(e);
					BubbleWindow.this.setVisible(false);
				}

				@Override
				public void windowDeiconified(WindowEvent e) {
					super.windowDeiconified(e);
					BubbleWindow.this.pointAtComponent();
					BubbleWindow.this.setVisible(true);
				}

			};
			if (addPerspective) {
				RapidMinerGUI.getMainFrame().getPerspectives().addPerspectiveChangeListener(perspectiveListener);
			}
			RapidMinerGUI.getMainFrame().addWindowStateListener(windowListener);
			listenersAdded = true;
		}

	}

	private void unregister() {
		if (close != null) {
			close.removeActionListener(listener);
		}
	}

	protected void unregisterMovementListener() {
		if(listenersAdded) {
			if(docKey == null) {
				RapidMinerGUI.getMainFrame().removeComponentListener(compListener);
			} else {
				BubbleWindow.this.dockable.getComponent().removeComponentListener(compListener);
				desktop.removeDockingActionListener(dockListener);
			}
			if (addPerspective) {
				RapidMinerGUI.getMainFrame().getPerspectives().removePerspectiveChangeListener(perspectiveListener);
			}
			RapidMinerGUI.getMainFrame().removeWindowStateListener(windowListener);
			listenersAdded = false;
		}
	}

	/**
	 * notifies the {@link BubbleListener}s and disposes the Bubble-speech.
	 */
	public void triggerFire() {
		fireEventActionPerformed();
		dispose();
	}

	protected void fireEventCloseClicked() {
		LinkedList<BubbleListener> listenerList = new LinkedList<BubbleWindow.BubbleListener>(listeners);
		this.unregister();
		for (BubbleListener l : listenerList) {
			l.bubbleClosed(this);
		}
		unregisterMovementListener();
	}

	protected void fireEventActionPerformed() {
		LinkedList<BubbleListener> listenerList = new LinkedList<BubbleWindow.BubbleListener>(listeners);
		for (BubbleListener l : listenerList) {
			l.actionPerformed(this);
		}
		unregisterMovementListener();
		unregister();
	}

	/**
	 * calculates the Alignment in the way, that the Bubble do not leave the Window
	 * @param preferredAlignment preferred Alignment of the User
	 * @param location Point which indicates the left upper corner of the Object to which the Bubble should point to
	 * @param xSize size in x-direction of the Object the Bubble should point to
	 * @param ySize size in y-direction of the Object the Bubble should point to 
	 * @return returns the calculated {@link Alignment}
	 */
	protected Alignment calculateAlignment(Alignment currentAlignment) {

		if (AlignedSide.MIDDLE == this.preferredAlignment) {
			return Alignment.MIDDLE;
		}
		//get Mainframe location
		Point frameLocation = owner.getLocationOnScreen();
		double xlocFrame = frameLocation.getX();
		double ylocFrame = frameLocation.getY();

		//get Mainframe size
		int frameWidth = owner.getWidth();
		int frameHeight = owner.getHeight();

		//location and size of Component the want to attach to
		Point componentLocation = this.getObjectLocation();
		double xlocComponent = componentLocation.getX();
		double ylocComponent = componentLocation.getY();
		int componentWidth = this.getObjectWidth();
		int componentHeight = this.getObjectHeight();

		//load height and width or the approximate Value of worst case
		double bubbleWidth = this.getWidth();
		double bubbleHeight = this.getHeight();
		if (bubbleWidth == 0 || bubbleHeight == 0) {
			bubbleWidth = 326;
			bubbleHeight = 200;
		}
//		TODO: after finishing design recalculate the save zone
		if (currentAlignment == Alignment.TOPLEFT || currentAlignment == Alignment.TOPRIGHT || currentAlignment == Alignment.BOTTOMLEFT || currentAlignment == Alignment.BOTTOMRIGHT) {
			bubbleWidth += 46;
		} else {
			bubbleHeight += 35;
		}
		// 0 = space above the component
		// 1 = space right of the component
		// 2 = space below the component
		// 3 = space left of the Component
		double space[] = new double[4];
		space[0] = (ylocComponent - ylocFrame) / (bubbleHeight);
		space[1] = ((frameWidth + xlocFrame) - (xlocComponent + componentWidth)) / (bubbleWidth);
		space[2] = ((frameHeight + ylocFrame) - (ylocComponent + componentHeight)) / (bubbleHeight);
		space[3] = (xlocComponent - xlocFrame) / (bubbleWidth);
		// check if the preferred Alignment is valid and take it if it is valid
		switch (this.preferredAlignment) {
			case BOTTOM:
				if (space[2] > 1)
					return this.fineTuneAlignment(Alignment.TOPLEFT, frameWidth, frameHeight, frameLocation, componentLocation, componentWidth, componentHeight);
				break;
			case RIGHT:
				if (space[1] > 1)
					return this.fineTuneAlignment(Alignment.LEFTBOTTOM, frameWidth, frameHeight, frameLocation, componentLocation, componentWidth, componentHeight);
				break;
			case LEFT:
				if (space[3] > 1)
					return this.fineTuneAlignment(Alignment.RIGHTBOTTOM, frameWidth, frameHeight, frameLocation, componentLocation, componentWidth, componentHeight);
				break;
			case TOP:
				if (space[0] > 1)
					return this.fineTuneAlignment(Alignment.BOTTOMLEFT, frameWidth, frameHeight, frameLocation, componentLocation, componentWidth, componentHeight);
				break;
			default:
		}
		//preferred Alignment was not valid. try to show bubble at the same position as before
		if (currentAlignment != null) {
			switch (currentAlignment) {
				case BOTTOMRIGHT:
				case BOTTOMLEFT:
					if (space[0] > 1)
						return this.fineTuneAlignment(Alignment.BOTTOMLEFT, frameWidth, frameHeight, frameLocation, componentLocation, componentWidth, componentHeight);
					break;
				case LEFTTOP:
				case LEFTBOTTOM:
					if (space[1] > 1)
						return this.fineTuneAlignment(Alignment.LEFTBOTTOM, frameWidth, frameHeight, frameLocation, componentLocation, componentWidth, componentHeight);
					break;
				case TOPRIGHT:
				case TOPLEFT:
					if (space[2] > 1)
						return this.fineTuneAlignment(Alignment.TOPLEFT, frameWidth, frameHeight, frameLocation, componentLocation, componentWidth, componentHeight);
					break;
				case RIGHTTOP:
				case RIGHTBOTTOM:
					if (space[3] > 1)
						return this.fineTuneAlignment(Alignment.RIGHTBOTTOM, frameWidth, frameHeight, frameLocation, componentLocation, componentWidth, componentHeight);
					break;
				case INNERRIGHT:
				case INNERLEFT:
					if (space[0] > 1) {
						return this.fineTuneAlignment(Alignment.BOTTOMLEFT, frameWidth, frameHeight, frameLocation, componentLocation, componentWidth, componentHeight);
					} else if (space[1] > 1) {
						return this.fineTuneAlignment(Alignment.LEFTBOTTOM, frameWidth, frameHeight, frameLocation, componentLocation, componentWidth, componentHeight);
					} else if (space[2] > 1) {
						return this.fineTuneAlignment(Alignment.TOPLEFT, frameWidth, frameHeight, frameLocation, componentLocation, componentWidth, componentHeight);
					} else if (space[3] > 1) {
						return this.fineTuneAlignment(Alignment.RIGHTBOTTOM, frameWidth, frameHeight, frameLocation, componentLocation, componentWidth, componentHeight);
					} else {
//						return this.fineTuneAlignment(Alignment.INNERLEFT, frameWidth, frameHeight, frameLocation, location, componentWidth, componentHeight);
						return realAlignment;
					}
				default:
					throw new IllegalStateException("this part of code should be unreachable for this state of BubbleWindow");
			}
		}
		if (space[1] > 1)
			return this.fineTuneAlignment(Alignment.LEFTTOP, frameWidth, frameHeight, frameLocation, componentLocation, componentWidth, componentHeight);
			
		//can not keep the old alignment. take the best fitting place
		int pointer = 0;
		for (int i = 1; i < space.length; i++) {
			if (space[i] > space[pointer]) {
				pointer = i;
			}
		}
		if (space[pointer] > 1) {
			switch (pointer) {
				case 0:
					return this.fineTuneAlignment(Alignment.BOTTOMLEFT, frameWidth, frameHeight, frameLocation, componentLocation, componentWidth, componentHeight);
				case 1:
					return this.fineTuneAlignment(Alignment.LEFTTOP, frameWidth, frameHeight, frameLocation, componentLocation, componentWidth, componentHeight);
				case 2:
					return this.fineTuneAlignment(Alignment.TOPLEFT, frameWidth, frameHeight, frameLocation, componentLocation, componentWidth, componentHeight);
				case 3:
					return this.fineTuneAlignment(Alignment.RIGHTTOP, frameWidth, frameHeight, frameLocation, componentLocation, componentWidth, componentHeight);
				default:
					return null;
			}
		} else {
			//can not place Bubble outside of the component so we take the right side of the inner of the Component.
			return this.fineTuneAlignment(Alignment.INNERLEFT, frameWidth, frameHeight, frameLocation, componentLocation, componentWidth, componentHeight);
		}

	}

	/**
	 * Whether we want the north-, south, west- or east-side of the {@link Component} was 
	 * chosen before this method the decide in which direction the Bubble will expand 
	 * @param firstCompute first computed Alignment
	 * @param xframe width of the owner
	 * @param yframe height of the owner
	 * @param frameLocation location of the origin of the owner
	 * @param componentLocation location of the origin of the Component to attach to
	 * @param compWidth width of the component to attach to
	 * @param compHeight height of the component to attach to
	 * @return
	 */
	private Alignment fineTuneAlignment(Alignment firstCompute, int xframe, int yframe, Point frameLocation, Point componentLocation, int compWidth, int compHeight) {
		switch (firstCompute) {
			case TOPLEFT:
			case TOPRIGHT:
				if (((componentLocation.x - frameLocation.x) + (compWidth / 2)) > (xframe / 2)) {
					return Alignment.TOPRIGHT;
				} else {
					return Alignment.TOPLEFT;
				}
			case LEFTBOTTOM:
			case LEFTTOP:
				if (((componentLocation.y - frameLocation.y) + (compHeight / 2)) > (yframe / 2)) {
					return Alignment.LEFTBOTTOM;
				} else {
					return Alignment.LEFTTOP;
				}
			case RIGHTBOTTOM:
			case RIGHTTOP:
				if (((componentLocation.y - frameLocation.y) + (compHeight / 2)) > (yframe / 2)) {
					return Alignment.RIGHTBOTTOM;
				} else {
					return Alignment.RIGHTTOP;
				}
			case BOTTOMLEFT:
			case BOTTOMRIGHT:
				if (((componentLocation.x - frameLocation.x) + (compWidth / 2)) > (xframe / 2)) {
					return Alignment.BOTTOMRIGHT;
				} else {
					return Alignment.BOTTOMLEFT;
				}
			default:
				if (realAlignment == Alignment.INNERLEFT || realAlignment == Alignment.INNERRIGHT)
					return realAlignment;

				if ((componentLocation.x - frameLocation.x) > ((xframe + frameLocation.x) - (compWidth + componentLocation.x))) {
					return Alignment.INNERRIGHT;
				} else {
					return Alignment.INNERLEFT;
				}
		}

	}

	protected void setAddPerspectiveListener(boolean addListener) {
		this.addPerspective = addListener;
	}

	/**
	 * returns the location of the Object the Bubble should attach to
	 * @return the Point indicates the left upper corner of the Object the Bubble should point to
	 */
	protected abstract Point getObjectLocation();

	/**
	 * method to get the width of the Object the Bubble should attach to
	 * @return returns the width of the Object
	 */
	protected abstract int getObjectWidth();

	/**
	 * method to get the height of the Object the Bubble should attach to
	 * @return returns the height of the Object
	 */
	protected abstract int getObjectHeight();

	/**
	 * deletes old listeners, updates the Components which are listened and adds the Component specific listeners again
	 */
	protected void reloadComponent() {
		if (docKey != null) {
			dockable = desktop.getContext().getDockableByKey(docKey);
			BubbleWindow.this.dockable.getComponent().addComponentListener(compListener);
			desktop.addDockingActionListener(dockListener);
		}
	}

	/**
	 * unregister the components specific listeners defined in the subclasses
	 */
	protected abstract void unregisterSpecificListeners();

	/** register the components specific listeners defined in the subclasses*/
	protected abstract void registerSpecificListener();

}
