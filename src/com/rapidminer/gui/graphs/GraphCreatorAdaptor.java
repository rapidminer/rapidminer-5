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
package com.rapidminer.gui.graphs;

import java.awt.Paint;

import javax.swing.JComponent;

import org.apache.commons.collections15.Transformer;

import edu.uci.ics.jung.visualization.VisualizationViewer;
import edu.uci.ics.jung.visualization.renderers.Renderer.EdgeLabel;
import edu.uci.ics.jung.visualization.renderers.Renderer.Vertex;
import edu.uci.ics.jung.visualization.renderers.Renderer.VertexLabel;

/**
 * An adaptor for the graph creator interface. Subclasses might want to override
 * some of the implemented methods and have to define the {@link #createGraph()}
 * method.
 * 
 * @author Ingo Mierswa
 */
public abstract class GraphCreatorAdaptor implements GraphCreator<String,String> {

    /** Returns null. */
    public EdgeLabel<String, String> getEdgeLabelRenderer() {
        return null;
    }

    /** Returns null. */
    public String getEdgeName(String id) {
        return null;
    }
    
    /** Returns 1. */
	public double getEdgeStrength(String id) {
		return 1.0d;
	}
	
	/** Returns the shape of the edges. */
	public int getEdgeShape() {
		return EDGE_SHAPE_LINE;
	}

    /** Returns -1. */
    public int getLabelOffset() {
        return -1;
    }

    /** Returns -1. */
    public int getMinLeafHeight() {
        return -1;
    }

    /** Returns -1. */
    public int getMinLeafWidth() {
        return -1;
    }

    /** Returns 0. */
    public int getNumberOfOptionComponents() {
        return 0;
    }

    /** Returns null. */
    public Object getObject(String id) {
        return null;
    }

    /** Returns null. */
    public GraphObjectViewer getObjectViewer() {
        return null;
    }

    /** Returns null. */
    public JComponent getOptionComponent(GraphViewer viewer, int index) {
        return null;
    }

    /** Returns null. */
    public VertexLabel<String, String> getVertexLabelRenderer() {
        return null;
    }

    /** Returns null. */
    public String getVertexName(String id) {
        return null;
    }

    /** Returns null. */
    public Transformer<String, Paint> getVertexPaintTransformer(VisualizationViewer<String,String> viewer) {
    	return null;
    }
    
    /** Returns null. */
    public Vertex<String, String> getVertexRenderer() {
        return null;
    }

    /** Returns null. */
    public String getVertexToolTip(String id) {
        return null;
    }

    /** Returns false. */
    public boolean isBold(String id) {
        return false;
    }

    /** Returns false. */
    public boolean isEdgeLabelDecorating() {
        return false;
    }

    /** Returns false. */
    public boolean isLeaf(String id) {
        return false;
    }

    /** Returns true. */
    public boolean isRotatingEdgeLabels() {
        return true;
    }
    
    /** Returns true. */
    public boolean showEdgeLabelsDefault() {
        return true;
    }
    
    /** Returns true. */
    public boolean showVertexLabelsDefault() {
        return true;
    }
}
