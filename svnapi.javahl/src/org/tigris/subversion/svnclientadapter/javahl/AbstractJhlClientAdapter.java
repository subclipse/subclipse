/*******************************************************************************
 * Copyright (c) 2005, 2006 svnClientAdapter project and others.
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
package org.tigris.subversion.svnclientadapter.javahl;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import org.apache.subversion.javahl.ClientException;
import org.apache.subversion.javahl.ConflictResult;
import org.apache.subversion.javahl.ISVNClient;
import org.apache.subversion.javahl.SubversionException;
import org.apache.subversion.javahl.callback.ListCallback;
import org.apache.subversion.javahl.callback.StatusCallback;
import org.apache.subversion.javahl.callback.UserPasswordCallback;
import org.apache.subversion.javahl.types.CopySource;
import org.apache.subversion.javahl.types.Depth;
import org.apache.subversion.javahl.types.DirEntry;
import org.apache.subversion.javahl.types.Lock;
import org.apache.subversion.javahl.types.Mergeinfo;
import org.apache.subversion.javahl.types.Revision;
import org.apache.subversion.javahl.types.RevisionRange;
import org.apache.subversion.javahl.types.Status;
import org.tigris.subversion.svnclientadapter.AbstractClientAdapter;
import org.tigris.subversion.svnclientadapter.ISVNAnnotations;
import org.tigris.subversion.svnclientadapter.ISVNConflictResolver;
import org.tigris.subversion.svnclientadapter.ISVNDirEntry;
import org.tigris.subversion.svnclientadapter.ISVNDirEntryWithLock;
import org.tigris.subversion.svnclientadapter.ISVNInfo;
import org.tigris.subversion.svnclientadapter.ISVNLogMessage;
import org.tigris.subversion.svnclientadapter.ISVNLogMessageCallback;
import org.tigris.subversion.svnclientadapter.ISVNMergeInfo;
import org.tigris.subversion.svnclientadapter.ISVNMergeinfoLogKind;
import org.tigris.subversion.svnclientadapter.ISVNNotifyListener;
import org.tigris.subversion.svnclientadapter.ISVNProgressListener;
import org.tigris.subversion.svnclientadapter.ISVNPromptUserPassword;
import org.tigris.subversion.svnclientadapter.ISVNProperty;
import org.tigris.subversion.svnclientadapter.ISVNStatus;
import org.tigris.subversion.svnclientadapter.ISVNStatusCallback;
import org.tigris.subversion.svnclientadapter.SVNBaseDir;
import org.tigris.subversion.svnclientadapter.SVNClientException;
import org.tigris.subversion.svnclientadapter.SVNDiffSummary;
import org.tigris.subversion.svnclientadapter.SVNDiffSummary.SVNDiffKind;
import org.tigris.subversion.svnclientadapter.SVNInfoUnversioned;
import org.tigris.subversion.svnclientadapter.SVNLogMessageCallback;
import org.tigris.subversion.svnclientadapter.SVNNodeKind;
import org.tigris.subversion.svnclientadapter.SVNNotificationHandler;
import org.tigris.subversion.svnclientadapter.SVNRevision;
import org.tigris.subversion.svnclientadapter.SVNRevisionRange;
import org.tigris.subversion.svnclientadapter.SVNScheduleKind;
import org.tigris.subversion.svnclientadapter.SVNStatusKind;
import org.tigris.subversion.svnclientadapter.SVNStatusUnversioned;
import org.tigris.subversion.svnclientadapter.SVNUrl;
import org.tigris.subversion.svnclientadapter.utils.Messages;

/**
 * This is a base class for the JavaHL Adapter.  It allows the JavaHL
 * Adapter and the SVNKit Adapter to share most of their implementation.
 * 
 * The SVNKit Adapter works by providing an implementation of the JavaHL
 * SVNClientInterface. 
 *
 */
public abstract class AbstractJhlClientAdapter extends AbstractClientAdapter {

    protected ISVNClient svnClient;
    protected JhlNotificationHandler notificationHandler;
    protected JhlConflictResolver conflictResolver;
    protected JhlProgressListener progressListener;
    
    private String postCommitError;

    public AbstractJhlClientAdapter() {

    }

	/**
	 * for users who want to directly use underlying javahl SVNClientInterface
	 * @return the SVNClientInterface instance
	 */
	public ISVNClient getSVNClient() {
		return svnClient;
	}
 
    /**
     * the default prompter : never prompts the user
     */
    public static class DefaultPromptUserPassword implements UserPasswordCallback {

        public int askTrustSSLServer(String info, boolean allowPermanently) {
			return UserPasswordCallback.AcceptTemporary;
		}

		public String askQuestion(String realm, String question, boolean showAnswer) {
            return "";
		}

        public boolean askYesNo(String realm, String question, boolean yesIsDefault) {
			return yesIsDefault;
		}

		public String getPassword() {
			return "";
		}

		public String getUsername() {
			return "";
		}

        public boolean prompt(String realm, String username) {
			return false;
		}

		public boolean prompt(String realm, String username, boolean maySave) {
			return false;
		}

		public String askQuestion(String realm, String question,
				boolean showAnswer, boolean maySave) {
			return null;
		}

		public boolean userAllowedSave() {
			return false;
		}
    }

    /* (non-Javadoc)
     * @see org.tigris.subversion.svnclientadapter.ISVNClientAdapter#addNotifyListener(org.tigris.subversion.svnclientadapter.ISVNNotifyListener)
     */
    public void addNotifyListener(ISVNNotifyListener listener) {
        notificationHandler.add(listener);
    }

    /* (non-Javadoc)
     * @see org.tigris.subversion.svnclientadapter.ISVNClientAdapter#removeNotifyListener(org.tigris.subversion.svnclientadapter.ISVNNotifyListener)
     */
    public void removeNotifyListener(ISVNNotifyListener listener) {
        notificationHandler.remove(listener);
    }

    /* (non-Javadoc)
	 * @see org.tigris.subversion.svnclientadapter.ISVNClientAdapter#getNotificationHandler()
	 */
	public SVNNotificationHandler getNotificationHandler() {
		return notificationHandler;
	}

	/* (non-Javadoc)
     * @see org.tigris.subversion.svnclientadapter.ISVNClientAdapter#setUsername(java.lang.String)
     */
    public void setUsername(String username) {
        svnClient.username(username);
    }

    /* (non-Javadoc)
     * @see org.tigris.subversion.svnclientadapter.ISVNClientAdapter#setPassword(java.lang.String)
     */
    public void setPassword(String password) {
        notificationHandler.setCommand(ISVNNotifyListener.Command.UNDEFINED);
        svnClient.password(password);
    }

    /**
     * Register callback interface to supply username and password on demand
     * @param prompt
     */
    public void setPromptUserPassword(UserPasswordCallback prompt) {
        svnClient.setPrompt(prompt);        
    }

    protected static String fileToSVNPath(File file, boolean canonical) {
    	if (file == null) return null;
    	// SVN need paths with '/' separators
    	if (canonical) {
            try {
	   	       return file.getCanonicalPath().replace('\\', '/');    		 
    	   } catch (IOException e)
    	   {
    	       return null;
    	   }
        } else
            return file.getPath().replace('\\', '/');
    }
    
    /* (non-Javadoc)
     * @see org.tigris.subversion.svnclientadapter.ISVNClientAdapter#addFile(java.io.File)
     */
    public void addFile(File file) throws SVNClientException {
        try{
            notificationHandler.setCommand(ISVNNotifyListener.Command.ADD);
            notificationHandler.logCommandLine("add -N "+file.toString());
			notificationHandler.setBaseDir(SVNBaseDir.getBaseDir(file));
            svnClient.add(fileToSVNPath(file, false), Depth.infinity, false, true, true);
        } catch (ClientException e) {
            notificationHandler.logException(e);
            throw new SVNClientException(e);
        }        
    }

	/* (non-Javadoc)
	 * @see org.tigris.subversion.subclipse.client.ISVNClientAdapter#addDirectory(java.io.File, boolean)
	 */
	public void addDirectory(File file, boolean recurse) throws SVNClientException {
		addDirectory(file, recurse, false);
	}

    /* (non-Javadoc)
     * @see org.tigris.subversion.svnclientadapter.ISVNClientAdapter#addDirectory(java.io.File, boolean, boolean)
     */
    public void addDirectory(File dir, boolean recurse, boolean force)
        throws SVNClientException {
        try {
            notificationHandler.setCommand(ISVNNotifyListener.Command.ADD);            
            notificationHandler.logCommandLine(
                "add"+
                (recurse?"":" -N")+
                (force?" --force":"")+
                " "+dir.toString());
			notificationHandler.setBaseDir(SVNBaseDir.getBaseDir(dir));
			boolean noIgnores = false;
			boolean addParents = true;
            svnClient.add(fileToSVNPath(dir, false), Depth.infinityOrEmpty(recurse), force, noIgnores, addParents);
        } catch (ClientException e) {
            notificationHandler.logException(e);
            throw new SVNClientException(e);
        }
    }

    /* (non-Javadoc)
     * @see org.tigris.subversion.svnclientadapter.ISVNClientAdapter#checkout(org.tigris.subversion.svnclientadapter.SVNUrl, java.io.File, org.tigris.subversion.svnclientadapter.SVNRevision, boolean)
     */
    public void checkout(
        SVNUrl moduleName,
        File destPath,
        SVNRevision revision,
        boolean recurse)
        throws SVNClientException {
    		checkout(moduleName, destPath, revision, Depth.infinityOrImmediates(recurse).ordinal(), false, true);
    }
    
    /* (non-Javadoc)
     * @see org.tigris.subversion.svnclientadapter.ISVNClientAdapter#checkout(org.tigris.subversion.svnclientadapter.SVNUrl, java.io.File, org.tigris.subversion.svnclientadapter.SVNRevision, int, boolean, boolean)
     */
    public void checkout(
        SVNUrl moduleName,
        File destPath,
        SVNRevision revision,
        int depth,
        boolean ignoreExternals,
        boolean force)
        throws SVNClientException {
        try {
        	String url = moduleName.toString();
        	Depth d = JhlConverter.depth(depth);
            notificationHandler.setCommand(ISVNNotifyListener.Command.CHECKOUT);
            StringBuffer commandLine = new StringBuffer("checkout " + url +
            		" -r " + revision.toString() + depthCommandLine(d));
            if (ignoreExternals) commandLine.append(" --ignore-externals");
            if (force) commandLine.append(" --force");            
            notificationHandler.logCommandLine(commandLine.toString());
			notificationHandler.setBaseDir(new File("."));
            svnClient.checkout(
			    url,
                fileToSVNPath(destPath, false),
                JhlConverter.convert(revision),
                JhlConverter.convert(revision),
                d,
                ignoreExternals,
                force);
        } catch (ClientException e) {
            notificationHandler.logException(e);
            throw new SVNClientException(e);
        }
    }    

    /* (non-Javadoc)
     * @see org.tigris.subversion.svnclientadapter.ISVNClientAdapter#commit(java.io.File[], java.lang.String, boolean)
     */
    public long commit(File[] paths, String message, boolean recurse)
        throws SVNClientException {
        return commit(paths, message, recurse, false);
    }

    /* (non-Javadoc)
     * @see org.tigris.subversion.svnclientadapter.ISVNClientAdapter#commit(java.io.File[], java.lang.String, boolean, boolean)
     */
    public long commit(File[] paths, String message, boolean recurse, boolean keepLocks)
        throws SVNClientException {
        try {
        	postCommitError = null;
        	String fixedMessage = fixSVNString(message);
        	if (fixedMessage == null)
        		fixedMessage = "";
            notificationHandler.setCommand(ISVNNotifyListener.Command.COMMIT);
            Set<String> files = new HashSet<String>(paths.length);
            String commandLine = "commit -m \""+getFirstMessageLine(fixedMessage)+"\"";
            if (!recurse)
                commandLine+=" -N";
            if (keepLocks)
                commandLine+=" --no-unlock";

            for (int i = 0; i < paths.length; i++) {
                files.add(fileToSVNPath(paths[i], false));
            }
            commandLine = appendPaths(commandLine, files);
            notificationHandler.logCommandLine(commandLine);
			notificationHandler.setBaseDir(SVNBaseDir.getBaseDir(paths));

			boolean keepChangeLists = false;
			JhlCommitCallback callback = new JhlCommitCallback();
            svnClient.commit(files, Depth.infinityOrEmpty(recurse), keepLocks, keepChangeLists, null, null, new JhlCommitMessage(fixedMessage), callback);
            postCommitError = callback.getPostCommitError();
            long newRev = callback.getRevision();
            if (newRev > 0)
            	notificationHandler.logCompleted("Committed revision " + newRev + ".");
            return newRev;
        } catch (ClientException e) {
            notificationHandler.logException(e);
            throw new SVNClientException(e);
        }

    }

	public String getPostCommitError() {
		return postCommitError;
	}

	/* (non-Javadoc)
	 * @see org.tigris.subversion.svnclientadapter.ISVNClientAdapter#getList(org.tigris.subversion.svnclientadapter.SVNUrl, org.tigris.subversion.svnclientadapter.SVNRevision, boolean)
	 */
	public ISVNDirEntry[] getList(SVNUrl url, SVNRevision revision, boolean recurse) 
            throws SVNClientException {
        String target = url.toString();
        return list(target, revision, SVNRevision.HEAD, recurse);
	}
	
	/* (non-Javadoc)
	 * @see org.tigris.subversion.svnclientadapter.ISVNClientAdapter#getList(org.tigris.subversion.svnclientadapter.SVNUrl, org.tigris.subversion.svnclientadapter.SVNRevision, org.tigris.subversion.svnclientadapter.SVNRevision, boolean)
	 */
	public ISVNDirEntry[] getList(SVNUrl url, SVNRevision revision, SVNRevision pegRevision, boolean recurse) 
            throws SVNClientException {
        String target = url.toString();
        return list(target, revision, pegRevision, recurse);
	}	

	/* (non-Javadoc)
	 * @see org.tigris.subversion.svnclientadapter.ISVNClientAdapter#getList(java.io.File, org.tigris.subversion.svnclientadapter.SVNRevision, boolean)
	 */
	public ISVNDirEntry[] getList(File path, SVNRevision revision, boolean recurse) 
            throws SVNClientException {
        String target = fileToSVNPath(path, false);
        return list(target, revision, SVNRevision.HEAD, recurse);
	}
	
	/* (non-Javadoc)
	 * @see org.tigris.subversion.svnclientadapter.ISVNClientAdapter#getList(java.io.File, org.tigris.subversion.svnclientadapter.SVNRevision, org.tigris.subversion.svnclientadapter.SVNRevision, boolean)
	 */
	public ISVNDirEntry[] getList(File path, SVNRevision revision, SVNRevision pegRevision, boolean recurse) 
            throws SVNClientException {
        String target = fileToSVNPath(path, false);
        return list(target, revision, pegRevision, recurse);
 	}	
	
	private ISVNDirEntry[] list(String path, SVNRevision revision, SVNRevision pegRevision, boolean recurse)
			throws SVNClientException {
		final List<JhlDirEntry> dirEntryList = new ArrayList<JhlDirEntry>();
		ListCallback callback = new ListCallback() {
			public void doEntry(DirEntry dirent, Lock lock) {
	            if (dirent.getPath().length() == 0)
	            {
	                if (dirent.getNodeKind() == org.apache.subversion.javahl.types.NodeKind.file)
	                {
	                    String absPath = dirent.getAbsPath();
	                    int lastSeparator = absPath.lastIndexOf('/');
	                    String path = absPath.substring(lastSeparator,
	                                                    absPath.length());
	                    dirent.setPath(path);
	                }
	                else
	                {
	                    // Don't add requested directory.        	
	                    return;
	                }
	            }

	            dirEntryList.add(new JhlDirEntry(dirent));
				
			}

		};
		try {
            notificationHandler.setCommand(ISVNNotifyListener.Command.LS);
            String commandLine = "list -r "+revision.toString()+(recurse?"-R":"")+" "+path;
            notificationHandler.logCommandLine(commandLine);
			notificationHandler.setBaseDir(new File("."));
			svnClient.list(path, JhlConverter.convert(revision), JhlConverter.convert(pegRevision), Depth.infinityOrImmediates(recurse), DirEntry.Fields.all, false, callback);
		} catch (ClientException e) {
	        notificationHandler.logException(e);
	        throw new SVNClientException(e);
		}
		ISVNDirEntry[] dirEntries = new ISVNDirEntry[dirEntryList.size()];
		dirEntryList.toArray(dirEntries);
		return dirEntries;
	}	
	
	public ISVNDirEntryWithLock[] getListWithLocks(SVNUrl url, SVNRevision revision, SVNRevision pegRevision, boolean recurse)
			throws SVNClientException {
		final List<JhlDirEntryWithLock> dirEntryList = new ArrayList<JhlDirEntryWithLock>();
		ListCallback callback = new ListCallback() {
			public void doEntry(DirEntry dirent, Lock lock) {
	            if (dirent.getPath().length() == 0)
	            {
	                if (dirent.getNodeKind() == org.apache.subversion.javahl.types.NodeKind.file)
	                {
	                    String absPath = dirent.getAbsPath();
	                    int lastSeparator = absPath.lastIndexOf('/');
	                    String path = absPath.substring(lastSeparator,
	                                                    absPath.length());
	                    dirent.setPath(path);
	                }
	                else
	                {
	                    // Don't add requested directory.        	
	                    return;
	                }
	            }

	            dirEntryList.add(new JhlDirEntryWithLock(dirent, lock));
				
			}

		};
		try {
            notificationHandler.setCommand(ISVNNotifyListener.Command.LS);
            String commandLine = "list -r "+revision.toString()+(recurse?"-R":"")+" "+url.toString();
            notificationHandler.logCommandLine(commandLine);
			notificationHandler.setBaseDir(new File("."));
			svnClient.list(url.toString(), JhlConverter.convert(revision), JhlConverter.convert(pegRevision), Depth.infinityOrImmediates(recurse), DirEntry.Fields.all, true, callback);
		} catch (ClientException e) {
	        notificationHandler.logException(e);
	        throw new SVNClientException(e);
		}
		ISVNDirEntryWithLock[] dirEntries = new ISVNDirEntryWithLock[dirEntryList.size()];
		dirEntryList.toArray(dirEntries);
		return dirEntries;
	}

	/*
	 * (non-Javadoc)
	 * @see org.tigris.subversion.svnclientadapter.ISVNClientAdapter#getDirEntry(org.tigris.subversion.svnclientadapter.SVNUrl, org.tigris.subversion.svnclientadapter.SVNRevision)
	 */
	public ISVNDirEntry getDirEntry(SVNUrl url, SVNRevision revision)
		throws SVNClientException {
		
		// list give the DirEntrys of the elements of a directory or the DirEntry
		// of a file
		ISVNDirEntry[] entries = getList(url.getParent(), revision,false);
		String expectedPath = url.getLastPathSegment();
		for (int i = 0; i < entries.length;i++) {
			if (entries[i].getPath().equals(expectedPath)) {
				return entries[i];
			}
		}
		return null; // not found
	}

	/*
	 * (non-Javadoc)
	 * @see org.tigris.subversion.svnclientadapter.ISVNClientAdapter#getDirEntry(java.io.File, org.tigris.subversion.svnclientadapter.SVNRevision)
	 */
	public ISVNDirEntry getDirEntry(File path, SVNRevision revision) 
		throws SVNClientException {

		// list give the DirEntrys of the elements of a directory or the DirEntry
		// of a file
		ISVNDirEntry[] entries = getList(path.getParentFile(), revision,false);
		String expectedPath = path.getName();
		for (int i = 0; i < entries.length;i++) {
			if (entries[i].getPath().equals(expectedPath)) {
				return entries[i];
			}
		}
		return null; // not found
	}
	
    /* (non-Javadoc)
     * @see org.tigris.subversion.svnclientadapter.ISVNClientAdapter#getSingleStatus(java.io.File)
     */
    public ISVNStatus getSingleStatus(File path) 
            throws SVNClientException {
        return getStatus(new File[] {path})[0];
    }
    
    /* (non-Javadoc)
     * @see org.tigris.subversion.svnclientadapter.ISVNClientAdapter#getStatus(java.io.File[])
     */
	public ISVNStatus[] getStatus(File[] path) throws SVNClientException {
		ISVNStatus[] statuses = new ISVNStatus[path.length];
		for (int i = 0; i < path.length; i++) {
			ISVNStatus[] s = getStatus(path[i], false, true, false, false);
			if (s == null || s.length == 0) {
				statuses[i] = new SVNStatusUnversioned(path[i]);
			} else {
				statuses[i] = s[0];
			}
		}
		return statuses;
	}

    /* (non-Javadoc)
     * @see org.tigris.subversion.svnclientadapter.ISVNClientAdapter#getStatus(java.io.File, boolean, boolean)
     */
    public ISVNStatus[] getStatus(File path, boolean descend, boolean getAll)
		throws SVNClientException {
		return getStatus(path, descend,getAll,false); 
	}

    /* (non-Javadoc)
     * @see org.tigris.subversion.svnclientadapter.ISVNClientAdapter#getStatus(java.io.File, boolean, boolean, boolean)
     */
    public ISVNStatus[] getStatus(File path, boolean descend, boolean getAll, boolean contactServer) throws SVNClientException {
    	return getStatus(path, descend, getAll, contactServer, false);
    }
    
    /* (non-Javadoc)
     * @see org.tigris.subversion.svnclientadapter.ISVNClientAdapter#getStatus(java.io.File, boolean, boolean, boolean, boolean)
     */
    public ISVNStatus[] getStatus(File path, boolean descend, boolean getAll, boolean contactServer, boolean ignoreExternals) throws SVNClientException {
    	return getStatus(path, descend, getAll, contactServer, ignoreExternals, null);
    }
    
    /* (non-Javadoc)
     * @see org.tigris.subversion.svnclientadapter.ISVNClientAdapter#getStatus(java.io.File, boolean, boolean, boolean, boolean, ISVNStatusCallback)
     */
    public ISVNStatus[] getStatus(File path, boolean descend, boolean getAll, boolean contactServer, boolean ignoreExternals, ISVNStatusCallback callback) throws SVNClientException {
    	return getStatus(path, descend, getAll, contactServer, ignoreExternals, true, callback);
    }

    /* (non-Javadoc)
     * @see org.tigris.subversion.svnclientadapter.ISVNClientAdapter#getStatus(java.io.File, boolean, boolean, boolean, boolean, boolean, ISVNStatusCallback)
     */
    public ISVNStatus[] getStatus(File path, boolean descend, boolean getAll, boolean contactServer, boolean ignoreExternals, boolean noIgnore, ISVNStatusCallback callback) throws SVNClientException {
		notificationHandler.setCommand(ISVNNotifyListener.Command.STATUS);
		String filePathSVN = fileToSVNPath(path, false);
		Depth depth = Depth.unknownOrImmediates(descend);    // If descend is true, recurse fully, else do only immediate children.
		notificationHandler.logCommandLine("status" + (contactServer?" -u":"")+ depthCommandLine(depth) + " " + filePathSVN);
		notificationHandler.setBaseDir(SVNBaseDir.getBaseDir(path));
		try {
			StatusCallback statusCallback;
			if (callback == null) {
				statusCallback = new MyStatusCallback();
			}
			else {
				statusCallback = new JhlStatusCallback(callback);
			}
			svnClient.status(
                    filePathSVN,  
                    depth,        
                    contactServer,      // If update is set, contact the repository and augment the status structures with information about out-of-dateness     
					getAll,noIgnore,	// retrieve all entries; otherwise, retrieve only "interesting" entries (local mods and/or out-of-date).
					ignoreExternals, null, statusCallback);

			List<Status> statusList = null;
			if (statusCallback instanceof MyStatusCallback) {
				statusList = ((MyStatusCallback)statusCallback).getStatusList();
			}
			else {
				statusList = ((JhlStatusCallback)statusCallback).getStatusList();
			}
			return processFolderStatuses(processExternalStatuses(JhlConverter.convertStatus(
					statusList, svnClient)), getAll, contactServer);  // if yes the svn:externals will be ignored
		} catch (ClientException e) {
			if (e.getAprError() == ErrorCodes.wcNotDirectory || e.getAprError() == ErrorCodes.wcPathNotFound) {
				// when folder is unversioned, an exception is thrown ...
				return new ISVNStatus[] {new SVNStatusUnversioned(path)};
			} else {
				notificationHandler.logException(e);
				throw new SVNClientException(e);
			}
		}
    }
    /**
     * A private status callback implementation used by thin wrappers.
     * Instances of this class are not thread-safe.
     */
    private class MyStatusCallback implements StatusCallback
    {
        public void doStatus(String path, Status status) {
        	// Status can be null, in which case you are supposed to use the
        	// String to construct an otherwise null status object.  I am not sure
        	// of the use-case for this right now, so I am just going to discard them
        	if (status != null)
        		statuses.add(status);
		}

		private List<Status> statuses = new ArrayList<Status>();

        public List<Status> getStatusList()
        {
            return statuses;
        }
    }

    /**
     * Post-process svn:externals statuses.
     * JavaHL answer two sort of statuses on externals:
     * - when ignoreExternals is set to true during call to status(),
     *  the returned status has textStatus set to EXTERNAL, but the url is null.<br>
     * - when ignoreExternals is set to false during call to status(),
     *  besides the "external + null" status, the second status with url and all fields is returned too, 
     *  but this one has textStatus NORMAL.
     *  
     *  This methods unifies both statuses to be complete and has textStatus external.
     *  In case the first sort (when ignoreExternals true), the url is retrieved by call the info()
     */
    protected JhlStatus[] processExternalStatuses(JhlStatus[] statuses) throws SVNClientException
    {
    	//Collect indexes of external statuses
    	List<Integer> externalStatusesIndexes = new ArrayList<Integer>();
    	for (int i = 0; i < statuses.length; i++) {
    		if (SVNStatusKind.EXTERNAL.equals(statuses[i].getTextStatus())) {
    			externalStatusesIndexes.add(new Integer(i));
    		}
		}
    	
    	if (externalStatusesIndexes.isEmpty()) {
    		return statuses;
    	}
    	
    	//Wrap the "second" externals so their textStatus is actually external
    	for (Integer integer : externalStatusesIndexes) {
    		int index = integer.intValue();
			JhlStatus jhlStatus = statuses[index];
			for (int i = 0; i < statuses.length; i++) {
				if ((statuses[i].getPath() != null) && (statuses[i].getPath().equals(jhlStatus.getPath()))) {
					statuses[i] = new JhlStatus.JhlStatusExternal(statuses[i], svnClient);
					statuses[index] = statuses[i];
				}
			}
		}
    	
    	return statuses;
    }
    /**
     * Post-process statuses.
     * Folders do not return proper lastChangedRevision information.
     * this allows it to be populated via the svn info command
     */
    protected ISVNStatus[] processFolderStatuses(JhlStatus[] statuses, boolean getAll, boolean contactServer) throws SVNClientException
    {
    	if (!getAll || !contactServer)
    		return statuses;
    	//Fill the missing last changed info on folders from the file info in the array
     	List<JhlStatus> folders = new ArrayList<JhlStatus>();
    	for (int i = 0; i < statuses.length; i++) {
			JhlStatus jhlStatus = statuses[i];
			if (SVNNodeKind.DIR == jhlStatus.getNodeKind() && jhlStatus.getReposLastChangedRevision() == null) {
				folders.add(jhlStatus);
			}
		}
    	for (int i = 0; i < statuses.length; i++) {
			JhlStatus jhlStatus = statuses[i];
			if (jhlStatus.getLastChangedRevision() != null) {
				for (JhlStatus folder : folders) {
					if (jhlStatus.getUrlString() != null && jhlStatus.getUrlString().startsWith(folder.getUrlString() + "/")) {
						if (folder.getLastChangedRevision() == null ||
								folder.getLastChangedRevision().getNumber() < jhlStatus.getLastChangedRevision().getNumber()) {
							folder.updateFromStatus(jhlStatus);
						}
					}
				}
			}
		}
    	return statuses;
    }
    
	/* (non-Javadoc)
	 * @see org.tigris.subversion.svnclientadapter.ISVNClientAdapter#copy(java.io.File, java.io.File)
	 */
	public void copy(File srcPath, File destPath) throws SVNClientException {
		try {
			notificationHandler.setCommand(ISVNNotifyListener.Command.COPY);

			String src = fileToSVNPath(srcPath, false);
			String dest = fileToSVNPath(destPath, false);
			notificationHandler.logCommandLine("copy " + src + " " + dest);
			notificationHandler.setBaseDir(SVNBaseDir.getBaseDir(new File[] {srcPath,destPath }));

			List<CopySource> copySources = new ArrayList<CopySource>();
			copySources.add(new CopySource(src, Revision.WORKING, Revision.WORKING));
			svnClient.copy(copySources, dest, true, true, false, null, null, null);
			
		} catch (ClientException e) {
			notificationHandler.logException(e);
			throw new SVNClientException(e);
		}
	}

	/* (non-Javadoc)
	 * @see org.tigris.subversion.svnclientadapter.ISVNClientAdapter#copy(java.io.File, org.tigris.subversion.svnclientadapter.SVNUrl, java.lang.String)
	 */
	public void copy(File srcPath, SVNUrl destUrl, String message)
		throws SVNClientException {
		try {
        	String fixedMessage = fixSVNString(message);
        	if (fixedMessage == null)
        		fixedMessage = "";
			notificationHandler.setCommand(ISVNNotifyListener.Command.COPY);
			String src = fileToSVNPath(srcPath, false);
			String dest = destUrl.toString();
			notificationHandler.logCommandLine("copy " + src + " " + dest);
			notificationHandler.setBaseDir(SVNBaseDir.getBaseDir(srcPath));
			List<CopySource> copySources = new ArrayList<CopySource>();
			copySources.add(new CopySource(src, Revision.WORKING, Revision.WORKING));
			svnClient.copy(copySources, dest, true, true, true, null, new JhlCommitMessage(fixedMessage), null);
			// last parameter is not used
		} catch (ClientException e) {
			notificationHandler.logException(e);
			throw new SVNClientException(e);
		}
	}
	
	/* (non-Javadoc)
	 * @see org.tigris.subversion.svnclientadapter.ISVNClientAdapter#copy(java.io.File[], org.tigris.subversion.svnclientadapter.SVNUrl, java.lang.String, boolean, boolean)
	 */
	public void copy(File[] srcPaths, SVNUrl destUrl, String message, boolean copyAsChild, boolean makeParents)
		throws SVNClientException {
		
    	String fixedMessage = fixSVNString(message);

		// This is a hack for now since copy of multiple isolated WC's is currently not working.
		if (srcPaths.length > 1) {
			mkdir(destUrl, makeParents, fixedMessage);
			for (int i = 0; i < srcPaths.length; i++) {
				File[] file = { srcPaths[i] };
				copy(file, destUrl, fixedMessage, copyAsChild, makeParents);
			}
			return;
		}
		
		try {
        	if (fixedMessage == null)
        		fixedMessage = "";
			notificationHandler.setCommand(ISVNNotifyListener.Command.COPY);
			List<CopySource> copySources = new ArrayList<CopySource>(srcPaths.length);
			for (int i = 0; i < srcPaths.length; i++) 
				copySources.add(new CopySource(fileToSVNPath(srcPaths[i], false), Revision.WORKING, Revision.WORKING));	
			String dest = destUrl.toString();
			String commandLine = "copy";
			Set<String> paths = new HashSet<String>(srcPaths.length);
			for (int i = 0; i < srcPaths.length; i++) {
				paths.add(fileToSVNPath(srcPaths[i], false));
			}
			commandLine = appendPaths(commandLine, paths) + " " + dest;
			notificationHandler.logCommandLine(commandLine.toString());
			notificationHandler.setBaseDir();
			svnClient.copy(copySources, dest, copyAsChild, makeParents, true, null, new JhlCommitMessage(fixedMessage), null);
		} catch (ClientException e) {
			notificationHandler.logException(e);
			throw new SVNClientException(e);
		}
	}		
	
	/* (non-Javadoc)
	 * @see org.tigris.subversion.svnclientadapter.ISVNClientAdapter#copy(org.tigris.subversion.svnclientadapter.SVNUrl, java.io.File, org.tigris.subversion.svnclientadapter.SVNRevision)
	 */
	public void copy(SVNUrl srcUrl, File destPath, SVNRevision revision)
		throws SVNClientException {
		copy(srcUrl, destPath, revision, true, false);
	}
	
	/* (non-Javadoc)
	 * @see org.tigris.subversion.svnclientadapter.ISVNClientAdapter#copy(org.tigris.subversion.svnclientadapter.SVNUrl, java.io.File, org.tigris.subversion.svnclientadapter.SVNRevision, boolean, boolean)
	 */
	public void copy(SVNUrl srcUrl, File destPath, SVNRevision revision, boolean copyAsChild, boolean makeParents)
		throws SVNClientException {
		copy(srcUrl, destPath, revision, SVNRevision.HEAD, copyAsChild, makeParents);
	}		
	
	/* (non-Javadoc)
	 * @see org.tigris.subversion.svnclientadapter.ISVNClientAdapter#copy(org.tigris.subversion.svnclientadapter.SVNUrl, java.io.File, org.tigris.subversion.svnclientadapter.SVNRevision, org.tigris.subversion.svnclientadapter.SVNRevision, boolean, boolean)
	 */
	public void copy(SVNUrl srcUrl, File destPath, SVNRevision revision, SVNRevision pegRevision, boolean copyAsChild, boolean makeParents)
		throws SVNClientException {
		try {
			notificationHandler.setCommand(ISVNNotifyListener.Command.COPY);
			String dest = fileToSVNPath(destPath, false);		
			List<CopySource> copySources = new ArrayList<CopySource>();
			copySources.add(new CopySource(srcUrl.toString(), JhlConverter.convert(revision), JhlConverter.convert(pegRevision)));
			notificationHandler.logCommandLine("copy " + srcUrl + " " + dest);
			notificationHandler.setBaseDir(SVNBaseDir.getBaseDir(destPath));
			svnClient.copy(copySources, dest, copyAsChild, makeParents, true, null, null, null);
		} catch (ClientException e) {
			notificationHandler.logException(e);
			throw new SVNClientException(e);
		}
	}	
	
	/* (non-Javadoc)
	 * @see org.tigris.subversion.svnclientadapter.ISVNClientAdapter#copy(org.tigris.subversion.svnclientadapter.SVNUrl, org.tigris.subversion.svnclientadapter.SVNUrl, java.lang.String, org.tigris.subversion.svnclientadapter.SVNRevision)
	 */
	public void copy(
		SVNUrl srcUrl,
		SVNUrl destUrl,
		String message,
		SVNRevision revision)
		throws SVNClientException {
		copy (srcUrl, destUrl, message, revision, false);
	}
	
	/* (non-Javadoc)
	 * @see org.tigris.subversion.svnclientadapter.ISVNClientAdapter#copy(org.tigris.subversion.svnclientadapter.SVNUrl, org.tigris.subversion.svnclientadapter.SVNUrl, java.lang.String, org.tigris.subversion.svnclientadapter.SVNRevision)
	 */
	public void copy(
		SVNUrl srcUrl,
		SVNUrl destUrl,
		String message,
		SVNRevision revision,
		boolean makeParents)
		throws SVNClientException {
		copy(new SVNUrl[] { srcUrl }, destUrl, message, revision, true, makeParents);
	}
	
	/* (non-Javadoc)
	 * @see org.tigris.subversion.svnclientadapter.ISVNClientAdapter#copy(org.tigris.subversion.svnclientadapter.SVNUrl[], org.tigris.subversion.svnclientadapter.SVNUrl, java.lang.String, org.tigris.subversion.svnclientadapter.SVNRevision, boolean, boolean)
	 */
	public void copy(
		SVNUrl[] srcUrls,
		SVNUrl destUrl,
		String message,
		SVNRevision revision,
		boolean copyAsChild,
		boolean makeParents)
		throws SVNClientException {
		try {
        	String fixedMessage = fixSVNString(message);

        	if (fixedMessage == null)
        		fixedMessage = "";
			notificationHandler.setCommand(ISVNNotifyListener.Command.COPY);
			List<CopySource> copySources = new ArrayList<CopySource>(srcUrls.length);
			for (int i = 0; i < srcUrls.length; i++)  copySources.add(new CopySource(srcUrls[i].toString(), JhlConverter.convert(revision), JhlConverter.convert(SVNRevision.HEAD)));
			String dest = destUrl.toString();
			String commandLine = "copy -r" + revision.toString();
			Set<String> paths = new HashSet<String>(srcUrls.length);
			for (int i = 0; i < srcUrls.length; i++) {
				paths.add(srcUrls[i].toString());
			}
			commandLine = appendPaths(commandLine, paths) + " " + dest;
			notificationHandler.logCommandLine(commandLine);
			notificationHandler.setBaseDir();
			svnClient.copy(copySources, dest, copyAsChild, makeParents, true, null, new JhlCommitMessage(fixedMessage), null);
		} catch (ClientException e) {
			notificationHandler.logException(e);
			throw new SVNClientException(e);
		}
	}		

	/* (non-Javadoc)
	 * @see org.tigris.subversion.svnclientadapter.ISVNClientAdapter#remove(org.tigris.subversion.svnclientadapter.SVNUrl[], java.lang.String)
	 */
	public void remove(SVNUrl url[], String message) throws SVNClientException {
        try {
        	String fixedMessage = fixSVNString(message);

        	if (fixedMessage == null)
        		fixedMessage = "";
            notificationHandler.setCommand(ISVNNotifyListener.Command.REMOVE);

            String commandLine = "delete -m \""+getFirstMessageLine(fixedMessage)+"\"";
            
            Set<String> targets = new HashSet<String>(url.length);
            for (int i = 0; i < url.length;i++) {
                targets.add(url[i].toString()); 
            }
            commandLine = appendPaths(commandLine, targets);
            notificationHandler.logCommandLine(commandLine);
			notificationHandler.setBaseDir();
		    svnClient.remove(targets,false, false, null, new JhlCommitMessage(fixedMessage), null);
            
        } catch (ClientException e) {
            notificationHandler.logException(e);
            throw new SVNClientException(e);
        }           
	}

	/* (non-Javadoc)
	 * @see org.tigris.subversion.svnclientadapter.ISVNClientAdapter#remove(java.io.File[], boolean)
	 */
	public void remove(File file[], boolean force) throws SVNClientException {
        try {
            notificationHandler.setCommand(ISVNNotifyListener.Command.REMOVE);
            
            String commandLine = "delete"+(force?" --force":"");
            Set<String> targets = new HashSet<String>(file.length);
            
            for (int i = 0; i < file.length;i++) {
                targets.add(fileToSVNPath(file[i], false));
            }
            commandLine = appendPaths(commandLine, targets);
            
            notificationHandler.logCommandLine(commandLine);
			notificationHandler.setBaseDir(SVNBaseDir.getBaseDir(file));
   
            svnClient.remove(targets, force, false, null, null, null);
        } catch (ClientException e) {
            notificationHandler.logException(e);
            throw new SVNClientException(e);
        }           
	}

	/* (non-Javadoc)
	 * @see org.tigris.subversion.svnclientadapter.ISVNClientAdapter#doExport(org.tigris.subversion.svnclientadapter.SVNUrl, java.io.File, org.tigris.subversion.svnclientadapter.SVNRevision, boolean)
	 */
	public void doExport(
		SVNUrl srcUrl,
		File destPath,
		SVNRevision revision,
		boolean force)
		throws SVNClientException {
		try {
			notificationHandler.setCommand(ISVNNotifyListener.Command.EXPORT);
			String src = srcUrl.toString();
			String dest = fileToSVNPath(destPath, false);
			notificationHandler.logCommandLine(
				"export -r " + revision.toString() + ' ' + src + ' ' + dest);
			notificationHandler.setBaseDir(SVNBaseDir.getBaseDir(destPath));
			svnClient.doExport(src, dest, JhlConverter.convert(revision), Revision.HEAD, force, false, Depth.infinity, null);
		} catch (ClientException e) {
			notificationHandler.logException(e);
			throw new SVNClientException(e);
		}
	}
	
	/* (non-Javadoc)
	 * @see org.tigris.subversion.svnclientadapter.ISVNClientAdapter#doExport(java.io.File, java.io.File, boolean)
	 */
	public void doExport(File srcPath, File destPath, boolean force)
		throws SVNClientException {
		try {
			notificationHandler.setCommand(ISVNNotifyListener.Command.EXPORT);
			String src = fileToSVNPath(srcPath, false);
			String dest = fileToSVNPath(destPath, false);
			notificationHandler.logCommandLine("export " + src + ' ' + dest);
			notificationHandler.setBaseDir(SVNBaseDir.getBaseDir(new File[]{srcPath,destPath }));
			// in this case, revision is not used but must be valid
			svnClient.doExport(src, dest, Revision.WORKING, Revision.BASE, force, false, Depth.infinity, null);
		} catch (ClientException e) {
			notificationHandler.logException(e);
			throw new SVNClientException(e);
		}
	}
	
	/* (non-Javadoc)
	 * @see org.tigris.subversion.svnclientadapter.ISVNClientAdapter#doImport(java.io.File, org.tigris.subversion.svnclientadapter.SVNUrl, java.lang.String, boolean)
	 */
	public void doImport(
		File path,
		SVNUrl url,
		String message,
		boolean recurse)
		throws SVNClientException {
		try {
        	String fixedMessage = fixSVNString(message);

        	if (fixedMessage == null)
        		fixedMessage = "";
			notificationHandler.setCommand(ISVNNotifyListener.Command.IMPORT);
			String src = fileToSVNPath(path, false);
			String dest = url.toString();
			notificationHandler.logCommandLine(
				"import -m \""
					+ getFirstMessageLine(fixedMessage)
					+ "\" "
					+ (recurse ? "" : "-N ")
					+ src
					+ ' '
					+ dest);
			notificationHandler.setBaseDir(SVNBaseDir.getBaseDir(path));
			svnClient.doImport(src, dest, Depth.infinityOrEmpty(recurse), false, true, null, new JhlCommitMessage(fixedMessage), null);
			notificationHandler.logCompleted(Messages.bind("notify.import.complete"));
		} catch (ClientException e) {
			notificationHandler.logException(e);
			throw new SVNClientException(e);
		}
	}

	/* (non-Javadoc)
	 * @see org.tigris.subversion.svnclientadapter.ISVNClientAdapter#mkdir(org.tigris.subversion.svnclientadapter.SVNUrl, java.lang.String)
	 */
	public void mkdir(SVNUrl url, String message) throws SVNClientException {
		this.mkdir(url, false, message);
	}
	
	/* (non-Javadoc)
	 * @see org.tigris.subversion.svnclientadapter.ISVNClientAdapter#mkdir(java.io.File)
	 */
	public void mkdir(File file) throws SVNClientException {
        try {
            notificationHandler.setCommand(ISVNNotifyListener.Command.MKDIR);
            Set<String> target = new HashSet<String>();
            target.add(fileToSVNPath(file, false));
            notificationHandler.logCommandLine(
                "mkdir "+target);
			notificationHandler.setBaseDir(SVNBaseDir.getBaseDir(file));
            svnClient.mkdir(target, false, null, null, null);
        } catch (ClientException e) {
            notificationHandler.logException(e);
            throw new SVNClientException(e);
        }           	
	}

	/* (non-Javadoc)
	 * @see org.tigris.subversion.svnclientadapter.ISVNClientAdapter#move(java.io.File, java.io.File, boolean)
	 */
	public void move(File srcPath, File destPath, boolean force) throws SVNClientException {
        // use force when you want to move file even if there are local modifications
        try {
            notificationHandler.setCommand(ISVNNotifyListener.Command.MOVE);
		    Set<String> src = new HashSet<String>();
		    src.add(fileToSVNPath(srcPath, false));
            String dest = fileToSVNPath(destPath, false);
            notificationHandler.logCommandLine(
                    "move "+fileToSVNPath(srcPath, false)+' '+dest);
			notificationHandler.setBaseDir(SVNBaseDir.getBaseDir(new File[] {srcPath, destPath}));        
            svnClient.move(src,dest,force, false, false,null,null,null);
        } catch (ClientException e) {
            notificationHandler.logException(e);
            throw new SVNClientException(e);
        }                   	
	}

	/* (non-Javadoc)
	 * @see org.tigris.subversion.svnclientadapter.ISVNClientAdapter#move(org.tigris.subversion.svnclientadapter.SVNUrl, org.tigris.subversion.svnclientadapter.SVNUrl, java.lang.String, org.tigris.subversion.svnclientadapter.SVNRevision)
	 */
	public void move(
		SVNUrl srcUrl,
		SVNUrl destUrl,
		String message,
		SVNRevision revision)
		throws SVNClientException {
		try {
        	String fixedMessage = fixSVNString(message);

			// NOTE:  The revision arg is ignored as you cannot move
			// a specific revision, only HEAD.
        	if (fixedMessage == null)
        		fixedMessage = "";
			notificationHandler.setCommand(ISVNNotifyListener.Command.MOVE);
		    Set<String> src = new HashSet<String>();
		    src.add(srcUrl.toString());
			String dest = destUrl.toString();
			notificationHandler.logCommandLine(
				"move -m \""
					+ getFirstMessageLine(fixedMessage)
					+ ' '
					+ srcUrl.toString()
					+ ' '
					+ dest);
			notificationHandler.setBaseDir();
			svnClient.move(src, dest, false, false, false, null, new JhlCommitMessage(fixedMessage), null);
		} catch (ClientException e) {
			notificationHandler.logException(e);
			throw new SVNClientException(e);
		}
	}	

	/* (non-Javadoc)
	 * @see org.tigris.subversion.svnclientadapter.ISVNClientAdapter#update(java.io.File, org.tigris.subversion.svnclientadapter.SVNRevision, boolean)
	 */
	public long update(File path, SVNRevision revision, boolean recurse)
		throws SVNClientException {
			return update(path, revision, Depth.unknownOrFiles(recurse).ordinal(), false, false, true);
	}
	
	/* (non-Javadoc)
	 * @see org.tigris.subversion.svnclientadapter.ISVNClientAdapter#update(java.io.File, org.tigris.subversion.svnclientadapter.SVNRevision, int, boolean, boolean, boolean)
	 */
	public long update(File path, SVNRevision revision, int depth, boolean setDepth, boolean ignoreExternals, boolean force)
		throws SVNClientException {
		try {
			notificationHandler.setCommand(ISVNNotifyListener.Command.UPDATE);
			Set<String> target = new HashSet<String>();
			String t = fileToSVNPath(path, false);
			target.add(t);
			Depth d = JhlConverter.depth(depth);
			StringBuffer commandLine;
			if (d == Depth.exclude)
				commandLine = new StringBuffer("update " + t + " --set-depth=exclude");
			else {
				commandLine = new StringBuffer("update " + t + " -r " +
					revision.toString() + depthCommandLine(d));
				if (ignoreExternals) commandLine.append(" --ignore-externals");
	            if (force) commandLine.append(" --force");				
			}
            notificationHandler.logCommandLine(commandLine.toString());
			notificationHandler.setBaseDir(SVNBaseDir.getBaseDir(path));
			boolean makeParents = false;
			long rev[] = svnClient.update(target, JhlConverter.convert(revision), d, setDepth,
					makeParents, ignoreExternals, force);
			return rev[0];
		} catch (ClientException e) {
			notificationHandler.logException(e);
			throw new SVNClientException(e);
		}
	}		

    /* (non-Javadoc)
     * @see org.tigris.subversion.svnclientadapter.ISVNClientAdapter#update(java.io.File[], org.tigris.subversion.svnclientadapter.SVNRevision, boolean, boolean)
     */
    public long[] update(File[] path, SVNRevision revision, boolean recurse, boolean ignoreExternals) 
        throws SVNClientException
	{
    	return update(path, revision, Depth.unknownOrFiles(recurse).ordinal(), false, ignoreExternals, true);
	}
    
    /* (non-Javadoc)
     * @see org.tigris.subversion.svnclientadapter.ISVNClientAdapter#update(java.io.File[], org.tigris.subversion.svnclientadapter.SVNRevision, int, boolean, boolean)
     */
    public long[] update(File[] path, SVNRevision revision, int depth, boolean setDepth, boolean ignoreExternals, boolean force) 
        throws SVNClientException
	{
		try {
			notificationHandler.setCommand(ISVNNotifyListener.Command.UPDATE);
			Set<String> targets = new HashSet<String>(path.length);
			for (int i = 0; i < path.length; i++) {
				targets.add(fileToSVNPath(path[i], false));
			}
			Depth d = JhlConverter.depth(depth);
			StringBuffer commandLine = new StringBuffer(appendPaths("update ", targets) + " -r " +
					revision.toString() + depthCommandLine(d));
		    if (ignoreExternals) commandLine.append(" --ignore-externals");
		    if (force) commandLine.append(" --force");          					
            notificationHandler.logCommandLine(commandLine.toString());
			notificationHandler.setBaseDir(SVNBaseDir.getBaseDir(path));
			notificationHandler.holdStats();
			boolean makeParents = false;
			long[] rtnCode =  svnClient.update(targets, JhlConverter.convert(revision), d, setDepth, makeParents, ignoreExternals, force);
			notificationHandler.releaseStats();
			return rtnCode;
		} catch (ClientException e) {
			notificationHandler.logException(e);
			throw new SVNClientException(e);
		}    	
	}        
	
    /* (non-Javadoc)
     * @see org.tigris.subversion.svnclientadapter.ISVNClientAdapter#revert(java.io.File, boolean)
     */
    public void revert(File path, boolean recurse) throws SVNClientException {
        try {
            notificationHandler.setCommand(ISVNNotifyListener.Command.REVERT);
            String target = fileToSVNPath(path, false);
            notificationHandler.logCommandLine(
                "revert "+
                (recurse?"":"-N ")+
                target);
			notificationHandler.setBaseDir(SVNBaseDir.getBaseDir(path)); 
            svnClient.revert(target,Depth.infinityOrEmpty(recurse), null);
        } catch (ClientException e) {
            notificationHandler.logException(e);
            throw new SVNClientException(e);
        }         
    }
    
	/* (non-Javadoc)
	 * @see org.tigris.subversion.svnclientadapter.ISVNClientAdapter#getContent(org.tigris.subversion.svnclientadapter.SVNUrl, org.tigris.subversion.svnclientadapter.SVNRevision, org.tigris.subversion.svnclientadapter.SVNRevision)
	 */
	public InputStream getContent(SVNUrl url, SVNRevision revision, SVNRevision pegRevision)
		throws SVNClientException {
		try {
			notificationHandler.setCommand(
				ISVNNotifyListener.Command.CAT);
			String commandLine = "cat -r "
                + revision
                + " "
                + url;
			if (pegRevision != null) {
				commandLine = commandLine + "@"	+ pegRevision;
			}
            notificationHandler.logCommandLine(commandLine);
			notificationHandler.setBaseDir();                
			
			byte[] contents = svnClient.fileContent(url.toString(), JhlConverter.convert(revision), JhlConverter.convert(pegRevision));
			InputStream input = new ByteArrayInputStream(contents);
			return input;
		} catch (ClientException e) {
			notificationHandler.logException(e);
			throw new SVNClientException(e);
		}
	}    
	
	/* (non-Javadoc)
	 * @see org.tigris.subversion.svnclientadapter.ISVNClientAdapter#getContent(org.tigris.subversion.svnclientadapter.SVNUrl, org.tigris.subversion.svnclientadapter.SVNRevision)
	 */
	public InputStream getContent(SVNUrl url, SVNRevision revision)
	throws SVNClientException {
		return getContent(url, revision, SVNRevision.HEAD);
	}


	/*
	 * (non-Javadoc)
	 * @see org.tigris.subversion.svnclientadapter.ISVNClientAdapter#getContent(java.io.File, org.tigris.subversion.svnclientadapter.SVNRevision)
	 */
	public InputStream getContent(File path, SVNRevision revision)
		throws SVNClientException {
		try {
			String target = fileToSVNPath(path, false);
			notificationHandler.setCommand(
				ISVNNotifyListener.Command.CAT);
			notificationHandler.logCommandLine(
							"cat -r "
								+ revision.toString()
								+ " "
								+ target);
			notificationHandler.setBaseDir();                
			
			if (revision.equals(SVNRevision.BASE)) {
			    // This is to work-around a JavaHL problem when trying to
			    // retrieve the base revision of a newly added file.
			    ISVNStatus status = getSingleStatus(path);
			    if (status.getTextStatus().equals(SVNStatusKind.ADDED))
			        return new ByteArrayInputStream(new byte[0]);
			}
			byte[] contents = svnClient.fileContent(target, JhlConverter.convert(revision), Revision.BASE);
			InputStream input = new ByteArrayInputStream(contents);
			return input;
		} catch (ClientException e) {
			notificationHandler.logException(e);
			throw new SVNClientException(e);
		}
	}
	
	/* (non-Javadoc)
	 * @see org.tigris.subversion.svnclientadapter.ISVNClientAdapter#getProperties(java.io.File)
	 */
	public ISVNProperty[] getProperties(File path) throws SVNClientException {
		return getProperties(path, false);
	}


	/* (non-Javadoc)
	 * @see org.tigris.subversion.svnclientadapter.ISVNClientAdapter#getProperties(java.io.File)
	 */
	public ISVNProperty[] getProperties(File path, boolean descend) throws SVNClientException {
		try {
			notificationHandler.setCommand(ISVNNotifyListener.Command.PROPLIST);
			String target = fileToSVNPath(path, false);
			StringBuffer commandLine = new StringBuffer("propList ");
			if (descend) {
				commandLine.append(" -R ");
			}
			commandLine.append(target);
			notificationHandler.logCommandLine(commandLine.toString());
			notificationHandler.setBaseDir(SVNBaseDir.getBaseDir(path));
			JhlProplistCallback callback = new JhlProplistCallback(true);
			if (descend) {
				svnClient.properties(target, null, null, Depth.infinity, null, callback);
			} else {
				svnClient.properties(target, null, null, Depth.empty, null, callback);
			}
			return callback.getPropertyData();
		} catch (ClientException e) {
			notificationHandler.logException(e);
			throw new SVNClientException(e);
		}		
	}
	
	/* (non-Javadoc)
	 * @see org.tigris.subversion.svnclientadapter.ISVNClientAdapter#getProperties(org.tigris.subversion.svnclientadapter.SVNUrl)
	 */
	public ISVNProperty[] getProperties(SVNUrl url) throws SVNClientException {
		return getProperties(url, SVNRevision.HEAD, SVNRevision.HEAD);
	}
	
	/* (non-Javadoc)
	 * @see org.tigris.subversion.svnclientadapter.ISVNClientAdapter#getProperties(org.tigris.subversion.svnclientadapter.SVNUrl)
	 */
	public ISVNProperty[] getProperties(SVNUrl url, SVNRevision revision, SVNRevision pegRevision) throws SVNClientException {
		return getProperties(url, revision, pegRevision, true);
	}

	/* (non-Javadoc)
	 * @see org.tigris.subversion.svnclientadapter.ISVNClientAdapter#getProperties(org.tigris.subversion.svnclientadapter.SVNUrl)
	 */
	public ISVNProperty[] getProperties(SVNUrl url, SVNRevision revision, SVNRevision pegRevision, boolean recurse) throws SVNClientException {
		try {
			notificationHandler.setCommand(ISVNNotifyListener.Command.PROPLIST);
			String target = url.toString();
			notificationHandler.logCommandLine(
					"proplist "+ target);
			notificationHandler.setBaseDir();
			JhlProplistCallback callback = new JhlProplistCallback(false);
			Depth depth;
			if (recurse) {
				depth = Depth.infinity;
			}
			else {
				depth = Depth.empty;
			}
			svnClient.properties(target, JhlConverter.convert(revision), JhlConverter.convert(pegRevision), depth, null, callback);
			return callback.getPropertyData();
		} catch (ClientException e) {
			notificationHandler.logException(e);
			throw new SVNClientException(e);
		}		
	}
	
	public ISVNProperty[] getPropertiesIncludingInherited(File path, boolean includeEmptyProperties, boolean includeClosestOnly, List<String> filterProperties) throws SVNClientException {
		ISVNProperty[] properties = getPropertiesIncludingInherited(fileToSVNPath(path, false), true);
		return filterProperties(properties, includeEmptyProperties, includeClosestOnly, filterProperties);
	}
	
	public ISVNProperty[] getPropertiesIncludingInherited(SVNUrl path, boolean includeEmptyProperties, boolean includeClosestOnly, List<String> filterProperties) throws SVNClientException {
		ISVNProperty[] properties = getPropertiesIncludingInherited(path.toString(), false);
		return filterProperties(properties, includeEmptyProperties, includeClosestOnly, filterProperties);
	}
	
	private ISVNProperty[] filterProperties(ISVNProperty[] properties, boolean includeEmptyProperties, boolean includeClosestOnly, List<String> filterProperties) {
		if (includeEmptyProperties == true && filterProperties == null) {
			return properties;
		}
		List<ISVNProperty> allProperties = null;
		Map<String, ISVNProperty> propertyMap = null;
		if (includeClosestOnly) {
			propertyMap = new HashMap<String, ISVNProperty>();
		}
		else {
			allProperties = new ArrayList<ISVNProperty>();
		}
		for (ISVNProperty property : properties) {
			if (filterProperties == null || filterProperties.contains(property.getName())) {
				if (includeClosestOnly) {
					propertyMap.put(property.getName(), property);
				}
				else {
					allProperties.add(property);
				}
			}
		}
		ISVNProperty[] propertyArray;
		if (includeClosestOnly) {
			propertyArray = new ISVNProperty[propertyMap.size()];
			propertyMap.values().toArray(propertyArray);
		}
		else {
			propertyArray = new ISVNProperty[allProperties.size()];
			allProperties.toArray(propertyArray);
		}
		return propertyArray;
	}
	
	public ISVNProperty[] getPropertiesIncludingInherited(File path) throws SVNClientException {
		return getPropertiesIncludingInherited(fileToSVNPath(path, false), true);
	}
	
	public ISVNProperty[] getPropertiesIncludingInherited(SVNUrl path) throws SVNClientException {
		return getPropertiesIncludingInherited(path.toString(), false);
	}

	private ISVNProperty[] getPropertiesIncludingInherited(String path, boolean isFile) throws SVNClientException {
		try {
			notificationHandler.setCommand(ISVNNotifyListener.Command.PROPLIST);
			notificationHandler.logCommandLine(
					"proplist "+ path);
			notificationHandler.setBaseDir();
			InheritedJhlProplistCallback callback = new InheritedJhlProplistCallback(isFile);
			Revision revision = null;
			if (!isFile) {
				revision = JhlConverter.convert(SVNRevision.HEAD);
			}
			svnClient.properties(path, revision, revision, Depth.empty, null, callback);
			return callback.getPropertyData();
		} catch (ClientException e) {
			notificationHandler.logException(e);
			throw new SVNClientException(e);
		}				
	}
	
	/* (non-Javadoc)
	 * @see org.tigris.subversion.svnclientadapter.ISVNClientAdapter#propertySet(org.tigris.subversion.svnclientadapter.SVNUrl, org.tigris.subversion.svnclientadapter.SVNRevision.Number,java.lang.String, java.lang.String, java.lang.String)
	 */
	public void propertySet(
		SVNUrl url,
		SVNRevision.Number baseRev,
		String propertyName,
		String propertyValue,
		String message)
		throws SVNClientException {
		try {
			notificationHandler.setCommand(ISVNNotifyListener.Command.PROPSET);
			if (propertyName.startsWith("svn:")) {
				// Normalize line endings in property value
				svnClient.propertySetRemote(url.toString(), baseRev.getNumber(), propertyName, fixSVNString(propertyValue).getBytes(), new JhlCommitMessage(message), false, null, new JhlCommitCallback());
			} else {
				svnClient.propertySetRemote(url.toString(), baseRev.getNumber(), propertyName, propertyValue.getBytes(), new JhlCommitMessage(message), false, null, new JhlCommitCallback());
			}			
		} catch (ClientException e) {
			notificationHandler.logException(e);
			throw new SVNClientException(e);
		}
	}	

	/* (non-Javadoc)
	 * @see org.tigris.subversion.svnclientadapter.ISVNClientAdapter#propertySet(java.io.File, java.lang.String, java.lang.String, boolean)
	 */
	public void propertySet(
		File path,
		String propertyName,
		String propertyValue,
		boolean recurse)
		throws SVNClientException {
		try {
			notificationHandler.setCommand(ISVNNotifyListener.Command.PROPSET);

			String target = fileToSVNPath(path, false);
			notificationHandler.logCommandLine(
				"propset "
					+ (recurse?"-R ":"")
					+ propertyName
					+ " \""
					+ propertyValue
					+ "\" "
					+ target);
			notificationHandler.setBaseDir(SVNBaseDir.getBaseDir(path));

			Set<String> statusBefore = null;
			if (recurse) {
				statusBefore = new HashSet<String>();
				ISVNStatus[] statuses = getStatus(path,recurse,false);
				for (int i = 0; i < statuses.length;i++) {
					statusBefore.add(statuses[i].getFile().getAbsolutePath());
				}
			}
			
			Set<String> paths = new HashSet<String>(1);
			paths.add(target);
			
			if (propertyName.startsWith("svn:")) {
				// Normalize line endings in property value
				svnClient.propertySetLocal(paths, propertyName, fixSVNString(propertyValue).getBytes(), Depth.infinityOrEmpty(recurse), null, false);
			} else {
				svnClient.propertySetLocal(paths, propertyName, propertyValue.getBytes(), Depth.infinityOrEmpty(recurse), null, false);
			}
			
			// there is no notification (Notify.notify is not called) when we set a property
			// so we will do notification ourselves
            if (recurse) {
	   		   ISVNStatus[] statuses = getStatus(path,recurse,false);
			   for (int i = 0; i < statuses.length;i++) {
				   String statusPath = statuses[i].getFile().getAbsolutePath();
				   notificationHandler.notifyListenersOfChange(statusPath);
				   statusBefore.remove(statusPath);
			   }
			   for (String status : statusBefore) 
				   notificationHandler.notifyListenersOfChange(status);
            } else {
 			   notificationHandler.notifyListenersOfChange(path.getAbsolutePath());	
            }
			
		} catch (ClientException e) {
			notificationHandler.logException(e);
			throw new SVNClientException(e);
		}
	}
    
	/* (non-Javadoc)
	 * @see org.tigris.subversion.svnclientadapter.ISVNClientAdapter#propertySet(java.io.File, java.lang.String, java.io.File, boolean)
	 */
	public void propertySet(
		File path,
		String propertyName,
		File propertyFile,
		boolean recurse)
		throws SVNClientException {
		try {
			notificationHandler.setCommand(ISVNNotifyListener.Command.PROPSET);

			String target = fileToSVNPath(path, false);
			notificationHandler.logCommandLine(
				"propset "
					+ (recurse?"-R ":"")
					+ propertyName
					+ "-F \""
					+ propertyFile.toString()
					+ "\" "
					+ target);
			notificationHandler.setBaseDir(SVNBaseDir.getBaseDir(path));
			byte[] propertyBytes = new byte[(int) propertyFile.length()];

			FileInputStream is = null;
			try {
				is = new FileInputStream(propertyFile);
				is.read(propertyBytes);
			}
			catch (IOException ioe) {
				throw new SVNClientException(ioe);
			}
			finally {
				if (is != null) {
					try {
						is.close();
					} catch (IOException e) {
						// ignore
					}
				}
			}

			Set<String> statusBefore = null;
			if (recurse) {
				statusBefore = new HashSet<String>();
				ISVNStatus[] statuses = getStatus(path,recurse,false);
				for (int i = 0; i < statuses.length;i++) {
					statusBefore.add(statuses[i].getFile().getAbsolutePath());
				}
			}
			
			Set<String> paths = new HashSet<String>(1);
			paths.add(target);

			svnClient.propertySetLocal(paths, propertyName, propertyBytes, Depth.infinityOrEmpty(recurse), null, false);

			// there is no notification (Notify.notify is not called) when we set a property
			// so we will do notification ourselves
            if (recurse) {
	   		   ISVNStatus[] statuses = getStatus(path,recurse,false);
			   for (int i = 0; i < statuses.length;i++) {
				   String statusPath = statuses[i].getFile().getAbsolutePath();
				   notificationHandler.notifyListenersOfChange(statusPath);
				   statusBefore.remove(statusPath);
			   }
			   for (String status : statusBefore) 
				   notificationHandler.notifyListenersOfChange(status);
            } else {
 			   notificationHandler.notifyListenersOfChange(path.getAbsolutePath());	
            }
			
			
		} catch (ClientException e) {
			notificationHandler.logException(e);
			throw new SVNClientException(e);
		}
	}
    
	/* (non-Javadoc)
	 * @see org.tigris.subversion.svnclientadapter.ISVNClientAdapter#propertyGet(java.io.File, java.lang.String)
	 */
	public ISVNProperty propertyGet(File path, String propertyName)
		throws SVNClientException {
		try {
			notificationHandler.setCommand(ISVNNotifyListener.Command.PROPGET);

			String target = fileToSVNPath(path, false);
			notificationHandler.logCommandLine(
				"propget " + propertyName + " " + target);
			notificationHandler.setBaseDir(SVNBaseDir.getBaseDir(path));
			byte[] bytes = svnClient.propertyGet(target, propertyName, null, null);
            if (bytes == null)
                return null;
            else
			    return JhlPropertyData.newForFile(target, propertyName, bytes);
		} catch (ClientException e) {
			notificationHandler.logException(e);
			throw new SVNClientException(e);
		}

	}
	
	/* (non-Javadoc)
	 * @see org.tigris.subversion.svnclientadapter.ISVNClientAdapter#propertyGet(org.tigris.subversion.svnclientadapter.SVNUrl, org.tigris.subversion.svnclientadapter.SVNRevision, org.tigris.subversion.svnclientadapter.SVNRevision, java.lang.String)
	 */
	public ISVNProperty propertyGet(SVNUrl url, SVNRevision revision,
			SVNRevision peg, String propertyName) throws SVNClientException {
		try {
			notificationHandler.setCommand(ISVNNotifyListener.Command.PROPGET);
			String target = url.toString();
			String commandLine = "propget -r " + revision.toString() + " " +
			  propertyName + " " + target;
			if (!peg.equals(SVNRevision.HEAD))
				commandLine += "@" + peg.toString();
			notificationHandler.logCommandLine(commandLine);
			notificationHandler.setBaseDir();
			byte[] bytes = svnClient.propertyGet(target, propertyName, JhlConverter.convert(revision),
					JhlConverter.convert(peg));
            if (bytes == null)
                return null;
            else
			    return JhlPropertyData.newForUrl(target, propertyName, bytes);
		} catch (ClientException e) {
			notificationHandler.logException(e);
			throw new SVNClientException(e);
		}
	}

    /* (non-Javadoc)
     * @see org.tigris.subversion.svnclientadapter.ISVNClientAdapter#propertyDel(java.io.File, java.lang.String, boolean)
     */
    public void propertyDel(File path, String propertyName,boolean recurse) throws SVNClientException {
        try {
            notificationHandler.setCommand(ISVNNotifyListener.Command.PROPDEL);
            
            String target = fileToSVNPath(path, false);
            notificationHandler.logCommandLine("propdel "+propertyName+" "+target);
			notificationHandler.setBaseDir(SVNBaseDir.getBaseDir(path));
            
			Set<String> statusBefore = null;
			if (recurse) {
				statusBefore = new HashSet<String>();
				ISVNStatus[] statuses = getStatus(path,recurse,false);
				for (int i = 0; i < statuses.length;i++) {
					statusBefore.add(statuses[i].getFile().getAbsolutePath());
				}
			}
			
			Set<String> paths = new HashSet<String>(1);
			paths.add(target);
			
            svnClient.propertySetLocal(paths, propertyName, null, Depth.infinityOrEmpty(recurse), null, true);
            
            // there is no notification (Notify.notify is not called) when we set a property
            // so we will do notification ourselves
            if (recurse) {
	   		   ISVNStatus[] statuses = getStatus(path,recurse,false);
			   for (int i = 0; i < statuses.length;i++) {
				   String statusPath = statuses[i].getFile().getAbsolutePath();
				   notificationHandler.notifyListenersOfChange(statusPath);
				   statusBefore.remove(statusPath);
			   }
			   for (String status : statusBefore) 
				   notificationHandler.notifyListenersOfChange(status);
            } else {
 			   notificationHandler.notifyListenersOfChange(path.getAbsolutePath());	
            }

        } catch (ClientException e) {
            notificationHandler.logException(e);
            throw new SVNClientException(e);            
        }        
    }
    
    /* (non-Javadoc)
     * @see org.tigris.subversion.svnclientadapter.ISVNClientAdapter#diff(java.io.File, org.tigris.subversion.svnclientadapter.SVNRevision, java.io.File, org.tigris.subversion.svnclientadapter.SVNRevision, java.io.File, boolean)
     */
    public void diff(File oldPath, SVNRevision oldPathRevision,
                     File newPath, SVNRevision newPathRevision,
                     File outFile, boolean recurse) throws SVNClientException {
    	diff(oldPath, oldPathRevision, newPath, newPathRevision, outFile, recurse, true, false, false);
    }

    private void diffRelative(File oldPath, SVNRevision oldPathRevision,
                     File newPath, SVNRevision newPathRevision,
                     File outFile, boolean recurse,	boolean ignoreAncestry, 
             		 boolean noDiffDeleted, boolean force, File relativeTo) throws SVNClientException {
        try {
            notificationHandler.setCommand(ISVNNotifyListener.Command.DIFF);
                
            if (oldPath == null)
                oldPath = new File(".");
            if (newPath == null)
                newPath = oldPath;
            if (oldPathRevision == null)
                oldPathRevision = SVNRevision.BASE;
            if (newPathRevision == null)
                newPathRevision = SVNRevision.WORKING;
            
            // we don't want canonical file path (otherwise the complete file name
            // would be in the patch). This way the user can choose to use a relative
            // path
            String oldTarget = fileToSVNPath(oldPath, false);
            String newTarget = fileToSVNPath(newPath, false);
            String svnOutFile = fileToSVNPath(outFile, false);
            String relativeToDir = fileToSVNPath(relativeTo, false);
            
            String commandLine = "diff ";
            if ( (oldPathRevision.getKind() != Revision.Kind.base.ordinal()) ||
                 (newPathRevision.getKind() != Revision.Kind.working.ordinal()) )
            {
                commandLine += "-r "+oldPathRevision.toString();
                if (newPathRevision.getKind() != Revision.Kind.working.ordinal())
                    commandLine+= ":"+newPathRevision.toString();
                commandLine += " ";         
            }
            if (!oldPath.equals(new File(".")))
                commandLine += "--old "+oldTarget+" ";
            if (!newPath.equals(oldPath))
                commandLine += "--new "+newTarget+" ";
            
            int depth = Depth.empty.ordinal();
            if (recurse)
            	depth = Depth.infinity.ordinal();
            else {
	            if (oldPath.isFile())
	            	depth = Depth.files.ordinal();
            }
            
            notificationHandler.logCommandLine(commandLine);
			notificationHandler.setBaseDir(SVNBaseDir.getBaseDir(new File[]{oldPath,newPath}));
            svnClient.diff(oldTarget,JhlConverter.convert(oldPathRevision),newTarget,JhlConverter.convert(newPathRevision), relativeToDir, svnOutFile, JhlConverter.depth(depth), null, ignoreAncestry, noDiffDeleted, force, true);
        } catch (ClientException e) {
            notificationHandler.logException(e);
            throw new SVNClientException(e);            
        }
    }
    /* (non-Javadoc)
     * @see org.tigris.subversion.svnclientadapter.ISVNClientAdapter#diff(java.io.File, org.tigris.subversion.svnclientadapter.SVNRevision, java.io.File, org.tigris.subversion.svnclientadapter.SVNRevision, java.io.File, boolean, boolean, boolean, boolean)
     */
    public void diff(File oldPath, SVNRevision oldPathRevision,
                     File newPath, SVNRevision newPathRevision,
                     File outFile, boolean recurse,	boolean ignoreAncestry, 
             		 boolean noDiffDeleted, boolean force) throws SVNClientException {
        try {
            notificationHandler.setCommand(ISVNNotifyListener.Command.DIFF);
                
            if (oldPath == null)
                oldPath = new File(".");
            if (newPath == null)
                newPath = oldPath;
            if (oldPathRevision == null)
                oldPathRevision = SVNRevision.BASE;
            if (newPathRevision == null)
                newPathRevision = SVNRevision.WORKING;
            
            // we don't want canonical file path (otherwise the complete file name
            // would be in the patch). This way the user can choose to use a relative
            // path
            String oldTarget = fileToSVNPath(oldPath, false);
            String newTarget = fileToSVNPath(newPath, false);
            String svnOutFile = fileToSVNPath(outFile, false);
            
            String commandLine = "diff ";
            if ( (oldPathRevision.getKind() != Revision.Kind.base.ordinal()) ||
                 (newPathRevision.getKind() != Revision.Kind.working.ordinal()) )
            {
                commandLine += "-r "+oldPathRevision.toString();
                if (newPathRevision.getKind() != Revision.Kind.working.ordinal())
                    commandLine+= ":"+newPathRevision.toString();
                commandLine += " ";         
            }
            if (!oldPath.equals(new File(".")))
                commandLine += "--old "+oldTarget+" ";
            if (!newPath.equals(oldPath))
                commandLine += "--new "+newTarget+" ";
            
            notificationHandler.logCommandLine(commandLine);
			notificationHandler.setBaseDir(SVNBaseDir.getBaseDir(new File[]{oldPath,newPath}));
            svnClient.diff(oldTarget,JhlConverter.convert(oldPathRevision),newTarget,JhlConverter.convert(newPathRevision), null, svnOutFile, Depth.infinityOrEmpty(recurse), null, ignoreAncestry, noDiffDeleted, force, true);
        } catch (ClientException e) {
            notificationHandler.logException(e);
            throw new SVNClientException(e);            
        }
    }
    
    
    /* (non-Javadoc)
     * @see org.tigris.subversion.svnclientadapter.ISVNClientAdapter#diff(java.io.File, java.io.File, boolean)
     */
    public void diff(File path, File outFile, boolean recurse) throws SVNClientException {
        diff(path, null,null,null,outFile,recurse);
    }

    /* (non-Javadoc)
     * @see org.tigris.subversion.svnclientadapter.ISVNClientAdapter#diff(org.tigris.subversion.svnclientadapter.SVNUrl, org.tigris.subversion.svnclientadapter.SVNRevision, org.tigris.subversion.svnclientadapter.SVNUrl, org.tigris.subversion.svnclientadapter.SVNRevision, java.io.File, boolean)
     */
    public void diff(SVNUrl oldUrl, SVNRevision oldUrlRevision,
                     SVNUrl newUrl, SVNRevision newUrlRevision,
                     File outFile, boolean recurse) throws SVNClientException {
    	diff(oldUrl, oldUrlRevision, newUrl, newUrlRevision, outFile, recurse, true, false, false);
    }

    /* (non-Javadoc)
     * @see org.tigris.subversion.svnclientadapter.ISVNClientAdapter#diff(org.tigris.subversion.svnclientadapter.SVNUrl, org.tigris.subversion.svnclientadapter.SVNRevision, org.tigris.subversion.svnclientadapter.SVNRevision, org.tigris.subversion.svnclientadapter.SVNRevision, java.io.File, int, boolean, boolean, boolean)
     */    
    public void diff(SVNUrl target, SVNRevision pegRevision, SVNRevision startRevision, SVNRevision endRevision,
			File outFile, int depth, boolean ignoreAncestry, 
			boolean noDiffDeleted, boolean force) throws SVNClientException {
        try {
            notificationHandler.setCommand(ISVNNotifyListener.Command.DIFF);
                
            if (pegRevision == null)
                pegRevision = SVNRevision.HEAD;
            if (startRevision == null)
                startRevision = SVNRevision.HEAD;
            if (endRevision == null)
                endRevision = SVNRevision.HEAD;
            
            String commandLine = "diff ";
            Depth d = JhlConverter.depth(depth);
            commandLine += depthCommandLine(d);
            if (ignoreAncestry)
            	commandLine += " --ignoreAncestry";
           commandLine += " -r " + startRevision + ":" + endRevision + " " + target;
            notificationHandler.logCommandLine(commandLine);
			notificationHandler.setBaseDir();
			svnClient.diff(target.toString(), JhlConverter.convert(pegRevision), JhlConverter.convert(startRevision), JhlConverter.convert(endRevision), 
					null, outFile.getAbsolutePath(), d, null, ignoreAncestry, noDiffDeleted, force, false);
        } catch (ClientException e) {
            notificationHandler.logException(e);
            throw new SVNClientException(e);            
        }    	
    }
    
    /* (non-Javadoc)
     * @see org.tigris.subversion.svnclientadapter.ISVNClientAdapter#diff(org.tigris.subversion.svnclientadapter.SVNUrl, org.tigris.subversion.svnclientadapter.SVNRevision, org.tigris.subversion.svnclientadapter.SVNRevision, org.tigris.subversion.svnclientadapter.SVNRevision, java.io.File, boolean)
     */    
    public void diff(SVNUrl target, SVNRevision pegRevision, SVNRevision startRevision, SVNRevision endRevision,
			File outFile, boolean recurse) throws SVNClientException {   	
        diff(target, pegRevision, startRevision, endRevision, outFile, Depth.infinityOrImmediates(recurse).ordinal(), true, false, false);
    }    

    /* (non-Javadoc)
     * @see org.tigris.subversion.svnclientadapter.ISVNClientAdapter#diff(org.tigris.subversion.svnclientadapter.SVNUrl, org.tigris.subversion.svnclientadapter.SVNRevision, org.tigris.subversion.svnclientadapter.SVNUrl, org.tigris.subversion.svnclientadapter.SVNRevision, java.io.File, boolean, boolean, boolean, boolean)
     */
    public void diff(SVNUrl oldUrl, SVNRevision oldUrlRevision,
                     SVNUrl newUrl, SVNRevision newUrlRevision,
                     File outFile, boolean recurse,	boolean ignoreAncestry, 
             		 boolean noDiffDeleted, boolean force) throws SVNClientException {
        try {
            notificationHandler.setCommand(ISVNNotifyListener.Command.DIFF);
                
            if (newUrl == null)
                newUrl = oldUrl;
            if (oldUrlRevision == null)
                oldUrlRevision = SVNRevision.HEAD;
            if (newUrlRevision == null)
                newUrlRevision = SVNRevision.HEAD;
            
            String svnOutFile = fileToSVNPath(outFile, false);
            
            String commandLine = "diff ";
            if ( (oldUrlRevision.getKind() != Revision.Kind.head.ordinal()) ||
                 (newUrlRevision.getKind() != Revision.Kind.head.ordinal()) )
            {
                commandLine += "-r "+oldUrlRevision.toString();
                if (newUrlRevision.getKind() != Revision.Kind.head.ordinal())
                    commandLine+= ":"+newUrlRevision.toString();
                commandLine += " ";         
            }
            commandLine += oldUrl+" ";
            if (!newUrl.equals(oldUrl))
                commandLine += newUrl+" ";
            
            notificationHandler.logCommandLine(commandLine);
			notificationHandler.setBaseDir();
            svnClient.diff(oldUrl.toString(),JhlConverter.convert(oldUrlRevision),newUrl.toString(),JhlConverter.convert(newUrlRevision), null, svnOutFile, Depth.infinityOrEmpty(recurse), null, ignoreAncestry, noDiffDeleted, force, false);
        } catch (ClientException e) {
            notificationHandler.logException(e);
            throw new SVNClientException(e);            
        }
    }

    /* (non-Javadoc)
     * @see org.tigris.subversion.svnclientadapter.ISVNClientAdapter#diff(org.tigris.subversion.svnclientadapter.SVNUrl, org.tigris.subversion.svnclientadapter.SVNRevision, org.tigris.subversion.svnclientadapter.SVNRevision, java.io.File, boolean)
     */
    public void diff(SVNUrl url, SVNRevision oldUrlRevision, SVNRevision newUrlRevision,
                     File outFile, boolean recurse) throws SVNClientException {
        diff(url,oldUrlRevision,url,newUrlRevision,outFile,recurse);                     
    }

    public ISVNAnnotations annotate(File file, SVNRevision revisionStart,
			SVNRevision revisionEnd, boolean ignoreMimeType,
			boolean includeMergedRevisions) throws SVNClientException {
		return annotate(file, revisionStart, revisionEnd, null, ignoreMimeType, includeMergedRevisions);
	}
    
    public ISVNAnnotations annotate(File file, SVNRevision revisionStart,
			SVNRevision revisionEnd, SVNRevision pegRevision, boolean ignoreMimeType,
			boolean includeMergedRevisions) throws SVNClientException {
		String target = fileToSVNPath(file, false);
		//If the file is an uncommitted rename/move, we have to refer to original/source, not the new copy.
		ISVNInfo info = getInfoFromWorkingCopy(file);
		if ((SVNScheduleKind.ADD == info.getSchedule()) && (info.getCopyUrl() != null)) {
			target = info.getCopyUrl().toString();			
		}
    	return annotate(target, revisionStart, revisionEnd, pegRevision, ignoreMimeType, includeMergedRevisions);
	}

	public ISVNAnnotations annotate(SVNUrl url, SVNRevision revisionStart,
			SVNRevision revisionEnd, SVNRevision pegRevision, 
			boolean ignoreMimeType, boolean includeMergedRevisions) throws SVNClientException {
    	return annotate(url.toString(), revisionStart, revisionEnd, pegRevision, ignoreMimeType, includeMergedRevisions);
	}

	private ISVNAnnotations annotate(String target, SVNRevision revisionStart, SVNRevision revisionEnd, SVNRevision pegRevision,
			boolean ignoreMimeType, boolean includeMergedRevisions)
    	throws SVNClientException
	{
        try {
            notificationHandler.setCommand(ISVNNotifyListener.Command.ANNOTATE);
            if(revisionStart == null)
                revisionStart = new SVNRevision.Number(1);
            if(revisionEnd == null)
                revisionEnd = SVNRevision.HEAD;
            if (pegRevision == null) 
            	pegRevision = SVNRevision.HEAD;
            String commandLine = "blame ";
            if (includeMergedRevisions)
            	commandLine += "-g ";
            commandLine = commandLine + "-r " + revisionStart.toString() + ":" + revisionEnd.toString() + " ";
            commandLine = commandLine + target + "@" + pegRevision;
            notificationHandler.logCommandLine(commandLine);
			notificationHandler.setBaseDir();
			
			JhlAnnotations annotations = new JhlAnnotations();
            svnClient.blame(target, JhlConverter.convert(pegRevision), JhlConverter.convert(revisionStart), JhlConverter.convert(revisionEnd), ignoreMimeType, includeMergedRevisions,  annotations);
            return annotations;
        } catch (ClientException e) { 
        	if (includeMergedRevisions && ((ClientException)e).getAprError() == SVNClientException.UNSUPPORTED_FEATURE) {
        		return annotate(target, revisionStart, revisionEnd, pegRevision, ignoreMimeType, false);
        	}
			if (e.getAprError() == ErrorCodes.fsNotFound && pegRevision != null && !pegRevision.equals(revisionEnd)) {
				return annotate(target, revisionStart, pegRevision, pegRevision, ignoreMimeType, includeMergedRevisions);
			} else {
				notificationHandler.logException(e);
				throw new SVNClientException(e);
			}
        }

	}
    
    /* (non-Javadoc)
     * @see org.tigris.subversion.svnclientadapter.ISVNClientAdapter#annotate(org.tigris.subversion.svnclientadapter.SVNUrl, org.tigris.subversion.svnclientadapter.SVNRevision, org.tigris.subversion.svnclientadapter.SVNRevision)
     */
    public ISVNAnnotations annotate(SVNUrl url, SVNRevision revisionStart, SVNRevision revisionEnd)
        throws SVNClientException
    {
    	return annotate(url.toString(), revisionStart, revisionEnd, null, false, false);
    }

    /* (non-Javadoc)
     * @see org.tigris.subversion.svnclientadapter.ISVNClientAdapter#annotate(java.io.File, org.tigris.subversion.svnclientadapter.SVNRevision, org.tigris.subversion.svnclientadapter.SVNRevision)
     */
    public ISVNAnnotations annotate(File file, SVNRevision revisionStart, SVNRevision revisionEnd)
        throws SVNClientException
    {
		String target = fileToSVNPath(file, false);
		//If the file is an uncommitted rename/move, we have to refer to original/source, not the new copy.
		ISVNInfo info = getInfoFromWorkingCopy(file);
		if ((SVNScheduleKind.ADD == info.getSchedule()) && (info.getCopyUrl() != null)) {
			target = info.getCopyUrl().toString();			
		}
    	return annotate(target, revisionStart, revisionEnd, null, false, false);
    }        
    
    /* (non-Javadoc)
     * @see org.tigris.subversion.svnclientadapter.ISVNClientAdapter#resolved(java.io.File)
     */
    public void resolved(File path) 
    	throws SVNClientException
    {
    	this.resolve(path, ISVNConflictResolver.Choice.chooseMerged);
    }

	public void resolve(File path, int result) throws SVNClientException {
		try {
			notificationHandler.setCommand(ISVNNotifyListener.Command.RESOLVE);
            
			String target = fileToSVNPath(path, true);
			String commandLine = "resolve ";
			ConflictResult.Choice choice = ConflictResult.Choice.chooseMerged;
			switch (result) {
			case ISVNConflictResolver.Choice.chooseMerged:
				commandLine += "--accept=working ";
				choice = ConflictResult.Choice.chooseMerged;
				break;
			case ISVNConflictResolver.Choice.chooseBase:
				commandLine += "--accept=base ";
				choice = ConflictResult.Choice.chooseBase;
				break;
			case ISVNConflictResolver.Choice.chooseTheirsFull:
				commandLine += "--accept=theirs-full ";
				choice = ConflictResult.Choice.chooseTheirsFull;
				break;
			case ISVNConflictResolver.Choice.chooseTheirs:
				commandLine += "--accept=theirs-conflict ";
				choice = ConflictResult.Choice.chooseTheirsConflict;
				break;				
			case ISVNConflictResolver.Choice.chooseMineFull:
				commandLine += "--accept=mine-full ";
				choice = ConflictResult.Choice.chooseMineFull;
				break;
			case ISVNConflictResolver.Choice.chooseMine:
				commandLine += "--accept=mine-conflict ";
				choice = ConflictResult.Choice.chooseMineConflict;
				break;				
			default:
				choice = ConflictResult.Choice.chooseMerged;
				break;
			}
			commandLine += target;
			notificationHandler.logCommandLine(commandLine);
			notificationHandler.setBaseDir(SVNBaseDir.getBaseDir(path));
			svnClient.resolve(target, Depth.empty, choice);
		} catch (SubversionException e) {
			notificationHandler.logException(e);
			throw new SVNClientException(e);            
		}        
		
	}

	/*
	 * (non-Javadoc)
	 * @see org.tigris.subversion.svnclientadapter.ISVNClientAdapter#cancelOperation()
	 */
	public void cancelOperation() throws SVNClientException {
		try {
			svnClient.cancelOperation();
		} catch (ClientException e) {
			notificationHandler.logException(e);
			throw new SVNClientException(e);			
		}
	}

	/* (non-Javadoc)
	 * @see org.tigris.subversion.svnclientadapter.ISVNClientAdapter#getInfoFromWorkingCopy(java.io.File)
	 */
	public ISVNInfo getInfoFromWorkingCopy(File path) throws SVNClientException {
		try {
			notificationHandler.setCommand(ISVNNotifyListener.Command.INFO);
            
			String target = fileToSVNPath(path, false);
			notificationHandler.logCommandLine("info "+target);
			notificationHandler.setBaseDir(SVNBaseDir.getBaseDir(path));
			JhlInfoCallback callback = new JhlInfoCallback();
			
			svnClient.info2(target, null, null, Depth.empty, null, callback);
			ISVNInfo[] items = callback.getInfo();
            if (items == null) {
            	return new SVNInfoUnversioned(path);
            } 
            return items[0];
		} catch (ClientException e) {
			if (e.getAprError() == ErrorCodes.wcNotDirectory || e.getAprError() == ErrorCodes.wcPathNotFound) {
				return new SVNInfoUnversioned(path);
			}
			else {
				notificationHandler.logException(e);
				throw new SVNClientException(e);   
			}
		}        
	}

	/* (non-Javadoc)
	 * @see org.tigris.subversion.svnclientadapter.ISVNClientAdapter#getInfo(java.io.File)
	 */
	public ISVNInfo getInfo(File path) throws SVNClientException {
		try {
			notificationHandler.setCommand(ISVNNotifyListener.Command.INFO);
            
			String target = fileToSVNPath(path, false);
			notificationHandler.logCommandLine("info "+target);
			notificationHandler.setBaseDir(SVNBaseDir.getBaseDir(path));
			
			JhlInfoCallback callback = new JhlInfoCallback();
			
			svnClient.info2(target, Revision.WORKING, Revision.BASE, Depth.infinity, null, callback);
            ISVNInfo[] items = callback.getInfo();
            if (items == null || items.length == 0) {
            	return new SVNInfoUnversioned(path);
            } else {
                return items[0];
            }
            
		} catch (ClientException e) {
			if (e.getAprError() == ErrorCodes.wcNotDirectory || e.getAprError() == ErrorCodes.wcPathNotFound) {
				return new SVNInfoUnversioned(path);
			}
			else {
				notificationHandler.logException(e);
				throw new SVNClientException(e);  
			}
		}        
	}

	/* (non-Javadoc)
	 * @see org.tigris.subversion.svnclientadapter.ISVNClientAdapter#getInfo(java.io.File, boolean)
	 */
	public ISVNInfo[] getInfo(File path, boolean descend) throws SVNClientException {
		try {
			notificationHandler.setCommand(ISVNNotifyListener.Command.INFO);    
			String target = fileToSVNPath(path, false);
			if (descend) notificationHandler.logCommandLine("info " + target + " --depth=infinity");
			else notificationHandler.logCommandLine("info " + target);
			notificationHandler.setBaseDir(SVNBaseDir.getBaseDir(path));
			JhlInfoCallback callback = new JhlInfoCallback();
			
			svnClient.info2(target, null, null, Depth.infinityOrEmpty(descend), null, callback);
            ISVNInfo[] items = callback.getInfo();
			if (items == null) {
				return new ISVNInfo[]{new SVNInfoUnversioned(path)};
			} else {
				return items;
			}
		} catch (ClientException e) {
			if (e.getAprError() == ErrorCodes.wcNotDirectory || e.getAprError() == ErrorCodes.wcPathNotFound) {
				return new ISVNInfo[]{new SVNInfoUnversioned(path)};
			}
			else {
				notificationHandler.logException(e);
				throw new SVNClientException(e);  
			}
		}      
	}
	
	/* (non-Javadoc)
	 * @see org.tigris.subversion.svnclientadapter.ISVNClientAdapter#getInfo(org.tigris.subversion.svnclientadapter.SVNUrl)
	 */
	public ISVNInfo getInfo(SVNUrl url, SVNRevision revision, SVNRevision peg) throws SVNClientException {
		try {
			notificationHandler.setCommand(ISVNNotifyListener.Command.INFO);
			String target = url.toString();
			notificationHandler.logCommandLine("info "+target);
//			notificationHandler.setBaseDir(SVNBaseDir.getBaseDir(url));
			JhlInfoCallback callback = new JhlInfoCallback();
			
			svnClient.info2(target, JhlConverter.convert(revision), JhlConverter.convert(peg), Depth.empty, null, callback);
            ISVNInfo[] items = callback.getInfo();
            if (items == null || items.length == 0) {
            	return new SVNInfoUnversioned(null);
            } else {
                return items[0];
            }
            
		} catch (ClientException e) {
			if (e.getAprError() == ErrorCodes.wcNotDirectory || e.getAprError() == ErrorCodes.wcPathNotFound) {
				return new SVNInfoUnversioned(null);
			}
			else {
				notificationHandler.logException(e);
				throw new SVNClientException(e);
			}
		}        
	}

    /*
     * (non-Javadoc)
     * @see org.tigris.subversion.svnclientadapter.ISVNClientAdapter#switchUrl(org.tigris.subversion.svnclientadapter.SVNUrl, java.io.File, org.tigris.subversion.svnclientadapter.SVNRevision, boolean)
     */
    public void switchToUrl(File path, SVNUrl url, SVNRevision revision, boolean recurse) throws SVNClientException {
    	switchToUrl(path, url, revision, Depth.unknownOrFiles(recurse).ordinal(), false, false, true);
    }
    
    /*
     * (non-Javadoc)
     * @see org.tigris.subversion.svnclientadapter.ISVNClientAdapter#switchUrl(org.tigris.subversion.svnclientadapter.SVNUrl, java.io.File, org.tigris.subversion.svnclientadapter.SVNRevision, int, boolean, boolean, boolean)
     */
    public void switchToUrl(File path, SVNUrl url, SVNRevision revision, int depth, boolean setDepth, boolean ignoreExternals, boolean force) throws SVNClientException {
        switchToUrl(path, url, revision, revision, depth, setDepth, ignoreExternals, force);
    }
    
    /*
     * (non-Javadoc)
     * @see org.tigris.subversion.svnclientadapter.ISVNClientAdapter#switchUrl(org.tigris.subversion.svnclientadapter.SVNUrl, java.io.File, org.tigris.subversion.svnclientadapter.SVNRevision, org.tigris.subversion.svnclientadapter.SVNRevision, int, boolean, boolean, boolean)
     */
    public void switchToUrl(File path, SVNUrl url, SVNRevision revision, SVNRevision pegRevision, int depth, boolean setDepth, boolean ignoreExternals, boolean force) throws SVNClientException {
    	switchToUrl(path, url, revision, pegRevision, depth, setDepth, ignoreExternals, force, false);
    }
    
    /*
     * (non-Javadoc)
     * @see org.tigris.subversion.svnclientadapter.ISVNClientAdapter#switchUrl(org.tigris.subversion.svnclientadapter.SVNUrl, java.io.File, org.tigris.subversion.svnclientadapter.SVNRevision, org.tigris.subversion.svnclientadapter.SVNRevision, int, boolean, boolean, boolean, boolean)
     */
    public void switchToUrl(File path, SVNUrl url, SVNRevision revision, SVNRevision pegRevision, int depth, boolean setDepth, boolean ignoreExternals, boolean force, boolean ignoreAncestry) throws SVNClientException {
        if (depth == Depth.exclude.ordinal()) {
        	update(path, pegRevision, depth, true, ignoreExternals, force);
        	return;
        }
    	try {
            notificationHandler.setCommand(ISVNNotifyListener.Command.SWITCH);
            
            Depth d = JhlConverter.depth(depth);
            String target = fileToSVNPath(path, false);
            StringBuffer commandLine = new StringBuffer("switch " + url + " " + target + " -r " + revision.toString() +
            		depthCommandLine(d));
            if (ignoreExternals) commandLine.append(" --ignore-externals");
            if (force) commandLine.append(" --force");            
            notificationHandler.logCommandLine(commandLine.toString());
            File baseDir = SVNBaseDir.getBaseDir(path);
            notificationHandler.setBaseDir(baseDir);
            Revision rev = JhlConverter.convert(revision);
            Revision pegRev = JhlConverter.convert(pegRevision);
            svnClient.doSwitch(target, url.toString(),rev,pegRev,d, setDepth, ignoreExternals, force, ignoreAncestry);
           
        } catch (ClientException e) {
            notificationHandler.logException(e);
            throw new SVNClientException(e);            
        }        
    	
    }            

	/* (non-Javadoc)
	 * @see org.tigris.subversion.svnclientadapter.ISVNClientAdapter#setConfigDirectory(java.io.File)
	 */
	public void setConfigDirectory(File dir) throws SVNClientException {
        try {
        	svnClient.setConfigDirectory(fileToSVNPath(dir,false));
        } catch (ClientException e) {
            notificationHandler.logException(e);
            throw new SVNClientException(e);            
        }
	}

    /* (non-Javadoc)
     * @see org.tigris.subversion.svnclientadapter.ISVNClientAdapter#cleanup(java.io.File)
     */
    public void cleanup(File path) throws SVNClientException {
        try {
            notificationHandler.setCommand(ISVNNotifyListener.Command.CLEANUP);
            String target = fileToSVNPath(path, false);
            String commandLine = "cleanup " + target;
            notificationHandler.logCommandLine(commandLine);
            svnClient.cleanup(target);
        } catch (ClientException e) {
            notificationHandler.logException(e);
            throw new SVNClientException(e);
        }
    }
    
    /* (non-Javadoc)
     * @see org.tigris.subversion.svnclientadapter.ISVNClientAdapter#upgrade(java.io.File)
     */
    public void upgrade(File path) throws SVNClientException {
        try {
            notificationHandler.setCommand(ISVNNotifyListener.Command.UPGRADE);
            String target = fileToSVNPath(path, false);
            String commandLine = "upgrade " + target;
            notificationHandler.logCommandLine(commandLine);
            svnClient.upgrade(target);
        } catch (ClientException e) {
            notificationHandler.logException(e);
            throw new SVNClientException(e);
        }
    }    
    
    /* (non-Javadoc)
     * @see org.tigris.subversion.svnclientadapter.ISVNClientAdapter#merge(org.tigris.subversion.svnclientadapter.SVNUrl, org.tigris.subversion.svnclientadapter.SVNRevision, org.tigris.subversion.svnclientadapter.SVNUrl, org.tigris.subversion.svnclientadapter.SVNRevision, java.io.File, boolean, boolean, boolean, boolean)
     */
    public void merge(SVNUrl path1, SVNRevision revision1, SVNUrl path2,
            SVNRevision revision2, File localPath, boolean force,
            boolean recurse, boolean dryRun, boolean ignoreAncestry) throws SVNClientException {
    	merge(path1, revision1, path2, revision2, localPath, force, Depth.infinityOrFiles(recurse).ordinal(), dryRun, ignoreAncestry, false);
    }
    
    public void merge(SVNUrl path1, SVNRevision revision1, SVNUrl path2,
            SVNRevision revision2, SVNRevision pegRevision, File localPath, boolean force,
            int depth, boolean dryRun, boolean ignoreAncestry, boolean recordOnly) throws SVNClientException {
    	try {
            notificationHandler.setCommand(ISVNNotifyListener.Command.MERGE);
            
            Depth d = JhlConverter.depth(depth);
            String target = fileToSVNPath(localPath, false);
            String commandLine = "merge";
            boolean samePath = false;
            if (dryRun) {
            	commandLine += " --dry-run";
            }
            commandLine += depthCommandLine(d);
            if (force) {
            	commandLine += " --force";
            }
            if (ignoreAncestry) {
            	commandLine += " --ignore-ancestry";
            }
            if (path1.toString().equals(path2.toString())) {
            	samePath = true;
            	if (revision1 == null || revision2 == null) commandLine += " " + path1;
            	else commandLine += " -r" + revision1.toString() + ":" + revision2.toString() + " " + path1;
            } else {
            	commandLine += " " + path1 + "@" + revision1.toString() + " " + path2 + "@" + revision2.toString();
            }
            commandLine += " " + target;
            notificationHandler.logCommandLine(commandLine);
            File baseDir = SVNBaseDir.getBaseDir(localPath);
            notificationHandler.setBaseDir(baseDir);
    
            if (samePath) {
            	List<RevisionRange> revisionRanges;
            	if (revision1 == null && revision2 == null) {
            		revisionRanges = null;
            	}
            	else {
                	Revision rev1;
                	Revision rev2;
                	if (revision1 == null)
                		rev1 = Revision.START;
                	else
                		rev1 = JhlConverter.convert(revision1);
                	if (revision2 == null)
                		rev2 = Revision.START;
                	else
                		rev2 = JhlConverter.convert(revision2);
                	revisionRanges = new ArrayList<RevisionRange>();
                	revisionRanges.add(new RevisionRange(rev1, rev2));
            	}
            	svnClient.merge(path1.toString(), JhlConverter.convert(pegRevision), revisionRanges, target, force, d, ignoreAncestry, dryRun, recordOnly );           	
            } else
            	svnClient.merge(path1.toString(), JhlConverter.convert(revision1), path2.toString(), JhlConverter.convert(revision2), target, force, d, ignoreAncestry, dryRun, recordOnly );
            if (dryRun)
                notificationHandler.logCompleted("Dry-run merge complete.");
            else
                notificationHandler.logCompleted("Merge complete.");
        } catch (ClientException e) {
            notificationHandler.logException(e);
            if (dryRun)
                notificationHandler.logCompleted("Dry-run merge completed abnormally.");
            else
                notificationHandler.logCompleted("Merge completed abnormally.");
            SVNClientException svnClientException = new SVNClientException(e);
            svnClientException.setAprError(e.getAprError());
            throw svnClientException;          
        }        
    }    
     
    /* (non-Javadoc)
     * @see org.tigris.subversion.svnclientadapter.ISVNClientAdapter#merge(org.tigris.subversion.svnclientadapter.SVNUrl, org.tigris.subversion.svnclientadapter.SVNRevision, org.tigris.subversion.svnclientadapter.SVNUrl, org.tigris.subversion.svnclientadapter.SVNRevision, java.io.File, boolean, int, boolean, boolean)
     */
    public void merge(SVNUrl path1, SVNRevision revision1, SVNUrl path2,
            SVNRevision revision2, File localPath, boolean force,
            int depth, boolean dryRun, boolean ignoreAncestry, boolean recordOnly) throws SVNClientException {
    	SVNUrlWithPegRevision svnUrlWithPegRevision = new SVNUrlWithPegRevision(path1);
    	SVNRevision pegRevision = svnUrlWithPegRevision.getPegRevision();
    	if (pegRevision == null) {
    		if (revision2 == null) pegRevision = SVNRevision.HEAD;
    		else pegRevision = revision2;
    	}
    	merge(svnUrlWithPegRevision.getUrl(), revision1, new SVNUrlWithPegRevision(path2).getUrl(), revision2, pegRevision, localPath, force, depth, dryRun, ignoreAncestry, recordOnly);
    }
    
    /* (non-Javadoc)
     * @see org.tigris.subversion.svnclientadapter.ISVNClientAdapter#mergeReintegrate(org.tigris.subversion.svnclientadapter.SVNUrl, org.tigris.subversion.svnclientadapter.SVNRevision, java.io.File, boolean, boolean)
     */
    public void mergeReintegrate(SVNUrl path, SVNRevision pegRevision,
            File localPath, boolean force, boolean dryRun) throws SVNClientException {
       	try {
            notificationHandler.setCommand(ISVNNotifyListener.Command.MERGE);
            
            String target = fileToSVNPath(localPath, false);
            String commandLine = "merge --reintegrate";
            if (dryRun) {
            	commandLine += " --dry-run";
            }
            if (force) {
            	commandLine += " --force";
            }
            commandLine += " " + path + " " + target;
            notificationHandler.logCommandLine(commandLine);
            File baseDir = SVNBaseDir.getBaseDir(localPath);
            notificationHandler.setBaseDir(baseDir);

        	Revision peg = JhlConverter.convert(pegRevision);
        	if (peg == null) peg = Revision.HEAD;
            svnClient.mergeReintegrate(path.toString(), peg, target, dryRun);
            if (dryRun)
                notificationHandler.logCompleted("Dry-run merge complete.");
            else
                notificationHandler.logCompleted("Merge complete.");
        } catch (ClientException e) {
            notificationHandler.logException(e);
            if (dryRun)
                notificationHandler.logCompleted("Dry-run merge completed abnormally.");
            else
                notificationHandler.logCompleted("Merge completed abnormally.");
            SVNClientException svnClientException = new SVNClientException(e);
            svnClientException.setAprError(e.getAprError());
            throw svnClientException;          
        }           	
    }
    
    /* (non-Javadoc)
     * @see org.tigris.subversion.svnclientadapter.ISVNClientAdapter#addPasswordCallback(org.tigris.subversion.svnclientadapter.ISVNPromptUserPassword)
     */
    public void addPasswordCallback(ISVNPromptUserPassword callback) {
        if (callback != null) {
	        JhlPromptUserPassword prompt = new JhlPromptUserPassword(callback);
	        this.setPromptUserPassword(prompt);
        }
    }
    
    /* (non-Javadoc)
     * @see org.tigris.subversion.svnclientadapter.ISVNClientAdapter#lock(org.tigris.subversion.svnclientadapter.SVNUrl[], java.lang.String, boolean)
     */
    public void lock(SVNUrl[] uris, String comment, boolean force)
            throws SVNClientException {
        notImplementedYet();
    }

    /* (non-Javadoc)
     * @see org.tigris.subversion.svnclientadapter.ISVNClientAdapter#unlock(org.tigris.subversion.svnclientadapter.SVNUrl[], boolean)
     */
    public void unlock(SVNUrl[] uris, boolean force)
        throws SVNClientException {
        notImplementedYet();
    
    }

    /* (non-Javadoc)
     * @see org.tigris.subversion.svnclientadapter.ISVNClientAdapter#lock(java.lang.String[], java.lang.String, boolean)
     */
    public void lock(File[] paths, String comment, boolean force)
            throws SVNClientException {
        try {
            notificationHandler.setCommand(ISVNNotifyListener.Command.LOCK);
            Set<String> files = new HashSet<String>(paths.length);
            String commandLine = "lock -m \""+comment+"\"";
            if (force)
                commandLine+=" --force";

            for (int i = 0; i < paths.length; i++) {
                files.add(fileToSVNPath(paths[i], false));
            }
            commandLine = appendPaths(commandLine, files);
            notificationHandler.logCommandLine(commandLine);
			notificationHandler.setBaseDir(SVNBaseDir.getBaseDir(paths));

            svnClient.lock(files, comment, force);
            for (String file : files) {
            	notificationHandler.notifyListenersOfChange(file);
			}
        } catch (ClientException e) {
            notificationHandler.logException(e);
//            throw new SVNClientException(e);
        }

    }
    
    /* (non-Javadoc)
     * @see org.tigris.subversion.svnclientadapter.ISVNClientAdapter#unlock(java.io.File[], boolean)
     */
    public void unlock(File[] paths, boolean force) throws SVNClientException {
        try {
            notificationHandler.setCommand(ISVNNotifyListener.Command.LOCK);
            Set<String> files = new HashSet<String>(paths.length);
            String commandLine = "unlock ";
            if (force)
                commandLine+=" --force";
    
            for (int i = 0; i < paths.length; i++) {
                files.add(fileToSVNPath(paths[i], false));
            }
            commandLine = appendPaths(commandLine, files);
            notificationHandler.logCommandLine(commandLine);
    		notificationHandler.setBaseDir(SVNBaseDir.getBaseDir(paths));
    
            svnClient.unlock(files, force);
            for (String file : files) {
            	notificationHandler.notifyListenersOfChange(file);
			}
        } catch (ClientException e) {
            notificationHandler.logException(e);
 //           throw new SVNClientException(e);
        }
    
    }

	/* (non-Javadoc)
	 * @see org.tigris.subversion.svnclientadapter.ISVNClientAdapter#setRevProperty(org.tigris.subversion.svnclientadapter.SVNUrl, org.tigris.subversion.svnclientadapter.SVNRevision.Number, java.lang.String, java.lang.String, boolean)
	 */
	public void setRevProperty(SVNUrl url, SVNRevision.Number revisionNo, String propName, String propertyData, boolean force) throws SVNClientException {
		try {
			notificationHandler.setCommand(ISVNNotifyListener.Command.PROPSET);

			notificationHandler.logCommandLine(
				"propset --revprop -r " + revisionNo.toString()
					+ (force ? " --force " : "")
					+ " \""
					+ propName
					+ "\"  \""
					+ propertyData
					+ "\" "
					+ url.toString());
			notificationHandler.setBaseDir();
			if (propName.startsWith("svn:")) {
				svnClient.setRevProperty(url.toString(), propName, Revision.getInstance(revisionNo.getNumber()), fixSVNString(propertyData), null, true);
			} else {
				svnClient.setRevProperty(url.toString(), propName, Revision.getInstance(revisionNo.getNumber()), propertyData, null, true);
			}
		} catch (ClientException e) {
			notificationHandler.logException(e);
			throw new SVNClientException(e);
		}		
	}
	
    /* (non-Javadoc)
     * @see org.tigris.subversion.svnclientadapter.ISVNClientAdapter#getRevProperty(org.tigris.subversion.svnclientadapter.SVNUrl, org.tigris.subversion.svnclientadapter.SVNRevision.Number)
     */
    public String getRevProperty(SVNUrl url, SVNRevision.Number revisionNo, String propName) throws SVNClientException {
      String propData = null;
        try {
            notificationHandler.setCommand(ISVNNotifyListener.Command.PROPGET);

            notificationHandler.logCommandLine(
                "propget --revprop -r " + revisionNo.toString()
                    + " \""
                    + propName
                    + "\"  "
                    + url.toString());
            notificationHandler.setBaseDir();
           byte[] bytes = svnClient.revProperty(url.toString(), propName, Revision.getInstance(revisionNo.getNumber())); //setRevProperty(url.toString(), propName, Revision.getInstance(revisionNo.getNumber()), fixSVNString(propertyData), true);
			try {
				// Assume property data is UTF8. Technically, only svn: props are forced
				// to be UTF8.
				propData = new String(bytes, "UTF8");
			} catch (UnsupportedEncodingException e) {
				propData = new String(bytes);
			}
            if (propName.startsWith("svn:")) {
              fixSVNString(propData);
            } 
        } catch (ClientException e) {
            notificationHandler.logException(e);
            throw new SVNClientException(e);
        }    
        return propData;
    }	
    
    /* (non-Javadoc)
     * @see org.tigris.subversion.svnclientadapter.ISVNClientAdapter#getRevProperties(org.tigris.subversion.svnclientadapter.SVNUrl, org.tigris.subversion.svnclientadapter.SVNRevision.Number)
     */
    public ISVNProperty[] getRevProperties(SVNUrl url, SVNRevision.Number revisionNo) throws SVNClientException {
        try {
            notificationHandler.setCommand(ISVNNotifyListener.Command.PROPLIST);
            String target = url.toString();
            notificationHandler.logCommandLine(
                    "proplist --revprop -r " + revisionNo.toString()
                    + target);
            
            notificationHandler.setBaseDir();
            Map<String, byte[]> propertiesData = svnClient.revProperties(target, Revision.getInstance(revisionNo.getNumber()));
            if (propertiesData == null) {
                // no properties
                return new JhlPropertyData[0];
            }
            Set<String> keys = propertiesData.keySet();
            JhlPropertyData[] svnProperties = new JhlPropertyData[keys.size()];
            int i=0;
            for (String key : keys) {
                svnProperties[i] = JhlPropertyData.newForUrl(target, key, propertiesData.get(key));
                i++;
			}
            return svnProperties;
        } catch (ClientException e) {
            notificationHandler.logException(e);
            throw new SVNClientException(e);
        }       
    }    

	/* (non-Javadoc)
	 * @see org.tigris.subversion.svnclientadapter.ISVNClientAdapter#getAdminDirectoryName()
	 */
	public String getAdminDirectoryName() {
		return svnClient.getAdminDirectoryName();
	}

	/* (non-Javadoc)
	 * @see org.tigris.subversion.svnclientadapter.ISVNClientAdapter#isAdminDirectory(java.lang.String)
	 */
	public boolean isAdminDirectory(String name) {
		return svnClient.isAdminDirectory(name);
	}

	private void getLogMessages(String target, SVNRevision pegRevision, SVNRevision revisionStart, SVNRevision revisionEnd, boolean stopOnCopy, boolean fetchChangePath, long limit, boolean includeMergedRevisions, String[] requestedProperties, ISVNLogMessageCallback worker) throws SVNClientException {
		try {
			notificationHandler.setCommand(
				ISVNNotifyListener.Command.LOG);
			String logExtras = "";
			if (includeMergedRevisions)
				logExtras = logExtras + " -g";
			if (stopOnCopy)
			    logExtras = logExtras + " --stop-on-copy";
			if (limit > 0 )
			    logExtras = logExtras + " --limit " + limit;
			notificationHandler.logCommandLine(
				"log -r "
					+ revisionStart.toString()
					+ ":"
					+ revisionEnd.toString()
					+ " "
					+ target
					+ logExtras);
			JhlLogMessageCallback callback = new JhlLogMessageCallback(worker);
			Set<String> revProps = new HashSet<String>(requestedProperties.length);
			for (int i = 0; i < requestedProperties.length; i++) {
				revProps.add(requestedProperties[i]);
			}
			List<RevisionRange> range = new ArrayList<RevisionRange>();
			range.add(new RevisionRange(JhlConverter.convert(revisionStart), JhlConverter.convert(revisionEnd)));
			svnClient.logMessages(target, JhlConverter.convert(pegRevision),
                    range, stopOnCopy, fetchChangePath, includeMergedRevisions, 
					revProps, limit, callback);
		} catch (ClientException e) {
			if (e.getAprError() == ErrorCodes.unsupportedFeature && includeMergedRevisions) {
				getLogMessages(target, pegRevision, revisionStart, revisionEnd, stopOnCopy, fetchChangePath, limit, false, requestedProperties, worker);
			} else {
				if ((e.getAprError() == ErrorCodes.fsNotFound || e.getAprError() == ErrorCodes.clientUnrelatedResources) && pegRevision != null && !pegRevision.equals(revisionStart)) {
					getLogMessages(target, pegRevision, pegRevision, revisionEnd, stopOnCopy, fetchChangePath, limit, includeMergedRevisions, requestedProperties, worker);
				} else {
					notificationHandler.logException(e);
					throw new SVNClientException(e);
				}
			}
		}
	}

    public void getLogMessages(
	            File path, 
	            SVNRevision pegRevision,
	            SVNRevision revisionStart,
	            SVNRevision revisionEnd,
	            boolean stopOnCopy,
	            boolean fetchChangePath,
	            long limit,
	            boolean includeMergedRevisions,
	            String [] requestedProperties,
	            ISVNLogMessageCallback worker)
	            throws SVNClientException {
    	
		String target = fileToSVNPath(path, false);
		notificationHandler.setBaseDir();
		this.getLogMessages(target, pegRevision, revisionStart, revisionEnd, stopOnCopy, fetchChangePath, limit, includeMergedRevisions, requestedProperties, worker);
    }
    
    public void getLogMessages(
	            SVNUrl url, 
	            SVNRevision pegRevision,
	            SVNRevision revisionStart,
	            SVNRevision revisionEnd,
	            boolean stopOnCopy,
	            boolean fetchChangePath,
	            long limit,
	            boolean includeMergedRevisions,
	            String [] requestedProperties,
	            ISVNLogMessageCallback worker)
	            throws SVNClientException {
    	
		String target = url.toString();
		notificationHandler.setBaseDir();
		this.getLogMessages(target, pegRevision, revisionStart, revisionEnd, stopOnCopy, fetchChangePath, limit, includeMergedRevisions, requestedProperties, worker);
    }

	/* (non-Javadoc)
     * @see org.tigris.subversion.svnclientadapter.ISVNClientAdapter#relocate(java.lang.String, java.lang.String, java.lang.String, boolean)
     */
    public void relocate(String from, String to, String path, boolean recurse)
            throws SVNClientException {
		try {
			notificationHandler.setCommand(ISVNNotifyListener.Command.RELOCATE);
		    notificationHandler.logCommandLine("switch --relocate "+ from + " " + to + " " + path);
			notificationHandler.setBaseDir(SVNBaseDir.getBaseDir(new File(path)));
			boolean ignoreAncestry = false;
			svnClient.relocate(from, to, path, ignoreAncestry);
		} catch (ClientException e) {
			notificationHandler.logException(e);
			throw new SVNClientException(e);            
		}        
    }
    
	/* (non-Javadoc)
	 * @see org.tigris.subversion.svnclientadapter.ISVNClientAdapter#diff(java.io.File, org.tigris.subversion.svnclientadapter.SVNUrl, org.tigris.subversion.svnclientadapter.SVNRevision, java.io.File, boolean)
	 */
	public void diff(File path, SVNUrl url, SVNRevision urlRevision,
			File outFile, boolean recurse) throws SVNClientException {
        try {
            notificationHandler.setCommand(ISVNNotifyListener.Command.DIFF);
                
            // we don't want canonical file path (otherwise the complete file name
            // would be in the patch). This way the user can choose to use a relative
            // path
            String wcPath = fileToSVNPath(path, false);
            String svnOutFile = fileToSVNPath(outFile, false);
            
            String commandLine = "diff --old " + wcPath + " ";
           	commandLine += "--new " + url.toString();
            if (!urlRevision.equals(SVNRevision.HEAD))
            	commandLine += "@"+ urlRevision.toString();
            
            notificationHandler.logCommandLine(commandLine);
			notificationHandler.setBaseDir(SVNBaseDir.getBaseDir(path));
            svnClient.diff(wcPath,Revision.WORKING,url.toString(),JhlConverter.convert(urlRevision), null, svnOutFile, Depth.infinityOrEmpty(recurse), null, false, true, false, true);
        } catch (ClientException e) {
            notificationHandler.logException(e);
            throw new SVNClientException(e);            
        }
	}

	public void mkdir(SVNUrl url, boolean makeParents, String message)
	throws SVNClientException {
        try {
        	String fixedMessage = fixSVNString(message);

        	if (fixedMessage == null)
        		fixedMessage = "";
           notificationHandler.setCommand(ISVNNotifyListener.Command.MKDIR);
		    Set<String> target = new HashSet<String>();
		    target.add(url.toString());
		    if (makeParents)
	            notificationHandler.logCommandLine(
	                    "mkdir --parents -m \""+getFirstMessageLine(fixedMessage)+"\" "+target);
		    else
	            notificationHandler.logCommandLine(
	                "mkdir -m \""+getFirstMessageLine(fixedMessage)+"\" "+target);
			notificationHandler.setBaseDir();
            svnClient.mkdir(target, makeParents, null, new JhlCommitMessage(fixedMessage), null);
        } catch (ClientException e) {
            notificationHandler.logException(e);
            throw new SVNClientException(e);
        }                   	
	}

	public void merge(SVNUrl url, SVNRevision pegRevision, SVNRevisionRange[] revisions, File localPath, boolean force, int depth, boolean ignoreAncestry, boolean dryRun, boolean recordOnly) throws SVNClientException {
    	try {
            notificationHandler.setCommand(ISVNNotifyListener.Command.MERGE);

            String target = fileToSVNPath(localPath, false);
            String commandLine = "merge";
            if (dryRun) {
            	commandLine += " --dry-run";
            }
            Depth d = JhlConverter.depth(depth);
            commandLine += depthCommandLine(d);
            if (force) {
            	commandLine += " --force";
            }
            if (ignoreAncestry) {
            	commandLine += " --ignore-ancestry";
            }
            List<RevisionRange> range = JhlConverter.convert(revisions);
            for (int i = 0; i < revisions.length; i++) {
				commandLine += " " + revisions[i].toMergeString();
			}
            commandLine += " " + url.toString();
            
            commandLine += " " + target;
            notificationHandler.logCommandLine(commandLine);
            File baseDir = SVNBaseDir.getBaseDir(localPath);
            notificationHandler.setBaseDir(baseDir);
        	Revision peg = JhlConverter.convert(pegRevision);
        	if (peg == null) peg = Revision.HEAD;
        	svnClient.merge(url.toString(), peg, range, target, force, d, ignoreAncestry, dryRun, recordOnly);
            if (dryRun)
                notificationHandler.logCompleted("Dry-run merge complete.");
            else
                notificationHandler.logCompleted("Merge complete.");
        } catch (ClientException e) {
            notificationHandler.logException(e);
            if (dryRun)
                notificationHandler.logCompleted("Dry-run merge completed abnormally.");
            else
                notificationHandler.logCompleted("Merge completed abnormally.");
            SVNClientException svnClientException = new SVNClientException(e);
            svnClientException.setAprError(e.getAprError());
            throw svnClientException;          
        }        
	}

	private String depthCommandLine(Depth depth) {
		switch (depth) {
		case empty:
			return " --depth=empty";
		case files:
			return " --depth=files";
		case immediates:
			return " --depth=immediates";
		case infinity:
			return " --depth=infinity";
		case exclude:
			return " --depth=exclude";
		default:
			return "";
		}
	}

	public ISVNMergeInfo getMergeInfo(File path, SVNRevision revision) throws SVNClientException {
		return this.getMergeInfo(fileToSVNPath(path, false), JhlConverter.convert(revision));
	}

	public ISVNMergeInfo getMergeInfo(SVNUrl url, SVNRevision revision) throws SVNClientException {
		return this.getMergeInfo(url.toString(), JhlConverter.convert(revision));
	}

	private ISVNMergeInfo getMergeInfo(String path, Revision revision) throws SVNClientException {
        try {
        	Mergeinfo info = svnClient.getMergeinfo(path, revision);
        	if (info == null) return null;
        	return new JhlMergeInfo(info);
        } catch (SubversionException e) {
            throw new SVNClientException(e);
		}           	
		
	}

	public void addConflictResolutionCallback(ISVNConflictResolver callback) {
		if (callback == null)
			conflictResolver = null;
		else
			conflictResolver = new JhlConflictResolver(callback);
		svnClient.setConflictResolver(conflictResolver);
	}
	
	public void setProgressListener(ISVNProgressListener listener) {
		progressListener.setWorker(listener);
	}

	private SVNDiffSummary[] diffSummarize(String target1, SVNRevision revision1,
			String target2, SVNRevision revision2, int depth,
			boolean ignoreAncestry)
			throws SVNClientException {
        try {
            notificationHandler.setCommand(ISVNNotifyListener.Command.DIFF);
                
            if (revision1 == null)
                revision1 = SVNRevision.HEAD;
            if (revision2 == null)
                revision2 = SVNRevision.HEAD;
            
            String commandLine = "diff --summarize";
            Depth d = JhlConverter.depth(depth);
            commandLine += depthCommandLine(d);
            if (ignoreAncestry)
            	commandLine += " --ignoreAncestry";
            commandLine += " " + target1 + "@" + revision1 + " " + target2 + "@" + revision2;
            notificationHandler.logCommandLine(commandLine);
			notificationHandler.setBaseDir();
			JhlDiffSummaryReceiver callback = new JhlDiffSummaryReceiver();
			svnClient.diffSummarize(target1, JhlConverter.convert(revision1), target2, JhlConverter.convert(revision2), d, null, ignoreAncestry, callback);
			return callback.getDiffSummary();
        } catch (ClientException e) {
            notificationHandler.logException(e);
            throw new SVNClientException(e);            
        }
	}

	private SVNDiffSummary[] diffSummarize(String target, SVNRevision pegRevision,
			SVNRevision startRevision, SVNRevision endRevision, int depth,
			boolean ignoreAncestry)
			throws SVNClientException {
        try {
            notificationHandler.setCommand(ISVNNotifyListener.Command.DIFF);
                
            if (pegRevision == null)
                pegRevision = SVNRevision.HEAD;
            if (startRevision == null)
                startRevision = SVNRevision.HEAD;
            if (endRevision == null)
                endRevision = SVNRevision.HEAD;
            
            String commandLine = "diff --summarize";
            Depth d = JhlConverter.depth(depth);
            commandLine += depthCommandLine(d);
            if (ignoreAncestry)
            	commandLine += " --ignoreAncestry";
           commandLine += " -r " + startRevision + ":" + endRevision + " " + target;
            notificationHandler.logCommandLine(commandLine);
			notificationHandler.setBaseDir();
			JhlDiffSummaryReceiver callback = new JhlDiffSummaryReceiver();
			svnClient.diffSummarize(target, JhlConverter.convert(pegRevision), JhlConverter.convert(startRevision), JhlConverter.convert(endRevision), 
					d, null, ignoreAncestry, callback);
			return callback.getDiffSummary();
        } catch (ClientException e) {
            notificationHandler.logException(e);
            throw new SVNClientException(e);            
        }
	}

	public SVNDiffSummary[] diffSummarize(File path, SVNRevision pegRevision,
			SVNRevision startRevision, SVNRevision endRevision, int depth,
			boolean ignoreAncestry) throws SVNClientException {
		String target = fileToSVNPath(path, false);
		return this.diffSummarize(target, pegRevision,
				startRevision, endRevision, depth, ignoreAncestry);
	}

	public SVNDiffSummary[] diffSummarize(SVNUrl url,
			SVNRevision pegRevision, SVNRevision startRevision,
			SVNRevision endRevision, int depth, boolean ignoreAncestry)
			throws SVNClientException {
		return this.diffSummarize(url.toString(), pegRevision,
				startRevision, endRevision, depth, ignoreAncestry);
	}

	public SVNDiffSummary[] diffSummarize(File target1, SVNRevision revision1,
			SVNUrl target2, SVNRevision revision2, int depth,
			boolean ignoreAncestry) throws SVNClientException {
		return diffSummarize(fileToSVNPath(target1, false), revision1,
				target2.toString(), revision2, depth,
				ignoreAncestry);
	}

	public SVNDiffSummary[] diffSummarize(SVNUrl target1,
			SVNRevision revision1, SVNUrl target2, SVNRevision revision2,
			int depth, boolean ignoreAncestry) throws SVNClientException {
		return diffSummarize(target1.toString(), revision1,
				target2.toString(), revision2, depth,
				ignoreAncestry);
	}

	public SVNDiffSummary[] diffSummarize(File path, SVNUrl toUrl, SVNRevision toRevision, boolean recurse) throws SVNClientException {
		return diffSummarize(path.getAbsolutePath(), SVNRevision.WORKING, toUrl.toString(), toRevision, Depth.infinity.ordinal(), true);
	}

	private SVNDiffKind getDiffKind(String changedResource,
			boolean deletedLines, boolean addedLines, boolean contextLines,
			boolean oldRev0, boolean newRev0) {
		SVNDiffKind diffKind;			
		if (oldRev0 && newRev0) {
			diffKind = SVNDiffKind.DELETED;
		}
		else if (addedLines && !deletedLines && !contextLines) diffKind = SVNDiffKind.ADDED;
		else if ((!deletedLines && !addedLines) || deletedLines && !addedLines && !contextLines) {
			if (exists(changedResource)) {
				diffKind = SVNDiffKind.DELETED;
			} else {
				diffKind = SVNDiffKind.ADDED;
			}
		}
		else diffKind = SVNDiffKind.MODIFIED;
		return diffKind;
	}
	
	private boolean exists(String changedResource) {
		File file = new File(changedResource);
		return file.exists();
	}

	public String[] suggestMergeSources(File path) throws SVNClientException {
		String[] sources = null;
		try {
			Set<String> s = svnClient.suggestMergeSources(fileToSVNPath(path, false), Revision.HEAD);
			sources = new String[s.size()];
			s.toArray(sources);
		} catch (SubversionException e) {
            throw new SVNClientException(e);
		}
		return sources;
	}

	public String[] suggestMergeSources(SVNUrl url, SVNRevision peg) throws SVNClientException {
		String[] sources = null;
		try {
			Set<String> s = svnClient.suggestMergeSources(url.toString(), JhlConverter.convert(peg));
			sources = new String[s.size()];
			s.toArray(sources);
		} catch (SubversionException e) {
            throw new SVNClientException(e);
		}
		return sources;
	}

	
	public void createPatch(File[] paths, File relativeToPath, File outFile,
			boolean recurse) throws SVNClientException {
		FileOutputStream os = null;
		try {
			List<File> tempFiles = new ArrayList<File>();
			for (int i = 0; i < paths.length; i++) {
				File tempFile = File.createTempFile("tempDiff", ".txt");
				tempFile.deleteOnExit();
				this.diffRelative(paths[i], SVNRevision.BASE, paths[i], SVNRevision.WORKING,
						tempFile, recurse, false, false, false,
						relativeToPath);
				tempFiles.add(tempFile);
			}
			os = new FileOutputStream(outFile);
			for (File tempFile : tempFiles) {
				FileInputStream is = new FileInputStream(tempFile);
				byte[] buffer = new byte[4096];
				int bytes_read;
				while ((bytes_read = is.read(buffer)) != -1)
					os.write(buffer, 0, bytes_read);				
				is.close();
			}
		} catch (Exception e) {
			throw new SVNClientException(e);
		} finally {
			if (os != null) try {os.close();} catch (IOException e) {}
		}
	}

	public void dispose() {
		svnClient.dispose();
	}

	public ISVNLogMessage[] getMergeinfoLog(int kind, File path,
			SVNRevision pegRevision, SVNUrl mergeSourceUrl,
			SVNRevision srcPegRevision, boolean discoverChangedPaths)
			throws SVNClientException {
		return this.getMergeinfoLog(kind, fileToSVNPath(path, false), pegRevision, mergeSourceUrl, srcPegRevision, discoverChangedPaths);
	}

	public ISVNLogMessage[] getMergeinfoLog(int kind, SVNUrl url,
			SVNRevision pegRevision, SVNUrl mergeSourceUrl,
			SVNRevision srcPegRevision, boolean discoverChangedPaths)
			throws SVNClientException {
		return this.getMergeinfoLog(kind, url.toString(), pegRevision, mergeSourceUrl, srcPegRevision, discoverChangedPaths);
	}

	private ISVNLogMessage[] getMergeinfoLog(int kind, String target,
			SVNRevision pegRevision, SVNUrl mergeSourceUrl,
			SVNRevision srcPegRevision, boolean discoverChangedPaths)
			throws SVNClientException {
		try {
			notificationHandler.setCommand(
				ISVNNotifyListener.Command.MERGEINFO);
			String show = "";
			org.apache.subversion.javahl.types.Mergeinfo.LogKind mergeKind = org.apache.subversion.javahl.types.Mergeinfo.LogKind.eligible;
			if (kind == ISVNMergeinfoLogKind.eligible)
				show = show + " --show-revs eligible ";
			if (kind == ISVNMergeinfoLogKind.merged) {
				show = show + " --show-revs merged ";
				mergeKind = org.apache.subversion.javahl.types.Mergeinfo.LogKind.merged;
			}
			notificationHandler.logCommandLine(
				"mergeinfo "
					+ show
					+ mergeSourceUrl.toString()
					+ " "
					+ target);
			SVNLogMessageCallback worker = new SVNLogMessageCallback();
			JhlLogMessageCallback callback = new JhlLogMessageCallback(worker);
			Set<String> revProps = new HashSet<String>();
			revProps.add("svn:author");
			revProps.add("svn:date");
			revProps.add("svn:log");
			svnClient.getMergeinfoLog(mergeKind, target, JhlConverter.convert(pegRevision),
					mergeSourceUrl.toString(), JhlConverter.convert(srcPegRevision),
					discoverChangedPaths, Depth.infinity, revProps, callback);
			return worker.getLogMessages();
		} catch (ClientException e) {
			if (e.getAprError() == ErrorCodes.unsupportedFeature) {
				return this.getLogMessages(mergeSourceUrl, srcPegRevision, new SVNRevision.Number(0), SVNRevision.HEAD, true, discoverChangedPaths, 0, false);
			}
			notificationHandler.logException(e);
			throw new SVNClientException(e);
		}
	}
	
	private String appendPaths(String commandLine, Set<String> paths) {
		StringBuffer stringBuffer = new StringBuffer(commandLine);
		if (paths.size() > 5) stringBuffer.append(" (" + paths.size() + " paths specified)");
		else {
			for (String s : paths) {
				stringBuffer.append(" " + s);
			}
		}
		return stringBuffer.toString();
	}
	
	private String getFirstMessageLine(String message) {
		StringTokenizer tokenizer = new StringTokenizer(message, "\n");
		int count = tokenizer.countTokens();
		if (count > 1) return tokenizer.nextToken() + "...";
		else return message;
		
	}

	/**
	 * Applies any SVN rules to strings (commit messages and property values).
	 * Currently that means making all line-endings LF
	 * @param message
	 * @return
	 */
	protected String fixSVNString(String message) {
		if (message == null)
			return null;
		// Normalize all line endings to LF
		return message.replaceAll("\r\n", "\n").replaceAll("\r", "\n");
	}
	
}
