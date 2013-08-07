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
package com.rapidminer.gui.processeditor.results;

import java.awt.BorderLayout;
import java.awt.Component;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;

import com.rapidminer.LoggingListener;
import com.rapidminer.Process;
import com.rapidminer.ProcessListener;
import com.rapidminer.datatable.DataTable;
import com.rapidminer.gui.MainFrame;
import com.rapidminer.gui.RapidMinerGUI;
import com.rapidminer.gui.renderer.RendererService;
import com.rapidminer.gui.tools.ExtendedJTabbedPane;
import com.rapidminer.gui.tools.ProgressThread;
import com.rapidminer.gui.tools.ResourceDockKey;
import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.gui.tools.UpdateQueue;
import com.rapidminer.gui.tools.dialogs.DecisionRememberingConfirmDialog;
import com.rapidminer.gui.viewer.DataTableViewer;
import com.rapidminer.operator.IOContainer;
import com.rapidminer.operator.MissingIOObjectException;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.ResultObject;
import com.vlsolutions.swing.docking.DockKey;


/**
 * The result display is the view of the RapidMiner GUI which refers to (intermediate)
 * results. It can display all IOObjects which are delivered, each in a tab which
 * is displayed at the top together with its icon. If the process produces some statistics,
 * e.g. performance against generation, these are plotted online.
 * 
 * @author Ingo Mierswa
 */
public class TabbedResultDisplay extends JPanel  implements ResultDisplay {

	private static final long serialVersionUID = 1970923271834221630L;

	private static final String OPERATOR_TREE_ICON_NAME = "table.png";
	private static final String DEFAULT_RESULT_ICON_NAME = "presentation_chart.png";

	private static Icon dataTableIcon = null;
	static Icon defaultResultIcon = null;

	static {
		dataTableIcon = SwingTools.createIcon("16/" + OPERATOR_TREE_ICON_NAME);
		defaultResultIcon = SwingTools.createIcon("16/" + DEFAULT_RESULT_ICON_NAME);
	}

	//private List<ResultObject> results = new LinkedList<ResultObject>();

	/** Maps names of currently displayed results to the number repetitions of the same name.
	 *  If more than one object with the same name is used, numbers are appended accordingly to the tabs names. 
	 */
	private final Map<String,Integer> currentResultNames = new TreeMap<String,Integer>();

	private final JTabbedPane tabs = new ExtendedJTabbedPane();

	private final JLabel label = new JLabel("Results");;

	private final Collection<DataTable> dataTables = new LinkedList<DataTable>();	

	private final UpdateQueue tableUpdateQueue = new UpdateQueue("ResultDisplayDataTableViewUpdater");

	public TabbedResultDisplay() {
		super(new BorderLayout());
		add(tabs, BorderLayout.CENTER);
		add(label, BorderLayout.NORTH);
		showData(null, "Results");
		tableUpdateQueue.start();
	}

	public void clear() {
		this.tabs.removeAll();
		//this.results.clear();
		this.dataTables.clear();
		synchronized (currentResultNames) {
			currentResultNames.clear();
		}
		label.setText("No results produced.");
		repaint();
	}

	/** Update the DataTableViewers. This does not happen on the EDT 
	 *  and is executed asynchronously by an {@link UpdateQueue}. */
	private void updateDataTables(final Collection<DataTable> newTableList) {
		final Collection<DataTable> copy = new LinkedList<DataTable>(newTableList);
		// this is time consuming, so execute off EDT
		tableUpdateQueue.execute(new Runnable() {
			public void run() {
				final Collection<DataTableViewer> viewers = new LinkedList<DataTableViewer>();				
				for (DataTable table : copy) {					
					viewers.add(new DataTableViewer(table, true, DataTableViewer.PLOT_MODE));
				}				
				installDataTableViewers(viewers);								
			}
			@Override
			public String toString() {
				return "Update data table list to size "+copy.size();
			}
		});
	}

	/** Adds the collection of components in the EDT (after removing the old tables. */
	private void installDataTableViewers(final Collection<DataTableViewer> viewers) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				for (int i = tabs.getTabCount() - 1; i >= 0; i--) {
					Component c = tabs.getComponentAt(i);
					if (c instanceof DataTableViewer) {		
						tabs.removeTabAt(i);
					}
				}
				int pos = 0;
				for (DataTableViewer viewer : viewers) {
					tabs.insertTab(viewer.getDataTable().getName(), dataTableIcon, viewer, "The data table '"+viewer.getDataTable().getName()+"'.", pos);
					pos++;
				}
			}
		});
	}

	public void showResult(final ResultObject result) {
		new ProgressThread("creating_display") {
			@Override
			public void run() {
				getProgressListener().setTotal(1);
				getProgressListener().setCompleted(0);
				addResultTab(createComponent(result, null), -2);
				getProgressListener().setCompleted(1);
			}			
		}.start();		
	}

	private void clearResults() {		
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				for (int i = tabs.getTabCount() - 1; i >= 0; i--) {
					Component c = tabs.getComponentAt(i);
					if (!(c instanceof DataTableViewer)) {
						tabs.removeTabAt(i);
					}
				}				
			}
		});
	}

	private JPanel createComponent(ResultObject resultObject, IOContainer resultContainer) {
		final String resultName = RendererService.getName(resultObject.getClass());
		String usedResultName = resultObject.getName();
		if (usedResultName == null) {
			usedResultName = resultName;
		}
		synchronized (currentResultNames) {
			Integer oldCount = currentResultNames.get(usedResultName);
			int myIndex;
			if (oldCount == null) {
				myIndex = 1;			
			} else {
				myIndex = oldCount+1;						
			}
			currentResultNames.put(usedResultName, myIndex);
			if (myIndex > 1) {
				usedResultName += "(" + myIndex + ")";
			}
		}
		return ResultDisplayTools.createVisualizationComponent(resultObject, resultContainer, usedResultName);
	}

	public void showData(final IOContainer resultContainer, final String message) {
		final int selectedIndex = tabs.getSelectedIndex();
		clearResults();		

		final List<ResultObject> newResults = convertToList(resultContainer); 
		//this.results.addAll(newResults);

		if (newResults.size() == 0) {
			label.setText("No results produced.");
		} else {
			label.setText(message);
			new ProgressThread("creating_display") {
				@Override
				public void run() {
					getProgressListener().setTotal(newResults.size());
					getProgressListener().setCompleted(0);
					int i = 0;
					for (final ResultObject result : newResults) {				
						addResultTab(createComponent(result, resultContainer), selectedIndex);
						i++;
						getProgressListener().setCompleted(i);						
					}
				}

			}.start();			
		}

		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				if (tabs.getSelectedIndex() == -1)
					if (tabs.getTabCount() > 0)
						tabs.setSelectedIndex(0);
			}
		});
	}

	/**
	 * @param selectMeIfIAmInsertedAsIndex If the tab created for this result equals this parameter, 
	 *        the tab will be immediately selected. If this parameter is -2, the tab will always be selected.
	 */
	private void addResultTab(final JPanel resultPanel, final int selectMeIfIAmInsertedAsIndex) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {				
				askForPerspectiveSwitch();
				tabs.addTab((String)resultPanel.getClientProperty(ResultDisplayTools.CLIENT_PROPERTY_RAPIDMINER_RESULT_NAME_HTML), 
						(Icon)resultPanel.getClientProperty(ResultDisplayTools.CLIENT_PROPERTY_RAPIDMINER_RESULT_ICON),
						resultPanel, 
						"Show the result '" + (String)resultPanel.getClientProperty(ResultDisplayTools.CLIENT_PROPERTY_RAPIDMINER_RESULT_NAME) + "'.");
				if ((selectMeIfIAmInsertedAsIndex == -2) || (tabs.getTabCount() - 1 == selectMeIfIAmInsertedAsIndex)) {
					tabs.setSelectedIndex(tabs.getTabCount()-1);
				}
			}			
		});
	}

	public Component getCurrentlyDisplayedComponent() {
		if (tabs.getTabCount() == 0) {
			return tabs;
		} else {
			return tabs.getSelectedComponent();
		}
	}

	private boolean isAskingForPerspectiveSwitch = false;
	private void askForPerspectiveSwitch() {
		if (isAskingForPerspectiveSwitch || RapidMinerGUI.getMainFrame().getPerspectives().getCurrentPerspective().getName().equals(RESULT_DOCK_KEY)) {
			return;
		} else {
			try {
				isAskingForPerspectiveSwitch = true;
				if (DecisionRememberingConfirmDialog.confirmAction("show_results_on_creation", 
						MainFrame.PROPERTY_RAPIDMINER_GUI_AUTO_SWITCH_TO_RESULTVIEW)) {
					RapidMinerGUI.getMainFrame().getPerspectives().showPerspective(RESULT_DOCK_KEY);
				}
			} finally {
				isAskingForPerspectiveSwitch = false;
			}
		}
	}

	private static List<ResultObject> convertToList(IOContainer container) {
		List<ResultObject> list = new LinkedList<ResultObject>();
		if (container != null) {
			ResultObject result = null;
			do {
				try {
					result = container.get(ResultObject.class, list.size());
					list.add(result);
				} catch (MissingIOObjectException e) {
					break;
				}
			} while (result != null);
		}
		return list;
	}

	private final LoggingListener logListener = new LoggingListener() {
		public void addDataTable(final DataTable dataTable) {
			TabbedResultDisplay.this.addDataTable(dataTable);
		}
		public void removeDataTable(final DataTable dataTable) {
			TabbedResultDisplay.this.dataTables.remove(dataTable);
			updateDataTables(dataTables);
		}
	};
	
	// Listeners
	private final ProcessListener processListener = new ProcessListener() {
		@Override public void processEnded(Process process) { }
		@Override public void processFinishedOperator(Process process, Operator op) { }
		@Override public void processStartedOperator(Process process, Operator op) { }
		@Override
		public void processStarts(Process process) {
			clear();			
		}		
	};

	
	private final DockKey DOCK_KEY = new ResourceDockKey(RESULT_DOCK_KEY);

	@Override
	public Component getComponent() {
		return this;
	}

	public void addDataTable(DataTable dataTable) {
		TabbedResultDisplay.this.dataTables.add(dataTable);
		updateDataTables(dataTables);
	}

	@Override
	public DockKey getDockKey() {
		return DOCK_KEY;
	}
	
	// ProcessEditor

	private Process process;

	@Override
	public void processChanged(Process process) {
		if (this.process != null) {
			this.process.removeLoggingListener(logListener);
			this.process.getRootOperator().removeProcessListener(processListener);
		}
		this.process = process;
		if (this.process != null) {
			this.process.addLoggingListener(logListener);
			this.process.getRootOperator().addProcessListener(processListener);
		}		
	}

	@Override
	public void processUpdated(Process process) { }

	@Override
	public void setSelection(List<Operator> selection) { }

	@Override
	public void init(MainFrame mainFrame) {
	}

	@Override
	public void clearAll() {
		tabs.removeAll();
	}
}
