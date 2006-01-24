package org.tigris.subversion.subclipse.ui.operations;

import java.io.File;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.ui.IWorkbenchPart;
import org.tigris.subversion.subclipse.core.ISVNRemoteFolder;
import org.tigris.subversion.subclipse.core.SVNException;
import org.tigris.subversion.subclipse.core.SVNTeamProvider;
import org.tigris.subversion.subclipse.core.commands.ImportCommand;
import org.tigris.subversion.subclipse.ui.Policy;

public class ImportOperation extends SVNOperation {
	private File directory;
	private ISVNRemoteFolder folder;
	private String commitComment;
	private boolean recurse;

	public ImportOperation(IWorkbenchPart part, ISVNRemoteFolder folder, File directory, String comment, boolean recurse) {
		super(part);
		this.folder = folder;
		this.directory = directory;
		this.commitComment = comment;
		this.recurse = recurse;
	}
	
	protected String getTaskName() {
		return Policy.bind("ImportOperation.taskName"); //$NON-NLS-1$;
	}

	protected String getTaskName(SVNTeamProvider provider) {
		return Policy.bind("ImportOperation.0", directory.toString()); //$NON-NLS-1$  		
	}

	/* (non-Javadoc)
	 * @see org.tigris.subversion.subclipse.ui.operations.SVNOperation#execute(org.eclipse.core.runtime.IProgressMonitor)
	 */
	protected void execute(IProgressMonitor monitor) throws SVNException,
			InterruptedException {

	    monitor.beginTask(null, 100);
		try {
		    ImportCommand command = new ImportCommand(folder, directory, commitComment, recurse);
	        command.run(monitor);
		} catch (SVNException e) {
		    collectStatus(e.getStatus());
		} finally {
			monitor.done();
		}         

	}
}
