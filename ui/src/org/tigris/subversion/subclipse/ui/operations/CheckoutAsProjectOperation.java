package org.tigris.subversion.subclipse.ui.operations;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.ui.IWorkbenchPart;
import org.tigris.subversion.subclipse.core.ISVNRemoteFolder;
import org.tigris.subversion.subclipse.core.SVNException;
import org.tigris.subversion.subclipse.core.commands.CheckoutCommand;
import org.tigris.subversion.subclipse.ui.Policy;

public class CheckoutAsProjectOperation extends SVNOperation {
    private ISVNRemoteFolder[] remoteFolders;
    private IProject[] localFolders;
    private IPath projectRoot;

    public CheckoutAsProjectOperation(IWorkbenchPart part, ISVNRemoteFolder[] remoteFolders, IProject[] localFolders) {
    	this(part, remoteFolders, localFolders, null);
    }
    
    public CheckoutAsProjectOperation(IWorkbenchPart part, ISVNRemoteFolder[] remoteFolders, IProject[] localFolders, IPath projectRoot) {
        super(part);
        this.remoteFolders = remoteFolders;
        this.localFolders = localFolders;
        this.projectRoot = projectRoot; 
    }
    
    protected String getTaskName() {
        return Policy.bind("CheckoutAsProjectOperation.taskName"); //$NON-NLS-1$;
    }

    public void execute(IProgressMonitor monitor) throws SVNException, InterruptedException {
        monitor.beginTask(null, remoteFolders.length * 1000);
        for (int i = 0; i < remoteFolders.length; i++) {
            IProgressMonitor subMonitor = new SubProgressMonitor(monitor, 1000);
            ISchedulingRule rule = localFolders[i].getWorkspace().getRuleFactory().modifyRule(localFolders[i]);
			try {
				Platform.getJobManager().beginRule(rule, monitor);
				monitor.setTaskName(Policy.bind("CheckoutAsProjectOperation.0", remoteFolders[i].getName())); //$NON-NLS-1$
				IProject[] local = new IProject[1];
				local[0] = localFolders[i];
				ISVNRemoteFolder[] remote = new ISVNRemoteFolder[1];
				remote[0] = remoteFolders[i];
				execute(remote, local, subMonitor);
			} finally {
				Platform.getJobManager().endRule(rule);
			}            
        }
    }
    
    protected void execute(ISVNRemoteFolder[] remote, IProject[] local, IProgressMonitor monitor) throws SVNException, InterruptedException {
		try {
			CheckoutCommand command;
			if (projectRoot==null) {
				command = new CheckoutCommand(remote, local);
			} else {
				command = new CheckoutCommand(remote, local, projectRoot);
			}
	    	command.run(monitor);
		} catch (SVNException e) {
		    collectStatus(e.getStatus());
		}
    }
    
}
