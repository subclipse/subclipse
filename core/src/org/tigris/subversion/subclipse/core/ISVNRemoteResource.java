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
package org.tigris.subversion.subclipse.core;

 
import java.util.Date;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.core.variants.IResourceVariant;
import org.tigris.subversion.subclipse.core.history.ILogEntry;
import org.tigris.subversion.svnclientadapter.SVNRevision;

/**
 * The interface represents a resource that exists in a SVN repository.
 * It purpose is to provide information about the remote resource from
 * the repository.
 * 
 * Clients are not expected to implement this interface.
 */
public interface ISVNRemoteResource extends ISVNResource, IAdaptable, IResourceVariant {
	
	/**
	 * Does the remote resource represented by this handle exist on the server. This
	 * method may contact the server and be long running.
	 */
	public boolean exists(IProgressMonitor monitor) throws TeamException;
	
	/**
	 * Answers the repository relative path of this remote folder.
	 */
	public String getRepositoryRelativePath();
	
	/**
	 * Compares two objects for equality; for svn emote resources, equality is defined in 
	 * terms of their handles: same url, and identical revision numbers. 
     * Remote resources are not equal to objects other 
	 * than svn remote resources.
	 *
	 * @param other the other object
	 * @return an indication of whether the objects are equals
	 */
	public boolean equals(Object other);

    /**
     * @return the last changed revision of this remote resource
     */
    public SVNRevision.Number getLastChangedRevision();

    /**
     * @return the revision of this remote resource
     */
    public SVNRevision getRevision();

    /**
     * @return the date of modification for this remote resource
     * @return null if this date is not available
     */
    public Date getDate();

    /**
     * @return the author of this remote resource
     * @return null if the author is not available
     */
    public String getAuthor();

    /**
     * Get all the log entries of the remote resource
     */
    public ILogEntry[] getLogEntries(IProgressMonitor monitor) throws TeamException;
    
    /**
     * Get log entries of the remote resource
     * @param monitor 		progress monitor
     * @param pegRevision   peg revision for URL
     * @param revisionStart first revision to show
     * @param revisionEnd   last revision to show
     * @param stopOnCopy    do not continue on copy operations
     * @param limit         limit the number of log messages (if 0 or less no
     *                      limit)
     * @return array of LogMessages
     */
    public ILogEntry[] getLogEntries(IProgressMonitor monitor, SVNRevision pegRevision, SVNRevision revisionStart, SVNRevision revisionEnd, boolean stopOnCopy, long limit) throws TeamException;

    public ISVNRemoteFolder getParent();
    
    /**
     * Get the members of this remote resource (at the same revision than this resource)
     * @param progress a progress monitor
     * @return ISVNRemoteResource[] and array of members (children resources)
     */
    public ISVNRemoteResource[] members(IProgressMonitor progress) throws TeamException;


    /**
	 * Answers if the remote element may have children.
	 * 
	 * @return <code>true</code> if the remote element may have children and 
	 * <code>false</code> otherwise.
	 */
	public boolean isContainer();
}
