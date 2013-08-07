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
package com.rapidminer.gui.tour;

import java.util.LinkedList;
import java.util.List;

/** A tour consisting of multiple {@link Step}s explaining the usage of RapidMiner
 *  or an Extension.
 *  
 *   Implementations of tour must implement a default (no-arg) constructor since
 *   they are created reflectively.
 * 
 * 
 * @author Thilo Kamradt
 *
 */
public abstract class IntroductoryTour {
	
	public static interface TourListener {

		/**
		 * will be called by a {@link Step} if the Tour was closed or is finished
		 */
		public void tourClosed();

	}

	private int maxSteps;
	
	/**
	 * This Array has to be filled with Subclasses of {@link Step} which will guide the Tour.
	 */
	protected Step[] step;

	private String tourKey;

	private boolean completeWindow;

	private List<TourListener> listeners;
	
	private Step head;

	/**
	 * This Constructor will initialize the {@link Step} step[] which has to be filled in the buildTour() Method and adds automatically a {@link FinalStep} to the end of your tour.
	 * 
	 * @param max number of steps you want to perform (size of the Array you want to fill)
	 * @param tourName name of the your tour (will be used as key as well)
	 */
	public IntroductoryTour(int steps, String tourName) {
		this(steps, tourName, true);
	}

	/**
	 * This Constructor will initialize the {@link Step} step[] which has to be filled in the buildTour() Method
	 * 
	 * @param steps number of Steps you will do (size of the Array you want to fill)
	 * @param tourName name of the your tour (will be used as key as well)
	 * @param addComppleteWindow indicates whether a {@link FinalStep} with will be added or not.
	 */
	public IntroductoryTour(int steps, String tourName, boolean addComppleteWindow) {
		this.tourKey = tourName;
		this.completeWindow = addComppleteWindow;
		this.maxSteps = steps;
		this.listeners = new LinkedList<TourListener>();
	}

	/**
	 * method to initializes the needed Array of {@link Step} and the FinalStep if wanted
	 */
	private void init() {
			step = new Step[maxSteps];
	}

	/**
	 * starts the Tour
	 */
	public void startTour() {
		init();
		buildTour();
		placeFollowers();
		head.start();
	}

	/**
	 * This method fills the step[] instances of subclasses of {@link Step} which will guide through the tour
	 */
	protected abstract void buildTour();

	/**
	 * method to get the key and name of the Tour.
	 * @return String with key of the Tour
	 */
	public String getKey() {
		return tourKey;
	}

	/**
	 * This method connects the single steps to a queue and the the needed parameters to the steps.
	 * After calling this method the isFinal-, tourKey-, index- and listeners-parameter of Step is set.
	 */
	private void placeFollowers() {
		Step tail;
		int counter = 1;
		Step[] currentPreconditions = step[0].getPreconditions();
		head = ((currentPreconditions.length == 0) ? step[0] : currentPreconditions[0]);
		tail = head;
		head.makeSettings(tourKey, ((currentPreconditions.length == 0) ? counter++ : counter), this.getSize(), false, listeners);
		//iterate over Array and create a queue
		for (int i = (counter - 1); i < step.length; i++) {
			//enqueue the Preconditions
			currentPreconditions = step[i].getPreconditions();
			for (int j = 0; j < currentPreconditions.length; j++) {
					if(i == 0 && j == 0) {
						continue;
					}
					currentPreconditions[j].makeSettings(tourKey, counter , this.getSize(), false, listeners);
					tail.setNext(currentPreconditions[j]);
					tail = tail.getNext();
			}
			// add the current Step to the queue and set the next step
			tail.setNext(step[i]);
			tail = tail.getNext();
			if(!completeWindow && i == (step.length - 1)){
				//this is the final step
				tail.makeSettings(tourKey, counter++ , this.getSize(), true, listeners);
			} else {
				//this is just a step
				tail.makeSettings(tourKey, counter++ , this.getSize(), false, listeners);
			}
		}
		if(completeWindow) {
			tail.setNext(new FinalStep(tourKey));
			tail.getNext().makeSettings(tourKey, counter++ , this.getSize(), true, listeners);
		}
	}
	
//	private void placeFollowers() {
//		Step tail;
//		Step[] currentPreconditions = step[0].getPreconditions();
//		Step[] nextPreconditions;
//		tail = head;
//		// set head and tail of the queue
//		if(currentPreconditions.length == 0) {
//			head = step[0];
//			tail = head;
//		} else {
//			head = currentPreconditions[0];
//			tail = head;
//			if(currentPreconditions.length != 1) {
//				for(int h = 1; h < currentPreconditions.length; h++) {
//					tail.setNext(currentPreconditions[h]);
//					tail = currentPreconditions[h];
//				}
//			}
//			tail.setNext(step[0]);
//			tail = step[0];
//		}
//		//iterate over Array and create a queue
//		for (int i = 1; i < step.length; i++) {
//			//enqueue the Preconditions
//			currentPreconditions = step[i].getPreconditions();
//			if(currentPreconditions.length != 0) {
//				for (int j = 0; j < currentPreconditions.length; j++) {
//					
//					currentPreconditions[j].makeSettings(tourKey, i + 1, this.getSize(), false, listeners);
//					if(j == (currentPreconditions.length -1)) {
//						currentPreconditions[j].setNext(step[i]);
//					} else {
//						currentPreconditions[j].setNext(currentPreconditions[j + 1]);
//					}
//				}
//			}
//			// add the current Step to the queue and set the next step
//			if(i <= (step.length - 2)) {
//				step[i].makeSettings(tourKey, i + 1, this.getSize(), false, listeners);
//				nextPreconditions = step[i +1].getPreconditions();
//				step[i].setNext(nextPreconditions.length == 0 ? step[i +1] : nextPreconditions[0]);
//			} else {
//				if(completeWindow){
//					step[i].makeSettings(tourKey, i + 1, this.getSize(), false, listeners);
//					step[i].setNext(new FinalStep(tourKey));
//					step[i].getNext().makeSettings(tourKey, maxSteps, this.getSize(), true, listeners);
//				} else {
//					step[i].makeSettings(tourKey, maxSteps, this.getSize(), true, listeners);
//				}
//			}
//		}
//	}

	/**
	 * Adds a {@link TourListener} to the IntroductoryTour and to all {@link Step}s of the Tour.
	 * @param listener TourListener
	 */
	public void addListener(TourListener listener) {
		this.listeners.add(listener);
	}

	/**
	 * 
	 * @return returns the size of the Tour including the {@link FinalStep} if the flag was set.
	 */
	public int getSize() {
		return (completeWindow ? this.maxSteps +1 : maxSteps);
	}
}
