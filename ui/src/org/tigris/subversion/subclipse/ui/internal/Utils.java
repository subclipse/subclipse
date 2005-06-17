/*******************************************************************************
 * copied from: org.eclipse.team.internal.ui.Utils
 * 
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.tigris.subversion.subclipse.ui.internal;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.team.core.TeamException;
import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.progress.IWorkbenchSiteProgressService;
import org.tigris.subversion.subclipse.ui.Policy;
import org.tigris.subversion.subclipse.ui.SVNUIPlugin;

public class Utils {
	
	public static void schedule(Job job, IWorkbenchSite site) {
		if (site != null) {
			IWorkbenchSiteProgressService siteProgress = (IWorkbenchSiteProgressService) site.getAdapter(IWorkbenchSiteProgressService.class);
			if (siteProgress != null) {
				siteProgress.schedule(job, 0, true /* use half-busy cursor */);
				return;
			}
		}
		job.schedule();
	}
	
	public static byte[] readBytes(InputStream in) {
		ByteArrayOutputStream bos= new ByteArrayOutputStream();
		try {		
			while (true) {
				int c= in.read();
				if (c == -1)
					break;
				bos.write(c);
			}
					
		} catch (IOException ex) {
			return null;
		
		} finally {
			if (in != null) {
				try {
					in.close();
				} catch (IOException x) {
					// silently ignored
				}
			}
			try {
				bos.close();
			} catch (IOException x) {
				// silently ignored
			}
		}	
		return bos.toByteArray();
	}
	/*
	 * This method is only for use by the Target Management feature (see bug
	 * 16509). @param t
	 */
	public static void handle(final Throwable exception) {
		Utils.getStandardDisplay().asyncExec(new Runnable() {
			public void run() {
				IStatus error = null;
				Throwable t = exception;
				if (t instanceof InvocationTargetException) {
					t = ((InvocationTargetException) t).getTargetException();
				}
				if (t instanceof CoreException) {
					error = ((CoreException) t).getStatus();
				} else if (t instanceof TeamException) {
					error = ((TeamException) t).getStatus();
				} else {
					error = new Status(IStatus.ERROR, SVNUIPlugin.ID, 1, Policy.bind("simpleInternal"), t); //$NON-NLS-1$
				}
				Shell shell = new Shell(Display.getDefault());
				if (error.getSeverity() == IStatus.INFO) {
					MessageDialog.openInformation(shell, Policy.bind("information"), error.getMessage()); //$NON-NLS-1$
				} else {
					ErrorDialog.openError(shell, Policy.bind("exception"), null, error); //$NON-NLS-1$
				}
				shell.dispose();
				// Let's log non-team exceptions
				if (!(t instanceof TeamException)) {
					SVNUIPlugin.log(error.getSeverity(), error.getMessage(), t);
				}
			}
		});
	}
	
	/**
	 * Returns the standard display to be used. The method first checks, if
	 * the thread calling this method has an associated display. If so, this
	 * display is returned. Otherwise the method returns the default display.
	 */
	public static Display getStandardDisplay() {
		Display display= Display.getCurrent();
		if (display == null) {
			display= Display.getDefault();
		}
		return display;		
	}
	
	/**
	 * Convenience method for logging statuses to the plugin log
	 * 
	 * @param status  the status to log
	 */
	public static void log(IStatus status) {
		SVNUIPlugin.getPlugin().getLog().log(status);
	}
	
	/**
	 * Convenience method for logging a TeamException in such a way that the
	 * stacktrace is logged as well.
	 * @param e
	 */
	public static void log(CoreException e) {
		IStatus status = e.getStatus();
		log (status.getSeverity(), status.getMessage(), e);
	}
	
	/**
	 * Log the given exception along with the provided message and severity indicator
	 */
	public static void log(int severity, String message, Throwable e) {
		log(new Status(severity, SVNUIPlugin.ID, 0, message, e));
	}
	/**
	 * Shows the given errors to the user.
	 * @param Exception
	 *            the exception containing the error
	 * @param title
	 *            the title of the error dialog
	 * @param message
	 *            the message for the error dialog
	 * @param shell
	 *            the shell to open the error dialog in
	 */
	public static void handleError(Shell shell, Exception exception, String title, String message) {
		IStatus status = null;
		boolean log = false;
		boolean dialog = false;
		Throwable t = exception;
		if (exception instanceof TeamException) {
			status = ((TeamException) exception).getStatus();
			log = false;
			dialog = true;
		} else if (exception instanceof InvocationTargetException) {
			t = ((InvocationTargetException) exception).getTargetException();
			if (t instanceof TeamException) {
				status = ((TeamException) t).getStatus();
				log = false;
				dialog = true;
			} else if (t instanceof CoreException) {
				status = ((CoreException) t).getStatus();
				log = true;
				dialog = true;
			} else if (t instanceof InterruptedException) {
				return;
			} else {
				status = new Status(IStatus.ERROR, SVNUIPlugin.ID, 1, Policy.bind("TeamAction.internal"), t); //$NON-NLS-1$
				log = true;
				dialog = true;
			}
		}
		if (status == null)
			return;
		if (!status.isOK()) {
			IStatus toShow = status;
			if (status.isMultiStatus()) {
				IStatus[] children = status.getChildren();
				if (children.length == 1) {
					toShow = children[0];
				}
			}
			if (title == null) {
				title = status.getMessage();
			}
			if (message == null) {
				message = status.getMessage();
			}
			if (dialog && shell != null) {
				ErrorDialog.openError(shell, title, message, toShow);
			}
			if (log || shell == null) {
				SVNUIPlugin.log(toShow.getSeverity(), message, t);
			}
		}
	}

}
