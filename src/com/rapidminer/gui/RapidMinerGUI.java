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

package com.rapidminer.gui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.URL;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.ToolTipManager;
import javax.swing.UIManager;

import com.rapid_i.deployment.update.client.UpdateManager;
import com.rapidminer.FileProcessLocation;
import com.rapidminer.NoOpUserError;
import com.rapidminer.Process;
import com.rapidminer.ProcessLocation;
import com.rapidminer.RapidMiner;
import com.rapidminer.RepositoryProcessLocation;
import com.rapidminer.gui.actions.OpenAction;
import com.rapidminer.gui.autosave.AutoSave;
import com.rapidminer.gui.docking.RapidDockableContainerFactory;
import com.rapidminer.gui.look.RapidLookAndFeel;
import com.rapidminer.gui.look.fc.BookmarkIO;
import com.rapidminer.gui.look.ui.RapidDockingUISettings;
import com.rapidminer.gui.safemode.SafeMode;
import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.gui.tools.dialogs.DecisionRememberingConfirmDialog;
import com.rapidminer.gui.viewer.MetaDataViewerTableModel;
import com.rapidminer.operator.io.DatabaseDataReader;
import com.rapidminer.parameter.ParameterTypeBoolean;
import com.rapidminer.parameter.ParameterTypeCategory;
import com.rapidminer.parameter.ParameterTypeInt;
import com.rapidminer.parameter.ParameterTypeString;
import com.rapidminer.repository.MalformedRepositoryLocationException;
import com.rapidminer.repository.RepositoryLocation;
import com.rapidminer.repository.RepositoryManager;
import com.rapidminer.tools.FileSystemService;
import com.rapidminer.tools.I18N;
import com.rapidminer.tools.LaunchListener;
import com.rapidminer.tools.LaunchListener.RemoteControlHandler;
import com.rapidminer.tools.LogService;
import com.rapidminer.tools.ParameterService;
import com.rapidminer.tools.Tools;
import com.rapidminer.tools.jdbc.connection.DatabaseConnectionService;
import com.rapidminer.tools.plugin.Plugin;
import com.rapidminer.tools.usagestats.UsageStatistics;
import com.rapidminer.tools.usagestats.UsageStatsTransmissionDialog;
import com.vlsolutions.swing.docking.DockableContainerFactory;
import com.vlsolutions.swing.docking.ui.DockingUISettings;

/**
 * The main class if RapidMiner is started in GUI mode. This class keeps a reference to the
 * {@link MainFrame} and some other GUI relevant information and methods.
 * 
 * @author Ingo Mierswa, Simon Fischer
 */
public class RapidMinerGUI extends RapidMiner {

	public static final String PROPERTY_GEOMETRY_X = "rapidminer.gui.geometry.x";

	public static final String PROPERTY_GEOMETRY_Y = "rapidminer.gui.geometry.y";

	public static final String PROPERTY_GEOMETRY_EXTENDED_STATE = "rapidminer.gui.geometry.extendedstate";

	public static final String PROPERTY_GEOMETRY_WIDTH = "rapidminer.gui.geometry.width";

	public static final String PROPERTY_GEOMETRY_HEIGHT = "rapidminer.gui.geometry.height";

	public static final String PROPERTY_GEOMETRY_DIVIDER_MAIN = "rapidminer.gui.geometry.divider.main";

	public static final String PROPERTY_GEOMETRY_DIVIDER_EDITOR = "rapidminer.gui.geometry.divider.editor";;

	public static final String PROPERTY_GEOMETRY_DIVIDER_LOGGING = "rapidminer.gui.geometry.divider.logging";

	public static final String PROPERTY_GEOMETRY_DIVIDER_GROUPSELECTION = "rapidminer.gui.geometry.divider.groupselection";

	public static final String PROPERTY_EXPERT_MODE = "rapidminer.gui.expertmode";

	public static final String PROPERTY_DISCONNECT_ON_DISABLE = "rapidminer.gui.disconnect_on_disable";

	// --- Properties ---

	public static final String PROPERTY_RAPIDMINER_GUI_PURCHASED_NOT_INSTALLED_CHECK = "rapidminer.update.purchased.not_installed.check";

	public static final String PROPERTY_RAPIDMINER_GUI_UPDATE_CHECK = "rapidminer.update.check";

	public static final String PROPERTY_RAPIDMINER_GUI_MAX_STATISTICS_ROWS = "rapidminer.gui.max_statistics_rows";

	public static final String PROPERTY_RAPIDMINER_GUI_MAX_SORTABLE_ROWS = "rapidminer.gui.max_sortable_rows";

	public static final String PROPERTY_RAPIDMINER_GUI_MAX_DISPLAYED_VALUES = "rapidminer.gui.max_displayed_values";

	public static final String PROPERTY_RAPIDMINER_GUI_SNAP_TO_GRID = "rapidminer.gui.snap_to_grid";

	public static final String PROPERTY_AUTOWIRE_INPUT = "rapidminer.gui.autowire_input";

	public static final String PROPERTY_AUTOWIRE_OUTPUT = "rapidminer.gui.autowire_output";

	public static final String PROPERTY_RESOLVE_RELATIVE_REPOSITORY_LOCATIONS = "rapidminer.gui.resolve_relative_repository_locations";

	public static final String PROPERTY_CLOSE_RESULTS_BEFORE_RUN = "rapidminer.gui.close_results_before_run";

	public static final String PROPERTY_TRANSFER_USAGESTATS = "rapidminer.gui.transfer_usagestats";

	public static final String[] PROPERTY_TRANSFER_USAGESTATS_ANSWERS = { "ask", "always", "never" };

	public static final String PROPERTY_ADD_BREAKPOINT_RESULTS_TO_HISTORY = "rapidminer.gui.add_breakpoint_results_to_history";

	public static final String PROPERTY_CONFIRM_EXIT = "rapidminer.gui.confirm_exit";

	public static final String PROPERTY_RUN_REMOTE_NOW = "rapidminer.gui.run_process_on_rapidanalytics_now";

	public static final String PROPERTY_OPEN_IN_FILEBROWSER = "rapidminer.gui.entry_open_in_filebrowser";

	public static final String PROPERTY_CLOSE_ALL_RESULTS_NOW = "rapidminer.gui.close_all_results_without_confirmation";

	public static final String PROPERTY_FETCH_DATA_BASE_TABLES_NAMES = "rapidminer.gui.fetch_data_base_table_names";
	public static final String PROPERTY_EVALUATE_MD_FOR_SQL_QUERIES = DatabaseDataReader.PROPERTY_EVALUATE_MD_FOR_SQL_QUERIES;

	public static final String PROPERTY_DRAG_TARGET_HIGHLIGHTING = "rapidminer.gui.drag_target_highlighting";
	public static final String PROPERTY_DRAG_TARGET_HIGHLIGHT_COLOR = "rapidminer.gui.drag_target_highlight_color";
	public static final String[] PROPERTY_DRAG_TARGET_HIGHLIGHTING_VALUES = { "full", "border", "none" };

	public static final int DRAG_TARGET_HIGHLIGHTING_FULL = 0;

	static {
		ParameterService.registerParameter(new ParameterTypeBoolean(PROPERTY_RAPIDMINER_GUI_PURCHASED_NOT_INSTALLED_CHECK, "Check for recently purchased but not installed RapidMiner extensions at start up time?", true));
		ParameterService.registerParameter(new ParameterTypeBoolean(PROPERTY_RAPIDMINER_GUI_UPDATE_CHECK, "Check for new RapidMiner versions at start up time?", true));
		ParameterService.registerParameter(new ParameterTypeInt(PROPERTY_RAPIDMINER_GUI_MAX_STATISTICS_ROWS, "Indicates the maximum number of rows for the automatic calculation of statistics and other time intensive data viewing actions.", 1, Integer.MAX_VALUE, 100000));
		ParameterService.registerParameter(new ParameterTypeInt(PROPERTY_RAPIDMINER_GUI_MAX_SORTABLE_ROWS, "Indicates the maximum number of rows for sortable tables.", 1, Integer.MAX_VALUE, 100000));
		ParameterService.registerParameter(new ParameterTypeInt(PROPERTY_RAPIDMINER_GUI_MAX_DISPLAYED_VALUES, "Indicates the maximum number of different values which will for example be displayed in the meta data view.", 1, Integer.MAX_VALUE, MetaDataViewerTableModel.DEFAULT_MAX_DISPLAYED_VALUES));
		ParameterService.registerParameter(new ParameterTypeBoolean(PROPERTY_RESOLVE_RELATIVE_REPOSITORY_LOCATIONS, "If checked, the repository browser dialog will resolve repository locations relative to the current process by default. Can be disabled within the dialog.", true));
		ParameterService.registerParameter(new ParameterTypeBoolean(PROPERTY_RAPIDMINER_GUI_SNAP_TO_GRID, "If checked, operators snap to the grid.", true));
		ParameterService.registerParameter(new ParameterTypeBoolean(PROPERTY_AUTOWIRE_INPUT, "If checked, operator's inputs are wired automatically when added. Can be checked also in the \"Operators\" tree.", false));
		ParameterService.registerParameter(new ParameterTypeBoolean(PROPERTY_AUTOWIRE_OUTPUT, "If checked, operator's outputs are wired automatically when added. Can be checked also in the \"Operators\" tree.", false));
		ParameterService.registerParameter(new ParameterTypeCategory(PROPERTY_CLOSE_RESULTS_BEFORE_RUN, "Close active result tabs when new process starts?", DecisionRememberingConfirmDialog.PROPERTY_VALUES, DecisionRememberingConfirmDialog.TRUE));
		ParameterService.registerParameter(new ParameterTypeCategory(PROPERTY_RUN_REMOTE_NOW, "Execute the process on RapidAnalytics now?", DecisionRememberingConfirmDialog.PROPERTY_VALUES, DecisionRememberingConfirmDialog.ASK));
		ParameterService.registerParameter(new ParameterTypeCategory(PROPERTY_CLOSE_ALL_RESULTS_NOW, "Close all results on button pressed?", DecisionRememberingConfirmDialog.PROPERTY_VALUES, DecisionRememberingConfirmDialog.ASK));

		ParameterService.registerParameter(new ParameterTypeBoolean(PROPERTY_FETCH_DATA_BASE_TABLES_NAMES, "Fetch the data base tables names in the SQL query dialog of all SQL operators.", true));
		ParameterService.registerParameter(new ParameterTypeBoolean(PROPERTY_EVALUATE_MD_FOR_SQL_QUERIES, "If selected, the SQL meta data will be fetched during meta data evaluation in RapidMiner. For some databases, this may be slow.", true));

		ParameterService.registerParameter(new ParameterTypeCategory(RapidMinerGUI.PROPERTY_TRANSFER_USAGESTATS, "Allow RapidMiner to transfer RapidMiner operator usage statistics?", RapidMinerGUI.PROPERTY_TRANSFER_USAGESTATS_ANSWERS, UsageStatsTransmissionDialog.ASK));
		ParameterService.registerParameter(new ParameterTypeBoolean(RapidMinerGUI.PROPERTY_ADD_BREAKPOINT_RESULTS_TO_HISTORY, "Should results produced at breakpoints be added to the result history?", false));

		ParameterService.registerParameter(new ParameterTypeBoolean(RapidMinerGUI.PROPERTY_DISCONNECT_ON_DISABLE, "Should operators be disconnected upon disabling them?", true));

		ParameterService.registerParameter(new ParameterTypeCategory(RapidMinerGUI.PROPERTY_DRAG_TARGET_HIGHLIGHTING, "Defines the way possible drag targets should be highlighted.", PROPERTY_DRAG_TARGET_HIGHLIGHTING_VALUES, DRAG_TARGET_HIGHLIGHTING_FULL));

		ParameterService.registerParameter(new ParameterTypeBoolean(PROPERTY_CONFIRM_EXIT, "Should RapidMiner ask if you are sure each time you exit?", false));

		// UPDATE
		ParameterService.registerParameter(new ParameterTypeBoolean(com.rapid_i.deployment.update.client.UpdateManager.PARAMETER_UPDATE_INCREMENTALLY, "Download (small) patches rather than complete installation archives?", true));
		ParameterService.registerParameter(new ParameterTypeString(com.rapid_i.deployment.update.client.UpdateManager.PARAMETER_UPDATE_URL, "URL of the RapidMiner update server.", com.rapid_i.deployment.update.client.UpdateManager.UPDATESERVICE_URL));
		ParameterService.registerParameter(new ParameterTypeBoolean(com.rapid_i.deployment.update.client.UpdateManager.PARAMETER_INSTALL_TO_HOME, "If checked, all upgrades will be installed to the users home directory. Otherwise, administrator privileges are required.", true));
	}

	private static final int NUMBER_OF_RECENT_FILES = 8;

	private static MainFrame mainFrame;

	private static LinkedList<ProcessLocation> recentFiles = new LinkedList<ProcessLocation>();

	private static SafeMode safeMode;

	/**
	 * This thread listens for System shutdown and cleans up after shutdown.
	 * This included saving the recent file list and other GUI properties.
	 */
	private static class ShutdownHook extends Thread {

		@Override
		public void run() {
			//LogService.getRoot().info("Running shutdown sequence.");
			LogService.getRoot().log(Level.INFO, "com.rapidminer.gui.RapidMinerGUI.running_shutdown_sequence");
			RapidMinerGUI.saveRecentFileList();
			RapidMinerGUI.saveGUIProperties();
			UsageStatistics.getInstance().save();
			RepositoryManager.shutdown();
		}
	}

	//private static UpdateManager updateManager = new CommunityUpdateManager();

	public void run(final String openLocation) throws Exception {

		// check if resources were copied
		URL logoURL = Tools.getResource("rapidminer_logo.png");
		if (logoURL == null) {
			//LogService.getRoot().severe("Cannot find resources. Probably the ant target 'copy-resources' must be performed!");
			LogService.getRoot().log(Level.SEVERE, "com.rapidminer.gui.RapidMinerGUI.finding_resources_error");
			RapidMiner.quit(RapidMiner.ExitMode.ERROR);
		}

		// Initialize Docking UI -- important must be done as early as possible!
		DockingUISettings.setInstance(new RapidDockingUISettings());
		DockableContainerFactory.setFactory(new RapidDockableContainerFactory());

		RapidMiner.showSplash();

		RapidMiner.splashMessage("basic");
		RapidMiner.init();

		RapidMiner.splashMessage("workspace");
		RapidMiner.splashMessage("plaf");
		setupToolTipManager();
		setupGUI();

		RapidMiner.splashMessage("history");
		loadRecentFileList();

		RapidMiner.splashMessage("icons");
		SwingTools.loadIcons();

		RepositoryManager.getInstance(null).createRepositoryIfNoneIsDefined();

		RapidMiner.splashMessage("create_frame");

		SwingUtilities.invokeAndWait(new Runnable() {

			@Override
			public void run() {
				setMainFrame(new MainFrame(openLocation != null ? "design" : "welcome"));
			}
		});

		RapidMiner.splashMessage("gui_properties");
		loadGUIProperties(mainFrame);

		RapidMiner.splashMessage("plugin_gui");
		Plugin.initPluginGuis(mainFrame);

		RapidMiner.splashMessage("show_frame");

		mainFrame.setVisible(true);

		UsageStatsTransmissionDialog.init();

		RapidMiner.splashMessage("checks");
		Plugin.initFinalChecks();

		RapidMiner.splashMessage("ready");

		RapidMiner.hideSplash();

		// file from command line or Welcome Dialog
		//		if (file != null) {
		//			ImportProcessAction.open(file);
		//		}
		if (openLocation != null) {
			if(!RepositoryLocation.isAbsolute(openLocation)) {
				SwingTools.showVerySimpleErrorMessage("malformed_repository_location", openLocation);
			} else {
				OpenAction.open(openLocation, false);
			}
		}

		// check for updates
		Plugin.initPluginUpdateManager();
		UpdateManager.checkForUpdates();
		UpdateManager.checkForPurchasedNotInstalled();

		AutoSave autosave = new AutoSave();
		autosave.onLaunch();

		//TODO: re-enable when tour is finished
//		new WelcomeTourAction().checkTours();
	}

	private void setupToolTipManager() {
		// setup tool tip text manager
		ToolTipManager manager = ToolTipManager.sharedInstance();
		manager.setDismissDelay(25000); // original: 4000
		manager.setInitialDelay(1500);   // original: 750
		manager.setReshowDelay(50);    // original: 500
	}

	/** This default implementation only setup the tool tip durations. Subclasses might
	 *  override this method. */
	protected void setupGUI() throws NoOpUserError {
		System.setProperty(BookmarkIO.PROPERTY_BOOKMARKS_DIR, FileSystemService.getUserRapidMinerDir().getAbsolutePath());
		System.setProperty(BookmarkIO.PROPERTY_BOOKMARKS_FILE, ".bookmarks");
		System.setProperty(DatabaseConnectionService.PROPERTY_CONNECTIONS_FILE, "connections");
		try {
			UIManager.setLookAndFeel(new RapidLookAndFeel());
			//OperatorService.reloadIcons();
		} catch (Exception e) {
			//LogService.getRoot().log(Level.WARNING, "Cannot setup modern look and feel, using default.", e);
			LogService.getRoot().log(Level.WARNING,
					I18N.getMessage(LogService.getRoot().getResourceBundle(),
							"com.rapidminer.gui.RapidMinerGUI.setting_up_modern_look_and_feel_error"),
					e);
		}
	}

	public static void setMainFrame(MainFrame mf) {
		mainFrame = mf;
	}

	public static MainFrame getMainFrame() {
		return mainFrame;
	}

	public static void useProcessFile(Process process) {
		ProcessLocation location = process.getProcessLocation();
		addToRecentFiles(location);
	}

	public static void addToRecentFiles(ProcessLocation location) {
		if (location != null) {
			while (recentFiles.contains(location)) {
				recentFiles.remove(location);
			}
			recentFiles.addFirst(location);
			while (recentFiles.size() > NUMBER_OF_RECENT_FILES) {
				recentFiles.removeLast();
			}
			saveRecentFileList();
		}
	}

	public static List<ProcessLocation> getRecentFiles() {
		return recentFiles;
	}

	private static void loadRecentFileList() {
		File file = FileSystemService.getUserConfigFile("history");
		if (!file.exists())
			return;
		BufferedReader in = null;
		try {
			in = new BufferedReader(new FileReader(file));
			recentFiles.clear();
			String line = null;
			while ((line = in.readLine()) != null) {
				if (line.startsWith("file ")) {
					recentFiles.add(new FileProcessLocation(new File(line.substring(5))));
				} else if (line.startsWith("repository ")) {
					try {
						recentFiles.add(new RepositoryProcessLocation(new RepositoryLocation(line.substring(11))));
					} catch (MalformedRepositoryLocationException e) {
						//LogService.getRoot().log(Level.WARNING, "Unparseable line in history file: "+line);
						LogService.getRoot().log(Level.WARNING, "com.rapidminer.gui.RapidMinerGUI.unparseable_line", line);
					}
				} else {
					//LogService.getRoot().log(Level.WARNING, "Unparseable line in history file: "+line);
					LogService.getRoot().log(Level.WARNING, "com.rapidminer.gui.RapidMinerGUI.unparseable_line", line);
				}
			}
		} catch (IOException e) {
			//LogService.getRoot().log(Level.WARNING, "Cannot read history file", e);
			LogService.getRoot().log(Level.WARNING,
					I18N.getMessage(LogService.getRoot().getResourceBundle(),
							"com.rapidminer.gui.RapidMinerGUI.reading_history_file_error"),
					e);
		} finally {
			if (in != null) {
				try {
					in.close();
				} catch (IOException e) {
					//LogService.getRoot().log(Level.WARNING, "Cannot read history file", e);
					LogService.getRoot().log(Level.WARNING,
							I18N.getMessage(LogService.getRoot().getResourceBundle(),
									"com.rapidminer.gui.RapidMinerGUI.reading_history_file_error"),
							e);
				}
			}
		}
	}

	private static void saveRecentFileList() {
		File file = FileSystemService.getUserConfigFile("history");
		PrintWriter out = null;
		try {
			out = new PrintWriter(new FileWriter(file));
			for (ProcessLocation loc : recentFiles) {
				out.println(loc.toHistoryFileString());
			}
		} catch (IOException e) {
			SwingTools.showSimpleErrorMessage("cannot_write_history_file", e);
		} finally {
			if (out != null) {
				out.close();
			}
		}
	}

	private static void saveGUIProperties() {
		Properties properties = new Properties();
		MainFrame mainFrame = getMainFrame();
		if (mainFrame != null) {
			properties.setProperty(PROPERTY_GEOMETRY_X, "" + (int) mainFrame.getLocation().getX());
			properties.setProperty(PROPERTY_GEOMETRY_Y, "" + (int) mainFrame.getLocation().getY());
			properties.setProperty(PROPERTY_GEOMETRY_WIDTH, "" + mainFrame.getWidth());
			properties.setProperty(PROPERTY_GEOMETRY_HEIGHT, "" + mainFrame.getHeight());
			properties.setProperty(PROPERTY_GEOMETRY_EXTENDED_STATE, "" + mainFrame.getExtendedState());
			//properties.setProperty(PROPERTY_GEOMETRY_DIVIDER_MAIN, "" + mainFrame.getMainDividerLocation());
			//properties.setProperty(PROPERTY_GEOMETRY_DIVIDER_EDITOR, "" + mainFrame.getEditorDividerLocation());
			//properties.setProperty(PROPERTY_GEOMETRY_DIVIDER_LOGGING, "" + mainFrame.getLoggingDividerLocation());
			//properties.setProperty(PROPERTY_GEOMETRY_DIVIDER_GROUPSELECTION, "" + mainFrame.getGroupSelectionDividerLocation());
			properties.setProperty(PROPERTY_EXPERT_MODE, "" + mainFrame.getPropertyPanel().isExpertMode());
			File file = FileSystemService.getUserConfigFile("gui.properties");
			OutputStream out = null;
			try {
				out = new FileOutputStream(file);
				properties.store(out, "RapidMiner GUI properties");
			} catch (IOException e) {
				//LogService.getRoot().log(Level.WARNING, "Cannot write GUI properties: " + e.getMessage(), e);
				LogService.getRoot().log(Level.WARNING,
						I18N.getMessage(LogService.getRoot().getResourceBundle(),
								"com.rapidminer.gui.RapidMinerGUI.writing_gui_properties_error",
								e.getMessage()),
						e);
			} finally {
				try {
					if (out != null)
						out.close();
				} catch (IOException e) {}
			}
			mainFrame.getResultDisplay().clearAll();
			mainFrame.getPerspectives().saveAll();
		}
	}

	private static void loadGUIProperties(MainFrame mainFrame) {
		Properties properties = new Properties();
		File file = FileSystemService.getUserConfigFile("gui.properties");
		if (file.exists()) {
			InputStream in = null;
			try {
				in = new FileInputStream(file);
				properties.load(in);
			} catch (IOException e) {
				setDefaultGUIProperties();
			} finally {
				try {
					if (in != null)
						in.close();
				} catch (IOException e) {
					throw new Error(e); // should not occur
				}
			}
			try {
				mainFrame.setLocation(Integer.parseInt(properties.getProperty(PROPERTY_GEOMETRY_X)), Integer.parseInt(properties.getProperty(PROPERTY_GEOMETRY_Y)));
				mainFrame.setSize(new Dimension(Integer.parseInt(properties.getProperty(PROPERTY_GEOMETRY_WIDTH)), Integer.parseInt(properties.getProperty(PROPERTY_GEOMETRY_HEIGHT))));
				int extendedState;
				if (properties.getProperty(PROPERTY_GEOMETRY_EXTENDED_STATE) == null) {
					extendedState = JFrame.NORMAL;
				} else {
					extendedState = Integer.parseInt(properties.getProperty(PROPERTY_GEOMETRY_EXTENDED_STATE));
				}
				mainFrame.setExtendedState(extendedState);
				mainFrame.setExpertMode(Boolean.valueOf(properties.getProperty(PROPERTY_EXPERT_MODE)).booleanValue());
			} catch (NumberFormatException e) {
				setDefaultGUIProperties();
			}
		} else {
			setDefaultGUIProperties();
		}
		mainFrame.getPerspectives().loadAll();
	}

	/** This method sets some default GUI properties. This method can be invoked if the properties
	 *  file was not found or produced any error messages (which might happen after version changes).
	 */
	private static void setDefaultGUIProperties() {
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		mainFrame.setLocation((int) (0.05d * screenSize.getWidth()), (int) (0.05d * screenSize.getHeight()));
		mainFrame.setSize((int) (0.9d * screenSize.getWidth()), (int) (0.9d * screenSize.getHeight()));
		//mainFrame.setDividerLocations((int)(0.6d * screenSize.getHeight()), (int)(0.2d * screenSize.getWidth()), (int)(0.75d * screenSize.getWidth()), (int)(0.4d * screenSize.getWidth()));
		mainFrame.setExpertMode(false);
	}

	public static void main(String[] args) throws Exception {
		System.setSecurityManager(null);
		RapidMiner.addShutdownHook(new ShutdownHook());
		setExecutionMode(System.getProperty(PROPERTY_HOME_REPOSITORY_URL) == null ?
				ExecutionMode.UI : ExecutionMode.WEBSTART);

		boolean shouldLaunch = true;
		if (args.length > 0) {
			if (!LaunchListener.defaultLaunchWithArguments(args, new RemoteControlHandler() {

				@Override
				public boolean handleArguments(String[] args) {
					//LogService.getRoot().info("Received message from second launching client: "+Arrays.toString(args));
					LogService.getRoot().log(Level.INFO, "com.rapidminer.gui.RapidMinerGUI.received_message", Arrays.toString(args));
					mainFrame.requestFocus();
					if (args.length >= 1) {
						OpenAction.open(args[0], false);
					}
					return true;
				}
			})) {
				shouldLaunch = false;
			}
		}

		if (shouldLaunch) {
			safeMode = new SafeMode();
			safeMode.launchStarts();
			launch(args);
			safeMode.launchComplete();
		} else {
			//LogService.getRoot().config("Other RapidMiner instance already up. Exiting.");
			LogService.getRoot().log(Level.CONFIG, "com.rapidminer.gui.RapidMinerGUI.other_instance_up");
		}
	}

	private static void launch(String[] args) throws Exception {
		String openLocation = null;

		if (args.length > 0) {
			if (args.length != 1) {
				System.out.println("java " + RapidMinerGUI.class.getName() + " [processfile]");
				return;
			}
			openLocation = args[0];
		}
		RapidMiner.setInputHandler(new GUIInputHandler());
		new RapidMinerGUI().run(openLocation);
	}

	/**
	 * @return the safeMode
	 */
	public static SafeMode getSafeMode() {
		return safeMode;
	}

	public enum DragHighlightMode {
		FULL,
		BORDER,
		NONE
	}

	public static DragHighlightMode getDragHighlighteMode() {
		String dragParameter = ParameterService.getParameterValue(PROPERTY_DRAG_TARGET_HIGHLIGHTING);
		if (dragParameter.equals(PROPERTY_DRAG_TARGET_HIGHLIGHTING_VALUES[0])) {
			return DragHighlightMode.FULL;
		} else if (dragParameter.equals(PROPERTY_DRAG_TARGET_HIGHLIGHTING_VALUES[1])) {
			return DragHighlightMode.BORDER;
		} else {
			return DragHighlightMode.NONE;
		}
	}

	public static Color getBodyHighlightColor() {
		return new Color(255, 255, 242);
	}
	
	public static Color getBorderHighlightColor() {
		return SwingTools.RAPID_I_ORANGE;
	}
}
