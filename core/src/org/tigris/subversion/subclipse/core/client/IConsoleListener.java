/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Cédric Chabanois (cchabanois@ifrance.com) - modified for Subversion 
 *******************************************************************************/
package org.tigris.subversion.subclipse.core.client;

import org.eclipse.core.runtime.IStatus;

public interface IConsoleListener {
	/**
	 * Called when a command is invoked.
	 * @param line the command invocation string
	 */
	public void commandInvoked(String line);
	
	/**
	 * Called when a line of message text has been received.
	 * @param line the line of text
	 */
	public void messageLineReceived(String line);
	
	/**
	 * Called when a line of error text has been received.
	 * @param line the line of text
	 */
	public void errorLineReceived(String line);
	
	/**
	 * Called when a command has been completed.
	 * @param status the status code, or null if not applicable
	 * @param exception an exception, or null if not applicable
	 */
	public void commandCompleted(IStatus status, Exception exception);
}
