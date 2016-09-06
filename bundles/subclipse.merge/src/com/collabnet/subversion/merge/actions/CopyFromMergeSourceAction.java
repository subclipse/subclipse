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
package com.collabnet.subversion.merge.actions;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.util.Iterator;

import org.eclipse.core.resources.IResource;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.team.core.TeamException;
import org.tigris.subversion.subclipse.core.ISVNLocalResource;
import org.tigris.subversion.subclipse.core.resources.SVNWorkspaceRoot;
import org.tigris.subversion.subclipse.ui.actions.WorkspaceAction;
import org.tigris.subversion.svnclientadapter.SVNRevision;
import org.tigris.subversion.svnclientadapter.SVNUrl;

import com.collabnet.subversion.merge.Activator;
import com.collabnet.subversion.merge.CopyOperation;
import com.collabnet.subversion.merge.ISkippedMergeResult;
import com.collabnet.subversion.merge.MergeOptions;
import com.collabnet.subversion.merge.MergeResult;
import com.collabnet.subversion.merge.Messages;
import com.collabnet.subversion.merge.SkippedMergeResult;
import com.collabnet.subversion.merge.SkippedMergeResultsFolder;

public class CopyFromMergeSourceAction extends WorkspaceAction {

	protected void execute(IAction action) throws InvocationTargetException, InterruptedException {
		Iterator iter = selection.iterator();
		while (iter.hasNext()) {
			Object object = iter.next();
			if (object instanceof ISkippedMergeResult) {
				SVNRevision svnRevision = null;
				IResource resource = null;
				SVNUrl srcUrl = null;
				MergeResult mergeResult = null;
				if (object instanceof SkippedMergeResult) {
					SkippedMergeResult skippedMergeResult = (SkippedMergeResult)object;
					mergeResult = skippedMergeResult;
					MergeOptions mergeOptions = skippedMergeResult.getMergeOutput().getMergeOptions();
					svnRevision = mergeOptions.getToRevision();
					SVNUrl svnUrl = mergeOptions.getFromUrl();
					resource = skippedMergeResult.getResource();
					if (!resource.exists()) {
						ISVNLocalResource svnResource = SVNWorkspaceRoot.getSVNResourceFor(resource);
						String resourceSubString = resource.getFullPath().toOSString().substring(skippedMergeResult.getMergeOutput().getTarget().length() + 1);
						if (!resourceSubString.startsWith("/")) resourceSubString = "/" + resourceSubString; //$NON-NLS-1$ //$NON-NLS-2$
						try {
							srcUrl = new SVNUrl(svnUrl.toString() + resourceSubString.replaceAll("\\\\", "/")); //$NON-NLS-1$ //$NON-NLS-2$
						} catch (MalformedURLException e) {
							Activator.handleError(Messages.CopyFromMergeSourceAction_error, e);
							MessageDialog.openError(getShell(), Messages.CopyFromMergeSourceAction_title, e.getLocalizedMessage());
							return;
						}	
					}
				}
				if (object instanceof SkippedMergeResultsFolder) {
					SkippedMergeResultsFolder skippedMergeResultsFolder = (SkippedMergeResultsFolder)object;
					mergeResult = skippedMergeResultsFolder.getMergeResult();
					MergeOptions mergeOptions = skippedMergeResultsFolder.getMergeOutput().getMergeOptions();
					svnRevision = mergeOptions.getToRevision();
					SVNUrl svnUrl = mergeOptions.getFromUrl();
					resource = skippedMergeResultsFolder.getFolder();
					if (!resource.exists()) {
						ISVNLocalResource svnResource = SVNWorkspaceRoot.getSVNResourceFor(resource);
						String resourceSubString = resource.getFullPath().toOSString().substring(skippedMergeResultsFolder.getMergeOutput().getTarget().length() + 1);
						if (!resourceSubString.startsWith("/")) resourceSubString = "/" + resourceSubString; //$NON-NLS-1$ //$NON-NLS-2$
						try {
							srcUrl = new SVNUrl(svnUrl.toString() + resourceSubString.replaceAll("\\\\", "/")); //$NON-NLS-1$ //$NON-NLS-2$
						} catch (MalformedURLException e) {
							Activator.handleError(Messages.CopyFromMergeSourceAction_error2, e);
							MessageDialog.openError(getShell(), Messages.CopyFromMergeSourceAction_title, e.getLocalizedMessage());
							return;
						}
					}
				}
				if (resource.exists()) {
					MessageDialog.openError(getShell(), Messages.CopyFromMergeSourceAction_title, resource.getName() + Messages.CopyFromMergeSourceAction_alreadyExists);
					return;
				}
				if (svnRevision == null) svnRevision = SVNRevision.HEAD;
				IResource[] resources = { resource };
				File destPath = new File(resource.getLocation().toOSString());
				CopyOperation copyOperation = new CopyOperation(getTargetPart(), resources, srcUrl, destPath, svnRevision, mergeResult);
				copyOperation.run();
			}
		}
	}
	
	protected boolean isEnabled() throws TeamException {
		return true;
	}

}
