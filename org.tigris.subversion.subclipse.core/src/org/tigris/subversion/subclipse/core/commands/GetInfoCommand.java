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
package org.tigris.subversion.subclipse.core.commands;

import org.eclipse.core.runtime.IProgressMonitor;
import org.tigris.subversion.subclipse.core.ISVNLocalResource;
import org.tigris.subversion.subclipse.core.SVNException;
import org.tigris.subversion.svnclientadapter.ISVNClientAdapter;
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
    	ISVNClientAdapter svnClient = resource.getRepository().getSVNClient();
        try {
            if (monitor != null) { monitor.beginTask(null, 100); }
            info = svnClient.getInfoFromWorkingCopy(resource.getFile());
            if (monitor != null) { monitor.worked(100); }
        } catch (SVNClientException e) {
            throw SVNException.wrapException(e);
        } finally {
        	resource.getRepository().returnSVNClient(svnClient);
        	if (monitor != null) { monitor.done(); }
        }
    }
    
    public ISVNInfo getInfo() {
        return info;
    }

}
