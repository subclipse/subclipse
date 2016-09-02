/*******************************************************************************
 * Copyright (c) 2003, 2006 svnClientAdapter project and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     svnClientAdapter project committers - initial API and implementation
 ******************************************************************************/
package org.tigris.subversion.svnclientadapter;

import java.io.File;
import java.util.Date;

/**
 * An interface defining the status of one subversion item (file or directory) in
 * the working copy or repository.
 * 
 * @author philip schatz
 */
public interface ISVNStatus {

    /**
     * @return the SVNUrl instance of url of the resource on repository
     */
	SVNUrl getUrl();
	
	/**
	 * @return the url (String) of the resource in repository
	 */
	String getUrlString();

	/**
	 * @return the last changed revision or null if resource is not managed 
	 */
	SVNRevision.Number getLastChangedRevision();

    /**
     * @return date this resource last changed
     */
	Date getLastChangedDate();

	/**
	 * get the last commit author or null if resource is not versionned
	 * or if last commit author is unknown
	 * @return the last commit author or null 
	 */
	String getLastCommitAuthor();

    /**
     * @return the file or directory status
     */
	SVNStatusKind getTextStatus();

    /**
     * @return the file or directory status of base
     */
	SVNStatusKind getRepositoryTextStatus();
	
	/**
     * @return status of properties (either Kind.NORMAL, Kind.CONFLICTED or Kind.MODIFIED)
	 */
	SVNStatusKind getPropStatus();

    /**
     * @return the status of the properties base (either Kind.NORMAL, Kind.CONFLICTED or Kind.MODIFIED)
     */
	SVNStatusKind getRepositoryPropStatus();

	/**
	 * @return the revision of the resource or null if not managed 
	 */
	SVNRevision.Number getRevision();

    /**
     * @return The path to this item relative to the directory from
     * which <code>status</code> was run.
     */
	String getPath();

    /**
     * @return The absolute path from which this item was moved.
     */
	String getMovedFromAbspath();

	 /**
     * @return The absolute path to which this item was moved.
     */
	String getMovedToAbspath();
    
    /**
     * @return The absolute path to this item.
     */
    File getFile();

    /**
     * @return The node kind of the managed resource, or {@link
     * SVNNodeKind#UNKNOWN} not managed.
     */
	SVNNodeKind getNodeKind();

    /**
     * @return true when the resource was copied
     */
    boolean isCopied();    
    
    /**
     * @return true when the working copy directory is locked. 
     */
    boolean isWcLocked();
    
    /**
     * @return true when the resource was switched relative to its parent.
     */
    boolean isSwitched();

    /**
     * Returns in case of conflict, the file of the most recent repository
     * version
     * @return the filename of the most recent repository version
     */
    public File getConflictNew();

    /**
     * Returns in case of conflict, the file of the common base version
     * @return the filename of the common base version
     */
    public File getConflictOld();

    /**
     * Returns in case of conflict, the file of the former working copy
     * version
     * @return the filename of the former working copy version
     */
    public File getConflictWorking();

    /**
     * Returns the lock  owner
     * @return the lock owner
     */
    public String getLockOwner();

    /**
     * Returns the lock creation date
     * @return the lock creation date
     */
    public Date getLockCreationDate();

    /**
     * Returns the lock  comment
     * @return the lock comment
     */
    public String getLockComment();
 
    /**
     * Returns the tree conflicted state
     * @return the tree conflicted state
     */    
    public boolean hasTreeConflict();

    /**
     * Returns the conflict descriptor for the tree conflict
     * @return the conflict descriptor for the tree conflict
     */    
    public SVNConflictDescriptor getConflictDescriptor();
    
    /**
     * Returns if the item is a file external
     * @return is the item is a file external
     */
    public boolean isFileExternal();

}
