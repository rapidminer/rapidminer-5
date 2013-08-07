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
package com.rapidminer.tools.jdbc;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.rapidminer.tools.ProgressListener;

/**
 * This class caches the table meta data so the DB is not queried all the time.
 * 
 * @author Marco Boeck
 *
 */
public class TableMetaDataCache {

	private Map<String, Map<TableName, List<ColumnIdentifier>>> tableMap;

	/** time of the last cache update */
	private Map<String, Long> lastQueryTimeMap;

	private Map<String, Object> lockMap;
	
	private boolean refreshCacheAfterInterval;


	/** time before the cache is refreshed again (in ms) */
	private static final int CACHE_REFRESH_INTERVAL = 60000;

	/** if true, will refresh cache if set amount of times expired since last cache update; if false will never update cache automatically */

	/** the instance of this class */
	private static TableMetaDataCache instance;

	/**
	 * Creates a new {@link TableMetaDataCache} instance.
	 * @param refreshCacheAfterInterval
	 */
	private TableMetaDataCache(boolean refreshCacheAfterInterval) {
		this.lastQueryTimeMap = new ConcurrentHashMap<String, Long>();
		this.tableMap = new ConcurrentHashMap<String, Map<TableName, List<ColumnIdentifier>>>();
		this.lockMap = new ConcurrentHashMap<String, Object>();
		this.refreshCacheAfterInterval = refreshCacheAfterInterval;
	}

	/**
	 * Get the instance of {@link TableMetaDataCache}.
	 * @return
	 */
	public static synchronized TableMetaDataCache getInstance() {
		if (instance == null) {
			// currently, default for this cache is to NOT refresh after a given interval
			instance = new TableMetaDataCache(false);
		}

		return instance;
	}

	/**
	 * Fetches meta data about all tables and, if selected, all columns in the database.
	 * The returned map maps table names to column descriptions.
	 * If fetchColumns is false, all lists in the returned map will be empty lists, so basically
	 * only the key set contains useful information.
	 * <p>
	 * This method is cached, so the data might not be up to date before the cache is refreshed.
	 * See {@link #clearCache()}.
	 * @param connectionName
	 * @param handler
	 * @param progressListener
	 * @param minProgress
	 * @param maxProgress
	 * @param fetchColumns
	 * @return
	 * @throws SQLException
	 */
	public Map<TableName, List<ColumnIdentifier>> getAllTableMetaData(String connectionName, DatabaseHandler handler,
			ProgressListener progressListener, int minProgress, int maxProgress)
			throws SQLException {
		// does lock for this connection already exist?
		if (!lockMap.containsKey(connectionName)) {
			lockMap.put(connectionName, new Object());
		}
		
		// only lock for the same connectionName aka same connection - different connections don't need to wait
		synchronized (lockMap.get(connectionName)) {
			Map<TableName, List<ColumnIdentifier>> map = this.tableMap.get(connectionName);
			if (map == null || (refreshCacheAfterInterval && (System.currentTimeMillis() - this.lastQueryTimeMap
							.get(connectionName)) > CACHE_REFRESH_INTERVAL)) {
				// if tableMap does not contain entry for this connectionName or the entry is too old (only if refreshCacheAfterInterval is true)
				// update cache and return new map, otherwise return cached map
				updateCache(connectionName, handler, progressListener, minProgress, maxProgress, true);
				map = tableMap.get(connectionName);
			}

			progressListener.setCompleted(maxProgress);
			return map;
		}
	}

	/**
	 * Fetches meta data about all tables and, if selected, all columns in the database.
	 * The returned map maps table names to column descriptions.
	 * If fetchColumns is false, all lists in the returned map will be empty lists, so basically
	 * only the key set contains useful information.
	 * <p>
	 * This method is cached, so the data might not be up to date before the cache is refreshed.
	 * See {@link #clearCache()}.
	 * @param connectionName
	 * @param handler
	 * @return
	 * @throws SQLException
	 */
	public Map<TableName, List<ColumnIdentifier>> getAllTableMetaData(String connectionName, DatabaseHandler handler)
			throws SQLException {
		return getAllTableMetaData(connectionName, handler, null, 0, 0);
	}
	
	/**
	 * Fetches the  for a given connection name.
	 * <p>
	 * This method is cached, so the data might not be up to date before the cache is refreshed.
	 * See {@link #clearCache()}.
	 * @param connectionName
	 * @param handler
	 * @return
	 * @throws SQLException
	 */
	public List<ColumnIdentifier> getAllColumnNames(String connectionName, DatabaseHandler handler, TableName tableName) throws SQLException {
		// does lock for this connection already exist?
		if (!lockMap.containsKey(connectionName)) {
			lockMap.put(connectionName, new Object());
		}

		// only lock for the same connectionName aka same connection - different connections don't need to wait
		synchronized (lockMap.get(connectionName)) {
			Map<TableName, List<ColumnIdentifier>> map = this.tableMap.get(connectionName);
			if (map == null || (refreshCacheAfterInterval && (System.currentTimeMillis() - this.lastQueryTimeMap
					.get(connectionName)) > CACHE_REFRESH_INTERVAL)) {
				// if tableMap does not contain entry for this connectionName or the entry is too old (only if refreshCacheAfterInterval is true)
				// update cache and return new map, otherwise return cached map
				updateCache(connectionName, handler, null, 0, 0, true);
				map = this.tableMap.get(connectionName);
			}

			return map.get(tableName);
		}
	}

	/**
	 * Clears the whole cache.
	 */
	public void clearCache() {
		this.tableMap.clear();
	}

	/**
	 * Updates the cache.
	 * @throws SQLException
	 */
	private void updateCache(String connectionName, DatabaseHandler handler, ProgressListener progressListener,
			int minProgress, int maxProgress, boolean fetchColumns) throws SQLException {
		Map<TableName, List<ColumnIdentifier>> tableMetaMap = handler.getAllTableMetaData(progressListener, minProgress, maxProgress, fetchColumns);
		this.tableMap.put(connectionName, tableMetaMap);
		
		this.lastQueryTimeMap.put(connectionName, new Long(System.currentTimeMillis()));
	}

}
