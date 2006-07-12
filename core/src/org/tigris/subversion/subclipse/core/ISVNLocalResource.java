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
package org.tigris.subversion.subclipse.core;

import java.io.File;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IAdaptable;
import org.tigris.subversion.subclipse.core.resources.LocalResourceStatus;
import org.tigris.subversion.subclipse.core.resources.SVNWorkspaceRoot;
import org.tigris.subversion.svnclientadapter.ISVNProperty;
import org.tigris.subversion.svnclientadapter.SVNRevision;

/**
 * SVN Local resource
 * 
 * @see ISVNLocalFile
 * @see ISVNLocalFolder
 */
public interface ISVNLocalResource extends ISVNResource, IAdaptable {

	/**
	 * Answers the workspace synchronization information for this resource. This
	 * function does not contact the server
	 * 
	 * @return the synchronization information for this resource, or
	 *         <code>null</code> if the resource does not have synchronization
	 *         information available.
	 */
	public LocalResourceStatus getStatus() throws SVNException;

	/**
	 * Answers the revision number for this resource.
	 * The revision might not be stored in workspace sychronization data,
	 * so a svn call might be required. This call however is not expected to contact server,
	 * just fetch the revision from working copy metadata
	 * @return
	 * @throws SVNException
	 */
	public SVNRevision getRevision() throws SVNException;
	
	/**
	 * refresh the status of the resource (which is cached)
	 */
	public void refreshStatus() throws SVNException;

	/**
	 * @return if this resource exists
	 */
	public boolean exists();

	/**
	 * @return true if this resource is managed by SVN
	 * @throws SVNException
	 */
	public boolean isManaged() throws SVNException;

	/**
	 * @return true if this resource is managed by SVN and has a remote counter
	 *         part
	 * @throws SVNException
	 */
	public boolean hasRemote() throws SVNException;

	/**
	 * @return the parent of this local resource
	 */
	public ISVNLocalFolder getParent();

	/**
	 * @return the underlaying resource
	 */
	public IResource getIResource();

	/**
	 * @return the file corresponding to the resource
	 */
	public File getFile();

	/**
	 * @return the latest remote version of this resource from repository
	 * @throws SVNException
	 */
	public ISVNRemoteResource getLatestRemoteResource() throws SVNException;

	/**
	 * get the base version for this local resource
	 * 
	 * @return @throws
	 *         SVNException
	 */
	public ISVNRemoteResource getBaseResource() throws SVNException;

	/**
	 * get the remote resource corresponding to the given revision of this local
	 * resource
	 * 
	 * @return null if there is no remote file corresponding to this local
	 *         resource
	 * @throws SVNException
	 */
	public ISVNRemoteResource getRemoteResource(SVNRevision revision)
			throws SVNException;

    /**    
     * resource is considered dirty if either 
     * <ul>
     * <li>text status is added, deleted, replaced, modified, merged or conflicted.</li>
     * <li>prop status is either conflicted or modified 
     * </ul>
     */    
    public boolean isDirty() throws SVNException;
    
	public void accept(ISVNResourceVisitor visitor) throws SVNException;

	/**
	 * Add the following file to the parent's ignore list
	 */
	public void setIgnored() throws SVNException;

	/**
	 * Answer whether the resource could be ignored. Even if a resource is
	 * ignored, it can still be added to a repository, at which time it should
	 * never be ignored by the SVN client.
	 *  
	 */
	public boolean isIgnored() throws SVNException;

	/**
	 * Remove file or directory from version control.
	 */
	public void delete() throws SVNException;

	/**
	 * Restore pristine working copy file (undo all local edits)
	 */
	public void revert() throws SVNException;

	/**
	 * Set a svn property
	 */
	public void setSvnProperty(String name, String value, boolean recurse)
			throws SVNException;

	/**
	 * Set a svn property
	 */
	public void setSvnProperty(String name, File value, boolean recurse)
			throws SVNException;

	/**
	 * Get a svn property
	 */
	public ISVNProperty getSvnProperty(String name) throws SVNException;

	/**
	 * Get the svn properties for this resource
	 */
	public ISVNProperty[] getSvnProperties() throws SVNException;

	/**
	 * Delete a svn property
	 */
	public void deleteSvnProperty(String name, boolean recurse)
			throws SVNException;

	/**
	 * @throws SVNException
	 *  
	 */
	public void resolve() throws SVNException;

	/**
	 * get the workspace root ie the project
	 */
	public SVNWorkspaceRoot getWorkspaceRoot();

}