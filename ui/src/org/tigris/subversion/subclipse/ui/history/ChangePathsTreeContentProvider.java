/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * Copyright(c) 2003-2005 by the authors indicated in the @author tags.
 *
 * All Rights are Reserved by the various authors.
 *******************************************************************************/
package org.tigris.subversion.subclipse.ui.history;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeMap;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.tigris.subversion.subclipse.core.SVNProviderPlugin;
import org.tigris.subversion.subclipse.core.history.ILogEntry;
import org.tigris.subversion.subclipse.core.history.LogEntryChangePath;

/**
 * Tree content provider for affected paths panel viewer
 *
 * @author Eugene Kuleshov
 */
class ChangePathsTreeContentProvider implements ITreeContentProvider {
    private static final Object[] EMPTY_CHANGE_PATHS = new Object[0];

    private final HistoryView view;

    public ChangePathsTreeContentProvider( HistoryView view) {
        this.view = view;
    }

    public Object[] getChildren(Object parentElement) {
        if(parentElement instanceof HistoryFolder) {
          return ((HistoryFolder) parentElement).getChildren();
        }
        return null;
    }

    public Object getParent(Object element) {
        return null;
    }

    public boolean hasChildren(Object element) {
        if(element instanceof HistoryFolder) {
            HistoryFolder folder = (HistoryFolder) element;
            return folder.getChildren().length>0;
        }
        return false;
    }

    public Object[] getElements(Object inputElement) {
        if (!view.isShowChangePaths() ||
            !(inputElement instanceof ILogEntry)) {
            return EMPTY_CHANGE_PATHS;
        }

        if (view.currentLogEntryChangePath != null) {
            
        }
        
        ILogEntry logEntry = (ILogEntry)inputElement;
        if (SVNProviderPlugin.getPlugin().getSVNClientManager().isFetchChangePathOnDemand()) {
            if (view.currentLogEntryChangePath != null) {
                return getGroups(this.view.currentLogEntryChangePath);
            }
            view.scheduleFetchChangePathJob(logEntry);
            return EMPTY_CHANGE_PATHS;
        }

        return getGroups(logEntry.getLogEntryChangePaths());
    }
    
    private Object[] getGroups(LogEntryChangePath[] changePaths) {
        // 1st pass. Collect folder names
        Set folderNames = new HashSet(); 
        for( int i = 0; i < changePaths.length; i++) {
          folderNames.add(getFolderName(changePaths[i]));
        }
        
        // 2nd pass. Sorting out explicitly changed folders
        TreeMap folders = new TreeMap();
        for( int i = 0; i < changePaths.length; i++) {
          LogEntryChangePath changePath = changePaths[i];
        String path = changePath.getPath();
          if(folderNames.contains(path)) {
            // changed folder
            HistoryFolder folder = (HistoryFolder) folders.get(path);
            if(folder==null) {
              folder = new HistoryFolder(changePath);
              folders.put(path, folder);
            }
          } else {
            // changed resource
            path = getFolderName(changePath);
            HistoryFolder folder = (HistoryFolder) folders.get(path);
            if(folder==null) {
              folder = new HistoryFolder(path);
              folders.put(path, folder);
            }
            folder.add(changePath);
          }
        }
        
        // 3rd pass. Optimize folders with one or no children 
        ArrayList groups = new ArrayList();
        for( Iterator it = folders.values().iterator(); it.hasNext();) {
            HistoryFolder folder = (HistoryFolder) it.next();
            Object[] children = folder.getChildren();
            if(children.length==1) {
                LogEntryChangePath changePath = (LogEntryChangePath)children[0];
                groups.add(new HistoryFolder(changePath));
            } else if(children.length>1) {
                groups.add(folder);
            }
        }
        
        return groups.toArray();
    }

    private String getFolderName(LogEntryChangePath changePath) {
      String path = changePath.getPath();
      int n = path.lastIndexOf('/');
      return n>-1 ? path.substring(0, n) : path;
    }

    public void dispose() {
    }

    public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
        this.view.currentLogEntryChangePath = null;
    }
    
}
