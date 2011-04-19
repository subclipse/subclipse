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

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.ui.IWorkbenchPart;
import org.tigris.subversion.subclipse.core.ISVNLocalResource;
import org.tigris.subversion.subclipse.core.ISVNRepositoryLocation;
import org.tigris.subversion.subclipse.core.ISVNResource;
import org.tigris.subversion.subclipse.core.SVNException;
import org.tigris.subversion.subclipse.core.SVNProviderPlugin;
import org.tigris.subversion.subclipse.core.resources.SVNWorkspaceRoot;
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
	private ISVNResource localResource;

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
			client = SVNProviderPlugin.getPlugin().getSVNClientManager().getSVNClient();
		try {
			SVNRevision pegRevision = null;
			if (fromUrl.toString().equals(toUrl.toString()) && localResource != null) {
				if (localResource.getResource() == null) pegRevision = SVNRevision.HEAD;
				else {
					IResource resource = localResource.getResource();
					ISVNLocalResource svnResource = SVNWorkspaceRoot.getSVNResourceFor(resource);
					pegRevision = svnResource.getRevision();
				}
			}
			if (pegRevision == null) client.diff(fromUrl, fromRevision, toUrl, toRevision, file, true);
			else client.diff(fromUrl, pegRevision, fromRevision, toRevision, file, true); 
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

	public void setLocalResource(ISVNResource localResource) {
		this.localResource = localResource;
	}

}
