/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 * 	   Korros Panagiotis - pkorros@tigris.org
 *******************************************************************************/
package org.tigris.subversion.subclipse.core.sync;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceStatus;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.team.core.RepositoryProvider;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.core.TeamStatus;
import org.eclipse.team.core.subscribers.ISubscriberChangeEvent;
import org.eclipse.team.core.subscribers.Subscriber;
import org.eclipse.team.core.subscribers.SubscriberChangeEvent;
import org.eclipse.team.core.synchronize.SyncInfo;
import org.eclipse.team.core.variants.IResourceVariantComparator;
import org.eclipse.team.core.variants.ResourceVariantByteStore;
import org.eclipse.team.core.variants.SessionResourceVariantByteStore;
import org.tigris.subversion.subclipse.core.IResourceStateChangeListener;
import org.tigris.subversion.subclipse.core.ISVNLocalResource;
import org.tigris.subversion.subclipse.core.Policy;
import org.tigris.subversion.subclipse.core.SVNProviderPlugin;
import org.tigris.subversion.subclipse.core.client.StatusAndInfoCommand;
import org.tigris.subversion.subclipse.core.resources.LocalResourceStatus;
import org.tigris.subversion.subclipse.core.resources.RemoteResourceStatus;
import org.tigris.subversion.subclipse.core.resources.SVNWorkspaceRoot;
import org.tigris.subversion.svnclientadapter.ISVNClientAdapter;
import org.tigris.subversion.svnclientadapter.SVNClientException;

public class SVNWorkspaceSubscriber extends Subscriber implements IResourceStateChangeListener {

	private static SVNWorkspaceSubscriber instance; 
	
	/**
	 * Return the file system subscriber singleton.
	 * @return the file system subscriber singleton.
	 */
	public static synchronized SVNWorkspaceSubscriber getInstance() {
		if (instance == null) {
			instance = new SVNWorkspaceSubscriber();
		}
		return instance;
	}

	protected SVNRevisionComparator comparator = new SVNRevisionComparator();

	protected ResourceVariantByteStore remoteSyncStateStore = new SessionResourceVariantByteStore();

	private SVNWorkspaceSubscriber() {
	    SVNProviderPlugin.addResourceStateChangeListener(this);
	}

    /* (non-Javadoc)
     * @see org.eclipse.team.core.subscribers.Subscriber#getResourceComparator()
     */
    public IResourceVariantComparator getResourceComparator() {
        return comparator;
    }

    /* (non-Javadoc)
     * @see org.eclipse.team.core.subscribers.Subscriber#getName()
     */
    public String getName() {
        return "SVNStatusSubscriber"; //$NON-NLS-1$
    }

    /* (non-Javadoc)
     * @see org.eclipse.team.core.subscribers.Subscriber#roots()
     */
    public IResource[] roots() {
		List result = new ArrayList();
		IProject[] projects = ResourcesPlugin.getWorkspace().getRoot().getProjects();
		for (int i = 0; i < projects.length; i++) {
			IProject project = projects[i];
			if(project.isAccessible()) {
				RepositoryProvider provider = RepositoryProvider.getProvider(project, SVNProviderPlugin.PROVIDER_ID);
				if(provider != null) {
					result.add(project);
				}
			}
		}
		return (IProject[]) result.toArray(new IProject[result.size()]);
    }

    /* (non-Javadoc)
     * @see org.eclipse.team.core.subscribers.Subscriber#isSupervised(org.eclipse.core.resources.IResource)
     */
    public boolean isSupervised(IResource resource) throws TeamException {
		try {
			RepositoryProvider provider = RepositoryProvider.getProvider(resource.getProject(), SVNProviderPlugin.getTypeId());
			if (provider == null) return false;
			// TODO: what happens for resources that don't exist?
			// TODO: is it proper to use ignored here?
			ISVNLocalResource svnThing = SVNWorkspaceRoot.getSVNResourceFor(resource);
			if (svnThing.isIgnored()) {
				// An ignored resource could have an incoming addition (conflict)
				return false;//getRemoteTree().hasResourceVariant(resource);
			}
			return true;
		} catch (TeamException e) {
			// If there is no resource in coe this measn there is no local and no remote
			// so the resource is not supervised.
			if (e.getStatus().getCode() == IResourceStatus.RESOURCE_NOT_FOUND) {
				return false;
			}
			throw e;
		}
    }

    /* (non-Javadoc)
     * @see org.eclipse.team.core.subscribers.Subscriber#members(org.eclipse.core.resources.IResource)
     */
    public IResource[] members(IResource resource) throws TeamException {
		if(resource.getType() == IResource.FILE) {
			return new IResource[0];
		}	
		try {
			Set allMembers = new HashSet();
			try {
				allMembers.addAll(Arrays.asList(((IContainer)resource).members(true)));
			} catch (CoreException e) {
				if (e.getStatus().getCode() == IResourceStatus.RESOURCE_NOT_FOUND) {
					// The resource is no longer exists so ignore the exception
				} else {
					throw e;
				}
			}
			//add remote changed resources (they may not exist locally)
			allMembers.addAll(Arrays.asList( remoteSyncStateStore.members( resource ) ) );

			return (IResource[]) allMembers.toArray(new IResource[allMembers.size()]);
		} catch (CoreException e) {
			throw TeamException.asTeamException(e);
		}
    }

	/* (non-Javadoc)
     * @see org.eclipse.team.core.subscribers.Subscriber#getSyncInfo(org.eclipse.core.resources.IResource)
     */
    public SyncInfo getSyncInfo(IResource resource) throws TeamException {
        if( ! isSupervised( resource ) )
            return null;
        
        //LocalResourceStatus localStatus = SVNWorkspaceRoot.getSVNResourceFor( resource );
        LocalResourceStatus localStatus = SVNProviderPlugin.getPlugin().getStatusCacheManager().getStatus(resource);

        RemoteResourceStatus remoteStatusInfo = null;
        byte[] remoteBytes = remoteSyncStateStore.getBytes( resource );
        if( remoteBytes != null )
            remoteStatusInfo = RemoteResourceStatus.fromBytes(remoteBytes);

        SyncInfo syncInfo = new SVNStatusSyncInfo(resource, localStatus, remoteStatusInfo, comparator);
        syncInfo.init();

        return syncInfo;
    }

    /* (non-Javadoc)
     * @see org.eclipse.team.core.subscribers.Subscriber#refresh(org.eclipse.core.resources.IResource[], int, org.eclipse.core.runtime.IProgressMonitor)
     */
    public void refresh(IResource[] resources, int depth, IProgressMonitor monitor) throws TeamException {
		monitor = Policy.monitorFor(monitor);
		List errors = new ArrayList();
		try {
			monitor.beginTask("", 1000 * resources.length);
			for (int i = 0; i < resources.length; i++) {
				IResource resource = resources[i];

				monitor.subTask(resource.getName());
				IStatus status = refresh(resource, depth, monitor);
				if (!status.isOK()) {
					errors.add(status);
				}
			}
		} finally {
			monitor.done();
		} 
		if (!errors.isEmpty()) {
			int numSuccess = resources.length - errors.size();
			throw new TeamException(new MultiStatus(SVNProviderPlugin.ID, 0, 
					(IStatus[]) errors.toArray(new IStatus[errors.size()]), 
					Policy.bind("SVNWorkspaceSubscriber.errorWhileSynchronizing.1", new Object[] {getName(), Integer.toString(numSuccess), Integer.toString(resources.length)}), null)); //$NON-NLS-1$
		}
    }
	
	private IStatus refresh(IResource resource, int depth, IProgressMonitor monitor) {
		try {
			monitor.setTaskName(Policy.bind("SVNWorkspaceSubscriber.refreshingSynchronizationData"));
			SVNProviderPlugin.getPlugin().getStatusCacheManager().refreshStatus(resource, IResource.DEPTH_INFINITE);
			monitor.worked(300);

			monitor.setTaskName(Policy.bind("SVNWorkspaceSubscriber.retrievingSynchronizationData"));
			IResource[] changedResources = findChanges(resource, depth);
			monitor.worked(400);

			fireTeamResourceChange(SubscriberChangeEvent.asSyncChangedDeltas(this, changedResources));
			monitor.worked(300);
			return Status.OK_STATUS;
		} catch (TeamException e) {
			return new TeamStatus(IStatus.ERROR, SVNProviderPlugin.ID, 0, Policy.bind("SVNWorkspaceSubscriber.errorWhileSynchronizing.2", resource.getFullPath().toString(), e.getMessage()), e, resource); //$NON-NLS-1$
		} 
	}

    private IResource[] findChanges(IResource resource, int depth) throws TeamException {
        IWorkspace workspace = ResourcesPlugin.getWorkspace();
        IWorkspaceRoot workspaceRoot = workspace.getRoot();
        
        remoteSyncStateStore.flushBytes(resource, depth);

        ISVNClientAdapter client = SVNProviderPlugin.getPlugin().createSVNClient();

        boolean descend = (depth == IResource.DEPTH_INFINITE)? true : false;
        try {
            StatusAndInfoCommand cmd = new StatusAndInfoCommand(SVNWorkspaceRoot.getSVNResourceFor( resource ), descend, false, true );
            cmd.execute( client );

            RemoteResourceStatus[] statuses = cmd.getRemoteResourceStatuses();

            IResource[] result = new IResource[statuses.length];
            for (int i = 0; i < statuses.length; i++) {
            	result[i] = statuses[i].getResource();
				
                if (isSupervised(result[i]))
                {
                	remoteSyncStateStore.setBytes( statuses[i].getResource(), statuses[i].getBytes() );
                }
			}
            
            return result;
        } catch (SVNClientException e) {
            throw new TeamException("Error getting status for resource " + resource + " " + e.getMessage(), e);
        }
    }

    /* (non-Javadoc)
     * @see org.tigris.subversion.subclipse.core.IResourceStateChangeListener#resourceSyncInfoChanged(org.eclipse.core.resources.IResource[])
     */
    public void resourceSyncInfoChanged(IResource[] changedResources) {
        internalResourceChanged(changedResources);
    }

    /* (non-Javadoc)
     * @see org.tigris.subversion.subclipse.core.IResourceStateChangeListener#resourceModified(org.eclipse.core.resources.IResource[])
     */
    public void resourceModified(IResource[] changedResources) {
        internalResourceChanged(changedResources);
    }

	/**
     * @param changedResources
     */
    private void internalResourceChanged(IResource[] changedResources) {
        fireTeamResourceChange(SubscriberChangeEvent.asSyncChangedDeltas(this, changedResources));
    }

    /* (non-Javadoc)
	 * @see org.tigris.subversion.subclipse.core.IResourceStateChangeListener#projectConfigured(org.eclipse.core.resources.IProject)
	 */
	public void projectConfigured(IProject project) {
		SubscriberChangeEvent delta = new SubscriberChangeEvent(this, ISubscriberChangeEvent.ROOT_ADDED, project);
		fireTeamResourceChange(new SubscriberChangeEvent[] {delta});
	}

	/* (non-Javadoc)
	 * @see org.tigris.subversion.subclipse.core.IResourceStateChangeListener#projectDeconfigured(org.eclipse.core.resources.IProject)
	 */
	public void projectDeconfigured(IProject project) {
		SubscriberChangeEvent delta = new SubscriberChangeEvent(this, ISubscriberChangeEvent.ROOT_REMOVED, project);
		fireTeamResourceChange(new SubscriberChangeEvent[] {delta});
	}
	
	public void updateRemote(IResource[] resources) throws TeamException {
	    for (int i = 0; i < resources.length; i++) {
	        remoteSyncStateStore.flushBytes(resources[i], IResource.DEPTH_INFINITE);
	    }
	}
}
