/*******************************************************************************
 * copied from: org.eclipse.team.internal.core.InfiniteSubProgressMonitor
 * 
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.tigris.subversion.subclipse.core.internal;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;

/**
 * Provides an infinite progress monitor by subdividing by half repeatedly.
 * 
 * The ticks parameter represents the number of ticks shown in the progress dialog
 * (or propogated up to a parent IProgressMonitor). The totalWork parameter provided
 * in actually a hint used to determine how work is translated into ticks.
 * The number of totalWork that can actually be worked is n*totalWork/2 where
 * 2^n = totalWork. What this means is that if you provide a totalWork of 32 (2^5) than
 * the maximum number of ticks is 5*32/2 = 80.
 * 
 */
public class InfiniteSubProgressMonitor extends SubProgressMonitor {

	int totalWork;
	int halfWay;
	int currentIncrement;
	int nextProgress;
	int worked;
		
	/**
	 * Constructor for InfiniteSubProgressMonitor.
	 * @param monitor
	 * @param ticks
	 */
	public InfiniteSubProgressMonitor(IProgressMonitor monitor, int ticks) {
		this(monitor, ticks, 0);
	}

	/**
	 * Constructor for InfiniteSubProgressMonitor.
	 * @param monitor
	 * @param ticks
	 * @param style
	 */
	public InfiniteSubProgressMonitor(IProgressMonitor monitor, int ticks, int style) {
		super(monitor, ticks, style);
	}
	
	public void beginTask(String name, int total) {
		super.beginTask(name, total);
		this.totalWork = total;
		this.halfWay = total / 2;
		this.currentIncrement = 1;
		this.nextProgress = currentIncrement;
		this.worked = 0;
	}
	
	public void worked(int work) {
		if (worked >= totalWork) return;
		if (--nextProgress <= 0) {
			super.worked(1);
			worked++;
			if (worked >= halfWay) {
				// we have passed the current halfway point, so double the
				// increment and reset the halfway point.
				currentIncrement *= 2;
				halfWay += (totalWork - halfWay) / 2;				
			}
			// reset the progress counter to another full increment
			nextProgress = currentIncrement;
		}			
	}

	/**
	 * Don't allow clearing of the subtask. This will stop the flickering
	 * of the subtask in the progress dialogs.
	 * 
	 * @see IProgressMonitor#subTask(String)
	 */
	public void subTask(String name) {
		if(name != null && ! name.equals("")) { //$NON-NLS-1$
			super.subTask(name);
		}
	}
}
