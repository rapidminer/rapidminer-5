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
package com.rapidminer.gui.actions;

import java.awt.event.ActionEvent;
import java.util.LinkedList;
import java.util.List;

import javax.swing.Action;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;

import com.rapidminer.BreakpointListener;
import com.rapidminer.Process;
import com.rapidminer.ProcessListener;
import com.rapidminer.gui.ConditionalAction;
import com.rapidminer.gui.MainFrame;
import com.rapidminer.gui.dnd.OperatorTransferHandler;
import com.rapidminer.gui.flow.AutoWireThread;
import com.rapidminer.gui.operatormenu.OperatorMenu;
import com.rapidminer.gui.operatortree.actions.DeleteOperatorAction;
import com.rapidminer.gui.operatortree.actions.InfoOperatorAction;
import com.rapidminer.gui.operatortree.actions.SaveBuildingBlockAction;
import com.rapidminer.gui.operatortree.actions.ToggleActivationItem;
import com.rapidminer.gui.operatortree.actions.ToggleAllBreakpointsItem;
import com.rapidminer.gui.operatortree.actions.ToggleBreakpointItem;
import com.rapidminer.gui.processeditor.ProcessEditor;
import com.rapidminer.gui.templates.NewBuildingBlockMenu;
import com.rapidminer.gui.tools.EditBlockingProgressThread;
import com.rapidminer.gui.tools.ResourceAction;
import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.operator.ExecutionUnit;
import com.rapidminer.operator.IOContainer;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorChain;
import com.rapidminer.operator.ProcessRootOperator;


/**
 * A process editor that enables/disables actions depending
 * on the selection of operators.
 * 
 * @author Simon Fischer, Tobias Malbrecht
 */
public class Actions implements ProcessEditor {

	public final Action INFO_OPERATOR_ACTION = new InfoOperatorAction() {
		private static final long serialVersionUID = 6758272768665592429L;

		@Override
		protected Operator getOperator() {
			return getFirstSelectedOperator();
		}		
	};
	
	public final ToggleActivationItem TOGGLE_ACTIVATION_ITEM = new ToggleActivationItem(this);
	
	public final Action RENAME_OPERATOR_ACTION = new ResourceAction(true, "rename_in_processrenderer") {
		{
			setCondition(OPERATOR_SELECTED, MANDATORY);
		}
		
		private static final long serialVersionUID = -3104160320178045540L;
		
		@Override
		public void actionPerformed(ActionEvent e) {
			Operator operator = getFirstSelectedOperator();
			String name = SwingTools.showInputDialog("rename_operator", operator.getName());
			if (name != null && name.length() > 0) {
				operator.rename(name);
			}
		}
	};
	
	public final Action NEW_OPERATOR_ACTION = new NewOperatorAction(this);
	
	public final Action NEW_BUILDING_BLOCK_ACTION = new NewBuildingBlockAction(this);
	
	public final Action SAVE_BUILDING_BLOCK_ACTION = new SaveBuildingBlockAction(this);
	
//	public final Action CUT_ACTION = new CutAction(this);
//	
//	public final Action COPY_ACTION = new CopyAction(this);
//	
//	public final Action PASTE_ACTION = new PasteAction(this);
	
	public final Action DELETE_OPERATOR_ACTION = new DeleteOperatorAction();
	
	public final ToggleBreakpointItem TOGGLE_BREAKPOINT[] = { 
			new ToggleBreakpointItem(this, BreakpointListener.BREAKPOINT_BEFORE), 
			new ToggleBreakpointItem(this, BreakpointListener.BREAKPOINT_AFTER) 
	};
	
	public transient final ToggleAllBreakpointsItem TOGGLE_ALL_BREAKPOINTS = new ToggleAllBreakpointsItem(this);
	
	public transient final Action MAKE_DIRTY_ACTION = new ResourceAction(true, "make_dirty") {				
		private static final long serialVersionUID = -1260942717363137733L;

		{
			setCondition(OPERATOR_SELECTED, MANDATORY);
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			for (Operator selectedOperator : new LinkedList<Operator>(getSelectedOperators())) {				
				selectedOperator.makeDirty();
			}
		}				
	};

	private List<Operator> selection;
	
	private Process process;

//	/** The current clip board, i.e. the selected operator before cut or copy was applied. */
//	private transient List<Operator> clipBoard = null;

	private final MainFrame mainFrame;
	
	private final BreakpointListener breakpointListener = new BreakpointListener() {
		@Override
		public void breakpointReached(Process process, Operator op, IOContainer iocontainer, int location) {
			enableActions();
		}
		@Override
		public void resume() {
			enableActions();
		}
	};
	private final ProcessListener processListener = new ProcessListener() {
		@Override
		public void processEnded(Process process) {
			enableActions();
			mainFrame.RUN_ACTION.setState(process.getProcessState());
		}
		@Override
		public void processFinishedOperator(Process process, Operator op) { }
		@Override
		public void processStartedOperator(Process process, Operator op) { }
		@Override
		public void processStarts(Process process) {
			enableActions();
		}		
	};

	public Actions(MainFrame mainFrame) {
		this.mainFrame = mainFrame;
//		copyProperties(new ResourceActionAdapter("cut"), TransferHandler.getCutAction());
//		copyProperties(new ResourceActionAdapter("copy"), TransferHandler.getCopyAction());
//		copyProperties(new ResourceActionAdapter("paste"), TransferHandler.getPasteAction());
	}
	
	/** Creates a new popup menu for the selected operator. */
	public void addToOperatorPopupMenu(JPopupMenu menu, Action renameAction) {
		final Operator op = getFirstSelectedOperator();
		final boolean singleSelection = getSelectedOperators().size() == 1;

		if (op != null && !singleSelection) {
			if (!(op instanceof ProcessRootOperator) &&
					(op.getParent() != null)) {
				// enable / disable operator
				menu.add(TOGGLE_ACTIVATION_ITEM.createMultipleActivationItem());				
			}			
		}
		
		if (op != null && singleSelection) {
			menu.add(INFO_OPERATOR_ACTION);

			if (!(op instanceof ProcessRootOperator) &&
					(op.getParent() != null)) {
				// enable / disable operator
				menu.add(TOGGLE_ACTIVATION_ITEM.createMenuItem());				
			}

			if (renameAction != null) {
				menu.add(renameAction);
			} else {
				menu.add(RENAME_OPERATOR_ACTION);
			}
			
			menu.addSeparator();
		}
		
		if ((op != null) && (op instanceof OperatorChain)) {
			menu.add(OperatorMenu.NEW_OPERATOR_MENU);
		}

		if ((op != null) && (!(op instanceof ProcessRootOperator)) && singleSelection) {
			if ((op instanceof OperatorChain) && (((OperatorChain) op).getAllInnerOperators().size() > 0)) {
				menu.add(OperatorMenu.REPLACE_OPERATORCHAIN_MENU);
			} else {
				menu.add(OperatorMenu.REPLACE_OPERATOR_MENU);
			}
		}

		// add building block menu
		if ((op != null) && (op instanceof OperatorChain)) {
			final NewBuildingBlockMenu buildingBlockMenu = new NewBuildingBlockMenu(this);
			menu.add(buildingBlockMenu);
			buildingBlockMenu.addMenuListener(new MenuListener() {
				public void menuCanceled(MenuEvent e) {}
				public void menuDeselected(MenuEvent e) {}
				public void menuSelected(MenuEvent e) {
					buildingBlockMenu.addAllMenuItems();
				}
			});
		}
		if ((op != null) && (!(op instanceof ProcessRootOperator))) {
			menu.add(SAVE_BUILDING_BLOCK_ACTION);
		}
		
		menu.addSeparator();
//		menu.add(COPY_ACTION);
//		menu.add(CUT_ACTION);
//		menu.add(PASTE_ACTION);
		OperatorTransferHandler.installMenuItems(menu);
//		menu.add(DELETE_OPERATOR_ACTION);
		
		menu.addSeparator();
		if ((op != null) && singleSelection) {
			for (int i = 0; i < TOGGLE_BREAKPOINT.length; i++) {
				menu.add(TOGGLE_BREAKPOINT[i].createMenuItem());	
			}
		}
		menu.add(TOGGLE_ALL_BREAKPOINTS.createMenuItem());
//		if ((op != null) && (!(op instanceof ProcessRootOperator)) && singleSelection) {
//			menu.add(MAKE_DIRTY_ACTION);
//		}

	}

	public Operator getFirstSelectedOperator() {
		if ((selection != null) && !selection.isEmpty()) {
			return selection.get(0);
		} else {
			return null;
		}
	}

	public List<Operator>getSelectedOperators() {
		return selection;
	}

	/**
	 * Enables and disables all actions according to the current state
	 * (process running, operator selected...
	 */
	public void enableActions() {
		if (SwingUtilities.isEventDispatchThread()) {
			enableActionsNow();
		} else {
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					enableActionsNow();
				}
			});
		}
		updateCheckboxStates();
	}

	private void enableActionsNow() {		
		synchronized (process) {
			boolean[] currentStates = new boolean[ConditionalAction.NUMBER_OF_CONDITIONS];
			Operator op = getFirstSelectedOperator();
			if (op != null) {
				currentStates[ConditionalAction.OPERATOR_SELECTED] = true;
				if (op instanceof OperatorChain)
					currentStates[ConditionalAction.OPERATOR_CHAIN_SELECTED] = true;
				if (op.getParent() == null) {
					currentStates[ConditionalAction.ROOT_SELECTED] = true;
				} else {
					currentStates[ConditionalAction.PARENT_ENABLED] = op.getParent().isEnabled();
					if (op.getExecutionUnit().getNumberOfOperators() > 1) {
						currentStates[ConditionalAction.SIBLINGS_EXIST] = true;
					}
				}
			}

			int processState = process.getProcessState();
			currentStates[ConditionalAction.PROCESS_STOPPED] = processState == Process.PROCESS_STATE_STOPPED;
			currentStates[ConditionalAction.PROCESS_PAUSED] = processState == Process.PROCESS_STATE_PAUSED;
			currentStates[ConditionalAction.PROCESS_RUNNING] = processState == Process.PROCESS_STATE_RUNNING;
			currentStates[ConditionalAction.EDIT_IN_PROGRESS] = EditBlockingProgressThread.isEditing();
			currentStates[ConditionalAction.PROCESS_SAVED] = process.hasSaveDestination();
			currentStates[ConditionalAction.PROCESS_RENDERER_IS_VISIBLE] = mainFrame.getProcessPanel().getProcessRenderer().isShowing();
			currentStates[ConditionalAction.PROCESS_RENDERER_HAS_UNDO_STEPS] = mainFrame.hasUndoSteps();
			currentStates[ConditionalAction.PROCESS_RENDERER_HAS_REDO_STEPS] = mainFrame.hasRedoSteps();
			ConditionalAction.updateAll(currentStates);
			updateCheckboxStates();
		}
	}

//	/** Cuts the currently selected operator into the clipboard. */
//	public void cut() {
//		copy();
//		delete();
//	}
//
//	/** Copies the currently selected operator into the clipboard. */
//	public void copy() {
//		Operator selectedOperator = getSelectedOperator();
//		if (selectedOperator != null) {
//			List<Operator> clones = new LinkedList<Operator>();
//			for (Operator op : getSelectedOperators()) {
//				clones.add(op.cloneOperator(op.getName()));
//			}
//			clipBoard = clones;
//			enableActions();
//		}
//	}
//
//	/** Pastes the current clipboard into the tree. */
//	public void paste() {
//		if (clipBoard != null) {
//			insert(clipBoard);
//			List<Operator> clones = new LinkedList<Operator>();
//			for (Operator op : clipBoard) {
//				clones.add(op.cloneOperator(op.getName()));
//			}
//			clipBoard = clones;
//		}
//		enableActions();
//	}

	/** The currently selected operator will be deleted. */
	public void delete() {
		Operator parent = null;
		for (Operator selectedOperator : new LinkedList<Operator>(getSelectedOperators())) {				
			if (parent == null) {
				parent = selectedOperator.getParent();
			}
			if (selectedOperator instanceof ProcessRootOperator) {
				return;
			}
			selectedOperator.remove();
		}		
		mainFrame.selectOperator(parent);
	}
	
	/** The given operators will be inserted at the last position of the currently selected operator chain. 
	 */
	public void insert(List<Operator> newOperators) {
		Object selectedNode = getSelectedOperator();
		if (selectedNode == null) {
			SwingTools.showVerySimpleErrorMessage("cannot_insert_operator");
			return;
		} else if ((selectedNode instanceof OperatorChain) && (((OperatorChain)selectedNode).getNumberOfSubprocesses() == 1)) {
			for (Operator newOperator : newOperators) {
				((OperatorChain)selectedNode).getSubprocess(0).addOperator(newOperator);
			}
		} else {
			int i = 0;
			Operator selectedOperator = (Operator)selectedNode;
			ExecutionUnit process = selectedOperator.getExecutionUnit();
			int parentIndex = process.getOperators().indexOf(selectedOperator) + 1;
			for (Operator newOperator : newOperators) {
				process.addOperator(newOperator, parentIndex + i);
				i++;
			}
		}
				
		AutoWireThread.autoWireInBackground(newOperators, true);
		mainFrame.selectOperators(newOperators);
	}
	
	public Operator getSelectedOperator() {
		return getFirstSelectedOperator();
	}
	
	public Operator getRootOperator() {
		if (process != null) {
			return process.getRootOperator();
		}
		return null;
	}

	public Process getProcess() {
		return process;
	}

	@Override
	public void processChanged(Process process) {
		if (this.process != process) {
			if (this.process != null) {
				this.process.removeBreakpointListener(breakpointListener);
				this.process.getRootOperator().removeProcessListener(processListener);
			}
			this.process = process;
			enableActions();
			if (this.process != null) {
				this.process.addBreakpointListener(breakpointListener );
				this.process.getRootOperator().addProcessListener(processListener);
			}			
		}				
	}

	@Override
	public void processUpdated(Process process) {
		enableActions();
	}

	@Override
	public void setSelection(List<Operator> selection) {
		this.selection = selection;
		enableActions();
	}

	private void updateCheckboxStates() {
		Operator op = getSelectedOperator();
		if (op != null) {
			for (int pos = 0; pos < TOGGLE_BREAKPOINT.length; pos++) {			
				TOGGLE_BREAKPOINT[pos].setSelected(op.hasBreakpoint(pos));
			}
			TOGGLE_ACTIVATION_ITEM.setSelected(op.isEnabled());
		}
	}
}
