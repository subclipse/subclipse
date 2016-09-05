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
package org.tigris.subversion.subclipse.core.sync;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceStatus;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Preferences.IPropertyChangeListener;
import org.eclipse.core.runtime.Preferences.PropertyChangeEvent;
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
import org.tigris.subversion.subclipse.core.CancelableSVNStatusCallback;
import org.tigris.subversion.subclipse.core.IResourceStateChangeListener;
import org.tigris.subversion.subclipse.core.ISVNCoreConstants;
import org.tigris.subversion.subclipse.core.ISVNLocalResource;
import org.tigris.subversion.subclipse.core.Policy;
import org.tigris.subversion.subclipse.core.SVNException;
import org.tigris.subversion.subclipse.core.SVNProviderPlugin;
import org.tigris.subversion.subclipse.core.SVNTeamProvider;
import org.tigris.subversion.subclipse.core.client.StatusAndInfoCommand;
import org.tigris.subversion.subclipse.core.resources.LocalResourceStatus;
import org.tigris.subversion.subclipse.core.resources.RemoteResourceStatus;
import org.tigris.subversion.subclipse.core.resources.SVNWorkspaceRoot;
import org.tigris.subversion.subclipse.core.util.Util;
import org.tigris.subversion.svnclientadapter.SVNStatusKind;

public class SVNWorkspaceSubscriber extends Subscriber implements IResourceStateChangeListener, IPropertyChangeListener {

	private static SVNWorkspaceSubscriber instance; 
	private HashMap<IResource, IResource[]> changesMap = new HashMap<IResource, IResource[]>();
	
	private boolean ignoreHiddenChanges = SVNProviderPlugin.getPlugin().getPluginPreferences().getBoolean(ISVNCoreConstants.PREF_IGNORE_HIDDEN_CHANGES);
	
	/** We need to store unchanged parents in remoteSyncStateStore.
	 * To distinguish them from real changed resources we store this dummy data instead for them */
	private static final byte[] DUMMY_SYNC_BYTES = new byte[] {-1, -2, -3, -4};
	
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
	    SVNProviderPlugin.getPlugin().getPluginPreferences().addPropertyChangeListener(this);
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
		List<IProject> result = new ArrayList<IProject>();
		IProject[] projects = ResourcesPlugin.getWorkspace().getRoot().getProjects();
		for (IProject project : projects) {
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
			if (resource.isTeamPrivateMember() || SVNWorkspaceRoot.isLinkedResource(resource)) return false;
			RepositoryProvider provider = RepositoryProvider.getProvider(resource.getProject(), SVNProviderPlugin.getTypeId());
			if (provider == null) return false;
			// TODO: what happens for resources that don't exist?
			// TODO: is it proper to use ignored here?
			ISVNLocalResource svnThing = SVNWorkspaceRoot.getSVNResourceFor(resource);
			if (svnThing.isIgnored()) {
				// An ignored resource could have an incoming addition (conflict)
				return (remoteSyncStateStore.getBytes(resource) != null) || 
						((remoteSyncStateStore.members(resource) != null) && (remoteSyncStateStore.members(resource).length > 0));
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
		if ((resource.getType() == IResource.FILE) || (!isSupervised(resource))){
			return new IResource[0];
		}	
		try {
			Set<IResource> allMembers = new HashSet<IResource>();
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

			return allMembers.toArray(new IResource[allMembers.size()]);
		} catch (CoreException e) {
			throw TeamException.asTeamException(e);
		}
    }

	/* (non-Javadoc)
     * @see org.eclipse.team.core.subscribers.Subscriber#getSyncInfo(org.eclipse.core.resources.IResource)
     */
    public SyncInfo getSyncInfo(IResource resource) throws TeamException {
        if (resource == null)
        	return null;
    	if( ! isSupervised( resource ) )
            return null;
    	
    	if (ignoreHiddenChanges && Util.isHidden(resource)) {
    		return null;
    	}
    	
        //LocalResourceStatus localStatus = SVNWorkspaceRoot.getSVNResourceFor( resource );
        LocalResourceStatus localStatus = SVNProviderPlugin.getPlugin().getStatusCacheManager().getStatus(resource);

        RemoteResourceStatus remoteStatusInfo = null;
        byte[] remoteBytes = remoteSyncStateStore.getBytes( resource );
        if ((remoteBytes != null) && (remoteBytes != DUMMY_SYNC_BYTES)) {
            remoteStatusInfo = RemoteResourceStatus.fromBytes(remoteBytes);
        }

        SyncInfo syncInfo = new SVNStatusSyncInfo(resource, localStatus, remoteStatusInfo, comparator);
        syncInfo.init();

        return syncInfo;
    }

    /* (non-Javadoc)
     * @see org.eclipse.team.core.subscribers.Subscriber#refresh(org.eclipse.core.resources.IResource[], int, org.eclipse.core.runtime.IProgressMonitor)
     */
    public void refresh(IResource[] resources, int depth, IProgressMonitor monitor) throws TeamException {
		monitor = Policy.monitorFor(monitor);
		List<IStatus> errors = new ArrayList<IStatus>();
		try {
			monitor.beginTask("", 1000 * resources.length);
			for (IResource resource : resources) {
				// Make certain that resource is still connected with SVN.  When
				// Synch is on a schedule it is possible for the project to become disconnected
				SVNTeamProvider teamProvider = (SVNTeamProvider)RepositoryProvider.getProvider(resource.getProject(), SVNProviderPlugin.getTypeId());
				if (teamProvider != null) {
					monitor.subTask(resource.getName());
					IStatus status = refresh(resource, depth, monitor);
					if (!status.isOK()) {
						errors.add(status);
					}
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
//			monitor.setTaskName(Policy.bind("SVNWorkspaceSubscriber.refreshingSynchronizationData", resource.getFullPath().toString()));
			monitor.worked(100);
//			SVNProviderPlugin.getPlugin().getStatusCacheManager().refreshStatus(resource, IResource.DEPTH_INFINITE);
//			monitor.worked(300);

			monitor.setTaskName(Policy.bind("SVNWorkspaceSubscriber.retrievingSynchronizationData"));
			IResource[] lastChangedResources = (IResource[])changesMap.get(resource);
			IResource[] changedResources = findChanges(resource, depth, Policy.infiniteSubMonitorFor(monitor, 400));
			changesMap.put(resource, changedResources);
			fireTeamResourceChange(SubscriberChangeEvent.asSyncChangedDeltas(this, changedResources));
			if (lastChangedResources != null && lastChangedResources.length > 0) fireTeamResourceChange(SubscriberChangeEvent.asSyncChangedDeltas(this, lastChangedResources));
			monitor.worked(400);
			return Status.OK_STATUS;
		} catch (TeamException e) {
			return new TeamStatus(IStatus.ERROR, SVNProviderPlugin.ID, 0, Policy.bind("SVNWorkspaceSubscriber.errorWhileSynchronizing.2", resource.getFullPath().toString(), e.getMessage()), e, resource); //$NON-NLS-1$
		} 
	}

    private IResource[] findChanges(IResource resource, int depth, IProgressMonitor monitor) throws TeamException {
        try {
        	monitor.beginTask("", 100);

        	remoteSyncStateStore.flushBytes(resource, depth);

//            ISVNClientAdapter client = SVNProviderPlugin.getPlugin().createSVNClient();
        	
            boolean descend = (depth == IResource.DEPTH_INFINITE)? true : false;
            boolean showOutOfDate = SVNProviderPlugin.getPlugin().getPluginPreferences().getBoolean(ISVNCoreConstants.PREF_SHOW_OUT_OF_DATE_FOLDERS);
            StatusAndInfoCommand cmd = new StatusAndInfoCommand(SVNWorkspaceRoot.getSVNResourceFor( resource ), descend, showOutOfDate, true );
            cmd.setCallback(new CancelableSVNStatusCallback(monitor));
            cmd.run(monitor);
            
            if (monitor.isCanceled()) {
            	return new IResource[0];
            }
            
            monitor.worked(70);

            RemoteResourceStatus[] statuses = cmd.getRemoteResourceStatuses();

            List<IResource> result = new ArrayList<IResource>(statuses.length);
            for (RemoteResourceStatus status : statuses) {
            	IResource changedResource = SVNWorkspaceRoot.getResourceFor(resource, status);

                if (changedResource == null)
                	continue;

                if (isSupervised(changedResource) || (status.getTextStatus() != SVNStatusKind.NONE))
                {
                	if (!ignoreHiddenChanges || !Util.isHidden(changedResource)) {
	                	result.add(changedResource);
	                	remoteSyncStateStore.setBytes( changedResource, status.getBytes() );
	                	registerChangedResourceParent(changedResource);
                	}
                }
			}
        	// Ensure that the local sync state is also updated
            IContainer container = resource.getType() == IResource.FILE ? resource.getParent() : (IContainer)resource;
        	SVNProviderPlugin.getPlugin().getStatusCacheManager().refreshStatus(container, true);

            monitor.worked(30);            
            return (IResource[]) result.toArray(new IResource[result.size()]);
        } catch (SVNException e) {
        	if (e.getMessage().contains("Operation cancelled")) {
        		return new IResource[0];
        	}
        	else {
        		throw new TeamException("Error getting status for resource " + resource + " " + e.getMessage(), e);
        	}
        } finally {
        	monitor.done();
		}
    }

    /**
     * SessionResourceVariantByteStore of remoteSyncStateStore used to store (and flush) sync changes
     * register only direct parents of the changed resources.
     * If we want to be able to flush arbitrary subtree from the remoteSyncStateStore (event subtree which root
     * is unchanged resource), we have to cache all parent of the changed resources up to the top.
     * These sync DUMMY_SYNC_BYTES are stored as synch info, so upon this dummy bytes we then filter out
     * the actually unchanged sync data from the cache 
     * @param changedResource
     */
    private void registerChangedResourceParent(IResource changedResource) throws TeamException
    {
    	IContainer parent = changedResource.getParent();
    	if (parent == null) return;
    	if (remoteSyncStateStore.getBytes(parent) == null)
    	{
    		remoteSyncStateStore.setBytes(parent, DUMMY_SYNC_BYTES);
    		registerChangedResourceParent(parent);
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
	
	public void initialize() {};
	
	public void updateRemote(IResource[] resources) throws TeamException {
	    for (int i = 0; i < resources.length; i++) {
	        remoteSyncStateStore.flushBytes(resources[i], IResource.DEPTH_INFINITE);
	    }
	}

	public void propertyChange(PropertyChangeEvent event) {
		if (event.getProperty().equals(ISVNCoreConstants.PREF_IGNORE_HIDDEN_CHANGES)) {
			ignoreHiddenChanges = SVNProviderPlugin.getPlugin().getPluginPreferences().getBoolean(ISVNCoreConstants.PREF_IGNORE_HIDDEN_CHANGES);
		}
	}
}
