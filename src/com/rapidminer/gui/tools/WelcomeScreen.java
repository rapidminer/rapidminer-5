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
package com.rapidminer.gui.tools;

import java.awt.Color;
import java.awt.Component;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.IOException;
import java.net.URL;
import java.util.logging.Level;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JEditorPane;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JToolBar;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.BevelBorder;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;

import com.rapidminer.ProcessLocation;
import com.rapidminer.gui.MainFrame;
import com.rapidminer.gui.RapidMinerGUI;
import com.rapidminer.gui.actions.OpenAction;
import com.rapidminer.gui.actions.WelcomeNewAction;
import com.rapidminer.gui.actions.WelcomeOpenAction;
import com.rapidminer.gui.actions.WelcomeOpenRecentAction;
import com.rapidminer.gui.actions.WelcomeTourAction;
import com.rapidminer.gui.actions.WelcomeTutorialAction;
import com.rapidminer.gui.actions.WelcomeWizardAction;
import com.rapidminer.gui.tools.dialogs.ButtonDialog;
import com.rapidminer.tools.I18N;
import com.rapidminer.tools.LogService;
import com.rapidminer.tools.RMUrlHandler;
import com.rapidminer.tools.Tools;
import com.vlsolutions.swing.docking.DockKey;
import com.vlsolutions.swing.docking.Dockable;

/**
 * Lets the user select with what he wants to start: blank, existing file,
 * recent file, wizard or tutorial. This panel is shown after RapidMiner was started.
 * 
 * @author Ingo Mierswa
 */
public final class WelcomeScreen extends JPanel implements Dockable {

	private static Image borderTopImage = null;
	
	private static Image borderBottomImage = null;
	
	private static Image bottomImage = null;
	
	static {
		try {
			URL url = Tools.getResource("welcome_border_top.png");
			if (url != null) {
				borderTopImage = ImageIO.read(url);
			}
			
			url = Tools.getResource("welcome_border_bottom.png");
			if (url != null) {
				borderBottomImage = ImageIO.read(url);
			}
			
			url = Tools.getResource("welcome_bottom.png");
			if (url != null) {
				bottomImage = ImageIO.read(url);
			}
		} catch (IOException e) {
			//LogService.getRoot().warning("Cannot load images for welcome screen. Using empty welcome screen...");
			LogService.getRoot().log(Level.WARNING, "com.rapidminer.gui.tools.WelcomeScreen.loading_images_error");
		}
	}
	
	private static final long serialVersionUID = -6916236648023490473L;

	protected static final String PROXY_HELP = "<p>We urgently recommend to provide proxy settings since they affect the functionality "+
		"of RapidMiner in various ways. Amongst others, Internet connectivity is required for</p><ul>"+
		"<li>updating RapidMiner via the Update Server,</li>"+
		"<li>installing numerous RapidMiner extensions,</li>"+
		"<li>Web crawling and Web content mining (RapidMiner Web Extension),</li>"+
		"<li>sharing processes via the RapidMiner Community Extension,</li>"+
		"<li>accessing remote repositories,</li>"+
		"<li>getting online help, and</li>"+
		"<li>displaying the RapidMiner news page.</li></ul>"+
		"<p>Specifying the respective settings in the preferences takes effect immediately without restarting RapidMiner.</p>";

	private final JList recentFileList;

	private final MainFrame mainFrame;

	public WelcomeScreen(MainFrame mainFrame) {
		this.mainFrame = mainFrame;
		GridBagLayout layout = new GridBagLayout();
		setLayout(layout);
		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.BOTH;
		c.weightx = 1;
		c.weighty = 0;
		c.gridwidth = GridBagConstraints.REMAINDER;
		
		
		// border top
		JPanel borderTopPanel = new ImagePanel(borderTopImage, ImagePanel.IMAGE_PREFERRED_HEIGHT);		

		add(borderTopPanel, c);
		

		// central actions		
		JToolBar actionBar = new ExtendedJToolBar();
		actionBar.setBorder(null);
		actionBar.setLayout(new FlowLayout(FlowLayout.CENTER));
		actionBar.setBackground(Color.WHITE);
		actionBar.setBorderPainted(false);

		JButton button = new JButton(new WelcomeNewAction(this.mainFrame));
		button.setHorizontalTextPosition(SwingConstants.CENTER);
		button.setVerticalTextPosition(SwingConstants.BOTTOM);
		actionBar.add(button);
		
		actionBar.addSeparator();
		actionBar.addSeparator();
		actionBar.addSeparator();
		actionBar.addSeparator();
		
		button = new JButton(new WelcomeOpenRecentAction(this));
		button.setHorizontalTextPosition(SwingConstants.CENTER);
		button.setVerticalTextPosition(SwingConstants.BOTTOM);
		if (RapidMinerGUI.getRecentFiles().size() == 0) {
			button.setEnabled(false);
		}
		actionBar.add(button);

		actionBar.addSeparator();
		actionBar.addSeparator();
		actionBar.addSeparator();
		actionBar.addSeparator();
		
		button = new JButton(new WelcomeOpenAction());
		button.setHorizontalTextPosition(SwingConstants.CENTER);
		button.setVerticalTextPosition(SwingConstants.BOTTOM);
		actionBar.add(button);
		
		actionBar.addSeparator();
		actionBar.addSeparator();
		actionBar.addSeparator();
		actionBar.addSeparator();
		
		button = new JButton(new WelcomeWizardAction(this.mainFrame));
		button.setHorizontalTextPosition(SwingConstants.CENTER);
		button.setVerticalTextPosition(SwingConstants.BOTTOM);
		actionBar.add(button);
		
		actionBar.addSeparator();
		actionBar.addSeparator();
		actionBar.addSeparator();
		actionBar.addSeparator();
		
		button = new JButton(new WelcomeTutorialAction(this.mainFrame));
		button.setHorizontalTextPosition(SwingConstants.CENTER);
		button.setVerticalTextPosition(SwingConstants.BOTTOM);
		actionBar.add(button);
		
//		actionBar.addSeparator();
//		actionBar.addSeparator();
//		actionBar.addSeparator();
//		actionBar.addSeparator();
//		
//		button = new JButton(new WelcomeTourAction());
//		button.setHorizontalTextPosition(SwingConstants.CENTER);
//		button.setVerticalTextPosition(SwingConstants.BOTTOM);
//		actionBar.add(button);
		

		add(actionBar, c);

		// recent files
		recentFileList = new JList(RapidMinerGUI.getRecentFiles().toArray(new Object[RapidMinerGUI.getRecentFiles().size()]));
		recentFileList.setBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED));
		recentFileList.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
		recentFileList.setBorder(ButtonDialog.createTitledBorder("Recent Processes"));
		MouseListener mouseListener = new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if (e.getClickCount() == 2) {
					WelcomeScreen.this.mainFrame.getPerspectives().showPerspective("design");
					openRecentProcess();
				}
			}
		};
		recentFileList.addMouseListener(mouseListener);
		
		JPanel listPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
		listPanel.add(recentFileList);
		listPanel.setBackground(Color.WHITE);
		add(listPanel, c);

		// border bottom
		JPanel borderBottomPanel = new ImagePanel(borderBottomImage, ImagePanel.IMAGE_PREFERRED_HEIGHT);

		add(borderBottomPanel, c);
		
		// bottom text panel
		JPanel bottomTextPanel = new ImagePanel(bottomImage, ImagePanel.CHILDRENS_PREFERRED_SIZE);
		BoxLayout textLayout = new BoxLayout(bottomTextPanel, BoxLayout.X_AXIS);
		bottomTextPanel.setLayout(textLayout);
		
		// news text
		/*
		String transformedNewsText = Tools.transformAllLineSeparators(newsText);
		final String[] newsLines = transformedNewsText.split("\n");
		JPanel newsTextPanel = new TextPanel("NEWS", newsLines, TextPanel.ALIGNMENT_LEFT, TextPanel.ALIGNMENT_BOTTOM);
		bottomTextPanel.add(newsTextPanel);
		*/
		

		final JEditorPane newsPane = new ExtendedHTMLJEditorPane("text/html", "<html><body><h1>RapidMiner News</h1><p>Downloading news. If news don't show up, check your Internet connection and proxy settings in the <a href=\""+RMUrlHandler.PREFERENCES_URL+"\">Preferences</a> under \"System\"."+PROXY_HELP+"</body></html>");
		((ExtendedHTMLJEditorPane) newsPane).installDefaultStylesheet();
		//newsPane.setText("<html><body><h1>RapidMiner News</h1><p>Downloading news. If news don't show up, check your Internet connection and proxy settings in the preferences.</body></html>");
		new Thread("Load News") {
			@Override
			public void run() {
				try {
					newsPane.setPage(new URL("http://news.rapidminer.com/"));
				} catch (IOException e2) {
					//LogService.getRoot().log(Level.INFO, "Cannot download news: "+e2, e2);
					LogService.getRoot().log(Level.INFO, 
							I18N.getMessage(LogService.getRoot().getResourceBundle(), 
							"com.rapidminer.gui.tools.WelcomeScreen.downloading_news_error", e2), 
					e2);
					
					// JEditorPane.setText("") should be thread-safe, but forum report suggests it may not be.
					// No harm in actually making it 100% swing deadlock safe, therefore using SingUtilities.invokeLater()
					SwingUtilities.invokeLater(new Runnable() {

						@Override
						public void run() {
							newsPane.setText("<html><body><h1>RapidMiner News</h1><p>Cannot download news. Internet connection may be down. If your Internet connection is up, please check your proxy settings in the <a href=\""+RMUrlHandler.PREFERENCES_URL+"\">preferences</a> under \"System\"."+PROXY_HELP+"</body></html>");
						}
						
					});
					
//					newsPane = new ExtendedHTMLJEditorPane("text/html", "<html><body><h1>RapidMiner News</h1><p>Cannot download news. Internet connection may be down. If your Internet connection is up, please check your proxy settings in the preferences.</body></html>");
				}				
			}
		}.start();
		newsPane.setOpaque(false);
		newsPane.setEditable(false);
		newsPane.setPreferredSize(new Dimension(300, 180));
		JScrollPane newsScrollPane = new JScrollPane(newsPane);
		newsScrollPane.setPreferredSize(new Dimension(300, 180));
		newsScrollPane.setOpaque(false);
		newsScrollPane.getViewport().setOpaque(false);
		//newsScrollPane.setBorder(BorderFactory.createTitledBorder("News"));
		newsScrollPane.setBorder(null);
		bottomTextPanel.add(newsScrollPane);
		newsPane.addHyperlinkListener(new HyperlinkListener() {
			@Override
			public void hyperlinkUpdate(HyperlinkEvent e) {
				if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
					if (!RMUrlHandler.handleUrl(e.getDescription())) {
						try {
							Desktop.getDesktop().browse(e.getURL().toURI());
						} catch (Exception e1) {
							//LogService.getRoot().log(Level.WARNING, "Cannot display news site "+e.getDescription()+" ("+e1.getMessage()+"). Network may be down.", e1);
							LogService.getRoot().log(Level.WARNING, 
									I18N.getMessage(LogService.getRoot().getResourceBundle(), 
									"com.rapidminer.gui.tools.WelcomeScreen.displaying_news_site_error", e.getDescription(), e1.getMessage(), e1), 
							e1);
							
						}	
					}
				}				
			}
		});
		
//		try {
//			newsPane.setPage("http://news.rapidminer.com/");
//		} catch (IOException e1) {
//			newsPane.setText("<html><body><h1>RapidMiner News</h1><p>Cannot download news. Internet connection may be down.</body></html>");
//			LogService.getRoot().log(Level.INFO, "Cannot download news: "+e1, e1);
//		}
		
		c.weighty = 1;
		layout.setConstraints(bottomTextPanel, c);
		add(bottomTextPanel);
	}
	
	public void openRecentProcess() {
		int selectedIndex = recentFileList.getSelectedIndex();
		if (selectedIndex < 0)
			selectedIndex = 0;
		if (RapidMinerGUI.getRecentFiles().size() > 0)
			OpenAction.open(RapidMinerGUI.getRecentFiles().get(selectedIndex), true);
		else
			OpenAction.open();
	}
	

	public static final String WELCOME_SCREEN_DOCK_KEY = "welcome";
	private final DockKey DOCK_KEY = new ResourceDockKey(WELCOME_SCREEN_DOCK_KEY);

	@Override
	public Component getComponent() {
		return this;
	}

	@Override
	public DockKey getDockKey() {
		return DOCK_KEY;
	}
	
	/**
	 * Updates the recent file list.
	 */
	public void updateRecentFileList() {
		DefaultListModel model = new DefaultListModel();
		for (ProcessLocation location : RapidMinerGUI.getRecentFiles()) {
			model.addElement(location);
		}
		recentFileList.setModel(model);
	}
}
