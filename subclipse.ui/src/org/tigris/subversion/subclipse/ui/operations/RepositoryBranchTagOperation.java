package org.tigris.subversion.subclipse.ui.operations;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.ui.IWorkbenchPart;
import org.tigris.subversion.subclipse.core.SVNException;
import org.tigris.subversion.subclipse.core.commands.BranchTagCommand;
import org.tigris.subversion.subclipse.ui.Policy;
import org.tigris.subversion.subclipse.ui.SVNUIPlugin;
import org.tigris.subversion.svnclientadapter.ISVNClientAdapter;
import org.tigris.subversion.svnclientadapter.SVNRevision;
import org.tigris.subversion.svnclientadapter.SVNUrl;

public class RepositoryBranchTagOperation extends SVNOperation {
	private ISVNClientAdapter svnClient;
    private SVNUrl[] sourceUrls;
    private SVNUrl destinationUrl;
    private SVNRevision revision;
    private boolean makeParents;
    private String message;
    private boolean multipleTransactions = true;

	public RepositoryBranchTagOperation(IWorkbenchPart part, ISVNClientAdapter svnClient, SVNUrl[] sourceUrls, SVNUrl destinationUrl, SVNRevision revision, String message, boolean makeParents) {
		super(part);
		this.svnClient = svnClient;
        this.sourceUrls = sourceUrls;
        this.destinationUrl = destinationUrl;
        this.revision = revision;
        this.message = message;
        this.makeParents = makeParents;
	}

	protected void execute(IProgressMonitor monitor) throws SVNException, InterruptedException {
    	monitor.beginTask(null, 100);
		try {	
			BranchTagCommand command = new BranchTagCommand(svnClient, null, sourceUrls, destinationUrl, message, true, revision);
	        command.setMakeParents(makeParents);
	        command.setMultipleTransactions(multipleTransactions);
	    	command.run(Policy.subMonitorFor(monitor,1000));
	    	SVNUIPlugin.getPlugin().getRepositoryManager().resourceCreated(null, null);
		} catch (SVNException e) {
			if (e.operationInterrupted()) {
				showCancelledMessage();
			} else {
				collectStatus(e.getStatus());
			}
		} finally {
			monitor.done();
		}         
	}
	
	public void setMultipleTransactions(boolean multipleTransactions) {
		this.multipleTransactions = multipleTransactions;
	}

	protected String getTaskName() {
		return Policy.bind("BranchTagOperation.taskName"); //$NON-NLS-1$;
	}

}
