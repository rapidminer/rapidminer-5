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
package com.rapidminer.repository.remote;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;
import javax.xml.ws.BindingProvider;

import com.rapid_i.repository.wsimport.ExecutionResponse;
import com.rapid_i.repository.wsimport.ProcessContextWrapper;
import com.rapid_i.repository.wsimport.ProcessResponse;
import com.rapid_i.repository.wsimport.ProcessService;
import com.rapid_i.repository.wsimport.ProcessService13;
import com.rapid_i.repository.wsimport.ProcessService13_Service;
import com.rapid_i.repository.wsimport.ProcessService_Service;
import com.rapid_i.repository.wsimport.QueueProperty;
import com.rapid_i.repository.wsimport.QueueState;
import com.rapid_i.repository.wsimport.RAInfoService;
import com.rapid_i.repository.wsimport.Response;
import com.rapidminer.gui.tools.VersionNumber;
import com.rapidminer.tools.I18N;
import com.rapidminer.tools.LogService;
import com.rapidminer.tools.WebServiceTools;

/**
 * This is a wrapper for all available Process Service Versions. It hides the current 
 * 
 * @author Nils Woehler
 *
 */
public class ProcessServiceFacade {
	private static final Logger LOGGER = Logger.getLogger(ProcessServiceFacade.class.getName());

	public static final String VERSION_1_0_URL = "RAWS/ProcessService?wsdl";
	public static final String VERSION_1_3_URL = "RAWS/ProcessService_1_3?wsdl";

	public static final VersionNumber VERSION_1_0 = new VersionNumber(1, 0);
	public static final VersionNumber VERSION_1_3 = new VersionNumber(1, 3);

	private final VersionNumber processServiceVersion;

	private URL baseURL;

	private ProcessService processService_1_0;
	private ProcessService13 processService_1_3;

	public ProcessServiceFacade(RAInfoService raInfoService, URL baseURL, String username, char[] password) {
		this.baseURL = baseURL;

		// is available in any case
		LOGGER.info("Setting up ProcessService v1.0");
		VersionNumber highestProcessServiceVersion = VERSION_1_0;
		ProcessService_Service serviceService = new ProcessService_Service(getProcessServiceWSDLUrl(VERSION_1_0_URL), new QName("http://service.web.rapidanalytics.de/", "ProcessService"));
		processService_1_0 = serviceService.getProcessServicePort();
		setupBindingProvider((BindingProvider) processService_1_0, username, password);

		// if raInfoService is not null, check which further versions are available
		
		if (raInfoService != null) {
			VersionNumber versionNumber = new VersionNumber(raInfoService.getVersionNumber());
			if (versionNumber.isAtLeast(VERSION_1_3)) {
				LOGGER.info("Setting up ProcessService v1.3");
				highestProcessServiceVersion = VERSION_1_3;
				ProcessService13_Service service13Service = new ProcessService13_Service(getProcessServiceWSDLUrl(VERSION_1_3_URL), new QName("http://service.web.rapidanalytics.de/", "ProcessService_1_3"));
				processService_1_3 = service13Service.getProcessService13Port();
				setupBindingProvider((BindingProvider) processService_1_3, username, password);
			} else {
				LOGGER.info("RapidAnalytics version is " + versionNumber.getLongVersion() + " - ProcessService v1.3 will be not initialized");
			}
		} else {
			LOGGER.warning("RapidAnalytics info service to set - can not setup ProcessService v1.3");
		}

		this.processServiceVersion = highestProcessServiceVersion;

	}

	private void setupBindingProvider(BindingProvider bp, String username, char[] password) {
		if (password != null) {
			bp.getRequestContext().put(BindingProvider.USERNAME_PROPERTY, username);
			bp.getRequestContext().put(BindingProvider.PASSWORD_PROPERTY, new String(password));
		}
		WebServiceTools.setTimeout(bp);
	}

	private URL getProcessServiceWSDLUrl(String url) {
		try {
			return new URL(getBaseURL(), url);
		} catch (MalformedURLException e) {
			// cannot happen
			//LogService.getRoot().log(Level.WARNING, "Cannot create Web service url: " + e, e);
			LogService.getRoot().log(Level.WARNING,
					I18N.getMessage(LogService.getRoot().getResourceBundle(),
							"com.rapidminer.repository.remote.RemoteRepository.creating_webservice_error",
							url),
					e);
			return null;
		}
	}

	/**
	 * @return the baseURL
	 */
	public URL getBaseURL() {
		return this.baseURL;
	}

	/**
	 * @param baseURL the baseURL to set
	 */
	public void setBaseURL(URL baseURL) {
		this.baseURL = baseURL;
	}

	/**
	 * @return the processServiceVersion
	 */
	public VersionNumber getProcessServiceVersion() {
		return this.processServiceVersion;
	}

	/**
	 * Works with all process service versions.
	 */
	public ExecutionResponse executeProcessSimple(String processName, XMLGregorianCalendar date,
													ProcessContextWrapper context) {
		return processService_1_0.executeProcessSimple(processName, date, context);
	}

	/**
	 * If Process Service version is not at least 1.3 queueName will not be considered. 
	 */
	public ExecutionResponse executeProcessSimple(String processName, XMLGregorianCalendar date, ProcessContextWrapper context,
													String queueName) {
		if (getProcessServiceVersion().isAtLeast(VERSION_1_3)) {
			return processService_1_3.executeProcessSimple13(processName, date, context, queueName);
		} else {
			return processService_1_0.executeProcessSimple(processName, date, context);
		}
	}

	/**
	 * Executes a process with an provided offset.
	 * 
	 * @return <code>null</code> if function is not supported.
	 */
	public ExecutionResponse executeProcessWithOffset(String processName, Long offset, ProcessContextWrapper context,
														String queueName) {
		if (getProcessServiceVersion().isAtLeast(VERSION_1_3)) {
			return processService_1_3.executeProcessWithOffset(processName, offset, context, queueName);
		} else {
			return null;
		}
	}

	/**
	 * Works with all process service versions.
	 */
	public ExecutionResponse executeProcessCron(String processName, String cronExpression, XMLGregorianCalendar start, XMLGregorianCalendar end,
												ProcessContextWrapper context) {
		return processService_1_0.executeProcessCron(processName, cronExpression, start, end, context);
	}

	/**
	 * If Process Service version is not at least 1.3 queueName will not be considered.
	 */
	public ExecutionResponse executeProcessCron(String processName, String cronExpression, XMLGregorianCalendar start, XMLGregorianCalendar end,
												ProcessContextWrapper context, String queueName) {
		if (getProcessServiceVersion().isAtLeast(VERSION_1_3)) {
			return processService_1_3.executeProcessCron13(processName, cronExpression, start, end, context, queueName);
		} else {
			return processService_1_0.executeProcessCron(processName, cronExpression, start, end, context);
		}
	}

	public Response cancelTrigger(String triggerName) {
		return processService_1_0.cancelTrigger(triggerName);
	}

	public List<Integer> getRunningProcesses(XMLGregorianCalendar since) {
		return processService_1_0.getRunningProcesses(since);
	}

	/** Note: This method returns info about the job associated with the given id of a 
	 *  {@link ScheduledProcess}, not of the {@link ProcessExecutionParameters} object
	 *  returned from {@link ProcessService_1_3#executeProcessSimple(String, Date)} 
	 *  when submitting the job. 
	 */
	public ProcessResponse getRunningProcessesInfo(int scheduledProcessId) {
		return processService_1_0.getRunningProcessesInfo(scheduledProcessId);
	}

	public List<Integer> getProcessIdsForJobId(int jobId) {
		return processService_1_0.getProcessIdsForJobId(jobId);
	}

	public Response stopProcess(int scheduledProcessId) {
		return processService_1_0.stopProcess(scheduledProcessId);
	}

	/** Returns a list of available execution queue names.
	 *  If the process service version is prior to version 1.3, <code>null</code> will be returned. */
	public List<String> getQueueNames() {
		if (getProcessServiceVersion().isAtLeast(VERSION_1_3)) {
			return processService_1_3.getQueueNames();
		} else {
			return null;
		}
	}

	/** Returns the state for the queue with the provided name.
	 *  If queue doesn't exist or process service version is prior to version 1.3, <code>null</code> will be returned. */
	public QueueState getQueueState(String queueName) {
		if (getProcessServiceVersion().isAtLeast(VERSION_1_3)) {
			return processService_1_3.getQueueState(queueName);
		} else {
			return null;
		}
	}

	/** Returns the properties for the queue with the provided name. 
	 *  If queue doesn't exist or process service version is prior to version 1.3, <code>null</code> will be returned. */
	public List<QueueProperty> getQueueInfo(String queueName) {
		if (getProcessServiceVersion().isAtLeast(VERSION_1_3)) {
			return processService_1_3.getQueueInfo(queueName);
		} else {
			return null;
		}
	}

}
