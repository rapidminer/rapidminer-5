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
import java.util.logging.Level;

import javax.swing.JLabel;
import javax.swing.JPanel;

import com.rapidminer.gui.MainFrame;
import com.rapidminer.gui.renderer.DefaultTextRenderer;
import com.rapidminer.gui.renderer.Renderer;
import com.rapidminer.gui.renderer.RendererService;
import com.rapidminer.gui.tools.RadioCardPanel;
import com.rapidminer.operator.IOContainer;
import com.rapidminer.operator.IOObject;
import com.rapidminer.operator.ResultObject;
import com.rapidminer.tools.I18N;
import com.rapidminer.tools.LogService;
import com.rapidminer.tools.ParameterService;

/** Static methods to generate result visualization components etc.
 * 
 * @author Simon Fischer
 * */
public class ResultDisplayTools {

    static final String CLIENT_PROPERTY_RAPIDMINER_RESULT_NAME_HTML = "rapidminer.result.name.html";
    static final String CLIENT_PROPERTY_RAPIDMINER_RESULT_ICON = "rapidminer.result.icon";
    static final String CLIENT_PROPERTY_RAPIDMINER_RESULT_NAME = "rapidminer.result.name";
    public static final String[] TYPE_NAMES = {"docking", "tabbed"};

    public static JPanel createVisualizationComponent(IOObject resultObject, IOContainer resultContainer, String usedResultName) {
        final String resultName = RendererService.getName(resultObject.getClass());
        Component visualisationComponent;
        Collection<Renderer> renderers = RendererService.getRenderers(resultName);

        // fallback to default toString method!
        if (resultName == null) {
            renderers.add(new DefaultTextRenderer());
        }

        // constructing panel of renderers
        visualisationComponent = new RadioCardPanel(usedResultName, resultObject);
        for (Renderer renderer : renderers) {
            try {
                Component rendererComponent = renderer.getVisualizationComponent(resultObject, resultContainer);
                if (rendererComponent != null) {
                    ((RadioCardPanel)visualisationComponent).addCard(renderer.getName(), rendererComponent);
                }
            } catch (Exception e) {
                //LogService.getRoot().log(Level.WARNING, "Error creating renderer: "+e, e);
				LogService.getRoot().log(Level.WARNING,
						I18N.getMessage(LogService.getRoot().getResourceBundle(), 
						"com.rapidminer.gui.processeditor.results.ResultDisplayTools.error_creating_renderer", 
						e),
						e);
                ((RadioCardPanel)visualisationComponent).addCard(renderer.getName(), new JLabel("Error creating renderer "+renderer.getName() + " (see log)."));
            }
        }

        // result panel
        final JPanel resultPanel = new JPanel(new BorderLayout());
        resultPanel.putClientProperty("main.component", visualisationComponent);
        resultPanel.add(visualisationComponent, BorderLayout.CENTER);

        if (resultObject instanceof ResultObject) {
            if (((ResultObject)resultObject).getResultIcon() != null) {
                resultPanel.putClientProperty(ResultDisplayTools.CLIENT_PROPERTY_RAPIDMINER_RESULT_ICON, ((ResultObject)resultObject).getResultIcon());
            } else {
                resultPanel.putClientProperty(ResultDisplayTools.CLIENT_PROPERTY_RAPIDMINER_RESULT_ICON, TabbedResultDisplay.defaultResultIcon);
            }
        }
        resultPanel.putClientProperty(ResultDisplayTools.CLIENT_PROPERTY_RAPIDMINER_RESULT_NAME, usedResultName);
        resultPanel.putClientProperty(ResultDisplayTools.CLIENT_PROPERTY_RAPIDMINER_RESULT_NAME_HTML,  "<html>" + usedResultName + "<br/><small>" + resultObject.getSource() + "</small></html>");

        return resultPanel;
    }

    public static ResultDisplay makeResultDisplay() {
        String chosen = ParameterService.getParameterValue(MainFrame.PROPERTY_RAPIDMINER_GUI_RESULT_DISPLAY_TYPE);
        if (chosen != null && chosen.equals("tabbed")) {
            return new TabbedResultDisplay();
        } else {
            return new DockableResultDisplay();
        }
    }

}
