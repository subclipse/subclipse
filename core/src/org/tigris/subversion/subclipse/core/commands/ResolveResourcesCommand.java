package org.tigris.subversion.subclipse.core.commands;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.tigris.subversion.subclipse.core.SVNException;
import org.tigris.subversion.subclipse.core.client.OperationManager;
import org.tigris.subversion.subclipse.core.resources.SVNWorkspaceRoot;
import org.tigris.subversion.svnclientadapter.ISVNClientAdapter;
import org.tigris.subversion.svnclientadapter.SVNClientException;

/**
 * Mark a conflicted resource as being resolved.  This will also remove the .mine and .r* files
 */
public class ResolveResourcesCommand implements ISVNCommand {

    private final SVNWorkspaceRoot root;
    private final IResource[] resources;

    public ResolveResourcesCommand(SVNWorkspaceRoot root, IResource[] resources) {
        this.root = root;
        this.resources = resources;
        
    }
    
    /* (non-Javadoc)
     * @see org.tigris.subversion.subclipse.core.commands.ISVNCommand#run(org.eclipse.core.runtime.IProgressMonitor)
     */
    public void run(IProgressMonitor monitor) throws SVNException {
        try {
            ISVNClientAdapter svnClient = root.getRepository().getSVNClient();
            OperationManager.getInstance().beginOperation(svnClient);
            
            for (int i = 0; i < resources.length; i++) {
                svnClient.resolved(resources[i].getLocation().toFile());
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
