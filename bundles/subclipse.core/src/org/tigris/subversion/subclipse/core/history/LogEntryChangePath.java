/*******************************************************************************
 * Copyright (c) 2004, 2006 Subclipse project and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Subclipse project committers - initial API and implementation
 ******************************************************************************/
package org.tigris.subversion.subclipse.core.history;

import org.eclipse.core.runtime.PlatformObject;
import org.tigris.subversion.subclipse.core.ISVNRemoteResource;
import org.tigris.subversion.subclipse.core.ISVNRepositoryLocation;
import org.tigris.subversion.subclipse.core.SVNException;
import org.tigris.subversion.subclipse.core.commands.GetRemoteResourceCommand;
import org.tigris.subversion.svnclientadapter.ISVNLogMessageChangePath;
import org.tigris.subversion.svnclientadapter.SVNRevision;
import org.tigris.subversion.svnclientadapter.SVNUrl;

/**
 * A changePath in LogEntry
 * @see ISVNLogMessageChangePath
 */
public class LogEntryChangePath extends PlatformObject {

    private ISVNLogMessageChangePath logMessageChangePath;
    private ILogEntry logEntry;
    private ISVNRemoteResource remoteResource;
    
    public LogEntryChangePath(ILogEntry logEntry, ISVNLogMessageChangePath logMessageChangePath) {
        this.logMessageChangePath = logMessageChangePath;
        this.logEntry = logEntry;
    }
    
    /**
     * Retrieve the path to the commited item
     * @return  the path to the commited item
     */
    public String getPath() {
    	return logMessageChangePath.getPath();
    }

    /**
     * Retrieve the copy source revision (if any)
     * @return  the copy source revision (if any)
     */
    public SVNRevision.Number getCopySrcRevision() {
    	return logMessageChangePath.getCopySrcRevision();
    }

    /**
     * Retrieve the copy source path (if any)
     * @return  the copy source path (if any)
     */
    public String getCopySrcPath() {
    	return logMessageChangePath.getCopySrcPath();
    }

    /**
     * Retrieve action performed
     * @return  action performed
     */
    public char getAction() {
        return logMessageChangePath.getAction();
    }
    
    
	/**
	 * @return Returns the logEntry.
	 */
	public ILogEntry getLogEntry() {
		return logEntry;
	}

    private ISVNRepositoryLocation getRepository() {
    	return logEntry.getResource().getRepository();
    }
    
    /**
     * get the url corresponding to this changed path or null if it cannot
     * be determined 
     */
    public SVNUrl getUrl() {
    	SVNUrl repositoryRoot = getRepository().getRepositoryRoot();
        if (repositoryRoot != null) {
			return repositoryRoot.appendPath(getPath());
        } else {
        	return null;
        }
    }
    
    public SVNRevision getRevision() {
    	return logEntry.getRevision();
    }
    
    /**
     * get the remote resource corresponding to this changed path or null
     * if it cannot be determined
     * @return
     * @throws SVNException
     */
    public ISVNRemoteResource getRemoteResource() throws SVNException {
    	SVNRevision revision = getRevision();
    	if (getAction() == 'D') {
    		long rev = Long.parseLong(revision.toString());
    		rev--;
    		revision = new SVNRevision.Number(rev);
    	}
        SVNUrl url = getUrl();
        if (url == null) {
            return null;
        }
        if (remoteResource == null) {
        	GetRemoteResourceCommand command = new GetRemoteResourceCommand(getRepository(), url, revision);
        	command.run(null);
        	remoteResource = command.getRemoteResource();
        }
        return remoteResource;
    }


    /*
     * (non-Javadoc)
     * @see org.eclipse.core.runtime.IAdaptable#getAdapter(java.lang.Class)
     */
    public Object getAdapter(Class adapter) {
      // Could the specfied adapter possibly be a remote resource?
      if (ISVNRemoteResource.class.isAssignableFrom(adapter)) {
        ISVNRemoteResource aRemoteResource = null;
        try {
          aRemoteResource = getRemoteResource();
        } catch (SVNException e) {
        }
        // Is the actual resource type compatible with the requested type?
        if (adapter.isInstance(aRemoteResource)) {
          return aRemoteResource;
        }
      }
      return super.getAdapter(adapter);
    }    
    
}
