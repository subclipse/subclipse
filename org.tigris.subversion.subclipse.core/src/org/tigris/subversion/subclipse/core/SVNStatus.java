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
 
import org.eclipse.core.runtime.Status;

/*
 * A status object represents the outcome of an operation.
 * All <code>CoreException</code>s carry a status object to indicate 
 * what went wrong. Status objects are also returned by methods needing 
 * to provide details of failures (e.g., validation methods).
 */	
public class SVNStatus extends Status {

	/*** Status codes ***/
	public static final int SERVER_ERROR = -10;
	public static final int CONFLICT = -12;
	public static final int DOES_NOT_EXIST = -17;
	

	public SVNStatus(int severity, int code, String message, Throwable t) {
		super(severity, SVNProviderPlugin.ID, code, message, t);
	}
	
	public SVNStatus(int severity, int code, String message) {
		this(severity, code, message, null);
	}
	
	public SVNStatus(int severity, String message, Throwable t) {
		this(severity, message);
	}
	
	public SVNStatus(int severity, String message) {
		this(severity, severity, message, null);
	}
	/**
	 * @see IStatus#getMessage()
	 */
	public String getMessage() {
		String message = super.getMessage();
		return message;
	}

}
