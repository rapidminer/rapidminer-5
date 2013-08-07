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
package com.rapidminer.gui.dialog;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JEditorPane;
import javax.swing.JScrollPane;
import javax.swing.UIDefaults;
import javax.swing.UIManager;
import javax.swing.plaf.FontUIResource;

import com.rapidminer.Process;
import com.rapidminer.RepositoryProcessLocation;
import com.rapidminer.gui.MainFrame;
import com.rapidminer.gui.RapidMinerGUI;
import com.rapidminer.gui.tools.ExtendedHTMLJEditorPane;
import com.rapidminer.gui.tools.ExtendedJScrollPane;
import com.rapidminer.gui.tools.ResourceAction;
import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.gui.tools.dialogs.ButtonDialog;
import com.rapidminer.repository.RepositoryLocation;
import com.rapidminer.repository.RepositoryManager;


/**
 * The RapidMiner online tutorial. This dialog loads a set of sample process definitions which
 * can be performed and altered by the user.
 * 
 * @author Ingo Mierswa
 */
public class Tutorial extends ButtonDialog implements WindowListener {

	private static final long serialVersionUID = 2826577972132069114L;

	static {
		UIDefaults uiDefaults = UIManager.getDefaults();
		Font f = new Font("SansSerif", Font.PLAIN, 12);
		Font font = new FontUIResource(f);

		uiDefaults.put("EditorPane.font", font);
	}

	private static final String START_TEXT = "<h2>Welcome to the RapidMiner online tutorial!</h2><p>This tutorial demonstrates basic concepts of RapidMiner and simple process setups which can be performed. The user should have some knowledge in the domains of data mining and ETL.</p><p>Whenever this tutorial refers to the &quot;RapidMiner Tutorial / Manual&quot;, it means the printed version of the RapidMiner manual available at<br><br><center><code>http://rapid-i.com</code></center></p><p>You should read the RapidMiner Manual for better motivation and an idea of the basic concepts of RapidMiner, but you can also try to start with the online tutorial without reading the printed version first. Please read the texts carefully and try at least the suggested steps. The online tutorial will take about one hour.</p><br><h4>Please note:</h4><p>Most parts of RapidMiner provide additional information if you hold the mouse pointer a few moments on the part (tool tip texts). In this way all operators and parameters are described too.</p>";

	private static final String END_TEXT = "<h2>Congratulations!</h2><p>You have finished the RapidMiner online tutorial. You should be able to perform many of the possible process definitions. Now, you know the most important building blocks of the possible data mining process definitions. Of course these building blocks can be arbitrarily nested in RapidMiner as long as their input and output types fits. For a reference of all operators please refer to the RapidMiner Tutorial. Check also the other sample process setups which can be found in the sample directory of RapidMiner.</p><p>We have added many known preprocessing steps and learning operators to RapidMiner. Most data formats can also be handled. If you need to adapt RapidMiner you should read the chapter of the RapidMiner Tutorial which describes the creation of operators and the extension mechanism. RapidMiner can easily be extended. Have fun!</p>";

	private static final String[] PROCESSES = new String[] {
		"01_Learner/01_DecisionTree",
		"01_Learner/12_AssociationRules",
		"01_Learner/19_Stacking",
		"07_Clustering/01_KMeans",
		"05_Visualisation/08_SVMVisualisation",
		"02_Preprocessing/07_MissingValueReplenishment",
		"02_Preprocessing/08_NoiseGenerator",
		"02_Preprocessing/15_ExampleSetJoin",
		"03_Validation/03_XValidation_Numerical",
		"01_Learner/14_CostSensitiveLearningAndROCPlot",
		"01_Learner/13_AsymmetricCostLearning",
		"01_Learner/18_SimpleCostSensitiveLearning",
		"04_Attributes/03_PrincipalComponents",
		"04_Attributes/10_ForwardSelection",
		//"04_Attributes/18_MultiobjectiveSelection", (Uses Weka)
		"03_Validation/12_WrapperValidation",
		"04_Attributes/19_YAGGA",
		"04_Attributes/20_YAGGAResultAttributeSetting",
		"02_Preprocessing/12_UserDefinedFeatureGeneration",
		"04_Attributes/13_EvolutionaryWeighting",
		"05_Visualisation/07_DataSetAndWeightsVisualisation",
		"06_Meta/01_ParameterOptimization",
		"06_Meta/06_OperatorEnabler",
		"04_Attributes/17_WeightingThreshold",
		"03_Validation/13_SignificanceTest",
		"02_Preprocessing/24_GroupBasedCalculations"
	};

	private int state = 0;

	private final MainFrame mainFrame;

	private final JEditorPane description;

	private final JScrollPane descriptionScrollPane;

	private final JButton prevButton, nextButton;

	public Tutorial(MainFrame mainFrame) {
		super("tutorial", false,new Object[]{});
		this.mainFrame = mainFrame;
		setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
		addWindowListener(this);

		description = new ExtendedHTMLJEditorPane("text/html", SwingTools.text2DisplayHtml(START_TEXT));
		description.setEditable(false);
		description.setBackground(this.getBackground());
		description.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		descriptionScrollPane = new ExtendedJScrollPane(description);
		descriptionScrollPane.setBorder(createBorder());

		prevButton = new JButton(new ResourceAction("previous") {
			private static final long serialVersionUID = 1L;
			public void actionPerformed(ActionEvent e) {
				previous();
			}
		});
		prevButton.setEnabled(false);
		nextButton = new JButton(new ResourceAction("next") {
			private static final long serialVersionUID = 1L;
			public void actionPerformed(ActionEvent e) {
				next();
			}
		});
//		mainFrame.newProcess();
		mainFrame.setTutorialMode(true);
		layoutDefault(descriptionScrollPane, prevButton, nextButton, makeCloseButton());
		Dimension size = new Dimension(600, 650);
		this.setSize(size);
		this.setMaximumSize(size);
		this.setPreferredSize(size);
		setLocationRelativeTo(mainFrame);
	}

	private void setProcess(String processName) {
		//String resourceId = "/com/rapidminer/resources/samples/processes/" + processName;

		Process process;
		try {
			RepositoryLocation loc = new RepositoryLocation(RepositoryLocation.REPOSITORY_PREFIX+
					RepositoryManager.getInstance(null).getSampleRepository().getName()+
					RepositoryLocation.SEPARATOR + "processes" + RepositoryLocation.SEPARATOR + processName);
			process = new RepositoryProcessLocation(loc).load(null);
			//process = new Process(Tutorial.class.getResourceAsStream(resourceId));			
		} catch (Exception e) {
			SwingTools.showSimpleErrorMessage("cannot_load_tutorial_file", e, processName);
			dispose();
			return;
		}
		RapidMinerGUI.getMainFrame().setOpenedProcess(process, false, null);			
		description.setText(SwingTools.text2DisplayHtml(process.getRootOperator().getUserDescription()));
		description.getCaret().setDot(0);
		descriptionScrollPane.getVerticalScrollBar().setValue(0);
		setTitle("RapidMiner Tutorial - Step "+state+" of "+PROCESSES.length);
	}

	private void previous() {
		nextButton.setEnabled(true);
		if (state > 0)
			state--;
		if (state == 0) {
			prevButton.setEnabled(false);
			description.setText(SwingTools.text2DisplayHtml(START_TEXT));
			setLocationRelativeTo(mainFrame);
		} else {
			setProcess(PROCESSES[state - 1]);
		}
	}

	private void next() {
		prevButton.setEnabled(true);
		if (state < PROCESSES.length + 1)
			state++;
		if (state == PROCESSES.length + 1) {
			nextButton.setEnabled(false);
			description.setText(SwingTools.text2DisplayHtml(END_TEXT));
		} else {
			setProcess(PROCESSES[state - 1]);
		}
	}

	@Override
	protected void close() {
		mainFrame.setTutorialMode(false);
		dispose();
	}

	public void windowClosed(WindowEvent e) {
	}

	public void windowActivated(WindowEvent e) {}

	public void windowClosing(WindowEvent e) {
		close();
	}

	public void windowDeactivated(WindowEvent e) {}

	public void windowDeiconified(WindowEvent e) {}

	public void windowIconified(WindowEvent e) {}

	public void windowOpened(WindowEvent e) {}
}
