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

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import com.rapidminer.RapidMiner;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorCapability;
import com.rapidminer.operator.OperatorCreationException;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.learner.Learner;
import com.rapidminer.tools.GroupTree;
import com.rapidminer.tools.OperatorService;

/**
 * 
 * @author Ingo Mierswa
 */
public class OperatorListCreator {

    public static void main(String[] argv) throws IOException {
        RapidMiner.init();
        
        GroupTree tree = OperatorService.getGroups();
        PrintWriter out = new PrintWriter(new FileWriter(argv[0]));
        printGroup(out, "", tree);
        //printMainGroups(out, tree);
        out.close();
    }

    public static void printMainGroups(PrintWriter out, GroupTree tree) {
        Collection<OperatorDescription> descriptions = tree.getOperatorDescriptions();
        List<OperatorDescription> descriptionsList = new LinkedList<OperatorDescription>(descriptions);
        Collections.sort(descriptionsList);
        for (OperatorDescription description : descriptionsList) {
            out.println(description.getName());
        }

        List<GroupTree> subgroups = new LinkedList<GroupTree>(tree.getSubGroups());
        Collections.sort(subgroups);
        for (GroupTree subtree : subgroups) {
            out.println("--------------------------");
            out.println(subtree.getName());
            out.println("--------------------------");

            descriptions = subtree.getAllOperatorDescriptions();
            descriptionsList = new LinkedList<OperatorDescription>(descriptions);
            Collections.sort(descriptionsList);
            for (OperatorDescription description : descriptionsList) {
                out.println(description.getName());
            }
        }
    }
    
    public static void printGroup(PrintWriter out, String parentName, GroupTree tree) {
        out.println("--------------------------");
        if (parentName.length() > 0)
            out.println(parentName + "." + tree.getName());
        else
            out.println(tree.getName());
        out.println("--------------------------");
        Collection<OperatorDescription> descriptions = tree.getOperatorDescriptions();
        List<OperatorDescription> descriptionsList = new LinkedList<OperatorDescription>(descriptions);
        Collections.sort(descriptionsList);
        for (OperatorDescription description : descriptionsList) {
            out.print(description.getName());
            Operator op = null;
            try {
                op = description.createOperatorInstance();
            } catch (OperatorCreationException e1) {
                e1.printStackTrace();
            }

            if (op != null) {
                if (op instanceof Learner) {
                    Learner learner = (Learner) op;
                    StringBuffer learnerCapabilities = new StringBuffer();                    
                    boolean first = true;
                    for (OperatorCapability capability : OperatorCapability.values()) {                         
                        try {
                            if (learner.supportsCapability(capability)) {
                                if (!first)
                                    learnerCapabilities.append(", ");
                                learnerCapabilities.append(capability.getDescription());
                                first = false;
                            }
                        } catch (Exception e) {
                            break;
                        }
                    }
                    String result = learnerCapabilities.toString();
                    if (result.length() > 0) {
                        out.print("  [" + result + "]");
                    }
                }
            }
            out.println();
        }
        
        List<GroupTree> subgroups = new LinkedList<GroupTree>(tree.getSubGroups());
        Collections.sort(subgroups);
        for (GroupTree subtree : subgroups) {
            if (parentName.length() > 0)
                printGroup(out, parentName + "." + tree.getName(), subtree);
            else
                printGroup(out, tree.getName(), subtree);
        }
    }
}
