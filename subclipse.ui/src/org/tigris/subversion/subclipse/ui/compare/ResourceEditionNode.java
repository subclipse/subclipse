/*******************************************************************************
 * Copyright (c) 2003, 2006 Subclipse project and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Subclipse project committers - initial API and implementation
 ******************************************************************************/
package org.tigris.subversion.subclipse.ui.compare;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.compare.CompareUI;
import org.eclipse.compare.IEncodedStreamContentAccessor;
import org.eclipse.compare.IStreamContentAccessor;
import org.eclipse.compare.ITypedElement;
import org.eclipse.compare.structuremergeviewer.IStructureComparator;
import org.eclipse.core.internal.dtree.IComparator;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.graphics.Image;
import org.eclipse.team.core.TeamException;
import org.tigris.subversion.subclipse.core.ISVNCoreConstants;
import org.tigris.subversion.subclipse.core.ISVNLocalResource;
import org.tigris.subversion.subclipse.core.ISVNRemoteResource;
import org.tigris.subversion.subclipse.core.SVNProviderPlugin;
import org.tigris.subversion.subclipse.core.resources.RemoteFile;
import org.tigris.subversion.subclipse.core.resources.RemoteFolder;
import org.tigris.subversion.subclipse.core.util.Util;
import org.tigris.subversion.subclipse.ui.SVNUIPlugin;
import org.tigris.subversion.svnclientadapter.SVNRevision;
/**
 * A class for comparing ISVNRemoteResource objects
 * 
 * <p>
 * <pre>
 * ResourceEditionNode left = new ResourceEditionNode(editions[0]);
 * ResourceEditionNode right = new ResourceEditionNode(editions[1]);
 * CompareUI.openCompareEditorOnPage(new SVNCompareEditorInput(left, right), getTargetPage());
 * </pre>
 * </p>
 *  
 */
public class ResourceEditionNode
		implements
			IStructureComparator,
			ITypedElement,
			IStreamContentAccessor,
			IEncodedStreamContentAccessor {
	private ISVNRemoteResource resource;
	private ResourceEditionNode[] children;
	private SVNLocalResourceNode localResource = null;
	private String charset = "UTF8";
	private SVNRevision pegRevision;
	
	private boolean ignoreHiddenChanges = SVNProviderPlugin.getPlugin().getPluginPreferences().getBoolean(ISVNCoreConstants.PREF_IGNORE_HIDDEN_CHANGES);
	
	
	public ResourceEditionNode(ISVNRemoteResource resourceEdition) {
		this(resourceEdition, null);
	}
	
	/**
	 * Creates a new ResourceEditionNode on the given resource edition.
	 */
	public ResourceEditionNode(ISVNRemoteResource resourceEdition, SVNRevision pegRevision) {
		this.resource = resourceEdition;
		this.pegRevision = pegRevision;
		if (pegRevision == null) {
			pegRevision = SVNRevision.HEAD;
		}
		if (resource instanceof RemoteFolder) {
			((RemoteFolder)resource).setPegRevision(pegRevision);
		}
		else if (resource instanceof RemoteFile) {
			((RemoteFile)resource).setPegRevision(pegRevision);
		}		
	}
	/*
	 * get the remote resource for this node
	 */
	public ISVNRemoteResource getRemoteResource() {
		return resource;
	}
	/**
	 * Returns true if both resources names are identical. The content is not
	 * considered.
	 * 
	 * @see IComparator#equals
	 */
	public boolean equals(Object other) {
		if (other instanceof ITypedElement) {
			String otherName = ((ITypedElement) other).getName();
			return getName().equals(otherName);
		}
		return super.equals(other);
	}
	/**
	 * Enumerate children of this node (if any).
	 * 
	 * @see IStructureComparator#getChildren
	 */
	public Object[] getChildren() {
		if (children == null) {
			children = new ResourceEditionNode[0];
	        ISVNLocalResource mylocalResource = null;
	        if (localResource instanceof SVNLocalResourceNode) {
	            mylocalResource = ((SVNLocalResourceNode)localResource).getLocalResource();
	            try {
	            	
	            	if (!mylocalResource.isDirty() && mylocalResource.getResource().getProjectRelativePath().toString().equals(getRemoteResource().getProjectRelativePath()) &&
	            			mylocalResource.getStatus().getLastChangedRevision().equals(getRemoteResource().getLastChangedRevision())) {
	            		return children;
	            	}
	            }
	            catch(CoreException e) {
					SVNUIPlugin.log(IStatus.ERROR, e.getMessage(), e);
				}
	        }
			if (resource != null) {
				try {
					SVNUIPlugin.runWithProgress(null, true /* cancelable */,
							new IRunnableWithProgress() {
								public void run(IProgressMonitor monitor)
										throws InvocationTargetException {
									try {
										ISVNRemoteResource[] members = resource
												.members(monitor);
										List<ResourceEditionNode> nonHiddenChildren = new ArrayList<ResourceEditionNode>();
										for (int i = 0; i < members.length; i++) {
											if (!ignoreHiddenChanges || members[i].getResource() == null || !Util.isHidden(members[i].getResource(), false)) {
												ResourceEditionNode child = new ResourceEditionNode(members[i], pegRevision);
												SVNLocalResourceNode localNode = matchLocalResource((ISVNRemoteResource) members[i]);
												if (localNode != null) {
													child.setLocalResource(localNode);
													localNode.setRemoteResource(child);
													try {
														child.setCharset(localNode.getCharset());
													} catch (CoreException e) {
														SVNUIPlugin.log(IStatus.ERROR, e.getMessage(), e);
													}
												}	
												nonHiddenChildren.add(child);
											}
										}
										
										children = new ResourceEditionNode[nonHiddenChildren.size()];
										nonHiddenChildren.toArray(children);
										
									} catch (TeamException e) {
										throw new InvocationTargetException(e);
									}
								}
							});
				} catch (InterruptedException e) {
					// operation canceled
				} catch (InvocationTargetException e) {
					Throwable t = e.getTargetException();
					if (t instanceof TeamException) {
						SVNUIPlugin.log(((TeamException) t).getStatus());
					}
				}
			}
		}
		return children;
	}
	/**
	 * @see IStreamContentAccessor#getContents()
	 */
	public InputStream getContents() throws CoreException {
		if (resource == null || resource.isContainer()) {
			return null;
		}
		try {
			final InputStream[] holder = new InputStream[1];
			SVNUIPlugin.runWithProgress(null, true,
					new IRunnableWithProgress() {
						public void run(IProgressMonitor monitor) {
							try {
								holder[0] = resource.getStorage(monitor).getContents();
							} catch (CoreException e1) {
								SVNUIPlugin.log(e1);
							}
						}
					});
			return holder[0];
		} catch (InterruptedException e) {
			// operation canceled
		} catch (InvocationTargetException e) {
			Throwable t = e.getTargetException();
			if (t instanceof TeamException) {
				throw new CoreException(((TeamException) t).getStatus());
			}
			// should not get here
		}
		return new ByteArrayInputStream(new byte[0]);
	}
	/*
	 * @see org.eclipse.compare.ITypedElement#getImage()
	 */
	public Image getImage() {
		return CompareUI.getImage(resource);
	}
	/*
	 * Returns the name of this node.
	 * 
	 * @see org.eclipse.compare.ITypedElement#getName()
	 */
	public String getName() {
		return resource == null ? "" : resource.getName(); //$NON-NLS-1$
	}
	/**
	 * Returns the comparison type for this node.
	 * 
	 * @see org.eclipse.compare.ITypedElement#getType()
	 */
	public String getType() {
		if (resource == null) {
			return UNKNOWN_TYPE;
		}
		if (resource.isContainer()) {
			return FOLDER_TYPE;
		}
		String name = resource.getName();
		name = name.substring(name.lastIndexOf('.') + 1);
		return name.length() == 0 ? UNKNOWN_TYPE : name;
	}
	/**
	 * @see IComparator#equals
	 */
	public int hashCode() {
		return getName().hashCode();
	}
	
	public String getCharset() throws CoreException {
		return charset;
	}
	
	public void setCharset(String charset) throws CoreException {
		this.charset = charset;
	}
	
	public void setLocalResource(SVNLocalResourceNode localResource){
		this.localResource = localResource;
	}
	
	private SVNLocalResourceNode matchLocalResource(ISVNRemoteResource remoteNode){
	    if (localResource == null) return null;
	    ISVNRemoteResource baseFolder = remoteNode;
	    if (baseFolder.getParent() != null) {
	    	baseFolder = baseFolder.getParent();
	    }
		Object[] lrn = localResource.getChildren();
		String remotePath=remoteNode.getRepositoryRelativePath();
		remotePath = remotePath.replaceAll(baseFolder.getRepositoryRelativePath(),"");
		for(int i=0;i<lrn.length;i++){
			String localPath=((SVNLocalResourceNode)lrn[i]).getResource().getFullPath().toString();
			localPath = localPath.substring(localResource.getResource().getFullPath().toString().length());
			if(localPath.equals(remotePath)){
				return (SVNLocalResourceNode)lrn[i];
			}
		}
		return null;
	}

}