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

import java.util.Set;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.tigris.subversion.subclipse.core.SVNException;
import org.tigris.subversion.subclipse.core.client.OperationManager;
import org.tigris.subversion.subclipse.core.client.OperationResourceCollector;
import org.tigris.subversion.subclipse.core.resources.SVNWorkspaceRoot;
import org.tigris.subversion.svnclientadapter.ISVNClientAdapter;
import org.tigris.subversion.svnclientadapter.SVNClientException;

/**
 * Mark a conflicted resource as being resolved.  This will also remove the .mine and .r* files
 */
public class ResolveResourcesCommand implements ISVNCommand {

    private final SVNWorkspaceRoot root;
    private final IResource[] resources;
    private final int resolution;
    
    private OperationResourceCollector operationResourceCollector = new OperationResourceCollector();

    public ResolveResourcesCommand(SVNWorkspaceRoot root, IResource[] resources, int resolution) {
        this.root = root;
        this.resources = resources;
        this.resolution = resolution;
        
    }
    
    /* (non-Javadoc)
     * @see org.tigris.subversion.subclipse.core.commands.ISVNCommand#run(org.eclipse.core.runtime.IProgressMonitor)
     */
    public void run(IProgressMonitor monitor) throws SVNException {
    	ISVNClientAdapter svnClient = root.getRepository().getSVNClient();
        try {            
            svnClient.addNotifyListener(operationResourceCollector);           
            OperationManager.getInstance().beginOperation(svnClient);           
            for (int i = 0; i < resources.length; i++) {
                svnClient.resolve(resources[i].getLocation().toFile(), resolution);
                monitor.worked(100);
            }
        } catch (SVNClientException e) {
            throw SVNException.wrapException(e);
        } finally {
        	Set<IResource> operationResources = operationResourceCollector.getOperationResources();
            OperationManager.getInstance().endOperation(true, operationResources);
            if (svnClient != null) {
	            svnClient.removeNotifyListener(operationResourceCollector);
	            root.getRepository().returnSVNClient(svnClient);
            }
            monitor.done();
        }
    }
}
