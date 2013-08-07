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
package com.rapidminer.gui.renderer.models;

import java.awt.BorderLayout;
import java.awt.Component;

import javax.swing.JLabel;
import javax.swing.JPanel;

import com.rapidminer.gui.processeditor.results.ResultDisplayTools;
import com.rapidminer.gui.renderer.AbstractRenderer;
import com.rapidminer.operator.IOContainer;
import com.rapidminer.operator.learner.meta.DelegationModel;
import com.rapidminer.report.Reportable;

/**
 * 
 * @author Sebastian Land
 */
public class DelegationModelRenderer extends AbstractRenderer {

	@Override
	public Reportable createReportable(Object renderable, IOContainer ioContainer, int desiredWidth, int desiredHeight) {
		// TODO: What to do?
		return null;
	}

	@Override
	public String getName() {
		return "Delegation Model";
	}

	@Override
	public Component getVisualizationComponent(Object renderable, IOContainer ioContainer) {
		DelegationModel model = (DelegationModel) renderable;
		JPanel result = new JPanel();
		result.setLayout(new BorderLayout());
		String info = model.getShortInfo();
		if (info != null)
			result.add(new JLabel(info), BorderLayout.NORTH);
		result.add(ResultDisplayTools.createVisualizationComponent(model.getBaseModel(), ioContainer, model.getBaseModel().getName()), BorderLayout.CENTER);
		return result;
	}

}
