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
package com.rapidminer.repository.gui;

import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyVetoException;
import java.text.DateFormat;
import java.util.Date;
import java.util.List;

import javax.swing.Action;
import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

import com.michaelbaranov.microba.calendar.DatePicker;
import com.rapidminer.Process;
import com.rapidminer.ProcessContext;
import com.rapidminer.ProcessLocation;
import com.rapidminer.RepositoryProcessLocation;
import com.rapidminer.gui.RapidMinerGUI;
import com.rapidminer.gui.actions.SaveAction;
import com.rapidminer.gui.dialog.CronEditorDialog;
import com.rapidminer.gui.processeditor.ProcessContextEditor;
import com.rapidminer.gui.tools.ResourceAction;
import com.rapidminer.gui.tools.ResourceLabel;
import com.rapidminer.gui.tools.ResourceTabbedPane;
import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.gui.tools.dialogs.ButtonDialog;
import com.rapidminer.gui.tools.dialogs.ConfirmDialog;
import com.rapidminer.repository.MalformedRepositoryLocationException;
import com.rapidminer.repository.Repository;
import com.rapidminer.repository.RepositoryException;
import com.rapidminer.repository.RepositoryLocation;
import com.rapidminer.repository.RepositoryManager;
import com.rapidminer.repository.remote.SchedulerResponse;
import com.rapidminer.repository.remote.ProcessSchedulerConfig;
import com.rapidminer.repository.remote.ProcessSchedulerFactory;
import com.rapidminer.repository.remote.RemoteRepository;
import com.rapidminer.tools.I18N;
import com.rapidminer.tools.Observable;
import com.rapidminer.tools.Observer;

/**
 * A dialog that lets the user run a process on a remote server, either now, at
 * a fixed later point of time or scheduled by a cron expression.
 * 
 * @author Simon Fischer, Nils Woehler
 * 
 */
public class RunRemoteDialog extends ButtonDialog {

	private static final long serialVersionUID = 1L;

	public static final String DEFAULT = "DEFAULT";

	private static final DateFormat DATE_FORMAT = DateFormat.getDateTimeInstance(); //new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");

	private final DatePicker dateField = new DatePicker(new Date(), DATE_FORMAT); // new
	// JTextField(DATE_FORMAT.format(new
	// Date()), 30);
	private final JTextField cronField = new JTextField(30);
	private final JComboBox repositoryBox = new JComboBox();
	private static int lastRepositoryIndexSelected = 0;
	private final JLabel dateLabel = new ResourceLabel("runremotedialog.date");
	private final JLabel cronLabel = new ResourceLabel("runremotedialog.cronexpression");
	private JLabel cronHelpIconLabel;
	private JButton cronEditorButton;
	private JComboBox queueComboBox;
	private final JCheckBox startBox = new JCheckBox(new ResourceAction("runremotedialog.cronstart") {

		private static final long serialVersionUID = 1L;

		@Override
		public void actionPerformed(ActionEvent e) {
			enableComponents();
		}
	});
	private final DatePicker startField = new DatePicker(new Date(), DATE_FORMAT);
	private final DatePicker endField = new DatePicker(new Date(), DATE_FORMAT);
	private final JCheckBox endBox = new JCheckBox(new ResourceAction("runremotedialog.cronend") {

		private static final long serialVersionUID = 1L;

		@Override
		public void actionPerformed(ActionEvent e) {
			enableComponents();
		}
	});
	private final JTextField processField = new JTextField(30);

	private JRadioButton nowButton;

	private JRadioButton onceButton;

	private JRadioButton cronButton;;

	private final ResourceTabbedPane tabs = new ResourceTabbedPane("runremotedialog");

	private ProcessContext context = new ProcessContext();

	private CronEditorDialog cronEditor = new CronEditorDialog();

	private ResourceLabel queueLabel;

	private DefaultComboBoxModel queueModel;

	private boolean scheduleOnOk;

	private ProcessContextEditor contextPanel;

	public RunRemoteDialog(Process process, boolean scheduleOnOk) {
		super("runremotedialog", true, new Object[] {});
		this.scheduleOnOk = scheduleOnOk;
		setModal(true);

		dateField.setStripTime(false);
		dateField.setKeepTime(true);
		startField.setStripTime(false);
		startField.setKeepTime(true);
		endField.setStripTime(false);
		endField.setKeepTime(true);

		startBox.setSelected(false);
		endBox.setSelected(false);

		ProcessLocation processLocation = process.getProcessLocation();
		if ((processLocation != null) && (processLocation instanceof RepositoryProcessLocation)) {
			processField.setText(((RepositoryProcessLocation) processLocation).getRepositoryLocation().getPath());
		} else {
			processField.setText("");
		}
		processField.selectAll();

		final JButton okButton = makeOkButton("run_remote_dialog_schedule");
		final JButton cancelButton = makeCancelButton();

		List<RemoteRepository> remoteRepositories = RepositoryManager.getInstance(null).getRemoteRepositories();
		DefaultComboBoxModel aModel = new DefaultComboBoxModel(remoteRepositories.toArray());
		repositoryBox.setModel(aModel);
		repositoryBox.setRenderer(new DefaultListCellRenderer() {

			private static final long serialVersionUID = 1L;

			@Override
			public Component getListCellRendererComponent(JList list,
															Object value, int index, boolean isSelected,
															boolean cellHasFocus) {
				JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected,
						cellHasFocus);
				if (value instanceof RemoteRepository) {
					RemoteRepository repo = (RemoteRepository) value;
					label.setText("<html>" + repo.getAlias() + "<br/><small style=\"color:gray\">(" + repo.getBaseUrl() + ")</small></html>");
				}
				return label;
			}
		});
		if (repositoryBox.getItemCount() < lastRepositoryIndexSelected) {
			lastRepositoryIndexSelected = 0;
		}
		try {
			RepositoryLocation repositoryLocation = process.getRepositoryLocation();
			if (repositoryLocation != null) {
				Repository repository = repositoryLocation.getRepository();
				if (repository instanceof RemoteRepository) {
					repositoryBox.setSelectedItem(repository);
				} else {
					repositoryBox.setSelectedIndex(lastRepositoryIndexSelected);
				}
			} else {
				repositoryBox.setSelectedIndex(lastRepositoryIndexSelected);
			}
		} catch (RepositoryException e1) {
			e1.printStackTrace();
			repositoryBox.setSelectedIndex(lastRepositoryIndexSelected);
		}

		repositoryBox.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				okButton.setEnabled(repositoryBox.getSelectedItem() != null);
				lastRepositoryIndexSelected = repositoryBox.getSelectedIndex();
				updateExecutionQueueComboBox();
			}
		});
		repositoryBox.addPopupMenuListener(new PopupMenuListener() {

			@Override
			public void popupMenuWillBecomeVisible(PopupMenuEvent e) {}

			@Override
			public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
				updateExecutionQueueComboBox();
			}

			@Override
			public void popupMenuCanceled(PopupMenuEvent e) {}

		});

		RepositoryManager.getInstance(null).addObserver(new Observer<Repository>() {

			@Override
			public void update(Observable<Repository> observable, Repository arg) {
				repositoryBox.setModel(new DefaultComboBoxModel(RepositoryManager.getInstance(null).getRemoteRepositories().toArray()));
				if ((arg != null) && (arg instanceof RemoteRepository)) {
					repositoryBox.setSelectedItem(arg);
				}
				pack();
			}
		}, true);

		JPanel schedulePanel = makeSchedulePanel();

		// copy context
		//		List<String> inputRepositoryLocations = new LinkedList<String>();
		//		inputRepositoryLocations.addAll(process.getContext().getInputRepositoryLocations());
		//		context.setInputRepositoryLocations(inputRepositoryLocations);
		//		
		//		List<String> outputRepositoryLocations = new LinkedList<String>();
		//		outputRepositoryLocations.addAll(process.getContext().getOutputRepositoryLocations());
		//		context.setOutputRepositoryLocations(outputRepositoryLocations);
		//
		//		List<Pair<String, String>> macros = new LinkedList<Pair<String,String>>();
		//		for (Pair<String,String> macro : process.getContext().getMacros()) {
		//			macros.add(new Pair<String,String>(macro.getFirst(),macro.getSecond()));
		//		}
		//		context.setMacros(macros);

		contextPanel = new ProcessContextEditor(process, context);
		tabs.addTabI18N("schedule", schedulePanel);
		tabs.addTabI18N("context", contextPanel);

		// Buttons		
		ButtonGroup buttonGroup = new ButtonGroup();
		buttonGroup.add(nowButton);
		buttonGroup.add(onceButton);
		buttonGroup.add(cronButton);

		nowButton.setSelected(true);

		layoutDefault(tabs, NORMAL, okButton, cancelButton);
		enableComponents();
		okButton.setEnabled(repositoryBox.getSelectedItem() != null);
		updateExecutionQueueComboBox();
	}

	private JPanel makeSchedulePanel() {
		GridBagConstraints c = new GridBagConstraints();
		c.anchor = GridBagConstraints.FIRST_LINE_START;
		c.fill = GridBagConstraints.BOTH;
		c.gridwidth = GridBagConstraints.REMAINDER;
		c.weightx = 1;
		c.weighty = 0;

		// Repository
		JPanel repositoryPanel = new JPanel(new GridBagLayout());
		c.insets = new Insets(0, GAP, 0, GAP);
		JLabel label = new ResourceLabel("runremotedialog.repository");
		label.setLabelFor(repositoryBox);
		c.insets = new Insets(GAP, GAP, GAP, GAP);
		repositoryPanel.add(label, c);
		c.gridwidth = GridBagConstraints.RELATIVE;
		c.insets = new Insets(0, GAP, GAP, 0);
		repositoryPanel.add(repositoryBox, c);
		JButton addRepositoryButton = new JButton(RepositoryBrowser.ADD_REPOSITORY_ACTION);
		addRepositoryButton.setText("");
		c.weightx = 0;
		c.gridwidth = GridBagConstraints.REMAINDER;
		c.insets = new Insets(0, 0, GAP, GAP);
		repositoryPanel.add(addRepositoryButton, c);

		// Process
		label = new ResourceLabel("runremotedialog.process_location");
		label.setLabelFor(processField);
		c.weightx = 1;
		c.insets = new Insets(GAP, GAP, 0, GAP);
		repositoryPanel.add(label, c);
		c.gridwidth = GridBagConstraints.RELATIVE;
		c.insets = new Insets(0, GAP, GAP, 0);
		repositoryPanel.add(processField, c);

		JButton selectButton = new JButton(new ResourceAction(true, "repository_select_location") {

			private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed(ActionEvent e) {
				String selected = RepositoryLocationChooser.selectLocation(null, RunRemoteDialog.this);
				if (selected != null) {
					try {
						RepositoryLocation location = new RepositoryLocation(selected);
						Repository repository = location.getRepository();
						String relative = location.getPath();
						repositoryBox.setSelectedItem(repository);
						processField.setText(relative);
					} catch (Exception ex) {
						processField.setText(selected);
					}
					processField.selectAll();
				}
			}
		});
		selectButton.setText("");
		c.weightx = 0;
		c.gridwidth = GridBagConstraints.REMAINDER;
		c.insets = new Insets(0, 0, GAP, GAP);
		repositoryPanel.add(selectButton, c);

		queueModel = new DefaultComboBoxModel();
		queueComboBox = new JComboBox(queueModel);

		queueLabel = new ResourceLabel("runremotedialog.execution_queue");
		queueLabel.setLabelFor(queueComboBox);
		c.gridwidth = GridBagConstraints.REMAINDER;
		c.weightx = 1;
		c.insets = new Insets(GAP, GAP, 0, GAP);
		repositoryPanel.add(queueLabel, c);
		repositoryPanel.add(queueComboBox, c);

		c.gridwidth = GridBagConstraints.REMAINDER;
		c.fill = GridBagConstraints.BOTH;
		c.weighty = 1;
		JPanel dummy = new JPanel();
		repositoryPanel.add(dummy, c);

		// RIGHT SIDE
		// Now
		JPanel schedPanel = new JPanel(new GridBagLayout());
		nowButton = new JRadioButton(new ResourceAction("runremotedialog.now") {

			private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed(ActionEvent e) {
				enableComponents();
			}
		});
		c.weightx = 1;
		c.insets = new Insets(GAP, GAP, GAP, GAP);
		c.gridwidth = GridBagConstraints.REMAINDER;
		schedPanel.add(nowButton, c);

		// Once
		onceButton = new JRadioButton(new ResourceAction("runremotedialog.once") {

			private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed(ActionEvent e) {
				enableComponents();
			}
		});
		c.insets = new Insets(3 * GAP, GAP, GAP, GAP);
		c.gridwidth = GridBagConstraints.REMAINDER;
		schedPanel.add(onceButton, c);

		dateLabel.setLabelFor(dateField);
		c.insets = new Insets(0, 8 * GAP, 0, GAP);
		c.gridwidth = GridBagConstraints.REMAINDER;
		schedPanel.add(dateLabel, c);
		c.insets = new Insets(0, 8 * GAP, GAP, GAP);
		schedPanel.add(dateField, c);

		// Cron
		cronButton = new JRadioButton(new ResourceAction("runremotedialog.cron") {

			private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed(ActionEvent e) {
				enableComponents();
			}
		});
		c.insets = new Insets(3 * GAP, GAP, GAP, GAP);
		c.gridwidth = GridBagConstraints.REMAINDER;
		schedPanel.add(cronButton, c);

		c.insets = new Insets(0, 8 * GAP, 0, GAP);
		cronLabel.setLabelFor(cronField);
		JPanel cronLabelPanel = new JPanel();
		cronLabelPanel.setLayout(new GridBagLayout());
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridx = 0;
		gbc.weightx = 0;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.insets = new Insets(5, 5, 5, 0);
		cronLabelPanel.add(cronLabel, gbc);

		cronHelpIconLabel = new JLabel();
		cronHelpIconLabel.setIcon(SwingTools.createIcon("16/" + I18N.getMessage(I18N.getGUIBundle(), "gui.action.cron_help.icon")));
		cronHelpIconLabel.addMouseListener(new MouseAdapter() {

			@Override
			public void mouseClicked(MouseEvent e) {
				if (cronButton.isSelected()) {
					SwingTools.showMessageDialog("cron_long_help");
				}
			}
		});
		gbc.gridx = 1;
		cronLabelPanel.add(cronHelpIconLabel, gbc);
		gbc.gridx = 2;
		gbc.weightx = 1;
		cronLabelPanel.add(Box.createHorizontalGlue(), gbc);
		schedPanel.add(cronLabelPanel, c);

		c.insets = new Insets(0, 8 * GAP, GAP, 0);
		c.gridwidth = GridBagConstraints.RELATIVE;
		schedPanel.add(cronField, c);

		c.insets = new Insets(0, 0, GAP, GAP);
		//		ResourceLabel cronHelp = new ResourceLabel("cron_help");
		cronEditorButton = new JButton(new ResourceAction(true, "cron_editor") {

			private static final long serialVersionUID = 1L;
			{
				putValue(Action.NAME, "");
			}

			@Override
			public void actionPerformed(ActionEvent e) {
				cronEditor.prompt();
				if (cronEditor.wasConfirmed()) {
					cronField.setText(cronEditor.getCronExpression());
				}
			}
		});
		c.weightx = 0;
		c.gridwidth = GridBagConstraints.REMAINDER;
		schedPanel.add(cronEditorButton, c);
		c.weightx = 1;

		c.insets = new Insets(GAP, 8 * GAP, 0, GAP);
		schedPanel.add(startBox, c);
		c.insets = new Insets(0, 8 * GAP, GAP, GAP);
		schedPanel.add(startField, c);

		c.insets = new Insets(GAP, 8 * GAP, 0, GAP);
		schedPanel.add(endBox, c);
		c.insets = new Insets(0, 8 * GAP, GAP, GAP);
		schedPanel.add(endField, c);

		JPanel panel = new JPanel(createGridLayout(1, 2));
		panel.add(repositoryPanel);
		panel.add(schedPanel);
		return panel;
	}

	private void updateExecutionQueueComboBox() {
		Repository selected = (Repository) repositoryBox.getSelectedItem();
		if (selected != null) {
			if (selected instanceof RemoteRepository) {
				RemoteRepository remoteRepo = (RemoteRepository) selected;
				List<String> processQueueNames = remoteRepo.getProcessQueueNames();
				updateQueueComoboxModel(processQueueNames);
				if (processQueueNames == null || processQueueNames.isEmpty()) {
					enableQueueSelection(false);
				} else {
					enableQueueSelection(true);
				}
			} else {
				enableQueueSelection(false);
				updateQueueComoboxModel(null);
			}
		}
	}

	private void updateQueueComoboxModel(List<String> queueNames) {
		queueModel.removeAllElements();
		if (queueNames == null || queueNames.isEmpty()) {
			queueModel.addElement(DEFAULT);
		} else {
			for (String queueName : queueNames) {
				queueModel.addElement(queueName);
			}
		}
		queueComboBox.setSelectedIndex(0);
	}

	private void enableQueueSelection(boolean enable) {
		queueComboBox.setEnabled(enable);
		queueLabel.setEnabled(enable);
	}

	private void enableComponents() {
		dateLabel.setEnabled(onceButton.isSelected());
		dateField.setEnabled(onceButton.isSelected());
		cronLabel.setEnabled(cronButton.isSelected());
		cronField.setEnabled(cronButton.isSelected());
		cronHelpIconLabel.setEnabled(cronButton.isSelected());
		cronEditorButton.setEnabled(cronButton.isSelected());
		startBox.setEnabled(cronButton.isSelected());
		endBox.setEnabled(cronButton.isSelected());
		startField.setEnabled(cronButton.isSelected() && startBox.isSelected());
		endField.setEnabled(cronButton.isSelected() && endBox.isSelected());
	}

	/**
	 * @param process may not be <code>null</code>!
	 * @param scheduleOnOkay if <code>true</code> the process will be scheduled when the user clicks ok
	 */
	public static void showDialog(Process process, boolean scheduleOnOkay) {
		showDialog(process, scheduleOnOkay, null);
	}

	public static void showDialog(Process process, boolean scheduleOnOkay, ProcessSchedulerConfig config) {
		// no RA repositories found, show message instead of dialog
		if (RepositoryManager.getInstance(null).getRemoteRepositories().size() <= 0) {
			SwingTools.showVerySimpleErrorMessage("schedule_on_ra_no_ra_repo_found");
			return;
		}
		RunRemoteDialog d = new RunRemoteDialog(process, scheduleOnOkay);
		if (config != null) {
			d.adaptToProcessScheduleConfig(process, config);
		}
		d.setVisible(true);
	}

	public ProcessSchedulerConfig createProcessScheduleConfig() throws MalformedRepositoryLocationException {
		String queueName = queueComboBox.isEnabled() ? (String) queueComboBox.getSelectedItem() : null;
		if (nowButton.isSelected()) {
			return new ProcessSchedulerConfig(getRepositoryLocation(), context, queueName);
		} else if (onceButton.isSelected()) {
			Date onceDate = dateField.getDate();
			return new ProcessSchedulerConfig(getRepositoryLocation(), onceDate, context, queueName);
		} else {
			Date start = startBox.isSelected() ? startField.getDate() : null;
			Date end = endBox.isSelected() ? endField.getDate() : null;
			return new ProcessSchedulerConfig(getRepositoryLocation(), cronField.getText(), start, end, context, queueName);
		}
	}

	private RepositoryLocation getRepositoryLocation() throws MalformedRepositoryLocationException {
		Repository repo = (Repository) repositoryBox.getSelectedItem();
		return new RepositoryLocation(RepositoryLocation.REPOSITORY_PREFIX + repo.getName() + processField.getText());
	}

	public void adaptToProcessScheduleConfig(Process process, ProcessSchedulerConfig config) {
		switch (config.getMode()) {
			case CRON:
				cronButton.setSelected(true);
				cronField.setText(config.getCronExpression());
				Date start = config.getStart();
				Date end = config.getEnd();
				startBox.setSelected(start != null);
				endBox.setSelected(end != null);
				try {
					startField.setDate(start);
					endField.setDate(end);
				} catch (PropertyVetoException e1) {}
				break;
			case ONCE:
				onceButton.setSelected(true);
				try {
					dateField.setDate(config.getOnceDate());
				} catch (PropertyVetoException e) {}
			default:
				nowButton.setSelected(true);
				break;
		}
		contextPanel.setProcess(process, config.getContext());
		enableComponents();
		RepositoryLocation location = config.getLocation();
		try {
			repositoryBox.setSelectedItem(location.getRepository());
		} catch (RepositoryException e) {
			repositoryBox.setSelectedIndex(lastRepositoryIndexSelected);
		}
		processField.setText(location.getPath());
		queueComboBox.setSelectedItem(config.getQueueName());
	}

	@Override
	public void ok() {
		if (scheduleOnOk) {
			String location = processField.getText();

			// check if process selected is the same as the current process in the GUI, if so check if it has been edited and ask for save
			// before continuing. Otherwise the last version would be executed which can result in confusion (and therefore support tickets..)
			if (RapidMinerGUI.getMainFrame().getProcess().getProcessLocation() != null) {
				String mainFrameProcessLocString = ((RepositoryProcessLocation) RapidMinerGUI.getMainFrame().getProcess().getProcessLocation()).getRepositoryLocation().getPath();
				if (location.equals(mainFrameProcessLocString) && RapidMinerGUI.getMainFrame().isChanged()) {
					if (SwingTools.showConfirmDialog("save_before_remote_run", ConfirmDialog.OK_CANCEL_OPTION) == ConfirmDialog.CANCEL_OPTION) {
						// user does not want to save "dirty" process, abort
						return;
					}
					SaveAction.save(RapidMinerGUI.getMainFrame().getProcess());
				}
			}

			try {
				SchedulerResponse response = ProcessSchedulerFactory.getInstance().getProcessScheduler().scheduleProcess(createProcessScheduleConfig(), null);
				Date firstExec = response.getFirstExecution();
				SwingTools.showMessageDialog("process_will_first_run", firstExec);
			} catch (Exception e) {
				SwingTools.showSimpleErrorMessage("run_proc_remote", e.getLocalizedMessage());
				return;
			}
		}
		dispose();
	}
}
