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
package com.rapidminer.gui.viewer.collection;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.SwingConstants;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;

import com.rapidminer.gui.processeditor.results.ResultDisplayTools;
import com.rapidminer.gui.tools.ExtendedJScrollPane;
import com.rapidminer.gui.tools.ResourceLabel;
import com.rapidminer.operator.GroupedModel;
import com.rapidminer.operator.IOContainer;
import com.rapidminer.operator.IOObject;
import com.rapidminer.operator.IOObjectCollection;
import com.rapidminer.operator.ResultObject;
import com.rapidminer.operator.learner.meta.MetaModel;


/**
 * Can be used to display the models of a ContainerModel.
 * 
 * @author Ingo Mierswa
 */
public class CollectionViewer extends JPanel {

	private static final long serialVersionUID = -322963469866592863L;

	/** The currently used visualization component. */
	private Component current;

	private final IOObject collection;
	
//	private static IOObjectCollection<IOObject> toCollection(GroupedModel model) {
//		List<IOObject> models = new ArrayList<IOObject>();
//		for (int i = 0; i < model.getNumberOfModels(); i++) {
//			models.add(model.getModel(i));
//		}
//		return new IOObjectCollection<IOObject>(models);
//	}

	public CollectionViewer(final GroupedModel model, final IOContainer container) {
		this((IOObject)model, container);
	}

	public CollectionViewer(final MetaModel model, final IOContainer container) {
		this((IOObject)model, container);
	}

	public CollectionViewer(IOObjectCollection<IOObject> collection, final IOContainer container) {
		this((IOObject)collection, container);
	}
	
	private CollectionViewer(IOObject collection, final IOContainer container) {
		this.collection = collection;
//		// selection list
//		List<String> modelNameList = new LinkedList<String>();
//		for (IOObject ioobject : ioobjects) {
//			if (ioobject instanceof ResultObject) {
//				modelNameList.add(((ResultObject)ioobject).getName());
//			} else {
//				modelNameList.add(ioobject.getClass().getName());
//			}
//		}
		constructPanel(container);
	}

//	public CollectionViewer(final List<? extends IOObject> ioobjects, List<String> ioobjectNames, final IOContainer container) {
//		constructPanel(ioobjects, ioobjectNames, container);
//	}

	private void constructPanel(final IOContainer container) {
		this.current = null;

//		final GridBagLayout gridBag = new GridBagLayout();
//		final GridBagConstraints c = new GridBagConstraints();
//		setLayout(gridBag);
//		c.fill = GridBagConstraints.BOTH;
		setLayout(new BorderLayout());
		
		int size;
		IOObject first = null;
		if (collection instanceof GroupedModel) {
			size  = ((GroupedModel)collection).getNumberOfModels();
			if (size > 0) {
				first = ((GroupedModel)collection).getModel(0);
			}
		} else if (collection instanceof MetaModel) {
			size  = ((MetaModel)collection).getModels().size();
			if (size > 0) {
				first = ((MetaModel)collection).getModels().get(0);
			}
		} else if (collection instanceof IOObjectCollection) {
			size  = ((IOObjectCollection)collection).size();
			if (size > 0) {
				first = ((IOObjectCollection)collection).getElement(0, false);
			}
		} else {
			size = 1;
			first = collection;
		}
		
		switch (size) {
		case 0:
			current = new JLabel("No elements in this collection");
			add(current, BorderLayout.CENTER);
			break;
		case 1:
			IOObject currentObject = first;
			current = ResultDisplayTools.createVisualizationComponent(currentObject, container, 
					(currentObject instanceof ResultObject) ? ((ResultObject)currentObject).getName() : currentObject.getClass().getName());

			add(current, BorderLayout.CENTER);
			break;
		default:
			JTree tree = new JTree(new CollectionTreeModel(collection));
			tree.setCellRenderer(new CollectionTreeCellRenderer(collection));
			tree.addTreeSelectionListener(new TreeSelectionListener() {
				@Override
				public void valueChanged(TreeSelectionEvent e) {
					if ((e.getPath() != null) &&
						(e.getPath().getLastPathComponent() != null)) {						
						if (current != null) {
							remove(current);
						}
						IOObject currentObject = (IOObject) e.getPath().getLastPathComponent();
						if ((currentObject != collection) && // prevent recursive trees
							!(currentObject instanceof IOObjectCollection) &&
							!(currentObject instanceof GroupedModel) &&
							!(currentObject instanceof MetaModel)) {
							current = ResultDisplayTools.createVisualizationComponent(currentObject, container, 
									(currentObject instanceof ResultObject) ? ((ResultObject)currentObject).getName() : currentObject.getClass().getName());
						} else {
							current = new ResourceLabel("collectionviewer.select_leaf");
							((JLabel) current).setVerticalAlignment(SwingConstants.TOP);
							((JLabel) current).setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
						}
						add(current, BorderLayout.CENTER);
						revalidate();
					}
				};
			});
			
			JScrollPane listScrollPane = new ExtendedJScrollPane(tree);
			listScrollPane.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, Color.LIGHT_GRAY));
			add(listScrollPane, BorderLayout.WEST);

			// select first model
			tree.setSelectionRow(0);
			break;
		}
	}
}
