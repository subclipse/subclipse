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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceStatus;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Path;
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
import org.eclipse.team.internal.core.Policy;
import org.eclipse.team.internal.core.TeamPlugin;
import org.tigris.subversion.subclipse.core.IResourceStateChangeListener;
import org.tigris.subversion.subclipse.core.ISVNLocalResource;
import org.tigris.subversion.subclipse.core.SVNProviderPlugin;
import org.tigris.subversion.subclipse.core.client.StatusCommand;
import org.tigris.subversion.subclipse.core.resources.LocalResourceStatus;
import org.tigris.subversion.subclipse.core.resources.SVNWorkspaceRoot;
import org.tigris.subversion.svnclientadapter.ISVNClientAdapter;
import org.tigris.subversion.svnclientadapter.ISVNStatus;
import org.tigris.subversion.svnclientadapter.SVNClientException;
import org.tigris.subversion.svnclientadapter.SVNNodeKind;
import org.tigris.subversion.svnclientadapter.SVNRevision;
import org.tigris.subversion.svnclientadapter.SVNStatusKind;
import org.tigris.subversion.svnclientadapter.SVNRevision.Number;

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

	public SVNWorkspaceSubscriber() {
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
				allMembers.addAll(Arrays.asList(((IContainer)resource).members()));
			} catch (CoreException e) {
				if (e.getStatus().getCode() == IResourceStatus.RESOURCE_NOT_FOUND) {
					// The resource is no longer exists so ignore the exception
				} else {
					throw e;
				}
			}
			//add remote changed resources (they may not exist locally)
			allMembers.addAll(Arrays.asList( remoteSyncStateStore.members( resource ) ) );

			//TODO: add local changed resources (they may not exist locally)
			//allMembers.addAll(Arrays.asList( localSyncStateStore.members( resource ) ) );

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
        ISVNLocalResource localResource = SVNWorkspaceRoot.getSVNResourceFor(resource);
        LocalResourceStatus localStatus = localResource.getStatus();

        StatusInfo localStatusInfo = new StatusInfo(localStatus.getLastChangedRevision(), localStatus.getTextStatus());

        StatusInfo remoteStatusInfo = null;
        byte[] remoteBytes = remoteSyncStateStore.getBytes( resource );
        if( remoteBytes != null )
            remoteStatusInfo = StatusInfo.fromBytes(remoteBytes);
        else {
            if( localStatus.hasRemote() )
                remoteStatusInfo = new StatusInfo(localStatus.getLastChangedRevision(), SVNStatusKind.NORMAL);
        }

        SyncInfo syncInfo = new SVNStatusSyncInfo(resource, localStatusInfo, remoteStatusInfo, comparator);
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
			monitor.beginTask("Refresing subversion resources", 1000 * resources.length);
			for (int i = 0; i < resources.length; i++) {
				IResource resource = resources[i];

				monitor.subTask(resource.getName());
				IStatus status = refresh(resource, depth);
				if (!status.isOK()) {
					errors.add(status);
				}
				monitor.worked(1000);
			}
		} finally {
			monitor.done();
		} 
		if (!errors.isEmpty()) {
			int numSuccess = resources.length - errors.size();
			throw new TeamException(new MultiStatus(TeamPlugin.ID, 0, 
					(IStatus[]) errors.toArray(new IStatus[errors.size()]), 
					Policy.bind("ResourceVariantTreeSubscriber.1", new Object[] {getName(), Integer.toString(numSuccess), Integer.toString(resources.length)}), null)); //$NON-NLS-1$
		}
    }
	
	private IStatus refresh(IResource resource, int depth) {
		try {
			IResource[] changedResources = findChanges(resource, depth);

			fireTeamResourceChange(SubscriberChangeEvent.asSyncChangedDeltas(this, changedResources));
			return Status.OK_STATUS;
		} catch (TeamException e) {
			return new TeamStatus(IStatus.ERROR, TeamPlugin.ID, 0, Policy.bind("ResourceVariantTreeSubscriber.2", resource.getFullPath().toString(), e.getMessage()), e, resource); //$NON-NLS-1$
		} 
	}

    private IResource[] findChanges(IResource resource, int depth) throws TeamException {
        System.out.println("SVNWorkspaceSubscriber.refresh()"+resource+" "+depth);		

        IWorkspace workspace = ResourcesPlugin.getWorkspace();
        IWorkspaceRoot workspaceRoot = workspace.getRoot();
        
        remoteSyncStateStore.flushBytes(resource, depth);

        ISVNClientAdapter client = SVNProviderPlugin.getPlugin().createSVNClient();

        ISVNLocalResource svnResource = SVNWorkspaceRoot.getSVNResourceFor( resource );
        boolean descend = (depth == IResource.DEPTH_INFINITE)? true : false;
        try {
            List allChanges = new ArrayList();
            Map syncState = new HashMap();
            Set containerSet = new HashSet();

            StatusCommand cmd = new StatusCommand(svnResource.getFile(), descend, false, true );
            cmd.execute( client );

            ISVNStatus[] statuses = cmd.getStatuses(); 
            for (int i = 0; i < statuses.length; i++) {
                ISVNStatus status = statuses[i];
                IPath path = new Path(status.getPath());

                IResource changedResource = null;
                if ( status.getNodeKind() == SVNNodeKind.DIR ) {
                    changedResource = workspaceRoot.getContainerForLocation(path);
                }
                else if ( status.getNodeKind() == SVNNodeKind.FILE ) {
                    changedResource = workspaceRoot.getFileForLocation(path);
                }
                else if ( status.getNodeKind() == SVNNodeKind.UNKNOWN ) {
                    changedResource = workspaceRoot.getContainerForLocation(path);
                    
                    if( changedResource.exists() )
                        containerSet.add( changedResource );
                    else  if( !containerSet.contains( changedResource ) )
                        changedResource = workspaceRoot.getFileForLocation(path);
                }

                if( changedResource != null ) {
                    containerSet.add(changedResource.getParent());
                    
                    StatusInfo remoteInfo = new StatusInfo(cmd.getRevision(), status.getRepositoryTextStatus() );
                    remoteSyncStateStore.setBytes( changedResource, remoteInfo.asBytes() );

                    //System.out.println(cmd.getRevision()+" "+changedResource+" R:"+status.getLastChangedRevision()+" L:"+status.getTextStatus()+" R:"+status.getRepositoryTextStatus());
                    allChanges.add(changedResource);
                }
            }

            return (IResource[]) allChanges.toArray(new IResource[allChanges.size()]);
        } catch (SVNClientException e) {
            throw new TeamException("Error getting status for resource "+resource, e);
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
        for (int i = 0; i < changedResources.length; i++) {
            IResource resource = changedResources[i];
            try {
                if( resource.exists() ) {
                    ISVNLocalResource localResource = SVNWorkspaceRoot.getSVNResourceFor( resource );
                    LocalResourceStatus status = localResource.getStatus();

                    if( remoteSyncStateStore.getBytes( resource ) == null ) {
                        StatusInfo remoteInfo;
                        if( localResource.hasRemote() ) {
                            remoteInfo = new StatusInfo(status.getLastChangedRevision(), SVNStatusKind.NORMAL );
                        }
                        else {
                            remoteInfo = new StatusInfo(null, SVNStatusKind.NONE );
                        }
                        remoteSyncStateStore.setBytes( resource, remoteInfo.asBytes() );
                    }
                }
//TODO: catch outgoing deletions
//                else {
//                    if( remoteSyncStateStore.getBytes( resource ) == null ) {
//                        localSyncStateStore.deleteBytes( resource );
//                    }
//                    else {
//                        StatusInfo localInfo = new StatusInfo(null, SVNStatusKind.NONE );
//                        localSyncStateStore.setBytes( resource,  localInfo.asBytes() );
//                    }
//                }
            } catch (TeamException e) {
                e.printStackTrace();
            }
        }
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

class StatusInfo {
    private final Number revision;
    private final SVNStatusKind kind;

    StatusInfo(SVNRevision.Number revision, SVNStatusKind kind) {
        this.revision = revision;
        this.kind = kind;
    }

    StatusInfo(byte[] fromBytes) {
        String[] segments = new String( fromBytes ).split(";");
        if( segments[0].length() > 0 )
            this.revision = new SVNRevision.Number( Long.parseLong( segments[0] ) );
        else
            this.revision = null;
        this.kind = fromString( segments[1] );
    }

    byte[] asBytes() {
        return new String( ((revision != null) ? revision.toString() : "" ) + ";"+ kind).getBytes();
    }

    public SVNStatusKind getKind() {
        return kind;
    }

    public Number getRevision() {
        return revision;
    }

    private static SVNStatusKind fromString(String kind) {
        if( kind.equals( "non-svn" ) ) {
            return SVNStatusKind.NONE;
        }
        if( kind.equals( "normal" ) ) {
            return SVNStatusKind.NORMAL;
        }
        if( kind.equals( "added" ) ) {
            return SVNStatusKind.ADDED;
        }
        if( kind.equals( "missing" ) ) {
            return SVNStatusKind.MISSING;
        }
        if( kind.equals( "incomplete" ) ) {
            return SVNStatusKind.INCOMPLETE;
        }
        if( kind.equals( "deleted" ) ) {
            return SVNStatusKind.DELETED;
        }
        if( kind.equals( "replaced" ) ) {
            return SVNStatusKind.REPLACED;
        }
        if( kind.equals( "modified" ) ) {
            return SVNStatusKind.MODIFIED;
        }
        if( kind.equals( "merged" ) ) {
            return SVNStatusKind.MERGED;
        }
        if( kind.equals( "conflicted" ) ) {
            return SVNStatusKind.CONFLICTED;
        }
        if( kind.equals( "obstructed" ) ) {
            return SVNStatusKind.OBSTRUCTED;
        }
        if( kind.equals( "ignored" ) ) {
            return SVNStatusKind.IGNORED;
        }
        if( kind.equals( "external" ) ) {
            return SVNStatusKind.EXTERNAL;
        }
        if( kind.equals( "unversioned" ) ) {
            return SVNStatusKind.UNVERSIONED;
        }
        return SVNStatusKind.NONE;
    }

    static StatusInfo fromBytes(byte[] bytes) {
        if( bytes == null )
            return null;
        
        return new StatusInfo( bytes );
    }
}
