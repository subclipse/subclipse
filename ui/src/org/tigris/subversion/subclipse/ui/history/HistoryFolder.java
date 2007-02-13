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
package org.tigris.subversion.subclipse.ui.history;

import java.util.ArrayList;
import java.util.List;

import org.tigris.subversion.subclipse.core.history.LogEntryChangePath;
import org.tigris.subversion.svnclientadapter.SVNRevision.Number;


/**
 * Node representing compressed folder on affected paths panel. 
 *
 * @author Eugene Kuleshov
 */
public class HistoryFolder {
    private final String path;
    private final char action;
    private final String copySrcPath;
    private final Number copySrcRevision;
    private final List children = new ArrayList();

    public HistoryFolder(String path) {
        this.path = path;
        this.action = '?';
        this.copySrcPath = null;
        this.copySrcRevision = null;
    }

    public HistoryFolder(LogEntryChangePath changePath) {
        this.path = changePath.getPath();
        this.action = changePath.getAction();
        this.copySrcPath = changePath.getCopySrcPath();
        this.copySrcRevision = changePath.getCopySrcRevision();
    }

    public String getPath() {
        return path;
    }
    
    public char getAction() {
        return action;
    }
    
    public String getCopySrcPath() {
        return copySrcPath;
    }
    
    public Number getCopySrcRevision() {
        return copySrcRevision;
    }

    public void add(LogEntryChangePath changePath) {
        children.add(changePath);
    }

    public Object[] getChildren() {
        return children.toArray();
    }
    
    public int getChildCount() {
        return children.size();
    }
    
}

