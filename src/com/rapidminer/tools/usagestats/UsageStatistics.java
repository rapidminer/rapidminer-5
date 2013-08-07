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
package com.rapidminer.tools.usagestats;

import java.io.File;
import java.net.URL;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.Date;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.logging.Level;

import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.ws.BindingProvider;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.rapid_i.rapidhome.wsimport.RapidHome;
import com.rapid_i.rapidhome.wsimport.RapidHomeService;
import com.rapid_i.rapidhome.wsimport.StatisticsRecord;
import com.rapid_i.rapidhome.wsimport.StatisticsReport;
import com.rapidminer.Process;
import com.rapidminer.RapidMiner;
import com.rapidminer.datatable.DataTable;
import com.rapidminer.io.process.XMLTools;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.tools.FileSystemService;
import com.rapidminer.tools.I18N;
import com.rapidminer.tools.LogService;
import com.rapidminer.tools.ProgressListener;
import com.rapidminer.tools.WebServiceTools;

/** Collects statistics about usage of operators.
 *  Statistics can be sent to a server collecting them.
 *  Counting and resetting is thread safe.
 * 
 * @see UsageStatsTransmissionDialog
 * 
 * @author Simon Fischer
 *
 */
public class UsageStatistics {

    /** URL to send the statistics values to. */
    private static final String RAPIDMINER_HOME_URLS[] = {
        //"http://localhost:8080/RapidHome/RapidHomeService?wsdl",
        "http://rapid1.de:80/RapidHome/RapidHomeService?wsdl",
        "http://rapid21.de:80/RapidHome/RapidHomeService?wsdl"
    };
    //private static final String RAPIDMINER_HOME_URL = "http://192.168.1.3:8080/RapidHome/RapidHomeService?wsdl";
    //URL url = new URL("http://localhost:8080/RapidHome/RapidHomeService?wsdl");

    private static final long TRANSMISSION_INTERVAL = 1000 * 60 * 60 * 24 * 14;
    //private static final long TRANSMISSION_INTERVAL = 1000 * 20;

    /** Selects with which scope the statistics are collected and reported. */
    public static enum StatisticsScope {
        /** Since the last reset. */
        CURRENT("current"),

        /** Since RapidMiner was installed. */
        ALL_TIME("allTime");

        private String xmlTag;

        private StatisticsScope(String xmlTag) {
            this.xmlTag = xmlTag;
        }

        /** Tag to put around statistic map when exported as XML. */
        protected String getXMLTag() {
            return xmlTag;
        }
    }

    private final EnumMap<StatisticsScope,Map<String,OperatorUsageStatistics>> statsMaps = new EnumMap<StatisticsScope,Map<String,OperatorUsageStatistics>>(StatisticsScope.class);
    private Date lastReset;
    private Date nextTransmission;

    private static final UsageStatistics INSTANCE = new UsageStatistics();

    private String randomKey;

    private transient boolean failedToday = false;

    public static UsageStatistics getInstance() {
        return INSTANCE;
    }

    private UsageStatistics() {
        for (StatisticsScope t : StatisticsScope.values()) {
            statsMaps.put(t, new HashMap<String,OperatorUsageStatistics>());
        }
        load();
    }

    /** Loads the statistics from the user file. */
    private void load() {
        if (!RapidMiner.getExecutionMode().canAccessFilesystem()) {
            //LogService.getRoot().config("Cannot access file system. Bypassing loading of operator usage statistics.");
            LogService.getRoot().log(Level.CONFIG, "com.rapidminer.gui.tools.usagestats.UsageStatistics.accessing_file_system_error_bypassing_loading");
            return;
        }
        File file = FileSystemService.getUserConfigFile("usagestats.xml");
        if (file.exists()) {
            try {
                //LogService.getRoot().config("Loading operator usage statistics.");
                LogService.getRoot().log(Level.CONFIG, "com.rapidminer.gui.tools.usagestats.UsageStatistics.loading_operator_statistics");
                Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file);

                Element root = doc.getDocumentElement();
                String lastReset = root.getAttribute("last_reset");
                if ((lastReset != null) && !lastReset.isEmpty()) {
                    try {
                        this.lastReset = getDateFormat().parse(lastReset);
                    } catch (ParseException e) {
                        this.lastReset = new Date();
                    }
                } else {
                    this.lastReset = new Date();
                }

                this.randomKey = root.getAttribute("random_key");
                if ((randomKey == null) || randomKey.isEmpty()) {
                    this.randomKey = createRandomKey();
                }

                String nextTransmission = root.getAttribute("next_transmission");
                if ((lastReset != null) && !lastReset.isEmpty()) {
                    try {
                        this.nextTransmission = getDateFormat().parse(nextTransmission);
                    } catch (ParseException e) {
                        scheduleTransmission(true);
                    }
                } else {
                    scheduleTransmission(false);
                }

                for (StatisticsScope scope : StatisticsScope.values()) {
                    Element element = (Element) doc.getElementsByTagName(scope.getXMLTag()).item(0);
                    NodeList children = element.getChildNodes();
                    for (int i = 0; i < children.getLength(); i++) {
                        if (children.item(i) instanceof Element) {
                            Element child = (Element) children.item(i);
                            getOperatorStatistics(scope, child.getTagName()).parse(child);
                        }
                    }
                }
            } catch (Exception e) {
                //LogService.getRoot().log(Level.WARNING, "Cannot load usage statistics: "+e, e);
            	LogService.getRoot().log(Level.WARNING,
    					I18N.getMessage(LogService.getRoot().getResourceBundle(), 
    					"com.rapidminer.gui.tools.usagestats.UsageStatistics.loading_operator_usage_error", 
    					e),
    					e);
            }
        } else {
            this.randomKey = createRandomKey();
        }
    }

    private String createRandomKey() {
        StringBuilder randomKey = new StringBuilder();
        Random random = new Random();
        for (int i = 0; i < 16; i++) {
            randomKey.append((char)('A' + random.nextInt(26)));
        }
        return randomKey.toString();
    }

    /** Sets all current counters to 0 and sets the last reset date to the current time. */
    public synchronized void reset() {
        getOperatorUsageStatistics(StatisticsScope.CURRENT).clear();
        this.lastReset = new Date();
    }

    /** Adds 1 to the statistics value for all operators contained in the current process in all scopes. */
    public synchronized void count(Process process, OperatorStatisticsValue type) {
        count(process, type, StatisticsScope.values());
    }

    /** Adds 1 to the statistics value for all operators contained in the current process the given scopes. */
    public void count(Process process, OperatorStatisticsValue type, StatisticsScope ... scopes) {
        List<Operator> allInnerOperators = process.getRootOperator().getAllInnerOperators();
        for (Operator op : allInnerOperators) {
            count(op, type, scopes);
        }
    }

    /** Adds 1 to the statistics value for the given operator in all scopes. */
    public void count(Operator op, OperatorStatisticsValue type) {
        count(op, type, StatisticsScope.values());
    }

    /** Adds 1 to the statistics value for the given operator in the given scopes. */
    public void count(Operator op, OperatorStatisticsValue type, StatisticsScope ... scopes) {
        if (op != null) {
            count(op.getOperatorDescription().getKey(), type, scopes);
        }
    }

    private synchronized void count(String operatorKey, OperatorStatisticsValue type, StatisticsScope ... scopes) {
        for (StatisticsScope scope : scopes) {
            getOperatorStatistics(scope, operatorKey).count(type);
        }
    }

    public OperatorUsageStatistics getOperatorStatistics(StatisticsScope scope, OperatorDescription op) {
        return getOperatorStatistics(scope, op.getKey());
    }

    /** Returns the statistics for the given scope and key. */
    synchronized OperatorUsageStatistics getOperatorStatistics(StatisticsScope scope, String opKey) {
        OperatorUsageStatistics stats = getOperatorUsageStatistics(scope).get(opKey);
        if (stats == null) {
            stats = new OperatorUsageStatistics();
            getOperatorUsageStatistics(scope).put(opKey, stats);
        }
        return stats;
    }

    private Map<String, OperatorUsageStatistics> getOperatorUsageStatistics(StatisticsScope scope) {
        return statsMaps.get(scope);
    }

    private Document getXML() {
        Document doc;
        try {
            doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
        } catch (ParserConfigurationException e) {
            throw new RuntimeException("Cannot create parser: "+e, e);
        }
        Element root = doc.createElement("usageStatistics");

        if (lastReset != null) {
            root.setAttribute("last_reset", getDateFormat().format(lastReset));
        }
        if (nextTransmission != null) {
            root.setAttribute("next_transmission", getDateFormat().format(nextTransmission));
        }
        root.setAttribute("random_key", this.randomKey);

        doc.appendChild(root);

        for (Entry<StatisticsScope, Map<String, OperatorUsageStatistics>> entry : statsMaps.entrySet()) {
            Map<String,OperatorUsageStatistics> statsMap = entry.getValue();
            StatisticsScope scope = entry.getKey();
            Element current = doc.createElement(scope.getXMLTag());
            root.appendChild(current);
            for (Map.Entry<String,OperatorUsageStatistics> statsEntry : statsMap.entrySet()) {
                current.appendChild(statsEntry.getValue().getXML(statsEntry.getKey(), doc));
            }
        }
        return doc;
    }

    /** Saves the statistics to a user file. */
    public void save() {
        if (RapidMiner.getExecutionMode().canAccessFilesystem()) {
            File file = FileSystemService.getUserConfigFile("usagestats.xml");
            try {
                //LogService.getRoot().config("Saving operator usage.");
                LogService.getRoot().log(Level.CONFIG, "com.rapidminer.gui.tools.usagestats.UsageStatistics.saving_operator_usage");
                XMLTools.stream(getXML(), file, null);
            } catch (Exception e) {
                //LogService.getRoot().log(Level.WARNING, "Cannot save operator usage statistics: "+e, e);
                LogService.getRoot().log(Level.WARNING,
    					I18N.getMessage(LogService.getRoot().getResourceBundle(), 
    					"com.rapidminer.gui.tools.usagestats.UsageStatistics.saving_operator_usage_error", 
    					e),
    					e);
            }
        } else {
            //LogService.getRoot().config("Cannot access file system. Bypassing save of operator usage statistics.");
            LogService.getRoot().config("com.rapidminer.gui.tools.usagestats.UsageStatistics.accessing_file_system_error_bypassing_save");
        }
    }

    /** Returns the statistics as a data table that can be displayed to the user. */
    public DataTable getAsDataTable(final StatisticsScope scope) {
        return new OperatorStatisticsDataTable(this, scope);
    }

    /** Returns a list of all operator names for which statistics are available. */
    public List<String> getOperatorKeys(StatisticsScope scope) {
        return new LinkedList<String>(getOperatorUsageStatistics(scope).keySet());
    }

    private static DateFormat getDateFormat() {
        return DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT, Locale.US);
    }

    /**
     * 
     * @return true on success
     */
    public boolean transferUsageStats(ProgressListener progressListener) throws Exception {
        StatisticsReport report = new StatisticsReport();
        report.setFrom(XMLTools.getXMLGregorianCalendar(lastReset));
        report.setTo(XMLTools.getXMLGregorianCalendar(new Date()));
        report.setUserKey(this.randomKey);
        for (String name : getOperatorKeys(StatisticsScope.CURRENT)) {
            OperatorUsageStatistics stats = getOperatorStatistics(StatisticsScope.CURRENT, name);
            StatisticsRecord record = new StatisticsRecord();
            record.setOperatorName(name);
            record.setExecution(stats.getStatistics(OperatorStatisticsValue.EXECUTION));
            record.setFailure(stats.getStatistics(OperatorStatisticsValue.FAILURE));
            record.setOperatorException(stats.getStatistics(OperatorStatisticsValue.OPERATOR_EXCEPTION));
            record.setRuntimeError(stats.getStatistics(OperatorStatisticsValue.RUNTIME_EXCEPTION));
            record.setStop(stats.getStatistics(OperatorStatisticsValue.STOPPED));
            record.setUserError(stats.getStatistics(OperatorStatisticsValue.USER_ERROR));
            report.getRecords().add(record);
        }

        if (progressListener != null) {
            progressListener.setCompleted(25);
        }

        RapidHome rapidHome = getPort();
        if (rapidHome != null) {
            if (progressListener != null) {
                progressListener.setCompleted(40);
            }
            rapidHome.uploadUsageStatistics(report);
            if (progressListener != null) {
                progressListener.setCompleted(80);
            }
            return true;
        } else {
            if (progressListener != null) {
                progressListener.setCompleted(80);
            }
            return false;
        }
    }

    private RapidHome getPort() {
        for (String urlString : RAPIDMINER_HOME_URLS) {
            try {
                URL url = new URL(urlString);
                //LogService.getRoot().info("Transferring operator usage statistics to "+url+".");
                LogService.getRoot().log(Level.INFO, "com.rapidminer.gui.tools.usagestats.UsageStatistics.transferring_operator", url);
                RapidHomeService rapidHomeService = new RapidHomeService(url,
                        new QName("http://ws.rapidhome.rapid_i.com/", "RapidHomeService"));

                final RapidHome port = rapidHomeService.getRapidHomePort();
                WebServiceTools.setTimeout((BindingProvider) port);
                return port;
            } catch (Exception e) {
                //LogService.getRoot().log(Level.WARNING, "Failed to connect to usage statistics service "+urlString+".", e);
                LogService.getRoot().log(Level.WARNING, "com.rapidminer.gui.tools.usagestats.UsageStatistics.connection_usage_statistics_error", urlString);
                continue;
            }
        }
        return null;
    }

    /** Sets the date for the next transmission. Starts no timers. */
    void scheduleTransmission(boolean lastAttemptFailed) {
        this.failedToday = true;
        this.nextTransmission = new Date(lastReset.getTime() + TRANSMISSION_INTERVAL);
    }
    
    /**
     * Returns the user key for this session.
     * @return the user key
     */
    public String getUserKey() {
    	return randomKey;
    }

    /** Returns the date at which the next transmission should be scheduled. */
    public Date getNextTransmission() {
        if (nextTransmission == null) {
            scheduleTransmissionFromNow();
        }
        return nextTransmission;
    }

    public void scheduleTransmissionFromNow() {
        this.nextTransmission = new Date(System.currentTimeMillis() + TRANSMISSION_INTERVAL);
    }

    public boolean hasFailedToday() {
        return failedToday;
    }

    public static void main(String[] args) {
        UsageStatistics.getInstance().getPort();
    }
}
