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

package com.rapidminer.gui.autosave;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;

import javax.swing.SwingUtilities;

import com.rapidminer.FileProcessLocation;
import com.rapidminer.Process;
import com.rapidminer.ProcessLocation;
import com.rapidminer.RapidMiner;
import com.rapidminer.RepositoryProcessLocation;
import com.rapidminer.gui.RapidMinerGUI;
import com.rapidminer.gui.processeditor.ProcessEditor;
import com.rapidminer.gui.tools.UpdateQueue;
import com.rapidminer.operator.Operator;
import com.rapidminer.repository.MalformedRepositoryLocationException;
import com.rapidminer.repository.RepositoryLocation;
import com.rapidminer.tools.FileSystemService;
import com.rapidminer.tools.LogService;
import com.rapidminer.tools.XMLException;

/**
 * @author Venkatesh Umaashankar
 *
 */
public class AutoSave {

	private File autoSavedProcessProperties;
	private File autoSavedProcess;
	private UpdateQueue autoSaveQueue;
	private boolean autoSaveEnabled;
	private boolean recoveryRequired;
	private String locationInfo;

	public AutoSave() {
		String rapidMinerDir = FileSystemService.getUserRapidMinerDir().getAbsolutePath();
		File autosaveDir = new File(rapidMinerDir + "/autosave");
		autosaveDir.mkdir();
		boolean autoSaveDirExists = autosaveDir.exists();
		if (autoSaveDirExists) {
			autoSavedProcessProperties = new File(autosaveDir.getAbsolutePath() + "/autosaved_process.properties");
			autoSavedProcess = new File(autosaveDir.getAbsolutePath() + "/autosaved_process.xml");
			autoSaveQueue = new UpdateQueue("autosave-queue");
			autoSaveQueue.start();
			this.autoSaveEnabled = true;
		} else {
			LogService.getRoot().log(Level.INFO, "com.rapidminer.gui.autosave.AutoSave.dir_creation_failed");
			this.autoSaveEnabled = false;
		}
	}

	public void onLaunch() {
		String processType = null;
		String processPath = null;
		boolean autoSavedProcessExists = autoSavedProcessProperties.exists();
		Properties autoSave = new Properties();
		FileInputStream autoSaveConfig = null;
		try {
			if (autoSavedProcessExists) {
				autoSaveConfig = new FileInputStream(autoSavedProcessProperties.getAbsolutePath());
				autoSave.load(autoSaveConfig);
				processType = autoSave.getProperty("autosave.process.type");
				processPath = autoSave.getProperty("autosave.process.path");

				if (this.autoSaveEnabled) {
					recoveryRequired = askForRecovery(processPath.equals("none") ? "" : processPath);
					if (recoveryRequired) {

						ProcessLocation autoSaveProcessLocation = new FileProcessLocation(autoSavedProcess);
						ProcessLocation actualProcessLocation = null;
						if (processType.equals("repository_object")) {
							actualProcessLocation = new RepositoryProcessLocation(new RepositoryLocation(processPath));
						} else if (processType.equals("file")) {
							actualProcessLocation = new FileProcessLocation(new File(processPath));
						}
						Process process = autoSaveProcessLocation.load(null);

						process = new Process(process.getRootOperator().getXML(false));

						if (actualProcessLocation != null) {
							process.setProcessLocation(actualProcessLocation);
							RapidMinerGUI.getMainFrame().setOpenedProcess(process, false, actualProcessLocation.toString());
						} else {
							RapidMinerGUI.getMainFrame().setProcess(process, true);
						}
						SwingUtilities.invokeLater(new Runnable() {

							@Override
							public void run() {
								RapidMinerGUI.getMainFrame().SAVE_ACTION.setEnabled(true);
							}
						});

					}

				}
			}
		} catch (IOException e) {
			LogService.getRoot().log(Level.WARNING, "com.rapidminer.gui.autosave.AutoSave.load_process_failed", e);
			this.autoSaveEnabled = false;
		} catch (MalformedRepositoryLocationException e) {
			LogService.getRoot().log(Level.WARNING, "com.rapidminer.gui.autosave.AutoSave.load_process_failed", e);
			this.autoSaveEnabled = false;
		} catch (XMLException e) {
			LogService.getRoot().log(Level.WARNING, "com.rapidminer.gui.autosave.AutoSave.load_process_failed", e);
			this.autoSaveEnabled = false;
		} finally {
			if (autoSaveConfig != null) {
				try {
					autoSaveConfig.close();
				} catch (IOException e) {}
			}
		}

		RapidMinerGUI.getMainFrame().addProcessEditor(new ProcessEditor() {

			@Override
			public void setSelection(List<Operator> selection) {
				// do nothing
			}

			@Override
			public void processUpdated(Process process) {
				saveProcess(process);
			}

			@Override
			public void processChanged(Process process) {
				saveProcess(process);
			}
		});

		RapidMiner.addShutdownHook(new Runnable() {

			@Override
			public void run() {
				AutoSave.this.onShutdown();

			}
		});

	}

	private void saveProcess(final Process process) {
		if (autoSaveEnabled) {
			this.autoSaveQueue.execute(new Runnable() {

				@Override
				public void run() {

					ProcessLocation processLocation = process.getProcessLocation();
					if (processLocation != null) {
						if (processLocation instanceof FileProcessLocation) {
							locationInfo = "autosave.process.type=file" + "\n" + "autosave.process.path=" + ((FileProcessLocation) processLocation).getFile().getAbsolutePath();
						} else if (processLocation instanceof RepositoryProcessLocation) {
							locationInfo = "autosave.process.type=repository_object" + "\n" + "autosave.process.path=" + ((RepositoryProcessLocation) processLocation).getRepositoryLocation().getAbsoluteLocation();
						}
					} else {
						//process is not saved yet
						locationInfo = "autosave.process.type=none" + "\n" + "autosave.process.path=none";
					}
					String processXML = process.getRootOperator().getXML(false);

					try {
						FileWriter infoWriter = new FileWriter(autoSavedProcessProperties);
						infoWriter.write(locationInfo);
						infoWriter.flush();
						infoWriter.close();

						FileWriter processWriter = new FileWriter(autoSavedProcess);
						processWriter.write(processXML);
						processWriter.flush();
						processWriter.close();
					} catch (IOException e) {
						LogService.getRoot().log(Level.INFO, "com.rapidminer.gui.autosave.AutoSave.dir_creation_failed", e);
						AutoSave.this.autoSaveEnabled = false;
					}

				}
			});
		}

	}

	private boolean askForRecovery(String processPath) {
		RecoverDialog recoverDialog = new RecoverDialog(processPath);
		recoverDialog.setVisible(true);
		int result = recoverDialog.getReturnOption();
		return result == RecoverDialog.YES_OPTION;
	}

	public void onShutdown() {
		if (autoSaveEnabled) {
			if (autoSavedProcessProperties.exists()) {
				autoSavedProcessProperties.delete();
			}
			if (autoSavedProcess.exists()) {
				autoSavedProcess.delete();
			}
		}

	}

}
