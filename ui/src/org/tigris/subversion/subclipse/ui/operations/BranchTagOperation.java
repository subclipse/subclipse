package org.tigris.subversion.subclipse.ui.operations;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.ui.IWorkbenchPart;
import org.tigris.subversion.subclipse.core.SVNException;
import org.tigris.subversion.subclipse.core.SVNTeamProvider;
import org.tigris.subversion.subclipse.core.commands.BranchTagCommand;
import org.tigris.subversion.subclipse.ui.Policy;
import org.tigris.subversion.svnclientadapter.SVNRevision;
import org.tigris.subversion.svnclientadapter.SVNUrl;

public class BranchTagOperation extends RepositoryProviderOperation {
    private SVNUrl sourceUrl;
    private SVNUrl destinationUrl;
    private SVNRevision revision;
    private boolean createOnServer;
    private String message;

    public BranchTagOperation(IWorkbenchPart part, IResource[] resources, SVNUrl sourceUrl, SVNUrl destinationUrl, boolean createOnServer, SVNRevision revision, String message) {
        super(part, resources);
        this.sourceUrl = sourceUrl;
        this.destinationUrl = destinationUrl;
        this.createOnServer = createOnServer;
        this.revision = revision;
        this.message = message;
    }
    
    protected String getTaskName() {
        return Policy.bind("BranchTagOperation.taskName"); //$NON-NLS-1$;
    }

    protected String getTaskName(SVNTeamProvider provider) {
        return Policy.bind("BranchTagOperation.0", provider.getProject().getName()); //$NON-NLS-1$  
    }

    protected void execute(SVNTeamProvider provider, IResource[] resources, IProgressMonitor monitor) throws SVNException, InterruptedException {
        monitor.beginTask(null, 100);
		try {			
	    	BranchTagCommand command = new BranchTagCommand(provider.getSVNWorkspaceRoot(),resources[0], sourceUrl, destinationUrl, message, createOnServer, revision);
	        command.run(Policy.subMonitorFor(monitor,1000));
		} catch (SVNException e) {
		    collectStatus(e.getStatus());
		} finally {
			monitor.done();
		}         
    }

}
