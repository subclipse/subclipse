/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     Cédric Chabanois (cchabanois@ifrance.com) - modified for Subversion 
 *******************************************************************************/
package org.tigris.subversion.subclipse.core.resources;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.tigris.subversion.subclipse.core.ISVNLocalResource;
import org.tigris.subversion.subclipse.core.ISVNRemoteFolder;
import org.tigris.subversion.subclipse.core.ISVNRemoteResource;
import org.tigris.subversion.subclipse.core.Policy;
import org.tigris.subversion.subclipse.core.SVNException;
import org.tigris.subversion.subclipse.core.SVNStatus;
import org.tigris.subversion.subclipse.core.commands.GetStatusCommand;
import org.tigris.subversion.subclipse.core.util.Assert;
import org.tigris.subversion.svnclientadapter.SVNNodeKind;
import org.tigris.subversion.svnclientadapter.SVNRevision;
import org.tigris.subversion.svnclientadapter.SVNStatusKind;

/**
 * represents the base revision of a folder
 * 
 */
public class BaseFolder extends RemoteFolder implements ISVNRemoteFolder {
	private ISVNLocalResource localResource;
	
	/**
	 * Constructor for RemoteFolder.
	 * @throws SVNException
	 */
	public BaseFolder( 
			ISVNLocalResource localResource,
	        SVNRevision.Number lastChangedRevision,
	        Date date,
	        String author) throws SVNException {
		super(null, localResource.getRepository(), localResource.getUrl(), SVNRevision.BASE, lastChangedRevision, date, author);
		Assert.isNotNull(localResource);
		this.localResource = localResource;
	}
	
	/* (non-Javadoc)
     * TODO This should use the synchronization information instead of hitting the WC
	 * @see org.tigris.subversion.subclipse.core.resources.RemoteFolder#getMembers(org.eclipse.core.runtime.IProgressMonitor)
	 */
	protected ISVNRemoteResource[] getMembers(IProgressMonitor monitor)
			throws SVNException {
		// we can't use the svnurl to get the members of a base folder, that's
		// why we needed to override this method
		
		final IProgressMonitor progress = Policy.monitorFor(monitor);
		progress.beginTask(Policy.bind("RemoteFolder.getMembers"), 100); //$NON-NLS-1$
        
        if (children != null)
        {
            progress.done();
            return children;
        }
		
		try {
            List baseChildren = new ArrayList();

            // XXX There is no way to get immediate children only
            GetStatusCommand c = new GetStatusCommand(localResource);
            c.run(monitor);
            LocalResourceStatus[] statuses = c.getStatuses();
            for (int i = 0; i < statuses.length; i++) {
                if (localResource.getFile().equals(statuses[i].getFile())) {
                    continue;
                }

                // Don't create base entries for files that aren't managed yet
                if (statuses[i].getTextStatus() == SVNStatusKind.UNVERSIONED) {
                    continue;
                }
                
                // External entries don't have base information either
                if (statuses[i].getTextStatus() == SVNStatusKind.EXTERNAL) {
                    continue;
                }
                
                if (statuses[i].getNodeKind() == SVNNodeKind.DIR) {
                    IContainer container = localResource.getIResource().getWorkspace().getRoot().getContainerForLocation(new Path(statuses[i].getFile().getAbsolutePath()));
                    if (localResource.getIResource().equals(container.getParent())) {
                        baseChildren.add(new BaseFolder(new LocalFolder(container), 
                                statuses[i].getLastChangedRevision(),
                                statuses[i].getLastChangedDate(),
                                statuses[i].getLastCommitAuthor()));
                    }
                } else {
                    IFile file = localResource.getIResource().getWorkspace().getRoot().getFileForLocation(new Path(statuses[i].getFile().getAbsolutePath()));
                    if (localResource.getIResource().equals(file.getParent())) {
                        baseChildren.add(new BaseFile(
                                new LocalFile(file), 
                                statuses[i].getLastChangedRevision(),
                                statuses[i].getLastChangedDate(),
                                statuses[i].getLastCommitAuthor()));
                    }
                }
            }

            children = (ISVNRemoteResource[]) baseChildren.toArray(new ISVNRemoteResource[baseChildren.size()]);
            return children;
        } catch (CoreException e)
		{
            throw new SVNException(new SVNStatus(SVNStatus.ERROR, SVNStatus.DOES_NOT_EXIST, Policy.bind("RemoteFolder.doesNotExist", getRepositoryRelativePath()))); //$NON-NLS-1$
        } finally {
			progress.done();
		}	 	
	}
}
