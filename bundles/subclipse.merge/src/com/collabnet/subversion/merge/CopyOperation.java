/*******************************************************************************
 * Copyright (c) 2009 CollabNet.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     CollabNet - initial API and implementation
 ******************************************************************************/
package com.collabnet.subversion.merge;

import java.io.File;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.ui.IWorkbenchPart;
import org.tigris.subversion.subclipse.core.SVNException;
import org.tigris.subversion.subclipse.core.SVNTeamProvider;
import org.tigris.subversion.subclipse.ui.Policy;
import org.tigris.subversion.subclipse.ui.operations.RepositoryProviderOperation;
import org.tigris.subversion.svnclientadapter.SVNRevision;
import org.tigris.subversion.svnclientadapter.SVNUrl;

import com.collabnet.subversion.merge.views.MergeResultsView;

public class CopyOperation extends RepositoryProviderOperation {
	private SVNUrl srcUrl;
	private File destPath;
	private SVNRevision svnRevision;
	private CopyCommand command;
	private MergeResult mergeResult;

	public CopyOperation(IWorkbenchPart part, IResource[] resources, SVNUrl srcUrl, File destPath, SVNRevision svnRevision, MergeResult mergeResult) {
		super(part, resources);
		this.srcUrl = srcUrl;
		this.destPath = destPath;
		this.svnRevision = svnRevision;
		this.mergeResult = mergeResult;
	}

	protected void execute(SVNTeamProvider provider, IResource[] resources, IProgressMonitor monitor) throws SVNException, InterruptedException {
		 monitor.beginTask(null, 100);
		 try {		
			 command = new CopyCommand(provider.getSVNWorkspaceRoot(), srcUrl, destPath, svnRevision);
			 command.run(Policy.subMonitorFor(monitor,1000));
			 mergeResult.setAction(MergeResult.ACTION_ADD);
			 if (mergeResult.getResource() instanceof IContainer) {
				 MergeResult[] mergeResults = mergeResult.getMergeOutput().getMergeResults();
				 for (int i = 0; i < mergeResults.length; i++) {
					 if (mergeResults[i].getResource() != null && mergeResults[i].getResource().getParent() != null && mergeResults[i].getAction().equals(MergeResult.ACTION_SKIP)) {
						 IResource parent = mergeResults[i].getResource().getParent();
						 if (parent.getFullPath().equals(mergeResult.getResource().getFullPath())) {
							 mergeResults[i].setAction(MergeResult.ACTION_ADD);
						 }
					 }
				 }
			 }
			 mergeResult.getMergeOutput().store();
			 MergeResultsView.getView().refreshAsync(mergeResult.getMergeOutput());
		 } catch (SVNException e) {
			 if (e.operationInterrupted()) {
				 showCancelledMessage();
			 } else {
				 collectStatus(e.getStatus());
			 }
		 } finally {
			 monitor.done();
		 } 
	}

	protected String getTaskName(SVNTeamProvider provider) {
		return Messages.CopyOperation_copying + provider.getProject().getName();
	}

	protected String getTaskName() {
		return Messages.CopyOperation_title;
	}

}
