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

import java.util.Date;

import javax.xml.datatype.XMLGregorianCalendar;

import com.rapid_i.repository.wsimport.ExecutionResponse;
import com.rapid_i.repository.wsimport.MacroDefinition;
import com.rapid_i.repository.wsimport.ProcessContextWrapper;
import com.rapidminer.ProcessContext;
import com.rapidminer.io.process.XMLTools;
import com.rapidminer.repository.Repository;
import com.rapidminer.repository.RepositoryAccessor;
import com.rapidminer.repository.RepositoryConstants;
import com.rapidminer.repository.RepositoryException;
import com.rapidminer.tools.container.Pair;

/**
 * A process scheduler that calls RapidAnalytics ProcessService Webservice.
 * 
 * @author Nils Woehler
 *
 */
public class RemoteProcessScheduler implements ProcessScheduler {

	@Override
	public SchedulerResponse scheduleProcess(ProcessSchedulerConfig config, RepositoryAccessor ignored) throws SchedulingException {

		ExecutionResponse response;
		ProcessServiceFacade processService;
		try {
			Repository repository = config.getLocation().getRepository();
			if (!(repository instanceof RemoteRepository)) {
				throw new SchedulingException("not_on_ra_repository", null, repository.getName());
			}
			processService = ((RemoteRepository) repository).getProcessService();
		} catch (RepositoryException e) {
			throw new SchedulingException("scheduling_failed", e, e.getLocalizedMessage());
		}
		String queueName = config.getQueueName();
		String path = config.getLocation().getPath();
		ProcessContext pc = config.getContext();
		ProcessContextWrapper processContextWrapper = createProcessContextWrapper(pc);
		switch (config.getMode()) {
			case NOW:
				response = processService.executeProcessSimple(path, null, processContextWrapper, queueName);
				break;
			case ONCE:
				Date date = config.getOnceDate();
				response = processService.executeProcessSimple(path, XMLTools.getXMLGregorianCalendar(date), processContextWrapper, queueName);
				break;
			case OFFSET:
				response = processService.executeProcessWithOffset(path, config.getOffset(), processContextWrapper, queueName);
				break;
			case CRON:
				Date startDate = config.getStart();
				Date endDate = config.getEnd();
				XMLGregorianCalendar start = startDate != null ? XMLTools.getXMLGregorianCalendar(startDate) : null;
				XMLGregorianCalendar end = endDate != null ? XMLTools.getXMLGregorianCalendar(endDate) : null;
				response = processService.executeProcessCron(path, config.getCronExpression(), start, end, processContextWrapper, queueName);
				break;
			default:
				throw new SchedulingException("scheduling_failed", null, "Unknown ScheduleMode " + config.getMode());
		}

		if (response == null) {
			throw new SchedulingException("scheduling_failed", null, "Unsupported operation. Check if RapidAnalytics has at least version 1.3.");
		} else if (response.getStatus() != RepositoryConstants.OK) {
			throw new SchedulingException("scheduling_failed", null, response.getErrorMessage());
		} else {
			return new SchedulerResponse(response.getFirstExecution().toGregorianCalendar().getTime(), response.getJobId());
		}

	}

	private ProcessContextWrapper createProcessContextWrapper(ProcessContext context) {
		ProcessContextWrapper pcWrapper = new ProcessContextWrapper();
		for (String loc : context.getInputRepositoryLocations()) {
			pcWrapper.getInputRepositoryLocations().add(loc);
		}
		for (String loc : context.getOutputRepositoryLocations()) {
			pcWrapper.getOutputRepositoryLocations().add(loc);
		}
		for (Pair<String, String> macro : context.getMacros()) {
			final MacroDefinition macroDef = new MacroDefinition();
			macroDef.setKey(macro.getFirst());
			macroDef.setValue(macro.getSecond());
			pcWrapper.getMacros().add(macroDef);
		}
		return pcWrapper;
	}

}
