/*******************************************************************************
 * Copyright (c) 2005, 2006 Subclipse project and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Subclipse project committers - initial API and implementation
 ******************************************************************************/
package org.tigris.subversion.subclipse.core.client;

import java.io.File;

import org.eclipse.core.runtime.IProgressMonitor;
import org.tigris.subversion.svnclientadapter.SVNNodeKind;

/**
 * ISVNNotifyListener implementation which intercepts the some log*() methods
 * and uses them to display the operation progress within the supplied progress monitor.
 * 
 * @author Martin Letenay (letenay at tigris.org)
 */
public class OperationProgressNotifyListener extends ISVNNotifyAdapter {

	private IProgressMonitor monitor = null;
	
	public OperationProgressNotifyListener(final IProgressMonitor monitor)
	{
		super();
		this.monitor = monitor;
	}
	
	/**
	 * Display the log the message in the progress monitor and increase the progress by 1
	 */
	public void onNotify(File path, SVNNodeKind kind) {
		if (monitor != null)
		{
		    monitor.worked(1);
		    monitor.subTask(path.getPath());
		}
	}

	/**
	 * The operation was completed, clear the progress' subTask.
	 */
	public void logCompleted(String message) {
		if (monitor != null)
		{
		    monitor.subTask(" ");
		}		
	}

	public IProgressMonitor getMonitor() {
		return monitor;
	}
}
