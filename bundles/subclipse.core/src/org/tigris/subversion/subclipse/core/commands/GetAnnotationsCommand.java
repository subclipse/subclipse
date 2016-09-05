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
package org.tigris.subversion.subclipse.core.commands;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.team.core.TeamException;
import org.tigris.subversion.subclipse.core.ISVNRemoteFile;
import org.tigris.subversion.subclipse.core.Policy;
import org.tigris.subversion.subclipse.core.SVNException;
import org.tigris.subversion.svnclientadapter.ISVNAnnotations;
import org.tigris.subversion.svnclientadapter.SVNRevision;

/**
 * Get the svn blame for the specified resource 
 *
 * @author Martin
 */
public class GetAnnotationsCommand implements ISVNCommand {

    private ISVNAnnotations annotations;
    private final SVNRevision fromRevision;
    private final SVNRevision toRevision;
    private final ISVNRemoteFile remoteFile;
    private final boolean includeMergedRevisions;
    private final boolean ignoreMimeType;
  
    /**
     * Constructor
     * @param remoteFile
     * @param fromRevision
     * @param toRevision
     * @param includeMergedRevisions
     * @param ignoreMimeType
     */
    public GetAnnotationsCommand(ISVNRemoteFile remoteFile, SVNRevision fromRevision, SVNRevision toRevision, boolean includeMergedRevisions, boolean ignoreMimeType) {
        this.remoteFile = remoteFile;
        this.fromRevision = fromRevision;
        this.toRevision = toRevision;
        this.includeMergedRevisions = includeMergedRevisions;
        this.ignoreMimeType = ignoreMimeType;
    }
    
    /* (non-Javadoc)
     * @see org.tigris.subversion.subclipse.core.commands.ISVNCommand#run(org.eclipse.core.runtime.IProgressMonitor)
     */
    public void run(IProgressMonitor aMonitor) throws SVNException {
		IProgressMonitor monitor = Policy.monitorFor(aMonitor);
		monitor.beginTask(Policy.bind("RemoteFile.getAnnotations"), 100);//$NON-NLS-1$
        try {        	
            annotations = remoteFile.getAnnotations(fromRevision, toRevision, includeMergedRevisions, ignoreMimeType);
            monitor.worked(100);
        } catch (TeamException e) {
            throw SVNException.wrapException(e);
        } finally {
            monitor.done();
        }
    }
    
    /**
     * @return the annotations retrieved for the specified resource
     */
    public ISVNAnnotations getAnnotations() {
        return annotations;
    }

}
