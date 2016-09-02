/*******************************************************************************
 * Copyright (c) 2006 Subclipse project and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Subclipse project committers - initial API and implementation
 ******************************************************************************/
package org.tigris.subversion.subclipse.core.commands;

import java.io.File;

import org.eclipse.core.runtime.IProgressMonitor;
import org.tigris.subversion.subclipse.core.ISVNRemoteFolder;
import org.tigris.subversion.subclipse.core.Policy;
import org.tigris.subversion.subclipse.core.SVNException;
import org.tigris.subversion.subclipse.core.client.OperationManager;
import org.tigris.subversion.subclipse.core.client.OperationProgressNotifyListener;
import org.tigris.subversion.svnclientadapter.ISVNClientAdapter;
import org.tigris.subversion.svnclientadapter.SVNClientException;

/**
 * Import local folder to repository
 */
public class ImportCommand implements ISVNCommand {

    private ISVNRemoteFolder folder;
    private File dir;
    String comment;
    boolean recurse;
    

    public ImportCommand(ISVNRemoteFolder folder, File dir, String comment, boolean recurse) {
        super();
        this.folder = folder;
        this.dir = dir;
        this.comment = comment;
        this.recurse = recurse;
    }

	/* (non-Javadoc)
	 * @see org.tigris.subversion.subclipse.core.commands.ISVNCommand#run(org.eclipse.core.runtime.IProgressMonitor)
	 */
	public void run(IProgressMonitor monitor) throws SVNException {       
		final IProgressMonitor subPm = Policy.infiniteSubMonitorFor(monitor, 100);
		ISVNClientAdapter svnClient = null;
        try {
    		subPm.beginTask(null, Policy.INFINITE_PM_GUESS_FOR_SWITCH);
            svnClient = folder.getRepository().getSVNClient();
            OperationManager.getInstance().beginOperation(svnClient, new OperationProgressNotifyListener(subPm, svnClient));
			svnClient.doImport(dir, folder.getUrl(), comment, recurse);
        } catch (SVNClientException e) {
            throw SVNException.wrapException(e);
        } finally {
        	folder.getRepository().returnSVNClient(svnClient);
            OperationManager.getInstance().endOperation();
            subPm.done();
        }
	}

}
