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
package org.tigris.subversion.subclipse.ui.console;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.team.internal.ui.TeamUIPlugin;
import org.eclipse.ui.PartInitException;
import org.tigris.subversion.subclipse.core.SVNStatus;
import org.tigris.subversion.subclipse.core.client.IConsoleListener;
import org.tigris.subversion.subclipse.ui.Policy;
import org.tigris.subversion.subclipse.ui.SVNUIPlugin;

/**
 * The console listener
 */
class ConsoleListener implements IConsoleListener {
    private long commandStarted = 0;
    private static final DateFormat TIME_FORMAT = new SimpleDateFormat(Policy.bind("Console.resultTimeFormat")); //$NON-NLS-1$
        
    public void commandInvoked(String line) {
        commandStarted = System.currentTimeMillis();
        ConsoleView.appendConsoleLines(ConsoleDocument.DELIMITER, Policy.bind("Console.preExecutionDelimiter")); //$NON-NLS-1$
        ConsoleView.appendConsoleLines(ConsoleDocument.COMMAND, line);
    }
    
    public void messageLineReceived(String line) {
        ConsoleView.appendConsoleLines(ConsoleDocument.MESSAGE, "  " + line); //$NON-NLS-1$
    }
    
    public void errorLineReceived(String line) {
        ConsoleView.appendConsoleLines(ConsoleDocument.ERROR, "  " + line); //$NON-NLS-1$
        
        // we show the console view if something goes wrong
        ConsoleView.findInActivePerspective();
    }
    
    public void commandCompleted(IStatus status, Exception exception) {
        long commandRuntime = System.currentTimeMillis() - commandStarted;
        String time;
        try {
            time = TIME_FORMAT.format(new Date(commandRuntime));
        } catch (RuntimeException e) {
            SVNUIPlugin.log(new Status(IStatus.ERROR, SVNUIPlugin.ID, 0, Policy.bind("Console.couldNotFormatTime"), e)); //$NON-NLS-1$
            time = ""; //$NON-NLS-1$
        }
        String statusText;
        if (status != null) {
            if (status.getCode() == SVNStatus.SERVER_ERROR) {
                statusText = Policy.bind("Console.resultServerError", status.getMessage(), time); //$NON-NLS-1$
            } else {
                statusText = Policy.bind("Console.resultOk", time); //$NON-NLS-1$
            }
            ConsoleView.appendConsoleLines(ConsoleDocument.STATUS, statusText);
            IStatus[] children = status.getChildren();
            if (children.length == 0) {
                if (!status.isOK())
                    ConsoleView.appendConsoleLines(ConsoleDocument.STATUS, messageLineForStatus(status));
            } else {
                for (int i = 0; i < children.length; i++) {
                    if (!children[i].isOK())
                        ConsoleView.appendConsoleLines(ConsoleDocument.STATUS, messageLineForStatus(children[i]));
                }
            }
        } else if (exception != null) {
            if (exception instanceof OperationCanceledException) {
                statusText = Policy.bind("Console.resultAborted", time); //$NON-NLS-1$
            } else {
                statusText = Policy.bind("Console.resultException", time); //$NON-NLS-1$
            }
            ConsoleView.appendConsoleLines(ConsoleDocument.STATUS, statusText);
        } else {
            statusText = Policy.bind("Console.resultOk", time); //$NON-NLS-1$
        }
        ConsoleView.appendConsoleLines(ConsoleDocument.DELIMITER, Policy.bind("Console.postExecutionDelimiter")); //$NON-NLS-1$
        ConsoleView.appendConsoleLines(ConsoleDocument.DELIMITER, ""); //$NON-NLS-1$
        ConsoleView.flushConsoleBuffer();
    }
    
    /**
     * Method messageLineForStatus.
     * @param status
     */
    private String messageLineForStatus(IStatus status) {
        if (status.getSeverity() == IStatus.ERROR) {
            return Policy.bind("Console.error", status.getMessage()); //$NON-NLS-1$
        } else if (status.getSeverity() == IStatus.WARNING) {
            return Policy.bind("Console.warning", status.getMessage()); //$NON-NLS-1$
        } else if (status.getSeverity() == IStatus.INFO) {
            return Policy.bind("Console.info", status.getMessage()); //$NON-NLS-1$
        }
        return status.getMessage();
    }
}
