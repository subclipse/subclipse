package org.tigris.subversion.subclipse.ui.operations;

import java.io.File;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.ui.IWorkbenchPart;
import org.tigris.subversion.subclipse.core.SVNException;
import org.tigris.subversion.subclipse.core.SVNTeamProvider;
import org.tigris.subversion.subclipse.core.resources.SVNWorkspaceRoot;
import org.tigris.subversion.subclipse.ui.Policy;
import org.tigris.subversion.svnclientadapter.ISVNClientAdapter;
import org.tigris.subversion.svnclientadapter.SVNClientException;

public class ExportOperation extends RepositoryProviderOperation {
	private String directory;

	public ExportOperation(IWorkbenchPart part, IResource[] resources, String directory) {
		super(part, resources);
		this.directory = directory;
	}
	
	protected String getTaskName() {
		return Policy.bind("ExportOperation.taskName"); //$NON-NLS-1$;
	}

	protected String getTaskName(SVNTeamProvider provider) {
		return Policy.bind("ExportOperation.0", provider.getProject().getName()); //$NON-NLS-1$  		
	}

	protected void execute(SVNTeamProvider provider, IResource[] resources, IProgressMonitor monitor) throws SVNException, InterruptedException {
		try {
			ISVNClientAdapter client = null;
			for (int i = 0; i < resources.length; i++) {	
				if (client == null) 
				    client = SVNWorkspaceRoot.getSVNResourceFor(resources[i]).getRepository().getSVNClient();
				File srcPath = new File(resources[i].getLocation().toString());
				File destPath= new File(directory + File.separator + resources[i].getName());
				try {
					client.doExport(srcPath, destPath, true);
				} catch (SVNClientException e) {
					throw SVNException.wrapException(e);
				}
			}
		} catch (SVNException e) {
		    collectStatus(e.getStatus());
		} finally {
			monitor.done();
		}         
	}

}
