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

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.core.sync.IRemoteResource;
import org.tigris.subversion.javahl.Revision;
import org.tigris.subversion.subclipse.core.history.LogEntry;
import org.tigris.subversion.svnclientadapter.SVNUrl;

/**
 * The interface represents a resource that exists in a CVS repository.
 * It purpose is to provide information about the remote resource from
 * the repository.
 * 
 * Clients are not expected to implement this interface.
 */
public interface ISVNRemoteResource extends IRemoteResource, ISVNResource {
	
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
     * get the url for this remote resource
     */
    public SVNUrl getUrl();

    /**
     * @return true if this remote resource has properties
     */
    public boolean getHasProps();

    /**
     * @return the last changed revision of this remote resource
     */
    public Revision.Number getLastChangedRevision();

    /**
     * @return the revision of this remote resource
     */
    public Revision getRevision();

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
    public LogEntry[] getLogEntries(IProgressMonitor monitor) throws TeamException;

    public ISVNRemoteFolder getParent();
    
    public ISVNRepositoryLocation getRepository();

}
