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
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * High level API for Subversion
 * 
 */
public interface ISVNClientAdapter {

	/** constant identifying the "bdb"  repository type */
    public final static String REPOSITORY_FSTYPE_BDB = "bdb";
	/** constant identifying the "fsfs"  repository type */
    public final static String REPOSITORY_FSTYPE_FSFS = "fsfs";
    
    public final static String[] DEFAULT_LOG_PROPERTIES = new String[] {"svn:author", "svn:date", "svn:log"};

	
	/**
	 * Returns whether the client adapter implementation is threadsafe
	 */
	public abstract boolean isThreadsafe();
	
	/**
	 * Add a notification listener
	 * @param listener
	 */
	public abstract void addNotifyListener(ISVNNotifyListener listener);
	
	/**
	 * Remove a notification listener
	 * @param listener 
	 */
	public abstract void removeNotifyListener(ISVNNotifyListener listener);

	/**
	 * @return the notification handler
	 */
	public abstract SVNNotificationHandler getNotificationHandler();
	
	/**
	 * Sets the username.
	 * @param username
	 */
	public abstract void setUsername(String username);
	
	/**
	 * Sets the password.
	 * @param password
	 */
	public abstract void setPassword(String password);
	
	/**
	 * Add a callback for prompting for username, password SSL etc...
	 * @param callback
	 */
	public abstract void addPasswordCallback(ISVNPromptUserPassword callback);   
	
	/**
	 * Add a callback for resolving conflicts during up/sw/merge
	 * @param callback
	 */
	public abstract void addConflictResolutionCallback(ISVNConflictResolver callback);
	
	/**
	 * Set a progress listener
	 * @param progressListener
	 */
	public abstract void setProgressListener(ISVNProgressListener progressListener);   	
    
	/**
	 * Adds a file (or directory) to the repository.
	 * @param file
	 * @throws SVNClientException
	 */
	public abstract void addFile(File file) throws SVNClientException;
	
	/**
	 * Adds a directory to the repository.
	 * @param dir
	 * @param recurse
	 * @throws SVNClientException
	 */
	public abstract void addDirectory(File dir, boolean recurse)
		throws SVNClientException;

	/**
	 * Adds a directory to the repository.
	 * @param dir
	 * @param recurse
	 * @param force
	 * @throws SVNClientException
	 */
	public abstract void addDirectory(File dir, boolean recurse, boolean force)
		throws SVNClientException;
	
	/**
	 * Executes a revision checkout.
	 * @param moduleName name of the module to checkout.
	 * @param destPath destination directory for checkout.
	 * @param revision the revision number to checkout. If the number is -1
	 *                 then it will checkout the latest revision.
	 * @param recurse whether you want it to checkout files recursively.
	 * @exception SVNClientException
	 */
	public abstract void checkout(
		SVNUrl moduleName,
		File destPath,
		SVNRevision revision,
		boolean recurse)
		throws SVNClientException;
	
	/**
	 * Executes a revision checkout.
	 * @param moduleName name of the module to checkout.
	 * @param destPath destination directory for checkout.
	 * @param revision the revision number to checkout. If the number is -1
	 *                 then it will checkout the latest revision.          
     * @param depth how deep to checkout files recursively.
     * @param ignoreExternals if externals are ignored during checkout.
     * @param force allow unversioned paths that obstruct adds.
	 * @exception SVNClientException
	 */
	public abstract void checkout(
		SVNUrl moduleName,
		File destPath,
		SVNRevision revision,
		int depth,
		boolean ignoreExternals,
		boolean force)
		throws SVNClientException;	
	
	/**
	 * Commits changes to the repository. This usually requires
	 * authentication, see Auth.
	 * @return Returns a long representing the revision. It returns a
	 *         -1 if the revision number is invalid.
	 * @param paths files to commit.
	 * @param message log message.
	 * @param recurse whether the operation should be done recursively.
	 * @exception SVNClientException
	 */
	public abstract long commit(File[] paths, String message, boolean recurse)
		throws SVNClientException;
	
    /**
     * Commits changes to the repository. This usually requires
     * authentication, see Auth.
     * @return Returns a long representing the revision. It returns a
     *         -1 if the revision number is invalid.
     * @param paths files to commit.
     * @param message log message.
     * @param recurse whether the operation should be done recursively.
     * @param keepLocks
     * @exception SVNClientException
     */
	public abstract long commit(File[] paths, String message, boolean recurse, boolean keepLocks)
		throws SVNClientException;
	
	/**
	 * Commits changes to the repository. This usually requires
	 * authentication, see Auth.
	 * 
	 * This differs from the normal commit method in that it can accept paths from
	 * more than one working copy.
	 * 
	 * @return Returns an array of longs representing the revisions. It returns a
	 *         -1 if the revision number is invalid.
	 * @param paths files to commit.
	 * @param message log message.
	 * @param recurse whether the operation should be done recursively.
	 * @param keepLocks whether to keep locks on files that are committed.
	 * @param atomic  whether to attempt to perform the commit from multiple
	 * working copies atomically.  Files from the same repository will be
	 * processed with one commit operation.  If files span multiple repositories
	 * they will be processed in multiple commits.
	 * When atomic is false, you will get one commit per WC.
	 * @exception SVNClientException
	 */
	public abstract long[] commitAcrossWC(File[] paths, String message, boolean recurse, boolean keepLocks, boolean atomic)
		throws SVNClientException;
	
	public String getPostCommitError();
	
	/**
	 * List directory entries of a URL
	 * @param url
	 * @param revision
	 * @param recurse
	 * @return an array of ISVNDirEntries 
	 * @throws SVNClientException
	 */
	public abstract ISVNDirEntry[] getList(
		SVNUrl url,
		SVNRevision revision,
		boolean recurse)
		throws SVNClientException;
	
	/**
	 * List directory entries of a URL
	 * @param url
	 * @param revision
	 * @param pegRevision
	 * @param recurse
	 * @return an array of ISVNDirEntries 
	 * @throws SVNClientException
	 */
	public abstract ISVNDirEntry[] getList(
		SVNUrl url,
		SVNRevision revision,
		SVNRevision pegRevision,
		boolean recurse)
		throws SVNClientException;
	
	/**
	 * List directory entries of a URL with lock information
	 * @param url
	 * @param revision
	 * @param pegRevision
	 * @param recurse
	 * @return an array of ISVNDirEntries 
	 * @throws SVNClientException
	 */
	public abstract ISVNDirEntryWithLock[] getListWithLocks(
		SVNUrl url,
		SVNRevision revision,
		SVNRevision pegRevision,
		boolean recurse)
		throws SVNClientException;		

	/**
	 * List directory entries of a directory
	 * @param path
	 * @param revision
	 * @param recurse
	 * @return an array of ISVNDirEntries 
	 * @throws SVNClientException
	 */	
	public ISVNDirEntry[] getList(File path, SVNRevision revision, boolean recurse) 
    	throws SVNClientException;
	
	/**
	 * List directory entries of a directory
	 * @param path
	 * @param revision
	 * @param pegRevision
	 * @param recurse
	 * @return an array of ISVNDirEntries 
	 * @throws SVNClientException
	 */	
	public ISVNDirEntry[] getList(File path, SVNRevision revision, SVNRevision pegRevision, boolean recurse) 
    	throws SVNClientException;		
	
	/**
	 * get the dirEntry for the given url
	 * @param url
	 * @param revision
	 * @return an ISVNDirEntry
	 * @throws SVNClientException
	 */
	public ISVNDirEntry getDirEntry(SVNUrl url, SVNRevision revision)
			throws SVNClientException;

	/**
	 * get the dirEntry for the given directory 
	 * @param path
	 * @param revision
	 * @return an ISVNDirEntry
	 * @throws SVNClientException
	 */
	public ISVNDirEntry getDirEntry(File path, SVNRevision revision)
			throws SVNClientException;
	
	/**
	 * Returns the status of a single file in the path.
	 *
	 * @param path File to gather status.
	 * @return a Status
	 * @throws SVNClientException
	 */
    public abstract ISVNStatus getSingleStatus(File path)
        throws SVNClientException;
        
    /**
     * Returns the status of given resources
     * @param path
     * @return the status of given resources
     * @throws SVNClientException
     */    
	public abstract ISVNStatus[] getStatus(File[] path)
		throws SVNClientException;
	
	/**
	 * Returns the status of path and its children.
     * If descend is true, recurse fully, else do only immediate children.
     * If getAll is set, retrieve all entries; otherwise, retrieve only 
     * "interesting" entries (local mods and/or out-of-date).
     *
	 * @param path File to gather status.
     * @param descend get recursive status information
     * @param getAll get status information for all files
	 * @return a Status
     * @throws SVNClientException
	 */
	public abstract ISVNStatus[] getStatus(File path, boolean descend, boolean getAll)
		throws SVNClientException;

	/**
	 * Returns the status of path and its children.
     * If descend is true, recurse fully, else do only immediate children.
     * If getAll is set, retrieve all entries; otherwise, retrieve only 
     * "interesting" entries (local mods and/or out-of-date). Use the
     * contactServer option to get server change information.
     *
	 * @param path File to gather status.
     * @param descend get recursive status information
     * @param getAll get status information for all files
     * @param contactServer contact server to get remote changes
	 * @return a Status
     * @throws SVNClientException
	 */
	public abstract ISVNStatus[] getStatus(File path, boolean descend, boolean getAll, boolean contactServer)
	throws SVNClientException;

	/**
	 * Returns the status of path and its children.
     * If descend is true, recurse fully, else do only immediate children.
     * If getAll is set, retrieve all entries; otherwise, retrieve only 
     * "interesting" entries (local mods and/or out-of-date). Use the
     * contactServer option to get server change information.
     *
	 * @param path File to gather status.
     * @param descend get recursive status information
     * @param getAll get status information for all files
     * @param contactServer contact server to get remote changes
     * @param ignoreExternals if externals are ignored during status
	 * @return a Status
     * @throws SVNClientException
	 */
	public abstract ISVNStatus[] getStatus(File path, boolean descend, boolean getAll, boolean contactServer, boolean ignoreExternals)
	throws SVNClientException;
	
	/**
	 * Returns the status of path and its children.
     * If descend is true, recurse fully, else do only immediate children.
     * If getAll is set, retrieve all entries; otherwise, retrieve only 
     * "interesting" entries (local mods and/or out-of-date). Use the
     * contactServer option to get server change information.
     *
	 * @param path File to gather status.
     * @param descend get recursive status information
     * @param getAll get status information for all files
     * @param contactServer contact server to get remote changes
     * @param ignoreExternals if externals are ignored during status
     * @param callback callback to collect statuses
	 * @return a Status
     * @throws SVNClientException
	 */
	public abstract ISVNStatus[] getStatus(File path, boolean descend, boolean getAll, boolean contactServer, boolean ignoreExternals, ISVNStatusCallback callback)
	throws SVNClientException;
	
	/**
	 * Returns the status of path and its children.
     * If descend is true, recurse fully, else do only immediate children.
     * If getAll is set, retrieve all entries; otherwise, retrieve only 
     * "interesting" entries (local mods and/or out-of-date). Use the
     * contactServer option to get server change information.
     *
	 * @param path File to gather status.
     * @param descend get recursive status information
     * @param getAll get status information for all files
     * @param contactServer contact server to get remote changes
     * @param ignoreExternals if externals are ignored during status
     * @param noIgnore if true, ignored entries are included
     * @param callback callback to collect statuses
	 * @return a Status
     * @throws SVNClientException
	 */
	public abstract ISVNStatus[] getStatus(File path, boolean descend, boolean getAll, boolean contactServer, boolean ignoreExternals, boolean noIgnore, ISVNStatusCallback callback)
	throws SVNClientException;

	/**
	 * copy and schedule for addition (with history)
	 * @param srcPath
	 * @param destPath
	 * @throws SVNClientException
	 */
	public abstract void copy(File srcPath, File destPath)
		throws SVNClientException;
	
	/**
	 * immediately commit a copy of WC to URL
	 * @param srcPath
	 * @param destUrl
	 * @param message
	 * @throws SVNClientException
	 */
	public abstract void copy(File srcPath, SVNUrl destUrl, String message)
		throws SVNClientException;
	
	/**
	 * immediately commit a copy of WC to URL
	 * @param srcPaths
	 * @param destUrl
	 * @param message
	 * @param copyAsChild
	 * @param makeParents
	 * @throws SVNClientException
	 */
	public abstract void copy(File[] srcPaths, SVNUrl destUrl, String message, boolean copyAsChild, boolean makeParents)
		throws SVNClientException;		
	
	/**
	 * check out URL into WC, schedule for addition
	 * @param srcUrl
	 * @param destPath
	 * @param revision
	 * @throws SVNClientException
	 */
	public abstract void copy(SVNUrl srcUrl, File destPath, SVNRevision revision)
		throws SVNClientException;
	
	/**
	 * check out URL into WC, schedule for addition
	 * @param srcUrl
	 * @param destPath
	 * @param revision
	 * @param boolean
	 * @param boolean
	 * @throws SVNClientException
	 */
	public abstract void copy(SVNUrl srcUrl, File destPath, SVNRevision revision, boolean copyAsChild, boolean makeParents)
		throws SVNClientException;	
	
	/**
	 * check out URL into WC, schedule for addition
	 * @param srcUrl
	 * @param destPath
	 * @param revision
	 * @param pegRevision
	 * @param boolean
	 * @param boolean
	 * @throws SVNClientException
	 */
	public abstract void copy(SVNUrl srcUrl, File destPath, SVNRevision revision, SVNRevision pegRevision, boolean copyAsChild, boolean makeParents)
		throws SVNClientException;		
	
	/**
	 * complete server-side copy;  used to branch & tag
	 * @param srcUrl
	 * @param destUrl
	 * @param message
	 * @param revision
	 * @throws SVNClientException
	 */
	public abstract void copy(
		SVNUrl srcUrl,
		SVNUrl destUrl,
		String message,
		SVNRevision revision)
		throws SVNClientException;
	
	/**
	 * complete server-side copy with option to create intermediate folders;  used to branch & tag
	 * @param srcUrl
	 * @param destUrl
	 * @param message
	 * @param revision
	 * @param make parents
	 * @throws SVNClientException
	 */
	public abstract void copy(
		SVNUrl srcUrl,
		SVNUrl destUrl,
		String message,
		SVNRevision revision,
		boolean makeParents)
		throws SVNClientException;
	
	/**
	 * complete server-side copy with option to create intermediate folders;  used to branch & tag
	 * @param srcUrl
	 * @param destUrl
	 * @param message
	 * @param revision
	 * @param copyAsChild
	 * @param make parents
	 * @throws SVNClientException
	 */
	public abstract void copy(
		SVNUrl[] srcUrls,
		SVNUrl destUrl,
		String message,
		SVNRevision revision,
		boolean copyAsChild,
		boolean makeParents)
		throws SVNClientException;		
	
	/**
	 * item is deleted from the repository via an immediate commit.
	 * @param url
	 * @param message
	 * @throws SVNClientException
	 */
	public abstract void remove(SVNUrl url[], String message)
		throws SVNClientException;
	
	/**
	 * the item is scheduled for deletion upon the next commit.  
	 * Files, and directories that have not been committed, are immediately 
	 * removed from the working copy.  The command will not remove TARGETs 
	 * that are, or contain, unversioned or modified items; 
	 * use the force option to override this behaviour.
	 * @param file
	 * @param force
	 * @throws SVNClientException
	 */
	public abstract void remove(File file[], boolean force)
		throws SVNClientException;
	
	/**
	 * Exports a clean directory tree from the repository specified by
	 * srcUrl, at revision revision 
	 * @param srcUrl
	 * @param destPath
	 * @param revision
	 * @param force
	 * @throws SVNClientException
	 */
	public abstract void doExport(
		SVNUrl srcUrl,
		File destPath,
		SVNRevision revision,
		boolean force)
		throws SVNClientException;
	
	/**
	 * Exports a clean directory tree from the working copy specified by
	 * PATH1 into PATH2.  all local changes will be preserved, but files
	 * not under revision control will not be copied.
	 * @param srcPath
	 * @param destPath
	 * @param force
	 * @throws SVNClientException
	 */
	public abstract void doExport(File srcPath, File destPath, boolean force)
		throws SVNClientException;

	/**
	 * Import file or directory PATH into repository directory URL at head
	 * @param path
	 * @param url
	 * @param message
	 * @param recurse
	 * @throws SVNClientException
	 */
	public abstract void doImport(
		File path,
		SVNUrl url,
		String message,
		boolean recurse)
		throws SVNClientException;

	/**
	 * Creates a directory directly in a repository
	 * @param url
	 * @param message
	 * @throws SVNClientException
	 */
	public abstract void mkdir(SVNUrl url, String message)
		throws SVNClientException;

	/**
	 * Creates a directory directly in a repository
	 * @param url
	 * @param makeParents
	 * @param message
	 * @throws SVNClientException
	 */
	public abstract void mkdir(SVNUrl url, boolean makeParents, String message)
		throws SVNClientException;

	/**
	 * creates a directory on disk and schedules it for addition.
	 * @param file
	 * @throws SVNClientException
	 */
	public abstract void mkdir(File file) throws SVNClientException;

	/**
	 * Moves or renames a file.
	 * @param srcPath
	 * @param destPath
	 * @param force
	 * @throws SVNClientException
	 */
	public abstract void move(File srcPath, File destPath, boolean force)
		throws SVNClientException;

	/**
	 * Moves or renames a file.
	 * @param srcUrl
	 * @param destUrl
	 * @param message
	 * @param revision
	 * @throws SVNClientException
	 */
	public abstract void move(
		SVNUrl srcUrl,
		SVNUrl destUrl,
		String message,
		SVNRevision revision)
		throws SVNClientException;
	
	/**
	 * Update a file or a directory
	 * @param path
	 * @param revision
	 * @param recurse
     * @return Returns a long representing the revision. It returns a
     *         -1 if the revision number is invalid.
	 * @throws SVNClientException
	 */
	public abstract long update(File path, SVNRevision revision, boolean recurse)
		throws SVNClientException;
	
	/**
	 * Update a file or a directory
	 * @param path
	 * @param revision
     * @param depth
     * @param setDepth
     * @param ignoreExternals
     * @param force
     * @return Returns a long representing the revision. It returns a
     *         -1 if the revision number is invalid.
	 * @throws SVNClientException
	 */
	public abstract long update(File path, SVNRevision revision, int depth, boolean setDepth, boolean ignoreExternals, boolean force)
		throws SVNClientException;		

    /**
     * Updates the directories or files from repository
     * @param path array of target files.
     * @param revision the revision number to update.
     * @param recurse recursively update.
     * @param ignoreExternals if externals are ignored during update
     * @return Returns an array of longs representing the revision. It returns a
     *         -1 if the revision number is invalid.
     * @throws SVNClientException
     * @since 1.2
     */
    public abstract long[] update(
    	File[] path, 
		SVNRevision revision, 
		boolean recurse,
		boolean ignoreExternals) 
    	throws SVNClientException;
    
    /**
     * Updates the directories or files from repository
     * @param path array of target files.
     * @param revision the revision number to update.
     * @param depth  the depth to recursively update.
     * @param setDepth change working copy to specified depth
     * @param ignoreExternals if externals are ignored during update.
     * @param force allow unversioned paths that obstruct adds.
     * @return Returns an array of longs representing the revision. It returns a
     *         -1 if the revision number is invalid.
     * @throws SVNClientException
     */
    public abstract long[] update(
    	File[] path, 
		SVNRevision revision, 
		int depth,
		boolean setDepth,
		boolean ignoreExternals,
		boolean force)
    	throws SVNClientException;	        
	
	/**
	 * Restore pristine working copy file (undo all local edits)
	 * @param path
	 * @param recurse
	 * @throws SVNClientException
	 */
	public abstract void revert(File path, boolean recurse)
		throws SVNClientException;
	/**
	 * Get the log messages for a set of revision(s) 
	 * @param url
	 * @param revisionStart
	 * @param revisionEnd
	 * @return The list of log messages.
	 * @throws SVNClientException
	 */
	public abstract ISVNLogMessage[] getLogMessages(
		SVNUrl url,
		SVNRevision revisionStart,
		SVNRevision revisionEnd)
		throws SVNClientException;
	
	/**
	 * Get the log messages for a set of revision(s) 
	 * @param url
	 * @param revisionStart
	 * @param revisionEnd
	 * @param fetchChangePath Whether or not to interogate the
	 * repository for the verbose log information containing the list
	 * of paths touched by the delta specified by
	 * <code>revisionStart</code> and <code>revisionEnd</code>.
	 * Setting this to <code>false</code> results in a more performant
	 * and memory efficient operation.
	 * @return The list of log messages.
	 * @throws SVNClientException
	 */
	public abstract ISVNLogMessage[] getLogMessages(
		SVNUrl url,
		SVNRevision revisionStart,
		SVNRevision revisionEnd,
		boolean fetchChangePath)
		throws SVNClientException;
	
	/**
	 * Get the log messages for a set paths and revision(s)
	 * @param url
	 * @param paths
	 * @param revStart
	 * @param revEnd
	 * @param stopOnCopy
	 * @param fetchChangePath
	 * @return The list of log messages.
	 * @throws SVNClientException
	 */
	public ISVNLogMessage[] getLogMessages(final SVNUrl url, final String [] paths,
			SVNRevision revStart, SVNRevision revEnd,
			boolean stopOnCopy, boolean fetchChangePath)
	throws SVNClientException;
    
	/**
	 * Get the log messages for a set of revision(s)
	 * @param path
	 * @param revisionStart
	 * @param revisionEnd
	 * @return The list of log messages.
	 * @throws SVNClientException
	 */
	public abstract ISVNLogMessage[] getLogMessages(
		File path,
		SVNRevision revisionStart,
		SVNRevision revisionEnd)
		throws SVNClientException;
	
	/**
	 * Get the log messages for a set of revision(s)
	 * @param path
	 * @param revisionStart
	 * @param revisionEnd
	 * @param fetchChangePath Whether or not to interogate the
	 * repository for the verbose log information containing the list
	 * of paths touched by the delta specified by
	 * <code>revisionStart</code> and <code>revisionEnd</code>.
	 * Setting this to <code>false</code> results in a more performant
	 * and memory efficient operation.
	 * @return The list of log messages.
	 * @throws SVNClientException
	 */
	public abstract ISVNLogMessage[] getLogMessages(
		File path,
		SVNRevision revisionStart,
		SVNRevision revisionEnd,
		boolean fetchChangePath)
		throws SVNClientException;
	
    /**
     * Retrieve the log messages for an item
     * @param path          path or url to get the log message for.
     * @param revisionStart first revision to show
     * @param revisionEnd   last revision to show
     * @param stopOnCopy    do not continue on copy operations
     * @param fetchChangePath  returns the paths of the changed items in the
     *                      returned objects
     * @return array of LogMessages
	 * @throws SVNClientException
     */
    public abstract ISVNLogMessage[] getLogMessages(
            File path,
            SVNRevision revisionStart,
            SVNRevision revisionEnd,
            boolean stopOnCopy,
            boolean fetchChangePath)
            throws SVNClientException;
    
    /**
     * Retrieve the log messages for an item
     * @param path          path to get the log message for.
     * @param revisionStart first revision to show
     * @param revisionEnd   last revision to show
     * @param stopOnCopy    do not continue on copy operations
     * @param fetchChangePath  returns the paths of the changed items in the
     *                      returned objects
     * @param limit         limit the number of log messages (if 0 or less no
     *                      limit)
     * @return array of LogMessages
	 * @throws SVNClientException
     */
    public abstract ISVNLogMessage[] getLogMessages(
            File path, 
            SVNRevision revisionStart,
            SVNRevision revisionEnd,
            boolean stopOnCopy,
            boolean fetchChangePath,
            long limit)
            throws SVNClientException;
    
    /**
     * Retrieve the log messages for an item
     * @param path          path to get the log message for.
     * @param pegRevision   peg revision for URL
     * @param revisionStart first revision to show
     * @param revisionEnd   last revision to show
     * @param stopOnCopy    do not continue on copy operations
     * @param fetchChangePath  returns the paths of the changed items in the
     *                      returned objects
     * @param limit         limit the number of log messages (if 0 or less no
     *                      limit)
     * @param includeMergedRevisions include revisions that were merged
     * @return array of LogMessages
	 * @throws SVNClientException
     */
    public abstract ISVNLogMessage[] getLogMessages(
            File path, 
            SVNRevision pegRevision,
            SVNRevision revisionStart,
            SVNRevision revisionEnd,
            boolean stopOnCopy,
            boolean fetchChangePath,
            long limit,
            boolean includeMergedRevisions)
            throws SVNClientException;
    
    /**
     * Retrieve the log messages for an item
     * @param url           url to get the log message for.
     * @param pegRevision   peg revision for URL
     * @param revisionStart first revision to show
     * @param revisionEnd   last revision to show
     * @param stopOnCopy    do not continue on copy operations
     * @param fetchChangePath  returns the paths of the changed items in the
     *                      returned objects
     * @param limit         limit the number of log messages (if 0 or less no
     *                      limit)
     * @return array of LogMessages
	 * @throws SVNClientException
     */
    public abstract ISVNLogMessage[] getLogMessages(
            SVNUrl url, 
            SVNRevision pegRevision,
            SVNRevision revisionStart,
            SVNRevision revisionEnd,
            boolean stopOnCopy,
            boolean fetchChangePath,
            long limit)
            throws SVNClientException;
    
    /**
     * Retrieve the log messages for an item
     * @param url           url to get the log message for.
     * @param pegRevision   peg revision for URL
     * @param revisionStart first revision to show
     * @param revisionEnd   last revision to show
     * @param stopOnCopy    do not continue on copy operations
     * @param fetchChangePath  returns the paths of the changed items in the
     *                      returned objects
     * @param limit         limit the number of log messages (if 0 or less no
     *                      limit)
     * @param includeMergedRevisions include revisions that were merged
     * @return array of LogMessages
	 * @throws SVNClientException
     */
    public abstract ISVNLogMessage[] getLogMessages(
            SVNUrl url, 
            SVNRevision pegRevision,
            SVNRevision revisionStart,
            SVNRevision revisionEnd,
            boolean stopOnCopy,
            boolean fetchChangePath,
            long limit,
            boolean includeMergedRevisions)
            throws SVNClientException;
    
    /**
     * Retrieve the log messages for an item
     * @param path          path to get the log message for.
     * @param pegRevision   peg revision for URL
     * @param revisionStart first revision to show
     * @param revisionEnd   last revision to show
     * @param stopOnCopy    do not continue on copy operations
     * @param fetchChangePath  returns the paths of the changed items in the
     *                      returned objects
     * @param limit         limit the number of log messages (if 0 or less no
     *                      limit)
     * @param includeMergedRevisions include revisions that were merged
     * @param requestedProperties the revision properties to return for each entry
     * @param callback      callback class to receive log messages
	 * @throws SVNClientException
     */
    public abstract void getLogMessages(
            File path, 
            SVNRevision pegRevision,
            SVNRevision revisionStart,
            SVNRevision revisionEnd,
            boolean stopOnCopy,
            boolean fetchChangePath,
            long limit,
            boolean includeMergedRevisions,
            String[] requestedProperties,
            ISVNLogMessageCallback callback)
            throws SVNClientException;    
    /**
     * Retrieve the log messages for an item
     * @param url           url to get the log message for.
     * @param pegRevision   peg revision for URL
     * @param revisionStart first revision to show
     * @param revisionEnd   last revision to show
     * @param stopOnCopy    do not continue on copy operations
     * @param fetchChangePath  returns the paths of the changed items in the
     *                      returned objects
     * @param limit         limit the number of log messages (if 0 or less no
     *                      limit)
     * @param includeMergedRevisions include revisions that were merged
     * @param requestedProperties the revision properties to return for each entry
     * @param callback      callback class to receive log messages
	 * @throws SVNClientException
     */
    public abstract void getLogMessages(
            SVNUrl url, 
            SVNRevision pegRevision,
            SVNRevision revisionStart,
            SVNRevision revisionEnd,
            boolean stopOnCopy,
            boolean fetchChangePath,
            long limit,
            boolean includeMergedRevisions,
            String[] requestedProperties,
            ISVNLogMessageCallback callback)
            throws SVNClientException;
    
	/**
	 * get the content of a file
	 * @param url
	 * @param revision
	 * @param peg revision
	 * @return the input stream with a content of the file
	 * @throws SVNClientException
	 */
	public abstract InputStream getContent(SVNUrl url, SVNRevision revision, SVNRevision pegRevision)
		throws SVNClientException;    
    
	/**
	 * get the content of a file
	 * @param url
	 * @param revision
	 * @return the input stream with a content of the file
	 * @throws SVNClientException
	 */
	public abstract InputStream getContent(SVNUrl url, SVNRevision revision)
		throws SVNClientException;
		
	/**
	 * get the content of a file
	 * @param path
	 * @param revision
	 * @return the input stream with a content of the file
	 * @throws SVNClientException
	 */
	public InputStream getContent(File path, SVNRevision revision) 
		throws SVNClientException;
		
	/**
	 * set a property
	 * @param path
	 * @param propertyName
	 * @param propertyValue
	 * @param recurse
	 * @throws SVNClientException
	 */
	public abstract void propertySet(
		File path,
		String propertyName,
		String propertyValue,
		boolean recurse)
		throws SVNClientException;
	
	/**
	 * set a property
	 * @param url
	 * @param baseRev
	 * @param propertyName
	 * @param propertyValue
	 * @param recurse
	 * @throws SVNClientException
	 */
	public abstract void propertySet(
		SVNUrl url,
		SVNRevision.Number baseRev,
		String propertyName,
		String propertyValue,
		String message)
		throws SVNClientException;

	/**
	 * set a property using the content of a file 
	 * @param path
	 * @param propertyName
	 * @param propertyFile
	 * @param recurse
	 * @throws SVNClientException
	 * @throws IOException
	 */
	public abstract void propertySet(
		File path,
		String propertyName,
		File propertyFile,
		boolean recurse)
		throws SVNClientException, IOException;
	
	/**
	 * get a property or null if property is not found
	 * @param path
	 * @param propertyName
	 * @return a property or null
	 * @throws SVNClientException
	 */
	public abstract ISVNProperty propertyGet(File path, String propertyName)
		throws SVNClientException;

	/**
	 * get a property or null if property is not found
	 * @param url
	 * @param propertyName
	 * @return a property or null
	 * @throws SVNClientException
	 */
	public abstract ISVNProperty propertyGet(SVNUrl url, String propertyName)
		throws SVNClientException;
	
	/**
	 * get a property or null if property is not found
	 * @param url
	 * @param revision
	 * @param peg
	 * @param propertyName
	 * @return a property or null
	 * @throws SVNClientException
	 */
	public abstract ISVNProperty propertyGet(SVNUrl url, SVNRevision revision,
			SVNRevision peg, String propertyName)
		throws SVNClientException;
	
	/**
	 * delete a property
	 * @param path
	 * @param propertyName
	 * @param recurse
	 * @throws SVNClientException
	 */
	public abstract void propertyDel(
		File path,
		String propertyName,
		boolean recurse)
		throws SVNClientException;
	
	/**
     * set the revision property for a given revision
     * @param path
     * @param revisionNo
     * @param propName
     * @param propertyData
     * @param force
     * @throws SVNClientException
     */    
    public abstract void setRevProperty(SVNUrl path,
			SVNRevision.Number revisionNo, String propName,
			String propertyData, boolean force) throws SVNClientException;
    
    /**
     * get a revision property for a given revision
     * @param path
     * @param revisionNo
     * @param propName
     * @throws SVNClientException
     */    
    public abstract String getRevProperty(SVNUrl path,
            SVNRevision.Number revisionNo, String propName) 
            throws SVNClientException;    
    
	/**
	 * get the ignored patterns for the given directory
	 * if path is not a directory, returns null
	 * @param path
	 * @return list of ignored patterns 
	 * @throws SVNClientException
	 */
	public abstract List getIgnoredPatterns(File path)
		throws SVNClientException;
	
	/**
	 * add a pattern to svn:ignore property 
	 * @param path
	 * @param pattern 
	 * @throws SVNClientException
	 */
	public abstract void addToIgnoredPatterns(File path, String pattern)
		throws SVNClientException;
	
	/**
	 * set the ignored patterns for the given directory 
	 * @param path
	 * @param patterns
	 * @throws SVNClientException
	 */
	public abstract void setIgnoredPatterns(File path, List patterns)
		throws SVNClientException;
	
	/**
	 * display the differences between two paths.
	 * @param oldPath
	 * @param oldPathRevision
	 * @param newPath
	 * @param newPathRevision
	 * @param outFile
	 * @param recurse
	 * @throws SVNClientException
	 */
	public abstract void diff(
		File oldPath,
		SVNRevision oldPathRevision,
		File newPath,
		SVNRevision newPathRevision,
		File outFile,
		boolean recurse)
		throws SVNClientException;

	/**
	 * display the differences between two paths.
	 * @param oldPath
	 * @param oldPathRevision
	 * @param newPath
	 * @param newPathRevision
	 * @param outFile
	 * @param recurse
	 * @param ignoreAncestry
	 * @param noDiffDeleted 
	 * @param force		
	 * @throws SVNClientException
	 */
	public abstract void diff(
		File oldPath,
		SVNRevision oldPathRevision,
		File newPath,
		SVNRevision newPathRevision,
		File outFile,
		boolean recurse,
		boolean ignoreAncestry, 
		boolean noDiffDeleted, 
		boolean force)		
		throws SVNClientException;

	/**
	 * display the differences between two paths.
	 * @param path
	 * @param outFile
	 * @param recurse
	 * @throws SVNClientException
	 */
	public abstract void diff(
		File path, 
		File outFile, 
		boolean recurse)
		throws SVNClientException;
	
	/**
	 * display the combined differences for an array of paths.
	 * @param paths
	 * @param outFile
	 * @param recurse
	 * @throws SVNClientException
	 */
	public abstract void diff(
		File[] paths, 
		File outFile, 
		boolean recurse)
		throws SVNClientException;	
	
	/**
	 * create a patch from local differences.
	 * @param paths
	 * @param relativeToPath - create patch relative to this location
	 * @param outFile
	 * @param recurse
	 * @throws SVNClientException
	 */
	public abstract void createPatch(
		File[] paths,
		File relativeToPath,
		File outFile, 
		boolean recurse)
		throws SVNClientException;	
	
	/**
	 * display the differences between two urls.
	 * @param oldUrl
	 * @param oldUrlRevision
	 * @param newUrl
	 * @param newUrlRevision
	 * @param outFile
	 * @param recurse
	 * @throws SVNClientException
	 */
	public abstract void diff(
		SVNUrl oldUrl,
		SVNRevision oldUrlRevision,
		SVNUrl newUrl,
		SVNRevision newUrlRevision,
		File outFile,
		boolean recurse)
		throws SVNClientException;

	/**
	 * display the differences between two urls.
	 * @param oldUrl
	 * @param oldUrlRevision
	 * @param newUrl
	 * @param newUrlRevision
	 * @param outFile
	 * @param recurse
	 * @param ignoreAncestry
	 * @param noDiffDeleted 
	 * @param force		
	 * @throws SVNClientException
	 */
	public abstract void diff(
		SVNUrl oldUrl,
		SVNRevision oldUrlRevision,
		SVNUrl newUrl,
		SVNRevision newUrlRevision,
		File outFile,
		boolean recurse,
		boolean ignoreAncestry, 
		boolean noDiffDeleted, 
		boolean force)		
		throws SVNClientException;

    /**
     * Display the differences between two paths.
     * @param target        
     * @param pegRevision   
     * @param startRevision 
     * @param endRevision   
     * @param outFile
     * @param depth         
     * @param ignoreAncestry 
     * @param noDiffDeleted 
     * @param force        
     * @throws SVNClientException
     */	
	public abstract void diff(
			SVNUrl target,
			SVNRevision pegRevision,
			SVNRevision startRevision,
			SVNRevision endRevision,
			File outFile,
			int depth,
			boolean ignoreAncestry, 
			boolean noDiffDeleted, 
			boolean force)		
			throws SVNClientException;
	
    /**
     * Display the differences between two paths.
     * @param target        
     * @param pegRevision   
     * @param startRevision 
     * @param endRevision   
     * @param outFile
     * @param recurse
     * @throws SVNClientException
     */	
	public abstract void diff(
			SVNUrl target,
			SVNRevision pegRevision,
			SVNRevision startRevision,
			SVNRevision endRevision,
			File outFile,
			boolean recurse)		
			throws SVNClientException;	

	/**
	 * display the differences between two urls.
	 * @param url
	 * @param oldUrlRevision
	 * @param newUrlRevision
	 * @param outFile
	 * @param recurse
	 * @throws SVNClientException
	 */
	public abstract void diff(
		SVNUrl url,
		SVNRevision oldUrlRevision,
		SVNRevision newUrlRevision,
		File outFile,
		boolean recurse)
		throws SVNClientException;
	
	/**
	 * display the differences between WC and url. 
	 * @param path
	 * @param url
	 * @param urlRevision
	 * @param outFile
	 * @param recurse
	 * @throws SVNClientException
	 */
	public abstract void diff(
		File path,
		SVNUrl url,
		SVNRevision urlRevision,
		File outFile,
		boolean recurse)
		throws SVNClientException;

    /**
     * returns the keywords used for substitution for the given resource
     * @param path
     * @return the keywords used for substitution 
     * @throws SVNClientException
     */         
    public abstract SVNKeywords getKeywords(File path) throws SVNClientException;    

    /**
     * set the keywords substitution for the given resource
     * @param path
     * @param keywords
     * @param recurse
     * @throws SVNClientException
     */    
    public abstract void setKeywords(File path, SVNKeywords keywords, boolean recurse) throws SVNClientException;

    /**
     * add some keyword to the keywords substitution list
     * @param path
     * @param keywords
     * @return keywords valid after this method call 
     * @throws SVNClientException
     */    
    public abstract SVNKeywords addKeywords(File path, SVNKeywords keywords) throws SVNClientException;

    /**
     * remove some keywords to the keywords substitution list
     * @param path
     * @param keywords
     * @return keywords valid after this method call 
     * @throws SVNClientException
     */    
    public SVNKeywords removeKeywords(File path, SVNKeywords keywords) throws SVNClientException;

    /**
     * Output the content of specified url with revision and 
     * author information in-line. 
     * @param url
     * @param revisionStart
     * @param revisionEnd
     * @return annotations for the given url
	 * @throws SVNClientException
     */
    public ISVNAnnotations annotate(SVNUrl url, SVNRevision revisionStart, SVNRevision revisionEnd)
        throws SVNClientException;

    /**
     * Output the content of specified file with revision and 
     * author information in-line.
     * @param file
     * @param revisionStart
     * @param revisionEnd
     * @return annotations for the given file
	 * @throws SVNClientException
     */
    public ISVNAnnotations annotate(File file, SVNRevision revisionStart, SVNRevision revisionEnd)
        throws SVNClientException;

    /**
     * Output the content of specified url with revision and 
     * author information in-line. 
     * @param url
     * @param revisionStart
     * @param revisionEnd
     * @param pegRevision
     * @param ignoreMimeType
     * @param includeMergedRevisons
     * @return annotations for the given url
	 * @throws SVNClientException
     */
    public ISVNAnnotations annotate(SVNUrl url, SVNRevision revisionStart, SVNRevision revisionEnd, SVNRevision pegRevision,
    		boolean ignoreMimeType, boolean includeMergedRevisions)
        throws SVNClientException;

    /**
     * Output the content of specified file with revision and 
     * author information in-line.
     * @param file
     * @param revisionStart
     * @param revisionEnd
     * @param ignoreMimeType
     * @param includeMergedRevisons
     * @return annotations for the given file
	 * @throws SVNClientException
     */
    public ISVNAnnotations annotate(File file, SVNRevision revisionStart, SVNRevision revisionEnd,
    		boolean ignoreMimeType, boolean includeMergedRevisions)
        throws SVNClientException;
    
    /**
     * Output the content of specified file with revision and 
     * author information in-line.
     * @param file
     * @param revisionStart
     * @param revisionEnd
     * @param pegRevision;
     * @param ignoreMimeType
     * @param includeMergedRevisons
     * @return annotations for the given file
	 * @throws SVNClientException
     */
    public ISVNAnnotations annotate(File file, SVNRevision revisionStart, SVNRevision revisionEnd, SVNRevision pegRevision,
    		boolean ignoreMimeType, boolean includeMergedRevisions)
        throws SVNClientException; 
 
    /**
     * Get all the properties for the given file or dir, including inherited
     * @param path
     * @return the properties for the given file or dir, including inherited
     * @throws SVNClientException
     */    
    public abstract ISVNProperty[] getPropertiesIncludingInherited(File path) throws SVNClientException;
    
    /**
     * Get all the properties for the given file or dir, including inherited
     * @param path
     * @param includeEmptyProperties
     * @param includeClosestOnly
     * @param filterParameters
     * @return the properties for the given file or dir, including inherited
     * @throws SVNClientException
     */    
    public abstract ISVNProperty[] getPropertiesIncludingInherited(File path, boolean includeEmptyProperties, boolean includeClosestOnly, List<String> filterProperties) throws SVNClientException;
    
    /**
     * Get all the properties for the given URL, including inherited
     * @param path
     * @return the properties for the given URL, including inherited
     * @throws SVNClientException
     */    
    public abstract ISVNProperty[] getPropertiesIncludingInherited(SVNUrl path) throws SVNClientException;  
    
    /**
     * Get all the properties for the given URL, including inherited
     * @param path
     * @param includeEmptyProperties
     * @param includeClosestOnly
     * @param filterParameters
     * @return the properties for the given URL, including inherited
     * @throws SVNClientException
     */    
    public abstract ISVNProperty[] getPropertiesIncludingInherited(SVNUrl path, boolean includeEmptyProperties, boolean includeClosestOnly, List<String> filterProperties) throws SVNClientException;   
    
    /**
     * Get all the properties for the given file or dir
     * @param path
     * @return the properties for the given url
     * @throws SVNClientException
     */    
	public abstract ISVNProperty[] getProperties(File path) throws SVNClientException;
	
    /**
     * Get all the properties for the given file or dir
     * @param path
     * @param descend get properties recursively
     * @return the properties for the given url
     * @throws SVNClientException
     */    
	public abstract ISVNProperty[] getProperties(File path, boolean descend) throws SVNClientException;
	
    /**
     * Get all the properties for the given url
	 * @param url
	 * @param revision
	 * @param peg
	 * @param recurse
	 * @return information about an URL.
	 * @throws SVNClientException
	 */
	public abstract ISVNProperty[] getProperties(SVNUrl url, SVNRevision revision, SVNRevision peg, boolean recurse) throws SVNClientException;
	
    /**
     * Get all the properties for the given url
	 * @param url
	 * @param revision
	 * @param peg
	 * @return information about an URL.
	 * @throws SVNClientException
	 */
	public abstract ISVNProperty[] getProperties(SVNUrl url, SVNRevision revision, SVNRevision peg) throws SVNClientException;
    
    /**
     * Get all the properties for the given url
     * @param url
     * @return the properties for the given url
     * @throws SVNClientException
     */    
	public abstract ISVNProperty[] getProperties(SVNUrl url) throws SVNClientException;

    /**
     * Get all the revision properties for the given url at a revision
     * @param url
     * @param revision
     * @return information about an URL.
     * @throws SVNClientException
     */
    public abstract ISVNProperty[] getRevProperties(SVNUrl url, SVNRevision.Number revision) throws SVNClientException;	
	
	/**
	 * Remove 'conflicted' state on working copy files or directories
	 * @param path
	 * @throws SVNClientException
	 */ 	
	public abstract void resolved(File path) throws SVNClientException;

	/**
	 * Remove 'conflicted' state on working copy files or directories
	 * @param path
	 * @param result - choose resolve option - {@link ISVNConflictResolver.Choice}
	 * @throws SVNClientException
	 */ 	
	public abstract void resolve(File path, int result) throws SVNClientException;
    
	/**
	 * Create a new, empty repository at path 
	 * 
	 * @param path
	 * @param repositoryType either {@link ISVNClientAdapter#REPOSITORY_FSTYPE_BDB} or
	 *        {@link ISVNClientAdapter#REPOSITORY_FSTYPE_FSFS} or null (will use svnadmin default)
	 * @throws SVNClientException
	 */
	public abstract void createRepository(File path, String repositoryType) throws SVNClientException;
	
	/**
	 * Cancel the current operation
	 * 
	 * @throws SVNClientException
	 */
	public void cancelOperation() throws SVNClientException;

	/**
	 * Get information about a file or directory from working copy.
	 * Uses info() call which does NOT contact the repository
	 * @param file
	 * @return information about a file or directory from working copy.
	 * @throws SVNClientException
	 */
	public ISVNInfo getInfoFromWorkingCopy(File file) throws SVNClientException;

	/**
	 * Get information about a file or directory.
	 * Uses info2() call which contacts the repository
	 * @param file
	 * @return information about a file or directory.
	 * @throws SVNClientException
	 */
	public ISVNInfo getInfo(File file) throws SVNClientException;
	
	/**
	 * Get information about a file or directory.
	 * Uses info2() call which contacts the repository
	 * @param file
	 * @param descend get recursive information
	 * @return information about a file or directory.
	 * @throws SVNClientException
	 */
	public ISVNInfo[] getInfo(File file, boolean descend) throws SVNClientException;	

	/**
	 * Get information about an URL.
	 * Uses info2() call which contacts the repository
	 * @param url
	 * @return information about an URL.
	 * @throws SVNClientException
	 */
	public ISVNInfo getInfo(SVNUrl url) throws SVNClientException;

	/**
	 * Get information about an URL.
	 * Uses info2() call which contacts the repository
	 * @param url
	 * @param revision
	 * @param peg
	 * @return information about an URL.
	 * @throws SVNClientException
	 */
	public ISVNInfo getInfo(SVNUrl url, SVNRevision revision, SVNRevision peg) throws SVNClientException;
    
    /**
     * Update the working copy to mirror a new URL within the repository.
     * This behaviour is similar to 'svn update', and is the way to
     * move a working copy to a branch or tag within the same repository.
     * @param url
     * @param path
     * @param revision
     * @param recurse
     * @throws SVNClientException
     */
    public void switchToUrl(File path, SVNUrl url, SVNRevision revision, boolean recurse) throws SVNClientException;
    
    
    /**
     * Update the working copy to mirror a new URL within the repository.
     * This behaviour is similar to 'svn update', and is the way to
     * move a working copy to a branch or tag within the same repository.
     * @param url
     * @param path
     * @param revision
     * @param depth
     * @param setDepth
     * @param ignoreExternals
     * @param force
     * @throws SVNClientException
     */
    public void switchToUrl(File path, SVNUrl url, SVNRevision revision, int depth, boolean setDepth, boolean ignoreExternals, boolean force) throws SVNClientException;        


    /**
     * Update the working copy to mirror a new URL within the repository.
     * This behaviour is similar to 'svn update', and is the way to
     * move a working copy to a branch or tag within the same repository.
     * @param url
     * @param path
     * @param revision
     * @param pegRevision
     * @param depth
     * @param setDepth
     * @param ignoreExternals
     * @param force
     * @throws SVNClientException
     */
    public void switchToUrl(File path, SVNUrl url, SVNRevision revision, SVNRevision pegRevision, int depth, boolean setDepth, boolean ignoreExternals, boolean force) throws SVNClientException;  
    
    /**
     * Update the working copy to mirror a new URL within the repository.
     * This behaviour is similar to 'svn update', and is the way to
     * move a working copy to a branch or tag within the same repository.
     * @param url
     * @param path
     * @param revision
     * @param pegRevision
     * @param depth
     * @param setDepth
     * @param ignoreExternals
     * @param force
     * @param ignoreAncestry
     * @throws SVNClientException
     */
    public void switchToUrl(File path, SVNUrl url, SVNRevision revision, SVNRevision pegRevision, int depth, boolean setDepth, boolean ignoreExternals, boolean force, boolean ignoreAncestry) throws SVNClientException;                
    
    /**
     * Set the configuration directory.
     * @param dir
     * @throws SVNClientException
     */
    public void setConfigDirectory(File dir) throws SVNClientException;
    
    /**
     * Perform a clanup on the working copy.  This will remove any stale transactions
     * @param dir
     * @throws SVNClientException
     */
    public abstract void cleanup(File dir) throws SVNClientException;
    
    /**
     * Recursively upgrade a working copy to a new metadata storage format.
     * @param dir
     * @throws SVNClientException
     */    
    public abstract void upgrade(File dir) throws SVNClientException;

    /**
     * Merge changes from two paths into a new local path.
     * @param path1         first path or url
     * @param revision1     first revision
     * @param path2         second path or url
     * @param revision2     second revision
     * @param localPath     target local path
     * @param force         overwrite local changes
     * @param recurse       traverse into subdirectories
     * @exception SVNClientException
     */
    public abstract void merge(SVNUrl path1, SVNRevision revision1, SVNUrl path2,
               SVNRevision revision2, File localPath, boolean force,
               boolean recurse) throws SVNClientException;

    /**
     * Merge changes from two paths into a new local path.
     * @param path1         first path or url
     * @param revision1     first revision
     * @param path2         second path or url
     * @param revision2     second revision
     * @param localPath     target local path
     * @param force         overwrite local changes
     * @param recurse       traverse into subdirectories
     * @param dryRun        do not update working copy
     * @exception SVNClientException
     */
    public abstract void merge(SVNUrl path1, SVNRevision revision1, SVNUrl path2,
               SVNRevision revision2, File localPath, boolean force,
               boolean recurse, boolean dryRun) throws SVNClientException;    

    /**
     * Merge changes from two paths into a new local path.
     * @param path1         first path or url
     * @param revision1     first revision
     * @param path2         second path or url
     * @param revision2     second revision
     * @param localPath     target local path
     * @param force         overwrite local changes
     * @param recurse       traverse into subdirectories
     * @param dryRun        do not update working copy
     * @param ignoreAncestry ignore ancestry when calculating merges
     * @exception SVNClientException
     */
    public abstract void merge(SVNUrl path1, SVNRevision revision1, SVNUrl path2,
               SVNRevision revision2, File localPath, boolean force,
               boolean recurse, boolean dryRun, boolean ignoreAncestry) throws SVNClientException;    

    /**
     * Merge changes from two paths into a new local path.
     * @param path1         first path or url
     * @param revision1     first revision
     * @param path2         second path or url
     * @param revision2     second revision
     * @param localPath     target local path
     * @param force         overwrite local changes
     * @param int           depth
     * @param dryRun        do not update working copy
     * @param ignoreAncestry ignore ancestry when calculating merges
     * @param recordOnly    just records mergeinfo, does not perform merge
     * @exception SVNClientException
     */
    public abstract void merge(SVNUrl path1, SVNRevision revision1, SVNUrl path2,
               SVNRevision revision2, File localPath, boolean force,
               int depth, boolean dryRun, boolean ignoreAncestry,
               boolean recordOnly) throws SVNClientException;   

    /**
     * Perform a reintegration merge of path into localPath.
     * localPath must be a single-revision, infinite depth,
     * pristine, unswitched working copy -- in other words, it must
     * reflect a single revision tree, the "target".  The mergeinfo on
     * path must reflect that all of the target has been merged into it.
     * Then this behaves like a merge from the target's URL to the
     * localPath.
     *
     * The depth of the merge is always infinity.
     * @param path          path or url
     * @param pegRevision   revision to interpret path
     * @param localPath     target local path
     * @param force         THIS IS NOT CURRENTLY USED
     * @param dryRun        do not update working copy
     * @exception SVNClientException
     */    
    public abstract void mergeReintegrate(SVNUrl path, SVNRevision pegRevision,
    		   File localPath, boolean force, boolean dryRun) throws SVNClientException;
    
    /**
     * Lock a working copy item
     * @param paths  path of the items to lock
     * @param comment
     * @param force break an existing lock
     * @throws SVNClientException
     */
    public abstract void lock(SVNUrl[] paths, String comment, boolean force)
            throws SVNClientException;

    /**
     * Unlock a working copy item
     * @param paths  path of the items to unlock
     * @param force break an existing lock
     * @throws SVNClientException
     */
    public abstract void unlock(SVNUrl[] paths, boolean force)
            throws SVNClientException;

    /**
     * Lock a working copy item
     * @param paths  path of the items to lock
     * @param comment
     * @param force break an existing lock
     * @throws SVNClientException
     */
    public abstract void lock(File[] paths, String comment, boolean force)
            throws SVNClientException;

    /**
     * Unlock a working copy item
     * @param paths  path of the items to unlock
     * @param force break an existing lock
     * @throws SVNClientException
     */
    public abstract void unlock(File[] paths, boolean force)
            throws SVNClientException;

    /**
     * Indicates whether a status call that contacts the
     * server includes the remote info in the status object
     * @return true when the client adapter implementation delivers remote info within status
     */
    public abstract boolean statusReturnsRemoteInfo();

    /**
     * Indicates whether the commitAcrossWC method is
     * supported in the adapter
     * @return true when the client adapter implementation supports commitAcrossWC 
     */
    public abstract boolean canCommitAcrossWC();

    /**
     * Returns the name of the Subversion administrative 
     * working copy directory.  Typically will be ".svn".
     * @return the name of the Subversion administrative wc dir
     */
    public abstract String getAdminDirectoryName();

    /**
     * Returns whether the passed folder name is a Subversion
     * administrative working copy directory.  Will always return
     * true if ".svn" is passed.  Otherwise, will be based on the
     * Subversion runtime
     * @param name
     * @return true whether the folder is a Subversion administrative dir
     */
    public abstract boolean isAdminDirectory(String name);

    /**
     * Rewrite the url's in the working copy
     * @param from      old url
     * @param to        new url
     * @param path      working copy path
     * @param recurse   recurse into subdirectories
     * @throws SVNClientException
     */
    public abstract void relocate(String from, String to, String path, boolean recurse)
            throws SVNClientException;
    
    /**
     * Merge set of revisions into a new local path.
     * @param url          url
     * @param pegRevision   revision to interpret path
     * @param revisions     revisions to merge (must be in the form N-1:M)
     * @param localPath     target local path
     * @param force         overwrite local changes
     * @param depth         how deep to traverse into subdirectories
     * @param ignoreAncestry ignore if files are not related
     * @param dryRun        do not change anything
     * @param recordOnly    just records mergeinfo, does not perform merge
     * @throws SVNClientException
     */
    public abstract void merge(SVNUrl url, SVNRevision pegRevision, SVNRevisionRange[] revisions,
               File localPath, boolean force, int depth,
               boolean ignoreAncestry, boolean dryRun,
               boolean recordOnly) throws SVNClientException;

    /**
     * Get merge info for <code>path</code> at <code>revision</code>.
     * @param path Local Path.
     * @param revision SVNRevision at which to get the merge info for
     * <code>path</code>.
     * @throws SVNClientException
     */
    public abstract ISVNMergeInfo getMergeInfo(File path, SVNRevision revision)
        throws SVNClientException;

    /**
     * Get merge info for <code>url</code> at <code>revision</code>.
     * @param url URL.
     * @param revision SVNRevision at which to get the merge info for
     * <code>path</code>.
     * @throws SVNClientException
     */
    public abstract ISVNMergeInfo getMergeInfo(SVNUrl url, SVNRevision revision)
        throws SVNClientException;

    /**
     * Retrieve either merged or eligible-to-be-merged revisions.
     * @param kind                   kind of revisions to receive
     * @param path                   target of merge
     * @param pegRevision            peg rev for path
     * @param mergeSourceUrl         the source of the merge
     * @param srcPegRevision         peg rev for mergeSourceUrl
     * @param discoverChangedPaths   return paths of changed items
     * @return array of log messages
     * @throws SVNClientException
     */
    public abstract ISVNLogMessage[] getMergeinfoLog(int kind, File path,
            SVNRevision pegRevision, SVNUrl mergeSourceUrl, SVNRevision srcPegRevision,
            boolean discoverChangedPaths) throws SVNClientException;

    /**
     * Retrieve either merged or eligible-to-be-merged revisions.
     * @param kind                   kind of revisions to receive
     * @param url                    target of merge
     * @param pegRevision            peg rev for path
     * @param mergeSourceUrl         the source of the merge
     * @param srcPegRevision         peg rev for mergeSourceUrl
     * @param discoverChangedPaths   return paths of changed items
     * @return array of log messages
     * @throws SVNClientException
     */
    public abstract ISVNLogMessage[] getMergeinfoLog(int kind, SVNUrl url,
            SVNRevision pegRevision, SVNUrl mergeSourceUrl, SVNRevision srcPegRevision,
            boolean discoverChangedPaths) throws SVNClientException;


  /**
   * Produce a diff summary which lists the items changed between
   * path and revision pairs.
   *
   * @param target1 URL.
   * @param revision1 Revision of <code>target1</code>.
   * @param target2 URL.
   * @param revision2 Revision of <code>target2</code>.
   * @param depth how deep to recurse.
   * @param ignoreAncestry Whether to ignore unrelated files during
   * comparison.  False positives may potentially be reported if
   * this parameter <code>false</code>, since a file might have been
   * modified between two revisions, but still have the same
   * contents.
   * @return the list of differences
   *
   * @throws SVNClientException
   */
  public abstract SVNDiffSummary[] diffSummarize(SVNUrl target1, SVNRevision revision1,
                     SVNUrl target2, SVNRevision revision2,
                     int depth, boolean ignoreAncestry)
          throws SVNClientException;

  /**
   * Produce a diff summary which lists the items changed between
   * path and revision pairs.
   *
   * @param target URL.
   * @param pegRevision Revision at which to interpret
   * <code>target</code>.  If {@link RevisionKind#unspecified} or
   * <code>null</code>, behave identically to {@link
   * diffSummarize(String, Revision, String, Revision, boolean,
   * boolean, DiffSummaryReceiver)}, using <code>path</code> for
   * both of that method's targets.
   * @param startRevision Beginning of range for comparsion of
   * <code>target</code>.
   * @param endRevision End of range for comparsion of
   * <code>target</code>.
   * @param depth how deep to recurse.
   * @param ignoreAncestry Whether to ignore unrelated files during
   * comparison.  False positives may potentially be reported if
   * this parameter <code>false</code>, since a file might have been
   * modified between two revisions, but still have the same
   * contents.
   * @return the list of differences
   *
   * @throws SVNClientException
   */
  public abstract SVNDiffSummary[] diffSummarize(SVNUrl target, SVNRevision pegRevision,
                     SVNRevision startRevision, SVNRevision endRevision,
                     int depth, boolean ignoreAncestry)
      throws SVNClientException;

  public abstract SVNDiffSummary[] diffSummarize(File path, SVNUrl toUrl, SVNRevision toRevision, boolean recurse)
  	  throws SVNClientException;
  
  /**
   * Return an ordered list of suggested merge source URLs.
   * @param path The merge target path for which to suggest sources.
   * @return The list of URLs, empty if there are no suggestions.
   * @throws SVNClientException If an error occurs.
   */
  public abstract String[] suggestMergeSources(File path)
          throws SVNClientException;

  /**
   * Return an ordered list of suggested merge source URLs.
   * @param url The merge target path for which to suggest sources.
   * @param peg The peg revision for the URL
   * @return The list of URLs, empty if there are no suggestions.
   * @throws SVNClientException If an error occurs.
   */
  public abstract String[] suggestMergeSources(SVNUrl url, SVNRevision peg)
          throws SVNClientException;

  /**
   * release the native peer (should not depend on finalize)
   */
  public abstract void dispose();
  
}
