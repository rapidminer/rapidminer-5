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
package com.rapidminer.gui.templates;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileFilter;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

import com.rapidminer.Process;
import com.rapidminer.gui.tools.ExtendedJScrollPane;
import com.rapidminer.gui.tools.ResourceLabel;
import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.gui.tools.dialogs.ButtonDialog;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorChain;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.UndefinedParameterError;
import com.rapidminer.tools.FileSystemService;
import com.rapidminer.tools.Tools;


/**
 * The save as template dialog assists the user in creating a new process
 * template. Template processes are saved in the local .rapidminer directory of the
 * user. The name, description and additional parameters to set can be specified
 * by the user.
 * 
 * @author Ingo Mierswa, Tobias Malbrecht
 */
public class SaveAsTemplateDialog extends ButtonDialog {

    private static final long serialVersionUID = -4892200177390173103L;

    private boolean ok = false;

    private final Set<OperatorParameterPair> selectedParameters = new HashSet<OperatorParameterPair>();

    private final JTextField nameField = new JTextField();

    private final JTextField groupField = new JTextField();

    private final JTextArea descriptionField = new JTextArea(5, 40);

    private Process process;

    /** Creates a new save as template dialog. */
    public SaveAsTemplateDialog(Process process) {
        super("save_as_template", true,new Object[]{});
        this.process = process;

        descriptionField.setText(process.getRootOperator().getUserDescription());
        descriptionField.setLineWrap(true);
        descriptionField.setWrapStyleWord(true);

        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.anchor = GridBagConstraints.FIRST_LINE_START;

        JLabel label = new ResourceLabel("save_as_template.name");
        c.fill = GridBagConstraints.NONE;
        c.weightx   = 0;
        c.weighty   = 0;
        c.gridwidth = GridBagConstraints.RELATIVE;
        c.insets    = new Insets(0, 0, GAP, GAP);
        label.setLabelFor(nameField);
        panel.add(label, c);

        c.weightx   = 1;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.insets    = new Insets(0, 0, GAP, 0);
        c.fill = GridBagConstraints.BOTH;
        panel.add(nameField, c);

        label = new ResourceLabel("save_as_template.group");
        c.fill = GridBagConstraints.NONE;
        c.weightx   = 0;
        c.weighty   = 0;
        c.gridwidth = GridBagConstraints.RELATIVE;
        c.insets    = new Insets(0, 0, GAP, GAP);
        label.setLabelFor(groupField);
        panel.add(label, c);
        c.weightx   = 1;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.insets    = new Insets(0, 0, GAP, 0);
        c.fill = GridBagConstraints.BOTH;
        panel.add(groupField, c);


        label = new ResourceLabel("save_as_template.description");
        c.insets    = new Insets(0, 0, GAP, GAP);
        c.weightx   = 0;
        c.gridwidth = GridBagConstraints.RELATIVE;
        c.fill = GridBagConstraints.NONE;
        label.setLabelFor(descriptionField);
        panel.add(label, c);

        c.weightx   = 1;
        c.weighty   = 0.1;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.insets    = new Insets(0, 0, GAP, 0);
        c.fill = GridBagConstraints.BOTH;
        JScrollPane descriptionPane = new ExtendedJScrollPane(descriptionField);
        descriptionPane.setBorder(createBorder());
        panel.add(descriptionPane, c);

        JScrollPane tablePane = new ExtendedJScrollPane(makeCheckboxTable(process.getRootOperator()));
        tablePane.setBorder(createBorder());

        c.weightx = 1;
        c.weighty = .9;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.insets    = new Insets(0, 0, 0, 0);
        panel.add(tablePane, c);

        JButton okButton = makeOkButton("save_as_template_dialog_save");
        layoutDefault(panel, NORMAL, okButton, makeCancelButton());
        getRootPane().setDefaultButton(okButton);
    }

    private JComponent makeCheckboxTable(Operator operator) {
        List<Operator> ops = new LinkedList<Operator>();
        ops.add(operator);
        if (operator instanceof OperatorChain) {
            ops.addAll(((OperatorChain)operator).getAllInnerOperators());
        }

        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.weightx = 1;
        c.weighty = 0;
        c.insets = new Insets(0, 0, 0, 0);
        c.fill = GridBagConstraints.BOTH;
        c.gridwidth = GridBagConstraints.REMAINDER;

        for (Operator op : ops) {
            JPanel operatorPanel = new JPanel(new BorderLayout());
            Collection<ParameterType> parameterTypes = op.getParameters().getParameterTypes();
            if (parameterTypes.isEmpty()) {
                continue;
            }
            JLabel label = new JLabel("<html>"+op.getName()+"<br/><small>"+op.getOperatorDescription().getName()+"</small></html>");
            //label.setFont(label.getFont().deriveFont(Font.BOLD));
            label.setIcon(op.getOperatorDescription().getSmallIcon());
            label.setPreferredSize(new Dimension(190,50));
            label.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 0));
            label.setVerticalAlignment(SwingConstants.TOP);
            operatorPanel.add(label, BorderLayout.WEST);

            int size = parameterTypes.size();
            GridLayout layout = new GridLayout(size, 2);
            layout.setHgap(GAP);
            layout.setVgap(GAP);
            JPanel parameterPanel = new JPanel(layout);
            for (final ParameterType type : parameterTypes) {
                final JCheckBox box = new JCheckBox(type.getKey());
                final OperatorParameterPair opp = new OperatorParameterPair(op.getName(), type.getKey());
                box.setSelected(false);
                parameterPanel.add(box);
                box.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        if (box.isSelected()) {
                            selectedParameters.add(opp);
                        } else {
                            selectedParameters.remove(opp);
                        }
                    }
                });

                String current;
                try {
                    current = op.getParameterAsString(type.getKey());
                    if (current == null){
                        current = "";
                    }
                } catch (UndefinedParameterError e1) {
                    current = "";
                }
                JLabel currentLabel = new JLabel(current);
                if (!current.equals(type.toString(type.getDefaultValue()))) {
                    currentLabel.setFont(currentLabel.getFont().deriveFont(Font.BOLD));
                    box.setSelected(true);
                    selectedParameters.add(opp);
                }
                parameterPanel.add(currentLabel);
            }
            operatorPanel.add(parameterPanel, BorderLayout.CENTER);
            operatorPanel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Color.LIGHT_GRAY),
                    BorderFactory.createEmptyBorder(4, 4, 4, 4)));
            panel.add(operatorPanel, c);
        }
        c.fill = GridBagConstraints.BOTH;
        c.weighty = 1;
        panel.add(new JPanel(new BorderLayout()), c);
        return panel;
    }

    private void addMandatoryParameters(Operator operator, Set<OperatorParameterPair> selectedOptional) {
        Iterator<ParameterType> i = operator.getParameters().getParameterTypes().iterator();
        while (i.hasNext()) {
            ParameterType type = i.next();
            if (!type.isOptional() && !operator.getParameters().isSet(type.getKey())) {
                selectedOptional.add(new OperatorParameterPair(operator.getName(), type.getKey()));
            }
        }
        if (operator instanceof OperatorChain) {
            for (Operator child : ((OperatorChain)operator).getImmediateChildren())
                addMandatoryParameters(child, selectedOptional);
        }
    }

    public boolean isOk() {
        return ok;
    }

    public Template getTemplate() {
        String name = nameField.getText();
        String group = groupField.getText();
        Set<OperatorParameterPair> selectedOptional = new TreeSet<OperatorParameterPair>(new Comparator<OperatorParameterPair>() {
            @Override
            public int compare(OperatorParameterPair o1, OperatorParameterPair o2) {
                return o1.compareTo(o2);
            }
        });
        selectedOptional.addAll(selectedParameters);
        addMandatoryParameters(process.getRootOperator(), selectedOptional);
        return new Template(name, group, descriptionField.getText(), name + ".xml", selectedOptional);
    }

    private boolean checkIfNameOk() {
        String name = nameField.getText();
        if ((name == null) || (name.length() == 0)) {
            SwingTools.showVerySimpleErrorMessage("no_template_name");
            return false;
        }

        //		File[] preDefinedTemplateFiles = ParameterService.getConfigFile("templates").listFiles(new FileFilter() {
        //			public boolean accept(File file) {
        //				return file.getName().endsWith(".template");
        //			}
        //		});
        File[] userDefinedTemplateFiles = FileSystemService.getUserRapidMinerDir().listFiles(new FileFilter() {
            @Override
            public boolean accept(File file) {
                return file.getName().endsWith(".template");
            }
        });

        for (int i = 0; i < userDefinedTemplateFiles.length; i++) {
            String tempName = userDefinedTemplateFiles[i].getName().substring(0, userDefinedTemplateFiles[i].getName().lastIndexOf("."));
            if (tempName.equals(name)) {
                SwingTools.showVerySimpleErrorMessage("Name '" + name + "' is already used." + Tools.getLineSeparator() + "Please change name or delete the old template!");
                return false;
            }
        }
        return true;
    }

    @Override
    protected void ok() {
        if (checkIfNameOk()) {
            ok = true;
            dispose();
        }
    }

    @Override
    protected void cancel() {
        ok = false;
        dispose();
    }
}
