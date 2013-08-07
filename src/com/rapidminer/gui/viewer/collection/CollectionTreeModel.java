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

import javax.swing.event.TreeModelListener;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;

import com.rapidminer.operator.GroupedModel;
import com.rapidminer.operator.IOObject;
import com.rapidminer.operator.IOObjectCollection;
import com.rapidminer.operator.learner.meta.MetaModel;

/** Tree model backed by an {@link IOObjectCollection}.
 *  
 *  @author Simon Fischer
 */
public class CollectionTreeModel implements TreeModel {

	private final IOObject root;
	
	public CollectionTreeModel(IOObject root) {
		this.root = root;
	}

	@Override
	public void addTreeModelListener(TreeModelListener l) {
		// tree is immutable, no listeners
	}

	@Override
	public void removeTreeModelListener(TreeModelListener l) {
		// tree is immutable, no listeners
	}

	@Override
	public void valueForPathChanged(TreePath path, Object newValue) {
		throw new UnsupportedOperationException("Tree is immutable.");		
	}

	@SuppressWarnings("unchecked")
	@Override
	public Object getChild(Object parent, int index) {
		if (parent instanceof IOObjectCollection) {
			IOObjectCollection<IOObject> col = (IOObjectCollection<IOObject>) parent;
			return col.getElement(index, false);
		} else if (parent instanceof GroupedModel) {
			return ((GroupedModel)parent).getModel(index);
		} else if (parent instanceof MetaModel) {
			return ((MetaModel)parent).getModels().get(index);
		} else {
			return null;
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public int getChildCount(Object parent) {
		if (parent instanceof IOObjectCollection) {
			IOObjectCollection<IOObject> col = (IOObjectCollection<IOObject>) parent;
			return col.size();
		} else if (parent instanceof GroupedModel) {
			return ((GroupedModel)parent).getNumberOfModels();
		} else if (parent instanceof MetaModel) {
			return ((MetaModel)parent).getModels().size();
		} else {
			return 0;
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public int getIndexOfChild(Object parent, Object child) {
		if (parent instanceof IOObjectCollection) {
			IOObjectCollection<IOObject> col = (IOObjectCollection<IOObject>) parent;
			return col.getObjects().indexOf(child);
		} else if (parent instanceof GroupedModel) {
			return ((GroupedModel)parent).getModels().indexOf(child);
		} else if (parent instanceof MetaModel) {
			return ((MetaModel)parent).getModels().indexOf(child);
		} else {
			return -1;
		}
	}

	@Override
	public Object getRoot() {
		return root;
	}

	@Override
	public boolean isLeaf(Object node) {
		return !((node instanceof IOObjectCollection) || (node instanceof MetaModel) || (node instanceof GroupedModel));
	}
}
