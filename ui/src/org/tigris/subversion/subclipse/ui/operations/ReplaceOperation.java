/******************************************************************************
 * This program and the accompanying materials are made available under
 * the terms of the Common Public License v1.0 which accompanies this
 * distribution, and is available at the following URL:
 * http://www.eclipse.org/legal/cpl-v10.html
 * Copyright(c) 2003-2005 by the authors indicated in the @author tags.
 *
 * All Rights are Reserved by the various authors.
 *******************************************************************************/
package org.tigris.subversion.subclipse.ui.operations;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.ui.IWorkbenchPart;
import org.tigris.subversion.subclipse.core.ISVNLocalResource;
import org.tigris.subversion.subclipse.core.SVNException;
import org.tigris.subversion.subclipse.core.SVNTeamProvider;
import org.tigris.subversion.subclipse.core.resources.SVNWorkspaceRoot;
import org.tigris.subversion.subclipse.ui.Policy;
import org.tigris.subversion.svnclientadapter.SVNRevision;

/**
 * @author Panagiotis K
 */
public class ReplaceOperation extends UpdateOperation {
	
    private final SVNRevision revision;


	/**
     * @param part
     * @param resources
     */
    public ReplaceOperation(IWorkbenchPart part, IResource[] resources, SVNRevision revision, boolean recursive) {
        super(part, resources, revision, recursive);
		this.revision = revision;
    }

    /**
     * @param part
     * @param resource
     */
    public ReplaceOperation(IWorkbenchPart part, IResource resource, SVNRevision revision) {
        super(part, resource, revision);
        this.revision = revision;
    }

    /* (non-Javadoc)
	 * @see org.eclipse.team.internal.ccvs.ui.operations.CVSOperation#getTaskName()
	 */
	protected String getTaskName() {
		return Policy.bind("ReplaceOperation.taskName"); //$NON-NLS-1$;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.internal.ccvs.ui.operations.RepositoryProviderOperation#getTaskName(org.eclipse.team.internal.ccvs.core.CVSTeamProvider)
	 */
	protected String getTaskName(SVNTeamProvider provider) {
		return Policy.bind("ReplaceOperation.0", provider.getProject().getName()); //$NON-NLS-1$
	}


    /* (non-Javadoc)
     * @see org.tigris.subversion.subclipse.ui.operations.RepositoryProviderOperation#execute(org.tigris.subversion.subclipse.core.SVNTeamProvider, org.eclipse.core.resources.IResource[], org.eclipse.core.runtime.IProgressMonitor)
     */
    protected void execute(SVNTeamProvider provider, IResource[] resources, IProgressMonitor monitor) throws SVNException, InterruptedException {
        monitor.beginTask(null, 100);
		try {
            // first we revert to base (otherwise it will do a merge instead of
            // replace resources)
		    for (int i = 0; i < resources.length; i++) {
                IResource resource = resources[i];

                ISVNLocalResource localResource = SVNWorkspaceRoot.getSVNResourceFor(resource);
    			if (localResource.isDirty()) {
    				localResource.revert();
    			}
            }

		    // We are already at the base revision after a revert, no need to update
		    if (this.revision.equals(SVNRevision.BASE)) {
		    	return;
		    }
		    
            // then we update to revision
		    super.execute(provider, resources, monitor);
		} catch (SVNException e) {
		    collectStatus(e.getStatus());
		} finally {
			monitor.done();
		}
    }
}
