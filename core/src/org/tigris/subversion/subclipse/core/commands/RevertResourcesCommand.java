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

import java.text.Collator;
import java.util.Arrays;
import java.util.Comparator;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.tigris.subversion.subclipse.core.SVNException;
import org.tigris.subversion.subclipse.core.client.OperationManager;
import org.tigris.subversion.subclipse.core.resources.LocalResourceStatus;
import org.tigris.subversion.subclipse.core.resources.SVNWorkspaceRoot;
import org.tigris.subversion.svnclientadapter.ISVNClientAdapter;
import org.tigris.subversion.svnclientadapter.SVNClientException;

/**
 * Revert changes made to the local verion of a file.  This is equivalent to replace with base revision
 */
public class RevertResourcesCommand implements ISVNCommand {

    private final SVNWorkspaceRoot root;
    private final IResource[] resources;

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
        // sort first, so that all children of a folder directly follow it in the array
        Arrays.sort( resources, resourceComparator );        
        try {
            ISVNClientAdapter svnClient = root.getRepository().getSVNClient();
            OperationManager.getInstance().beginOperation(svnClient);
            for (int i = 0; i < resources.length; i++) {
                LocalResourceStatus status = SVNWorkspaceRoot.getSVNResourceFor( resources[i] ).getStatus();
					// If a folder add is reverted, all the adds underneath it will be reverted too.
                // Don't try to revert them. Because the resources are sorted by path we can just
                // keep going along the IResource array until we find one that doesn't have the 
                // current as a base path. 
                if (resources[i].getType() == IResource.FOLDER && status.isAdded()) {
                    svnClient.revert(resources[i].getLocation().toFile(), true);
                    monitor.worked(100);
                    SVNWorkspaceRoot.getSVNFolderFor( (IContainer)resources[i] ).refreshStatus(IResource.DEPTH_INFINITE);
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
	                    svnClient.revert(resources[i].getLocation().toFile(), false);
	                    monitor.worked(100);
                	}
                }
                	
            }
        } catch (SVNClientException e) {
            throw SVNException.wrapException(e);
        } finally {
            OperationManager.getInstance().endOperation();
            monitor.done();
        }
    }
}
