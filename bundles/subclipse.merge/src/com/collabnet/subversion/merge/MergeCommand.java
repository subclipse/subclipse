/*******************************************************************************
 * Copyright (c) 2009 CollabNet.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     CollabNet - initial API and implementation
 ******************************************************************************/
package com.collabnet.subversion.merge;

import java.io.File;
import java.util.Set;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.ui.IWorkbenchPart;
import org.tigris.subversion.subclipse.core.ISVNCoreConstants;
import org.tigris.subversion.subclipse.core.ISVNLocalResource;
import org.tigris.subversion.subclipse.core.ISVNRepositoryLocation;
import org.tigris.subversion.subclipse.core.SVNException;
import org.tigris.subversion.subclipse.core.client.OperationManager;
import org.tigris.subversion.subclipse.core.client.OperationProgressNotifyListener;
import org.tigris.subversion.subclipse.core.client.OperationResourceCollector;
import org.tigris.subversion.subclipse.core.commands.ISVNCommand;
import org.tigris.subversion.subclipse.core.resources.SVNWorkspaceRoot;
import org.tigris.subversion.subclipse.core.util.SVNUrlWithPegRevision;
import org.tigris.subversion.svnclientadapter.ISVNClientAdapter;
import org.tigris.subversion.svnclientadapter.SVNClientException;
import org.tigris.subversion.svnclientadapter.SVNRevision;
import org.tigris.subversion.svnclientadapter.SVNRevisionRange;
import org.tigris.subversion.svnclientadapter.SVNUrl;

public class MergeCommand implements ISVNCommand {
	// selected resource
    private IResource resource;  
    
    private SVNUrl svnUrl1;
    private SVNUrl svnUrl2;
    
    private SVNRevision svnRevision1;
    private SVNRevision svnRevision2;
    
    private SVNRevisionRange[] revisions;

    private boolean force = false;
    private boolean ignoreAncestry = false;
    private int depth = ISVNCoreConstants.DEPTH_INFINITY;
    private boolean recordOnly = false;
    
    private int textConflictHandling;
    private int binaryConflictHandling;    
    private int propertyConflictHandling;
    private int treeConflictHandling;

	private MergeOutput mergeOutput;
    
    private IWorkbenchPart part;
    
    private boolean mergeAborted;
    private String errorMessage;
    
    private OperationResourceCollector operationResourceCollector = new OperationResourceCollector();
 
    public MergeCommand(IResource resource, SVNUrl svnUrl1, SVNRevision svnRevision1, SVNUrl svnUrl2, SVNRevision svnRevision2, SVNRevisionRange[] revisions, MergeOutput mergeOutput) {    	
        super();
        this.resource = resource;
        this.svnUrl1 = svnUrl1;
        this.svnRevision1 = svnRevision1;
        this.svnUrl2 = svnUrl2;
        this.svnRevision2 = svnRevision2;
        this.revisions = revisions;
        this.mergeOutput = mergeOutput;
    }

    public void run(IProgressMonitor monitor) throws SVNException {
    	mergeAborted = false;
    	MergeListener mergeListener = null;
    	ISVNClientAdapter svnClient = null;
    	ISVNRepositoryLocation repository = null;
        try {
            monitor.beginTask(null, 100);
            ISVNLocalResource svnResource = SVNWorkspaceRoot.getSVNResourceFor(resource);
            repository = svnResource.getRepository();
            svnClient = repository.getSVNClient();
            SVNConflictResolver conflictResolver = new SVNConflictResolver(resource, textConflictHandling, binaryConflictHandling, propertyConflictHandling, treeConflictHandling);
            conflictResolver.setPart(part);
            svnClient.addConflictResolutionCallback(conflictResolver);
            MergeOptions mergeOptions = new MergeOptions();
            mergeOptions.setFromUrl(svnUrl1);
            mergeOptions.setFromRevision(svnRevision1);
            mergeOptions.setToUrl(svnUrl2);
            mergeOptions.setToRevision(svnRevision2);
            mergeOptions.setRevisions(revisions);
            mergeOptions.setForce(force);
            mergeOptions.setIgnoreAncestry(ignoreAncestry);
            mergeOptions.setDepth(depth);
            mergeListener = new MergeListener(resource, mergeOptions, conflictResolver, mergeOutput);
            svnClient.addNotifyListener(mergeListener);
            svnClient.addNotifyListener(operationResourceCollector);
            OperationManager.getInstance().beginOperation(svnClient, new OperationProgressNotifyListener(monitor));
            monitor.subTask(resource.getName());
            File file = resource.getLocation().toFile();
            if (revisions == null) {
            	svnClient.merge(svnUrl1, svnRevision1, svnUrl2, svnRevision2, file, force, depth, false, ignoreAncestry, recordOnly);
            } else {
            	SVNUrlWithPegRevision svnUrlWithPegRevision = new SVNUrlWithPegRevision(svnUrl1);
            	SVNRevision pegRevision = svnUrlWithPegRevision.getPegRevision();
            	if (pegRevision == null) pegRevision = SVNRevision.HEAD;
            	svnClient.merge(svnUrlWithPegRevision.getUrl(), pegRevision, revisions, file, force, depth, ignoreAncestry, false, recordOnly);
            }
            monitor.worked(100);            
            
        } catch (SVNClientException e) {
        	if (e.getAprError() == SVNClientException.MERGE_CONFLICT) {
        		mergeAborted = true;
        		errorMessage = e.getCause().getLocalizedMessage().replaceAll("\n", " "); //$NON-NLS-1$ //$NON-NLS-2$
        	}
        	else throw SVNException.wrapException(e);
        } finally {
        	if (mergeListener != null) mergeOutput = mergeListener.getMergeOutput();
        	Set<IResource> operationResources = operationResourceCollector.getOperationResources();
        	OperationManager.getInstance().endOperation(true, operationResources);
            monitor.done();
            svnClient.removeNotifyListener(mergeListener);
            svnClient.removeNotifyListener(operationResourceCollector);
            svnClient.addConflictResolutionCallback(null);  
            if (repository != null) {
            	repository.returnSVNClient(svnClient);
            }
        }        
    }  
    
	public void setForce(boolean force) {
		this.force = force;
	}

	public void setIgnoreAncestry(boolean ignoreAncestry) {
		this.ignoreAncestry = ignoreAncestry;
	}
	
	public void setDepth(int depth) {
		this.depth = depth;
	}

	public MergeOutput getMergeOutput() {
		return mergeOutput;
	}
	
	public void setTextConflictHandling(int textConflictHandling) {
		this.textConflictHandling = textConflictHandling;
	}

	public void setBinaryConflictHandling(int binaryConflictHandling) {
		this.binaryConflictHandling = binaryConflictHandling;
	}
	
    public void setPropertyConflictHandling(int propertyConflictHandling) {
		this.propertyConflictHandling = propertyConflictHandling;
	}
    
    public void setTreeConflictHandling(int treeConflictHandling) {
		this.treeConflictHandling = treeConflictHandling;
	}

	public void setPart(IWorkbenchPart part) {
		this.part = part;
	}

	public boolean isMergeAborted() {
		return mergeAborted;
	}

	public String getErrorMessage() {
		return errorMessage;
	}

	public void setRecordOnly(boolean recordOnly) {
		this.recordOnly = recordOnly;
	}	

}
