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
import java.util.LinkedHashSet;
import java.util.Set;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.tigris.subversion.subclipse.core.ISVNCoreConstants;
import org.tigris.subversion.subclipse.core.Policy;
import org.tigris.subversion.subclipse.core.SVNException;
import org.tigris.subversion.subclipse.core.client.OperationManager;
import org.tigris.subversion.subclipse.core.client.OperationProgressNotifyListener;
import org.tigris.subversion.subclipse.core.client.OperationResourceCollector;
import org.tigris.subversion.subclipse.core.resources.SVNWorkspaceRoot;
import org.tigris.subversion.svnclientadapter.ISVNClientAdapter;
import org.tigris.subversion.svnclientadapter.SVNClientException;
import org.tigris.subversion.svnclientadapter.SVNNodeKind;
import org.tigris.subversion.svnclientadapter.SVNRevision;
import org.tigris.subversion.svnclientadapter.SVNUrl;

/**
 * Switch URL for selected resource
 */
public class SwitchToUrlCommand implements ISVNCommand {
	// resource to switch
    private IResource resource;  
    
    private SVNUrl svnUrl;
    
    private SVNRevision svnRevision;
    
    private SVNWorkspaceRoot root;
    
    private int depth = ISVNCoreConstants.DEPTH_UNKNOWN;
    private boolean setDepth = false;
    private boolean ignoreExternals = false;
    private boolean force = true;
    
    private OperationResourceCollector operationResourceCollector = new OperationResourceCollector();

    public SwitchToUrlCommand(SVNWorkspaceRoot root, IResource resource, SVNUrl svnUrl, SVNRevision svnRevision) {
        super();
        this.root = root;
        this.resource = resource;
        this.svnUrl = svnUrl;
        this.svnRevision = svnRevision;
    }

	/* (non-Javadoc)
	 * @see org.tigris.subversion.subclipse.core.commands.ISVNCommand#run(org.eclipse.core.runtime.IProgressMonitor)
	 */
	public void run(IProgressMonitor monitor) throws SVNException {    
		final IProgressMonitor subPm = Policy.infiniteSubMonitorFor(monitor, 100);
        try {
    		subPm.beginTask(null, Policy.INFINITE_PM_GUESS_FOR_SWITCH);
            ISVNClientAdapter svnClient = root.getRepository().getSVNClient();
            svnClient.addNotifyListener(operationResourceCollector);
            OperationManager.getInstance().beginOperation(svnClient, new OperationProgressNotifyListener(subPm, svnClient));
            File file = resource.getLocation().toFile();
            SVNRevision pegRevision = null;
//            ISVNLocalResource svnResource = SVNWorkspaceRoot.getSVNResourceFor(resource);
//            if (svnResource != null && svnResource.getRevision() != null) pegRevision = svnResource.getRevision();
//            else pegRevision = svnRevision;
            pegRevision = SVNRevision.HEAD;
            svnClient.switchToUrl(file, svnUrl, svnRevision, pegRevision, depth, setDepth, ignoreExternals, force);
            OperationManager.getInstance().onNotify(resource.getLocation().toFile(), SVNNodeKind.UNKNOWN);
        } catch (SVNClientException e) {
            throw SVNException.wrapException(e);
        } finally {
        	Set<IResource> operationResources = operationResourceCollector.getOperationResources();
        	if (operationResources.size() == 0) {
        		operationResources.add(resource);
        	}
            OperationManager.getInstance().endOperation(true, operationResources);
            subPm.done();
        }
	}

	public void setDepth(int depth) {
		this.depth = depth;
	}
	
	public void setSetDepth(boolean setDepth) {
		this.setDepth = setDepth;
	}

	public void setIgnoreExternals(boolean ignoreExternals) {
		this.ignoreExternals = ignoreExternals;
	}

	public void setForce(boolean force) {
		this.force = force;
	}

}
