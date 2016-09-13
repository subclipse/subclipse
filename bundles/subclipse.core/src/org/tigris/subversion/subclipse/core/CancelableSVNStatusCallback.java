package org.tigris.subversion.subclipse.core;

import org.eclipse.core.runtime.IProgressMonitor;
import org.tigris.subversion.svnclientadapter.ISVNClientAdapter;
import org.tigris.subversion.svnclientadapter.ISVNStatus;
import org.tigris.subversion.svnclientadapter.SVNClientException;
import org.tigris.subversion.svnclientadapter.SVNStatusCallback;

public class CancelableSVNStatusCallback extends SVNStatusCallback {
	private IProgressMonitor monitor;
	private ISVNClientAdapter svnClient;
	private boolean canceled;

	public CancelableSVNStatusCallback(IProgressMonitor monitor) {
		super();
		this.monitor = monitor;
	}

	public void setSvnClient(ISVNClientAdapter svnClient) {
		this.svnClient = svnClient;
	}

	@Override
	public void doStatus(String path, ISVNStatus status) {
		super.doStatus(path, status);
		if (svnClient != null && monitor != null && monitor.isCanceled() && !canceled) {
			try {
				svnClient.cancelOperation();
				canceled = true;
			} catch (SVNClientException e) {}
		}
	}

}