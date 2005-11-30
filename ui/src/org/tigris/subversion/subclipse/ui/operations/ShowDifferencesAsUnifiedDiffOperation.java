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

public class ShowDifferencesAsUnifiedDiffOperation extends SVNOperation {
	private SVNUrl fromUrl;
	private SVNRevision fromRevision;
	private SVNUrl toUrl;
	private SVNRevision toRevision;
	private File file;

	public ShowDifferencesAsUnifiedDiffOperation(IWorkbenchPart part, SVNUrl fromUrl, SVNRevision fromRevision, SVNUrl toUrl, SVNRevision toRevision, File file) {
		super(part);
		this.fromUrl = fromUrl;
		this.toUrl = toUrl;
		this.fromRevision = fromRevision;
		this.toRevision = toRevision;
		this.file = file;
	}

	protected void execute(IProgressMonitor monitor) throws SVNException, InterruptedException {
		ISVNClientAdapter client = null;
		ISVNRepositoryLocation repository = SVNProviderPlugin.getPlugin().getRepository(fromUrl.toString());
		if (repository != null)
			client = repository.getSVNClient();
		if (client == null)
			client = SVNProviderPlugin.getPlugin().getSVNClientManager().createSVNClient();
		try {
			client.diff(fromUrl, fromRevision, toUrl, toRevision, file, true);
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
