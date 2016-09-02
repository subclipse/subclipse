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
package org.tigris.subversion.subclipse.core.status;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.tigris.subversion.subclipse.core.Policy;
import org.tigris.subversion.subclipse.core.SVNException;
import org.tigris.subversion.subclipse.core.SVNProviderPlugin;
import org.tigris.subversion.svnclientadapter.ISVNClientAdapter;
import org.tigris.subversion.svnclientadapter.ISVNStatus;
import org.tigris.subversion.svnclientadapter.SVNClientException;

/**
 * A strategy that when asked to get the status of a given resource, 
 * get the status of its parent (if not present yet) and parent's children recursively
 * 
 * @author cedric chabanois (cchab at tigris.org)
 */
public class RecursiveStatusUpdateStrategy extends StatusUpdateStrategy {

	public RecursiveStatusUpdateStrategy(IStatusCache statusCache)
	{
		super(statusCache);
	}

	/* (non-Javadoc)
	 * @see org.tigris.subversion.subclipse.core.status.StatusUpdateStrategy#statusesToUpdate(org.eclipse.core.resources.IResource)
	 */
	protected ISVNStatus[] statusesToUpdate(IResource resource) throws SVNException {
        if (!(resource instanceof IProject)) {
            // if the status of the resource parent is not known, we
            // recursively update it instead 
            IContainer parent = resource.getParent();
            if (parent != null) {
                if (statusCache.getStatus(parent) == null) {
                    return statusesToUpdate(parent);
                }
            }
        }
        
        if (Policy.DEBUG_STATUS) {
            System.out.println("[svn] getting status for : " + resource.getFullPath()); //$NON-NLS-1$   
        }
        
        // don't do getRepository().getSVNClient() as we can ask the status of a file
        // that is not associated with a known repository
        // we don't need login & password so this is not a problem
        ISVNStatus[] statuses = null;
        ISVNClientAdapter svnClientAdapterStatus = null;
        try {
            SVNProviderPlugin.disableConsoleLogging(); 
            svnClientAdapterStatus = SVNProviderPlugin.getPlugin().getSVNClient();
            statuses = svnClientAdapterStatus.getStatus(resource.getLocation().toFile(),true, true);
        } catch (SVNClientException e1) {
        	if (!e1.getMessage().contains(SVNProviderPlugin.UPGRADE_NEEDED)) {
        		throw SVNException.wrapException(e1);
        	}
        } finally {
            SVNProviderPlugin.enableConsoleLogging();
            SVNProviderPlugin.getPlugin().getSVNClientManager().returnSVNClient(svnClientAdapterStatus);
        }

        return collectUnversionedFolders(statuses, true);
	}
}
