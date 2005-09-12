/******************************************************************************
 * This program and the accompanying materials are made available under
 * the terms of the Common Public License v1.0 which accompanies this
 * distribution, and is available at the following URL:
 * http://www.eclipse.org/legal/cpl-v10.html
 * Copyright(c) 2003-2005 by the authors indicated in the @author tags.
 *
 * All Rights are Reserved by the various authors.
 *******************************************************************************/
package org.tigris.subversion.subclipse.core.commands;

import java.io.File;
import java.util.Arrays;
import java.util.Comparator;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.tigris.subversion.subclipse.core.SVNException;
import org.tigris.subversion.subclipse.core.client.OperationManager;
import org.tigris.subversion.subclipse.core.client.OperationProgressNotifyListener;
import org.tigris.subversion.subclipse.core.resources.SVNWorkspaceRoot;
import org.tigris.subversion.svnclientadapter.ISVNClientAdapter;
import org.tigris.subversion.svnclientadapter.SVNClientException;
import org.tigris.subversion.svnclientadapter.SVNRevision;

/**
 * Update the given resources in the given project to the given revision
 * 
 * @author Cedric Chabanois (cchab at tigris.org)
 */
public class UpdateResourcesCommand implements ISVNCommand {
    private SVNWorkspaceRoot root;
    private IResource[] resources;
    private SVNRevision revision;
    
    
    public UpdateResourcesCommand(SVNWorkspaceRoot root, IResource[] resources, SVNRevision revision) {
        this.root = root;
        this.resources = (IResource[]) resources.clone();
        this.revision = revision;
        Arrays.sort(this.resources, new Comparator() {
			public int compare(Object o1, Object o2) {
				return ((IResource) o1).getFullPath().toString().compareTo(((IResource) o2).getFullPath().toString());
			}});
    }
    
	/* (non-Javadoc)
	 * @see org.tigris.subversion.subclipse.core.commands.ISVNCommand#run(org.eclipse.core.runtime.IProgressMonitor)
	 */
	public void run(final IProgressMonitor monitor) throws SVNException {
        try {
            monitor.beginTask(null, 100 * resources.length);                    
            ISVNClientAdapter svnClient = root.getRepository().getSVNClient();

            OperationManager.getInstance().beginOperation(svnClient, new OperationProgressNotifyListener(monitor));
    		if (resources.length == 1)
    		{
                monitor.subTask(resources[0].getName());
                svnClient.update(resources[0].getLocation().toFile(),revision,true);
                monitor.worked(100);    			
    		}
    		else
    		{
    			File[] files = new File[resources.length];
    			for (int i = 0; i < resources.length; i++) {
					files[i] = resources[i].getLocation().toFile();
				}
   				svnClient.update(files, revision, false, true);
   				monitor.worked(100);
    		}
        } catch (SVNClientException e) {
            throw SVNException.wrapException(e);
        } finally {
            OperationManager.getInstance().endOperation();
            monitor.done();
        }        
	}
    
    
}
