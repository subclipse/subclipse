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

import java.io.File;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.tigris.subversion.subclipse.core.ISVNLocalResource;
import org.tigris.subversion.subclipse.core.ISVNRepositoryLocation;
import org.tigris.subversion.subclipse.core.SVNException;
import org.tigris.subversion.subclipse.core.client.OperationManager;
import org.tigris.subversion.subclipse.core.client.OperationResourceCollector;
import org.tigris.subversion.subclipse.core.commands.ISVNCommand;
import org.tigris.subversion.subclipse.core.resources.LocalResourceStatus;
import org.tigris.subversion.subclipse.core.resources.SVNWorkspaceRoot;
import org.tigris.subversion.svnclientadapter.ISVNClientAdapter;
import org.tigris.subversion.svnclientadapter.SVNClientException;
import org.tigris.subversion.svnclientadapter.SVNNodeKind;

public class UndoMergeCommand implements ISVNCommand {

  private final IResource resource;

  private OperationResourceCollector operationResourceCollector = new OperationResourceCollector();

  public UndoMergeCommand(IResource resource) {
    this.resource = resource;
  }

  public void run(IProgressMonitor monitor) throws SVNException {
    ISVNClientAdapter svnClient = null;
    ISVNRepositoryLocation repository = null;
    try {
      final OperationManager operationManager = OperationManager.getInstance();
      ISVNLocalResource svnResource = SVNWorkspaceRoot.getSVNResourceFor(resource);
      repository = svnResource.getRepository();
      svnClient = repository.getSVNClient();
      svnClient.addNotifyListener(operationResourceCollector);
      operationManager.beginOperation(svnClient);

      LocalResourceStatus status = SVNWorkspaceRoot.getSVNResourceFor(resource).getStatus();
      if (!status.isManaged()) {
        try {
          resource.delete(true, monitor);
        } catch (CoreException ex) {
          throw SVNException.wrapException(ex);
        }
      } else {
        File path = resource.getLocation().toFile();
        svnClient.revert(path, true);
        if (resource.getType() != IResource.FILE)
          operationManager.onNotify(path, SVNNodeKind.UNKNOWN);
        monitor.worked(100);
      }
    } catch (SVNClientException e) {
      throw SVNException.wrapException(e);
    } finally {
      if (repository != null) {
        repository.returnSVNClient(svnClient);
      }
      OperationManager.getInstance()
          .endOperation(true, operationResourceCollector.getOperationResources());
      monitor.done();
    }
  }
}
