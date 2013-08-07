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


/**
 * 
 * The response from the {@link ProcessScheduler} which contains information
 * as the first date of execution and the job id of the scheduled job.
 * 
 * @author Marcin Skirzynski
 *
 */
public class SchedulerResponse {
	
	/**
	 * The date of the first execution.
	 */
	private final Date firstExecution;
	
	/**
	 * The job id of the scheduled job.
	 */
	private final int jobId;
	
	/**
	 * Creates a response with the first execution date and the job id.
	 */
	public SchedulerResponse(Date firstExecution, int jobId) {
		this.firstExecution = firstExecution;
		this.jobId = jobId;
	}

	/**
	 * The date when the process will or was be executed first.
	 */
	public Date getFirstExecution() {
		return firstExecution;
	}

	/**
	 * The job id of the scheduler job.
	 */
	public int getJobId() {
		return jobId;
	}


}
