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
package com.rapidminer.tools;

import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;

import javax.imageio.ImageIO;

/** Creates dynamic icons, based on the hostname. Currently, only a progress bar icon is available.
 *  It can be used by linking to the URL dynicon://progress/200/8/35 where 200 is the width of the progressbar (in px), 8 is the height of the progressbar (in px) and 35 is the progress (out of 100).
 * 
 * @author Thilo Kamradt, Simon Fischer
 *
 */
public class DynamicIconUrlStreamHandler extends URLStreamHandler {

	@Override
	protected URLConnection openConnection(final URL u) throws IOException {
		return new URLConnection(u) {
			@Override
			public InputStream getInputStream() throws IOException {
				BufferedImage img;
				String type = u.getHost();
				if ("progress".equals(type)) {
					String[] parameter= u.getFile().substring(1).split("/");
					int width = Integer.parseInt(parameter[0]);
					int height = Integer.parseInt(parameter[1]);
					double progress = Double.parseDouble(parameter[2]);
					img = new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);
					Graphics2D g = (Graphics2D) img.getGraphics();
					g.setColor(Color.WHITE);
					g.setPaint(new GradientPaint(0, 0, Color.WHITE.darker(), 0, (float) (height*0.5), Color.WHITE, true));
					g.fillRect(0, 0, width, height);
					g.setColor(Color.GREEN);
					g.setPaint(new GradientPaint(0, 0, Color.GREEN.darker().darker(), 0, (float) (height*0.5), Color.GREEN, true));
					g.fillRect(0, 0, (int) (progress * 200d / 100d), height);
					g.setColor(Color.BLACK);
					g.drawRect(0, 0, width - 1, height - 1);
					g.dispose();
				} else {
					throw new IOException("Unknown dynamic icon type: "+type);
				}
				try {
					ByteArrayOutputStream buffer = new ByteArrayOutputStream();
					ImageIO.write(img, "png", buffer);
					buffer.close();
					return new ByteArrayInputStream(buffer.toByteArray());
				} catch (IOException e) {
					e.printStackTrace();
					return null;
				}
			}
			@Override
			public void connect() throws IOException {
				// no-op
			}
		};
	}
}