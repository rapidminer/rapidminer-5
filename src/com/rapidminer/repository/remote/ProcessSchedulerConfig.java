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

import com.rapidminer.ProcessContext;
import com.rapidminer.repository.RepositoryLocation;

/**
 * A class that represents a configuration for process scheduling.
 * 
 * @author Nils Woehler
 *
 */
public class ProcessSchedulerConfig {

	private final ScheduleMode mode;
	private final RepositoryLocation location;
	private final ProcessContext context;
	private final String queueName;
	private Date onceDate;
	private String cronExpression;
	private Date start;
	private Date end;
	private Long offset;

	public static enum ScheduleMode {
		NOW,
		ONCE,
		OFFSET,
		CRON
	}

	/**
	 * Constructor that creates a schedule config that is executed immediately.
	 * @param queueName if <code>null</code> DEFAULT queue will be used.
	 */
	public ProcessSchedulerConfig(RepositoryLocation location, ProcessContext context, String queueName) {
		this(ScheduleMode.NOW, location, null, null, null, null, null, context, queueName);
	}

	/**
	 * Constructor that creates a schedule config that is executed once at a specified date.
	 * @param queueName if <code>null</code> DEFAULT queue will be used.
	 */
	public ProcessSchedulerConfig(RepositoryLocation location, Date onceDate, ProcessContext context, String queueName) {
		this(ScheduleMode.ONCE, location, null, onceDate, null, null, null, context, queueName);
	}

	/**
	 * Constructor that creates a schedule config that is executed after a specified amount of time.
	 * @param queueName if <code>null</code> DEFAULT queue will be used.
	 */
	public ProcessSchedulerConfig(RepositoryLocation location, Long offset, ProcessContext context, String queueName) {
		this(ScheduleMode.OFFSET, location, null, null, null, null, offset, context, queueName);
	}

	/**
	 * Constructor that creates a cron schedule config. If start date is <code>null</code>, start and end will not be used.
	 * If start is not <code>null</code> and end is <code>null</code>, only start will be used.
	 * @param queueName if <code>null</code> DEFAULT queue will be used
	 */
	public ProcessSchedulerConfig(RepositoryLocation location, String cronExpression, Date start, Date end, ProcessContext context, String queueName) {
		this(ScheduleMode.CRON, location, cronExpression, null, start, end, null, context, queueName);
	}

	private ProcessSchedulerConfig(ScheduleMode mode, RepositoryLocation location, String cronExpression, Date onceDate, Date start, Date end, Long offset, ProcessContext context, String queueName) {
		if (location == null) {
			throw new IllegalArgumentException("Null location is not allowed!");
		}
		if (context == null) {
			throw new IllegalArgumentException("Null context is not allowed!");
		}
		if (mode.equals(ScheduleMode.ONCE) && onceDate == null) {
			throw new IllegalArgumentException("Null onceDate is not allowed with ScheduleMode ONCE!");
		}
		if (mode.equals(ScheduleMode.CRON) && cronExpression == null) {
			throw new IllegalArgumentException("Null cronExpression is not allowed with ScheduleMode CRON!");
		}
		if (mode.equals(ScheduleMode.OFFSET) && (offset == null)) {
			throw new IllegalArgumentException("Null offset is with ScheduleMode TIMER!");
		}
		this.onceDate = onceDate;
		this.location = location;
		this.cronExpression = cronExpression;
		this.start = start;
		this.end = end;
		this.context = context;
		this.queueName = queueName;
		this.mode = mode;
		this.offset = offset;
	}

	/**
	 * @return the mode
	 */
	public ScheduleMode getMode() {
		return this.mode;
	}

	/**
	 * @return the context
	 */
	public ProcessContext getContext() {
		return this.context;
	}

	/**
	 * @return the onceDate
	 */
	public Date getOnceDate() {
		return this.onceDate;
	}

	/**
	 * @return the cronExpression
	 */
	public String getCronExpression() {
		return this.cronExpression;
	}

	/**
	 * @return the start
	 */
	public Date getStart() {
		return this.start;
	}

	/**
	 * @return the end
	 */
	public Date getEnd() {
		return this.end;
	}

	/**
	 * @param onceDate the onceDate to set
	 */
	public void setOnceDate(Date onceDate) {
		this.onceDate = onceDate;
	}

	/**
	 * @param cronExpression the cronExpression to set
	 */
	public void setCronExpression(String cronExpression) {
		this.cronExpression = cronExpression;
	}

	/**
	 * @param start the start to set
	 */
	public void setStart(Date start) {
		this.start = start;
	}

	/**
	 * @param end the end to set
	 */
	public void setEnd(Date end) {
		this.end = end;
	}

	/**
	 * @return the location
	 */
	public RepositoryLocation getLocation() {
		return this.location;
	}

	/**
	 * @return the queueName
	 */
	public String getQueueName() {
		return this.queueName;
	}

	/**
	 * @return the offset
	 */
	public Long getOffset() {
		return this.offset;
	}

	/**
	 * @param offset the offset to set
	 */
	public void setOffset(Long offset) {
		this.offset = offset;
	}

}
