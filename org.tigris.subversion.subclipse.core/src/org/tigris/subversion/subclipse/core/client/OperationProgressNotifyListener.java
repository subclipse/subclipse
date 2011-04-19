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
import org.tigris.subversion.subclipse.core.SVNException;
import org.tigris.subversion.subclipse.core.SVNProviderPlugin;
import org.tigris.subversion.svnclientadapter.ISVNClientAdapter;
import org.tigris.subversion.svnclientadapter.ISVNProgressListener;
import org.tigris.subversion.svnclientadapter.SVNClientException;
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
	private ISVNClientAdapter svnClient;
	private File path;
	private SVNProgressEvent progressEvent;
	
	private long lastProgress;
	private long lastTotal;
	private long delta;
	private long grandTotal;
	
	public OperationProgressNotifyListener(final IProgressMonitor monitor)
	{
		super();
		this.monitor = monitor;
	}
	
	public OperationProgressNotifyListener(final IProgressMonitor monitor, ISVNClientAdapter svnClient)
	{
		this(monitor);
		this.svnClient = svnClient;
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
		this.clear();
		
		if (monitor != null)
		{
		    monitor.subTask(" ");
		}		
	}
	
	public void clear() {
		path = null;
		progressEvent = null;
		
		lastProgress = 0;
		lastTotal = 0;
		delta = 0;
		grandTotal = 0;
	}

	public IProgressMonitor getMonitor() {
		return monitor;
	}

	public void onProgress(SVNProgressEvent progressEvent) {
		
		if (monitor != null && monitor.isCanceled()) {
			if (svnClient != null) {
				try {
					svnClient.cancelOperation();
				} catch (SVNClientException e) {
					SVNProviderPlugin.log(SVNException.wrapException(e));
				}
			}
			return;
		}
		
		this.progressEvent = progressEvent;
		
		delta = progressEvent.getProgress();
		if (progressEvent.getProgress() >= lastProgress && progressEvent.getTotal() == lastTotal)
			delta = progressEvent.getProgress() - lastProgress;
		lastProgress = progressEvent.getProgress();
		lastTotal = progressEvent.getTotal();
		grandTotal += delta;

		if (monitor != null) {
		    subTask();
		}
	}

	private void subTask() {
		String subTask = null;
		if (progressEvent == null) subTask = path.getPath();
		else {
			Object t = null;
			String unit;
			if (grandTotal < 1000) {
				t = new Long(grandTotal);
				unit = " bytes"; //$NON-NLS-1$
			} else if (grandTotal < 1000000) {
				t = new Double(grandTotal/1000.0);
				unit = " kB"; //$NON-NLS-1$
			} else {
				t = new Double(grandTotal/1000000.0);
				unit = " MB"; //$NON-NLS-1$
			}
			String roundedTotal;
			if (t instanceof Double) {
				DecimalFormat df = new DecimalFormat("###.#"); //$NON-NLS-1$
				roundedTotal = df.format(((Double)t).doubleValue());
			} else roundedTotal = t.toString();
			if (path == null) subTask = roundedTotal + unit;
			else subTask = roundedTotal + unit + "\n" + path.getPath(); //$NON-NLS-1$
		}
		monitor.subTask(subTask);
	}
}
