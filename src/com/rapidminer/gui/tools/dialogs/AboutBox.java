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
package com.rapidminer.gui.tools.dialogs;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Paint;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;

import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.KeyStroke;

import com.rapidminer.gui.tools.ResourceAction;
import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.gui.tools.components.LinkButton;
import com.rapidminer.tools.LogService;
import com.rapidminer.tools.Tools;
import com.rapidminer.tools.plugin.Plugin;

/**
 * This dialog displays some informations about the product. The product logo should have a size of approximately 270
 * times 70 pixels.
 * 
 * @author Ingo Mierswa
 */
public class AboutBox extends JDialog {

	private static final long serialVersionUID = -3889559376722324215L;

	private static final String PROPERTY_FILE = "about_infos.properties";

	private static final String RAPID_MINER_LOGO_NAME = "rapidminer_logo.png";
	public static final Image RAPID_MINER_LOGO;
	public static Image backgroundImage = null;
	static {
		URL url = Tools.getResource(RAPID_MINER_LOGO_NAME);
		Image rmLogo = null;
		if (url != null) {
			try {
				rmLogo = ImageIO.read(url);
			} catch (IOException e) {
				//LogService.getGlobal().logWarning("Cannot load logo for about box. Using empty image...");
				LogService.getRoot().log(Level.WARNING, "com.rapidminer.gui.tools.dialogs.AboutBox.loading_logo_error");
			}
		}
		RAPID_MINER_LOGO = rmLogo;
		url = Tools.getResource("splashscreen_community.png");
		if (url != null) {
			try {
				backgroundImage = ImageIO.read(url);
			} catch (IOException e) {
				//LogService.getGlobal().logWarning("Cannot load background for about box. Using empty image...");
				LogService.getRoot().log(Level.WARNING, "com.rapidminer.gui.tools.dialogs.AboutBox.loading_background_error");
			}
		}
	}

	private ContentPanel contentPanel;

	private static class ContentPanel extends JPanel {

		private static final long serialVersionUID = -1763842074674706654L;

		private static final Paint MAIN_PAINT = Color.LIGHT_GRAY;

		private static final int MARGIN = 10;

		private Properties properties;

		private transient Image productLogo;

		public ContentPanel(Properties properties, Image productLogo) {
			this.properties = properties;
			this.productLogo = productLogo;

			int width = 450;
			int height = 350;
			if (backgroundImage != null) {
				width = backgroundImage.getWidth(this);
				height = backgroundImage.getHeight(this);
			}
			setPreferredSize(new Dimension(width, height));
			setMinimumSize(new Dimension(width, height));
			setMaximumSize(new Dimension(width, height));
		}

		@Override
		public void paint(Graphics g) {
			super.paint(g);
			((Graphics2D) g).setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
			drawMain((Graphics2D) g);
			g.setColor(Color.black);
			g.drawRect(0, 0, getWidth() - 1, getHeight() - 1);
		}

		public void drawMain(Graphics2D g) {
			g.setPaint(MAIN_PAINT);
			g.fillRect(0, 0, getWidth(), getHeight());

			if (backgroundImage != null)
				g.drawImage(backgroundImage, 0, 0, this);

			int nameY = 100 + 26;
			g.setFont(new java.awt.Font("SansSerif", java.awt.Font.BOLD, 26));
			g.setColor(SwingTools.RAPID_I_BROWN);
			if (productLogo != null) {
				if ("true".equals(properties.getProperty("textNextToLogo"))) {
					g.drawImage(productLogo, 20, 90, this);
					g.drawString(properties.getProperty("name"), 20 + productLogo.getWidth(null) + 10, nameY);
				} else {
					g.drawImage(productLogo, getWidth() / 2 - productLogo.getWidth(this) / 2, 90, this);
				}
			} else {
				g.drawString(properties.getProperty("name"), 20, nameY);
			}

			int y = 240;
			g.setColor(SwingTools.BROWN_FONT_COLOR);
			g.setFont(new java.awt.Font("SansSerif", java.awt.Font.BOLD, 11));
			drawString(g, properties.getProperty("name") + " " + properties.getProperty("version"), y);
			y += 20;

			g.setFont(new java.awt.Font("SansSerif", java.awt.Font.PLAIN, 10));
			y = drawStringAndAdvance(g, properties.getProperty("name") + " " + properties.getProperty("version"), y);
			y = drawStringAndAdvance(g, properties.getProperty("copyright"), y);
			y = drawStringAndAdvance(g, properties.getProperty("licensor"), y);
			y = drawStringAndAdvance(g, properties.getProperty("license"), y);
			y = drawStringAndAdvance(g, properties.getProperty("warranty"), y);
			y = drawStringAndAdvance(g, properties.getProperty("more"), y);
		}

		private int drawStringAndAdvance(Graphics2D g, String string, int y) {
			if (string == null) {
				return y;
			} else {
				List<String> lines = new LinkedList<String>();
				String[] words = string.split("\\s+");
				String current = "";
				for (String word : words) {
					if (current.length() + word.length() < 80) {
						current += word + " ";
					} else {
						lines.add(current);
						current = word + " ";
					}
				}
				if (!current.isEmpty()) {
					lines.add(current);
				}
				for (String line : lines) {
					drawString(g, line, y);
					y += 15;
				}
				return y;
			}
		}

		private void drawString(Graphics2D g, String text, int y) {
			if (text == null)
				return;
			float xPos = MARGIN;
			float yPos = y;
			g.drawString(text, xPos, yPos);
		}
	}

	public AboutBox(Frame owner, String productName, String productVersion, String licensor, String url, String text, boolean renderTextNextToLogo, Image productLogo) {
		this(owner, createProperties(productName, productVersion, licensor, url, text, renderTextNextToLogo), productLogo);
	}

	public AboutBox(Frame owner, String productVersion, Image productLogo) {
		this(owner, createProperties(productVersion), productLogo);
	}

	public AboutBox(Frame owner, Properties properties, Image productLogo) {
		super(owner, "About", true);
//		if (productLogo == null) {
//			productLogo = rapidMinerLogo;
//		}
		setResizable(false);

		setLayout(new BorderLayout());

		String name = properties.getProperty("name");
		if (name != null) {
			setTitle("About " + name);
		}
		contentPanel = new ContentPanel(properties, productLogo);
		add(contentPanel, BorderLayout.CENTER);

		JPanel buttonPanel = new JPanel(new GridBagLayout());
		// FlowLayout(FlowLayout.RIGHT));
		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.BOTH;
		final String url = properties.getProperty("url");
		if (url != null) {
			c.weightx = 1;
			c.gridwidth = GridBagConstraints.RELATIVE;
			buttonPanel.add(new LinkButton(new ResourceAction("simple_link_action", url) {
				private static final long serialVersionUID = 1L;

				@Override
				public void actionPerformed(ActionEvent e) {
					try {
						Desktop.getDesktop().browse(new URI(url));
					} catch (Exception e1) {
						e1.printStackTrace();
					}
				}
			}), c);
		}

		ResourceAction closeAction = new ResourceAction("close") {
			private static final long serialVersionUID = 1407089394491740308L;

			public void actionPerformed(ActionEvent e) {
				dispose();
			}
		};
		JButton closeButton = new JButton(closeAction);
		c.weightx = 0;
		c.gridwidth = GridBagConstraints.REMAINDER;
		buttonPanel.add(closeButton, c);

		add(buttonPanel, BorderLayout.SOUTH);

		getRootPane().setDefaultButton(closeButton);
		getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0, false), "CANCEL");
		getRootPane().getActionMap().put("CANCEL", closeAction);

		pack();
		setLocationRelativeTo(owner);
	}

	public static Properties createProperties(InputStream inputStream, String productVersion) {
		Properties properties = new Properties();
		if (inputStream != null) {
			try {
				properties.load(inputStream);
			} catch (Exception e) {
				//LogService.getGlobal().logError("Cannot read splash screen infos: " + e.getMessage());
				LogService.getRoot().log(Level.SEVERE, "com.rapidminer.gui.tools.dialogs.AboutBox.reading_splash_screen_error",  e.getMessage());
			}
		}
		properties.setProperty("version", productVersion);
		Plugin.initAboutTexts(properties);
		return properties;
	}

	private static Properties createProperties(String productVersion) {
		Properties properties = new Properties();
		try {
			URL propUrl = Tools.getResource(PROPERTY_FILE);
			if (propUrl != null) {
				InputStream in = propUrl.openStream();
				properties.load(in);
				in.close();
			}
		} catch (Exception e) {
			//LogService.getGlobal().logError("Cannot read splash screen infos: " + e.getMessage());
			LogService.getRoot().log(Level.SEVERE, "com.rapidminer.gui.tools.dialogs.AboutBox.reading_splash_screen_error",  e.getMessage());
		}
		properties.setProperty("version", productVersion);
		Plugin.initAboutTexts(properties);
		return properties;
	}

	private static Properties createProperties(String productName, String productVersion, String licensor, String url, String text, boolean renderTextNextToLogo) {
		Properties properties = new Properties();
		properties.setProperty("name", productName);
		properties.setProperty("version", productVersion);
		properties.setProperty("licensor", licensor);
		properties.setProperty("license", "URL: " + url);
		properties.setProperty("more", text);
		properties.setProperty("textNextToLogo", "" + renderTextNextToLogo);
		properties.setProperty("url", url);
		return properties;
	}

}
