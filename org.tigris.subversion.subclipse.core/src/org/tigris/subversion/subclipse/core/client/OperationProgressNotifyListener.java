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
import java.text.DecimalFormat;

import org.eclipse.core.runtime.IProgressMonitor;
import org.tigris.subversion.svnclientadapter.ISVNProgressListener;
import org.tigris.subversion.svnclientadapter.SVNNodeKind;
import org.tigris.subversion.svnclientadapter.SVNProgressEvent;

/**
 * ISVNNotifyListener implementation which intercepts the some log*() methods
 * and uses them to display the operation progress within the supplied progress monitor.
 * 
 * @author Martin Letenay (letenay at tigris.org)
 */
public class OperationProgressNotifyListener extends ISVNNotifyAdapter implements ISVNProgressListener {

	private IProgressMonitor monitor = null;
	private File path;
	private SVNProgressEvent progressEvent;
	private long totalBytes;
	private long subtotalBytes;
	
	public OperationProgressNotifyListener(final IProgressMonitor monitor)
	{
		super();
		this.monitor = monitor;
	}
	
	/**
	 * Display the log the message in the progress monitor and increase the progress by 1
	 */
	public void onNotify(File path, SVNNodeKind kind) {
		this.path = path;
		if (monitor != null)
		{
		    monitor.worked(1);
		    subTask();
		}
	}

	/**
	 * The operation was completed, clear the progress' subTask.
	 */
	public void logCompleted(String message) {
		path = null;
		progressEvent = null;
		totalBytes = 0;
		subtotalBytes = 0;
		if (monitor != null)
		{
		    monitor.subTask(" ");
		}		
	}

	public IProgressMonitor getMonitor() {
		return monitor;
	}

	public void onProgress(SVNProgressEvent progressEvent) {
		this.progressEvent = progressEvent;
		if (progressEvent.getTotal() == SVNProgressEvent.UNKNOWN) {
			subtotalBytes = subtotalBytes + progressEvent.getProgress();
		} else {
			subtotalBytes = 0;
			totalBytes = totalBytes + progressEvent.getProgress();
		}
		if (monitor != null) {
		    subTask();
		}
	}

	private void subTask() {
		String subTask = null;
		if (progressEvent == null) subTask = path.getPath();
		else {
			long grandTotal = totalBytes + subtotalBytes;
			Object t = null;
			String unit;
			if (grandTotal < 1024) {
				t = new Long(grandTotal);
				unit = " bytes"; //$NON-NLS-1$
			} else if (grandTotal < 1200000) {
				t = new Double(grandTotal/1024.0);
				unit = "KB"; //$NON-NLS-1$
			} else {
				t = new Double(grandTotal/1048576.0);
				unit = "MB"; //$NON-NLS-1$
			}
			String roundedTotal;
			if (t instanceof Double) {
				DecimalFormat df = new DecimalFormat("###.###"); //$NON-NLS-1$
				roundedTotal = df.format(((Double)t).doubleValue());
			} else roundedTotal = t.toString();
			if (path == null) subTask = roundedTotal + unit;
			else subTask = roundedTotal + unit + "\n" + path.getPath(); //$NON-NLS-1$
		}
		monitor.subTask(subTask);
	}
}
