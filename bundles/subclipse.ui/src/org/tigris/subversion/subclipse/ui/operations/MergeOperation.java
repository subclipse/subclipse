/*******************************************************************************
 * Copyright (c) 2004, 2006 Subclipse project and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Subclipse project committers - initial API and implementation
 ******************************************************************************/
package org.tigris.subversion.subclipse.ui.operations;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.ui.IWorkbenchPart;
import org.tigris.subversion.subclipse.core.SVNException;
import org.tigris.subversion.subclipse.core.SVNTeamProvider;
import org.tigris.subversion.subclipse.core.commands.MergeCommand;
import org.tigris.subversion.subclipse.ui.Policy;
import org.tigris.subversion.svnclientadapter.SVNRevision;
import org.tigris.subversion.svnclientadapter.SVNUrl;

public class MergeOperation extends RepositoryProviderOperation {
    private SVNUrl svnUrl1;
    private SVNUrl svnUrl2;
    
    private SVNRevision svnRevision1;
    private SVNRevision svnRevision2;
    
    private boolean force = false;
    private boolean ignoreAncestry = false;
    private boolean recurse = true;

    public MergeOperation(IWorkbenchPart part, IResource[] resources, SVNUrl svnUrl1, SVNRevision svnRevision1, SVNUrl svnUrl2, SVNRevision svnRevision2) {
        super(part, resources);
        this.svnUrl1 = svnUrl1;
        this.svnRevision1 = svnRevision1;
        this.svnUrl2 = svnUrl2;
        this.svnRevision2 = svnRevision2;         
    }
    
    protected String getTaskName() {
        return Policy.bind("MergeOperation.taskName"); //$NON-NLS-1$;
    }

    protected String getTaskName(SVNTeamProvider provider) {
        return Policy.bind("MergeOperation.0", provider.getProject().getName()); //$NON-NLS-1$              
    }

    protected void execute(SVNTeamProvider provider, IResource[] resources, IProgressMonitor monitor) throws SVNException, InterruptedException {
        monitor.beginTask(null, 100);
		try {			
	    	MergeCommand command = new MergeCommand(provider.getSVNWorkspaceRoot(),resources[0], svnUrl1, svnRevision1, svnUrl2, svnRevision2);
	        command.setForce(force);
	    	command.setIgnoreAncestry(ignoreAncestry);
	    	command.setRecurse(recurse);
	    	command.run(Policy.subMonitorFor(monitor,1000));
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
    
	public void setForce(boolean force) {
		this.force = force;
	}

	public void setIgnoreAncestry(boolean ignoreAncestry) {
		this.ignoreAncestry = ignoreAncestry;
	}

	public void setRecurse(boolean recurse) {
		this.recurse = recurse;
	}

}
