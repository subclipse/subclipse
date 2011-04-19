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

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.team.core.TeamException;
import org.tigris.subversion.subclipse.core.ISVNFolder;
import org.tigris.subversion.subclipse.core.ISVNLocalFolder;
import org.tigris.subversion.subclipse.core.ISVNLocalResource;
import org.tigris.subversion.subclipse.core.ISVNResource;
import org.tigris.subversion.subclipse.core.Policy;
import org.tigris.subversion.subclipse.core.SVNException;
import org.tigris.subversion.subclipse.core.SVNProviderPlugin;
import org.tigris.subversion.subclipse.core.client.OperationManager;
import org.tigris.subversion.svnclientadapter.ISVNClientAdapter;
import org.tigris.subversion.svnclientadapter.SVNClientException;

/**
 * Adds a pattern to the set of ignores for the specified folder.
 * 
 * @author Cedric Chabanois (cchab at tigris.org) 
 */
public class AddIgnoredPatternCommand implements ISVNCommand {
    private ISVNLocalFolder folder;
    private String pattern;
    
    public AddIgnoredPatternCommand(ISVNLocalFolder folder, String pattern) {
        this.folder = folder;
        this.pattern = pattern;
    }
    
    
	/* (non-Javadoc)
	 * @see org.tigris.subversion.subclipse.core.commands.ISVNCommand#run(org.eclipse.core.runtime.IProgressMonitor)
	 */
	public void run(IProgressMonitor monitor) throws SVNException {
		monitor = Policy.monitorFor(monitor);
        monitor.beginTask(null, 100); //$NON-NLS-1$
        if (!folder.getStatus().isManaged())
            throw new SVNException(IStatus.ERROR, TeamException.UNABLE,
                Policy.bind("SVNTeamProvider.ErrorSettingIgnorePattern", folder.getIResource().getFullPath().toString())); //$NON-NLS-1$
        ISVNClientAdapter svnClient = folder.getRepository().getSVNClient();
        try {
            OperationManager.getInstance().beginOperation(svnClient);
            
            try {
                svnClient.addToIgnoredPatterns(folder.getFile(), pattern);
                
                // broadcast changes to unmanaged children - they are the only candidates for being ignored
                ISVNResource[] members = folder.members(null, ISVNFolder.UNMANAGED_MEMBERS);
                IResource[] possiblesIgnores = new IResource[members.length];
                for (int i = 0; i < members.length;i++) {
                    possiblesIgnores[i] = ((ISVNLocalResource)members[i]).getIResource();
                }
                folder.refreshStatus(false);
                SVNProviderPlugin.broadcastSyncInfoChanges(possiblesIgnores, false);
                broadcastNestedFolders(possiblesIgnores);
            }
            catch (SVNClientException e) {
                throw SVNException.wrapException(e);
            }

        } finally {
            OperationManager.getInstance().endOperation();
            monitor.done();
            folder.getRepository().returnSVNClient(svnClient);
        }
	}

    /**
     * @param resources
     */
    private void broadcastNestedFolders(IResource[] resources) {
        for (int i = 0; i < resources.length;i++) {
            if (resources[i].getType() == IResource.FOLDER) {
                IFolder folder = (IFolder) resources[i];
                try {
                    IResource[] children = folder.members(true);
                    SVNProviderPlugin.broadcastSyncInfoChanges(children, false);
                    broadcastNestedFolders(children);
                } catch (CoreException e1) {
                }
            }
        }
    }
    
}
