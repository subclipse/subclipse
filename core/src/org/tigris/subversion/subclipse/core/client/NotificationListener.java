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
import org.tigris.subversion.svnclientadapter.StandardNotificationHandler;

/**
 * This class listen to notifications from jsvn and redirect them to the console listener
 */
public class NotificationListener extends StandardNotificationHandler {

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

	public void setCommandLine(String commandLine) {
		super.setCommandLine(commandLine);
		consoleListener.commandInvoked(commandLine);
	}

	protected void log(int logType, String message) {
		switch (logType) {
			case NotificationListener.LOG_MESSAGE : consoleListener.messageLineReceived(message); break;
			case NotificationListener.LOG_ERROR : consoleListener.errorLineReceived(message); break;
			case NotificationListener.LOG_COMPLETED : consoleListener.messageLineReceived(message);  break;
		}
	}	

}
