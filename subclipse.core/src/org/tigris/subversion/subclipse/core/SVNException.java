/*******************************************************************************
 * Copyright (c) 2003, 2006 Subclipse project and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Subclipse project committers - initial API and implementation
 ******************************************************************************/
package org.tigris.subversion.subclipse.core;


import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.team.core.TeamException;
import org.tigris.subversion.svnclientadapter.SVNClientException;

/**
 * A checked expection representing a failure in the SVN plugin.
 * <p>
 * SVN exceptions contain a status object describing the cause of 
 * the exception.
 * </p>
 *
 * @see IStatus
 */
public class SVNException extends TeamException {
	private boolean operationInterrupted;

	/*
	 * Helpers for creating SVN exceptions
	 */
	public SVNException(int severity, int code, String message, Throwable e) {
		super(new SVNStatus(severity, code, message, null));
		operationInterrupted = (getMessage() != null && getMessage().indexOf(SVNClientException.OPERATION_INTERRUPTED) != -1);
	}
	
	public SVNException(int severity, int code, String message) {
		this(severity, code, message, null);
		operationInterrupted = (message != null && message.indexOf(SVNClientException.OPERATION_INTERRUPTED) != -1);
	}

	public SVNException(String message) {
		super(new SVNStatus(IStatus.ERROR, UNABLE, message, null));
		operationInterrupted = (message != null && message.indexOf(SVNClientException.OPERATION_INTERRUPTED) != -1);
	}
	
	public SVNException(String message, boolean operationInterrupted) {
		this(message);
		this.operationInterrupted = operationInterrupted;
	}

	public SVNException(String message, Throwable e) {
		this(IStatus.ERROR, UNABLE, message, e);
		operationInterrupted = (getMessage() != null && getMessage().indexOf(SVNClientException.OPERATION_INTERRUPTED) != -1);
	}

	public SVNException(IStatus status) {
		super(status);
		operationInterrupted = (status.getMessage() != null && status.getMessage().indexOf(SVNClientException.OPERATION_INTERRUPTED) != -1);
	}
	
	public boolean operationInterrupted() {
		return operationInterrupted;
	}

	/*
	 * Static helper methods for creating exceptions
	 */
	public static SVNException wrapException(IResource resource, String message, CoreException e) {
		return new SVNException(new SVNStatus(IStatus.ERROR, e.getStatus().getCode(), message, e));
	}

	/*
	 * Static helper methods for creating exceptions
	 */
	public static SVNException wrapException(Exception e) {
		Throwable t = e;
		if (e instanceof InvocationTargetException) {
			Throwable target = ((InvocationTargetException) e).getTargetException();
			if (target instanceof SVNException) {
				return (SVNException) target;
			}
			t = target;
		}
		return new SVNException(new SVNStatus(IStatus.ERROR, UNABLE, t.getMessage() != null ? t.getMessage() : "",	t)); //$NON-NLS-1$
	}
	
	public static SVNException wrapException(CoreException e) {
		IStatus status = e.getStatus();
		// If the exception is not a multi-status, wrap the exception to keep the original stack trace.
		// If the exception is a multi-status, the interesting stack traces should be in the childen already
		if ( ! status.isMultiStatus()) {
			status = new SVNStatus(status.getSeverity(), status.getCode(), status.getMessage(), e);
		}
		return new SVNException(status);
	}
	
	/*
	 * Static helper methods for creating exceptions
	 */
	public static SVNException wrapException(TeamException e) {
		if (e instanceof SVNException)
			return (SVNException)e;
		else
			return new SVNException(e.getStatus());
	}
	
	public CoreException toCoreException() {
		IStatus status = getStatus();
		return new CoreException(new Status(status.getSeverity(), status.getPlugin(), 0, status.getMessage(), this));
	}
}
