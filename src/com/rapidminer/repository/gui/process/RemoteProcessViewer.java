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

package com.rapidminer.repository.gui.process;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.logging.Level;

import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JToolBar;
import javax.swing.JTree;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeExpansionListener;
import javax.swing.tree.TreePath;

import com.rapid_i.repository.wsimport.ProcessResponse;
import com.rapid_i.repository.wsimport.Response;
import com.rapidminer.RepositoryProcessLocation;
import com.rapidminer.gui.MainFrame;
import com.rapidminer.gui.actions.OpenAction;
import com.rapidminer.gui.actions.RunRemoteAction;
import com.rapidminer.gui.tools.ExtendedJScrollPane;
import com.rapidminer.gui.tools.ResourceAction;
import com.rapidminer.gui.tools.ResourceDockKey;
import com.rapidminer.gui.tools.ResourceLabel;
import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.gui.tools.ViewToolBar;
import com.rapidminer.gui.tools.components.ToolTipWindow;
import com.rapidminer.gui.tools.components.ToolTipWindow.TipProvider;
import com.rapidminer.repository.IOObjectEntry;
import com.rapidminer.repository.MalformedRepositoryLocationException;
import com.rapidminer.repository.RemoteProcessState;
import com.rapidminer.repository.Repository;
import com.rapidminer.repository.RepositoryConstants;
import com.rapidminer.repository.RepositoryException;
import com.rapidminer.repository.RepositoryLocation;
import com.rapidminer.repository.gui.ToolTipProviderHelper;
import com.rapidminer.repository.remote.RemoteRepository;
import com.rapidminer.tools.I18N;
import com.rapidminer.tools.LogService;
import com.vlsolutions.swing.docking.DockKey;
import com.vlsolutions.swing.docking.Dockable;

/**
 * Displays a tree of processes running on a remote server.
 * 
 * @author Simon Fischer, Tobias Malbrecht
 */
public class RemoteProcessViewer extends JPanel implements Dockable {

	private final class OpenProcessAction extends ResourceAction {
		private final Repository repository;
		private final ProcessResponse processResponse;
		private static final long serialVersionUID = 1L;

		private OpenProcessAction(Repository repository, ProcessResponse processResponse) {
			super("remoteprocessviewer.open");
			this.repository = repository;
			this.processResponse = processResponse;
		}

		public OpenProcessAction(Repository repository, OutputLocation location, TreePath tp) {
			super("remoteprocessviewer.open");
			this.repository = repository;
			this.processResponse = (ProcessResponse) tp.getPath()[2];
		}


		@Override
		public void actionPerformed(ActionEvent e) {
			TreePath selectionPath = tree.getSelectionPath();
			if (selectionPath != null) {
				Object selection = selectionPath.getLastPathComponent();
				if (selection instanceof ProcessResponse) {
					String locStr = RepositoryLocation.REPOSITORY_PREFIX + repository.getName() +
							((ProcessResponse) selection).getProcessLocation();
					RepositoryLocation loc;
					try {
						loc = new RepositoryLocation(locStr);
					} catch (MalformedRepositoryLocationException e1) {
						SwingTools.showSimpleErrorMessage("while_loading", e1, locStr, e1.getMessage());
						return;
					}
					OpenAction.open(new RepositoryProcessLocation(loc), true);
				} else if (selection instanceof OutputLocation) {
					try {
						RepositoryLocation procLoc = new RepositoryLocation(RepositoryLocation.REPOSITORY_PREFIX + repository.getName() +
								processResponse.getProcessLocation());
						RepositoryLocation ioLoc = new RepositoryLocation(procLoc.parent(), ((OutputLocation) selection).getLocation());

						IOObjectEntry locatedEntry = (IOObjectEntry) ioLoc.locateEntry();
						if (locatedEntry == null) { // may happen if entry has been deleted in the meantime
							SwingTools.showVerySimpleErrorMessage("cannot_find_repository_location", ioLoc.toString());
							return;
						}
						OpenAction.showAsResult(locatedEntry);
					} catch (Exception e1) {
						SwingTools.showSimpleErrorMessage("cannot_fetch_data_from_repository", e1);
					}
				}
			}
		}
	}

	private final class StopAction extends ResourceAction {

		private static final long serialVersionUID = 1L;
		private TreePath treePath;

		private StopAction(TreePath tp) {
			super("remoteprocessviewer.stop");
			this.treePath = tp;
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			if (treePath != null) {
				Object selection = treePath.getLastPathComponent();
				if (selection instanceof ProcessResponse) {
					ProcessResponse processResponse = (ProcessResponse) selection;
					if (!RemoteProcessState.valueOf(processResponse.getState()).isTerminated()) {
						if (treePath.getLastPathComponent() instanceof ProcessResponse) {
							RemoteRepository repository = (RemoteRepository) treePath.getPath()[1];
							try {

								Response stopResponse = repository.getProcessService().stopProcess(processResponse.getId());
								if (stopResponse.getStatus() != RepositoryConstants.OK) {
									SwingTools.showVerySimpleErrorMessage("remoteprocessviewer.stop_failed", stopResponse.getErrorMessage());
								}
							} catch (RepositoryException e1) {
								SwingTools.showSimpleErrorMessage("remoteprocessviewer.stop_failed", e1);
							}
						}
					}
				}
			}
		}
	}
	
	private final class ShowLogAction extends ResourceAction {

		private RemoteRepository repository;
		private TreePath treePath;
		private static final long serialVersionUID = 1L;

		private ShowLogAction(RemoteRepository repository, TreePath tp) {
			super("remoteprocessviewer.show_log");
			this.treePath = tp;
			this.repository = repository;
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			if (treePath != null) {
				Object selection = treePath.getLastPathComponent();
				if (selection instanceof ProcessResponse) {
					ProcessResponse processResponse = (ProcessResponse) selection;
					repository.showLog(processResponse.getId());
					}
			}
		}
	}

	private final class BrowseProcessAction extends ResourceAction {

		private RemoteRepository repository;
		private TreePath treePath;
		private static final long serialVersionUID = 1L;

		private BrowseProcessAction(RemoteRepository repositoy, TreePath tp) {
			super("remoteprocessviewer.browse");
			this.treePath = tp;
			this.repository = repositoy;

		}

		@Override
		public void actionPerformed(ActionEvent e) {
			if (treePath != null) {
				Object selection = treePath.getLastPathComponent();
				if (selection instanceof ProcessResponse) {
					repository.browse(((ProcessResponse) selection).getProcessLocation());
				} else if (selection instanceof OutputLocation) {
					try {
						ProcessResponse proResponse = (ProcessResponse) treePath.getPath()[2];
						RepositoryLocation procLoc = new RepositoryLocation(RepositoryLocation.REPOSITORY_PREFIX + repository.getName() +
								proResponse.getProcessLocation());
						RepositoryLocation ioLoc = new RepositoryLocation(procLoc.parent(), ((OutputLocation) selection).getLocation());
						repository.browse(ioLoc.getPath());

					} catch (Exception e1) {
						SwingTools.showSimpleErrorMessage("cannot_fetch_data_from_repository", e1);
					}
				}
			}
		}
	}

	private static final long serialVersionUID = 1L;

	private JTree tree;

	private RemoteProcessesTreeModel treeModel;

	private Date sessionStartDate = new Date();
	private JComboBox sinceWhenCombo = new JComboBox(new Object[] {
			I18N.getMessage(I18N.getGUIBundle(), "gui.combo.remoteprocessviewer.since_session_start"),
			I18N.getMessage(I18N.getGUIBundle(), "gui.combo.remoteprocessviewer.for_today"),
			I18N.getMessage(I18N.getGUIBundle(), "gui.combo.remoteprocessviewer.all")
	});

	public RemoteProcessViewer() {
		setLayout(new BorderLayout());
		treeModel = new RemoteProcessesTreeModel();
		treeModel.setSince(sessionStartDate);
		tree = new JTree(treeModel);
		tree.setCellRenderer(new RemoteProcessTreeCellRenderer());
		tree.setShowsRootHandles(true);
		tree.setRootVisible(false);
		JScrollPane scrollPane = new ExtendedJScrollPane(tree);
		scrollPane.setBorder(null);
		add(scrollPane, BorderLayout.CENTER);

		JToolBar toolBar = new ViewToolBar();

		add(toolBar, BorderLayout.NORTH);
		toolBar.add(new RunRemoteAction());
		toolBar.addSeparator();
		ResourceLabel label = new ResourceLabel("remoteprocessviewer.filter");
		label.setLabelFor(sinceWhenCombo);
		toolBar.add(label);
		toolBar.add(sinceWhenCombo);

		ToolTipWindow window = new ToolTipWindow(new TipProvider() {
			@Override
			public Component getCustomComponent(Object id) {
				if (id instanceof TreePath) {
					RepositoryLocation loc = getSelectedRepositoryLocation((TreePath) id);
					if (loc != null) {
						try {
							// TODO: Need to run locateEntry() in background. How?
							return ToolTipProviderHelper.getCustomComponent(loc.locateEntry());
						} catch (RepositoryException e) {
							//LogService.getRoot().log(Level.WARNING, "Error locating entry for "+loc+": "+e, e);
							LogService.getRoot().log(Level.WARNING,
									I18N.getMessage(LogService.getRoot().getResourceBundle(),
											"com.rapidminer.repository.gui.process.RemoteProcessViewer.locating_entry_error",
											loc, e),
									e);

							return null;
						}
					} else {
						return null;
					}
				} else {
					return null;
				}
			}

			@Override
			public Object getIdUnder(Point point) {
				TreePath path = tree.getPathForLocation((int) point.getX(), (int) point.getY());
				if (path != null) {
					return path;
				} else {
					return null;
				}
			}
			
			@Override
			public String getTip(Object o) {
				if (o instanceof TreePath) {
					Object last = ((TreePath) o).getLastPathComponent();
					if (last instanceof ProcessResponse) {
						ProcessResponse pr = (ProcessResponse) last;
						StringBuilder b = new StringBuilder();
						b.append("<html><body>");
						b.append("<strong>").append(pr.getProcessLocation()).append("</strong> ");
						if (RemoteProcessState.valueOf(pr.getState()) == RemoteProcessState.FAILED) {
							b.append("<span style=\"color:red\">(").append(pr.getState().toLowerCase()).append(")</span><br/>");
						} else {
							b.append("(").append(pr.getState().toLowerCase()).append(")<br/>");
						}
						if (pr.getStartTime() != null) {
							b.append("<em>Started: </em>").append(DateFormat.getDateTimeInstance().format(pr.getStartTime().toGregorianCalendar().getTime())).append("<br/>");
						}
						if (pr.getCompletionTime() != null) {
							b.append("<em>Completed: </em>").append(DateFormat.getDateTimeInstance().format(pr.getCompletionTime().toGregorianCalendar().getTime())).append("<br/>");
						}
						if (pr.getException() != null) {
							b.append("<span style=\"color:red\">").append(pr.getException()).append("</span><br/>");
						}
						RemoteRepository repos = (RemoteRepository) ((TreePath) o).getPath()[1];
						b.append("<a href=\"" + repos.getProcessLogURI(pr.getId()).toString() + "\">View Log</a>");
						b.append("</body></html>");
						return b.toString();
					} else if (last instanceof RemoteRepository) {
						return ((RemoteRepository) last).getDescription();
					} else if (last == RemoteProcessesTreeModel.EMPTY_PROCESS_LIST) {
						return I18N.getMessage(I18N.getGUIBundle(), "gui.label.remoteprocessviewer.empty");
					} else if (last == RemoteProcessesTreeModel.PENDING_PROCESS_LIST) {
						return I18N.getMessage(I18N.getGUIBundle(), "gui.label.remoteprocessviewer.pending");
					} else {
						RepositoryLocation loc = getSelectedRepositoryLocation((TreePath)o);
						if (loc != null) {
							try {
								return ToolTipProviderHelper.getTip(loc.locateEntry());
							} catch (RepositoryException e) {
								//LogService.getRoot().log(Level.WARNING, "Error locating entry for "+loc+": "+e, e);
								LogService.getRoot().log(Level.WARNING,
										I18N.getMessage(LogService.getRoot().getResourceBundle(),
												"com.rapidminer.repository.gui.process.RemoteProcessViewer.locating_entry_error",
												loc, e),
										e);
								return null;
							}
						} else {
							return null;
						}
					}
				} else {
					return null;
				}
			}
		}, tree);
		sinceWhenCombo.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				switch (sinceWhenCombo.getSelectedIndex()) {
					case 0:
						treeModel.setSince(sessionStartDate);
						break;
					case 1:
						Calendar today = new GregorianCalendar();
						today.set(Calendar.HOUR_OF_DAY, 0);
						today.set(Calendar.MINUTE, 0);
						today.set(Calendar.SECOND, 0);
						today.set(Calendar.MILLISECOND, 0);
						treeModel.setSince(today.getTime());
						break;
					case 2:
						treeModel.setSince(null);
				}

			}
		});

		tree.addTreeExpansionListener(new TreeExpansionListener() {

			@Override
			public void treeExpanded(TreeExpansionEvent event) {
				Object leaf = event.getPath().getLastPathComponent();
				if (leaf instanceof RemoteRepository) {
					treeModel.observe((RemoteRepository) leaf);
				}
			}

			@Override
			public void treeCollapsed(TreeExpansionEvent event) {
				Object leaf = event.getPath().getLastPathComponent();
				if (leaf instanceof RemoteRepository) {
					treeModel.ignore((RemoteRepository) leaf);
				}
			}
		});
		tree.setToggleClickCount(3);
		tree.addMouseListener(new MouseAdapter() {

			@Override
			public void mousePressed(MouseEvent e) {
				showPopupMenu(e);
			}

			@Override
			public void mouseReleased(MouseEvent e) {
				showPopupMenu(e);
			}

			@Override
			public void mouseClicked(MouseEvent e) {
				if (e.getClickCount() == 2) {
					doubleCLickEvent(e);
					e.consume();
				} else {
					showPopupMenu(e);
				}
			}

			private void showPopupMenu(MouseEvent e) {
				TreePath tp = tree.getPathForLocation(e.getX(), e.getY());

				if (tp != null) {
					final RemoteRepository repository = (RemoteRepository) tp.getPath()[1];
					Object last = tp.getLastPathComponent();

					if (e.isPopupTrigger()) {
						JPopupMenu menu = new JPopupMenu();

						if (last instanceof ProcessResponse) {
							final ProcessResponse processResponse = (ProcessResponse) last;

							// select the process on which the mouse is pointing:
							tree.setSelectionPath(tp);

							menu.add(new OpenProcessAction(repository, processResponse));

							menu.add(new BrowseProcessAction(repository, tp));
							if (((ProcessResponse) last).getState().equals("RUNNING")) {
								menu.add(new StopAction(tp));
							}
							menu.add(new ShowLogAction(repository, tp));
						}

						if (last instanceof OutputLocation) {
							menu.add(new OpenProcessAction(repository, (OutputLocation) last, tp));
						}

						menu.show(tree, e.getX(), e.getY());

					}
				}
			}

			private void doubleCLickEvent(MouseEvent e) {
				TreePath tp = tree.getPathForLocation(e.getX(), e.getY());
				if (tp != null) {
					final RemoteRepository repository = (RemoteRepository) tp.getPath()[1];
					Object last = tp.getLastPathComponent();

					if (last instanceof ProcessResponse) {
						// prevent the tree node from collapsing again after a double-click:
						//tree.expandPath(tp);

						final ProcessResponse processResponse = (ProcessResponse) last;
						OpenProcessAction openAction = new OpenProcessAction(repository, processResponse);
						openAction.actionPerformed(new ActionEvent(e.getSource(), e.getID(), ""));
					} else if (last instanceof OutputLocation) {
						OpenProcessAction openAction = new OpenProcessAction(repository, (OutputLocation) last, tp);
						openAction.actionPerformed(new ActionEvent(e.getSource(), e.getID(), ""));
					}


				}
			}
		

		});

	}

	private RepositoryLocation getSelectedRepositoryLocation(TreePath selectionPath) {
		try {
			if (selectionPath != null) {
				Object selection = selectionPath.getLastPathComponent();
				if (selection instanceof ProcessResponse) {
					Repository repository = (Repository) selectionPath.getPath()[1];
					return new RepositoryLocation(RepositoryLocation.REPOSITORY_PREFIX + repository.getName() +
							((ProcessResponse) selection).getProcessLocation());
				} else if (selection instanceof OutputLocation) {
					Repository repository = (Repository) selectionPath.getPath()[1];
					ProcessResponse proResponse = (ProcessResponse) selectionPath.getPath()[2];
					RepositoryLocation procLoc = new RepositoryLocation(RepositoryLocation.REPOSITORY_PREFIX + repository.getName() +
							proResponse.getProcessLocation());
					return new RepositoryLocation(procLoc.parent(), ((OutputLocation) selection).getLocation());
				}
			}
		} catch (MalformedRepositoryLocationException e) {
			return null;
		}
		return null;
	}

	public static final String PROCESS_PANEL_DOCK_KEY = "remote_process_viewer";
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
}

