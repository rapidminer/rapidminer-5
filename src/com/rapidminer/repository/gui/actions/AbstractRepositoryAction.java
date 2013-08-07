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
package com.rapidminer.repository.gui.actions;

import java.awt.event.ActionEvent;

import com.rapidminer.gui.tools.ResourceAction;
import com.rapidminer.repository.Entry;
import com.rapidminer.repository.gui.RepositoryTree;

/**
 * Abstract superclass of actions that are executed on subclasses of {@link Entry}.
 * Automatically enables/disables itself.
 * 
 *  @author Simon Fischer
 */
public abstract class AbstractRepositoryAction<T extends Entry> extends ResourceAction {

	/** the tree to which the action belongs to */
	protected final RepositoryTree tree;
	
	/** the required selection type for the action to show/enable */
	private final Class<T> requiredSelectionType;
	
	/** if write access to the repository is needed for this action */
	private final boolean needsWriteAccess;
	
	private static final long serialVersionUID = -6415235351430454776L;
	
	
	public AbstractRepositoryAction(RepositoryTree tree, Class<T> requiredSelectionType, boolean needsWriteAccess, String i18nKey) {
		super(true, i18nKey);			
		this.tree = tree;
		this.requiredSelectionType = requiredSelectionType;
		this.needsWriteAccess = needsWriteAccess;
		setEnabled(false);
	}
	
	@Override
	protected void update(boolean[] conditions) {
		// we have our own mechanism to enable/disable actions,
		// so ignore ConditionalAction mechanism
	}
	
	public void enable() {
		Entry entry = tree.getSelectedEntry();
		setEnabled((entry != null) && requiredSelectionType.isInstance(entry) && (!needsWriteAccess || !entry.isReadOnly()));
	}

	public void actionPerformed(ActionEvent e) {
		actionPerformed(requiredSelectionType.cast(tree.getSelectedEntry()));
	}
	
	public abstract void actionPerformed(T cast);
}
