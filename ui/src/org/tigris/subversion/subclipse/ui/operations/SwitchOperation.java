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

import java.util.HashMap;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.ui.IWorkbenchPart;
import org.tigris.subversion.subclipse.core.SVNException;
import org.tigris.subversion.subclipse.core.SVNTeamProvider;
import org.tigris.subversion.subclipse.core.commands.SwitchToUrlCommand;
import org.tigris.subversion.subclipse.ui.Policy;
import org.tigris.subversion.svnclientadapter.SVNRevision;
import org.tigris.subversion.svnclientadapter.SVNUrl;

public class SwitchOperation extends RepositoryProviderOperation {
    private SVNRevision svnRevision;
    private HashMap urlMap = new HashMap();
    
    public SwitchOperation(IWorkbenchPart part, IResource[] resources, SVNUrl[] svnUrls, SVNRevision svnRevision) {
        super(part, resources);
        this.svnRevision = svnRevision;
        for (int i = 0; i < resources.length; i++) 
        	urlMap.put(resources[i], svnUrls[i]);
    }
    
    protected String getTaskName() {
        return Policy.bind("SwitchOperation.taskName"); //$NON-NLS-1$;
    }

    protected String getTaskName(SVNTeamProvider provider) {
        return Policy.bind("SwitchOperation.0", provider.getProject().getName()); //$NON-NLS-1$       
    }

    protected void execute(SVNTeamProvider provider, IResource[] resources, IProgressMonitor monitor) throws SVNException, InterruptedException {
        monitor.beginTask("Switch to Branch/Tag", resources.length);
		try {
			for (int i = 0; i < resources.length; i++) {
				monitor.subTask("Switching " + resources[i].getName() + ". . .");
				SVNUrl svnUrl = (SVNUrl)urlMap.get(resources[i]);
		    	SwitchToUrlCommand command = new SwitchToUrlCommand(provider.getSVNWorkspaceRoot(),resources[i], svnUrl, svnRevision);
		        command.run(monitor);
		        monitor.worked(1);
			}
		} catch (SVNException e) {
		    collectStatus(e.getStatus());
		} finally {
			monitor.done();
		}  
    }

}
