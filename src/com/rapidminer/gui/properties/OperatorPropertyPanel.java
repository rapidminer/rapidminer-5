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
package com.rapidminer.gui.properties;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.LinkedList;
import java.util.List;

import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JToggleButton;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import com.rapidminer.BreakpointListener;
import com.rapidminer.Process;
import com.rapidminer.gui.MainFrame;
import com.rapidminer.gui.actions.ToggleAction.ToggleActionListener;
import com.rapidminer.gui.operatortree.actions.DeleteOperatorAction;
import com.rapidminer.gui.operatortree.actions.InfoOperatorAction;
import com.rapidminer.gui.operatortree.actions.ToggleActivationItem;
import com.rapidminer.gui.processeditor.ProcessEditor;
import com.rapidminer.gui.properties.celleditors.value.PropertyValueCellEditor;
import com.rapidminer.gui.tools.ExtendedJScrollPane;
import com.rapidminer.gui.tools.ResourceAction;
import com.rapidminer.gui.tools.ResourceDockKey;
import com.rapidminer.gui.tools.ResourceLabel;
import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.gui.tools.ViewToolBar;
import com.rapidminer.gui.tools.components.ToggleDropDownButton;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorVersion;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.Parameters;
import com.rapidminer.tools.Observable;
import com.rapidminer.tools.Observer;
import com.vlsolutions.swing.docking.DockKey;
import com.vlsolutions.swing.docking.Dockable;

/** This panel displays parameters of an operator. It refreshes in either of these cases:
 *  <ul>
 *    <li>A new operator is selected.</li>
 *    <li>The {@link Parameters} of the current operator (which are observed) change
 *      in a way such that the parameter value differs from the one displayed by the editor.
 *      This should only happen if a parameter value is changed programmatically, e.g. by an
 *      operator.</li>
 *    <li>{@link #processUpdated(Process)} is called and {@link #getProperties()} returns a 
 *      different list than the one returned during the last {@link #setupComponents()}.</li>
 *    <li>When changing to expert mode.</li>
 *  </ul>  
 * 
 * @author Simon Fischer, Tobias Malbrecht
 *
 */
public class OperatorPropertyPanel extends PropertyPanel implements Dockable, ProcessEditor {	

	private static final long serialVersionUID = 6056794546696461864L;
	
	private class BreakpointButton extends ToggleDropDownButton implements ToggleActionListener {

		private static final long serialVersionUID = 7364886954405951709L;
		
		private final Icon IMAGE_BREAKPOINTS = SwingTools.createIcon("16/breakpoints.png");
		private final Icon IMAGE_BREAKPOINT_BEFORE = SwingTools.createIcon("16/breakpoint_up.png");
		private final Icon IMAGE_BREAKPOINT_AFTER = SwingTools.createIcon("16/breakpoint_down.png");

		{
			for (int i = 0; i < mainFrame.getActions().TOGGLE_BREAKPOINT.length; i++) {
				mainFrame.getActions().TOGGLE_BREAKPOINT[i].addToggleActionListener(this);	
			}
		}
		
		public BreakpointButton() {
			super(new ResourceAction(true, "breakpoint_after") {
				private static final long serialVersionUID = -8913366165786891652L;
				
				@Override
				public void actionPerformed(ActionEvent e) {
					if (mainFrame.getActions().TOGGLE_BREAKPOINT[0].isSelected() ||
						mainFrame.getActions().TOGGLE_BREAKPOINT[1].isSelected()) {
						mainFrame.getActions().TOGGLE_BREAKPOINT[0].resetAction(false);
						mainFrame.getActions().TOGGLE_BREAKPOINT[1].resetAction(false);
						return;
					}
					mainFrame.getActions().TOGGLE_BREAKPOINT[1].actionPerformed(null);
				}
			});
		}
		
		@Override
		public void setSelected(boolean selected) {
			Icon breakpointIcon;
			if (operator != null && operator.hasBreakpoint()) {
				super.setSelected(true);
					if (operator.getNumberOfBreakpoints() == 1) {
					if (operator.hasBreakpoint(BreakpointListener.BREAKPOINT_BEFORE)) {
						breakpointIcon = IMAGE_BREAKPOINT_BEFORE;
					} else {
						breakpointIcon = IMAGE_BREAKPOINT_AFTER;
					}
				} else {
					breakpointIcon = IMAGE_BREAKPOINTS;
				}
			} else {
				super.setSelected(false);
				breakpointIcon = IMAGE_BREAKPOINT_AFTER;
			}
			setIcon(breakpointIcon);
		}
		
		@Override
		protected JPopupMenu getPopupMenu() {
			JPopupMenu menu = new JPopupMenu();
			for (int i = 0; i < mainFrame.getActions().TOGGLE_BREAKPOINT.length; i++) {
				menu.add(mainFrame.getActions().TOGGLE_BREAKPOINT[i].createMenuItem());	
			}
			return menu;
		}
		
	}

	private final BreakpointButton breakpointButton;
	
	private final MainFrame mainFrame;
	
	private static final Icon WARNING_ICON = SwingTools.createIcon("16/sign_warning.png");
	
	private final JLabel headerLabel = new JLabel("");
	
	private final Font selectedFont = headerLabel.getFont().deriveFont(Font.BOLD);
	
	private final Font unselectedFont = headerLabel.getFont();
	
	private final JLabel expertModeHintLabel = new JLabel("");
	
	private Operator operator;
	
	private final Observer<String> parameterObserver = new Observer<String>() {
		@Override
		public void update(Observable<String> observable, String key) {
			PropertyValueCellEditor editor = getEditorForKey(key);
			if (editor != null) {				
				ParameterType type = operator.getParameters().getParameterType(key);
				String editorValue = type.toString(editor.getCellEditorValue());
				String opValue = operator.getParameters().getParameterOrNull(key);
				if (((opValue != null) && (editorValue == null)) ||
					((opValue == null) && (editorValue != null)) ||
					((opValue != null) && (editorValue != null) && !opValue.equals(editorValue))) {
					editor.getTableCellEditorComponent(null, opValue, false, 0, 1);
				}				
			} else {
				setupComponents();			
			}
		}
	};

	private JSpinner compatibilityLevelSpinner = new JSpinner(new CompatibilityLevelSpinnerModel());
	private ResourceLabel compatibilityLabel = new ResourceLabel("compatibility_level");
	private JPanel compatibilityPanel = new JPanel(new FlowLayout(FlowLayout.LEADING));;

	public OperatorPropertyPanel(final MainFrame mainFrame) {
		super();
		this.mainFrame = mainFrame;
		breakpointButton = new BreakpointButton();
		headerLabel.setHorizontalAlignment(SwingConstants.CENTER);
		expertModeHintLabel.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
		expertModeHintLabel.setIcon(WARNING_ICON);
		expertModeHintLabel.addMouseListener(new MouseListener() {
			public void mouseReleased(MouseEvent e) {
				mainFrame.TOGGLE_EXPERT_MODE_ACTION.actionPerformed(null);
			}
			public void mouseClicked(MouseEvent e) {}
			public void mouseEntered(MouseEvent e) {}
			public void mouseExited(MouseEvent e) {}
			public void mousePressed(MouseEvent e) {}
		});
		expertModeHintLabel.setCursor(new Cursor(Cursor.HAND_CURSOR));
		expertModeHintLabel.setHorizontalAlignment(SwingConstants.LEFT);
		setupComponents();
		
		compatibilityLevelSpinner.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				// compatibility level
				OperatorVersion[] versionChanges = operator.getIncompatibleVersionChanges();
				if (versionChanges.length != 0) {
					OperatorVersion latestChange = versionChanges[versionChanges.length - 1];
					if (latestChange.isAtLeast(operator.getCompatibilityLevel())) {
						compatibilityLabel.setIcon(WARNING_ICON);				
					} else {
						compatibilityLabel.setIcon(SwingTools.createIcon("16/ok.png"));					
					}
				}
			}
		});
	}
	
	@Override
	protected String getValue(ParameterType type) {
		return operator.getParameters().getParameterOrNull(type.getKey());		
	}

	@Override
	protected void setValue(Operator operator, ParameterType type, String value) {
		if (value.length() == 0) {
			value = null;
		}
		operator.setParameter(type.getKey(), value);
	}

	@Override
	protected List<ParameterType> getProperties() {
		List<ParameterType> visible = new LinkedList<ParameterType>();
		int hidden = 0;
		if (operator != null) {
			for (ParameterType type : operator.getParameters().getParameterTypes()) {
				if (type.isHidden()) { 
					continue;
				}
				if (!isExpertMode() && type.isExpert()) {
					hidden++;
					continue;				
				}
				visible.add(type);
			}
		}

		if (hidden > 0) {
			expertModeHintLabel.setText(hidden + " hidden expert parameter" + (hidden == 1 ? "" : "s"));
			expertModeHintLabel.setVisible(true);
		} else {
			expertModeHintLabel.setVisible(false);
		}
		return visible;
	}

	@Override
	public void processChanged(Process process) { }

	@Override
	public void processUpdated(Process process) {
		setNameFor(operator);
		// check if we have editors for the current parameters. If not, refresh.		
		int count = 0; // count hits. If we have to many, also refresh
		List<ParameterType> properties = getProperties();
		if (properties.size() != getNumberOfEditors()) {
			setupComponents();
			return;
		}
		for (ParameterType type : properties) {
			if (hasEditorFor(type)) {
				count++;
			} else {		
				setupComponents();
				return;
			}
		}
		if (count != properties.size()) {
			setupComponents();
		}
	}
	
	@Override
	public void setSelection(List<Operator> selection) {
		Operator operator = selection.isEmpty() ? null : selection.get(0);
		if (operator == this.operator) {
			return;
		}
		if (this.operator != null) {
			this.operator.getParameters().removeObserver(parameterObserver);
		}
		this.operator = operator;	
		if (operator != null) {
			this.operator.getParameters().addObserver(parameterObserver, true);
			breakpointButton.setEnabled(true);
			
			// compatibility level
			OperatorVersion[] versionChanges = operator.getIncompatibleVersionChanges();
			if (versionChanges.length == 0) {
				// no incompatible versions exist
				compatibilityLevelSpinner.setVisible(false);
				compatibilityLabel.setVisible(false);
			} else {
				compatibilityLevelSpinner.setVisible(true);
				compatibilityLabel.setVisible(true);
				((CompatibilityLevelSpinnerModel)compatibilityLevelSpinner.getModel()).setOperator(operator);
			}
			
		} else {
			breakpointButton.setEnabled(false);
		}
		setNameFor(operator);
		setupComponents();
	}

	private void setNameFor(Operator operator) {
		if (operator != null) {
			headerLabel.setFont(selectedFont);
			if (operator.getName().equals(operator.getOperatorDescription().getName())) {
				headerLabel.setText(operator.getName());				
			} else {
				headerLabel.setText(operator.getName() + " (" + operator.getOperatorDescription().getName() + ")");
			}
			headerLabel.setIcon(operator.getOperatorDescription().getSmallIcon());

		} else {
			headerLabel.setFont(unselectedFont);
			headerLabel.setText("No Operator Selected");
			headerLabel.setIcon(null);
		}
	}

	@Override
	public Component getComponent() {
		if (dockableComponent == null) {
			JScrollPane scrollPane = new ExtendedJScrollPane(this);
			scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
			scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
			scrollPane.setBorder(null);

			dockableComponent = new JPanel(new BorderLayout());

			JPanel toolBarPanel = new JPanel(new BorderLayout());
			ViewToolBar toolBar = new ViewToolBar();
			JToggleButton toggleExpertModeButton = mainFrame.TOGGLE_EXPERT_MODE_ACTION.createToggleButton();
			toggleExpertModeButton.setText(null);
			toolBar.add(toggleExpertModeButton);
			Action infoOperatorAction = new InfoOperatorAction() {
				private static final long serialVersionUID = 6758272768665592429L;

				@Override
				protected Operator getOperator() {
					return mainFrame.getFirstSelectedOperator();
				}		
			};
			toolBar.add(infoOperatorAction);
			JToggleButton enableOperatorButton = new ToggleActivationItem(mainFrame.getActions()).createToggleButton();
			enableOperatorButton.setText(null);
			toolBar.add(enableOperatorButton);
			Action renameOperatorAction = new ResourceAction(true, "rename_in_processrenderer") {
				{
					setCondition(OPERATOR_SELECTED, MANDATORY);
				}
				
				private static final long serialVersionUID = -3104160320178045540L;
				
				@Override
				public void actionPerformed(ActionEvent e) {
					Operator operator = mainFrame.getFirstSelectedOperator();
					String name = SwingTools.showInputDialog("rename_operator", operator.getName());
					if (name != null && name.length() > 0) {
						operator.rename(name);
					}
				}
			};
			toolBar.add(renameOperatorAction);
			toolBar.add(new DeleteOperatorAction());
			breakpointButton.addToToolBar(toolBar);
//			toolBar.add(mainFrame.getActions().MAKE_DIRTY_ACTION);			
			toolBarPanel.add(toolBar, BorderLayout.NORTH);
			
			JPanel headerPanel = new JPanel();
			headerPanel.setBackground(SwingTools.LIGHTEST_BLUE);
			headerPanel.add(headerLabel);
			headerPanel.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Color.LIGHT_GRAY));
			toolBarPanel.add(headerPanel, BorderLayout.SOUTH);
			
			dockableComponent.add(toolBarPanel, BorderLayout.NORTH);
			dockableComponent.add(scrollPane, BorderLayout.CENTER);
				
			//compatibility level and warnings
			JPanel southPanel = new JPanel(new BorderLayout());
			southPanel.add(expertModeHintLabel, BorderLayout.CENTER);
			compatibilityLabel.setLabelFor(compatibilityLevelSpinner);
			compatibilityLevelSpinner.setPreferredSize(new Dimension(80, (int) compatibilityLevelSpinner.getPreferredSize().getHeight()));			
			compatibilityPanel.add(compatibilityLabel);
			compatibilityPanel.add(compatibilityLevelSpinner);
			southPanel.add(compatibilityPanel, BorderLayout.SOUTH);
			
			dockableComponent.add(southPanel, BorderLayout.SOUTH);
		}
		return dockableComponent;
	}

	// implements Dockable

	public static final String PROPERTY_EDITOR_DOCK_KEY = "property_editor";
	private final DockKey DOCK_KEY = new ResourceDockKey(PROPERTY_EDITOR_DOCK_KEY);
	{
		DOCK_KEY.setDockGroup(MainFrame.DOCK_GROUP_ROOT);
	}
	private JPanel dockableComponent;

	@Override
	public DockKey getDockKey() {
		return DOCK_KEY;
	}

	public boolean isExpertMode() {
		return mainFrame.TOGGLE_EXPERT_MODE_ACTION.isSelected();
	}

	@Override
	protected Operator getOperator() {
		return operator;
	}
}
