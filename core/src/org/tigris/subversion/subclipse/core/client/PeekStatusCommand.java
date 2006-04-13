/******************************************************************************
 * This program and the accompanying materials are made available under
 * the terms of the Common Public License v1.0 which accompanies this
 * distribution, and is available at the following URL:
 * http://www.eclipse.org/legal/cpl-v10.html
 * Copyright(c) 2003-2005 by the authors indicated in the @author tags.
 *
 * All Rights are Reserved by the various authors.
 *******************************************************************************/
package org.tigris.subversion.subclipse.core.client;

import java.io.File;

import org.eclipse.core.resources.IResource;
import org.tigris.subversion.subclipse.core.SVNException;
import org.tigris.subversion.subclipse.core.resources.LocalResourceStatus;
import org.tigris.subversion.svnclientadapter.ISVNClientAdapter;
import org.tigris.subversion.svnclientadapter.ISVNInfo;
import org.tigris.subversion.svnclientadapter.ISVNNotifyListener;
import org.tigris.subversion.svnclientadapter.ISVNStatus;
import org.tigris.subversion.svnclientadapter.SVNClientException;
import org.tigris.subversion.svnclientadapter.SVNNodeKind;
import org.tigris.subversion.svnclientadapter.SVNRevision;
import org.tigris.subversion.svnclientadapter.SVNStatusKind;
import org.tigris.subversion.svnclientadapter.SVNUrl;

/**
 * Peek for (get) the resource status.
 * Do not descend to children and DO NOT affect sync cache in any way !
 * This command should have no side effects.
 */
public class PeekStatusCommand {
    private final IResource resource;

    private ISVNStatus status = null;
    private ISVNInfo info = null;
    protected SVNRevision.Number revision;

    public PeekStatusCommand(IResource resource) {
        this.resource = resource;
    }

    public void execute(ISVNClientAdapter client) throws SVNException {
        ISVNNotifyListener revisionListener = new ISVNNotifyListener() {
            public void setCommand(int command) {}
            public void logCommandLine(String commandLine) {}
            public void logMessage(String message) {}
            public void logError(String message) {}
            public void logRevision(long aRevision, String path) {
                PeekStatusCommand.this.revision = new SVNRevision.Number(aRevision);
            }
            public void logCompleted(String message) {}
            public void onNotify(File path, SVNNodeKind kind) {}
        };

        try{
            client.addNotifyListener( revisionListener );
            File file = resource.getLocation().toFile();
            status = null;
            ISVNStatus[] statuses = client.getStatus( file, false, true, false);
            for (int i = 0; i < statuses.length; i++) {
				if (file.equals(statuses[i].getFile()))
				{
					status = statuses[i];
					if (status.getUrl() == null && !(status.getTextStatus() == SVNStatusKind.UNVERSIONED))
						info = client.getInfo(status.getFile());
					break;
				}
			}
        }
        catch (SVNClientException e) {
        	throw SVNException.wrapException(e);
        }
        finally {
            client.removeNotifyListener( revisionListener );
        }
    }

    public ISVNStatus getStatus() {
        return status;
    }

    public LocalResourceStatus getLocalResourceStatus()
    {    	
    	return (status != null) ? new LocalResourceStatus(status, getURL(status)) : null;
    }
    
    public SVNRevision.Number getRevision() {
        return revision;
    }


    // getStatuses returns null URL for svn:externals folder.  This will
    // get the URL using svn info command on the local resource
	private SVNUrl getURL(ISVNStatus status) {
		SVNUrl url = status.getUrl();
		if (url == null && info != null) {
	    	url = info.getUrl();
		}
		return url;
	}
}
