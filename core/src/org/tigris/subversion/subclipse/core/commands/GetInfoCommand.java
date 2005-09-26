/******************************************************************************
 * This program and the accompanying materials are made available under
 * the terms of the Common Public License v1.0 which accompanies this
 * distribution, and is available at the following URL:
 * http://www.eclipse.org/legal/cpl-v10.html
 * Copyright(c) 2003-2005 by the authors indicated in the @author tags.
 *
 * All Rights are Reserved by the various authors.
 *******************************************************************************/
package org.tigris.subversion.subclipse.core.commands;

import org.eclipse.core.runtime.IProgressMonitor;
import org.tigris.subversion.subclipse.core.ISVNLocalResource;
import org.tigris.subversion.subclipse.core.SVNException;
import org.tigris.subversion.svnclientadapter.ISVNInfo;
import org.tigris.subversion.svnclientadapter.SVNClientException;

/**
 * Get the svn info for the specified resource.
 * 
 * @author Martin Letenay (letenay at tigris.org) 
 */
public class GetInfoCommand implements ISVNCommand {

    private ISVNInfo info = null;
    private ISVNLocalResource resource = null;

    public GetInfoCommand(ISVNLocalResource resource)    
    {
    	this.resource = resource;
    }
    
    /* (non-Javadoc)
     * @see org.tigris.subversion.subclipse.core.commands.ISVNCommand#run(org.eclipse.core.runtime.IProgressMonitor)
     */
    public void run(IProgressMonitor monitor) throws SVNException {
        try {
            if (monitor != null) { monitor.beginTask(null, 100); }
            info = resource.getRepository().getSVNClient().getInfoFromWorkingCopy(resource.getFile());
            if (monitor != null) { monitor.worked(100); }
        } catch (SVNClientException e) {
            throw SVNException.wrapException(e);
        } finally {
        	if (monitor != null) { monitor.done(); }
        }
    }
    
    public ISVNInfo getInfo() {
        return info;
    }

}
