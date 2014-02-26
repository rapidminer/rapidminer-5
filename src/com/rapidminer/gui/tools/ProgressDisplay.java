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

import com.rapidminer.gui.ApplicationFrame;
import com.rapidminer.tools.ProgressListener;


/**
 * @author Tobias Malbrecht
 */
class ProgressDisplay {

	private int total = 0;
	private int completed = 0;
	private String label;
	private String message;

	private final ProgressListener progressListener = new ProgressListener() {
		@Override
		public void setCompleted(int completed) {
			checkCancelled();
			ProgressDisplay.this.completed = completed;
			if (ApplicationFrame.getApplicationFrame() != null) {
				ApplicationFrame.getApplicationFrame().getStatusBar().setProgress(label, ProgressDisplay.this.completed, total);
			}
			ProgressThreadDialog.getInstance().refreshDialog();
		}
	

		@Override		
		public void setTotal(int total) {		
			ProgressDisplay.this.total = total;
			setCompleted(completed);
			ProgressThreadDialog.getInstance().refreshDialog();
		}

		@Override
		public void complete() {			
			setCompleted(total);
			ProgressThreadDialog.getInstance().refreshDialog();
		}

		@Override
		public void setMessage(String message) {
			checkCancelled();
			ProgressDisplay.this.message = message;			
		}
		
	};
	private ProgressThread progressThread;

	public ProgressDisplay(String label) {
		this(label, null);
	}
	
	public ProgressDisplay(String label, ProgressThread progressThread) {
		super();
		this.label = label;
		this.progressThread = progressThread;
	}

	public ProgressListener getListener() {
		return progressListener;
	}

	public int getCompleted() {
		return completed;
	}
	public int getTotal() {
		return total;
	}
	
	public String getMessage() {
		return message;
	}
	
	public void setLabel(String label) {
		this.label = label;
	}
	
	private void checkCancelled() {
		if (progressThread != null) {
			progressThread.checkCancelled();
		}
	}
}
