/*
 *  RapidMiner
 *
 *  Copyright (C) 2001-2014 by RapidMiner and the contributors
 *
 *  Complete list of developers available at our web site:
 *
 *       http://rapidminer.com
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
package com.rapidminer.gui.tools;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.logging.Level;

import javax.swing.AbstractListModel;
import javax.swing.SwingUtilities;

import com.rapidminer.tools.I18N;
import com.rapidminer.tools.LogService;
import com.rapidminer.tools.ProgressListener;

/** {@link Runnable}s implementing this class can be execute in a dedicated thread
 *  (single thread pool) and automatically display their progress in the status bar.
 *  To use this class, define a property "gui.progress.KEY.label" in the GUI 
 *  properties file, and pass KEY to the constructor. Then, from within the
 *  {@link #run()} method, use {@link #getProgressListener()} to report any
 *  progress the task makes.
 *  
 *  @author Simon Fischer
 */
public abstract class ProgressThread implements Runnable {

	Thread thread = new Thread();
	
	private final Set<ProgressThreadListener> listeners = new CopyOnWriteArraySet<ProgressThreadListener>();
	
	/** Task currently being executed. */
	private static ProgressThread current = null;

	protected static final class QueueListModel extends AbstractListModel {
		private static final long serialVersionUID = 1L;
		private List<ProgressThread> queue = new ArrayList<ProgressThread>();
		@Override
		public Object getElementAt(int index) { return queue.get(index); }

		@Override
		public int getSize() { return queue.size(); }

		private void add(final ProgressThread thread) {
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					queue.add(thread);
					fireIntervalAdded(this, queue.size()-1, queue.size()-1);			
				}
			});
		}			

		private void remove(final ProgressThread thread) {
			SwingUtilities.invokeLater(new Runnable() {
				@Override
				public void run() {
					int index = queue.indexOf(thread);
					queue.remove(index);			
					fireIntervalRemoved(this, index, index);					
				}
			});
		}
		private void jobCancelled(ProgressThread pt) {
			int index = queue.indexOf(pt);
			fireContentsChanged(this, index, index);			
		}
	}

	private static ExecutorService EXECUTOR = Executors.newSingleThreadExecutor(new ThreadFactory() {
		@Override
		public Thread newThread(Runnable r) {
			Thread thread = new Thread(r, "ProgressThread");
			thread.setDaemon(true);
			thread.setPriority(Thread.MIN_PRIORITY);
			return thread;
		}		
	});

	static final QueueListModel QUEUE_MODEL = new QueueListModel();

	private final ProgressDisplay display;
	private final String name;	
	private boolean runInForeground;

	public ProgressThread(String i18nKey) {
		this(i18nKey, false);
	}

	public ProgressThread(String i18nKey, boolean runInForeground) {
		this.name = I18N.getMessage(I18N.getGUIBundle(), "gui.progress."+i18nKey+".label");
		this.display = new ProgressDisplay(name, this);
		this.runInForeground = runInForeground;
	}

	public String getName() {
		return name;
	}

	public String toString() {
		return name + (cancelled ? " (cancelled)" : "");
	}

	public ProgressListener getProgressListener() {
		checkCancelled();
		return display.getListener();
	}

	public ProgressDisplay getDisplay() {
		return display;
	}
	
	public void setDisplayLabel(String i18nKey) {
		String newName = I18N.getMessage(I18N.getGUIBundle(), "gui.progress."+i18nKey+".label");
		display.setLabel(newName);
	}

	/** Note that this method has nothing to do with Thread.strart. It merely enqueues
	 *  this Runnable in the Executor's queue. */
	public void start() {
		EXECUTOR.execute(makeWrapper());
	}

	/** Enqueues this task and waits for its completion. If you call this method, you probably
	 *  want to set the runInForeground flag in the constructor to true. */
	public void startAndWait() {
		try {
			EXECUTOR.submit(makeWrapper()).get();
		} catch (InterruptedException e) {
			//LogService.getRoot().log(Level.SEVERE, "Cannot execute '"+name+"'.", e);
			LogService.getRoot().log(Level.SEVERE,
					I18N.getMessage(LogService.getRoot().getResourceBundle(), 
					"com.rapidminer.gui.tools.ProgressThread.executing_error", 
					name),
					e);

		} catch (ExecutionException e) {
			//LogService.getRoot().log(Level.SEVERE, "Cannot execute '"+name+"'.", e);	
			LogService.getRoot().log(Level.SEVERE,
					I18N.getMessage(LogService.getRoot().getResourceBundle(), 
					"com.rapidminer.gui.tools.ProgressThread.executing_error", 
					name),
					e);
		}
	}

	/** Creates a wrapper that executes this class' run method, sets {@link #current} and subsequently
	 *  removes it from the list of pending tasks and shows a {@link ProgressThreadDialog}
	 *  if necessary. As a side effect, calling this method also results in adding
	 *  this ProgressThread to the list of pending tasks. 
	 *  */
	private Runnable makeWrapper() {
		QUEUE_MODEL.add(this);
		return new Runnable() {
			@Override
			public void run() {		
				synchronized (LOCK) {
					if (cancelled) {
						//LogService.getRoot().info("Task "+getName()+" was cancelled.");
						LogService.getRoot().log(Level.INFO, "com.rapidminer.gui.tools.ProgressThread.task_cacelled", getName());
						return;
					}
					started = true;
				}
				try {
					current = ProgressThread.this;
					if (runInForeground) {					
						SwingUtilities.invokeLater(new Runnable() {
							public void run() {
								if (!ProgressThreadDialog.getInstance().isVisible()) {
									ProgressThreadDialog.getInstance().setVisible(true);
								}	
							};
						});					
					}
					ProgressThread.this.run();
				} catch (ProgressThreadStoppedException e) {
					//LogService.getRoot().fine("Progress thread "+getName()+" aborted (cancelled).");
					LogService.getRoot().log(Level.FINE, "com.rapidminer.gui.tools.ProgressThread.progress_thread_aborted", getName());
				} catch (Exception e) {
					//LogService.getRoot().log(Level.WARNING, "Error executing background job '"+name+"': "+e, e);
					LogService.getRoot().log(Level.WARNING,
							I18N.getMessage(LogService.getRoot().getResourceBundle(), 
							"com.rapidminer.gui.tools.ProgressThread.error_executing_background_job", 
							name, e),
							e);

					SwingTools.showSimpleErrorMessage("error_executing_background_job", e, name, e);
				} finally {
					if (!ProgressThread.this.isCancelled()) {
						ProgressThread.this.getProgressListener().complete();
					}
					QUEUE_MODEL.remove(ProgressThread.this);
					current = null;
					for (ProgressThreadListener listener : listeners) {
						listener.threadFinished(ProgressThread.this);
				    }
				}
			}				
		};
	}

	public static ProgressThread getCurrent() {
		return current;
	}

	private static final Object LOCK = new Object();

	private boolean cancelled = false;
	/** True if the thread is started. (Remains true after cancelling.) */
	private boolean started = false;

	/** Returns true if the thread was cancelled. */
	public final boolean isCancelled() {
		return cancelled;
	}

	/** If the thread is currently active, calls {@link #executionCancelled()} to notify children.
	 *  If not active, removes the thread from the queue so it won't become active. */
	public final void cancel() {
		synchronized (LOCK) {
			cancelled = true;
			if (started) {
				executionCancelled();
				QUEUE_MODEL.jobCancelled(this);
			} else {
				QUEUE_MODEL.remove(this);				
			}
		}
	}

	/** Subclasses can implemented this method if they want to be notified about cancellation of this thread.
	 *  In most cases, this is not necessary. Subclasses can ask {@link #isCancelled()} whenever cancelling
	 *  is possible, or, even easier, directly call {@link #checkCancelled()}. */
	protected void executionCancelled() {		
	}

	/** If cancelled, throws a RuntimeException to stop the thread. */
	protected void checkCancelled() throws ProgressThreadStoppedException {
		if (cancelled) {
			throw new ProgressThreadStoppedException();
		}
	}
	
	/** Adds a new ProgressThreadListener **/
	public final void addProgressThreadListener(final ProgressThreadListener listener) {
		listeners.add(listener);
	}
	
	/** Removes a ProgressThreadListener **/
	public final void removeProgressThreadListener(final ProgressThreadListener listener) {
		listeners.remove(listener);
	}
}
