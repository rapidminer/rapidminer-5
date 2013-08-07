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

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JPanel;

import com.rapidminer.RapidMiner;
import com.rapidminer.gui.properties.SettingsDialog;
import com.rapidminer.gui.tools.ResourceActionAdapter;
import com.rapidminer.parameter.ParameterTypeCategory;
import com.rapidminer.tools.ParameterService;

/** A dialog that asks the user a question which can be answered with yes or no
 *  and remembers its decision. The user can decide whether or not their answer
 *  is remembered. If it is remembered, the dialog will not be displayed the
 *  next time.
 * 
 *  To use this class, define "gui.dialog.KEY.title" and "gui.dialog.KEY.message"
 *  in the GUI properties file. Also, register a property using
 *  {@link RapidMiner#registerRapidMinerProperty(com.rapidminer.parameter.ParameterType)}
 *  with a {@link ParameterTypeCategory} where the categories are {@link #PROPERTY_VALUES}
 *  (yes, no, ask). Preferrably, the default value should be {@link #ASK}.
 *  Pass the i18n key and the key of this property to {@link #confirmAction(String, String)}.
 * 
 *  The behaviour of this dialog depends on the current value of the property.
 *  <ul>
 *  <li>If its value is either "true" or "false", nothing will happen and the method will
 *  simply return true or false, depending on this value.</li>
 *  <li>If the value is "ask", a dialog will pop up, and either true (yes) or false (false)
 *  will be returned. Furthermore, if the user checks the "Remember my decision" checkbox,
 *  the property will be set to the users decision ("true" or "false"), and the property
 *  will be saved to the users private property file. Hence, the next call to
 *  {@link #confirmAction(String, String)} will return without showing a dialog.</li>
 *  </ul>
 * 
 *  In order to make the dialog shown again, the user must set the value back to "ask"
 *  in the {@link SettingsDialog}.
 * 
 * @author Simon Fischer
 *
 */
public class DecisionRememberingConfirmDialog extends ButtonDialog {

    public static final String[] PROPERTY_VALUES = { "true", "false", "ask" };

    private static final long serialVersionUID = 1L;

    public static final int TRUE  = 0;
    public static final int FALSE = 1;
    public static final int ASK   = 2;

    private final JCheckBox dontAskAgainBox;

    private final String propertyName;

    private boolean confirmed;

    private DecisionRememberingConfirmDialog(String i18nKey, String property) {
        super(i18nKey, true,new Object[]{});
        this.propertyName = property;

        dontAskAgainBox = new JCheckBox(new ResourceActionAdapter("remember_decision"));
        dontAskAgainBox.setSelected(false);

        JButton yesButton = makeOkButton("yes");
        JButton noButton = makeCancelButton("no");

        JPanel buttonPanel = new JPanel(new BorderLayout());
        buttonPanel.add(dontAskAgainBox, BorderLayout.WEST);
        buttonPanel.add(makeButtonPanel(yesButton, noButton), BorderLayout.EAST);
        layoutDefault(null, buttonPanel);

        noButton.requestFocusInWindow();
    }

    @Override
    public void ok() {
        confirmed = true;
        saveIfDesired();
        dispose();
    }

    @Override
    public void cancel() {
        confirmed = false;
        saveIfDesired();
        dispose();
    }

    private void saveIfDesired() {
        if (dontAskAgainBox.isSelected()) {
            String value = confirmed ? "true" : "false";
            ParameterService.setParameterValue(propertyName, value);
            ParameterService.saveParameters();
        }
    }

    public static boolean confirmAction(String i18nKey, String propertyKey) {
        String propValue = ParameterService.getParameterValue(propertyKey);
        if (propValue != null) {
            if (propValue.equals("true")) {
                return true;
            } else if (propValue.equals("false")) {
                return false;
            }
        }
        DecisionRememberingConfirmDialog d = new DecisionRememberingConfirmDialog(i18nKey, propertyKey);
        d.setVisible(true);
        return d.confirmed;
    }
}
