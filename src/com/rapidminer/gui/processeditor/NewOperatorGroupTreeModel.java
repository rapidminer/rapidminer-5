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
package com.rapidminer.gui.processeditor;

import java.io.Serializable;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;

import com.rapidminer.RapidMiner;
import com.rapidminer.gui.tools.CamelCaseFilter;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.ProcessRootOperator;
import com.rapidminer.tools.GroupTree;
import com.rapidminer.tools.OperatorService;
import com.rapidminer.tools.OperatorService.OperatorServiceListener;
import com.rapidminer.tools.documentation.OperatorDocBundle;
import com.rapidminer.tools.usagestats.OperatorStatisticsValue;
import com.rapidminer.tools.usagestats.OperatorUsageStatistics;
import com.rapidminer.tools.usagestats.UsageStatistics;
import com.rapidminer.tools.usagestats.UsageStatistics.StatisticsScope;


/**
 * This is the model for the group selection tree in the new operator editor panel.
 * 
 * @author Ingo Mierswa, Tobias Malbrecht, Sebastian Land
 */
public class NewOperatorGroupTreeModel implements TreeModel, OperatorServiceListener {

    /** Compares operator descriptions based on their usage statistics. */
    private static final class UsageStatsComparator implements Comparator<OperatorDescription>, Serializable {
        private static final long serialVersionUID = 1L;

        @Override
        public int compare(OperatorDescription op1, OperatorDescription op2) {
            OperatorUsageStatistics operatorStatistics1 = UsageStatistics.getInstance().getOperatorStatistics(StatisticsScope.ALL_TIME, op1);
            int usageCount1 = (operatorStatistics1 == null) ? 0 : operatorStatistics1.getStatistics(OperatorStatisticsValue.EXECUTION);

            OperatorUsageStatistics operatorStatistics2 = UsageStatistics.getInstance().getOperatorStatistics(StatisticsScope.ALL_TIME, op2);
            int usageCount2 = (operatorStatistics2 == null) ? 0 : operatorStatistics2.getStatistics(OperatorStatisticsValue.EXECUTION);

            return usageCount2 - usageCount1;
        }
    }

    private final GroupTree completeTree;

    private GroupTree displayedTree;

    private boolean filterDeprecated = true;

    private String filter = null;

    /** The list of all tree model listeners. */
    private final List<TreeModelListener> treeModelListeners = new LinkedList<TreeModelListener>();

    private boolean sortByUsage = false;

    public NewOperatorGroupTreeModel() {
        this.completeTree = OperatorService.getGroups();
        OperatorService.addOperatorServiceListener(this);

        removeHidden(completeTree);

        this.displayedTree = this.completeTree;
        this.filterDeprecated = true;
        updateTree();
    }

    public void setFilterDeprecated(boolean filterDeprecated) {
        this.filterDeprecated = filterDeprecated;
        updateTree();
    }

    @Override
    public void addTreeModelListener(TreeModelListener l) {
        treeModelListeners.add(l);
    }

    public boolean contains(Object o) {
        return contains(this.getRoot(), o);
    }

    private boolean contains(Object start, Object o) {
        if (o.equals(start)) {
            return true;
        } else {
            for (int i = 0; i < getChildCount(start); i++) {
                if (contains(getChild(start, i), o)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public Object getChild(Object parent, int index) {
        if (parent instanceof GroupTree) {
            GroupTree tree = (GroupTree) parent;
            int numSubGroups = tree.getSubGroups().size();
            if (index < numSubGroups) {
                return tree.getSubGroup(index);
            } else {
                return tree.getOperatorDescriptions().get(index - numSubGroups);
            }
        } else {
            return null;
        }

    }

    @Override
    public int getChildCount(Object parent) {
        if (parent instanceof GroupTree) {
            GroupTree tree = (GroupTree)parent;
            return tree.getSubGroups().size() + tree.getOperatorDescriptions().size();
        } else {
            return 0;
        }
    }

    @Override
    public int getIndexOfChild(Object parent, Object child) {
        GroupTree tree = (GroupTree)parent;
        if (child instanceof GroupTree) {
            return tree.getIndexOfSubGroup((GroupTree)child);
        } else {
            return tree.getOperatorDescriptions().indexOf(child) + tree.getSubGroups().size();
        }
    }

    @Override
    public Object getRoot() {
        return displayedTree;
    }

    @Override
    public boolean isLeaf(Object node) {
        return !(node instanceof GroupTree);
    }

    @Override
    public void removeTreeModelListener(TreeModelListener l) {
        treeModelListeners.remove(l);
    }

    /** Will be invoked after editing changes of nodes. */
    @Override
    public void valueForPathChanged(TreePath path, Object node) {
        fireTreeChanged(node, path);
    }

    private void fireTreeChanged(Object source, TreePath path) {
        Iterator i = treeModelListeners.iterator();
        while (i.hasNext()) {
            ((TreeModelListener) i.next()).treeStructureChanged(new TreeModelEvent(source, path));
        }
    }

    private void fireCompleteTreeChanged(Object source) {
        Iterator i = treeModelListeners.iterator();
        while (i.hasNext()) {
            ((TreeModelListener) i.next()).treeStructureChanged(new TreeModelEvent(this, new TreePath(getRoot())));
        }
    }

    public int applyFilter(String filter) {
        this.filter = filter;
        return updateTree();
    }

    public int updateTree() {
        int hits = Integer.MAX_VALUE;
        GroupTree filteredTree = this.completeTree.clone();
        if (!"true".equals(System.getProperty(RapidMiner.PROPERTY_DEVELOPER_MODE))) {
            removeDeprecatedGroup(filteredTree);
        }
        if (filter != null && filter.trim().length() > 0) {
            CamelCaseFilter ccFilter = new CamelCaseFilter(filter);
            hits = removeFilteredInstances(ccFilter, filteredTree);
        }
        if (filterDeprecated) {
            hits = removeDeprecated(filteredTree);
        }
        this.displayedTree = filteredTree;
        if (sortByUsage) {
            filteredTree.sort(new UsageStatsComparator());
        }
        fireCompleteTreeChanged(this);
        return hits;
    }


    public GroupTree getNonDeprecatedGroupTree(GroupTree tree) {
        GroupTree filteredTree = tree.clone();
        removeDeprecated(filteredTree);
        return filteredTree;
    }

    private void removeHidden(GroupTree tree) {
        Iterator<? extends GroupTree> g = tree.getSubGroups().iterator();
        while (g.hasNext()) {
            GroupTree child = g.next();
            removeHidden(child);
            if (child.getAllOperatorDescriptions().size() == 0) {
                g.remove();
            }
        }
        Iterator<OperatorDescription> o = tree.getOperatorDescriptions().iterator();
        while (o.hasNext()) {
            OperatorDescription description = o.next();
            if (description.getOperatorClass().equals(ProcessRootOperator.class)) {
                o.remove();
            }
        }
    }

    private void removeDeprecatedGroup(GroupTree tree) {
        Iterator<? extends GroupTree> g = tree.getSubGroups().iterator();
        while (g.hasNext()) {
            GroupTree child = g.next();
            if (child.getKey().equals("deprecated")) {
                g.remove();
            } else {
                removeDeprecatedGroup(child);
            }
        }
    }

    private int removeDeprecated(GroupTree tree) {
        int hits = 0;
        Iterator<? extends GroupTree> g = tree.getSubGroups().iterator();
        while (g.hasNext()) {
            GroupTree child = g.next();
            hits += removeDeprecated(child);
            if (child.getAllOperatorDescriptions().size() == 0) {
                g.remove();
            }
        }
        Iterator<OperatorDescription> o = tree.getOperatorDescriptions().iterator();
        while (o.hasNext()) {
            OperatorDescription description = o.next();
            if (description.isDeprecated()) {
                o.remove();
            } else {
                hits++;
            }
        }
        return hits;
    }

    private int removeFilteredInstances(CamelCaseFilter filter, GroupTree filteredTree) {
        int hits = 0;
        Iterator<? extends GroupTree> g = filteredTree.getSubGroups().iterator();
        while (g.hasNext()) {
            GroupTree child = g.next();
            boolean matches = filter.matches(child.getName());
            if (!matches) {
                hits += removeFilteredInstances(filter, child);
                if (child.getAllOperatorDescriptions().size() == 0) {
                    g.remove();
                }
            }
        }

        // remove non matching operator descriptions if the group does not match, keep all in matching group
        boolean groupMatches = filter.matches(filteredTree.getName());
        if (!groupMatches) {
            Iterator<OperatorDescription> o = filteredTree.getOperatorDescriptions().iterator();
            while (o.hasNext()) {
                OperatorDescription description = o.next();
                boolean matches = filter.matches(description.getName()) || filter.matches(description.getShortName());
                if (!filterDeprecated) {
                    for (String replaces : description.getReplacedKeys()) {
                        matches |= filter.matches(replaces);
                    }
                }
                if (!matches) {
                    o.remove();
                } else {
                    hits++;
                }
            }
        }
        return hits;
    }

    public void setSortByUsage(boolean sort) {
        if (sort != this.sortByUsage ) {
            this.sortByUsage = sort;
            updateTree();
        }
    }

    @Override
    public void operatorRegistered(OperatorDescription description, OperatorDocBundle bundle) {
        updateTree();
    }

    @Override
    public void operatorUnregistered(OperatorDescription description) {
        updateTree();
    }
}
