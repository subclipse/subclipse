package org.tigris.subversion.subclipse.ui.operations;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.ui.IWorkbenchPart;
import org.tigris.subversion.subclipse.core.SVNException;
import org.tigris.subversion.subclipse.core.SVNTeamProvider;
import org.tigris.subversion.subclipse.core.commands.SwitchToUrlCommand;
import org.tigris.subversion.subclipse.ui.Policy;
import org.tigris.subversion.svnclientadapter.SVNRevision;
import org.tigris.subversion.svnclientadapter.SVNUrl;

public class SwitchOperation extends RepositoryProviderOperation {
    private SVNUrl svnUrl; 
    private SVNRevision svnRevision;
    
    public SwitchOperation(IWorkbenchPart part, IResource[] resources, SVNUrl svnUrl, SVNRevision svnRevision) {
        super(part, resources);
        this.svnUrl = svnUrl;
        this.svnRevision = svnRevision;
    }
    
    protected String getTaskName() {
        return Policy.bind("SwitchOperation.taskName"); //$NON-NLS-1$;
    }

    protected String getTaskName(SVNTeamProvider provider) {
        return Policy.bind("SwitchOperation.0", provider.getProject().getName()); //$NON-NLS-1$       
    }

    protected void execute(SVNTeamProvider provider, IResource[] resources, IProgressMonitor monitor) throws SVNException, InterruptedException {
        monitor.beginTask(null, 100);
		try {			
	    	SwitchToUrlCommand command = new SwitchToUrlCommand(provider.getSVNWorkspaceRoot(),resources[0], svnUrl, svnRevision);
	        command.run(monitor);
		} catch (SVNException e) {
		    collectStatus(e.getStatus());
		} finally {
			monitor.done();
		}  
    }

}
