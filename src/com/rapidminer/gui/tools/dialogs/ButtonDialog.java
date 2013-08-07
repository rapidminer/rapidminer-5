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
package com.rapidminer.gui.tools.dialogs;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.KeyEvent;
import java.util.Collection;
import java.util.LinkedList;

import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.WindowConstants;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import com.rapidminer.gui.ApplicationFrame;
import com.rapidminer.gui.RapidMinerGUI;
import com.rapidminer.gui.tools.ResourceAction;
import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.gui.tools.components.FixedWidthLabel;
import com.rapidminer.tools.I18N;


/** Dialog that provides some helper methods to create buttons. Automatically registers accelerators
 *  and action listeners. Override {@link #ok()}, {@link #cancel()} and {@link #close()} to customize
 *  the behaviour.
 *  
 *  The user can query if the ok button was pressed ({@link #wasConfirmed}).
 * @author Simon Fischer
 */
public class ButtonDialog extends JDialog {

	private static final long serialVersionUID = 1L;

	private String key = null;

	protected boolean wasConfirmed = false;

	public static final int NORMAL = 1;
	public static final int NARROW = 2;
	public static final int LARGE = 3;
	public static final int HUGE = 9;
	public static final int MESSAGE = 4;
	public static final int MESSAGE_EXTENDED = 5;
	public static final int DEFAULT_SIZE = 8;
	

	public static final int GAP = 6;

	protected static final Insets INSETS = new Insets(GAP, GAP, GAP, GAP);

	protected FixedWidthLabel infoTextLabel = null;

	/**
	 * Arguments which will replace the place holder in the 
	 * I18n-Properties message. The first argument will replace 
	 * <code>{0}</code>, the second <code>{1}</code> and so on.
	 */
	protected final Object[] arguments;

	private Component centerComponent;
	
	private final LinkedList<ChangeListener> listeners = new LinkedList<ChangeListener>();

	/**
	 * The key will be used for the properties gui.dialog.-key-.title and
	 * gui.dialog.-key-.icon
	 */
	public ButtonDialog(String key, Object ... arguments) {
		super(ApplicationFrame.getApplicationFrame(), I18N.getMessage(I18N.getGUIBundle(), "gui.dialog." + key + ".title", arguments), false);
		this.arguments = arguments;
		configure(key);
		pack();
	}

	/**
	 * The key will be used for the properties gui.dialog.-key-.title and
	 * gui.dialog.-key-.icon
	 */
	public ButtonDialog(String key, boolean modal, Object ... arguments) {
		super(ApplicationFrame.getApplicationFrame(), I18N.getMessage(I18N.getGUIBundle(), "gui.dialog." + key + ".title", arguments), modal);
		this.arguments = arguments;
		configure(key);
		pack();
	}
	
	/**
	 * The key will be used for the properties gui.dialog.-key-.title and
	 * gui.dialog.-key-.icon
	 */
	public ButtonDialog(String key, ModalityType type, Object ... arguments) {
		super(ApplicationFrame.getApplicationFrame(), I18N.getMessage(I18N.getGUIBundle(), "gui.dialog." + key + ".title", arguments), type);
		this.arguments = arguments;
		configure(key);
		pack();
	}

	/**
	 * @param key			Key for the message in the I18n-GUI-properties file
	 * @param arguments		Arguments which will replace the place holder in the 
	 * 						I18n-Properties message. The first argument will replace 
	 * 						<code>{0}</code>, the second <code>{1}</code> and so on.
	 * @deprecated
	 */
	@Deprecated
	public ButtonDialog(Dialog owner, String key, boolean modal, Object... arguments ) {
		this(key, modal, arguments);
//		super(owner, I18N.getMessage(I18N.getGUIBundle(), "gui.dialog." + key + ".title"), modal);
//		this.arguments = arguments;
//		configure(key);
	}

	/**
	 * @param key			Key for the message in the I18n-GUI-properties file
	 * @param arguments		Arguments which will replace the place holder in the 
	 * 						I18n-Properties message. The first argument will replace 
	 * 						<code>{0}</code>, the second <code>{1}</code> and so on.
	 * @deprecated
	 */
	@Deprecated
	public ButtonDialog(Dialog owner, String key, Object... arguments ) {
		this(key, arguments);
//		super(owner, I18N.getMessage(I18N.getGUIBundle(), "gui.dialog." + key + ".title"));
//		this.arguments = arguments;
//		configure(key);
	}

	/**
	 * @param key			Key for the message in the I18n-GUI-properties file
	 * @param arguments		Arguments which will replace the place holder in the 
	 * 						I18n-Properties message. The first argument will replace 
	 * 						<code>{0}</code>, the second <code>{1}</code> and so on.
	 * @deprecated
	 */
	@Deprecated
	public ButtonDialog(Frame owner, String key, boolean modal, Object... arguments ) {
		this(key, modal, arguments);
//		super(owner, I18N.getMessage(I18N.getGUIBundle(), "gui.dialog." + key + ".title"), modal);
//		this.arguments = arguments;
//		configure(key);
	}

	/**
	 * @param key			Key for the message in the I18n-GUI-properties file
	 * @param arguments		Arguments which will replace the place holder in the 
	 * 						I18n-Properties message. The first argument will replace 
	 * 						<code>{0}</code>, the second <code>{1}</code> and so on.
	 * @deprecated
	 */
	@Deprecated
	public ButtonDialog(Frame owner, String key, Object... arguments ) {
		this(key, arguments);
//		super(owner, I18N.getMessage(I18N.getGUIBundle(), "gui.dialog." + key + ".title"));
//		this.arguments = arguments;
//		configure(key);
	}

	private void configure(String key) {
		this.key = key;
		setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
	}

	protected final String getKey() {
		return "gui.dialog." + key;
	}

	/**
	 * Returns the internationalized info text for this dialog.
	 * Argument formation is already applied.  
	 */
	protected String getInfoText() {
		return I18N.getMessage(I18N.getGUIBundle(), getKey() + ".message", this.arguments);
	}

	protected Icon getInfoIcon() {
		return SwingTools.createIcon("48/" + I18N.getMessage(I18N.getGUIBundle(), getKey() + ".icon"));
	}

	/**
	 * Returns the internationalized title for this dialog.
	 * Argument formation is already applied.  
	 */
	protected String getDialogTitle() {
		return I18N.getMessage(I18N.getGUIBundle(), getKey() + ".title", this.arguments); 
	}

	private JPanel makeInfoPanel() {
		return makeInfoPanel(getInfoText(), getInfoIcon());
	}

	private JPanel makeInfoPanel(String message, Icon icon) {
		JLabel infoIcon = new JLabel(icon);
		infoIcon.setVerticalAlignment(SwingConstants.TOP);
		JPanel infoPanel = new JPanel(new BorderLayout(20, 0));
		infoPanel.setBorder(BorderFactory.createEmptyBorder(12, 16, 16, 4));
		infoPanel.add(infoIcon, BorderLayout.WEST);
		int width;
		if (centerComponent!= null) {
			width = (int) centerComponent.getPreferredSize().getWidth() - 88; // icon plus padding
			if (width < 420) {
				width = 420;
			}
		} else {
			width = 420;
		}
		infoTextLabel = new FixedWidthLabel(width, message);		
		infoPanel.add(infoTextLabel, BorderLayout.CENTER);
		
		return infoPanel;
	}

	protected void layoutDefault(JComponent centerComponent, int size, Collection<AbstractButton> buttons) {
		layoutDefault(centerComponent, size, buttons.toArray(new AbstractButton[buttons.size()]));
	}

	protected void layoutDefault(JComponent centerComponent, Collection<AbstractButton> buttons) {
		layoutDefault(centerComponent, DEFAULT_SIZE, buttons.toArray(new AbstractButton[buttons.size()]));
	}

	protected void layoutDefault(JComponent centerComponent, AbstractButton ... buttons) {
		layoutDefault(centerComponent, DEFAULT_SIZE, buttons);
	}

	protected void layoutDefault(JComponent centerComponent, int size, AbstractButton ... buttons) {
		layoutDefault(centerComponent, makeButtonPanel(buttons), size);
	}

	protected void layoutDefault(final JComponent centerComponent, JPanel buttonPanel) {
		layoutDefault(centerComponent, buttonPanel, DEFAULT_SIZE);
	}

	protected void layoutDefault(final JComponent centerComponent, JPanel buttonPanel, int size) {
		this.centerComponent = centerComponent;
		setTitle(getDialogTitle());
		setLayout(new BorderLayout());
		add(makeInfoPanel(), BorderLayout.NORTH);
		if (centerComponent != null) {
			JPanel centerPanel = new JPanel(new BorderLayout());
			centerPanel.setBorder(BorderFactory.createEmptyBorder(0, GAP, 0, GAP));
			centerPanel.add(centerComponent, BorderLayout.CENTER);
			add(centerPanel, BorderLayout.CENTER);
		}
		add(buttonPanel, BorderLayout.SOUTH);
		this.addComponentListener(new ComponentListener() {

			@Override
			public void componentHidden(ComponentEvent e) {}

			@Override
			public void componentMoved(ComponentEvent e) {}

			@Override
			public void componentResized(ComponentEvent e) {
				if ((infoTextLabel != null) && (centerComponent != null)) {
					int prefHeightBefore = infoTextLabel.getPreferredSize().height;
					infoTextLabel.setWidth(centerComponent.getWidth() - 88);
					int prefHeightAfter = infoTextLabel.getPreferredSize().height;
					int heightDiff = prefHeightAfter - prefHeightBefore;
					if (heightDiff > 0) {
						// re-pack this dialog if the infoTextLabel has changed its prefHeight after the resize
						// fixes center component being overlapped/cut off
						ButtonDialog.this.pack();
					}
				}
			}

			@Override
			public void componentShown(ComponentEvent e) {}
		});
		switch (size) {
		case DEFAULT_SIZE:
			break;
		default:
			setPreferredSize(getDefaultSize(size));
			break;
		}
		pack();
		setDefaultLocation();
	}

	protected void setDefaultLocation() {
		setLocationRelativeTo(ApplicationFrame.getApplicationFrame());
	}

	protected void setDefaultSize() {
		setDefaultSize(NORMAL);
	}

	protected Dimension getDefaultSize(int size) {
		switch (size) {
		case NARROW:
			return new Dimension(360, 540);
		case NORMAL:
			return new Dimension(720, 540);
		case LARGE:
			return new Dimension(800, 600);
		case HUGE:
			// this dimension is too large for HD-ready displays and also for presentation resolutions
			// return the next smaller dimension instead to avoid components being too large for display
			if (RapidMinerGUI.getMainFrame() != null && RapidMinerGUI.getMainFrame().getGraphicsConfiguration() != null) {
				if (RapidMinerGUI.getMainFrame().getGraphicsConfiguration().getBounds().getHeight() < 801) {
					return getDefaultSize(LARGE);
				} else {
					return new Dimension(1000, 760);		
				}
			} else {
				if (Toolkit.getDefaultToolkit().getScreenSize().getHeight() < 801) {
					return getDefaultSize(LARGE);
				} else {
					return new Dimension(1000, 760);		
				}
			}
		case MESSAGE:
			return new Dimension(600, 200);
		case MESSAGE_EXTENDED:
			return new Dimension(600, 400);
		default:
			return new Dimension(420, 300);
		}
	}

	protected void setDefaultSize(int size) {
		if (size != DEFAULT_SIZE) {
			setPreferredSize(getDefaultSize(size));
		}
		pack();
	}

	protected JPanel makeButtonPanel(Collection<AbstractButton> buttons) {
		return makeButtonPanel(buttons.toArray(new AbstractButton[buttons.size()]));
	}

	protected JPanel makeButtonPanel(AbstractButton ... buttons) {
		JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, GAP, GAP));
		for (AbstractButton button : buttons) {
			if (button != null) {
				buttonPanel.add(button);
			}
		}
		return buttonPanel;
	}

	/** Will be default button. */
	protected JButton makeOkButton() {
		return makeOkButton("ok");
	}

	protected JButton makeOkButton(String i18nKey) {
		Action okAction = new ResourceAction(i18nKey) {
			private static final long serialVersionUID = 1L;
			@Override
			public void actionPerformed(ActionEvent e) {
				wasConfirmed = true;
				ok();
			}			
		};
		JButton button = new JButton(okAction);
		getRootPane().setDefaultButton(button);

		return button;
	}

	/** Will listen to ESCAPE. */
	protected JButton makeCancelButton() {
		return makeCancelButton("cancel");
	}

	protected JButton makeCancelButton(String i18nKey) {
		Action cancelAction = new ResourceAction(i18nKey) {
			private static final long serialVersionUID = 1L;
			@Override
			public void actionPerformed(ActionEvent e) {
				wasConfirmed = false;
				cancel();
			}			
		};
		getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0, false), "CANCEL");  
		getRootPane().getActionMap().put("CANCEL", cancelAction);
		return new JButton(cancelAction);
	}	

	/** Will be default button and listen to ESCAPE. */
	protected JButton makeCloseButton() {
		Action action = new ResourceAction("close") {
			private static final long serialVersionUID = 1L;
			@Override
			public void actionPerformed(ActionEvent e) {
				wasConfirmed = false;
				close();
			}			
		};
		getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0, false), "CLOSE");  
		getRootPane().getActionMap().put("CLOSE", action);
		JButton button = new JButton(action);
		getRootPane().setDefaultButton(button);
		return button;
	}

	protected void cancel() {
		dispose();
	}	

	protected void ok() {
		dispose();
	}

	protected void close() {
		dispose();
	}

	/** Returns true iff the user pressed the generated ok button. */
	public boolean wasConfirmed() {
		return wasConfirmed;
	}
	
	protected void setConfirmed(boolean b) {
		this.wasConfirmed = b;
	}

	public static TitledBorder createTitledBorder(String title) {
		TitledBorder border = new TitledBorder(createBorder(), title) {
			private static final long serialVersionUID = 3113821577644055057L;

			@Override
			public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
				super.paintBorder(c, g, x - EDGE_SPACING, y, width + 2 * EDGE_SPACING, height);
			}
		};
		return border;
	}

	public static Border createBorder() {
		return BorderFactory.createMatteBorder(1, 1, 1, 1, Color.LIGHT_GRAY);
	}
	
	public static GridLayout createGridLayout(int rows, int columns) {
		return new GridLayout(rows, columns, GAP, GAP);
	}
	
	public void addChangeListener(ChangeListener l) {
		listeners.add(l);
	}
	
	public void removeChangeListener(ChangeListener l) {
		listeners.remove(l);
	}
	
	protected void fireStateChanged() {
		ChangeEvent e = new ChangeEvent(this);
		for (ChangeListener l : listeners) {
			l.stateChanged(e);
		}
	}
}
