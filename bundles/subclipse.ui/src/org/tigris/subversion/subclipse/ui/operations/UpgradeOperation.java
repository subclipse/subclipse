package org.tigris.subversion.subclipse.ui.operations;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.ui.IWorkbenchPart;
import org.tigris.subversion.subclipse.core.SVNException;
import org.tigris.subversion.subclipse.core.SVNTeamProvider;
import org.tigris.subversion.subclipse.core.commands.UpgradeResourcesCommand;
import org.tigris.subversion.subclipse.ui.Policy;

public class UpgradeOperation extends RepositoryProviderOperation {

	public UpgradeOperation(IWorkbenchPart part, IResource[] resources) {
		super(part, resources);
	}
	
	@Override
	protected String getTaskName() {
		 return Policy.bind("UpgradeOperation.taskName"); //$NON-NLS-1$;
	}

	@Override
	protected String getTaskName(SVNTeamProvider provider) {
		 return Policy.bind("UpgradeOperation.0", provider.getProject().getName()); //$NON-NLS-1$
	}
	
	@Override
	protected void execute(SVNTeamProvider provider, IResource[] resources, IProgressMonitor monitor) throws SVNException, InterruptedException {
        monitor.beginTask(null, 100);
        try {           
            UpgradeResourcesCommand command = new UpgradeResourcesCommand(provider.getSVNWorkspaceRoot(),resources);
            command.run(Policy.subMonitorFor(monitor,100));
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

}
