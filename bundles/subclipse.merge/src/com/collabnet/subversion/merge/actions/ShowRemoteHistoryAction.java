/**
 * ***************************************************************************** Copyright (c) 2009
 * CollabNet. All rights reserved. This program and the accompanying materials are made available
 * under the terms of the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 *
 * <p>Contributors: CollabNet - initial API and implementation
 * ****************************************************************************
 */
package com.collabnet.subversion.merge.actions;

import com.collabnet.subversion.merge.Activator;
import com.collabnet.subversion.merge.MergeOptions;
import com.collabnet.subversion.merge.MergeOutput;
import com.collabnet.subversion.merge.MergeResult;
import com.collabnet.subversion.merge.MergeResultsFolder;
import com.collabnet.subversion.merge.Messages;
import java.text.ParseException;
import java.util.Iterator;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.tigris.subversion.subclipse.core.ISVNLocalResource;
import org.tigris.subversion.subclipse.core.ISVNRemoteResource;
import org.tigris.subversion.subclipse.core.resources.SVNWorkspaceRoot;
import org.tigris.subversion.subclipse.ui.actions.ShowHistoryAction;
import org.tigris.subversion.subclipse.ui.history.HistorySearchViewerFilter;
import org.tigris.subversion.subclipse.ui.history.SVNHistoryPage;
import org.tigris.subversion.svnclientadapter.SVNRevision;
import org.tigris.subversion.svnclientadapter.SVNUrl;

public class ShowRemoteHistoryAction extends ShowHistoryAction {
  public IStructuredSelection fSelection;

  public ShowRemoteHistoryAction() {
    super();
  }

  protected ISVNRemoteResource[] getSelectedRemoteResources() {
    MergeOutput mergeOutput = null;
    ISVNRemoteResource remoteResource = null;
    Iterator iter = fSelection.iterator();
    while (iter.hasNext()) {
      Object object = iter.next();
      if (object instanceof MergeResult) {
        MergeResult mergeResult = (MergeResult) object;
        IResource resource = mergeResult.getResource();
        mergeOutput = mergeResult.getMergeOutput();
        MergeOptions mergeOptions = mergeOutput.getMergeOptions();
        SVNUrl svnUrl = mergeOptions.getFromUrl();
        ISVNLocalResource svnResource = SVNWorkspaceRoot.getSVNResourceFor(resource);
        try {
          String resourceSubString =
              resource.getFullPath().toOSString().substring(mergeOutput.getTarget().length() + 1);
          if (!resourceSubString.startsWith("/"))
            resourceSubString = "/" + resourceSubString; // $NON-NLS-1$ //$NON-NLS-2$
          SVNUrl remoteResourceUrl =
              new SVNUrl(
                  svnUrl.toString()
                      + resourceSubString.replaceAll("\\\\", "/")); // $NON-NLS-1$ //$NON-NLS-2$
          remoteResource = svnResource.getRepository().getRemoteFile(remoteResourceUrl);
        } catch (Exception e) {
          Activator.handleError(Messages.ShowRemoteHistoryAction_error, e);
          MessageDialog.openError(
              getShell(), Messages.ShowRemoteHistoryAction_title, e.getLocalizedMessage());
        }
        break;
      }
      if (object instanceof MergeResultsFolder) {
        MergeResultsFolder mergeResultsFolder = (MergeResultsFolder) object;
        IContainer folder = mergeResultsFolder.getFolder();
        mergeOutput = mergeResultsFolder.getMergeOutput();
        MergeOptions mergeOptions = mergeOutput.getMergeOptions();
        SVNUrl svnUrl = mergeOptions.getFromUrl();
        ISVNLocalResource svnResource = SVNWorkspaceRoot.getSVNResourceFor(folder);
        try {
          String resourceSubString =
              folder.getFullPath().toOSString().substring(mergeOutput.getTarget().length() + 1);
          if (!resourceSubString.startsWith("/"))
            resourceSubString = "/" + resourceSubString; // $NON-NLS-1$ //$NON-NLS-2$
          SVNUrl remoteResourceUrl =
              new SVNUrl(
                  svnUrl.toString()
                      + resourceSubString.replaceAll("\\\\", "/")); // $NON-NLS-1$ //$NON-NLS-2$
          remoteResource = svnResource.getRepository().getRemoteFile(remoteResourceUrl);
        } catch (Exception e) {
          Activator.handleError(Messages.ShowRemoteHistoryAction_error, e);
          MessageDialog.openError(
              getShell(), Messages.ShowRemoteHistoryAction_title, e.getLocalizedMessage());
        }
        break;
      }
    }
    if (remoteResource != null) {
      ISVNRemoteResource[] selectedResource = {remoteResource};
      SVNRevision.Number startRevision = null;
      try {
        startRevision =
            (SVNRevision.Number)
                SVNRevision.getRevision(Long.toString(mergeOutput.getLowerRevision()));
      } catch (ParseException e) {
      }
      SVNRevision.Number endRevision = null;
      if (mergeOutput.getUpperRevision() > 0) {
        try {
          endRevision =
              (SVNRevision.Number)
                  SVNRevision.getRevision(Long.toString(mergeOutput.getUpperRevision()));
        } catch (ParseException e) {
        }
      }
      HistorySearchViewerFilter historySearchViewerFilter =
          new HistorySearchViewerFilter(null, null, null, null, true, startRevision, endRevision);
      SVNHistoryPage.setHistorySearchViewerFilter(historySearchViewerFilter);
      return selectedResource;
    }
    return new ISVNRemoteResource[0];
  }

  public void selectionChanged(IAction action, ISelection sel) {
    if (sel instanceof IStructuredSelection) {
      fSelection = (IStructuredSelection) sel;
    }
  }
}
