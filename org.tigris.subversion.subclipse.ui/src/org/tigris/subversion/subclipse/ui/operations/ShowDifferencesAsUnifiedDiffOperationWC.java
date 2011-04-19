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
	private boolean graphicalCompare = false;
	private boolean canceled = false;

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
			client = SVNProviderPlugin.getPlugin().getSVNClientManager().getSVNClient();
		try {
			client.diff(path, toUrl, toRevision, file, true);
			if (monitor.isCanceled()) canceled = true;
		} catch (SVNClientException e) {
			throw SVNException.wrapException(e);
		} finally {
			monitor.done();
			SVNProviderPlugin.getPlugin().getSVNClientManager().returnSVNClient(client);
		}      
	}

	protected String getTaskName() {
		return Policy.bind("HistoryView.showDifferences"); //$NON-NLS-1$
	}

	public void setGraphicalCompare(boolean graphicalCompare) {
		this.graphicalCompare = graphicalCompare;
	}

	protected boolean canRunAsJob() {
		if (graphicalCompare) return false;
		else return super.canRunAsJob();
	}

	public boolean isCanceled() {
		return canceled;
	}

	public File getFile() {
		return file;
	}

}
