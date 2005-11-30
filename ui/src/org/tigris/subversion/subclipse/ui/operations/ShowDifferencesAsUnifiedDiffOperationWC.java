package org.tigris.subversion.subclipse.ui.operations;

import java.io.File;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.ui.IWorkbenchPart;
import org.tigris.subversion.subclipse.core.ISVNRepositoryLocation;
import org.tigris.subversion.subclipse.core.SVNException;
import org.tigris.subversion.subclipse.core.SVNProviderPlugin;
import org.tigris.subversion.subclipse.ui.Policy;
import org.tigris.subversion.svnclientadapter.ISVNClientAdapter;
import org.tigris.subversion.svnclientadapter.SVNClientException;
import org.tigris.subversion.svnclientadapter.SVNRevision;
import org.tigris.subversion.svnclientadapter.SVNUrl;

public class ShowDifferencesAsUnifiedDiffOperationWC extends SVNOperation {
	private File path;
	private SVNUrl toUrl;
	private SVNRevision toRevision;
	private File file;

	public ShowDifferencesAsUnifiedDiffOperationWC(IWorkbenchPart part, File path, SVNUrl toUrl, SVNRevision toRevision, File file) {
		super(part);
		this.path = path;
		this.toUrl = toUrl;
		this.toRevision = toRevision;
		this.file = file;
	}

	protected void execute(IProgressMonitor monitor) throws SVNException, InterruptedException {
		ISVNClientAdapter client = null;
		ISVNRepositoryLocation repository = SVNProviderPlugin.getPlugin().getRepository(toUrl.toString());
		if (repository != null)
			client = repository.getSVNClient();
		if (client == null)
			client = SVNProviderPlugin.getPlugin().getSVNClientManager().createSVNClient();
		try {
			client.diff(path, toUrl, toRevision, file, true);
		} catch (SVNClientException e) {
			throw SVNException.wrapException(e);
		} finally {
			monitor.done();
		}      
	}

	protected String getTaskName() {
		return Policy.bind("HistoryView.showDifferences"); //$NON-NLS-1$
	}

}
