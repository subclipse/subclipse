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
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.tigris.subversion.subclipse.core.ISVNRemoteFolder;
import org.tigris.subversion.subclipse.core.ISVNRemoteResource;
import org.tigris.subversion.subclipse.core.Policy;
import org.tigris.subversion.subclipse.core.SVNException;
import org.tigris.subversion.subclipse.core.SVNStatus;
import org.tigris.subversion.subclipse.core.commands.GetStatusCommand;
import org.tigris.subversion.subclipse.core.util.Assert;
import org.tigris.subversion.svnclientadapter.SVNRevision;

/**
 * represents the base revision of a folder
 * 
 */
public class BaseFolder extends RemoteFolder implements ISVNRemoteFolder {
	private LocalResourceStatus localResourceStatus;
	
	public BaseFolder(LocalResourceStatus localResourceStatus)
	{
		super(null, 
				localResourceStatus.getRepository(), 
				localResourceStatus.getUrl(), 
				SVNRevision.BASE, 
				localResourceStatus.getLastChangedRevision(), 
				localResourceStatus.getLastChangedDate(), 
				localResourceStatus.getLastCommitAuthor());
		Assert.isNotNull(localResourceStatus);
		this.localResourceStatus = localResourceStatus;		
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
            GetStatusCommand c = new GetStatusCommand(localResourceStatus.getRepository(), localResourceStatus.getFile(), false, true);
            c.run(monitor);
            LocalResourceStatus[] statuses = c.getStatuses();
            List baseChildren = new ArrayList(statuses.length);

            for (int i = 0; i < statuses.length; i++) {
                if (localResourceStatus.getFile().equals(statuses[i].getFile())) {
                    continue;
                }

                // Don't create base entries for files that aren't managed yet
                if (!statuses[i].hasRemote()) {
                    continue;
                }
                
//                // External entries don't have base information either
//                if (statuses[i].getTextStatus() == SVNStatusKind.EXTERNAL) {
//                    continue;
//                }
                
                // The folders itself is not its own child, all direct children are
                if (!statuses[i].getUrlString().equals(localResourceStatus.getUrlString()))
                {
                	baseChildren.add(new BaseFolder(statuses[i]));
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
