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
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.compare.CompareConfiguration;
import org.eclipse.compare.CompareEditorInput;
import org.eclipse.compare.ITypedElement;
import org.eclipse.compare.internal.BufferedResourceNode;
import org.eclipse.compare.structuremergeviewer.DiffNode;
import org.eclipse.compare.structuremergeviewer.IDiffElement;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.team.ui.ISaveableWorkbenchPart;
import org.eclipse.ui.IPropertyListener;
import org.eclipse.ui.IWorkbenchPartSite;
import org.tigris.subversion.subclipse.core.ISVNLocalResource;
import org.tigris.subversion.subclipse.core.ISVNRemoteFolder;
import org.tigris.subversion.subclipse.core.ISVNRemoteResource;
import org.tigris.subversion.subclipse.core.SVNException;
import org.tigris.subversion.subclipse.core.SVNProviderPlugin;
import org.tigris.subversion.subclipse.core.commands.AddResourcesCommand;
import org.tigris.subversion.subclipse.core.commands.GetRemoteResourceCommand;
import org.tigris.subversion.subclipse.core.resources.LocalFolder;
import org.tigris.subversion.subclipse.core.resources.LocalResourceStatus;
import org.tigris.subversion.subclipse.core.resources.RemoteFolder;
import org.tigris.subversion.subclipse.core.resources.SVNWorkspaceRoot;
import org.tigris.subversion.subclipse.ui.Policy;
import org.tigris.subversion.subclipse.ui.SVNUIPlugin;
import org.tigris.subversion.subclipse.ui.compare.internal.Utilities;
import org.tigris.subversion.subclipse.ui.internal.Utils;
import org.tigris.subversion.subclipse.ui.operations.ShowDifferencesAsUnifiedDiffOperationWC;
import org.tigris.subversion.svnclientadapter.ISVNClientAdapter;
import org.tigris.subversion.svnclientadapter.ISVNInfo;
import org.tigris.subversion.svnclientadapter.ISVNStatus;
import org.tigris.subversion.svnclientadapter.SVNClientException;
import org.tigris.subversion.svnclientadapter.SVNDiffSummary;
import org.tigris.subversion.svnclientadapter.SVNDiffSummary.SVNDiffKind;
import org.tigris.subversion.svnclientadapter.SVNNodeKind;
import org.tigris.subversion.svnclientadapter.SVNRevision;
import org.tigris.subversion.svnclientadapter.SVNUrl;
import org.tigris.subversion.svnclientadapter.utils.SVNStatusUtils;

/**
 * A compare input for comparing local resource with its remote revision
 * Perform textual check on:
 * - any local modification
 * - revision numbers don't match
 */
public class SVNLocalCompareInput extends CompareEditorInput implements ISaveableWorkbenchPart {
	private Object fRoot;
    private final SVNRevision remoteRevision;
	private ISVNLocalResource resource;
	private ISVNRemoteResource remoteResource; // the remote resource to compare to or null if it does not exist
	private SVNRevision pegRevision;
	private boolean readOnly;
	private File diffFile;
	private ShowDifferencesAsUnifiedDiffOperationWC diffOperation;
	
	private ISVNLocalResource[] resources;
	private ISVNRemoteFolder[] remoteFolders;
	
	private List<IResource> unaddedList = new ArrayList<IResource>();
	private List<IResource> unversionedFolders = new ArrayList<IResource>();
	
	/**
	 * @throws SVNException
	 * creates a SVNLocalCompareInput, allows setting whether the current local resource is read only or not.
	 */
	public SVNLocalCompareInput(ISVNLocalResource resource, SVNRevision revision, boolean readOnly) throws SVNException, SVNClientException {
		super(new CompareConfiguration());
        this.remoteRevision = revision;
        this.readOnly = readOnly;
        this.resource = resource;
        
        LocalResourceStatus status = resource.getStatus();
        if (status != null && status.isCopied()) {
        	ISVNClientAdapter svnClient = null;
        	try {
	        	svnClient = resource.getRepository().getSVNClient();
	        	ISVNInfo info = svnClient.getInfoFromWorkingCopy(resource.getFile());
	        	SVNUrl copiedFromUrl = info.getCopyUrl();
	        	if (copiedFromUrl != null) {
	        		GetRemoteResourceCommand getRemoteResourceCommand = new GetRemoteResourceCommand(resource.getRepository(), copiedFromUrl, SVNRevision.HEAD);
	        		getRemoteResourceCommand.run(null);
	        		this.remoteResource = getRemoteResourceCommand.getRemoteResource();
	        	}
        	}
        	finally {
        		resource.getRepository().returnSVNClient(svnClient);
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
	public SVNLocalCompareInput(ISVNLocalResource resource, SVNRevision revision) throws SVNException, SVNClientException {
		this(resource, revision, false);
	}
	
	public SVNLocalCompareInput(ISVNLocalResource resource, ISVNRemoteResource remoteResource) throws SVNException {
		this(resource, remoteResource, null);
	}

	/**
	 * @throws SVNException
	 * creates a SVNCompareRevisionsInput  
	 */
	public SVNLocalCompareInput(ISVNLocalResource resource, ISVNRemoteResource remoteResource, SVNRevision pegRevision) throws SVNException {
		super(new CompareConfiguration());
		this.resource = resource;
		this.remoteResource = remoteResource;
        this.remoteRevision = remoteResource.getRevision();
        this.pegRevision = pegRevision;
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
	protected Object prepareInput(IProgressMonitor monitor) throws InterruptedException {

		initLabels();
	
		if (resource instanceof LocalFolder) {	
			try {	
				if (monitor == null) {
					monitor = new NullProgressMonitor();
				}
				monitor.beginTask(Policy.bind("SVNCompareEditorInput.comparing"), 30); //$NON-NLS-1$
				IProgressMonitor sub = new SubProgressMonitor(monitor, 30);
				sub.beginTask(Policy.bind("SVNCompareEditorInput.comparing"), 100); //$NON-NLS-1$
				Object[] result = new Object[] { null };
				ArrayList resourceSummaryNodeList = new ArrayList();
				ArrayList summaryEditionNodeList = new ArrayList();
				ISVNClientAdapter client = null;
				if (resources == null) {
					resources = new ISVNLocalResource[] { resource };
				}
				if (remoteFolders == null) {
					remoteFolders = new ISVNRemoteFolder[] { (ISVNRemoteFolder)remoteResource };
				}
				try {
					for (int i = 0; i < resources.length; i++) {
						ISVNLocalResource resource = resources[i];
						ISVNRemoteFolder remoteFolder = remoteFolders[i];
						SVNDiffSummary[] diffSummary = null;
						client = SVNProviderPlugin.getPlugin().getSVNClientManager().getSVNClient();
						
						File file = new File(resource.getResource().getLocation().toString());
						getUnadded(client, resource, file);
						IResource[] unaddedResources = new IResource[unaddedList.size()];
						unaddedList.toArray(unaddedResources);
						SVNWorkspaceRoot workspaceRoot = new SVNWorkspaceRoot(resource.getResource().getProject());
						AddResourcesCommand command = new AddResourcesCommand(workspaceRoot, unaddedResources, IResource.DEPTH_INFINITE); 
				        command.run(monitor);
						
						diffSummary = client.diffSummarize(file, remoteFolder.getUrl(), remoteFolder.getRevision(), true);
						
						for (IResource unaddedResource : unaddedResources) {
							try {
								SVNWorkspaceRoot.getSVNResourceFor(unaddedResource).revert();
							} catch (Exception e) {}
						}
						
						SVNProviderPlugin.getPlugin().getSVNClientManager().returnSVNClient(client);
						client = null;
						if (diffSummary != null && diffSummary.length > 0) {
							diffSummary = getDiffSummaryWithSubfolders(diffSummary);
							ITypedElement left = new SVNLocalResourceSummaryNode(resource, diffSummary, resource.getResource().getLocation().toString());
							SummaryEditionNode right = new SummaryEditionNode(remoteFolder);
							right.setName(resource.getFile().getName());
							right.setRootFolder((RemoteFolder)remoteFolder);
							right.setNodeType(SummaryEditionNode.RIGHT);
							right.setRoot(true);		
							right.setDiffSummary(diffSummary);	
							String localCharset = Utilities.getCharset(resource.getIResource());
							try {
								right.setCharset(localCharset);
							} catch (CoreException e) {
								SVNUIPlugin.log(IStatus.ERROR, e.getMessage(), e);
							}	
							resourceSummaryNodeList.add(left);
							summaryEditionNodeList.add(right);
						}
					}
					if (resourceSummaryNodeList.size() == 0) {
						result[0] = null;
					} else {
						Object[] resourceSummaryNodes = new Object[resourceSummaryNodeList.size()];
						resourceSummaryNodeList.toArray(resourceSummaryNodes);
						Object[] summaryEditionNodes = new Object[summaryEditionNodeList.size()];
						summaryEditionNodeList.toArray(summaryEditionNodes);
						MultipleSelectionNode left = new MultipleSelectionNode(resourceSummaryNodes);
						MultipleSelectionNode right = new MultipleSelectionNode(summaryEditionNodes);
				        result[0] = new SummaryDifferencer().findDifferences(false, monitor, null, null, left, right);
				        fRoot = result[0];
					}
				} finally {
					sub.done();
					if (client != null) {
						SVNProviderPlugin.getPlugin().getSVNClientManager().returnSVNClient(client);
					}
				}
				if (result[0] instanceof DiffNode) {
			       	DiffNode diffNode = (DiffNode)result[0];
		        	if (!diffNode.hasChildren()) {
		        		return null;
		        	}				
				}
		        return result[0];
			} catch (OperationCanceledException e) {
				throw new InterruptedException(e.getMessage());
			} catch (Exception e) {
				return e.getMessage();
			} finally {
				monitor.done();
			}
		}
		else {		
			ITypedElement left = new SVNLocalResourceNode(resource);
			ResourceEditionNode right = new ResourceEditionNode(remoteResource, pegRevision);
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
	        	fRoot = new StatusAwareDifferencer().findDifferences(false, monitor,null,null,left,right);
	            return fRoot;
	        }
	        fRoot = new RevisionAwareDifferencer((SVNLocalResourceNode)left,right, diffFile, pegRevision).findDifferences(false, monitor,null,null,left,right);
	        return fRoot;
		}
	}

	private void getUnadded(ISVNClientAdapter client, ISVNLocalResource resource, File file) throws SVNClientException, SVNException {
		ISVNStatus[] statuses = client.getStatus(file, true, true);
		for (ISVNStatus status : statuses) {
			IResource currentResource = SVNWorkspaceRoot.getResourceFor(resource.getResource(), status);
			if (currentResource != null) {
				ISVNLocalResource localResource = SVNWorkspaceRoot.getSVNResourceFor(currentResource);
				if (!localResource.isIgnored()) {
					if (!SVNStatusUtils.isManaged(status)) {
						if (!isSymLink(currentResource)) {
		                 	if (currentResource.getType() != IResource.FILE) {
		                 		unversionedFolders.add(currentResource);
							} else {
		             			if (addToUnadded(currentResource)) unaddedList.add(currentResource);
							}
						}
					}
				}
			}
		}
		IResource[] unaddedResources = getUnaddedResources(unversionedFolders);
		for (IResource unaddedResource : unaddedResources) {
			unaddedList.add(unaddedResource);
		}
	}
	
	private SVNDiffSummary[] getDiffSummaryWithSubfolders(SVNDiffSummary[] diffSummary) {
		ArrayList paths = new ArrayList();
		ArrayList diffs = new ArrayList();
		for (int i = 0; i < diffSummary.length; i++) {
			paths.add(diffSummary[i].getPath());
			diffs.add(diffSummary[i]);
		}
		for (int i = 0; i < diffSummary.length; i++) {
			File file = new File(diffSummary[i].getPath());
			while (file.getParentFile() != null) {
				file = file.getParentFile();
				String path = file.getPath();
				path = path.replaceAll("\\\\", "/"); //$NON-NLS-1$ //$NON-NLS-2$
				if (!paths.contains(path)) {
					paths.add(path);
					SVNDiffSummary folder = new SVNDiffSummary(path, SVNDiffKind.NORMAL, false, SVNNodeKind.DIR.toInt());
					diffs.add(folder);
				}
			}
		}
		diffSummary = new SVNDiffSummary[diffs.size()];
		diffs.toArray(diffSummary);
		return diffSummary;
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
	
	public void saveChanges(IProgressMonitor pm) throws CoreException {
		super.saveChanges(pm);
		if (fRoot instanceof DiffNode) {
			try {
				commit(pm, (DiffNode) fRoot);
			} finally {		
				setDirty(false);
			}
		}
	}
	
	private static void commit(IProgressMonitor pm, DiffNode node) throws CoreException {
		ITypedElement left= node.getLeft();
		if (left instanceof BufferedResourceNode)
			((BufferedResourceNode) left).commit(pm);
			
		ITypedElement right= node.getRight();
		if (right instanceof BufferedResourceNode)
			((BufferedResourceNode) right).commit(pm);

		IDiffElement[] children= node.getChildren();
		if (children != null) {
			for (int i= 0; i < children.length; i++) {
				IDiffElement element= children[i];
				if (element instanceof DiffNode)
					commit(pm, (DiffNode) element);
			}
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
	
	private IResource[] getUnaddedResources(List resources) throws SVNException {
		final List unadded = new ArrayList();
		final SVNException[] exception = new SVNException[] { null };
		for (Iterator iter = resources.iterator(); iter.hasNext();) {
			IResource resource = (IResource) iter.next();
	        if (resource.exists()) {
			    // visit each resource deeply
			    try {
				    resource.accept(new IResourceVisitor() {
					public boolean visit(IResource aResource) {
						ISVNLocalResource svnResource = SVNWorkspaceRoot.getSVNResourceFor(aResource);
						// skip ignored resources and their children
						try {
							if (svnResource.isIgnored())
								return false;
							// visit the children of shared resources
							if (svnResource.isManaged())
								return true;
							if ((aResource.getType() == IResource.FOLDER) && isSymLink(aResource)) // don't traverse into symlink folders
								return false;
						} catch (SVNException e) {
							exception[0] = e;
						}
						// file/folder is unshared so record it
						unadded.add(aResource);
						return aResource.getType() == IResource.FOLDER;
					}
				}, IResource.DEPTH_INFINITE, false /* include phantoms */);
			    } catch (CoreException e) {
				    throw SVNException.wrapException(e);
			    }
			    if (exception[0] != null) throw exception[0];
	        }
		}
		return (IResource[]) unadded.toArray(new IResource[unadded.size()]);
	}
	
	private boolean isSymLink(IResource resource) {
		File file = resource.getLocation().toFile();
	    try {
	    	if (!file.exists())
	    		return true;
	    	else {
	    		String cnnpath = file.getCanonicalPath();
	    		String abspath = file.getAbsolutePath();
	    		return !abspath.equals(cnnpath);
	    	}
	    } catch(IOException ex) {
	      return true;
	    }	
	}
	
	private boolean addToUnadded(IResource resource) {
		IResource parent = resource;
		while (parent != null) {
			parent = parent.getParent();
			if (unaddedList.contains(parent)) return false;
		}
		return true;
	}
    
}
