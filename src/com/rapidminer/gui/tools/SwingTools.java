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

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.GradientPaint;
import java.awt.Image;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.logging.Level;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileSystemView;

import com.rapidminer.RapidMiner;
import com.rapidminer.gui.MainFrame;
import com.rapidminer.gui.RapidMinerGUI;
import com.rapidminer.gui.look.fc.Bookmark;
import com.rapidminer.gui.look.fc.BookmarkIO;
import com.rapidminer.gui.tools.dialogs.ConfirmDialog;
import com.rapidminer.gui.tools.dialogs.ErrorDialog;
import com.rapidminer.gui.tools.dialogs.ExtendedErrorDialog;
import com.rapidminer.gui.tools.dialogs.InputDialog;
import com.rapidminer.gui.tools.dialogs.LongMessageDialog;
import com.rapidminer.gui.tools.dialogs.MessageDialog;
import com.rapidminer.gui.tools.dialogs.RepositoryEntryInputDialog;
import com.rapidminer.gui.tools.dialogs.ResultViewDialog;
import com.rapidminer.gui.tools.dialogs.SelectionInputDialog;
import com.rapidminer.gui.tools.syntax.SyntaxStyle;
import com.rapidminer.gui.tools.syntax.SyntaxUtilities;
import com.rapidminer.gui.tools.syntax.TextAreaDefaults;
import com.rapidminer.gui.tools.syntax.Token;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.ports.Port;
import com.rapidminer.operator.ports.Ports;
import com.rapidminer.tools.FileSystemService;
import com.rapidminer.tools.I18N;
import com.rapidminer.tools.LogService;
import com.rapidminer.tools.ParameterService;
import com.rapidminer.tools.ParentResolvingMap;
import com.rapidminer.tools.StringColorMap;
import com.rapidminer.tools.Tools;


/**
 * This helper class provides some static methods and properties which might be
 * useful for several GUI classes. These methods include
 * <ul>
 * <li>the creation of gradient paints</li>
 * <li>displaying (simple) error messages</li>
 * <li>creation of file chosers</li>
 * <li>creation of text panels</li>
 * <li>escaping HTML messages</li>
 * </ul>
 * 
 * @author Ingo Mierswa
 */
public class SwingTools {


    /** Defines the maximal length of characters in a line of the tool tip text. */
    private static final int TOOL_TIP_LINE_LENGTH = 100;

    /** Defines the extra height for each row in a table. */
    public static final int TABLE_ROW_EXTRA_HEIGHT = 4;

    /** Defines the extra height for rows in a table with components. If an
     *  {@link ExtendedJTable} is used, this amount can be added additionally
     *  to the amount of {@link #TABLE_ROW_EXTRA_HEIGHT} which is already
     *  added in the constructor. */
    public static final int TABLE_WITH_COMPONENTS_ROW_EXTRA_HEIGHT = 10;


    /** Some color constants for Java Look and Feel. */
    public static final Color DARKEST_YELLOW = new Color(250, 219, 172);

    /** Some color constants for Java Look and Feel. */
    public static final Color DARK_YELLOW = new Color(250, 226, 190);

    /** Some color constants for Java Look and Feel. */
    public static final Color LIGHT_YELLOW = new Color(250, 233, 207);

    /** Some color constants for Java Look and Feel. */
    public static final Color LIGHTEST_YELLOW = new Color(250, 240, 225);

    /** Some color constants for Java Look and Feel. */
    public static final Color TRANSPARENT_YELLOW = new Color(255, 245, 230, 190);

    /** Some color constants for Java Look and Feel. */
    public static final Color VERY_DARK_BLUE = new Color(172, 172, 212);

    /** Some color constants for Java Look and Feel. */
    public static final Color DARKEST_BLUE = new Color(182, 202, 242);

    /** Some color constants for Java Look and Feel. */
    public static final Color DARK_BLUE = new Color(199, 213, 242);

    /** Some color constants for Java Look and Feel. */
    public static final Color LIGHT_BLUE = new Color(216, 224, 242);

    /** Some color constants for Java Look and Feel. */
    public static final Color LIGHTEST_BLUE = new Color(233, 236, 242);

    /** The Rapid-I orange color. */
    public static final Color RAPID_I_ORANGE = new Color(242, 146, 0);

    /** The Rapid-I brown color. */
    public static final Color RAPID_I_BROWN = new Color(97, 66, 11);

    /** The Rapid-I beige color. */
    public static final Color RAPID_I_BEIGE = new Color(202, 188, 165);

    /** Some color constants for Java Look and Feel. */
    public static final Color LIGHTEST_RED = new Color(250, 210, 210);

    /** A brown font color. */
    public static final Color BROWN_FONT_COLOR = new Color(63,53,24);

    /** A brown font color. */
    public static final Color LIGHT_BROWN_FONT_COLOR = new Color(113,103,74);

    /** This set stores all lookup paths for icons */
    private static Set<String> iconPaths = new LinkedHashSet<String>(Collections.singleton("icons/"));

    /** Contains the small frame icons in all possible sizes. */
    private static List<Image> allFrameIcons = new LinkedList<Image>();

    private static FrameIconProvider frameIconProvider;

    private static final String DEFAULT_FRAME_ICON_BASE_NAME = "rapidminer_frame_icon_";

    private static ParentResolvingMap<String,Color> GROUP_TO_COLOR_MAP = new StringColorMap();

    static {
        setupFrameIcons(DEFAULT_FRAME_ICON_BASE_NAME);
        try {
            GROUP_TO_COLOR_MAP.parseProperties("com/rapidminer/resources/groups.properties", "group.", ".color", OperatorDescription.class.getClassLoader());
        } catch (IOException e) {
            //LogService.getRoot().warning("Cannot load operator group colors.");
            LogService.getRoot().log(Level.WARNING, "com.rapidminer.gui.tools.SwingTools.loading_operator_group_colors_error");
        }
    }

    public static void setFrameIconProvider(FrameIconProvider _frameIconProvider) {
        frameIconProvider = _frameIconProvider;
        reloadFrameIcons();
    }

    public static void setupFrameIcons(String frameIconBaseName) {
        setFrameIconProvider(new DefaultFrameIconProvider(frameIconBaseName));
        reloadFrameIcons();
    }

    private static void reloadFrameIcons() {
        if (frameIconProvider == null) {
            allFrameIcons = new LinkedList<Image>();
        } else {
            allFrameIcons = frameIconProvider.getFrameIcons();
        }
    }

    /**
     * Returns the list of all available program icon sizes.
     */
    public static List<Image> getFrameIconList() {
        return allFrameIcons;
    }

    /** Returns the list of all possible frame icons. */
    public static void setFrameIcon(JFrame frame) {
        Method iconImageMethod = null;
        try {
            iconImageMethod = frame.getClass().getMethod("setIconImages", new Class[] { List.class });
        } catch (Throwable e) {
            // ignore this and use single small icon below
        }

        if (iconImageMethod != null) {
            try {
                iconImageMethod.invoke(frame, new Object[] { allFrameIcons });
            } catch (Throwable e) {
                // ignore this and use single small icon
                if (allFrameIcons.size() > 0)
                    frame.setIconImage(allFrameIcons.get(0));
            }
        } else {
            if (allFrameIcons.size() > 0)
                frame.setIconImage(allFrameIcons.get(0));
        }
    }

    /** Returns the list of all possible frame icons. */
    public static void setDialogIcon(JDialog dialog) {
        Method iconImageMethod = null;
        try {
            iconImageMethod = dialog.getClass().getMethod("setIconImages", new Class[] { List.class });
        } catch (Throwable e) {
            // ignore this and use no icons or parent icon
        }

        if (iconImageMethod != null) {
            try {
                iconImageMethod.invoke(dialog, new Object[] { allFrameIcons });
            } catch (Throwable e) {
                // ignore this and use no or parent icon
            }
        }
    }

    /** Creates a red gradient paint. */
    public static GradientPaint makeRedPaint(double width, double height) {
        return new GradientPaint(0f, 0f, new Color(200,50,50), (float) width / 2, (float) height / 2, new Color(255,100,100), true);
    }

    /** Creates a blue gradient paint. */
    public static GradientPaint makeBluePaint(double width, double height) {
        return new GradientPaint(0f, 0f, LIGHT_BLUE, (float) width / 2, (float) height / 2, LIGHTEST_BLUE, true);
    }

    /** Creates a yellow gradient paint. */
    public static GradientPaint makeYellowPaint(double width, double height) {
        return new GradientPaint(0f, 0f, LIGHT_YELLOW, (float) width / 2, (float) height / 2, LIGHTEST_YELLOW, true);
    }

    private static final Map<String,ImageIcon> ICON_CACHE = new HashMap<String,ImageIcon>();

    private static final Object ICON_LOCK = new Object();

    /** Tries to load the icon for the given resource. Returns null (and writes a warning) if the
     *  resource file cannot be loaded. This method automatically adds all icon paths specified since startup
     *  time. The default /icons is always searched. Additional paths might be specified by {@link SwingTools#addIconStoragePath(String)}.
     * 
     *  The given names must contain '/' instead of backslashes! */
    public static ImageIcon createIcon(String iconName) {
        if (RapidMiner.getExecutionMode().isHeadless()) {
            return null;
        }
        for (String path: iconPaths) {
            ImageIcon icon = createImage(path + iconName);
            if (icon != null)
                return icon;
        }
        return null;
    }

    /**
     * This method returns the path of the icon given.
     * @param iconName
     * @return
     */
    public static String getIconPath(String iconName) {
        if (RapidMiner.getExecutionMode().isHeadless()) {
            return null;
        }
        for (String path: iconPaths) {
            ImageIcon icon = createImage(path + iconName);
            if (icon != null)
                return Tools.getResource(path + iconName).toString();
        }
        return null;
    }

    /**
     * This method adds a path to the set of paths which are searched for icons if
     * the {@link SwingTools#createIcon(String)} is called.
     */
    public static void addIconStoragePath(String path) {
        if (path.startsWith("/"))
            path = path.substring(1);
        if (!path.endsWith("/"))
            path = path + "/";
        iconPaths.add(path);
    }

    /** Tries to load the image for the given resource. Returns null (and writes a warning) if the
     *  resource file cannot be loaded. */
    public static ImageIcon createImage(String imageName) {
        if (RapidMiner.getExecutionMode().isHeadless()) {
            return null;
        }
        synchronized (ICON_LOCK) {
            if (ICON_CACHE.containsKey(imageName)) {
                return ICON_CACHE.get(imageName);
            } else {
                URL url = Tools.getResource(imageName);
                if (url != null) {
                    ImageIcon icon = new ImageIcon(url);
                    ICON_CACHE.put(imageName, icon);
                    return icon;
                } else {
                    //LogService.getRoot().fine("Cannot load image '" + imageName + "': icon will not be displayed");
                    LogService.getRoot().log(Level.FINE, "com.rapidminer.gui.tools.SwingTools.loading_image_error");
                    return null;
                }
            }
        }
    }

    public static void loadIcons() {
        ResourceBundle guiBundle = I18N.getGUIBundle();
        Enumeration<String> e = guiBundle.getKeys();
        while (e.hasMoreElements()) {
            String key = e.nextElement();
            if (key.endsWith(".icon")) {
                String resource = guiBundle.getString(key);
                if (!resource.isEmpty()) {
                    if (Character.isDigit(resource.charAt(0))) {
                        // We start with a number, size explicitly stated, so load directly
                        createIcon(resource);
                    } else {
                        // Otherwise prepend sizes
                        createIcon("16/"+resource);
                        createIcon("24/"+resource);
                    }
                }
            }
        }
    }

    /** This method transforms the given tool tip text into HTML. Lines are splitted at linebreaks
     *  and additional line breaks are added after ca. {@link #TOOL_TIP_LINE_LENGTH} characters. */
    public static String transformToolTipText(String description) {
        return transformToolTipText(description, true, TOOL_TIP_LINE_LENGTH);
    }

    /** This method transforms the given tool tip text into HTML. Lines are splitted at linebreaks
     *  and additional line breaks are added after ca. {@link #TOOL_TIP_LINE_LENGTH} characters.
     *  @param escapeSlashes Inidicates if forward slashes ("/") are escaped by the html code "&#47;"
     */
    public static String transformToolTipText(String description, boolean escapeSlashes) {
        return transformToolTipText(description, true, TOOL_TIP_LINE_LENGTH, escapeSlashes);
    }

    /** This method transforms the given tool tip text into HTML. Lines are splitted at linebreaks
     *  and additional line breaks are added after ca. {@link #TOOL_TIP_LINE_LENGTH} characters.
     *  @param escapeSlashes Inidicates if forward slashes ("/") are escaped by the html code "&#47;"
     *  @param escapeHTML Indicates if previously added html tags are escaped
     */
    public static String transformToolTipText(String description, boolean escapeSlashes, boolean escapeHTML) {
        return transformToolTipText(description, true, TOOL_TIP_LINE_LENGTH, escapeSlashes, escapeHTML);
    }
    
    public static String transformToolTipText(String description, boolean addHTMLTags, int lineLength) {
    	return transformToolTipText(description, addHTMLTags, lineLength, false);
    }
    
    /** This method transforms the given tool tip text into HTML. Lines are splitted at linebreaks
     *  and additional line breaks are added after ca. lineLength characters.
     *  @param escapeSlashes Inidicates if forward slashes ("/") are escaped by the html code "&#47;"
     */
    public static String transformToolTipText(String description, boolean addHTMLTags, int lineLength, boolean escapeSlashes) {
    	return transformToolTipText(description, addHTMLTags, lineLength, escapeSlashes, true);
    }
    
    
    /** This method transforms the given tool tip text into HTML. Lines are splitted at linebreaks
     *  and additional line breaks are added after ca. lineLength characters.
     *  @param escapeSlashes Inidicates if forward slashes ("/") are escaped by the html code "&#47;"
     *  @param escapeHTML Indicates if previously added html tags are escaped
     *  TODO: Use <div style="width:XXXpx"> */
    public static String transformToolTipText(String description, boolean addHTMLTags, int lineLength, boolean escapeSlashes, boolean escapeHTML) {
    	String completeText = description.trim();
    	if (escapeHTML) {
        	completeText = Tools.escapeHTML(completeText);
        }
        if (escapeSlashes) {
        	completeText = completeText.replaceAll("/", "&#47;");
        }
        StringBuffer result = new StringBuffer();
        if (addHTMLTags)
            result.append("<html>");
        // line.separator does not work here (transform and use \n)
        completeText = Tools.transformAllLineSeparators(completeText);
        String[] lines = completeText.split("\n");
        for (String text : lines) {
            boolean first = true;
            while (text.length() > lineLength) {
                int spaceIndex = text.indexOf(" ", lineLength);
                if (!first) {
                    result.append("<br>");
                }
                first = false;
                if (spaceIndex >= 0) {
                    result.append(text.substring(0, spaceIndex));
                    text = text.substring(spaceIndex + 1);
                } else {
                    result.append(text);
                    text = "";
                }
            }
            if (!first && text.length() > 0) {
                result.append("<br>");
            }
            result.append(text);
            result.append("<br>");
        }
        if (addHTMLTags)
            result.append("</html>");
        return result.toString();
    }

    //	/** Transforms the given class name array into a comma separated string of the short class names. */
    //	public static String getStringFromClassArray(Class[] classes) {
    //		StringBuffer outputString = new StringBuffer();
    //		if (classes == null)
    //			outputString.append("none");
    //		else {
    //			for (int i = 0; i < classes.length; i++) {
    //				if (i != 0)
    //					outputString.append(", ");
    //				outputString.append(Tools.classNameWOPackage(classes[i]));
    //			}
    //		}
    //		if (outputString.length() == 0)
    //			outputString.append("none");
    //		return outputString.toString();
    //	}

    /** Adds line breaks after {@link #TOOL_TIP_LINE_LENGTH} letters. */
    public static String addLinebreaks(String message) {
        if (message == null)
            return null;
        String completeText = message.trim();
        StringBuffer result = new StringBuffer();
        // line.separator does not work here (transform and use \n)
        completeText = Tools.transformAllLineSeparators(completeText);
        String[] lines = completeText.split("\n");
        for (String text : lines) {
            boolean first = true;
            while (text.length() > TOOL_TIP_LINE_LENGTH) {
                int spaceIndex = text.indexOf(" ", TOOL_TIP_LINE_LENGTH);
                if (!first) {
                    result.append(Tools.getLineSeparator());
                }
                first = false;
                if (spaceIndex >= 0) {
                    result.append(text.substring(0, spaceIndex));
                    text = text.substring(spaceIndex + 1);
                } else {
                    result.append(text);
                    text = "";
                }
            }
            if (!first && text.length() > 0) {
                result.append(Tools.getLineSeparator());
            }
            result.append(text);
            result.append(Tools.getLineSeparator());
        }
        return result.toString();
    }

    /**
     * The key will be used for the properties gui.dialog.-key-.title and
     * gui.dialog.results.-key-.icon
     */
    public static void showResultsDialog(final String i18nKey, JComponent results, Object...i18nArgs) {
        ResultViewDialog dialog = new ResultViewDialog(i18nKey, results, i18nArgs);
        dialog.setVisible(true);
    }

    /**
     * The key will be used for the properties gui.dialog.-key-.title and
     * gui.dialog.message.-key-.icon
     */
    public static void showMessageDialog(final String key, Object...keyArguments) {
        MessageDialog dialog = new MessageDialog(key, keyArguments);
        dialog.setVisible(true);
    }

    /**
     * The key will be used for the properties gui.dialog.-key-.title and
     * gui.dialog.message.-key-.icon
     */
    public static void showMessageDialog(final String key, JComponent component, Object...keyArguments) {
        MessageDialog dialog = new MessageDialog(key, component, keyArguments);
        dialog.setVisible(true);
    }

    /**
     * The key will be used for the properties gui.dialog.-key-.title and
     * gui.dialog.confirm.-key-.icon
     * 
     * See {@link ConfirmDialog} for details on the mode options.
     */
    public static int showConfirmDialog(final String key, int mode, Object...keyArguments) {
        ConfirmDialog dialog = new ConfirmDialog(key, mode, false, keyArguments);
        dialog.setVisible(true);
        return dialog.getReturnOption();
    }

    /**
     * This method will present a dialog to enter a text. This text will be returned
     * if the user confirmed the edit. Otherwise null is returned.
     * The key will be used for the properties gui.dialog.input.-key-.title, gui.dialog.input.-key-.message and
     * gui.dialog.input.-key-.icon
     */
    public static String showInputDialog(final String key, String text, Object...keyArguments) {
        InputDialog dialog = new InputDialog(key, text, keyArguments);
        dialog.setVisible(true);
        if (dialog.wasConfirmed()) {
            return dialog.getInputText();
        } else {
            return null;
        }
    }
    
    /**
     * This method will present a repository entry dialog to enter a text. This text will be returned
     * if the user confirmed the edit. Otherwise null is returned. Prevents invalid repository names.
     * The key will be used for the properties gui.dialog.input.-key-.title, gui.dialog.input.-key-.message and
     * gui.dialog.input.-key-.icon
     */
    public static String showRepositoryEntryInputDialog(final String key, String text, Object...keyArguments) {
        RepositoryEntryInputDialog dialog = new RepositoryEntryInputDialog(key, text, keyArguments);
        dialog.setVisible(true);
        if (dialog.wasConfirmed()) {
            return dialog.getInputText();
        } else {
            return null;
        }
    }

    /**
     * The key will be used for the properties gui.dialog.-key-.title and
     * gui.dialog.input.-key-.icon
     */
    public static Object showInputDialog(final String key, Object[] selectionValues, Object initialSelectionVale, final Object...keyArguments) {
        SelectionInputDialog dialog = new SelectionInputDialog(key, selectionValues, initialSelectionVale, keyArguments);
        dialog.setVisible(true);
        if (dialog.wasConfirmed()) {
            return dialog.getInputSelection();
        } else {
            return null;
        }
    }

    /**
     * This will open a simple input dialog, where a comboBox presents the given values. The Combobox might be editable depending
     * on parameter setting.
     * 
     * The key will be used for the properties gui.dialog.-key-.title and
     * gui.dialog.input.-key-.icon
     */
    public static Object showInputDialog(final String key, boolean editable, Object[] selectionValues, Object initialSelectionVale, final Object...keyArguments) {
        SelectionInputDialog dialog = new SelectionInputDialog(key, editable, selectionValues, initialSelectionVale, keyArguments);
        dialog.setVisible(true);
        if (dialog.wasConfirmed()) {
            return dialog.getInputSelection();
        } else {
            return null;
        }
    }

    /**
     * Shows a very simple error message without any Java exception hints.
     * 
     * @param key						the I18n-key which will be used to display the internationalized message
     * @param arguments					additional arguments for the internationalized message, which replace <code>{0}</code>, <code>{1}</code>, etcpp.
     */
    public static void showVerySimpleErrorMessage(final String key, final Object... arguments) {
        if (SwingUtilities.isEventDispatchThread()) {
            ErrorDialog dialog = new ErrorDialog(key, arguments);
            dialog.setModal(true);
            dialog.setVisible(true);
        } else {
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    ErrorDialog dialog = new ErrorDialog(key, arguments);
                    dialog.setModal(true);
                    dialog.setVisible(true);
                }
            });
        }
    }

    public static void showVerySimpleErrorMessageAndWait(final String key, final Object... arguments) {
        if (SwingUtilities.isEventDispatchThread()) {
            ErrorDialog dialog = new ErrorDialog(key, arguments);
            dialog.setModal(true);
            dialog.setVisible(true);
        } else {
            try {
				SwingUtilities.invokeAndWait(new Runnable() {
				    @Override
				    public void run() {
				        ErrorDialog dialog = new ErrorDialog(key, arguments);
				        dialog.setModal(true);
				        dialog.setVisible(true);
				    }
				});
			} catch (InvocationTargetException e) {
				LogService.getRoot().log(Level.WARNING, "Error showing error message: "+e, e);
			} catch (InterruptedException e) {}
        }
    }
    /**
     * This is the normal method which could be used by GUI classes for errors caused by
     * some exception (e.g. IO issues). Of course these error message methods should never be
     * invoked by operators or similar.
     * 
     * @param key						the I18n-key which will be used to display the internationalized message
     * @param e							the exception associated to this message
     * @param arguments					additional arguments for the internationalized message, which replace <code>{0}</code>, <code>{1}</code>, etcpp.
     */
    public static void showSimpleErrorMessage(final String key, final Throwable e, final Object... arguments) {
        showSimpleErrorMessage(key, e, true, arguments);
    }

    /**
     * This is the normal method which could be used by GUI classes for errors caused by
     * some exception (e.g. IO issues). Of course these error message methods should never be
     * invoked by operators or similar.
     * 
     * @param key						the I18n-key which will be used to display the internationalized message
     * @param e							the exception associated to this message
     * @param displayExceptionMessage	indicates if the exception message will be displayed in the dialog or just in the detailed panel
     * @param arguments					additional arguments for the internationalized message, which replace <code>{0}</code>, <code>{1}</code>, etcpp.
     */
    public static void showSimpleErrorMessage(final String key, final Throwable e, final boolean displayExceptionMessage,  final Object... arguments) {
        // if debug mode is enabled, send exception to logger
        if ("true".equals(ParameterService.getParameterValue(RapidMiner.PROPERTY_RAPIDMINER_GENERAL_DEBUGMODE))) {
            //LogService.getRoot().log(Level.WARNING, "Error: "+e.getMessage(), e);
            LogService.getRoot().log(Level.WARNING,
					I18N.getMessage(LogService.getRoot().getResourceBundle(), 
					"com.rapidminer.gui.tools.SwingTools.show_simple_get_message", 
					e.getMessage()),
					e);            
        }
        if (SwingUtilities.isEventDispatchThread()) {
            ExtendedErrorDialog dialog = new ExtendedErrorDialog(key, e, displayExceptionMessage, arguments);
            dialog.setVisible(true);
        } else {
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    ExtendedErrorDialog dialog = new ExtendedErrorDialog(key, e, displayExceptionMessage, arguments);
                    dialog.setVisible(true);
                }
            });
        }
    }

    /**
     * This is the normal method which could be used by GUI classes for errors caused by
     * some exception (e.g. IO issues). Of course these error message methods should never be
     * invoked by operators or similar.
     * The key is constructed as gui.dialog.error.-key- and uses .title and .icon properties
     * 
     * @param key						the I18n-key which will be used to display the internationalized message
     * @param errorMessage				the error message associated to this message
     * @param displayExceptionMessage	indicates if the exception message will be displayed in the dialog or just in the detailed panel
     * @param arguments					additional arguments for the internationalized message, which replace <code>{0}</code>, <code>{1}</code>, etcpp.
     */
    public static void showSimpleErrorMessage(final String key, final String errorMessage, final Object... arguments ) {
        if (SwingUtilities.isEventDispatchThread()) {
            ExtendedErrorDialog dialog = new ExtendedErrorDialog(key, errorMessage, arguments);
            dialog.setVisible(true);
        } else {
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    ExtendedErrorDialog dialog = new ExtendedErrorDialog(key, errorMessage, arguments);
                    dialog.setVisible(true);
                }
            });
        }

        // if debug mode is enabled, print throwable into logger
        if (ParameterService.getParameterValue(RapidMiner.PROPERTY_RAPIDMINER_GENERAL_DEBUGMODE).equals("true")) {
            LogService.getRoot().log(Level.WARNING, errorMessage);
        }
    }

//    /**
//     * This is the normal method which could be used by GUI classes for errors caused by
//     * some exception (e.g. IO issues). Of course these error message methods should never be
//     * invoked by operators or similar.
//     * 
//     * @param key						the I18n-key which will be used to display the internationalized message
//     * @param errorMessage				the error message associated to this message
//     * @param arguments					additional arguments for the internationalized message, which replace <code>{0}</code>, <code>{1}</code>, etcpp.
//     */
//    public static void showSimpleErrorMessage(final String key, final String errorMessage, final Object... arguments ) {
//        showSimpleErrorMessage(key, errorMessage, false, arguments);
//    }

    /**
     * Shows the final error message dialog. This dialog also allows to send a bug report if
     * the error was not (definitely) a user error.
     * 
     * @param key						the I18n-key which will be used to display the internationalized message
     * @param e							the exception associated to this message
     * @param displayExceptionMessage	indicates if the exception message will be displayed in the dialog or just in the detailed panel
     * @param arguments					additional arguments for the internationalized message, which replace <code>{0}</code>, <code>{1}</code>, etcpp.
     */
    public static void showFinalErrorMessage(String key, Throwable e, boolean displayExceptionMessage, Object...objects ) {
        // if debug modus is enabled, print throwable into logger
        if (ParameterService.getParameterValue(RapidMiner.PROPERTY_RAPIDMINER_GENERAL_DEBUGMODE).equals("true")) {
            LogService.getRoot().log(Level.SEVERE, e.getMessage(), e);
        }
        ExtendedErrorDialog dialog = new ExtendedErrorDialog( key, e, displayExceptionMessage, objects );
        //		dialog.setLocationRelativeTo(RapidMinerGUI.getMainFrame());
        dialog.setVisible(true);
    }

    /**
     * Shows the final error message dialog. This dialog also allows to send a bug report if
     * the error was not (definitely) a user error.
     * 
     * @param key						the I18n-key which will be used to display the internationalized message
     * @param e							the exception associated to this message
     * @param arguments					additional arguments for the internationalized message, which replace <code>{0}</code>, <code>{1}</code>, etcpp.
     */
    public static void showFinalErrorMessage(String key, Throwable e, Object...objects ) {
        showFinalErrorMessage(key, e, false, objects);
    }

    /** Opens a file chooser with a reasonable start directory. If the extension is null, no file filters will be used. */
    public static File chooseFile(Component parent, File file, boolean open, String extension, String extensionDescription) {
        return chooseFile(parent, null, file, open, extension, extensionDescription);
    }

    public static File chooseFile(Component parent, String i18nKey, File file, boolean open, String extension, String extensionDescription) {
        return chooseFile(parent, i18nKey, file, open, false, extension, extensionDescription);
    }

    /** Opens a file chooser with a reasonable start directory. If the extension is null, no file filters will be used.
     *  This method allows choosing directories. */
    public static File chooseFile(Component parent, File file, boolean open, boolean onlyDirs, String extension, String extensionDescription) {
        return chooseFile(parent, null, file, open, onlyDirs, extension, extensionDescription);
    }

    public static File chooseFile(Component parent, String i18nKey, File file, boolean open, boolean onlyDirs, String extension, String extensionDescription) {
        return chooseFile(parent, i18nKey, file, open, onlyDirs, extension == null ? null : new String[] { extension },
                extensionDescription == null ? null : new String[] { extensionDescription });
    }
    
    public static File chooseFile(Component parent, String i18nKey, File file, boolean open, boolean onlyDirs, String extension, String extensionDescription, boolean acceptAllFiles) {
        return chooseFile(parent, i18nKey, file, open, onlyDirs, extension == null ? null : new String[] { extension },
                extensionDescription == null ? null : new String[] { extensionDescription }, acceptAllFiles);
    }

    /** Returns the user selected file. */
    public static File chooseFile(Component parent, File file, boolean open, boolean onlyDirs, String[] extensions, String[] extensionDescriptions) {
        return chooseFile(parent, null, file, open, onlyDirs, extensions, extensionDescriptions);
    }

    public static File chooseFile(Component parent, String i18nKey, File file, boolean open, boolean onlyDirs, String[] extensions, String[] extensionDescriptions) {
        FileFilter[] filters = null;
        if (extensions != null) {
            filters = new FileFilter[extensions.length];
            for (int i = 0; i < extensions.length; i++) {
                filters[i] = new SimpleFileFilter(extensionDescriptions[i] + " (*." + extensions[i] + ")", "." + extensions[i]);
            }
        }
        return chooseFile(parent, i18nKey, file, open, onlyDirs, filters, true);
    }
    
    public static File chooseFile(Component parent, String i18nKey, File file, boolean open, boolean onlyDirs, String[] extensions, String[] extensionDescriptions, boolean acceptAllFiles) {
        FileFilter[] filters = null;
        if (extensions != null) {
            filters = new FileFilter[extensions.length];
            for (int i = 0; i < extensions.length; i++) {
                filters[i] = new SimpleFileFilter(extensionDescriptions[i] + " (*." + extensions[i] + ")", "." + extensions[i]);
            }
        }
        return chooseFile(parent, i18nKey, file, open, onlyDirs, filters, acceptAllFiles);
    }

    /**
     * Opens a file chooser with a reasonable start directory. onlyDirs
     * indidcates if only files or only can be selected.
     * 
     * @param file
     *            The initially selected value of the file chooser dialog
     * @param open
     *            Open or save dialog?
     * @param onlyDirs
     *            Only allow directories to be selected
     * @param fileFilters
     *            List of FileFilters to use
     */
    private static File chooseFile(Component parent, String i18nKey, File file, boolean open, boolean onlyDirs, FileFilter[] fileFilters, boolean acceptAllFiles) {
        if (parent == null)
            parent = RapidMinerGUI.getMainFrame();
        String key = "file_chooser." + (i18nKey != null ? i18nKey : open ? (onlyDirs ? "open_directory" : "open") : "save");
        JFileChooser fileChooser = createFileChooser(key, file, onlyDirs, fileFilters);
        fileChooser.setAcceptAllFileFilterUsed(acceptAllFiles);
        int returnValue = open ? fileChooser.showOpenDialog(parent) : fileChooser.showSaveDialog(parent);
        switch (returnValue) {
        case JFileChooser.APPROVE_OPTION:
            // check extension
            File selectedFile = fileChooser.getSelectedFile();

            FileFilter selectedFilter = fileChooser.getFileFilter();
            String extension = null;
            if (selectedFilter instanceof SimpleFileFilter) {
                SimpleFileFilter simpleFF = (SimpleFileFilter)selectedFilter;
                extension = simpleFF.getExtension();
            }
            if (extension != null) {
                if (!selectedFile.getAbsolutePath().toLowerCase().endsWith(extension.toLowerCase())) {
                    selectedFile = new File(selectedFile.getAbsolutePath() + extension);
                }
            }

            if (selectedFile != null) {
                File parentFile = selectedFile.getParentFile();
                if (parentFile != null) {
                    List<Bookmark> bookmarks = null;
                    File bookmarksFile = new File(FileSystemService.getUserRapidMinerDir(), ".bookmarks");
                    if (bookmarksFile.exists()) {
                        bookmarks = BookmarkIO.readBookmarks(bookmarksFile);
                        Iterator<Bookmark> b = bookmarks.iterator();
                        while (b.hasNext()) {
                            Bookmark bookmark = b.next();
                            if (bookmark.getName().equals("--- Last Directory")) {
                                b.remove();
                            }
                        }
                        bookmarks.add(new Bookmark("--- Last Directory", parentFile.getAbsolutePath()));
                        Collections.sort(bookmarks);
                        BookmarkIO.writeBookmarks(bookmarks, bookmarksFile);
                    }
                }
            }
            return selectedFile;
        default:
            return null;
        }
    }

    /**
     * Creates file chooser with a reasonable start directory. You may use the
     * following code snippet in order to retrieve the file:
     * 
     * <pre>
     * 	if (fileChooser.showOpenDialog(parent) == JFileChooser.APPROVE_OPTION)
     * 	    File selectedFile = fileChooser.getSelectedFile();
     * </pre>
     * 
     * Usually, the method
     * {@link #chooseFile(Component, File, boolean, boolean, FileFilter[])} or
     * one of the convenience wrapper methods can be used to do this. This
     * method is only useful if one is interested, e.g., in the selected file
     * filter.
     * 
     * @param file
     *            The initially selected value of the file chooser dialog
     * @param onlyDirs
     *            Only allow directories to be selected
     * @param fileFilters
     *            List of FileFilters to use
     */
    public static JFileChooser createFileChooser(String i18nKey, File file, boolean onlyDirs, FileFilter[] fileFilters) {
        File directory = null;

        if (file != null) {
            if (file.isDirectory()) {
                directory = file;
            } else {
                directory = file.getAbsoluteFile().getParentFile();
            }
        } else {
            directory = FileSystemView.getFileSystemView().getDefaultDirectory();
        }

        JFileChooser fileChooser = new ExtendedJFileChooser(i18nKey, directory);
        if (onlyDirs)
            fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        if (fileFilters != null) {
            fileChooser.setAcceptAllFileFilterUsed(true);
            for (FileFilter fileFilter : fileFilters)
                fileChooser.addChoosableFileFilter(fileFilter);
        }

        if (file != null)
            fileChooser.setSelectedFile(file);

        return fileChooser;
    }

    /** Creates a panel with title and text. The panel has a border layout and the text
     *  is placed into the NORTH section. */
    public static JPanel createTextPanel(String title, String text) {
        JPanel panel = new JPanel(new java.awt.BorderLayout());
        JLabel label = new JLabel("<html><h3>" + title + "</h3>" + (text != null ? "<p>" + text + "</p>" : "") + "</html>");
        label.setBorder(BorderFactory.createEmptyBorder(11, 11, 11, 11));
        label.setFont(label.getFont().deriveFont(java.awt.Font.PLAIN));
        panel.add(label, java.awt.BorderLayout.NORTH);
        return panel;
    }

    // ================================================================================

    //	/**
    //	 * Replaces simple html tags and quotes by RapidMiner specific text elements.
    //	 * These can be used in XML files without confusing an XML parser.
    //	 */
    //	public static String html2RapidMinerText(String html) {
    //		if (html == null)
    //			return null;
    //		String result = html.replaceAll("<", "#ylt#");
    //		result = result.replaceAll(">", "#ygt#");
    //		result = result.replaceAll("\"", "#yquot#");
    //        result = result.replaceAll(Tools.getLineSeparator(), "");
    //		return result;
    //	}

    /**
     * Replaces the RapidMiner specific tag elements by normal HTML tags.
     * The given text is also embedded in an HTML and body tag with an
     * appropriated style sheet definition.
     * 
     * Currently, the only replaced tag is &lt;icon&gt;NAME&lt;/icon&gt;
     * which will be replaced by &lt;img src="path/to/NAME"/&gt;.
     * 
     */
    public static String text2DisplayHtml(String text) {
        String result = "<html><head><style type=text/css>body { font-family:sans-serif; font-size:12pt; }</style></head><body>" + text + "</body></html>";
        result = text2SimpleHtml(result);
        //result = result.replaceAll("#yquot#", "&quot;");
        while (result.indexOf("<icon>") != -1) {
            int startIndex = result.indexOf("<icon>");
            int endIndex = result.indexOf("</icon>");
            String start = result.substring(0, startIndex);
            String end = result.substring(endIndex + 7);
            String icon = result.substring(startIndex + 6, endIndex).trim().toLowerCase();
            java.net.URL url = Tools.getResource("icons/" + icon + ".png");
            if (url != null)
                result = start + "<img src=\"" + url + "\"/>" + end;
            else
                result = start + end;
        }
        return result;
    }

    /**
     * Replaces the RapidMiner specific tag elements by normal HTML tags. This method
     * does not embed the given text in a root HTML tag.
     */
    private static String text2SimpleHtml(String htmlText) {
        if (htmlText == null)
            return null;
        String replaceString = htmlText;
        //		replaceString = htmlText.replaceAll("#ygt#", ">");
        //		replaceString = replaceString.replaceAll("#ylt#", "<");

        StringBuffer result = new StringBuffer();
        boolean afterClose = true;
        int currentLineLength = 0;
        for (int i = 0; i < replaceString.length(); i++) {
            char c = replaceString.charAt(i);
            // skip white space after close
            if (afterClose)
                if (c == ' ')
                    continue;

            // opening bracket
            if (c == '<') {
                if (!afterClose) {
                    result.append(Tools.getLineSeparator());
                    currentLineLength = 0;
                }
            }

            // append char
            afterClose = false;
            result.append(c);
            currentLineLength++;

            // break long lines
            if (currentLineLength > 70 && c == ' ') {
                result.append(Tools.getLineSeparator());
                currentLineLength = 0;
            }

            // closing bracket
            if (c == '>') {
                result.append(Tools.getLineSeparator());
                currentLineLength = 0;
                afterClose = true;
            }
        }
        return result.toString();
    }

    /**
     * Returns a color equivalent to the value of <code>value</code>. The value
     * has to be normalized between 0 and 1.
     */
    public static Color getPointColor(double value) {
        return new Color(Color.HSBtoRGB((float) (0.68 * (1.0d - value)), 1.0f, 1.0f)); // all
        // colors
    }

    /**
     * Returns a color equivalent to the value of <code>value</code>. The value
     * will be normalized between 0 and 1 using the parameters max and min. Which
     * are the minimum and maximum of the complete dataset.
     */
    public static Color getPointColor(double value, double max, double min){
        value = (value - min) / (max - min);
        return getPointColor(value);
    }

    /** Returns JEditTextArea defaults with adapted syntax color styles. */
    public static TextAreaDefaults getTextAreaDefaults() {
        TextAreaDefaults defaults = TextAreaDefaults.getDefaults();
        defaults.styles = getSyntaxStyles();
        return defaults;
    }

    /**
     * Returns adapted syntax color and font styles matching RapidMiner colors.
     */
    public static SyntaxStyle[] getSyntaxStyles() {
        SyntaxStyle[] styles = SyntaxUtilities.getDefaultSyntaxStyles();
        styles[Token.COMMENT1] = new SyntaxStyle(new Color(0x990033), true, false);
        styles[Token.COMMENT2] = new SyntaxStyle(Color.black, true, false);
        styles[Token.KEYWORD1] = new SyntaxStyle(Color.black, false, true);
        styles[Token.KEYWORD2] = new SyntaxStyle(new Color(255,51,204), false, false);
        styles[Token.KEYWORD3] = new SyntaxStyle(new Color(255,51,204), false, false);
        styles[Token.LITERAL1] = new SyntaxStyle(new Color(51,51,255), false, false);
        styles[Token.LITERAL2] = new SyntaxStyle(new Color(51,51,255), false, false);
        styles[Token.LABEL] = new SyntaxStyle(new Color(0x990033), false, true);
        styles[Token.OPERATOR] = new SyntaxStyle(Color.black, false, true);
        styles[Token.INVALID] = new SyntaxStyle(Color.red, false, true);
        return styles;
    }

    public static String toHTMLString(Ports<? extends Port> ports) {
        StringBuilder b = new StringBuilder();
        boolean first = true;
        for (Port port : ports.getAllPorts()) {
            if (!first) {
                b.append(", ");
            } else {
                first = false;
            }
            b.append(port.getName());
            String desc = port.getDescription();
            if (desc.length() > 0) {
                b.append(": ");
                b.append(port.getDescription());
            }
        }
        return b.toString();
    }

    public static void showLongMessage(String i18nKey, final String message) {
        LongMessageDialog dialog = new LongMessageDialog(i18nKey, message);
        dialog.setVisible(true);
    }

    public static void setEnabledRecursive(Component c, boolean enabled) {
        c.setEnabled(enabled);
        if (c instanceof Container) {
            for (Component child : ((Container)c).getComponents()) {
                setEnabledRecursive(child, enabled);
            }
        }
    }

    public static void setOpaqueRecursive(Component c, boolean enabled) {
        if (c instanceof JComponent) {
            ((JComponent)c).setOpaque(enabled);
        }
        if (c instanceof Container) {
            for (Component child : ((Container)c).getComponents()) {
                setOpaqueRecursive(child, enabled);
            }
        }
    }

    public static void setProcessEditorsEnabled(boolean enabled) {
        MainFrame mainFrame = RapidMinerGUI.getMainFrame();
        setEnabledRecursive(mainFrame.getProcessPanel().getComponent(), enabled);
        setEnabledRecursive(mainFrame.getPropertyPanel().getComponent(), enabled);
        setEnabledRecursive(mainFrame.getOperatorTree(), enabled);
        setEnabledRecursive(mainFrame.getProcessContextEditor().getComponent(), enabled);
        setEnabledRecursive(mainFrame.getXMLEditor(), enabled);

        mainFrame.getActions().enableActions();
    }

    public static Color getOperatorColor(Operator operator) {
        return GROUP_TO_COLOR_MAP.get(operator.getOperatorDescription().getGroup());
    }

    public static Color getOperatorColor(String operatorGroup) {
        return GROUP_TO_COLOR_MAP.get(operatorGroup);
    }

    /**
     * This method adds the colors of the given property file to the global group colors
     */
    public static void registerAdditionalGroupColors(String groupProperties, String pluginName, ClassLoader classLoader) {
        try {
            GROUP_TO_COLOR_MAP.parseProperties(groupProperties, "group.", ".color", classLoader);
        } catch (IOException e) {
            LogService.getRoot().warning("Cannot load operator group colors for plugin " + pluginName + ".");
        }
    }
}
