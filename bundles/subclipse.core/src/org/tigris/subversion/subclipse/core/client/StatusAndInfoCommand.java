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
package org.tigris.subversion.subclipse.core.client;

import java.util.Arrays;
import java.util.Comparator;

import org.eclipse.core.runtime.IProgressMonitor;
import org.tigris.subversion.subclipse.core.ISVNLocalResource;
import org.tigris.subversion.subclipse.core.Policy;
import org.tigris.subversion.subclipse.core.SVNException;
import org.tigris.subversion.subclipse.core.SVNProviderPlugin;
import org.tigris.subversion.subclipse.core.commands.ISVNCommand;
import org.tigris.subversion.subclipse.core.resources.RemoteResourceStatus;
import org.tigris.subversion.svnclientadapter.ISVNClientAdapter;
import org.tigris.subversion.svnclientadapter.ISVNStatus;
import org.tigris.subversion.svnclientadapter.SVNClientException;
import org.tigris.subversion.svnclientadapter.SVNStatusKind;

/**
 * svn status + subsequent svn info command(s).
 * After execute() of superclass StatusCommand, not all necessary information is retrieved.
 * For added files or resources (incoming additions), their e.g. nodeKind is not known from the status.
 * To get that information, subsequent calls to "svn info" are executed for those unknown urls ...
 * 
 * @author Martin Letenay
 */
public class StatusAndInfoCommand extends StatusCommand implements ISVNCommand {
	
	private RemoteResourceStatus[] remoteStatuses;

    public StatusAndInfoCommand(ISVNLocalResource svnResource, boolean descend, boolean getAll, boolean contactServer) {
        super(svnResource.getFile(), descend, getAll, contactServer);
    }

    protected void execute(final ISVNClientAdapter client, final IProgressMonitor monitor) throws SVNClientException {
        super.execute(client, monitor);
        monitor.worked(50);        
        remoteStatuses = collectRemoteStatuses(getStatuses(), client, Policy.subMonitorFor(monitor,50));
    }

    /* (non-Javadoc)
     * @see org.tigris.subversion.subclipse.core.commands.ISVNCommand#run(org.eclipse.core.runtime.IProgressMonitor)
     */
    public void run(IProgressMonitor monitor) throws SVNException {
    	ISVNClientAdapter svnClient = null;
        try { 
            svnClient = SVNProviderPlugin.getPlugin().getSVNClient();
            execute(svnClient, monitor);
        } catch (SVNClientException e) {
            throw SVNException.wrapException(e);
        } finally {
        	SVNProviderPlugin.getPlugin().getSVNClientManager().returnSVNClient(svnClient);
        }
    }

    /**
     * Answer sorted array of informed statuses ... 
     * @return
     */
    public RemoteResourceStatus[] getRemoteResourceStatuses()
    {
    	return remoteStatuses;
    }

    private RemoteResourceStatus[] collectRemoteStatuses(ISVNStatus[] statuses, ISVNClientAdapter client, final IProgressMonitor monitor)
    {
    	monitor.beginTask("", statuses.length);
    	try {
    	RemoteResourceStatus[] result = new RemoteResourceStatus[statuses.length];

        Arrays.sort(statuses, new Comparator() {
			public int compare(Object o1, Object o2) {
				return ((ISVNStatus) o1).getPath().compareTo(((ISVNStatus) o2).getPath());
			}            	
        });

        for (int i = 0; i < statuses.length; i++) {
        	ISVNStatus status = statuses[i];
        	SVNStatusKind localTextStatus = status.getTextStatus();

        	if (SVNStatusKind.UNVERSIONED.equals(localTextStatus)
        			|| SVNStatusKind.ADDED.equals(localTextStatus)
        			|| SVNStatusKind.IGNORED.equals(localTextStatus)
        		)
        	{
        		if (SVNStatusKind.NONE.equals(status.getRepositoryTextStatus()))
        			result[i] = RemoteResourceStatus.NONE;
        		else
            		result[i] = new RemoteResourceStatus(statuses[i], getRevisionFor(statuses[i]));
        	}
        	else
        	{
        		result[i] = new RemoteResourceStatus(statuses[i], getRevisionFor(statuses[i]));
        	}
        	monitor.worked(1);
        }	
        
        return result;
    	} finally {
    		monitor.done();
    	}
    }

}
