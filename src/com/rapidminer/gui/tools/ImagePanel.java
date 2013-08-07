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
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;

import javax.swing.JPanel;

/**
 * This panel can be used to display an image.
 *
 * @author Ingo Mierswa
 */
public class ImagePanel extends JPanel {
	
	private static final long serialVersionUID = 3903395116300542548L;
	
	public static final int CHILDRENS_PREFERRED_SIZE = 0;
	
	public static final int IMAGE_PREFERRED_SIZE = 1;
	
	public static final int IMAGE_PREFERRED_HEIGHT = 2;
	
	private transient Image image = null;
	
	private int preferredSizeType = CHILDRENS_PREFERRED_SIZE;
	
	public ImagePanel(Image image, int preferredSizeType) {
		this.image = image;
		this.preferredSizeType = preferredSizeType;
		setOpaque(true);
	}
	
	@Override
	public Dimension getPreferredSize() {
		Dimension dimension = super.getPreferredSize();
		switch (this.preferredSizeType) {
		case CHILDRENS_PREFERRED_SIZE:
			break;
		case IMAGE_PREFERRED_HEIGHT:
			dimension.height = image.getHeight(null);
			break;
		case IMAGE_PREFERRED_SIZE:
			dimension.height = image.getHeight(null);
			dimension.width = image.getWidth(null);
			break;
		}
		return dimension;
	}

	@Override
	public void paintComponent(Graphics graphics) {
		super.paintComponent(graphics);
		Graphics2D g = (Graphics2D)graphics;
		g.setColor(Color.WHITE);
		g.fillRect(0, 0, getWidth(), getHeight());
		if (image != null) {
			g.drawImage(image, 0, 0, getWidth(), getHeight(), this);
		}
		paintChildren(graphics);
	}
}
