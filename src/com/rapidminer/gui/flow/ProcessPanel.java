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
package com.rapidminer.gui.flow;


import java.awt.BorderLayout;
import java.awt.Component;
import java.util.List;

import javax.swing.Action;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JToggleButton;
import javax.swing.JViewport;

import com.rapidminer.Process;
import com.rapidminer.gui.MainFrame;
import com.rapidminer.gui.actions.AutoWireAction;
import com.rapidminer.gui.processeditor.ProcessEditor;
import com.rapidminer.gui.tools.PrintingTools;
import com.rapidminer.gui.tools.ResourceDockKey;
import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.gui.tools.ViewToolBar;
import com.rapidminer.gui.tools.components.DropDownButton;
import com.rapidminer.operator.ExecutionUnit;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorChain;
import com.rapidminer.operator.ports.metadata.CompatibilityLevel;
import com.vlsolutions.swing.docking.DockKey;
import com.vlsolutions.swing.docking.Dockable;

/**
 * Contains the main {@link ProcessRenderer} and a {@link ProcessButtonBar}
 * to navigate through the process.
 * 
 * @author Simon Fischer, Tobias Malbrecht
 */
public class ProcessPanel extends JPanel implements Dockable, ProcessEditor {

	private static final long serialVersionUID = -4419160224916991497L;

	private final ProcessRenderer renderer;
	
	private final ProcessButtonBar processButtonBar;

	private OperatorChain operatorChain;
	
	private JScrollPane scrollPane;
	
	public ProcessPanel(final MainFrame mainFrame) {
		processButtonBar = new ProcessButtonBar(mainFrame);
		renderer = new ProcessRenderer(this, mainFrame);
		renderer.setBackground(SwingTools.LIGHTEST_BLUE);
		
		ViewToolBar toolBar = new ViewToolBar();
		toolBar.add(processButtonBar);
		final Action autoWireAction = new AutoWireAction(mainFrame, "wire", CompatibilityLevel.PRE_VERSION_5, false, true);
		DropDownButton autoWireDropDownButton = DropDownButton.makeDropDownButton(autoWireAction);
		autoWireDropDownButton.add(autoWireAction);
		autoWireDropDownButton.add(new AutoWireAction(mainFrame, "wire_recursive", CompatibilityLevel.PRE_VERSION_5, true, true));
		autoWireDropDownButton.add(new AutoWireAction(mainFrame, "rewire", CompatibilityLevel.PRE_VERSION_5, false, false));
		autoWireDropDownButton.add(new AutoWireAction(mainFrame, "rewire_recursive", CompatibilityLevel.PRE_VERSION_5, true, false));
		autoWireDropDownButton.addToToolBar(toolBar, ViewToolBar.RIGHT);
		toolBar.add(renderer.ARRANGE_OPERATORS_ACTION, ViewToolBar.RIGHT);
		toolBar.add(renderer.getFlowVisualizer().SHOW_ORDER_TOGGLEBUTTON, ViewToolBar.RIGHT);
		toolBar.add(renderer.AUTO_FIT_ACTION, ViewToolBar.RIGHT);
		JToggleButton toggleRealMetadataPropagationButton = new JToggleButton(mainFrame.PROPAGATE_REAL_METADATA_ACTION);
		toggleRealMetadataPropagationButton.setText("");
		toolBar.add(toggleRealMetadataPropagationButton, ViewToolBar.RIGHT);
		
		String name = "process";
		if (mainFrame.getActions().getProcess() != null &&
			mainFrame.getActions().getProcess().getProcessLocation() != null) {
			name = mainFrame.getActions().getProcess().getProcessLocation().getShortName();
		}
		DropDownButton exportDropDownButton = PrintingTools.makeExportPrintDropDownButton(renderer, name);
		exportDropDownButton.addToToolBar(toolBar, ViewToolBar.RIGHT);

		setLayout(new BorderLayout());
		add(toolBar, BorderLayout.NORTH);
		
		scrollPane = new JScrollPane(renderer);
		scrollPane.getVerticalScrollBar().setUnitIncrement(10);
		scrollPane.setBorder(null);
		add(scrollPane, BorderLayout.CENTER);
	}

	public void showOperatorChain(OperatorChain operatorChain) {
		this.operatorChain = operatorChain;
		if (operatorChain == null) {
			processButtonBar.setSelectedNode(null);
			renderer.showOperatorChain(null);
		} else {
			renderer.showOperatorChain(operatorChain);
			processButtonBar.setSelectedNode(operatorChain);
			StringBuilder b = new StringBuilder("<html><strong>");
			b.append(operatorChain.getName());
			b.append("</strong>&nbsp;<emph>(");
			b.append(operatorChain.getOperatorDescription().getName());
			b.append(")</emph><br/>");
			b.append("Subprocesses: ");
			boolean first = true;
			for (ExecutionUnit executionUnit : operatorChain.getSubprocesses()) {
				if (first) {
					first = false;
				} else {
					b.append(", ");
				}
				b.append("<em>");
				b.append(executionUnit.getName());
				b.append("</em> (");
				b.append(executionUnit.getOperators().size());
				b.append(" operators)");
			}

			b.append("</html>");
		}
	}

	public void setSelection(List<Operator> selection) {
		Operator first = selection.isEmpty() ? null : selection.get(0);
		if (first != null) {
			processButtonBar.addToHistory(first);
		}
		if (renderer.getSelection().equals(selection)) {
			return;
		}
		if (first == null) {
			showOperatorChain(null);
		} else {
			OperatorChain target;
			if (first instanceof OperatorChain) {
				target = (OperatorChain) first;
			} else {
				target = first.getParent();
			}
			showOperatorChain(target);
			renderer.setSelection(selection);
		}
	}

	@Override
	public void processChanged(Process process) {
		processButtonBar.clearHistory();
		renderer.processChanged();
	}

	@Override
	public void processUpdated(Process process) {
		renderer.processUpdated();
		processButtonBar.setSelectedNode(this.operatorChain);
	}

	public ProcessRenderer getProcessRenderer() {
		return renderer;
	}

	
	public static final String PROCESS_PANEL_DOCK_KEY = "process_panel";
	private final DockKey DOCK_KEY = new ResourceDockKey(PROCESS_PANEL_DOCK_KEY);
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

	public JViewport getViewPort() {
		return scrollPane.getViewport();
	}
}
