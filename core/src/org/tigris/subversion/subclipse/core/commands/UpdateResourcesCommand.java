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
package org.tigris.subversion.subclipse.core.commands;

import java.io.File;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.tigris.subversion.subclipse.core.ISVNCoreConstants;
import org.tigris.subversion.subclipse.core.SVNException;
import org.tigris.subversion.subclipse.core.client.OperationManager;
import org.tigris.subversion.subclipse.core.client.OperationProgressNotifyListener;
import org.tigris.subversion.subclipse.core.resources.SVNWorkspaceRoot;
import org.tigris.subversion.svnclientadapter.ISVNClientAdapter;
import org.tigris.subversion.svnclientadapter.SVNClientException;
import org.tigris.subversion.svnclientadapter.SVNRevision;

/**
 * Update the given resources in the given project to the given revision
 * 
 * @author Cedric Chabanois (cchab at tigris.org)
 */
public class UpdateResourcesCommand implements ISVNCommand {
    private SVNWorkspaceRoot root;
    private IResource[] resources;
    private SVNRevision revision; 
    private int depth = ISVNCoreConstants.DEPTH_UNKNOWN;
    private boolean ignoreExternals = false;
    private boolean force = true;
    
    /**
     * Update the given resources.
     * BEWARE ! The resource array has to be sorted properly, so parent folder (incoming additions) are updated sooner than their children.
     * BEWARE ! For incoming deletions, it has to be opposite. 
     * WATCH OUT ! These two statements mean that you CANNOT have both additions and deletions within the same call !!!
     * When doing recursive call, it's obviously not an issue ... 
     * @param root
     * @param resources
     * @param revision
     */
    public UpdateResourcesCommand(SVNWorkspaceRoot root, IResource[] resources, SVNRevision revision) {
        this.root = root;
        this.resources = resources;
        this.revision = revision;
    }
    
	/* (non-Javadoc)
	 * @see org.tigris.subversion.subclipse.core.commands.ISVNCommand#run(org.eclipse.core.runtime.IProgressMonitor)
	 */
	public void run(final IProgressMonitor monitor) throws SVNException {
        try {
            monitor.beginTask(null, 100 * resources.length);                    
            ISVNClientAdapter svnClient = root.getRepository().getSVNClient();

            OperationManager.getInstance().beginOperation(svnClient, new OperationProgressNotifyListener(monitor));
    		if (resources.length == 1)
    		{
                monitor.subTask(resources[0].getName());
                svnClient.update(resources[0].getLocation().toFile(),revision, depth, ignoreExternals, force);
                monitor.worked(100);    			
    		}
    		else
    		{
    			File[] files = new File[resources.length];
    			for (int i = 0; i < resources.length; i++) {
					files[i] = resources[i].getLocation().toFile();
				}
   				svnClient.update(files, revision, depth, ignoreExternals, force);
   				monitor.worked(100);
    		}
        } catch (SVNClientException e) {
            throw SVNException.wrapException(e);
        } finally {
            OperationManager.getInstance().endOperation();
            monitor.done();
        }        
	}

	public void setDepth(int depth) {
		this.depth = depth;
	}

	public void setIgnoreExternals(boolean ignoreExternals) {
		this.ignoreExternals = ignoreExternals;
	}

	public void setForce(boolean force) {
		this.force = force;
	}    
    
}
