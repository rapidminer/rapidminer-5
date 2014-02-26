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
package com.rapidminer.gui.properties;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Vector;

import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;

import com.rapidminer.Process;
import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.metadata.AttributeMetaData;
import com.rapidminer.operator.ports.metadata.ExampleSetMetaData;
import com.rapidminer.operator.ports.metadata.ModelMetaData;
import com.rapidminer.parameter.ParameterTypeExpression;
import com.rapidminer.tools.LogService;
import com.rapidminer.tools.expression.parser.AbstractExpressionParser;
import com.rapidminer.tools.expression.parser.AbstractExpressionParser.ExpressionParserException;
import com.rapidminer.tools.expression.parser.ExpressionParserFactory;
import com.rapidminer.tools.expression.parser.FunctionDescription;

/**
 * 
 * @author Ingo Mierswa, Marco Boeck
 *
 */
public class ExpressionPropertyDialog extends PropertyDialog {

	private static final long serialVersionUID = 5567661137372752202L;

	private static final int FUNCTION_ROW_LENGTH = 4;
	
	private JTextField currentExpression = new JTextField();
	
	private static final String OK_ICON_NAME = "ok.png";
	private static final String ERROR_ICON_NAME = "error.png";
	
	private static Icon OK_ICON = null;
	private static Icon ERROR_ICON = null;
	
	static {
		OK_ICON = SwingTools.createIcon("16/" + OK_ICON_NAME);
		ERROR_ICON = SwingTools.createIcon("16/" + ERROR_ICON_NAME);
	}
	
	private JLabel validationLabel = new JLabel();
	private JLabel validationIcon = new JLabel(ERROR_ICON);
	
	private final AbstractExpressionParser parser;// = new ExpressionParser(true);
	private final com.rapidminer.Process controlingProcess;
	
	private JScrollPane functionButtonScrollPane;
	private JPanel functionsButtonsPanel = new JPanel();
	private GridBagLayout functionButtonsLayout = new GridBagLayout();
	private GridBagConstraints functionButtonsC = new GridBagConstraints();
	
	public ExpressionPropertyDialog(final ParameterTypeExpression type, String initialValue) {
		this(type, null, initialValue);
	}	
	
	public ExpressionPropertyDialog(ParameterTypeExpression type, Process process, String initialValue) {
		super(type, "expression");
		// create ExpressionParser with Process to enable Processfunctions
		if (process != null) {
			this.controlingProcess = process;
		} else if(type.getInputPort() != null) {
			controlingProcess = type.getInputPort().getPorts().getOwner().getOperator().getProcess();
		} else {
			controlingProcess=null;			
		}
		parser = ExpressionParserFactory.getExpressionParser(true, controlingProcess);
		
		final Vector<String> knownAttributes = new Vector<String>();
		
		InputPort inPort = ((ParameterTypeExpression)getParameterType()).getInputPort();
		if (inPort != null) {
			if (inPort.getMetaData() instanceof ExampleSetMetaData) {
				ExampleSetMetaData emd = (ExampleSetMetaData) inPort.getMetaData();
				for (AttributeMetaData amd : emd.getAllAttributes()) {
					knownAttributes.add(amd.getName());
				}
			} else if (inPort.getMetaData() instanceof ModelMetaData) {
				ModelMetaData mmd = (ModelMetaData) inPort.getMetaData();
				if (mmd != null) {
					ExampleSetMetaData emd = mmd.getTrainingSetMetaData();
					if (emd != null) {
						for (AttributeMetaData amd : emd.getAllAttributes()) {
							knownAttributes.add(amd.getName());
						}
					}
				}
			}
		}

		Collections.sort(knownAttributes);
		
		Collection<AbstractButton> buttons = new LinkedList<AbstractButton>();
		buttons.add(makeOkButton("expression_property_dialog_apply"));
		buttons.add(makeCancelButton());
		
		JPanel mainPanel = new JPanel();
		GridBagLayout mainLayout = new GridBagLayout();
		mainPanel.setLayout(mainLayout);
		GridBagConstraints mainC = new GridBagConstraints();
		mainC.fill = GridBagConstraints.BOTH;
		mainC.weightx = 1;
		mainC.weighty = 0;
		mainC.insets = new Insets(7, 7, 7, 7);
		
		JPanel expressionPanel = new JPanel();
		GridBagLayout expressionLayout = new GridBagLayout();
		expressionPanel.setLayout(expressionLayout);
		GridBagConstraints expressionC = new GridBagConstraints();
		expressionC.fill = GridBagConstraints.BOTH;
		expressionC.insets = new Insets(7, 7, 7, 7);
		expressionC.weightx = 0;
		expressionC.weighty = 0;
		
		JLabel label = new JLabel("Expression:");
		expressionLayout.setConstraints(label, expressionC);
		expressionPanel.add(label);
		
		currentExpression.setPreferredSize(new Dimension(200, 23));
		currentExpression.addKeyListener(new KeyListener() {
			@Override
			public void keyTyped(KeyEvent e) {}
			@Override
			public void keyPressed(KeyEvent e) {}
			@Override
			public void keyReleased(KeyEvent e) {
				validateExpression();
			}
		});
		expressionC.weightx = 1;
		expressionC.gridwidth = GridBagConstraints.REMAINDER;
		expressionLayout.setConstraints(currentExpression, expressionC);
		expressionPanel.add(currentExpression);
		
		expressionC.weightx = 0;
		expressionC.gridwidth = GridBagConstraints.RELATIVE;
		expressionLayout.setConstraints(validationIcon, expressionC);
		expressionPanel.add(validationIcon);
		
		Dimension dimension200to30 = new Dimension(200, 30);
		validationLabel.setPreferredSize(dimension200to30);
		validationLabel.setMinimumSize(dimension200to30);
		validationLabel.setMaximumSize(dimension200to30);
		validationLabel.setAlignmentX(SwingConstants.TOP);
		expressionC.weightx = 1;
		expressionC.gridwidth = GridBagConstraints.REMAINDER;
		expressionLayout.setConstraints(validationLabel, expressionC);
		expressionPanel.add(validationLabel);
		
		expressionC.weightx = 0;
		expressionC.gridwidth = GridBagConstraints.RELATIVE;
		JLabel hiddenLabel = new JLabel();
		expressionLayout.setConstraints(hiddenLabel, expressionC);
		expressionPanel.add(hiddenLabel);
		
		final JCheckBox allowUndeclaredBox = new JCheckBox("Allow Unknown?", false);
		allowUndeclaredBox.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (controlingProcess!= null) {
				setAllowUndeclared(allowUndeclaredBox.isSelected(),controlingProcess);
				} else {
					setAllowUndeclared(allowUndeclaredBox.isSelected());
				}
				
			}
		});
		if (controlingProcess!= null) {
			setAllowUndeclared(false, controlingProcess);
		} else {
			setAllowUndeclared(false);
		}
		expressionC.weightx = 0;
		expressionC.anchor = GridBagConstraints.WEST;
		expressionC.gridwidth = GridBagConstraints.REMAINDER;
		expressionLayout.setConstraints(allowUndeclaredBox, expressionC);
		expressionPanel.add(allowUndeclaredBox);
		
		mainC.gridwidth = GridBagConstraints.REMAINDER;
		mainLayout.setConstraints(expressionPanel, mainC);
		mainPanel.add(expressionPanel);
		
		
		
		JPanel functionPanel = new JPanel();
		GridBagLayout functionsLayout = new GridBagLayout();
		functionPanel.setLayout(functionsLayout);
		functionPanel.setBorder(BorderFactory.createTitledBorder("Functions"));
		GridBagConstraints functionsC = new GridBagConstraints();
		functionsC.fill = GridBagConstraints.BOTH;
		functionsC.insets = new Insets(7, 7, 7, 7);
		functionsC.weightx = 0;
		functionsC.weighty = 0;
		
		label = new JLabel("Type:");
		functionsLayout.setConstraints(label, functionsC);
		functionPanel.add(label);
		
		final JComboBox functionsTypeBox = new JComboBox(parser.getFunctionGroups());
		functionsTypeBox.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				updateFunctions((String)functionsTypeBox.getSelectedItem());
			}
		});
		functionsC.gridwidth = GridBagConstraints.REMAINDER;
		functionsC.weightx = 1;
		functionsLayout.setConstraints(functionsTypeBox, functionsC);
		functionPanel.add(functionsTypeBox);
		
		functionsButtonsPanel.setLayout(functionButtonsLayout);
		functionButtonsC.fill = GridBagConstraints.BOTH;
		functionButtonsC.anchor = GridBagConstraints.NORTHWEST;
		functionButtonsC.insets = new Insets(7, 7, 7, 7);
		functionButtonsC.weightx = 1;
		functionButtonsC.weighty = 0;

		updateFunctions(parser.getFunctionGroups()[0]);
		
		JPanel outerButtonPanel = new JPanel();
		outerButtonPanel.setLayout(new GridBagLayout());
		GridBagConstraints outerButtonC = new GridBagConstraints();
		outerButtonC.gridwidth = GridBagConstraints.REMAINDER;
		outerButtonC.fill = GridBagConstraints.HORIZONTAL;
		outerButtonC.weightx = 1;
		outerButtonC.weighty = 1;
		outerButtonC.anchor = GridBagConstraints.NORTHWEST;
		outerButtonPanel.add(functionsButtonsPanel, outerButtonC);
		
		outerButtonC.weighty = 1;
		outerButtonC.fill = GridBagConstraints.BOTH;
		outerButtonPanel.add(new JPanel(), outerButtonC);
		
		functionsC.weightx = 0;
		functionsC.weighty = 1;
		functionsC.anchor = GridBagConstraints.NORTH;
		functionButtonScrollPane = new JScrollPane(outerButtonPanel);
		functionButtonScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
		functionsLayout.setConstraints(functionButtonScrollPane, functionsC);
		functionPanel.add(functionButtonScrollPane);
		
		JPanel attributesPanel = new JPanel();
		GridBagLayout attributesLayout = new GridBagLayout();
		attributesPanel.setLayout(attributesLayout);
		GridBagConstraints attributesC = new GridBagConstraints();
		attributesC.fill = GridBagConstraints.BOTH;
		attributesC.insets = new Insets(7, 7, 7, 7);
		attributesC.weightx = 1;
		attributesC.weighty = 1;
		
		final JList attributeList = new JList(knownAttributes);
		attributeList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		attributeList.addMouseListener(new MouseListener() {
			@Override
			public void mouseReleased(MouseEvent e) {}
			@Override
			public void mousePressed(MouseEvent e) {}
			@Override
			public void mouseExited(MouseEvent e) {}
			@Override
			public void mouseEntered(MouseEvent e) {}
			@Override
			public void mouseClicked(MouseEvent e) {
				if (e.getClickCount() >= 2) {
					addToExpression(attributeList.getSelectedValue().toString());
				}
			}
		});
		attributesC.gridwidth = GridBagConstraints.REMAINDER;
		attributesLayout.setConstraints(attributeList, attributesC);
		attributesPanel.add(attributeList);
		
		mainC.weighty = 1;
		mainC.weightx = 0.6;
		mainC.gridwidth = GridBagConstraints.RELATIVE;
		mainLayout.setConstraints(functionPanel, mainC);
		mainPanel.add(functionPanel);
		
		mainC.weightx = 0.4;
		JScrollPane attributeScrollPane = new JScrollPane(attributesPanel);
		attributeScrollPane.setBorder(BorderFactory.createTitledBorder("Attributes"));
		mainLayout.setConstraints(attributeScrollPane, mainC);
		mainPanel.add(attributeScrollPane);
		
		layoutDefault(mainPanel, NORMAL, buttons.toArray(new AbstractButton[buttons.size()]));
		
		
		if (initialValue != null)
			currentExpression.setText(initialValue);
		
		validateExpression();
		
		setSize(850, 675);
	}
	

	private void setAllowUndeclared(boolean allowUndeclared) {
		setAllowUndeclared(allowUndeclared,null);
	}
	
	private void setAllowUndeclared(boolean allowUndeclared, com.rapidminer.Process process) {
		if(process != null) {
			parser.initParser(true,process);
		} else {
			parser.initParser(true);
		}
		if (allowUndeclared) {
			parser.setAllowUndeclared(true);
		} else {
			InputPort inPort = ((ParameterTypeExpression)getParameterType()).getInputPort();
			if (inPort != null) {
				if (inPort.getMetaData() instanceof ExampleSetMetaData) {
					ExampleSetMetaData emd = (ExampleSetMetaData) inPort.getMetaData();
					for (AttributeMetaData amd : emd.getAllAttributes()) {
						try {
							if (amd.isNominal()) {
								parser.addVariable(amd.getName(), "");
							} else {
								parser.addVariable(amd.getName(), Double.NaN);
							}							
						} catch (ExpressionParserException e) {
							LogService.getRoot().warning(e.getMessage());
						}
					}
				} else if (inPort.getMetaData() instanceof ModelMetaData) {
					ModelMetaData mmd = (ModelMetaData) inPort.getMetaData();
					if (mmd != null) {
						ExampleSetMetaData emd = mmd.getTrainingSetMetaData();
						if (emd != null) {
							for (AttributeMetaData amd : emd.getAllAttributes()) {
								try {
									if (amd.isNominal()) {
										parser.addVariable(amd.getName(), "");
									} else {
										parser.addVariable(amd.getName(), Double.NaN);
									}
								} catch (ExpressionParserException e) {
									LogService.getRoot().warning(e.getMessage());
								}
							}
						}
					}
				}
			}
		}
		validateExpression();
		requestExpressionFocus();
	}
	
	private void addToExpression(String value) {
		if (value == null)
			return;
		
		String selectedText = currentExpression.getSelectedText();
		if ((selectedText != null) && (selectedText.length() > 0)) {
			// replace selected text by function including the selection as argument (if the string to be added actually IS a function...)
			if (value.endsWith("()")) {
				int selectionStart = currentExpression.getSelectionStart();
				int selectionEnd = currentExpression.getSelectionEnd();
				String text = currentExpression.getText();
				String firstPart = text.substring(0, selectionStart);
				String lastPart = text.substring(selectionEnd);

				currentExpression.setText(firstPart + value + lastPart);
				int lengthForCaretPosition = value.length();
				if (value.endsWith("()")) {
					lengthForCaretPosition--;
				}
				currentExpression.setCaretPosition(selectionStart + lengthForCaretPosition);
				
				addToExpression(selectedText);
				currentExpression.setCaretPosition(currentExpression.getCaretPosition() + 1);
				
				validateExpression();
				requestExpressionFocus();
			} else {
				int selectionStart = currentExpression.getSelectionStart();
				int selectionEnd = currentExpression.getSelectionEnd();
				String text = currentExpression.getText();
				String firstPart = text.substring(0, selectionStart);
				String lastPart = text.substring(selectionEnd);

				currentExpression.setText(firstPart + value + lastPart);
				int lengthForCaretPosition = value.length();
				if (value.endsWith("()")) {
					lengthForCaretPosition--;
				}
				currentExpression.setCaretPosition(selectionStart + lengthForCaretPosition);
								
				validateExpression();
				requestExpressionFocus();
			}
		} else {
			// just add the text at the current caret position
			int caretPosition = currentExpression.getCaretPosition();
			String text = currentExpression.getText();
			if ((text != null) && (text.length() > 0)) {
				String firstPart = text.substring(0, caretPosition);
				String lastPart = text.substring(caretPosition);
				currentExpression.setText(firstPart + value + lastPart);

				int lengthForCaretPosition = value.length();
				if (value.endsWith("()")) {
					lengthForCaretPosition--;
				}
				currentExpression.setCaretPosition(caretPosition + lengthForCaretPosition);
			} else {
				currentExpression.setText(value);
				int lengthForCaretPosition = value.length();
				if (value.endsWith("()")) {
					lengthForCaretPosition--;
				}
				currentExpression.setCaretPosition(caretPosition + lengthForCaretPosition);
				currentExpression.setCaretPosition(lengthForCaretPosition);
			}
			
			validateExpression();
			requestExpressionFocus();
		}
	}
	
	private void requestExpressionFocus() {
		currentExpression.requestFocusInWindow();
	}
	
	private void updateFunctions(String functionGroup) {
		// hack to prevent extremely wide rows due to long function names
		// future function groups may need to be added as well
		int funtionRowLength = FUNCTION_ROW_LENGTH;
		if (functionGroup.equals("Date")) {
			funtionRowLength -= 1;
		}
		functionsButtonsPanel.removeAll();
		
		List<FunctionDescription> functions = parser.getFunctions(functionGroup);
		functionButtonsC.gridwidth = 1;
		// dummy components so buttons always start at the top-left corner
		for (int i=0; i<funtionRowLength-1; i++) {
			functionsButtonsPanel.add(new JLabel(), functionButtonsC);
		}
		functionButtonsC.gridwidth = GridBagConstraints.REMAINDER;
		functionsButtonsPanel.add(new JLabel(), functionButtonsC);
		functionButtonsC.gridwidth = 1;
		
		int index = 1;
		for (final FunctionDescription currentFunction : functions) {			
			JButton currentButton = new JButton();
			currentButton.setText(currentFunction.getDisplayName());
			String argumentString = null;
			int numberOfArguments = currentFunction.getNumberOfArguments();
			if (numberOfArguments == FunctionDescription.UNLIMITED_NUMBER_OF_ARGUMENTS) {
				argumentString = "unlimited arguments";
			} else if (numberOfArguments == 1) {
				argumentString = "1 argument";
			} else {
				argumentString = numberOfArguments + " arguments";
			}
			currentButton.setToolTipText("<html><b>" + currentFunction.getHelpTextName() + "</b>: " + currentFunction.getDescription() + " (<i>" + argumentString + "</i>)</html>");
			currentButton.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					addToExpression(currentFunction.getDisplayName());
				}
			});
			functionButtonsLayout.setConstraints(currentButton, functionButtonsC);
			functionsButtonsPanel.add(currentButton);
			
			index++;
			
			if (index % funtionRowLength == 0) {
				index = 0;
				functionButtonsC.gridwidth = GridBagConstraints.REMAINDER;
			} else {
				functionButtonsC.gridwidth = 1;		
			}
		}
		
		functionsButtonsPanel.revalidate();
		requestExpressionFocus();
	}
	
	private void validateExpression() {
		String expression = currentExpression.getText();
		if (expression != null) {
			if (expression.length() > 0) {				
				if (parser.hasError()) {
					validationIcon.setIcon(ERROR_ICON);
					validationLabel.setText("<html><b>Error: </b>" + parser.getErrorInfo() + "</html>");
					return;
				} else {
					validationIcon.setIcon(OK_ICON);
					validationLabel.setText("Expression is syntactically correct.");			
				}
			} else {
				validationIcon.setIcon(ERROR_ICON);
				validationLabel.setText("<html><b>Warning: </b>Please specify a valid expression.</html>");
			}
		} else {
			validationIcon.setIcon(ERROR_ICON);
			validationLabel.setText("<html><b>Warning: </b>Please specify a valid expression.</html>");
		}
	}
	
	public String getExpression() {
		return currentExpression.getText();
	}
	
	@Override
	protected String getInfoText() {
		//Hint 1: no function symbols (parentheses etc.) are allowed. 
		//Hint 2: validation may not work if unknown attributes or macros are included
		return "<html><p>" + 
			   "Please specify a valid expression in this dialog. Expressions can consist of numbers, text, constants (like Pi, e true, or false), functions, and variable names. " +
			   "Fractions in numbers are indicated by '.' and nominal text has to be quoted with double quotes. The possible functions can be selected by clicking on them below. " + 
			   "Variables can be attribute names (select them by double clicking below or type in the name) or macros. " +
			   "Hence, used attribute names are not allowed to contain parentheses or other function symbols. " +
			   "All changes will result in a validation check. The icon on the right shows if the check was successful, a tool tip gives you more information." +
			   "</p></html>";
	}
}
