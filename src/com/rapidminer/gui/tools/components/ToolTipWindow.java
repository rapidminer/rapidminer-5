/*
 *  RapidMiner
 *
 *  Copyright (C) 2001-2014 by RapidMiner and the contributors
 *
 *  Complete list of developers available at our web site:
 *
 *       http://rapidminer.com
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

import java.awt.AWTEvent;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Insets;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.AWTEventListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.StyleSheet;

import com.rapidminer.gui.ApplicationFrame;
import com.rapidminer.gui.MainFrame;
import com.rapidminer.gui.tools.ExtendedHTMLJEditorPane;
import com.rapidminer.gui.tools.ProgressThread;
import com.rapidminer.gui.tools.ResourceLabel;
import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.repository.Entry;
import com.rapidminer.repository.IOObjectEntry;
import com.rapidminer.repository.RepositoryLocation;
import com.rapidminer.tools.RMUrlHandler;

/** This class manages dynamic largish tool tips for JComponents.
 *  In order to use this class, implement a {@link TipProvider} that generates
 *  tool tip texts depending on the a mouse position relative to a component and
 *  pass this component and the tip provider to the constructor of this class.
 * 
 *  This class will listen to mouse events of the specified component and will
 *  display an undecorated scrollable dialog whenever the mouse does not move for a certain time.
 *  The user can focus (an then resize) the dialog by pressing F3.
 * 
 * @author Simon Fischer
 *
 */
public class ToolTipWindow {

    private static final long serialVersionUID = 1L;

    public interface TipProvider {

        /** Returns the actual tip belonging to this point. Called after {@link #getIdUnder(Point)}. */
        public String getTip(Object id);

        /** Returns an additional tooltip component to be added below the text field. */
        public Component getCustomComponent(Object id);

        /** Returns an ID of the object under the given mouse position.
         *  This is only used to determine whether the mouse has left the area corresponding to the current
         *  tool tip. We could have called {@link #getTip(Object)} directly, however this may be a too time
         *  consuming operation.
         *  Note: IDs are compared by == ! */
        public Object getIdUnder(Point point);
    }

    private enum State {
        IDLE,
        SHOWING_TIP,
        IN_FOCUS,
        DISPOSED
    }

    /** Component observed by this object. */
    private final JComponent parent;

    private final TipProvider tipProvider;

    private State state = State.IDLE;

    /** Point at which the tip was last displayed. Relative to {@link #parent}. */
    private Point lastPoint;

    /** Mouse position when it was last moved. Relative to {@link #parent}. */
    private Point lastMousePosition;

    /** Position of the mouse at the point of time when the tip was displayed.*/
    private Point mousePositionAtPopup;

    private Object currentId;

    /** Panel containing the {@link #tipScrollPane} and a short label (F3 to focus). */
    private final JPanel mainPanel = new JPanel(new BorderLayout());

    /** Pane containing the help text. */
    private final ExtendedHTMLJEditorPane tipPane = new ExtendedHTMLJEditorPane("text/html", "<html></html>");
    private Component customComponent;

    /** Contains the {@link #tipPane}. */
    private final JScrollPane tipScrollPane;

    /** Current (decorated or undecorated) dialog containing the main panel. May be null if state is IDLE. */
    private JDialog currentDialog;

    /** Shows a tip after 500ms. */
    private final Timer showTipTimer = new Timer(500, new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
            showTip();
        }
    });
    
    /** We use this approach for tracking the mouse exited event from the dialog.
     * http://weblogs.java.net/blog/alexfromsun/archive/2006/09/a_wellbehaved_g.html
     * The method described in the Java Tutorial on using glasspanes described here:
     * http://java.sun.com/docs/books/tutorial/uiswing/components/rootpane.html
     * fails for various reasons. Most importantly, it makes the scroll bars unusable. */
    private AWTEventListener hideTipOnExitListener = new AWTEventListener() {
        @Override
        public void eventDispatched(AWTEvent event) {
            if (event instanceof MouseEvent) {
                MouseEvent me = (MouseEvent) event;
                if ((me.getID() != MouseEvent.MOUSE_EXITED) || (state != State.SHOWING_TIP) ||
                        !SwingUtilities.isDescendingFrom(me.getComponent(), currentDialog)) {
                    return;
                }
                Point origin = currentDialog.getLocationOnScreen();
                Point mep    = me.getLocationOnScreen();
                if (mep.getX() < origin.getX() ||
                        mep.getY() < origin.getY() ||
                        mep.getX() > origin.getX() + currentDialog.getWidth() ||
                        mep.getY() > origin.getY() + currentDialog.getHeight()) {
                    hideTip();
                }
            }
        }

    };

    //	/** Hides the tip after 25s. */
    //	private final Timer hideTipTimer = new Timer(25000, new ActionListener() {
    //		@Override
    //		public void actionPerformed(ActionEvent e) {
    //			hideTip();
    //		}
    //	});

    /** Decorates the tip dialog if the user presses F3. */
    private final Action FOCUS_TIP_ACTION = new AbstractAction() {
        private static final long serialVersionUID = 1L;
        @Override
        public void actionPerformed(ActionEvent e) {
            focusTip();
        }
    };

    private final ResourceLabel f3Label;

    private final Dialog owner;

    private boolean mouseOnParentIsDown = false;

    public ToolTipWindow(TipProvider tipProvider, JComponent parent) {
        this(null, tipProvider, parent);
        tipPane.setToolTipText(null);
    }

    /**
     * TODO: Is there a way to find the owner elegantly? Travers ancestors?
     * 
     * @param owner The owner of the tool tip dialog. If null, the {@link MainFrame} will be used.
     *    If this tool tip is for a component in a dialog, but the owner is not set, the tool tip
     *    will be displayed behind the dialog.
     * @param tipProvider Generates tool tip texts whenever needed
     * @param parent The component to observe
     */
    public ToolTipWindow(Dialog owner, TipProvider tipProvider, JComponent parent) {
        this.owner = owner;
        this.tipProvider = tipProvider;
        this.parent = parent;

        showTipTimer.setRepeats(false);
        //		hideTipTimer.setRepeats(false);

        tipPane.setFont(new Font("Sans-serif", Font.PLAIN, 9));
        tipPane.setMargin(new Insets(4, 4, 4, 4));
        tipPane.setEditable(false);
        tipPane.addHyperlinkListener(new HyperlinkListener() {
            @Override
            public void hyperlinkUpdate(HyperlinkEvent e) {
                if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                    //					if (e.getDescription().startsWith("operator?")) {
                    //						String opName = e.getDescription().substring("operator?".length());
                    //						MainFrame mainFrame = RapidMinerGUI.getMainFrame();
                    //						mainFrame.selectOperator(mainFrame.getProcess().getOperator(opName));
                    //					} else {
                    if (e.getDescription().startsWith("loadMetaData?")) {
                        final String loc = e.getDescription().substring("loadMetaData?".length());
                        final Object idAtTimeOfDownload = currentId;
                        tipPane.setText("<p>Please stand by...</p>");
                        final AtomicBoolean tipWasClosed = new AtomicBoolean(false);
                        currentDialog.addWindowListener(new WindowAdapter() {
                            @Override
                            public void windowClosed(WindowEvent e) {
                                tipWasClosed.set(true);
                            }
                        });
                        new ProgressThread("download_md_from_repository") {
                            @Override
                            public void run() {
                                getProgressListener().setTotal(100);
                                getProgressListener().setCompleted(10);
                                try {
                                    Entry entry = new RepositoryLocation(loc).locateEntry();
                                    if (entry instanceof IOObjectEntry) {
                                        ((IOObjectEntry) entry).retrieveMetaData();
                                    }
                                } catch (Exception e) {
                                    SwingTools.showSimpleErrorMessage("error_downloading_metadata", e, loc, e.getMessage());
                                } finally {
                                    getProgressListener().complete();
                                }
                                SwingUtilities.invokeLater(new Runnable() {
                                    @Override
                                    public void run() {
                                        if (!tipWasClosed.get()) {
                                            refreshDialogContents(idAtTimeOfDownload);
                                            autoAdjustDialogSize(state == State.IN_FOCUS);
                                            currentDialog.pack();
                                        }
                                    }
                                });
                            }
                        }.start();
                    } else {
                        RMUrlHandler.handleUrl(e.getDescription());
                    }

                }
            }
        });
        tipPane.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        StyleSheet css = ((HTMLEditorKit)tipPane.getEditorKit()).getStyleSheet();
        css.addRule("body {font-family:Sans;font-size:12pt}");
        css.addRule("h3 {margin:0; padding:0}");
        css.addRule("h4 {margin-bottom:0; margin-top:1ex; padding:0}");
        css.addRule("p  {margin-top:0; margin-bottom:1ex; padding:0}");
        css.addRule("ul {margin-top:0; margin-bottom:1ex; list-style-image: url(" + getClass().getResource("/com/rapidminer/resources/icons/modern/help/circle.png") + ")}");
        css.addRule("ul li {padding-bottom: 2px}");
        css.addRule("li.outPorts {padding-bottom: 0px}");
        css.addRule("ul li ul {margin-top:0; margin-bottom:1ex; list-style-image: url(" + getClass().getResource("/com/rapidminer/resources/icons/modern/help/line.png") + ")");
        css.addRule("li ul li {padding-bottom:0}");

        tipScrollPane = new JScrollPane(tipPane);
        tipScrollPane.setBorder(null);
        mainPanel.add(tipScrollPane, BorderLayout.CENTER);
        f3Label = new ResourceLabel("F3_for_focus");
        f3Label.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, Color.LIGHT_GRAY),
                BorderFactory.createEmptyBorder(0, 4, 0, 0)));
        mainPanel.add(f3Label, BorderLayout.SOUTH);

        //int focusCondition = JComponent.WHEN_IN_FOCUSED_WINDOW;
        int focusCondition = JComponent.WHEN_FOCUSED;
        parent.getInputMap(focusCondition).put(KeyStroke.getKeyStroke(KeyEvent.VK_F3, 0), "focusTip");
        parent.getActionMap().put("focusTip", FOCUS_TIP_ACTION);
        mainPanel.getInputMap(focusCondition).put(KeyStroke.getKeyStroke(KeyEvent.VK_F3, 0), "focusTip");
        mainPanel.getActionMap().put("focusTip", FOCUS_TIP_ACTION);
        tipPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_F3, 0), "focusTip");
        mainPanel.getActionMap().put("focusTip", FOCUS_TIP_ACTION);

        parent.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                mouseOnParentIsDown = true;
                showTipTimer.stop();
                if (state == State.SHOWING_TIP) {
                    hideTip();
                }
            }
            @Override
            public void mouseReleased(MouseEvent e) {
                mouseOnParentIsDown = false;
                if (state == State.IDLE) {
                    showTipTimer.start();
                }
            }
            @Override
            public void mouseExited(MouseEvent e) {
                mouseOnParentIsDown = false;
                showTipTimer.stop();
            }
            @Override
            public void mouseEntered(MouseEvent e) {
                mouseOnParentIsDown = false;
                if (state == State.IDLE) {
                    showTipTimer.start();
                }
            }
        });

        parent.addMouseMotionListener(new MouseMotionListener() {
            @Override
            public void mouseMoved(MouseEvent e) {
                parentIsActive(e);
            }
            @Override public void mouseDragged(MouseEvent e) {
                parentIsActive(e);
            }
        });
    }

    private void parentIsActive(MouseEvent e) {
        lastMousePosition = e.getPoint();
        switch (state) {
        case IDLE:
            if (!mouseOnParentIsDown ) {
                showTipTimer.restart();
            }
            break;
        case SHOWING_TIP:
            Object id = ToolTipWindow.this.tipProvider.getIdUnder(e.getPoint());
            if (id == currentId) {
                return;
            } else {
                double dx = e.getX() - mousePositionAtPopup.getX();
                double dy = e.getY() - mousePositionAtPopup.getY();
                double dist = dx*dx + dy*dy;
                if (dist > 100) {
                    hideTip();
                }
            }
            break;
        case DISPOSED:
            state = State.IDLE;
            break;
        }
    }

    private void makeDialog(boolean undecorated, Point point) {
        currentDialog = new JDialog(owner != null ? owner : ApplicationFrame.getApplicationFrame());
        if (undecorated) {
            //currentDialog.setModal(true);
            currentDialog.setUndecorated(true);
            f3Label.setVisible(true);
            currentDialog.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_F3, 0), "focusTip");
            currentDialog.getRootPane().getActionMap().put("focusTip", FOCUS_TIP_ACTION);
        } else {
            f3Label.setVisible(false);
        }

        currentDialog.getRootPane().setBorder(BorderFactory.createLineBorder(Color.BLACK));

        currentDialog.getContentPane().setLayout(new BorderLayout());
        currentDialog.getContentPane().add(mainPanel);
        SwingTools.setDialogIcon(currentDialog);
        // dispose focused if focus lost
        if (state == State.IN_FOCUS) {
            currentDialog.addWindowFocusListener(new WindowFocusListener() {
                @Override
                public void windowGainedFocus(WindowEvent e) { }
                @Override
                public void windowLostFocus(WindowEvent e) {
                    hideTip();
                }
            });
        }
        currentDialog.addWindowListener(new WindowAdapter() {
            @Override
            public void windowOpened(WindowEvent e) {
            	Toolkit.getDefaultToolkit().addAWTEventListener(hideTipOnExitListener, AWTEvent.MOUSE_MOTION_EVENT_MASK | AWTEvent.MOUSE_EVENT_MASK);
                if (state == State.SHOWING_TIP) {
                    tipScrollPane.getVerticalScrollBar().setValue(0);
                }
            }
            @Override
            public void windowClosed(WindowEvent e) {
            	Toolkit.getDefaultToolkit().removeAWTEventListener(hideTipOnExitListener);
                state = State.IDLE;
            }
        });

        autoAdjustDialogSize(undecorated);

        currentDialog.pack();
        if (undecorated) {
            currentDialog.setLocation(new Point((int)(parent.getLocationOnScreen().getX() + point.getX()),
                    (int)(parent.getLocationOnScreen().getY() + point.getY())));
        } else {
            Rectangle innerBounds = currentDialog.getComponent(0).getBounds();
            currentDialog.setLocation(new Point((int)(parent.getLocationOnScreen().getX() + point.getX() - innerBounds.x),
                    (int)(parent.getLocationOnScreen().getY() + point.getY() - innerBounds.y)));
            int dx = currentDialog.getSize().width - tipScrollPane.getSize().width;
            int dy = currentDialog.getSize().height - tipScrollPane.getSize().height;
            currentDialog.setPreferredSize(new Dimension(tipScrollPane.getPreferredSize().width + dx,
                    tipScrollPane.getPreferredSize().height + dy));
        }
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        Rectangle bounds = currentDialog.getBounds();
        if (bounds.getMaxX() > screenSize.getWidth()) {
            currentDialog.setLocation(new Point((int)(screenSize.getWidth() - bounds.getWidth()),
                    (int)(bounds.getY())));
            bounds = currentDialog.getBounds();
        }
        if (bounds.getMaxY() > screenSize.getHeight()) {
            currentDialog.setLocation(new Point((int)(bounds.getX()),
                    (int)(screenSize.getHeight() - bounds.getHeight())));
        }
        currentDialog.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0, false), "CLOSE");
        currentDialog.getRootPane().getActionMap().put("CLOSE", new AbstractAction() {
            private static final long serialVersionUID = 1373293026453738733L;

            @Override
            public void actionPerformed(ActionEvent e) {
                currentDialog.dispose();
            }
        });
        currentDialog.setVisible(true);
    }

    private void autoAdjustDialogSize(boolean undecorated) {
        int tipPaneHeight = (int)tipPane.getPreferredSize().getHeight();
        int tipPaneWidth  = (int)tipPane.getPreferredSize().getWidth();
        if (tipPaneHeight > 300) {
            tipScrollPane.setPreferredSize(new Dimension(tipPaneWidth + 50, 300 + (undecorated ? 0 : f3Label.getHeight())));
        } else {
            tipScrollPane.setPreferredSize(new Dimension(tipPaneWidth + 50, tipPaneHeight + 30 + (undecorated ? 0 : f3Label.getHeight())));
        }
    }

    private void showTip() {
        if (state == State.IDLE) {
            // if parent does not have focus we don't want to show tool tip!
            if (!parent.hasFocus()) {
                return;
            }
            // check if we are really under the mouse. Necessary since mouse exited events
            // might have been lost when dragging.
            if (!parent.isDisplayable()) {
                return;
            }
            Rectangle parentBounds = new Rectangle(parent.getLocationOnScreen(), new Dimension(parent.getWidth(), parent.getHeight()));
            if (!parentBounds.contains(MouseInfo.getPointerInfo().getLocation())) {
                return;
            }

            if (lastMousePosition == null) {
                return;
            }
            currentId = tipProvider.getIdUnder(lastMousePosition);
            if (currentId == null) {
                return;
            }

            refreshDialogContents(currentId);

            state = State.SHOWING_TIP;
            showTipTimer.stop();
            //hideTipTimer.start();
            lastPoint = new Point((int)lastMousePosition.getX()-50, (int)lastMousePosition.getY()+10);;
            mousePositionAtPopup = lastMousePosition;
            makeDialog(true, lastPoint);
            parent.requestFocus();
        } else {
            state = State.IDLE;
        }
    }

    private void focusTip() {
        if (state == State.SHOWING_TIP) {
            state = State.IN_FOCUS;
            //hideTipTimer.stop();
            currentDialog.dispose();
            currentDialog = null;
            makeDialog(false, lastPoint);
        }
    }

    private void hideTip() {
        if (currentDialog != null) {
            currentDialog.dispose();
            state = State.DISPOSED;
        }
    }

    private void refreshDialogContents(Object objectId) {
        String tipText = tipProvider.getTip(objectId);
        if ((tipText == null) || (tipText.length() == 0)) {
            return;
        }
        if (customComponent != null) {
            mainPanel.remove(customComponent);
        }
        mainPanel.remove(tipScrollPane);
        mainPanel.remove(tipPane);
        tipPane.setText("<html><body><div style=\"width:300px\">"+tipText+"</div></body></html>");
        if (customComponent != null) {
            mainPanel.remove(customComponent);
        }
        mainPanel.remove(tipPane);
        mainPanel.remove(tipScrollPane);
        customComponent = tipProvider.getCustomComponent(objectId);
        if (customComponent != null) {
            mainPanel.add(tipPane, BorderLayout.NORTH);
            mainPanel.add(customComponent, BorderLayout.CENTER);
        } else {
            tipScrollPane.setViewportView(tipPane);
            mainPanel.add(tipScrollPane, BorderLayout.CENTER);
        }
    }
}
