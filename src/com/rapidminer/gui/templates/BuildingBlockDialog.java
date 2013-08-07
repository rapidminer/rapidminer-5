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
package com.rapidminer.gui.templates;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.List;
import java.util.logging.Level;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.UIManager;

import com.rapidminer.gui.tools.ExtendedJScrollPane;
import com.rapidminer.gui.tools.FilterTextField;
import com.rapidminer.gui.tools.FilterableListModel;
import com.rapidminer.gui.tools.ResourceAction;
import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.gui.tools.FilterableListModel.FilterCondition;
import com.rapidminer.gui.tools.dialogs.ButtonDialog;
import com.rapidminer.tools.BuildingBlockService;
import com.rapidminer.tools.I18N;
import com.rapidminer.tools.LogService;


/**
 * This dialog can be used to add a new building block to the process setup. 
 *  
 * @author Tobias Malbrecht
 */
public class BuildingBlockDialog extends ButtonDialog {
		
	private static final long serialVersionUID = 4234757981716378086L;
	
	private static final Color SELECTED_COLOR = UIManager.getColor("Tree.selectionBackground");

	private static final Color TEXT_SELECTED_COLOR = UIManager.getColor("Tree.selectionForeground");

	private static final Color TEXT_NON_SELECTED_COLOR = UIManager.getColor("Tree.textForeground");
	
	private static final Icon ICON_USER_DEFINED = SwingTools.createIcon("16/" + I18N.getMessage(I18N.getGUIBundle(), "gui.dialog.building_block.source.icon"));
	
	private static final String TOOLTIP_USER_DEFINED = I18N.getMessage(I18N.getGUIBundle(), "gui.dialog.building_block.source.tip");

	private boolean ok = false;
	
	protected final FilterableListModel listModel = new FilterableListModel();
	
	private final FilterCondition predefinedCondition = new FilterCondition() {
		@Override
		public boolean matches(Object o) {
			return ((BuildingBlock) o).isPredefined();
		}
	};
	
	private final FilterCondition userdefinedCondition = new FilterCondition() {
		@Override
		public boolean matches(Object o) {
			return ((BuildingBlock) o).isUserDefined();
		}
	};
	
	protected final JList buildingBlockList = new JList(listModel);
	{
		buildingBlockList.setCellRenderer(new DefaultListCellRenderer() {
			final class BuildingBlockPanel extends JPanel {
				private static final long serialVersionUID = 514170387011803814L;
				
				private final JLabel label = new JLabel("");
				
				private final JLabel sourceIconLabel = new JLabel("");
				
				private boolean isSelected = false;
				
				private BuildingBlockPanel() {
					setLayout(new BorderLayout());
					setBackground(new java.awt.Color(0, 0, 0, 0));
					label.setBorder(BorderFactory.createEmptyBorder(GAP, GAP, GAP, GAP));
					sourceIconLabel.setBorder(BorderFactory.createEmptyBorder(GAP, GAP, GAP, 2 * GAP));
					sourceIconLabel.setToolTipText(TOOLTIP_USER_DEFINED);
					add(label, BorderLayout.CENTER);
					add(sourceIconLabel, BorderLayout.EAST);
				}
				
				public void update(BuildingBlock buildingBlock, boolean selected, boolean hasFocus) {
					label.setText("<html>" + buildingBlock.getName() + "<br/><small>" + buildingBlock.getDescription() + "</small></html>");
					label.setIcon(buildingBlock.getSmallIcon());
					if (buildingBlock.isUserDefined()) {
						sourceIconLabel.setIcon(ICON_USER_DEFINED);
					} else {
						sourceIconLabel.setIcon(null);
					}
					if (selected) {
						label.setForeground(TEXT_SELECTED_COLOR);
						sourceIconLabel.setForeground(TEXT_SELECTED_COLOR);
					} else {
						label.setForeground(TEXT_NON_SELECTED_COLOR);
						sourceIconLabel.setForeground(TEXT_NON_SELECTED_COLOR);
					}
					this.isSelected = selected;
				}
				
				@Override
				public void paint(Graphics g) {
					if (isSelected) {
						g.setColor(SELECTED_COLOR);
						g.fillRect(0, 0, getWidth(), getHeight());
					}
					super.paint(g);
				}
			}
			
			private final BuildingBlockPanel panel = new BuildingBlockPanel();
			
			private static final long serialVersionUID = 5546228931379122434L;
			
			@Override
			public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
				BuildingBlock buildingBlock = (BuildingBlock) value;
				panel.update(buildingBlock, isSelected, cellHasFocus);
				return panel;
			}
		});
		buildingBlockList.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if (e.getClickCount() == 2) {
					actOnDoubleClick();
				}
			}
		});
		buildingBlockList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
	}
	
	protected final JScrollPane listPane = new ExtendedJScrollPane(buildingBlockList);
	{
		listPane.setBorder(createBorder());
	}
	
	protected JPanel filterPanel = new JPanel(new BorderLayout());
	{
		((BorderLayout) filterPanel.getLayout()).setHgap(0);
		((BorderLayout) filterPanel.getLayout()).setVgap(0);
		final FilterTextField filter = new FilterTextField();
		filter.addFilterListener(listModel);
		filterPanel.add(filter, BorderLayout.CENTER);
	
		JButton clearFilterButton = new JButton(new ResourceAction(true, "clear_filter") {
			private static final long serialVersionUID = -6347296002673216464L;
	
			@Override
			public void actionPerformed(ActionEvent e) {
				filter.clearFilter();
			}
		});
		clearFilterButton.setText(null);
		filterPanel.add(clearFilterButton, BorderLayout.EAST);
	}
	
	protected JPanel typePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
	{
		((FlowLayout) typePanel.getLayout()).setVgap(0);
		final JCheckBox predefinedCheckBox = new JCheckBox("Show predefined");
		predefinedCheckBox.setSelected(true);
		predefinedCheckBox.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (predefinedCheckBox.isSelected()) {
					listModel.removeCondition(predefinedCondition);
				} else {
					listModel.addCondition(predefinedCondition);
				}
			}			
		});
		typePanel.add(predefinedCheckBox);
		final JCheckBox userdefinedCheckBox = new JCheckBox("Show user defined");
		userdefinedCheckBox.setSelected(true);
		userdefinedCheckBox.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (userdefinedCheckBox.isSelected()) {
					listModel.removeCondition(userdefinedCondition);
				} else {
					listModel.addCondition(userdefinedCondition);
				}
			}			
		});
		typePanel.add(userdefinedCheckBox);
	}

	public BuildingBlockDialog(String i18nKey) {
		this(i18nKey, BuildingBlock.ALL);
	}
	
	public BuildingBlockDialog(String i18nKey, int buildingBlockSource) {
		super(i18nKey, true,new Object[]{});

		// add components to main panel
		List<BuildingBlock> buildingBlocks = null;
		switch (buildingBlockSource) {
		case BuildingBlock.PREDEFINED:
			buildingBlocks = BuildingBlockService.getPredefinedBuildingBlocks();
			break;
		case BuildingBlock.USER_DEFINED:
			buildingBlocks = BuildingBlockService.getUserBuildingBlocks();
			break;
		case BuildingBlock.PLUGIN_DEFINED:
			buildingBlocks = BuildingBlockService.getPluginBuildingBlocks();
			break;
		default:
			buildingBlocks = BuildingBlockService.getBuildingBlocks();
		}
		
		for (BuildingBlock buildingBlock : buildingBlocks) {
			if (!NewBuildingBlockMenu.checkBuildingBlock(buildingBlock)) {
				//LogService.getRoot().log(Level.WARNING, "Cannot initialize building block '" + buildingBlock.getName());		
				LogService.getRoot().log(Level.WARNING, 
						"com.rapidminer.gui.templates.BuildingBlockDialog.intialize_building_block_error", 
						buildingBlock.getName());
			} else {
				listModel.addElement(buildingBlock);
			}
		}
	}
	
	protected void actOnDoubleClick() {
		ok();
	}
	
	@Override
	protected void ok() {
		this.ok = true;
		dispose();
	}
	
	@Override
	protected void cancel() {
		this.ok = false;
		dispose();
	}
	
	public boolean isOk() {
		return ok;
	}
	
	public BuildingBlock getSelectedBuildingBlock() {
		return (BuildingBlock) buildingBlockList.getSelectedValue();
	}
	
	protected void delete() {
		int[] selectionIndices = buildingBlockList.getSelectedIndices();
		for (int i = selectionIndices.length - 1; i >= 0; i--) {
			BuildingBlock buildingBlock = (BuildingBlock) listModel.getElementAt(selectionIndices[i]);
			File buildingBlockFile = buildingBlock.getFile();
			if (buildingBlockFile != null) {
				boolean result = buildingBlockFile.delete();
				if (!result) {
					//LogService.getGlobal().logWarning("Unable to delete building block file: " + buildingBlockFile);
					LogService.getRoot().log(Level.WARNING, "com.rapidminer.gui.templates.BuildingBlockDialog.deleting_building_block_file_error", buildingBlockFile);
				} else {
					listModel.removeElementAt(selectionIndices[i]);
				}
			}
		}
	}
}
