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
package org.tigris.subversion.subclipse.ui.compare;


import java.io.File;

import org.eclipse.compare.CompareConfiguration;
import org.eclipse.compare.CompareEditorInput;
import org.eclipse.compare.ITypedElement;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.team.ui.ISaveableWorkbenchPart;
import org.eclipse.ui.IPropertyListener;
import org.eclipse.ui.IWorkbenchPartSite;
import org.tigris.subversion.subclipse.core.ISVNLocalResource;
import org.tigris.subversion.subclipse.core.ISVNRemoteResource;
import org.tigris.subversion.subclipse.core.SVNException;
import org.tigris.subversion.subclipse.core.commands.GetRemoteResourceCommand;
import org.tigris.subversion.subclipse.core.resources.LocalResourceStatus;
import org.tigris.subversion.subclipse.ui.Policy;
import org.tigris.subversion.subclipse.ui.SVNUIPlugin;
import org.tigris.subversion.subclipse.ui.compare.internal.Utilities;
import org.tigris.subversion.subclipse.ui.internal.Utils;
import org.tigris.subversion.subclipse.ui.operations.ShowDifferencesAsUnifiedDiffOperationWC;
import org.tigris.subversion.svnclientadapter.SVNRevision;
import org.tigris.subversion.svnclientadapter.SVNUrl;

/**
 * A compare input for comparing local resource with its remote revision
 * Perform textual check on:
 * - any local modification
 * - revision numbers don't match
 */
public class SVNLocalCompareInput extends CompareEditorInput implements ISaveableWorkbenchPart {
    private final SVNRevision remoteRevision;
	private ISVNLocalResource resource;
	private ISVNRemoteResource remoteResource; // the remote resource to compare to or null if it does not exist
	private boolean readOnly;
	private File diffFile;
	private ShowDifferencesAsUnifiedDiffOperationWC diffOperation;
	
	/**
	 * @throws SVNException
	 * creates a SVNLocalCompareInput, allows setting whether the current local resource is read only or not.
	 */
	public SVNLocalCompareInput(ISVNLocalResource resource, SVNRevision revision, boolean readOnly) throws SVNException {
		super(new CompareConfiguration());
        this.remoteRevision = revision;
        this.readOnly = readOnly;
        this.resource = resource;
        
        LocalResourceStatus status = resource.getStatus();
        if (status != null && status.isCopied()) {
        	SVNUrl copiedFromUrl = status.getUrlCopiedFrom();
        	if (copiedFromUrl != null) {
        		GetRemoteResourceCommand getRemoteResourceCommand = new GetRemoteResourceCommand(resource.getRepository(), copiedFromUrl, SVNRevision.HEAD);
        		getRemoteResourceCommand.run(null);
        		this.remoteResource = getRemoteResourceCommand.getRemoteResource();
        	}
        }
        
		// SVNRevision can be any valid revision : BASE, HEAD, number ...
		if (this.remoteResource == null) this.remoteResource = resource.getRemoteResource(revision);

        // remoteResouce can be null if there is no corresponding remote resource
        // (for example no base because resource has just been added)
	}

	/**
	 * Constructor which allows 
	 * @throws SVNException
	 * creates a SVNLocalCompareInput, defaultin to read/write.  
	 */
	public SVNLocalCompareInput(ISVNLocalResource resource, SVNRevision revision) throws SVNException {
		this(resource, revision, false);
	}

	/**
	 * @throws SVNException
	 * creates a SVNCompareRevisionsInput  
	 */
	public SVNLocalCompareInput(ISVNLocalResource resource, ISVNRemoteResource remoteResource) throws SVNException {
		super(new CompareConfiguration());
		this.resource = resource;
		this.remoteResource = remoteResource;
        this.remoteRevision = remoteResource.getRevision();
	}
	
	
	/**
	 * initialize the labels : the title, the lft label and the right one
	 */
	private void initLabels() {
		CompareConfiguration cc = getCompareConfiguration();
		String resourceName = resource.getName();	
		setTitle(Policy.bind("SVNCompareRevisionsInput.compareResourceAndVersions", new Object[] {resourceName})); //$NON-NLS-1$
		cc.setLeftEditable(! readOnly);
		cc.setRightEditable(false);
		
		String leftLabel = Policy.bind("SVNCompareRevisionsInput.workspace", new Object[] {resourceName}); //$NON-NLS-1$
		cc.setLeftLabel(leftLabel);
		String remoteResourceName = null;
		if (remoteResource != null) {
			remoteResourceName = remoteResource.getName();
		} else {
			remoteResourceName = resourceName;
		}
		String rightLabel = Policy.bind("SVNCompareRevisionsInput.repository", new Object[] {remoteResourceName}); //$NON-NLS-1$
		cc.setRightLabel(rightLabel);
	}
	
	/**
	 * Runs the compare operation and returns the compare result.
	 */
	protected Object prepareInput(IProgressMonitor monitor){
		
		if (diffOperation != null) {
			try {
				diffOperation.run(monitor);
				diffFile = diffOperation.getFile();
			} catch (Exception e) {}
			if (diffOperation.isCanceled() || monitor.isCanceled()) {
				return null;
			}
		}
		
		initLabels();
		ITypedElement left = new SVNLocalResourceNode(resource);
		ResourceEditionNode right = new ResourceEditionNode(remoteResource);
		if(left.getType()==ITypedElement.FOLDER_TYPE){
			right.setLocalResource((SVNLocalResourceNode) left);
		}
		if(right.getType()==ITypedElement.FOLDER_TYPE){
			((SVNLocalResourceNode)left).setRemoteResource((ResourceEditionNode) right);
		}


		String localCharset = Utilities.getCharset(resource.getIResource());
		try {
			right.setCharset(localCharset);
		} catch (CoreException e) {
			SVNUIPlugin.log(IStatus.ERROR, e.getMessage(), e);
		}

        if (SVNRevision.BASE.equals(remoteRevision)) {
            return new StatusAwareDifferencer().findDifferences(false, monitor,null,null,left,right);
        }
        return new RevisionAwareDifferencer((SVNLocalResourceNode)left,right, diffFile).findDifferences(false, monitor,null,null,left,right);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.ISaveablePart#doSave(org.eclipse.core.runtime.IProgressMonitor)
	 */
	public void doSave(IProgressMonitor monitor) {
		try {
			saveChanges(monitor);
		} catch (CoreException e) {
			Utils.handle(e);
		}
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.ISaveablePart#doSaveAs()
	 */
	public void doSaveAs() {
		// noop
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.ISaveablePart#isDirty()
	 */
	public boolean isDirty() {
		return isSaveNeeded();
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.ISaveablePart#isSaveAsAllowed()
	 */
	public boolean isSaveAsAllowed() {
		return false;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.ISaveablePart#isSaveOnCloseNeeded()
	 */
	public boolean isSaveOnCloseNeeded() {
		return true;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.IWorkbenchPart#addPropertyListener(org.eclipse.ui.IPropertyListener)
	 */
	public void addPropertyListener(IPropertyListener listener) {
		
	}
	
	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.IWorkbenchPart#createPartControl(org.eclipse.swt.widgets.Composite)
	 */
	public void createPartControl(Composite parent) {
		createContents(parent);
	}
	
	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.IWorkbenchPart#dispose()
	 */
	public void dispose() {
	}
	
	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.IWorkbenchPart#getSite()
	 */
	public IWorkbenchPartSite getSite() {
		return null;
	}
	
	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.IWorkbenchPart#getTitleToolTip()
	 */
	public String getTitleToolTip() {
		return null;
	}
	
	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.IWorkbenchPart#removePropertyListener(org.eclipse.ui.IPropertyListener)
	 */
	public void removePropertyListener(IPropertyListener listener) {
	}

	public void setDiffOperation(
			ShowDifferencesAsUnifiedDiffOperationWC diffOperation) {
		this.diffOperation = diffOperation;
	}

	public void setDiffFile(File diffFile) {
		this.diffFile = diffFile;
	}

	public boolean canRunAsJob() {
		return true;
	}
    
}
