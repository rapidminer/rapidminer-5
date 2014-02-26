/*
 *  RapidMiner
 *
 *  Copyright (C) 2001-2014 by RapidMiner and the contributors
 *
 *  Complete list of developers available at our web site:
 *
 *       http://rapidminer.com
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
package com.rapidminer.gui.tools.components;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.tools.I18N;

/** Container in which the user can switch the displayed children by clicking on an
 *  icon in a bar on the left hand side.
 *  
 *  Titles and icons for the elements representing the individual cards are taken from
 *  the GUI properties gui.cards.PANEL_KEY.CARD_KEY.title and gui.cards.PANEL_KEY.CARD_KEY.icon
 *  where PANEL_KEY is the key passed to {@link #ButtonBarCardPanel(String)} and CARD_KEY
 *  is the one passed to {@link #addCard(String, JComponent)}.
 * 
 * @author Florian Ziegler
 *
 */
public class ButtonBarCardPanel extends JPanel {

	/** Data container used in the JList in the West panel. */
	private static class Card {

		private String title;
		
		private Icon icon;

		private String key;
		
		public Card(String key, String i18nKey) {
			this.key = key;
			this.title = I18N.getMessage(I18N.getGUIBundle(), "gui.cards."+i18nKey+".title");
			String iconName = I18N.getMessageOrNull(I18N.getGUIBundle(), "gui.cards."+i18nKey+".icon");
			if (iconName != null) {
				this.icon = SwingTools.createIcon("24/"+iconName);
			}
		}
		
		public String getKey() {
			return key;
		}
		
		public String getTitle() {
			return title;
		}
		
		public Icon getIcon() {
			return icon;
		}
	}

	private static final long serialVersionUID = 1L;
	
	private DefaultListModel<Card> cardListModel = new DefaultListModel<Card>();

	private JPanel content;
	
	private JList<Card> navigation;
	
	private CardLayout cardLayout;
	
	private String i18nKey;
	
	public ButtonBarCardPanel(String i18nKey) {
		this.i18nKey = i18nKey;
		
		navigation = new JList<Card>(cardListModel);
		cardLayout = new CardLayout();
		content = new JPanel(cardLayout);
		setLayout(new BorderLayout());
		
		JScrollPane navigationScrollPane = new JScrollPane(navigation);
		navigationScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants
				.HORIZONTAL_SCROLLBAR_NEVER);
		//navigation.setBorder(Borders.getScrollPaneBorder());
		navigation.setBorder(BorderFactory.createLineBorder(Color.black));
		add(navigationScrollPane, BorderLayout.WEST);
		add(content, BorderLayout.CENTER);
		
		navigation.addListSelectionListener(new ListSelectionListener() {			
			@Override
			public void valueChanged(ListSelectionEvent e) {
				Card card = navigation.getSelectedValue();
				if (card != null) {
					cardLayout.show(content, card.getKey());
				}
			}
		});
	
		navigation.setCellRenderer(new DefaultListCellRenderer() {
			private static final long serialVersionUID = 1L;
			
			@Override
			public Component getListCellRendererComponent(JList<?> list,
					Object value, int index, boolean isSelected,
					boolean cellHasFocus) {
				JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index,
						isSelected, cellHasFocus);
				Card card = (Card) value;
				label.setText(card.getTitle());
				label.setIcon(card.getIcon());
				label.setVerticalTextPosition(JLabel.BOTTOM);
				label.setHorizontalTextPosition(JLabel.CENTER);
				label.setHorizontalAlignment(CENTER);
				if (isSelected) {
					label.setBorder(
							BorderFactory.createCompoundBorder(
									BorderFactory.createLineBorder(Color.white, 1),
							BorderFactory.createLineBorder(Color.black, 1)));
				} else {
					label.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));					
				}
				return label;
			}
		});
	}
	
	public void addCard(String key, JComponent componentToAdd) {
		content.add(componentToAdd, key);
		Card card1 = new Card(key, i18nKey + "." + key);
		cardListModel.addElement(card1);
		if (cardListModel.size() == 1) {
			navigation.setSelectedIndex(0);
		}
	}
}
