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
import java.text.Collator;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.tigris.subversion.subclipse.core.SVNException;
import org.tigris.subversion.subclipse.core.SVNProviderPlugin;
import org.tigris.subversion.subclipse.core.client.OperationManager;
import org.tigris.subversion.subclipse.core.resources.LocalResourceStatus;
import org.tigris.subversion.subclipse.core.resources.SVNWorkspaceRoot;
import org.tigris.subversion.subclipse.core.util.Util;
import org.tigris.subversion.svnclientadapter.ISVNClientAdapter;
import org.tigris.subversion.svnclientadapter.SVNClientException;
import org.tigris.subversion.svnclientadapter.SVNNodeKind;

/**
 * Revert changes made to the local verion of a file.  This is equivalent to replace with base revision
 */
public class RevertResourcesCommand implements ISVNCommand {

    private final SVNWorkspaceRoot root;
    private final IResource[] resources;
    private IResource[] resourcesToRevert;
    private boolean recurse = false;
    private IProject project;

    public RevertResourcesCommand(SVNWorkspaceRoot root, IResource[] resources) {
        this.root = root;
        this.resources = resources;
    }

    // derived from org.eclipse.team.internal.ui.Sorter
    // Compares IResources by their full path. Uses Comparator because that accounts for 
    // situations like 
    // /foo
    // /foo/file
    // /foobar
    // /foobar/file
    // where if ordered using string compare, foobar would come after foo and before foo/file,
    // and that would break the parent/child ordering of the array
    public static final Comparator resourceComparator = new Comparator() {
        Collator collator = Collator.getInstance();
        public boolean equals(Object obj) {
            return false;
        }
        public int compare(Object o1, Object o2) {
            IResource resource0 = (IResource) o1;
            IResource resource1 = (IResource) o2;
            return collator.compare(resource0.getFullPath().toString(), resource1.getFullPath().toString());
        }
    };
    
    /* (non-Javadoc)
     * @see org.tigris.subversion.subclipse.core.commands.ISVNCommand#run(org.eclipse.core.runtime.IProgressMonitor)
     */
    public void run(IProgressMonitor monitor) throws SVNException {
    	final Set<IResource> propertiesOnlyFolders = new LinkedHashSet<IResource>();
        // sort first, so that all children of a folder directly follow it in the array
        Arrays.sort( resources, resourceComparator );   
        ISVNClientAdapter svnClient = root.getRepository().getSVNClient();
        try {
        	final OperationManager operationManager = OperationManager.getInstance();          
            operationManager.beginOperation(svnClient);
            // If we are doing a recursive revert, take snapshot of resources for
            // local history first.  Also remove unversioned resources.
            if (recurse && resourcesToRevert != null) {
            	for (int i = 0; i < resourcesToRevert.length; i++) {
            		if (project == null || resourcesToRevert[i].getProject().equals(project)) {
	            		try {
							Util.saveLocalHistory(resourcesToRevert[i]);
						} catch (CoreException e) {
							SVNProviderPlugin.log(IStatus.ERROR, e.getMessage(), e);
						} 
						LocalResourceStatus status = SVNWorkspaceRoot.getSVNResourceFor( resourcesToRevert[i] ).getStatus();
						if (!(resourcesToRevert[i].getType() == IResource.FOLDER) || !status.isAdded()) {
							if (!status.isManaged()) {
		                		try {
									resourcesToRevert[i].delete(true, monitor);
		                		}
		                		catch (CoreException ex) {
		                			throw SVNException.wrapException(ex);
		                		}							
							}
						}
            		}
            	}
            }
            for (int i = 0; i < resources.length; i++) {
                LocalResourceStatus status = SVNWorkspaceRoot.getSVNResourceFor( resources[i] ).getStatus();
				// If a folder add is reverted, all the adds underneath it will be reverted too.
                // Don't try to revert them. Because the resources are sorted by path we can just
                // keep going along the IResource array until we find one that doesn't have the 
                // current as a base path. 
                if (resources[i].getType() == IResource.FOLDER && status.isAdded()) {
                    svnClient.revert(resources[i].getLocation().toFile(), true);
                    propertiesOnlyFolders.add(resources[i]);
                    monitor.worked(100);

                    // Add the subdirectories to the list of resources which must
                    // be refreshed.
                    try {
	                    resources[i].accept(new IResourceVisitor() {
	            			public boolean visit(IResource aResource) {
	            				if (aResource.getType() == IResource.FOLDER) {
	    	                    	operationManager.onNotify(aResource.getLocation().toFile(), SVNNodeKind.UNKNOWN);
	    	                    	// This is necessary for folders, that are ignored after the revert
	    	                    	propertiesOnlyFolders.add(aResource);
	            				}
	            				
	            				return true;
	            			}
	            		}, IResource.DEPTH_INFINITE, false);
                    } catch (CoreException e) {
                    	SVNProviderPlugin.log(Status.WARNING, "", e);
                    }
                    // If folder path has no ending / we can have problem where dir foobar will look like subdir of foo
                    String baseFullPath = resources[i].getFullPath().addTrailingSeparator().toString();
                    while (i < resources.length - 1 && resources[i+1].getFullPath().toString().startsWith( baseFullPath )) {
                        monitor.worked(100);
                        i++;
                    }
                } else {
                	if (!status.isManaged()) {
                		try {
								resources[i].delete(true, monitor);
						}
						catch (CoreException ex) {
							throw SVNException.wrapException(ex);
						}
                	}
                	else {
                		if (!recurse) {
	                		try {
								Util.saveLocalHistory(resources[i]);
							} catch (CoreException e) {
								SVNProviderPlugin.log(IStatus.ERROR, e.getMessage(), e);
							}
                		}
                		File path = resources[i].getLocation().toFile();
	                    svnClient.revert(path, recurse);
	                    // If only properties were changed, svn 1.4.0 does not 
	                    // notify the change. As workaround, do it manually.
	                    if (resources[i].getType() != IResource.FILE) {
	                    	operationManager.onNotify(path, SVNNodeKind.UNKNOWN);
	                    	propertiesOnlyFolders.add(resources[i]);
	                    }
	                    monitor.worked(100);
                	}
                }
                	
            }
        } catch (SVNClientException e) {
            throw SVNException.wrapException(e);
        } finally {
        	root.getRepository().returnSVNClient(svnClient);
        	if (propertiesOnlyFolders.size() > 0) {
        		OperationManager.getInstance().endOperation(true, propertiesOnlyFolders);
        	}
        	else {
        		OperationManager.getInstance().endOperation();
        	}
            monitor.done();
        }
    }

	public void setRecurse(boolean recurse) {
		this.recurse = recurse;
	}

	public void setResourcesToRevert(IResource[] resourcesToRevert) {
		this.resourcesToRevert = resourcesToRevert;
	}

	public void setProject(IProject project) {
		this.project = project;
	}
}
