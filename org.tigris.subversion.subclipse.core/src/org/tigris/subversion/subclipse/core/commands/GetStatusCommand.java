/*******************************************************************************
 * Copyright (c) 2003, 2004 Subclipse project and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Subclipse project committers - initial API and implementation
 ******************************************************************************/
package org.tigris.subversion.subclipse.core.commands;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.tigris.subversion.subclipse.core.ISVNCoreConstants;
import org.tigris.subversion.subclipse.core.ISVNLocalResource;
import org.tigris.subversion.subclipse.core.ISVNRepositoryLocation;
import org.tigris.subversion.subclipse.core.SVNException;
import org.tigris.subversion.subclipse.core.SVNProviderPlugin;
import org.tigris.subversion.subclipse.core.resources.LocalResourceStatus;
import org.tigris.subversion.svnclientadapter.ISVNClientAdapter;
import org.tigris.subversion.svnclientadapter.ISVNInfo;
import org.tigris.subversion.svnclientadapter.ISVNStatus;
import org.tigris.subversion.svnclientadapter.SVNClientException;
import org.tigris.subversion.svnclientadapter.SVNStatusKind;
import org.tigris.subversion.svnclientadapter.SVNUrl;

/**
 * Command to get the statuses of local resources
 */
public class GetStatusCommand implements ISVNCommand {
    private ISVNRepositoryLocation repository;
    private IResource resource;
    private boolean descend = true;
    private boolean getAll = true;
    private ISVNStatus[] svnStatuses;
    private boolean checkForReadOnly = SVNProviderPlugin.getPlugin().getPluginPreferences().getBoolean(ISVNCoreConstants.PREF_SHOW_READ_ONLY);
    
    public GetStatusCommand(ISVNLocalResource svnResource, boolean descend, boolean getAll) {
    	this.repository = svnResource.getRepository();
    	this.resource = svnResource.getIResource();
        this.descend = descend;
        this.getAll = getAll;
    }

    public GetStatusCommand(ISVNRepositoryLocation repository, IResource resource, boolean descend, boolean getAll) {
    	this.repository = repository;
    	this.resource = resource;
        this.descend = descend;
        this.getAll = getAll;
    }    

    /* (non-Javadoc)
     * @see org.tigris.subversion.subclipse.core.commands.ISVNCommand#run(org.eclipse.core.runtime.IProgressMonitor)
     */
    public void run(IProgressMonitor monitor) throws SVNException {
        ISVNClientAdapter svnClient = repository.getSVNClient();
        try { 
            svnStatuses = svnClient.getStatus(resource.getLocation().toFile(), descend, getAll);
        } catch (SVNClientException e) {
            throw SVNException.wrapException(e);
        }
        finally {
        	repository.returnSVNClient(svnClient);
        }
    }

    private LocalResourceStatus[] convert(ISVNStatus[] statuses) {
        LocalResourceStatus[] localStatuses = new LocalResourceStatus[statuses.length];
        for (int i = 0; i < statuses.length;i++) {
            localStatuses[i] = new LocalResourceStatus(statuses[i], getURL(statuses[i]), checkForReadOnly);
        }
        return localStatuses;
    }

    // getStatuses returns null URL for svn:externals folder.  This will
    // get the URL using svn info command on the local resource
	private String getURL(ISVNStatus status) {
		ISVNClientAdapter svnClient = null;
		String url = status.getUrlString();
		if (url == null && !(status.getTextStatus() == SVNStatusKind.UNVERSIONED)) {
		    try { 
		    	svnClient = repository.getSVNClient();
		    	ISVNInfo info = svnClient.getInfoFromWorkingCopy(status.getFile());
		    	SVNUrl svnurl = info.getUrl();
		    	url = (svnurl != null) ? svnurl.toString() : null;
		    } catch (SVNException e) {
			} catch (SVNClientException e) {
			}
		    finally {
		    	repository.returnSVNClient(svnClient);
		    }
		}
		return url;
	}

    /**
     * get the results
     * @return
     */
    public ISVNStatus[] getStatuses() {
        return svnStatuses;
    } 

    /**
     * get the results
     * @return
     */
    public LocalResourceStatus[] getLocalResourceStatuses() {
        return convert(svnStatuses);
    }    
}
