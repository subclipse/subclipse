/*******************************************************************************
 * Copyright (c) 2003, 2006 Subclipse project and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Subclipse project committers - initial API and implementation
 ******************************************************************************/
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
    
    /**
     * Returns the number of child log messages.  When merge-sensitive
     * log option was specified.
     * @return the number of revisions merged by this commit
     */

    public long getNumberOfChildren();
    
    /**
     * Returns the child log messages.  When merge-sensitive
     * log option was specified.
     * @return the revisions merged by this commit
     */
    public ILogEntry[] getChildMessages(); 
    
    /**
     * Returns the merged revisions as a String
     * when merge-sensitive log option was specified.
     * @return the revisions merged by this commit in String form.
     */
    public String getMergedRevisionsAsString();    
    
}

