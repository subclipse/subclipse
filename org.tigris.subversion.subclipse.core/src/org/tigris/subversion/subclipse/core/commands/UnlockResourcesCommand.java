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

import java.io.File;
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
 * Unlock resources
 * 
 */
public class UnlockResourcesCommand implements ISVNCommand {
	// resources to unlock
    private IResource[] resources;
    // break the lock of another owner
    private boolean force;
    
    private SVNWorkspaceRoot root;
    
    private OperationResourceCollector operationResourceCollector = new OperationResourceCollector();

    public UnlockResourcesCommand(SVNWorkspaceRoot root, IResource[] resources, boolean force) {
    	this.resources = resources;
        this.force = force;
        this.root = root;
    }
    
	/* (non-Javadoc)
	 * @see org.tigris.subversion.subclipse.core.commands.ISVNCommand#run(org.eclipse.core.runtime.IProgressMonitor)
	 */
	public void run(IProgressMonitor monitor) throws SVNException {
        final ISVNClientAdapter svnClient = root.getRepository().getSVNClient();
        
        final File[] resourceFiles = new File[resources.length];
        for (int i = 0; i < resources.length;i++)
            resourceFiles[i] = resources[i].getLocation().toFile(); 
        
        try {
            monitor.beginTask(null, 100);
            
            svnClient.addNotifyListener(operationResourceCollector);
            
            OperationManager.getInstance().beginOperation(svnClient);

            svnClient.unlock(resourceFiles, force);
        } catch (SVNClientException e) {
            throw SVNException.wrapException(e);
        } finally {
        	Set<IResource> operationResources = operationResourceCollector.getOperationResources();
            OperationManager.getInstance().endOperation(true, operationResources);
            svnClient.removeNotifyListener(operationResourceCollector);
            root.getRepository().returnSVNClient(svnClient);
            monitor.done();
        }
 	}
    
}
