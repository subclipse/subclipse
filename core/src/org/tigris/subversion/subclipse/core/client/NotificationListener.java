/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     Cédric Chabanois (cchabanois@ifrance.com) - modified for Subversion 
 *******************************************************************************/
package org.tigris.subversion.subclipse.core.client;

import org.tigris.subversion.subclipse.core.SVNProviderPlugin;
import org.tigris.subversion.svnclientadapter.ISVNNotifyListener;
import org.tigris.subversion.svnclientadapter.SVNNodeKind;

/**
 * This class listen to notifications from jsvn and redirect them to the console listener
 */
public class NotificationListener implements ISVNNotifyListener {

	private IConsoleListener consoleListener;
    private static NotificationListener instance;

    /*
     * private contructor 
     */
    private NotificationListener() {
        consoleListener = SVNProviderPlugin.getPlugin().getConsoleListener();     
    }
    
    /**
     * Returns the singleton instance
     */
    public static NotificationListener getInstance() {      
        if(instance==null) {
            instance = new NotificationListener();
        }
        return instance;
    }

	/* (non-Javadoc)
	 * @see org.tigris.subversion.svnclientadapter.ISVNNotifyListener#logCommandLine(java.lang.String)
	 */
	public void logCommandLine(String commandLine) {
        consoleListener.logCommandLine(commandLine);
	}

	/* (non-Javadoc)
	 * @see org.tigris.subversion.svnclientadapter.ISVNNotifyListener#logCompleted(java.lang.String)
	 */
	public void logCompleted(String message) {
        consoleListener.logCompleted(message);
	}

	/* (non-Javadoc)
	 * @see org.tigris.subversion.svnclientadapter.ISVNNotifyListener#logError(java.lang.String)
	 */
	public void logError(String message) {
        consoleListener.logError(message);
	}

	/* (non-Javadoc)
	 * @see org.tigris.subversion.svnclientadapter.ISVNNotifyListener#logMessage(java.lang.String)
	 */
	public void logMessage(String message) {
		consoleListener.logMessage(message);
	}

	/* (non-Javadoc)
	 * @see org.tigris.subversion.svnclientadapter.ISVNNotifyListener#onNotify(java.lang.String, org.tigris.subversion.svnclientadapter.SVNNodeKind)
	 */
	public void onNotify(String path, SVNNodeKind kind) {
		consoleListener.onNotify(path,kind);
	}

	/* (non-Javadoc)
	 * @see org.tigris.subversion.svnclientadapter.ISVNNotifyListener#setCommand(int)
	 */
	public void setCommand(int command) {
        consoleListener.setCommand(command);
	}

}
