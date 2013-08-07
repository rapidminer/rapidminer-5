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
package com.rapidminer.tools;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.logging.Level;

import javax.swing.JComponent;

import com.rapidminer.RapidMiner;

/**
 * @author Simon Fischer, Nils Woehler
 */
public class I18N {

	private static final ExtensibleResourceBundle USER_ERROR_BUNDLE;
	private static final ExtensibleResourceBundle ERROR_BUNDLE; 
	private static final ExtensibleResourceBundle GUI_BUNDLE; 
	
	// init I18N
	static {
		ParameterService.init();
		
		String localeLanguage = ParameterService.getParameterValue(RapidMiner.PROPERTY_RAPIDMINER_GENERAL_LOCALE_LANGUAGE);
		Locale locale = Locale.getDefault();
		if (localeLanguage != null) {
            locale = new Locale(localeLanguage);
			Locale.setDefault(locale);
            LogService.getRoot().log(Level.INFO, "com.rapidminer.tools.I18N.set_locale_to", locale);	
        } else {
        	LogService.getRoot().log(Level.INFO, "com.rapidminer.tools.I18N.using_default_locale", locale);
        }
		JComponent.setDefaultLocale(locale);
		
		USER_ERROR_BUNDLE = new ExtensibleResourceBundle(ResourceBundle.getBundle("com.rapidminer.resources.i18n.UserErrorMessages", locale, I18N.class.getClassLoader()));
		ERROR_BUNDLE = new ExtensibleResourceBundle(ResourceBundle.getBundle("com.rapidminer.resources.i18n.Errors", locale, I18N.class.getClassLoader()));
		GUI_BUNDLE = new ExtensibleResourceBundle(ResourceBundle.getBundle("com.rapidminer.resources.i18n.GUI", locale, I18N.class.getClassLoader()));

		ResourceBundle plotterBundle = ResourceBundle.getBundle("com.rapidminer.resources.i18n.PlotterMessages", locale, I18N.class.getClassLoader());

		GUI_BUNDLE.addResourceBundle(plotterBundle);
	}

	/** Returns the resource bundle for error messages and quick fixes. */
	public static ResourceBundle getErrorBundle() {
		return ERROR_BUNDLE;
	}

	public static ResourceBundle getGUIBundle() {
		return GUI_BUNDLE;
	}

	public static ResourceBundle getUserErrorMessagesBundle() {
		return USER_ERROR_BUNDLE;
	}

	/** registers the properties of the given bundle on the global error bundle */
	public static void registerErrorBundle(ResourceBundle bundle) {
		registerErrorBundle(bundle, false);
	}

	/** registers the properties of the given bundle on the global gui bundle */
	public static void registerGUIBundle(ResourceBundle bundle) {
		registerGUIBundle(bundle, false);
	}

	/** registers the properties of the given bundle on the global userError bundle */
	public static void registerUserErrorMessagesBundle(ResourceBundle bundle) {
		registerUserErrorMessagesBundle(bundle, false);
	}

	/** registers the properties of the given bundle on the global error bundle */
	public static void registerErrorBundle(ResourceBundle bundle, boolean overwrite) {
		if (!overwrite)
			ERROR_BUNDLE.addResourceBundle(bundle);
		else
			ERROR_BUNDLE.addResourceBundleAndOverwrite(bundle);
	}

	/** registers the properties of the given bundle on the global gui bundle */
	public static void registerGUIBundle(ResourceBundle bundle, boolean overwrite) {
		if (!overwrite)
			GUI_BUNDLE.addResourceBundle(bundle);
		else
			GUI_BUNDLE.addResourceBundleAndOverwrite(bundle);
	}

	/** registers the properties of the given bundle on the global userError bundle */
	public static void registerUserErrorMessagesBundle(ResourceBundle bundle, boolean overwrite) {
		if (!overwrite)
			USER_ERROR_BUNDLE.addResourceBundle(bundle);
		else
			USER_ERROR_BUNDLE.addResourceBundleAndOverwrite(bundle);
	}

	/** 
	 * Returns a message if found or the key if not found. 
	 * Arguments <b>can</b> be specified which will be used to format the String. 
	 * In the {@link ResourceBundle} the String '{0}' (without ') will be replaced
	 * by the first argument, '{1}' with the second and so on.   
	 *  
	 *  Catches the exception thrown by ResourceBundle in the latter case. 
	 **/
	public static String getMessage(ResourceBundle bundle, String key, Object... arguments) {
		try {

			if (arguments == null || arguments.length == 0) {
				return bundle.getString(key);
			} else {
				String message = bundle.getString(key);
				if (message != null) {
					return MessageFormat.format(message, arguments);
				} else {
					return key;
				}
			}

		} catch (MissingResourceException e) {
			LogService.getRoot().log(Level.FINE, "com.rapidminer.tools.I18N.missing_key", key);
			return key;
		}
	}

	/**
	 * Returns a message if found or <code>null</code> if not.
	 *  
	 * Arguments <b>can</b> be specified which will be used to format the String. 
	 * In the {@link ResourceBundle} the String '{0}' (without ') will be replaced
	 * by the first argument, '{1}' with the second and so on. 
	 * 
	 */
	public static String getMessageOrNull(ResourceBundle bundle, String key, Object... arguments) {

		if (bundle.containsKey(key))
			return getMessage(bundle, key, arguments);
		else
			return null;

	}

	/**
	 * This will return the value of the property gui.label.-key- of the
	 * GUI bundle or the key itself if unknown.
	 */
	public static String getGUILabel(String key, Object... arguments) {
		String completeKey = "gui.label." + key;
		if (GUI_BUNDLE.containsKey(completeKey)) {
			return getMessage(GUI_BUNDLE, completeKey, arguments);
		} else {
			return completeKey;
		}
	}
}
