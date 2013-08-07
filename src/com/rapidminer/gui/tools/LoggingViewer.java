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
package com.rapidminer.gui.tools;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.SimpleFormatter;

import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JMenu;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.JToolBar;
import javax.swing.SwingUtilities;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;

import com.rapidminer.gui.MainFrame;
import com.rapidminer.gui.RapidMinerGUI;
import com.rapidminer.gui.dialog.SearchDialog;
import com.rapidminer.gui.dialog.SearchableJTextComponent;
import com.rapidminer.gui.tools.actions.ClearMessageAction;
import com.rapidminer.gui.tools.actions.LoggingSearchAction;
import com.rapidminer.gui.tools.actions.SaveLogFileAction;
import com.rapidminer.operator.ProcessRootOperator;
import com.rapidminer.parameter.ParameterTypeColor;
import com.rapidminer.parameter.UndefinedParameterError;
import com.rapidminer.tools.LogService;
import com.rapidminer.tools.ParameterService;
import com.vlsolutions.swing.docking.DockKey;
import com.vlsolutions.swing.docking.Dockable;


/**
 * A text area displaying the log output. All kinds of streams can be redirected
 * to this viewer by using an instance of a special inner stream subclass. The
 * message viewer highlights some text which contains keywords like
 * &quot;error&quot; or &quot;warning&quot;. Since keeping all lines might
 * dramatically increase memory usage and slow down RapidMiner, only a maximum number
 * of lines is displayed.
 * 
 * @author Ingo Mierswa
 */
public class LoggingViewer extends JPanel implements MouseListener, Dockable {

    private static final long serialVersionUID = 551259537624386372L;

    public static final Level[] SELECTABLE_LEVELS = {
        Level.ALL,
        Level.FINEST,
        Level.FINER,
        Level.FINE,
        Level.CONFIG,
        Level.INFO,
        Level.WARNING,
        Level.SEVERE,
        Level.OFF
    };
    public static final int DEFAULT_LEVEL_INDEX = 4;
    public static final String[] SELECTABLE_LEVEL_NAMES = new String[SELECTABLE_LEVELS.length];
    static {
        for (int i = 0; i < SELECTABLE_LEVELS.length; i++) {
            SELECTABLE_LEVEL_NAMES[i] = SELECTABLE_LEVELS[i].getName();
        }
    }

    public transient final Action CLEAR_MESSAGE_VIEWER_ACTION = new ClearMessageAction(this);

    public transient final Action SAVE_LOGFILE_ACTION = new SaveLogFileAction(this);

    public transient final Action SEARCH_ACTION = new LoggingSearchAction(this);

    public transient final JMenu LEVEL_MENU = new LoggingLevelMenu(this);

    private transient final SimpleAttributeSet attributeSet = new SimpleAttributeSet();

    private transient final LinkedList<Integer> lineLengths = new LinkedList<Integer>();

    private final JTextPane textArea;

    private static final Color COLOR_DEFAULT;
    private static final Color COLOR_WARNING;
    private static final Color COLOR_ERROR;
    private static final Color COLOR_INFO;
    static {
        String colorStr = ParameterService.getParameterValue(MainFrame.PROPERTY_RAPIDMINER_GUI_MESSAGEVIEWER_HIGHLIGHT_LOGSERVICE);
        if (colorStr != null) {
            COLOR_DEFAULT = ParameterTypeColor.string2Color(colorStr);
        } else {
            COLOR_DEFAULT = Color.BLACK;
        }
        colorStr = ParameterService.getParameterValue(MainFrame.PROPERTY_RAPIDMINER_GUI_MESSAGEVIEWER_HIGHLIGHT_NOTES);
        if (colorStr != null) {
            COLOR_INFO = ParameterTypeColor.string2Color(colorStr);
        } else {
            COLOR_INFO = Color.BLACK;
        }
        colorStr = ParameterService.getParameterValue(MainFrame.PROPERTY_RAPIDMINER_GUI_MESSAGEVIEWER_HIGHLIGHT_WARNINGS);
        if (colorStr != null) {
            COLOR_WARNING= ParameterTypeColor.string2Color(colorStr);
        } else {
            COLOR_WARNING  = Color.BLACK;
        }
        colorStr = ParameterService.getParameterValue(MainFrame.PROPERTY_RAPIDMINER_GUI_MESSAGEVIEWER_HIGHLIGHT_ERRORS);
        if (colorStr != null) {
            COLOR_ERROR = ParameterTypeColor.string2Color(colorStr);
        } else {
            COLOR_ERROR = Color.BLACK;
        }
    }

    private final Formatter formatter =    
    	new SimpleFormatter() {
        @Override
        public String format(LogRecord record) {        	
            StringBuilder b = new StringBuilder();
            //			b.append(record.getLoggerName());
            //			b.append(": ");
            b.append(DateFormat.getDateTimeInstance().format(new Date(record.getMillis())));
            b.append(" ");
            b.append(record.getLevel().getLocalizedName());
            b.append(": ");
            //b.append(record.getMessage());
            b.append(formatMessage(record));
            b.append("\n");
            return b.toString();
        }

    };

    private int maxRows;

    private final Handler handler = new Handler() {
        @Override
        public void close() throws SecurityException {
        }
        @Override
        public void flush() {
        }
        @Override
        public void publish(final LogRecord record) {
            if (isLoggable(record)) {
                if (SwingUtilities.isEventDispatchThread()) {
                    append(record);
                } else {
                    SwingUtilities.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            append(record);
                        }

                    });
                }
            }
        }
    };

    public LoggingViewer() {
        this(new JTextPane());
    }

    private LoggingViewer(JTextPane textArea) {
        super(new BorderLayout());
        final Level level = getSpecifiedLogLevelIndex();
        handler.setLevel(level);
        LogService.getRoot().setLevel(level);
        maxRows = 1000;
        try {
            String maxRowsString = ParameterService.getParameterValue(MainFrame.PROPERTY_RAPIDMINER_GUI_MESSAGEVIEWER_ROWLIMIT);
            if (maxRowsString != null)
                maxRows = Integer.parseInt(maxRowsString);
        } catch (NumberFormatException e) {
            //LogService.getGlobal().log("Bad integer format for property '', using default number of maximum rows for logging (1000).", LogService.WARNING);
            LogService.getRoot().log(Level.WARNING, "com.rapidminer.gui.tools.LoggingViewer.bad_integer_format_for_property");           
        }

        this.textArea = textArea;
        this.textArea.setToolTipText("Displays logging messages according to the current log verbosity (parameter of root operator).");
        this.textArea.setEditable(false);
        this.textArea.addMouseListener(this);
        this.textArea.setFont(this.textArea.getFont().deriveFont(Font.PLAIN));
        LogService.getRoot().addHandler(handler);

        JToolBar toolBar = new ExtendedJToolBar();
        toolBar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Color.LIGHT_GRAY));
        toolBar.add(SAVE_LOGFILE_ACTION);
        toolBar.add(CLEAR_MESSAGE_VIEWER_ACTION);
        toolBar.add(SEARCH_ACTION);
        add(toolBar, BorderLayout.NORTH);
        JScrollPane scrollPane = new ExtendedJScrollPane(textArea);
        scrollPane.setBorder(null);
        add(scrollPane, BorderLayout.CENTER);
    }

    private static Level getSpecifiedLogLevelIndex() {
        String value = ParameterService.getParameterValue(MainFrame.PROPERTY_RAPIDMINER_GUI_LOG_LEVEL);
        if (value == null) {
            return Level.CONFIG;
        } else {
            for (int i = 0; i < SELECTABLE_LEVEL_NAMES.length; i++) {
                if (SELECTABLE_LEVEL_NAMES[i].equals(value)) {
                    return SELECTABLE_LEVELS[i];
                }
            }
            return Level.CONFIG;
        }
    }

    public void setLevel(Level level) {
        LogService.getRoot().setLevel(level);
        handler.setLevel(level);
        ParameterService.setParameterValue(MainFrame.PROPERTY_RAPIDMINER_GUI_LOG_LEVEL, level.getName());
        ParameterService.saveParameters();
    }

    protected Object readResolve() {
        return this;
    }

    @Override
    public void mouseEntered(MouseEvent e) {}

    @Override
    public void mouseExited(MouseEvent e) {}

    @Override
    public void mouseClicked(MouseEvent e) {}

    @Override
    public void mouseReleased(MouseEvent e) {
        evaluatePopup(e);
    }

    @Override
    public void mousePressed(MouseEvent e) {
        evaluatePopup(e);
    }

    private void evaluatePopup(MouseEvent e) {
        if (e.isPopupTrigger()) {
            createPopupMenu().show(textArea, e.getX(), e.getY());
        }
    }

    private JPopupMenu createPopupMenu() {
        JPopupMenu menu = new JPopupMenu();
        menu.add(CLEAR_MESSAGE_VIEWER_ACTION);
        menu.add(SAVE_LOGFILE_ACTION);
        menu.add(SEARCH_ACTION);
        menu.add(LEVEL_MENU);
        return menu;
    }

    private synchronized void append(LogRecord record) {
        Document doc = textArea.getStyledDocument();
        String formatted = formatter.format(record);

        if (record.getLevel().intValue() >= Level.SEVERE.intValue()) {
            StyleConstants.setForeground(attributeSet, COLOR_ERROR);
            StyleConstants.setBold(attributeSet, true);
        } else if (record.getLevel().intValue() >= Level.WARNING.intValue()) {
            StyleConstants.setForeground(attributeSet, COLOR_WARNING);
            StyleConstants.setBold(attributeSet, true);
        } else if (record.getLevel().intValue() >= Level.INFO.intValue()) {
            StyleConstants.setForeground(attributeSet, COLOR_INFO);
            StyleConstants.setBold(attributeSet, false);
        } else {
            StyleConstants.setForeground(attributeSet, COLOR_DEFAULT);
            StyleConstants.setBold(attributeSet, false);
        }

        try {
            doc.insertString(doc.getLength(), formatted, attributeSet);
        } catch (BadLocationException e) {
            // cannot happen
            // rather dump to stderr than logging and having this method called back
            e.printStackTrace();
        }

        if (maxRows >= 0) {
            int removeLength = 0;
            while (lineLengths.size() > maxRows) {
                removeLength += lineLengths.removeFirst();
            }
            try {
                doc.remove(0, removeLength);
            } catch (BadLocationException e) {
                SwingTools.showSimpleErrorMessage("error_during_logging", e);
            }
        }
        textArea.setCaretPosition(textArea.getDocument().getLength());
    }


    public String getLogMessage() {
        return textArea.getText();
    }

    public void clear() {
        textArea.setText("");
    }

    public void saveLog() {
        File file = new File("." + File.separator);
        String logFile = null;
        try {
            logFile = RapidMinerGUI.getMainFrame().getProcess().getRootOperator().getParameterAsString(ProcessRootOperator.PARAMETER_LOGFILE);
        } catch (UndefinedParameterError ex) {
            // tries to use process file name for initialization
        }
        if (logFile != null) {
            file = RapidMinerGUI.getMainFrame().getProcess().resolveFileName(logFile);
        }
        file = SwingTools.chooseFile(RapidMinerGUI.getMainFrame(), file, false, "log", "log file");
        if (file != null) {
            PrintWriter out = null;
            try {
                out = new PrintWriter(new FileWriter(file));
                out.println(textArea.getText());
            } catch (IOException ex) {
                SwingTools.showSimpleErrorMessage("cannot_write_log_file", ex);
            } finally {
                if (out != null) {
                    out.close();
                }
            }
        }
    }

    public void performSearch() {
        new SearchDialog(textArea, new SearchableJTextComponent(textArea)).setVisible(true);
    }

    public static final String LOG_VIEWER_DOCK_KEY = "log_viewer";

    private final DockKey DOCK_KEY = new ResourceDockKey(LOG_VIEWER_DOCK_KEY);
    {
        DOCK_KEY.setDockGroup(MainFrame.DOCK_GROUP_ROOT);
    }

    @Override
    public Component getComponent() {
        return this;
    }

    @Override
    public DockKey getDockKey() {
        return DOCK_KEY;
    }
}
