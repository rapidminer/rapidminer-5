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

import com.rapidminer.repository.RepositoryAccessor;

/**
 * A {@link ProcessScheduler} can be used to schedule processes on RapidAnalytics. It can be created via a {@link ProcessSchedulerFactory}.
 * 
 * @author Nils Woehler
 *
 */
public interface ProcessScheduler {

	/**
	 * Schedules a process on RapidAnalytics.
	 * 
	 * @param config the config that determines when the process should be executed
	 * @param repositoryAccessor 
	 * @throws SchedulingException if the config has errors or the scheduling did fail a {@link SchedulingException} is thrown.
	 * @return the date the process will run first
	 */
	public SchedulerResponse scheduleProcess(ProcessSchedulerConfig config, RepositoryAccessor repositoryAccessor) throws SchedulingException;
	
}
