package org.tigris.subversion.subclipse.ui.operations;

import java.io.File;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.ui.IWorkbenchPart;
import org.tigris.subversion.subclipse.core.SVNException;
import org.tigris.subversion.subclipse.core.SVNTeamProvider;
import org.tigris.subversion.subclipse.core.commands.CleanupResourcesCommand;
import org.tigris.subversion.subclipse.ui.Policy;
import org.tigris.subversion.svnclientadapter.ISVNClientAdapter;
import org.tigris.subversion.svnclientadapter.SVNRevision;
import org.tigris.subversion.svnclientadapter.SVNUrl;
import org.tigris.subversion.svnclientadapter.utils.Depth;

public class ReplaceWithRemoteOperation extends RepositoryProviderOperation {
	private IResource resource;
	private SVNUrl url;
	private SVNRevision revision;

	public ReplaceWithRemoteOperation(IWorkbenchPart part, IResource resource, SVNUrl url, SVNRevision revision) {
		super(part, new IResource[] { resource } );
		this.resource = resource;
		this.url = url;
		this.revision = revision;
	}
	

	@Override
	protected String getTaskName() {
		return Policy.bind("ReplaceWithRemoteOperation.0"); //$NON-NLS-1$
	}


	@Override
	protected String getTaskName(SVNTeamProvider provider) {
		return Policy.bind("ReplaceWithRemoteOperation.0"); //$NON-NLS-1$
	}


	@Override
	protected void execute(SVNTeamProvider provider, IResource[] resources, IProgressMonitor monitor) throws SVNException, InterruptedException {
		ISVNClientAdapter client = provider.getSVNWorkspaceRoot().getRepository().getSVNClient();
		try {
			File[] files = new File[] { resources[0].getLocation().toFile() };
			client.remove(files, true);
			client.copy(url, files[0], revision);
		} catch (Exception e) {
			throw SVNException.wrapException(e);
		}
		finally {
			try {
				resource.refreshLocal(Depth.infinity, monitor);
				if (resource instanceof IContainer) {
		            CleanupResourcesCommand command = new CleanupResourcesCommand(provider.getSVNWorkspaceRoot(),resources);
		            command.run(Policy.subMonitorFor(monitor,100));
				}
			} catch (CoreException e) {}
			monitor.done();
		}
	}

}
