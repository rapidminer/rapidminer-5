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
package com.rapidminer;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Observable;

/**
 * This class can be used to store macros for an process which can be defined
 * by the operator {@link com.rapidminer.operator.MacroDefinitionOperator}. It also
 * defines some standard macros like the process path or file name.
 * 
 * @author Ingo Mierswa
 */
public class MacroHandler extends Observable {

    // TODO: remove experiment macros later
    private static final String[] PREDEFINED_MACROS = { "experiment_name", "experiment_file", "experiment_path", "process_name", "process_file", "process_path" };

    // TODO: remove experiment constants later
    private static final int EXPERIMENT_NAME = 0;
    private static final int EXPERIMENT_FILE = 1;
    private static final int EXPERIMENT_PATH = 2;
    private static final int PROCESS_NAME = 3;
    private static final int PROCESS_FILE = 4;
    private static final int PROCESS_PATH = 5;

    private final Process process;

    private final Map<String, String> macroMap = new HashMap<String, String>();

    public MacroHandler(Process process) {
        this.process = process;
    }

    public void clear() {
    	setChanged();
        this.macroMap.clear();        
        notifyObservers(this);
    }

    public Iterator<String> getDefinedMacroNames() {
        return macroMap.keySet().iterator();
    }
    
    /**
     * Adds a macro to this MacroHandler. If a macro with this
     * name is already present, it will be overwritten.
     * @param macro The name of the macro.
     * @param value The new value of the macro.
     */
    public void addMacro(String macro, String value) {
    	if (macro != null && !macro.isEmpty()){
	    	setChanged();
	        this.macroMap.put(macro, value);
	        notifyObservers(this);
    	}
    }

    public void removeMacro(String macro) {
    	setChanged();
        this.macroMap.remove(macro);
        notifyObservers(this);
    }

    public String getMacro(String macro) {
        for (int i = 0; i < PREDEFINED_MACROS.length; i++) {
            if (PREDEFINED_MACROS[i].equals(macro)) {
                switch (i) {
                case EXPERIMENT_NAME:
                case PROCESS_NAME:
                    ProcessLocation processLocation = process.getProcessLocation();
                    if (processLocation instanceof FileProcessLocation) {
                        return processLocation.getShortName().substring(0, processLocation.getShortName().lastIndexOf("."));
                    }
                    return processLocation.getShortName();
                case EXPERIMENT_FILE:
                case PROCESS_FILE:
                    return process.getProcessLocation().getShortName();
                case EXPERIMENT_PATH:
                case PROCESS_PATH:
                    return process.getProcessLocation().toString();
                }
            }
        }
        return this.macroMap.get(macro);
    }

    @Override
    public String toString() {
        return this.macroMap.toString();
    }
}
