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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import com.rapidminer.operator.IOContainer;
import com.rapidminer.operator.Operator;
import com.rapidminer.parameter.UndefinedParameterError;
import com.rapidminer.repository.RepositoryLocation;
import com.rapidminer.tools.I18N;
import com.rapidminer.tools.LogService;
import com.rapidminer.tools.ParameterService;
import com.rapidminer.tools.Tools;
import com.rapidminer.tools.container.Pair;
import com.rapidminer.tools.usagestats.OperatorStatisticsValue;
import com.rapidminer.tools.usagestats.UsageStatistics;


/**
 * Main command line program.
 * 
 * @author Ingo Mierswa, Simon Fischer
 */
public class RapidMinerCommandLine extends RapidMiner implements BreakpointListener {

    private static final String LICENSE = "RapidMiner version " + RapidMiner.getLongVersion() + ", Copyright (C) 2001-2012" + Tools.getLineSeparator() + "RapidMiner comes with ABSOLUTELY NO WARRANTY; This is free software," + Tools.getLineSeparator() + "and you are welcome to redistribute it under certain conditions;" + Tools.getLineSeparator() + "see license information in the file named LICENSE.";

    private String repositoryLocation = null;
    private boolean readFromFile = false;
    
    private List<Pair<String, String>> macros = new ArrayList<Pair<String,String>>();

    /**
     * This tread waits for pressing an arbitrary key. Used for resuming an
     * process if a breakpoint was reached in command line mode.
     */
    private static class WaitForKeyThread extends Thread {

        private final Process process;

        public WaitForKeyThread(Process process) {
            this.process = process;
        }

        @Override
        public void run() {
            try {
                System.in.read();
            } catch (IOException e) {
                //LogService.getRoot().log(Level.WARNING, "Error occured while waiting for user input: " + e.getMessage(), e);
    			LogService.getRoot().log(Level.WARNING,
    					I18N.getMessage(LogService.getRoot().getResourceBundle(), 
    					"com.rapidminer.RapidMinerCommandLine.waiting_for_user_input_error", 
    					e.getMessage()),
    					e);
            }
            process.resume();
        }
    }

    @Override
    public void breakpointReached(Process process, Operator operator, IOContainer container, int location) {
        System.out.println("Results in application " + operator.getApplyCount() + " of " + operator.getName() + ":" + Tools.getLineSeparator() + container);
        System.out.println("Breakpoint reached " + (location == BreakpointListener.BREAKPOINT_BEFORE ? "before " : "after ") + operator.getName() + ", press enter...");
        new WaitForKeyThread(process).start(); // must be extra thread to
        // ensure that wait is invoked
        // before notify...
    }

    /** Does nothing. */
    @Override
    public void resume() {}

    /** Parses the commandline arguments. */
    private void parseArguments(String[] argv) {
        repositoryLocation = null;

        for (String element : argv) {
        	if (element==null) continue;
            if ("-f".equals(element)) {
                readFromFile = true;
            } else if (element.startsWith("-M")) {
                element = element.substring(2);
            	String[] split = element.split("=");
                macros.add(new Pair<String, String>(split[0], split[1]));
            } else if (repositoryLocation==null){
                repositoryLocation = element;
            }
        }

        if (repositoryLocation == null) {
            printUsage();
        }
    }

    private static void printUsage() {
        System.err.println("Usage: " + RapidMinerCommandLine.class.getName() + " [-f] PROCESS [-Mname=value]\n"+
                "  PROCESS       a repository location containing a process\n"+
        "  -f            interpret PROCESS as a file rather than a repository location (deprecated)\n"+
        "  -Mname=value  sets the macro 'name' with the value 'value'");
        System.exit(1);
    }

    private void run() {
        ParameterService.ensureRapidMinerHomeSet();

        // init rapidminer
        RapidMiner.init();

        Process process = null;
        try {
            if (readFromFile) {
                process = RapidMiner.readProcessFile(new File(repositoryLocation));
            } else {
                RepositoryProcessLocation loc = new RepositoryProcessLocation(new RepositoryLocation(repositoryLocation));
                process = loc.load(null);
            }
        } catch (Exception e) {
            //LogService.getRoot().severe("Cannot read process setup '" + repositoryLocation + "': "+e.getMessage());
			LogService.getRoot().log(Level.WARNING,
					I18N.getMessage(LogService.getRoot().getResourceBundle(), 
					"com.rapidminer.RapidMinerCommandLine.reading_process_setup_error", 
					repositoryLocation, e.getMessage()),
					e);
            RapidMiner.quit(RapidMiner.ExitMode.ERROR);
        }

        if (process != null) {
            try {
            	for(Pair<String, String> macro : macros) {
                    process.getContext().addMacro(macro);
                }
                process.addBreakpointListener(this);
                IOContainer results = process.run();
                process.getRootOperator().sendEmail(results, null);
                //LogService.getRoot().info("Process finished successfully");
                LogService.getRoot().log(Level.INFO, "com.rapidminer.RapidMinerCommandLine.process_finished");
                RapidMiner.quit(RapidMiner.ExitMode.NORMAL);
            } catch (Throwable e) {
                UsageStatistics.getInstance().count(process.getCurrentOperator(), OperatorStatisticsValue.FAILURE);
                UsageStatistics.getInstance().count(process.getCurrentOperator(), OperatorStatisticsValue.RUNTIME_EXCEPTION);
                String debugProperty = ParameterService.getParameterValue(PROPERTY_RAPIDMINER_GENERAL_DEBUGMODE);
                boolean debugMode = Tools.booleanValue(debugProperty, false);
                String message = e.getMessage();
                if (!debugMode) {
                    if (e instanceof RuntimeException) {
                        if (e.getMessage() != null)
                            message = "operator cannot be executed (" + e.getMessage() + "). Check the log messages...";
                        else
                            message = "operator cannot be executed. Check the log messages...";
                    }
                }
                process.getLogger().log(Level.SEVERE, "Process failed: " + message, e);
                process.getLogger().log(Level.SEVERE, "Here: "+process.getRootOperator().createMarkedProcessTree(10, "==>", process.getCurrentOperator()));
                try {
                    process.getRootOperator().sendEmail(null, e);
                } catch (UndefinedParameterError ex) {
                    // cannot happen
                }
                LogService.getRoot().severe("Process not successful");
                RapidMiner.quit(RapidMiner.ExitMode.ERROR);
            }
        }
    }

    public static void main(String argv[]) {
        setExecutionMode(ExecutionMode.COMMAND_LINE);
        System.out.println(LICENSE);
        RapidMinerCommandLine main = new RapidMinerCommandLine();
        main.parseArguments(argv);
        main.run();
    }

}
