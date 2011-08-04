/*******************************************************************************
 * Copyright (c) 2005, 2006 Subclipse project and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Subclipse project committers - initial API and implementation
 ******************************************************************************/
package org.tigris.subversion.subclipse.ui.operations;

import java.io.File;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.ui.IWorkbenchPart;
import org.tigris.subversion.subclipse.core.ISVNRemoteResource;
import org.tigris.subversion.subclipse.core.SVNException;
import org.tigris.subversion.subclipse.core.SVNTeamProvider;
import org.tigris.subversion.subclipse.ui.Policy;
import org.tigris.subversion.svnclientadapter.ISVNClientAdapter;
import org.tigris.subversion.svnclientadapter.SVNClientException;
import org.tigris.subversion.svnclientadapter.SVNRevision;

public class ExportRemoteFolderOperation extends SVNOperation {
	private ISVNRemoteResource folder;
	private File directory;
	private SVNRevision revision;

	public ExportRemoteFolderOperation(IWorkbenchPart part, ISVNRemoteResource folder, File directory, SVNRevision revision) {
		super(part);
		this.folder = folder;
		this.directory = directory;
		this.revision = revision;
	}
	
	protected String getTaskName() {
		return Policy.bind("ExportOperation.taskName"); //$NON-NLS-1$;
	}

	protected String getTaskName(SVNTeamProvider provider) {
		return Policy.bind("ExportOperation.0", provider.getProject().getName()); //$NON-NLS-1$  		
	}

	protected void execute(IProgressMonitor monitor) throws SVNException, InterruptedException {
		ISVNClientAdapter client = null;
		try {
			client = folder.getRepository().getSVNClient();
			try {
				client.doExport(folder.getUrl(), directory, revision, true);
			} catch (SVNClientException e) {
				throw SVNException.wrapException(e);
			}
		} catch (SVNException e) {
			if (e.operationInterrupted()) {
				showCancelledMessage();
			} else {
				collectStatus(e.getStatus());
			}
		} finally {
			folder.getRepository().returnSVNClient(client);
			monitor.done();
		}         
	}

}
