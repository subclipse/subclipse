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
import java.util.List;

import org.tigris.subversion.subclipse.core.history.LogEntryChangePath;


/**
 * Node representing compressed folder on affected paths panel. 
 *
 * @author Eugene Kuleshov
 */
public class HistoryFolder {
    private final String path;
    private final char action;
    private final List children = new ArrayList();

    public HistoryFolder(String path, char action) {
        this.path = path;
        this.action = action;
    }

    public String getPath() {
        return path;
    }
    
    public char getAction() {
        return action;
    }

    public void add(LogEntryChangePath changePath) {
        children.add(changePath);
    }

    public Object[] getChildren() {
        return children.toArray();
    }
    
    public String toString() {
        return path + 
            (children.size()==0 ? "" : " ["+children.size()+"]");
    }

}

