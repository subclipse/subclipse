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

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.ui.IWorkbenchPart;
import org.tigris.subversion.subclipse.core.SVNException;
import org.tigris.subversion.subclipse.core.SVNTeamProvider;
import org.tigris.subversion.subclipse.core.commands.ResolveResourcesCommand;
import org.tigris.subversion.subclipse.ui.Policy;

public class ResolveOperation extends RepositoryProviderOperation {
	
	private int resolution; // ISVNConflictResolver.Result

    public ResolveOperation(IWorkbenchPart part, IResource[] resources, int resolution) {
        super(part, resources);
        this.resolution = resolution;
    }

    /* (non-Javadoc)
     * @see org.eclipse.team.internal.ccvs.ui.operations.CVSOperation#getTaskName()
     */
    protected String getTaskName() {
        return Policy.bind("ResolveOperation.taskName"); //$NON-NLS-1$;
    }
    
    /* (non-Javadoc)
     * @see org.eclipse.team.internal.ccvs.ui.operations.RepositoryProviderOperation#getTaskName(org.eclipse.team.internal.ccvs.core.CVSTeamProvider)
     */
    protected String getTaskName(SVNTeamProvider provider) {
        return Policy.bind("ResolveOperation.0", provider.getProject().getName()); //$NON-NLS-1$
    }


    /* (non-Javadoc)
     * @see org.tigris.subversion.subclipse.ui.operations.RepositoryProviderOperation#execute(org.tigris.subversion.subclipse.core.SVNTeamProvider, org.eclipse.core.resources.IResource[], org.eclipse.core.runtime.IProgressMonitor)
     */
    protected void execute(SVNTeamProvider provider, IResource[] resources, IProgressMonitor monitor) throws SVNException, InterruptedException {
        monitor.beginTask(null, 100);
        try {           
            ResolveResourcesCommand command = new ResolveResourcesCommand(provider.getSVNWorkspaceRoot(),resources,resolution);
            command.run(Policy.subMonitorFor(monitor,100));
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
