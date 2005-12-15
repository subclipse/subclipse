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
package org.tigris.subversion.subclipse.core.history;


import java.util.Date;

import org.eclipse.core.runtime.IAdaptable;
import org.tigris.subversion.subclipse.core.ISVNRemoteResource;
import org.tigris.subversion.subclipse.core.ISVNResource;
import org.tigris.subversion.svnclientadapter.SVNRevision;

/**
 * Instances of ILogEntry represent an entry for a SVN file that results
 * from the svn log command.
 * 
 */
public interface ILogEntry extends IAdaptable {

	/**
	 * Get the revision for the entry
	 */
	public SVNRevision.Number getRevision();
	
	/**
	 * Get the author of the revision
	 */
	public String getAuthor();
	
	/**
	 * Get the date the revision was committed
	 */
	public Date getDate();
	
	/**
	 * Get the comment for the revision
	 */
	public String getComment();
	
	/**
	 * Get the remote file for this entry <br>
     * Returns null if this cannot be determined (which is the case
     * when we ask the history of a folder)
	 */
	public ISVNRemoteResource getRemoteResource();
	
	/**
     * get the resource for which we asked history 
	 */
    public ISVNResource getResource();
    
    /**
     * get the change paths
     */
    public LogEntryChangePath[] getLogEntryChangePaths();
    
    /**
     *  Get the tags for the revision
     */
    public Alias[] getTags();

    /**
     *  Set the tags for the revision
     */
    public void setTags(Alias[] tags);
    
}

