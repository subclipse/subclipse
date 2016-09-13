package org.tigris.subversion.subclipse.core.commands;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.tigris.subversion.subclipse.core.SVNException;
import org.tigris.subversion.subclipse.core.client.OperationManager;
import org.tigris.subversion.subclipse.core.resources.SVNWorkspaceRoot;
import org.tigris.subversion.svnclientadapter.ISVNClientAdapter;
import org.tigris.subversion.svnclientadapter.SVNClientException;

public class UpgradeResourcesCommand implements ISVNCommand {
	
    private final SVNWorkspaceRoot root;
    private final IResource[] resources;
   
	public UpgradeResourcesCommand(SVNWorkspaceRoot root, IResource[] resources) {
		this.root = root;
		this.resources = resources;
	}


    /* (non-Javadoc)
     * @see org.tigris.subversion.subclipse.core.commands.ISVNCommand#run(org.eclipse.core.runtime.IProgressMonitor)
     */
    public void run(IProgressMonitor monitor) throws SVNException {
    	ISVNClientAdapter svnClient = null;
        try {
            monitor.beginTask(null, 100 * resources.length);
            svnClient = root.getRepository().getSVNClient();
            OperationManager.getInstance().beginOperation(svnClient);
            
            for (int i = 0; i < resources.length; i++) {
                svnClient.upgrade(resources[i].getLocation().toFile());
                monitor.worked(100);
            }
        } catch (SVNClientException e) {
            throw SVNException.wrapException(e);
        } finally {
        	root.getRepository().returnSVNClient(svnClient);
            OperationManager.getInstance().endOperation();
            monitor.done();
        }
    } 

}
