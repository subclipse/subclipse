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

import org.eclipse.core.runtime.IProgressMonitor;
import org.tigris.subversion.subclipse.core.SVNException;
import org.tigris.subversion.subclipse.core.client.OperationManager;
import org.tigris.subversion.subclipse.core.commands.ISVNCommand;
import org.tigris.subversion.subclipse.core.resources.SVNWorkspaceRoot;
import org.tigris.subversion.svnclientadapter.ISVNClientAdapter;
import org.tigris.subversion.svnclientadapter.SVNClientException;
import org.tigris.subversion.svnclientadapter.SVNRevision;
import org.tigris.subversion.svnclientadapter.SVNUrl;

public class CopyCommand implements ISVNCommand {
	private SVNWorkspaceRoot root;
	private SVNUrl srcUrl;
	private File destPath;
	private SVNRevision svnRevision;

	public CopyCommand(SVNWorkspaceRoot root, SVNUrl srcUrl, File destPath, SVNRevision svnRevision) {
		super();
		this.root = root;
		this.srcUrl = srcUrl;
		this.destPath = destPath;
		this.svnRevision = svnRevision;
	}

	public void run(IProgressMonitor monitor) throws SVNException {
		ISVNClientAdapter svnClient = null;
        try {
            monitor.beginTask(null, 100);
            svnClient = root.getRepository().getSVNClient();
            OperationManager.getInstance().beginOperation(svnClient);
            monitor.subTask(destPath.getName());
            svnClient.copy(srcUrl, destPath, svnRevision, false, true);
            monitor.worked(100);        
        } catch (SVNClientException e) {
            throw SVNException.wrapException(e);
        } finally {
        	root.getRepository().returnSVNClient(svnClient);
            OperationManager.getInstance().endOperation();
            monitor.done();
        }                  
	}

}
