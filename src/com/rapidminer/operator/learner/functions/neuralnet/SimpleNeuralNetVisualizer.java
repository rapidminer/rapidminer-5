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
package com.rapidminer.operator.learner.functions.neuralnet;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.util.Iterator;
import java.util.List;

import javax.swing.JPanel;

import org.encog.matrix.Matrix;
import org.encog.neural.networks.BasicNetwork;
import org.encog.neural.networks.Layer;

import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.report.Renderable;
import com.rapidminer.tools.Tools;


/**
 * Visualizes a simple neural net. The nodes can be selected by clicking. The next tool tip will then 
 * show the input weights for the selected node.
 * 
 * @author Ingo Mierswa
 */
public class SimpleNeuralNetVisualizer extends JPanel implements MouseListener, Renderable {
	
	private static final long serialVersionUID = 1511167115976161350L;

	private static final int ROW_HEIGHT = 36;
	
	private static final int LAYER_WIDTH = 150;
	
	private static final int MARGIN = 30;
	
	private static final int NODE_RADIUS = 24;
	
	private static final Font LABEL_FONT = new Font("SansSerif", Font.PLAIN, 11);
		
	private BasicNetwork neuralNet;
	
	private int selectedLayerIndex = -1;
	
	private int selectedRowIndex = -1;
	
	private double maxAbsoluteWeight = Double.NEGATIVE_INFINITY;

    private String key = null;
    
    private int keyX = -1;
    
    private int keyY = -1;
    
    private String[] attributeNames;
    
    public SimpleNeuralNetVisualizer(SimpleNeuralNetModel neuralNetModel) {
    	this(neuralNetModel.getNeuralNet(), neuralNetModel.getAttributeNames());
    }
    
	public SimpleNeuralNetVisualizer(BasicNetwork neuralNet, String[] attributeNames) {
		this.neuralNet = neuralNet;
		this.attributeNames = attributeNames;
		addMouseListener(this);
        
        // calculate maximal absolute weight
        this.maxAbsoluteWeight = Double.NEGATIVE_INFINITY;
        List<Layer> layers = this.neuralNet.getLayers();
        Iterator i = layers.iterator();
        while (i.hasNext()) {
            Layer layer = (Layer)i.next();
            if (layer.hasMatrix()) {
            	Matrix matrix = layer.getMatrix();
        		int rows = matrix.getRows();
        		int cols = matrix.getCols();
        		for (int c = 0; c < cols; c++) {
        			for (int r = 0; r < rows; r++) {
        				double weight = matrix.get(r, c);
        				this.maxAbsoluteWeight = Math.max(this.maxAbsoluteWeight, Math.abs(weight));
        			}
        		}
            }
        }
	}
	
	@Override
	public Dimension getPreferredSize() {
		List<Layer> layers = this.neuralNet.getLayers();
		Iterator i = layers.iterator();
		int maxRows = -1;
		int layerIndex = 0;
		while (i.hasNext()) {
			Layer layer = (Layer)i.next();
			int rows = layer.getNeuronCount();
			if (layerIndex < layers.size() - 1)
				rows++;
			maxRows = Math.max(maxRows, rows);
			layerIndex++;
		}
		return new Dimension(layers.size() * LAYER_WIDTH + 2 * MARGIN, maxRows * ROW_HEIGHT + 2 * MARGIN);
	}
	
	@Override
	public void paint(Graphics graphics) {
		graphics.clearRect(0, 0, getWidth(), getHeight());
        graphics.setColor(Color.WHITE);
        graphics.fillRect(0,0,getWidth(),getHeight());
        
		Dimension dim = getPreferredSize();
		int height = dim.height;
		
		Graphics2D g = (Graphics2D)graphics;
		Graphics2D translated = (Graphics2D)g.create();
		translated.translate(MARGIN, MARGIN);
		translated.setFont(LABEL_FONT);
		
		Graphics2D synapsesG = (Graphics2D)translated.create();
		paintSynapses(synapsesG, height);
		synapsesG.dispose();

		Graphics2D nodeG = (Graphics2D)translated.create();
		paintNodes(nodeG, height);
		nodeG.dispose();
		
		translated.dispose();
        
        // key
        if (key != null) {
            // line.separator does not work for split, transform and use \n
            key = Tools.transformAllLineSeparators(key);
            String[] lines = key.split("\n");
            double maxWidth = Double.NEGATIVE_INFINITY;
            double totalHeight = 0.0d;
            for (String line : lines) {
                Rectangle2D keyBounds = g.getFontMetrics().getStringBounds(line, g);
                maxWidth = Math.max(maxWidth, keyBounds.getWidth());
                totalHeight += keyBounds.getHeight();
            }
            totalHeight += (lines.length - 1) * 3;
            
            Rectangle frame = new Rectangle(keyX - 4, keyY, (int)maxWidth + 8, (int)totalHeight + 6);
            g.setColor(SwingTools.LIGHTEST_YELLOW);
            g.fill(frame);
            g.setColor(SwingTools.DARK_YELLOW);
            g.draw(frame);
            g.setColor(Color.BLACK);
            int xPos = keyX;
            int yPos = keyY;
            for (String line : lines) {
                Rectangle2D keyBounds = g.getFontMetrics().getStringBounds(line, g);
                yPos += (int)keyBounds.getHeight();
                g.drawString(line, xPos, yPos);
                yPos += 3;
            }
        }
	}
	
	private void paintSynapses(Graphics2D g, int height) {
		List<Layer> layers = this.neuralNet.getLayers();
		Iterator i = layers.iterator();
		int layerIndex = 0;
		while (i.hasNext()) {
			Layer layer = (Layer)i.next();
			if (layer.hasMatrix()) {
				Matrix matrix = layer.getMatrix();				
				int inputRows  = matrix.getRows();
				int outputRows = matrix.getCols();
				int inputY  = (height / 2) - (inputRows  * ROW_HEIGHT / 2);
				int offset = layerIndex == layers.size() - 2 ? 0 : 1; // last layer no threshold --> second last is the current
				for (int x = 0; x < inputRows; x++) {
					int outputY = (height / 2) - ((outputRows + offset) * ROW_HEIGHT / 2);
					for (int y = 0; y < outputRows; y++) {
                        float weight = 1.0f - (float)(Math.abs(matrix.get(x, y)) / this.maxAbsoluteWeight);
                        Color color = new Color(weight, weight, weight);
                        g.setColor(color);
						g.drawLine(NODE_RADIUS / 2, inputY + NODE_RADIUS / 2, NODE_RADIUS / 2 + LAYER_WIDTH, outputY + NODE_RADIUS / 2);
						outputY += ROW_HEIGHT;
					}	
					inputY += ROW_HEIGHT;
				}
			}
			g.translate(LAYER_WIDTH, 0);
			layerIndex++;
		}
	}
	
	private void paintNodes(Graphics2D g, int height) {
		List<Layer> layers = this.neuralNet.getLayers();
		Iterator i = layers.iterator();
		int layerIndex = 0;
		while (i.hasNext()) {
			Layer layer = (Layer)i.next();
			int rows = layer.getNeuronCount();
			if (layerIndex < layers.size() - 1)
				rows++;
			String layerName = "Layer " + (layerIndex + 1);
			Rectangle2D stringBounds = LABEL_FONT.getStringBounds(layerName, g.getFontRenderContext());
			g.setColor(Color.BLACK);
			g.drawString(layerName, (int)(((-1)*stringBounds.getWidth() / 2) + NODE_RADIUS / 2), 0);
			int yPos = (height / 2) - (rows * ROW_HEIGHT / 2);
			for (int r = 0; r < rows; r++) {
				Shape node = new Ellipse2D.Double(0, yPos, NODE_RADIUS, NODE_RADIUS);
				if ((layerIndex == 0) || (layerIndex == layers.size() - 1)) {
					if ((r < rows - 1) || (layerIndex == layers.size() - 1)) {
						g.setPaint(SwingTools.makeYellowPaint(NODE_RADIUS, NODE_RADIUS));
					} else {
						g.setPaint(new Color(233, 233, 233));
					}
				} else {
					if (r < rows - 1) {
						g.setPaint(SwingTools.makeBluePaint(NODE_RADIUS, NODE_RADIUS));
					} else {
						g.setPaint(new Color(233, 233, 233));
					}
				}
				g.fill(node);
				if ((layerIndex == this.selectedLayerIndex) && (r == this.selectedRowIndex))
					g.setColor(Color.RED);
				else
					g.setColor(Color.BLACK);
				g.draw(node);
				yPos += ROW_HEIGHT;
			}
			g.translate(LAYER_WIDTH, 0);
			layerIndex++;
		}
	}

    private void setKey(String key, int keyX, int keyY) {
        this.key = key;
        this.keyX = keyX;
        this.keyY = keyY;
        repaint();
    }
    
	private void setSelectedNode(int layerIndex, int rowIndex, int xPos, int yPos) {
		this.selectedLayerIndex = layerIndex;
		this.selectedRowIndex = rowIndex;
		// set tool tip text
		if (layerIndex >= 1) {
			Layer layer = this.neuralNet.getLayers().get(selectedLayerIndex - 1);
			Layer currentLayer = this.neuralNet.getLayers().get(selectedLayerIndex);
			if (layer.hasMatrix()) {
				Matrix matrix = layer.getMatrix();
				// #rows --> input nodes
				// #columns --> output nodes
				int inputRows  = matrix.getRows();
				int outputCols  = inputRows;
				if (currentLayer.hasMatrix())
					outputCols = currentLayer.getMatrix().getRows();
				if ((rowIndex >= 0) && (rowIndex < outputCols - 1)) {
					StringBuffer toolTip = new StringBuffer("Weights:" + Tools.getLineSeparator());
					for (int x = 0; x < inputRows; x++) {
						toolTip.append(Tools.formatNumber(matrix.get(x, this.selectedRowIndex)) + Tools.getLineSeparator());
					}
					setKey(toolTip.toString(), xPos, yPos);
				} else {
					setKey("Threshold Node", xPos, yPos);
				}
			} else {
				setKey(null, -1, -1);
			}
		} else {
			if ((rowIndex >= 0) && (rowIndex < this.attributeNames.length)) {
				setKey(this.attributeNames[rowIndex], xPos, yPos);
			} else {
				if (rowIndex == this.attributeNames.length) {
					setKey("Threshold Node", xPos, yPos);
				} else {
					setKey(null, -1, -1);
				}
			}
		}
		repaint();
	}
	
	private void checkMousePos(int xPos, int yPos) {
		int x = xPos - MARGIN;
		int y = yPos - MARGIN;
		int layerIndex = x / LAYER_WIDTH;
		int layerMod = x % LAYER_WIDTH;
		boolean layerHit = ((layerMod > 0) && (layerMod < NODE_RADIUS));
		if ((layerHit) && (layerIndex >= 0) && (layerIndex < this.neuralNet.getLayers().size())) {
			Layer layer = this.neuralNet.getLayers().get(layerIndex);
			int rows = layer.getNeuronCount();
			if (layerIndex < neuralNet.getLayers().size() - 1)
				rows++;
			int yMargin = (getPreferredSize().height / 2) - (rows * ROW_HEIGHT / 2);
			if (y > yMargin) {
				for (int i = 0; i < rows; i++) {
					if ((y > yMargin) && (y < yMargin + NODE_RADIUS)) {
						if ((this.selectedLayerIndex == layerIndex) && (this.selectedRowIndex == i)) {
							setSelectedNode(-1, -1, -1, -1);
						} else {
							setSelectedNode(layerIndex, i, xPos, yPos);
						}
						return;
					}
					yMargin += ROW_HEIGHT;
				}
			}
		}
		setSelectedNode(-1, -1, -1, -1);
	}
	
	public void mouseClicked(MouseEvent e) {}
	public void mouseEntered(MouseEvent e) {}
	public void mouseExited(MouseEvent e) {}
	public void mousePressed(MouseEvent e) {}
	
	public void mouseReleased(MouseEvent e) {
		int xPos = e.getX();
		int yPos = e.getY();
		checkMousePos(xPos, yPos);
	}

    public void prepareRendering() {}
    
    public void finishRendering() {}
    
	public int getRenderHeight(int preferredHeight) {
		int height = getPreferredSize().height;
		if (height < 1) {
			height = preferredHeight;
		}
		if (preferredHeight > height) {
			height = preferredHeight;
		}
		return height;
	}

	public int getRenderWidth(int preferredWidth) {
		int width = getPreferredSize().width;
		if (width < 1) {
			width = preferredWidth;
		}
		if (preferredWidth > width) {
			width = preferredWidth;
		}
		return width;
	}

	public void render(Graphics graphics, int width, int height) {
		setSize(width, height);
		paint(graphics);
	}
}
