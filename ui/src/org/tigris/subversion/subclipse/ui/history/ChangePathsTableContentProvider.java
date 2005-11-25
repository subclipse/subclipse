/******************************************************************************
 * This program and the accompanying materials are made available under
 * the terms of the Common Public License v1.0 which accompanies this
 * distribution, and is available at the following URL:
 * http://www.eclipse.org/legal/cpl-v10.html
 * Copyright(c) 2003-2005 by the authors indicated in the @author tags.
 *
 * All Rights are Reserved by the various authors.
 *******************************************************************************/
package org.tigris.subversion.subclipse.ui.history;

import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.tigris.subversion.subclipse.core.SVNProviderPlugin;
import org.tigris.subversion.subclipse.core.history.ILogEntry;
import org.tigris.subversion.subclipse.core.history.LogEntryChangePath;

/**
 * Flat table representation of the affected paths panel.
 */
class ChangePathsTableContentProvider implements IStructuredContentProvider {
    private static final LogEntryChangePath[] EMPTY_CHANGE_PATHS = new LogEntryChangePath[0];
    
    private final HistoryView view;

    ChangePathsTableContentProvider(HistoryView view) {
        this.view = view;
    }

    public Object[] getElements(Object inputElement) {
        if (!view.isShowChangePaths() || !(inputElement instanceof ILogEntry)) {
            return EMPTY_CHANGE_PATHS;
        }
    	
    	ILogEntry logEntry = (ILogEntry)inputElement;
        if (SVNProviderPlugin.getPlugin().getSVNClientManager().isFetchChangePathOnDemand()) {
            if (view.currentLogEntryChangePath != null) {
                return this.view.currentLogEntryChangePath;
            }
            view.scheduleFetchChangePathJob(logEntry);
            return EMPTY_CHANGE_PATHS;
        }

        return logEntry.getLogEntryChangePaths();
    }

    public void dispose() {
    }

    public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
    	this.view.currentLogEntryChangePath = null;
    }
    
}

