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

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;

import com.rapidminer.gui.MainFrame;
import com.rapidminer.gui.RapidMinerGUI;
import com.rapidminer.gui.actions.CloseAllResultsAction;
import com.rapidminer.gui.renderer.RendererService;
import com.rapidminer.gui.tools.ProgressThread;
import com.rapidminer.gui.tools.ResourceLabel;
import com.rapidminer.operator.IOContainer;
import com.rapidminer.operator.IOObject;
import com.rapidminer.operator.ResultObject;
import com.rapidminer.tools.Tools;
import com.vlsolutions.swing.docking.DockKey;
import com.vlsolutions.swing.docking.Dockable;
import com.vlsolutions.swing.docking.DockableActionCustomizer;

/** Dockable containing a single result.
 * 
 * @author Simon Fischer
 *
 */
public class ResultTab extends JPanel implements Dockable {

	private static final long serialVersionUID = 1L;
//	private static final String CLIENT_PROPERTY_RAPIDMINER_RESULT_ICON = "rapidminer.result.icon";
//	private static final String CLIENT_PROPERTY_RAPIDMINER_RESULT_MAIN_COMPONENT = "main.component";
//	private static final String CLIENT_PROPERTY_RAPIDMINER_RESULT_NAME_HTML = "rapidminer.result.name.html";
	
//	private static final String CLIENT_PROPERTY_RAPIDMINER_RESULT_NAME = "rapidminer.result.name";

//	private static final String DEFAULT_RESULT_ICON_NAME = "presentation_chart.png";
//	private static final Icon DEFAULT_RESULT_ICON = SwingTools.createIcon("16/" + DEFAULT_RESULT_ICON_NAME);
	
	public static final String DOCKKEY_PREFIX = "result_";
	
	private Component label;
	private Component component;
	private final DockKey dockKey;
	private final String id;
	
	public ResultTab(String id) {
		setLayout(new BorderLayout());
		this.id = id;
		this.dockKey = new DockKey(id, "Result "+id);
		this.dockKey.setDockGroup(MainFrame.DOCK_GROUP_RESULTS);
		this.dockKey.setName(id);
		this.dockKey.setFloatEnabled(true);
		DockableActionCustomizer customizer = new DockableActionCustomizer(){

			@Override
			public void visitTabSelectorPopUp(JPopupMenu popUpMenu, Dockable dockable){
				popUpMenu.add(new JMenuItem(new CloseAllResultsAction(RapidMinerGUI.getMainFrame())));
			}
		};
		customizer.setTabSelectorPopUpCustomizer(true); // enable tabbed dock custom popup menu entries
		this.dockKey.setActionCustomizer(customizer);
		label = makeStandbyLabel();
		add(label, BorderLayout.NORTH);
	}
	
	/** Creates a component for this object and displays it. 
	 *  This method does not have to be called on the EDT. It executes a
	 *  time consuming task and should be called from a {@link ProgressThread}. */
	public void showResult(final ResultObject resultObject) {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				if (label != null) {
					remove(label);
					label = null;
				}
				if (resultObject != null) {
					dockKey.setName(resultObject.getName() + " ("+resultObject.getSource()+")");
					dockKey.setTooltip(Tools.toString(resultObject.getProcessingHistory(), " \u2192 "));
					label = makeStandbyLabel();
					add(label, BorderLayout.NORTH);
				} else {
					if (id.startsWith(DOCKKEY_PREFIX+"process_")) {
						String number = id.substring((DOCKKEY_PREFIX+"process_").length()); 
						label = new ResourceLabel("resulttab.cannot_be_restored_process_result", number);
						dockKey.setName("Result #"+number);
					} else {
						label = new ResourceLabel("resulttab.cannot_be_restored");
						dockKey.setName("Result "+id);
					}
					((JComponent)label).setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
					add(label, BorderLayout.NORTH);
				}
				// remove old component
				if (component != null) {
					remove(component);
				}				
			}			
		});			
		
		if (resultObject != null) {
			final Component newComponent = createComponent(resultObject, null);
			SwingUtilities.invokeLater(new Runnable() {
				@Override
				public void run() {	
					if (label != null) {
						remove(label);
						label = null;
					}
					component = newComponent;
					add(component, BorderLayout.CENTER);					
					if (component instanceof JComponent) {
						dockKey.setIcon((Icon)((JComponent)component).getClientProperty(ResultDisplayTools.CLIENT_PROPERTY_RAPIDMINER_RESULT_ICON));
					}
				}
			});		
		}
	}
	
	private static JComponent makeStandbyLabel() {
		JComponent label = new ResourceLabel("resulttab.creating_display");
		label.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
		return label;
	}
	
	/** Creates an appropriate name, appending a number to make names unique, and calls 
	 *  {@link #createVisualizationComponent(IOObject, IOContainer, String)}. */
	private JPanel createComponent(ResultObject resultObject, IOContainer resultContainer) {
		final String resultName = RendererService.getName(resultObject.getClass());
		String usedResultName = resultObject.getName();
		if (usedResultName == null) {
			usedResultName = resultName;
		}
		return ResultDisplayTools.createVisualizationComponent(resultObject, resultContainer, id + ": "+usedResultName);
	}

//	private static JPanel createVisualizationComponent(IOObject resultObject, IOContainer resultContainer, String usedResultName) {
//		final String resultName = RendererService.getName(resultObject.getClass());
//		Component visualisationComponent;
//		Collection<Renderer> renderers = RendererService.getRenderers(resultName);
//
//		// fallback to default toString method!
//		if (resultName == null) {
//			renderers.add(new DefaultTextRenderer());
//		}
//
//		// constructing panel of renderers
//		visualisationComponent = new RadioCardPanel(usedResultName, resultObject);
//		for (Renderer renderer : renderers) {
//			Component rendererComponent = renderer.getVisualizationComponent(resultObject, resultContainer);
//			if (rendererComponent != null)
//				((RadioCardPanel)visualisationComponent).addCard(renderer.getName(), rendererComponent);
//		}
//
//		// result panel
//		final JPanel resultPanel = new JPanel(new BorderLayout());
//		resultPanel.putClientProperty(CLIENT_PROPERTY_RAPIDMINER_RESULT_MAIN_COMPONENT, visualisationComponent);
//		resultPanel.add(visualisationComponent, BorderLayout.CENTER);
//
//		if (resultObject instanceof ResultObject) {
//			if (((ResultObject)resultObject).getResultIcon() != null) {
//				resultPanel.putClientProperty(CLIENT_PROPERTY_RAPIDMINER_RESULT_ICON, ((ResultObject)resultObject).getResultIcon());
//			} else {
//				resultPanel.putClientProperty(CLIENT_PROPERTY_RAPIDMINER_RESULT_ICON, DEFAULT_RESULT_ICON);					 
//			}
//		}
//		resultPanel.putClientProperty(CLIENT_PROPERTY_RAPIDMINER_RESULT_NAME, usedResultName);
//		resultPanel.putClientProperty(CLIENT_PROPERTY_RAPIDMINER_RESULT_NAME_HTML,  "<html>" + usedResultName + "<br/><small>" + resultObject.getSource() + "</small></html>");
//
//		return resultPanel;
//	}



	@Override
	public Component getComponent() {
		return this;
	}

	@Override
	public DockKey getDockKey() {
		return dockKey;
	}

	public void freeResources() {
		if (component != null) {
			remove(component);
			component = null;
		}		
	}
}
