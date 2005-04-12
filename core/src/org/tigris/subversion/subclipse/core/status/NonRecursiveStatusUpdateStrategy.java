/******************************************************************************
 * This program and the accompanying materials are made available under
 * the terms of the Common Public License v1.0 which accompanies this
 * distribution, and is available at the following URL:
 * http://www.eclipse.org/legal/cpl-v10.html
 * Copyright(c) 2003-2005 by the authors indicated in the @author tags.
 *
 * All Rights are Reserved by the various authors.
 *******************************************************************************/
package org.tigris.subversion.subclipse.core.status;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.tigris.subversion.subclipse.core.Policy;
import org.tigris.subversion.subclipse.core.SVNException;
import org.tigris.subversion.subclipse.core.SVNProviderPlugin;
import org.tigris.subversion.svnclientadapter.ISVNClientAdapter;
import org.tigris.subversion.svnclientadapter.ISVNStatus;
import org.tigris.subversion.svnclientadapter.SVNClientException;

/**
 * A strategy that get the status of parent and all the direct children of the asked resource
 * 
 * @author cedric chabanois (cchab at tigris.org) 
 */
public class NonRecursiveStatusUpdateStrategy extends StatusUpdateStrategy {

	/* (non-Javadoc)
	 * @see org.tigris.subversion.subclipse.core.status.StatusUpdateStrategy#updateStatus(org.eclipse.core.resources.IResource)
	 */
	void updateStatus(IResource resource) throws SVNException {
        // we update the parent and its immediate children 
        IResource resourceToUpdate = resource;
        if (!(resource instanceof IProject)) {
            resourceToUpdate = resource.getParent();
        }
        
        if (Policy.DEBUG_STATUS) {
            System.out.println("[svn] getting status for : " + resourceToUpdate.getFullPath()); //$NON-NLS-1$   
        }
        
        // don't do getRepository().getSVNClient() as we can ask the status of a file
        // that is not associated with a known repository
        // we don't need login & password so this is not a problem
        ISVNStatus[] statuses = null;
        try {
            ISVNClientAdapter svnClientAdapterStatus = SVNProviderPlugin.getPlugin().createSVNClient();
            statuses = svnClientAdapterStatus.getStatus(
                    resourceToUpdate.getLocation().toFile(),
                    false, // do only immediate children. 
                    true); // retrieve all entries
        } catch (SVNClientException e1) {
            throw SVNException.wrapException(e1);
        }
        updateCache(statuses);

	}

}
