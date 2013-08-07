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
package com.rapidminer.gui.tour;

import com.rapidminer.gui.flow.ProcessPanel;
import com.rapidminer.gui.processeditor.NewOperatorEditor;
import com.rapidminer.gui.properties.OperatorPropertyPanel;
import com.rapidminer.gui.tools.components.BubbleWindow.AlignedSide;
import com.rapidminer.gui.tour.AddBreakpointStep.Position;
import com.rapidminer.gui.tour.Step.BubbleTo;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.SimpleOperatorChain;
import com.rapidminer.operator.io.RepositorySource;
import com.rapidminer.operator.learner.AbstractLearner;
import com.rapidminer.operator.learner.tree.AbstractTreeLearner;
import com.rapidminer.repository.gui.RepositoryBrowser;

/**
 * A class that starts a beginner's tour for RapidMiner as soon as the <code>startTour()</code> method is called.
 * When the user completes an action, the next one is shown by a bubble.
 * 
 * @author Philipp Kersting and Thilo Kamradt
 *
 */
public class RapidMinerTour extends IntroductoryTour {

	AlignedSide side = AlignedSide.RIGHT;
	
	public RapidMinerTour() {
		super(19, "RapidMiner");
	}

	protected void buildTour() {
		//create Steps which will be performed
		step[0] = new SimpleStep(side, "start", "new", true);
		step[1] = new SimpleStep(side, "tryrun", "run", true);
		step[2] = new AddOperatorStep(side, "adddatabase", RepositorySource.class, RepositoryBrowser.REPOSITORY_BROWSER_DOCK_KEY);
		step[3] = new AddOperatorStep(side, "dragdrop", AbstractTreeLearner.class, NewOperatorEditor.NEW_OPERATOR_DOCK_KEY);
		step[4] = new ChangeParameterStep(side, "changeparam", AbstractTreeLearner.class, AbstractTreeLearner.PARAMETER_CRITERION, OperatorPropertyPanel.PROPERTY_EDITOR_DOCK_KEY, "information_gain");
		step[5] = new AddBreakpointStep(BubbleTo.OPERATOR, side, "addbreakpoint", AbstractTreeLearner.class, Position.AFTER);
		step[6] = new SimpleStep(side, "run", "run", true);
		step[7] = new ResumeFromBreakpointStep(side, "goon",((Class<? extends Operator>) AbstractTreeLearner.class), Position.DONT_CARE);
		step[8] = new SaveProcessStep(side, "saveas", "save_as");
		step[9] = new OpenProcessStep(side, "open", "open");
		step[10] = new RemoveOperatorStep(BubbleTo.BUTTON, side, "remove", AbstractTreeLearner.class);
		step[11] = new AddOperatorStep(side, "restore", AbstractLearner.class, NewOperatorEditor.NEW_OPERATOR_DOCK_KEY);
		step[12] = new AddBreakpointStep(BubbleTo.OPERATOR, side, "restorebreakpoint", AbstractLearner.class, Position.BEFORE);
		step[13] = new RemoveBreakpointStep(BubbleTo.OPERATOR, side, "removebreakpoint", AbstractLearner.class, Position.DONT_CARE);
		step[14] = new RenameOperatorStep(BubbleTo.OPERATOR, side, "rename", AbstractLearner.class, "Tree");
		step[15] = new SaveProcessStep(side, "save", "save");
		step[16] = new AddOperatorStep(side, "subprocess", SimpleOperatorChain.class, ProcessPanel.PROCESS_PANEL_DOCK_KEY);
		step[17] = new OpenSubprocessStep(BubbleTo.OPERATOR, side, "opensubprocess", SimpleOperatorChain.class);
		step[18] = new AddOperatorStep(side, "subprocesses", Operator.class, ProcessPanel.PROCESS_PANEL_DOCK_KEY, false);
	}
}
