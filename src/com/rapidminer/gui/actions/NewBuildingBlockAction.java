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
package com.rapidminer.gui.actions;

import java.awt.event.ActionEvent;
import java.io.StringReader;
import java.util.Collections;
import java.util.LinkedList;

import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;

import com.rapidminer.gui.templates.BuildingBlock;
import com.rapidminer.gui.templates.NewBuildingBlockDialog;
import com.rapidminer.gui.tools.ResourceAction;
import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.io.process.XMLImporter;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.UnknownParameterInformation;


/**
 * Start the corresponding action.
 * 
 * @author Ingo Mierswa, Tobias Malbrecht
 */
public class NewBuildingBlockAction extends ResourceAction {

	private static final long serialVersionUID = 3466426013029085115L;

	private final Actions actions;
	
	public NewBuildingBlockAction(Actions actions) {
		super(true, "new_building_block");
		setCondition(OPERATOR_CHAIN_SELECTED, MANDATORY);
		this.actions = actions;
	}

	public void actionPerformed(ActionEvent e) {
		Operator selectedOperator = this.actions.getSelectedOperator();
		if (selectedOperator != null) {
			NewBuildingBlockDialog dialog = new NewBuildingBlockDialog();
			dialog.setVisible(true);
			if (dialog.isOk()) {
				try {
					BuildingBlock buildingBlock = dialog.getSelectedBuildingBlock();
					if (buildingBlock != null) {
						String xmlDescription = buildingBlock.getXML();
						try {
							InputSource source = new InputSource(new StringReader(xmlDescription));
							Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(source);
							Element element = document.getDocumentElement();
                            Operator operator = Operator.createFromXML(element, actions.getProcess(), new LinkedList<UnknownParameterInformation>(), null, XMLImporter.CURRENT_VERSION);
                            operator.setUserDescription(buildingBlock.getDescription());
							actions.insert(Collections.singletonList(operator));
						} catch (Exception ex) {
							SwingTools.showSimpleErrorMessage("cannot_instantiate_building_block", ex, buildingBlock.getName());
						}
					}
				} catch (Exception ex) {
					SwingTools.showSimpleErrorMessage("cannot_instantiate_building_block", ex);
				}
			}
		}
	}
}
