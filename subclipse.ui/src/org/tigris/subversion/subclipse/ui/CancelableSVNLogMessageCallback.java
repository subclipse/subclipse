package org.tigris.subversion.subclipse.ui;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.tigris.subversion.svnclientadapter.ISVNClientAdapter;
import org.tigris.subversion.svnclientadapter.ISVNLogMessage;
import org.tigris.subversion.svnclientadapter.SVNClientException;
import org.tigris.subversion.svnclientadapter.SVNLogMessageCallback;

public class CancelableSVNLogMessageCallback extends SVNLogMessageCallback {
	private IProgressMonitor monitor;
	private ISVNClientAdapter svnClient;
	private boolean canceled;

	public CancelableSVNLogMessageCallback(IProgressMonitor monitor, ISVNClientAdapter svnClient) {
		super();
		this.monitor = monitor;
		this.svnClient = svnClient;
	}

	public void singleMessage(ISVNLogMessage msg) {
		super.singleMessage(msg);
		if (monitor != null && monitor.isCanceled() && !canceled) {
			try {
				svnClient.cancelOperation();
				canceled = true;
			} catch (SVNClientException e) {
				SVNUIPlugin.log(IStatus.ERROR, e.getMessage(), e);
			}
		}
	}

}
