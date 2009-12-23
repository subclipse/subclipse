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
import org.tigris.subversion.subclipse.core.ISVNLocalResource;
import org.tigris.subversion.subclipse.core.SVNException;
import org.tigris.subversion.subclipse.core.SVNTeamProvider;
import org.tigris.subversion.subclipse.core.commands.AddIgnoredPatternCommand;
import org.tigris.subversion.subclipse.core.resources.SVNWorkspaceRoot;
import org.tigris.subversion.subclipse.ui.Policy;
import org.tigris.subversion.subclipse.ui.dialogs.IgnoreResourcesDialog;

public class IgnoreOperation extends RepositoryProviderOperation {

    private final IgnoreResourcesDialog dialog;


    public IgnoreOperation(IWorkbenchPart part, IResource[] resources, IgnoreResourcesDialog dialog) {
        super(part, resources);
        this.dialog = dialog;
    }

    /* (non-Javadoc)
     * @see org.eclipse.team.internal.ccvs.ui.operations.CVSOperation#getTaskName()
     */
    protected String getTaskName() {
        return Policy.bind("IgnoreOperation.taskName"); //$NON-NLS-1$;
    }
    
    /* (non-Javadoc)
     * @see org.eclipse.team.internal.ccvs.ui.operations.RepositoryProviderOperation#getTaskName(org.eclipse.team.internal.ccvs.core.CVSTeamProvider)
     */
    protected String getTaskName(SVNTeamProvider provider) {
        return Policy.bind("IgnoreOperation.0", provider.getProject().getName()); //$NON-NLS-1$
    }


    /* (non-Javadoc)
     * @see org.tigris.subversion.subclipse.ui.operations.RepositoryProviderOperation#execute(org.tigris.subversion.subclipse.core.SVNTeamProvider, org.eclipse.core.resources.IResource[], org.eclipse.core.runtime.IProgressMonitor)
     */
    protected void execute(SVNTeamProvider provider, IResource[] resources, IProgressMonitor monitor) throws SVNException, InterruptedException {
        try {
            for (int i = 0; i < resources.length; i++) {
                IResource resource = resources[i];
                String pattern = dialog.getIgnorePatternFor(resource);
                ISVNLocalResource svnResource = SVNWorkspaceRoot.getSVNResourceFor(resource);
                new AddIgnoredPatternCommand(svnResource.getParent(), pattern).run(monitor);
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
