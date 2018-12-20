/**
 * ***************************************************************************** Copyright (c) 2009
 * CollabNet. All rights reserved. This program and the accompanying materials are made available
 * under the terms of the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 *
 * <p>Contributors: CollabNet - initial API and implementation
 * ****************************************************************************
 */
package com.collabnet.subversion.merge;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.ui.IWorkbenchPart;
import org.tigris.subversion.subclipse.core.SVNException;
import org.tigris.subversion.subclipse.core.SVNTeamProvider;
import org.tigris.subversion.subclipse.ui.Policy;
import org.tigris.subversion.subclipse.ui.operations.SVNOperation;

public class UndoMergeOperation extends SVNOperation {
  private IResource[] mergedResources;

  public UndoMergeOperation(IWorkbenchPart part, IResource[] resources) {
    super(part);
    mergedResources = resources;
  }

  protected String getTaskName() {
    return Messages.UndoMergeOperation_undo;
  }

  protected String getTaskName(SVNTeamProvider provider) {
    return Messages.UndoMergeOperation_undo2 + provider.getProject().getName();
  }

  protected boolean canRunAsJob() {
    return false;
  }

  protected void execute(IProgressMonitor monitor) throws SVNException, InterruptedException {
    monitor.beginTask(getTaskName(), mergedResources.length);
    try {
      for (int i = 0; i < mergedResources.length; i++) {
        monitor.subTask(mergedResources[i].getName());
        UndoMergeCommand command = new UndoMergeCommand(mergedResources[i]);
        command.run(Policy.subMonitorFor(monitor, 100));
        monitor.worked(1);
      }
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
}
