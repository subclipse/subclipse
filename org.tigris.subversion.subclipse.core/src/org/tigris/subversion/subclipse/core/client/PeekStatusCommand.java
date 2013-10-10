/*******************************************************************************
 * Copyright (c) 2005, 2006 Subclipse project and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Subclipse project committers - initial API and implementation
 ******************************************************************************/
package org.tigris.subversion.subclipse.core.client;

import java.io.File;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;
import org.tigris.subversion.subclipse.core.ISVNCoreConstants;
import org.tigris.subversion.subclipse.core.SVNException;
import org.tigris.subversion.subclipse.core.SVNProviderPlugin;
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
    private final IPath     path;

    private ISVNStatus status = null;
    private ISVNInfo info = null;
    protected SVNRevision.Number revision;
    
    private boolean checkForReadOnly = SVNProviderPlugin.getPlugin().getPluginPreferences().getBoolean(ISVNCoreConstants.PREF_SHOW_READ_ONLY);

    public PeekStatusCommand(IResource resource) {
        this.resource = resource;
        this.path     = null;
    }

    public PeekStatusCommand(IPath path) {
        this.resource = null;
        this.path     = path;
    }

    public void execute() throws SVNException {
    	ISVNClientAdapter client = null;
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
            client = SVNProviderPlugin.getPlugin().getSVNClientManager().getSVNClient();
            client.addNotifyListener( revisionListener );
            File file;
            if (resource != null)
            	file = resource.getLocation().toFile();
            else
            	file = path.toFile();
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
        	if (client != null) {
	            client.removeNotifyListener( revisionListener );
	            SVNProviderPlugin.getPlugin().getSVNClientManager().returnSVNClient(client);
        	}
        }
    }

    public ISVNStatus getStatus() {
        return status;
    }

    public LocalResourceStatus getLocalResourceStatus()
    {    	
    	return (status != null) ? new LocalResourceStatus(status, getURL(status), checkForReadOnly) : null;
    }
    
    public SVNRevision.Number getRevision() {
        return revision;
    }


    // getStatuses returns null URL for svn:externals folder.  This will
    // get the URL using svn info command on the local resource
	private String getURL(ISVNStatus status) {
		String url = status.getUrlString();
		if (url == null && info != null) {
	    	SVNUrl svnurl = info.getUrl();
	    	url = (svnurl != null) ? svnurl.toString() : null;
		}
		return url;
	}
}
