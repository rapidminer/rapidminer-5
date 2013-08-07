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


import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.text.MessageFormat;
import java.util.logging.Level;

import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;

import com.rapidminer.gui.ConditionalAction;
import com.rapidminer.gui.RapidMinerGUI;
import com.rapidminer.tools.I18N;
import com.rapidminer.tools.LogService;

/**
 * This will create an action, whose settings are take from a .properties file being part of
 * the GUI Resource bundles of RapidMiner. These might be accessed using the I18N class.
 * 
 * A resource action needs a key specifier, which will be used to build the complete keys of
 * the form:
 * gui.action.<specifier>.label = Which will be the caption
 * gui.action.<specifier>.icon = The icon of this action. For examples used in menus or buttons
 * gui.action.<specifier>.acc = The accelerator key used for menu entries
 * gui.action.<specifier>.tip = Which will be the tool tip
 * gui.action.<specifier>.mne = Which will give you access to the mnemonics key. Please make it the same case as in the label
 * 
 * @author Simon Fischer, Sebastian Land
 */
public abstract class ResourceAction extends ConditionalAction {

	private static final long serialVersionUID = -3699425760142415331L;

	private final String key;

	private final String iconName;

	public ResourceAction(String i18nKey, Object ... i18nArgs) {
		this(false, i18nKey, i18nArgs);

		setCondition(EDIT_IN_PROGRESS, DONT_CARE);
	}

	public ResourceAction(boolean smallIcon, String i18nKey, Object ... i18nArgs) {
		super((i18nArgs == null) || (i18nArgs.length == 0) ? 
				getMessage(i18nKey+".label") : 
					MessageFormat.format(getMessage(i18nKey+".label"), i18nArgs));
		this.key = i18nKey;
		String mne = getMessageOrNull(i18nKey + ".mne");		
		if (mne != null && mne.length() > 0) {			
			String name = (String)getValue(NAME);
			if (name != null && name.length() > 0 && 
					(name.indexOf(mne.charAt(0)) == -1) && 
					(name.indexOf(Character.toLowerCase(mne.charAt(0))) == -1)) {
				LogService.getRoot().log(Level.FINE, 
						"com.rapidminer.gui.tools.ResourceAction.key_not_found", 
						new Object[] {mne, i18nKey, name});
			}
			mne = mne.toUpperCase();
			putValue(MNEMONIC_KEY, (int)mne.charAt(0));
		}
		String tip = getMessageOrNull(i18nKey + ".tip");
		if (tip != null) {
			putValue(SHORT_DESCRIPTION, 
					(i18nArgs == null) || (i18nArgs.length == 0) ?
							tip :
								MessageFormat.format(tip, i18nArgs));
		}
		this.iconName = getMessageOrNull(i18nKey + ".icon");
		if (getIconName() != null) {
			ImageIcon small = SwingTools.createIcon("16/"+getIconName());
			ImageIcon large = SwingTools.createIcon("24/"+getIconName());
			putValue(LARGE_ICON_KEY, smallIcon ? (small != null ? small : large) : large);
			putValue(SMALL_ICON, small != null ? small : large);
		}
		String acc = getMessageOrNull(i18nKey + ".acc");
		if (acc != null) {
			KeyStroke stroke = KeyStroke.getKeyStroke(acc);
			putValue(ACCELERATOR_KEY, stroke);			
		}
		putValue("rm_id", i18nKey);
	}

	private static String getMessage(String key) {
		return I18N.getMessage(I18N.getGUIBundle(), "gui.action."+key);
	}

	private static String getMessageOrNull(String key) {
		return I18N.getMessageOrNull(I18N.getGUIBundle(), "gui.action."+key);
	}

	/** Adds the action to the input and action map of the components.
	 * 
	 * @param condition one out of {@link JComponent#WHEN_FOCUSED}, ...
	 * @param disableOnFocusLost if <code>true</code>, will disable the action on FocusLost event
	 * 			and enable it again on FocusGained (if conditions of superclass are met).
	 * @param components the {@link JComponent}s to register this action to
	 */
	public void addToActionMap(int condition, boolean disableOnFocusLost, boolean initiallyDisabled, String actionKey, JComponent... components) {
		for (JComponent comp : components) {
			if (comp == null) {
				throw new IllegalArgumentException("components must not be null!");
			}

			KeyStroke keyStroke = (KeyStroke)getValue(ACCELERATOR_KEY);
			if (keyStroke != null) {
				actionKey = actionKey == null ? key : actionKey;
				comp.getInputMap(condition).put(keyStroke, actionKey);
				comp.getActionMap().put(actionKey, this);
			} else {
				LogService.getRoot().log(Level.FINE, "com.rapidminer.gui.tools.ResourceAction.add_action_key_error", key);
			}
			if (disableOnFocusLost) {
				comp.addFocusListener(new FocusListener() {

					@Override
					public void focusLost(FocusEvent e) {
						if (!e.isTemporary()) {
							// focus lost here means disable it no matter the conditions
							ResourceAction.this.setEnabled(false);
							ResourceAction.super.setDisabledDueToFocusLost(true);
						}
					}

					@Override
					public void focusGained(FocusEvent e) {
						if (!e.isTemporary()) {
							// focus gained here means enable it if conditions are fulfilled
							ResourceAction.super.setDisabledDueToFocusLost(false);
							RapidMinerGUI.getMainFrame().getActions().enableActions();
						}
					}
				});
				if (initiallyDisabled) {
					if (SwingUtilities.isEventDispatchThread()) {
						super.setDisabledDueToFocusLost(true);
						setEnabled(false);
					} else {
						SwingUtilities.invokeLater(new Runnable() {

							@Override
							public void run() {
								ResourceAction.super.setDisabledDueToFocusLost(true);
								ResourceAction.this.setEnabled(false);
							}

						});
					}
				}
			}
		}
	}

	/** Adds the action to the input and action map of the component.
	 * 
	 * @param condition one out of WHEN_IN_FOCUES, ...
	 */
	public void addToActionMap(JComponent component, int condition) {
		addToActionMap(component, null, condition);
	}

	/** Adds the action to the input and action map of the component.
	 * 
	 * @param condition one out of WHEN_IN_FOCUES, ...
	 */
	public void addToActionMap(JComponent component, String actionKey, int condition) {
		addToActionMap(condition, false, false, actionKey, component);
	}

	/**
	 * This returns the i18n key of this action.
	 */
	public String getKey() {
		return key;
	}

	public String getIconName() {
		return iconName;
	}
}
