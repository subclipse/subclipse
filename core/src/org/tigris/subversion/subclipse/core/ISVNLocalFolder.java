/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     Cédric Chabanois (cchabanois@ifrance.com)
 *******************************************************************************/
package org.tigris.subversion.subclipse.core;

import org.eclipse.core.runtime.IProgressMonitor;

/**
 * SVN local folder. SVN folders have access to synchronization information
 * that describes the association between the folder and the remote repository.
 *
 * @see ISVNFolder
 */
public interface ISVNLocalFolder extends ISVNLocalResource, ISVNFolder {

    /**
     * refresh the status of this folde and its children according to depth 
     * DEPTH_ZERO : only this folder
     * DEPTH_ONE  : this folder and its immediate children
     * DEPTH_INFINITE : this folder and its children (recursively)
     * @param depth
     * @see ISVNLocalResource#refreshStatus
     */
    public void refreshStatus(int depth);

    public void acceptChildren(ISVNResourceVisitor visitor) throws SVNException;
    
    /** 
     * unmanage the folder, ie delete its svn subdirectory. This is a recursive operation
     */
    public void unmanage(IProgressMonitor monitor) throws SVNException;

    /**
     * Add the following pattern to the file's parent ignore list
     */  
    public void setIgnoredAs(final String pattern) throws SVNException;

}
