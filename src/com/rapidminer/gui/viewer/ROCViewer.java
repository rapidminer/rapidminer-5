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
package com.rapidminer.gui.viewer;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JTextPane;

import com.rapidminer.report.Renderable;
import com.rapidminer.tools.math.ROCData;
import com.rapidminer.tools.math.ROCDataGenerator;

/**
 * This viewer can be used to show the ROC curve for the given ROC data.
 * It is also able to display the average values of averaged ROC curves
 * together with their standard deviations.
 *  
 * @author Ingo Mierswa
 */
public class ROCViewer extends JPanel implements Renderable {

    private static final long serialVersionUID = -5441366103559588567L;

    private ROCChartPlotter plotter;
    
    public ROCViewer(String message, ROCDataGenerator rocGenerator, List<ROCData> rocData) {
        setLayout(new BorderLayout());
                
        // info string
        JPanel infoPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JTextPane infoText = new JTextPane();
        infoText.setEditable(false);
        infoText.setBackground(infoPanel.getBackground());
        infoText.setFont(infoText.getFont().deriveFont(Font.BOLD));
        infoText.setText(message);
        infoPanel.add(infoText);
        infoPanel.setBorder(BorderFactory.createEtchedBorder());
        add(infoPanel, BorderLayout.NORTH);
                
        // plot panel
        plotter = new ROCChartPlotter();
        plotter.addROCData("ROC", rocData);
        add(plotter, BorderLayout.CENTER);
    }

    public void prepareRendering() {
    	plotter.prepareRendering();
    }
    
    public void finishRendering() {
    	plotter.finishRendering();
    }
    
	public int getRenderHeight(int preferredHeight) {
		return plotter.getRenderHeight(preferredHeight);
	}

	public int getRenderWidth(int preferredWidth) {
		return plotter.getRenderWidth(preferredWidth);
	}

	public void render(Graphics graphics, int width, int height) {
		plotter.render(graphics, width, height);
	}
}
