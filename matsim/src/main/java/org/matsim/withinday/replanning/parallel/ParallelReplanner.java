/* *********************************************************************** *
 * project: org.matsim.*
 * ParallelReplanner.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package org.matsim.withinday.replanning.parallel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

import org.apache.log4j.Logger;
import org.matsim.core.gbl.Gbl;
import org.matsim.withinday.replanning.identifiers.interfaces.AgentsToReplanIdentifier;
import org.matsim.withinday.replanning.replanners.interfaces.WithinDayReplanner;
import org.matsim.withinday.replanning.replanners.tools.ReplanningTask;

/*
 * Abstract class that contains the basic elements that are needed
 * to do parallel replanning within the QueueSimulation.
 *
 * Features like the creation of parallel running threads and the
 * split up of the replanning actions have to be implemented in
 * the subclasses.
 * 
 * Please ensure that each ParallelReplanner is registered as 
 * a SimulationListener to the Controller!
 */
public abstract class ParallelReplanner<T extends WithinDayReplanner<? extends AgentsToReplanIdentifier>> { 

	private final static Logger log = Logger.getLogger(ParallelReplanner.class);

	protected int numOfThreads = 1;	// use by default only one thread
	protected List<T> originalReplanners = new ArrayList<T>();
	protected ReplanningThread[] replanningThreads;
	protected int roundRobin = 0;
	private int lastRoundRobin = 0;
	protected CyclicBarrier timeStepStartBarrier;
	protected CyclicBarrier betweenReplannerBarrier;
	protected CyclicBarrier timeStepEndBarrier;

	public ParallelReplanner(int numOfThreads) {
		this.setNumberOfThreads(numOfThreads);
	}

	public final void init(String replannerName) {
		replanningThreads = new InternalReplanningThread[numOfThreads];

		// Do initial Setup of the Threads
		for (int i = 0; i < numOfThreads; i++) {
			ReplanningThread replanningThread = new InternalReplanningThread(replannerName + " Thread" + i + " replanned plans: ");
			replanningThread.setName(replannerName + i);
			replanningThreads[i] = replanningThread;
		}
		
		this.timeStepStartBarrier = new CyclicBarrier(numOfThreads + 1);
		this.betweenReplannerBarrier = new CyclicBarrier(numOfThreads);
		this.timeStepEndBarrier = new CyclicBarrier(numOfThreads + 1);

		// finalize Thread Setup
		for (int i = 0; i < numOfThreads; i++) {
			ReplanningThread replanningThread = replanningThreads[i];

			replanningThread.setCyclicTimeStepStartBarrier(this.timeStepStartBarrier);
			replanningThread.setBetweenReplannerBarrier(betweenReplannerBarrier);
			replanningThread.setCyclicTimeStepEndBarrier(this.timeStepEndBarrier);
			replanningThread.setDaemon(true);

			replanningThread.start();
		}

		/*
		 * After initialization the Threads are waiting at the
		 * TimeStepEndBarrier. We trigger this Barrier once so
		 * they wait at the TimeStepStartBarrier what has to be
		 * their state if the run() method is called.
		 */
		try {
			this.timeStepEndBarrier.await();
		} catch (InterruptedException e) {
			Gbl.errorMsg(e);
		} catch (BrokenBarrierException e) {
			Gbl.errorMsg(e);
		}
	}

	/*
	 * Typical Implementations should be able to use this Method
	 * "as it is"...
	 */
	public final void run(double time) {
		// no Agents to Replan
		if (lastRoundRobin == roundRobin) return;
		else lastRoundRobin = roundRobin;

		try {
			// set current Time & AgentCounter
			for (ReplanningThread replannerThread : replanningThreads) {
				replannerThread.setTime(time);
			}

			this.timeStepStartBarrier.await();

			this.timeStepEndBarrier.await();

		} catch (InterruptedException e) {
			Gbl.errorMsg(e);
		} catch (BrokenBarrierException e) {
	      	Gbl.errorMsg(e);
		}
	}

	public final void addWithinDayReplanner(T replanner) {
		this.originalReplanners.add(replanner);

		for (ReplanningThread replanningThread : this.replanningThreads) {
			WithinDayReplanner<? extends AgentsToReplanIdentifier> newInstance = replanner.getReplannerFactory().createReplanner();
			replanningThread.addWithinDayReplanner(newInstance);
		}
	}

	public final List<T> getWithinDayReplanners() {
		return Collections.unmodifiableList(this.originalReplanners);
	}

	public final void addReplanningTask(ReplanningTask replanningTask) {
		this.replanningThreads[this.roundRobin % this.numOfThreads].addReplanningTask(replanningTask);
		this.roundRobin++;
	}

	private final void setNumberOfThreads(int numberOfThreads) {
		numOfThreads = Math.max(numberOfThreads, 1); // it should be at least 1 here; we allow 0 in other places for "no threads"

		log.info("Using " + numOfThreads + " parallel threads to replan routes.");

		/*
		 *  Throw error message if the number of threads is bigger than the number of available CPUs.
		 *  This should not speed up calculation anymore.
		 */
		if (numOfThreads > Runtime.getRuntime().availableProcessors()) {
			log.error("The number of parallel running replanning threads is bigger than the number of available CPUs/Cores!");
		}
	}
		
	/*
	 * The thread class that really handles the replanning.
	 */
	/*package*/ static final class InternalReplanningThread extends ReplanningThread {		
		
		public InternalReplanningThread(String counterText) {
			super(counterText);
		}
				
	}	// InternalReplanningThread
}
