/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.tigris.subversion.subclipse.core.mapping;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.osgi.util.NLS;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.core.diff.IDiff;
import org.eclipse.team.core.mapping.provider.ResourceDiffTree;
import org.eclipse.team.core.subscribers.Subscriber;
import org.eclipse.team.internal.core.BackgroundEventHandler;
import org.eclipse.team.internal.core.Messages;
import org.eclipse.team.internal.core.Policy;
import org.eclipse.team.internal.core.TeamPlugin;
import org.eclipse.team.internal.core.subscribers.ActiveChangeSet;
import org.eclipse.team.internal.core.subscribers.ActiveChangeSetManager;
import org.eclipse.team.internal.core.subscribers.ChangeSet;
import org.osgi.service.prefs.Preferences;

/**
 * This class manages the active change sets associated with a subscriber.
 */
public class SubclipseSubscriberChangeSetManager extends ActiveChangeSetManager {
    
    private static final String PREF_CHANGE_SETS = "changeSets"; //$NON-NLS-1$
    
    private static final int RESOURCE_REMOVAL = 1;
    private static final int RESOURCE_CHANGE = 2;
    
    private EventHandler handler;
    private ResourceCollector collector;
    
    /*
     * Background event handler for serializing and batching change set changes
     */
    private class EventHandler extends BackgroundEventHandler {

        private List dispatchEvents = new ArrayList();
        
        protected EventHandler(String jobName, String errorTitle) {
            super(jobName, errorTitle);
        }

        /* (non-Javadoc)
         * @see org.eclipse.team.internal.core.BackgroundEventHandler#processEvent(org.eclipse.team.internal.core.BackgroundEventHandler.Event, org.eclipse.core.runtime.IProgressMonitor)
         */
        protected void processEvent(Event event, IProgressMonitor monitor) throws CoreException {
            // Handle everything in the dispatch
            if (isShutdown())
                throw new OperationCanceledException();
            dispatchEvents.add(event);
        }
        
        /* (non-Javadoc)
         * @see org.eclipse.team.internal.core.BackgroundEventHandler#doDispatchEvents(org.eclipse.core.runtime.IProgressMonitor)
         */
        protected boolean doDispatchEvents(IProgressMonitor monitor) throws TeamException {
            if (dispatchEvents.isEmpty()) {
                return false;
            }
            if (isShutdown())
                throw new OperationCanceledException();
            ResourceDiffTree[] locked = null;
            try {
                locked = beginDispath();
                for (Iterator iter = dispatchEvents.iterator(); iter.hasNext();) {
                    Event event = (Event) iter.next();
	                switch (event.getType()) {
	                case RESOURCE_REMOVAL:
	                    handleRemove(event.getResource());
	                    break;
	                case RESOURCE_CHANGE:
	                    handleChange(event.getResource(), ((ResourceEvent)event).getDepth());
	                    break;
	                default:
	                    break;
	                }
                    if (isShutdown())
                        throw new OperationCanceledException();
                }
            } catch (CoreException e) {
				throw TeamException.asTeamException(e);
			} finally {
                try {
                    endDispatch(locked, monitor);
                } finally {
                    dispatchEvents.clear();
                }
            }
            return true;
        }

        /*
         * Begin input on all the sets and return the sync sets that were 
         * locked. If this method throws an exception then the client
         * can assume that no sets were locked
         */
        private ResourceDiffTree[] beginDispath() {
            ChangeSet[] sets = getSets();
            List lockedSets = new ArrayList();
            try {
                for (int i = 0; i < sets.length; i++) {
                    ActiveChangeSet set = (ActiveChangeSet)sets[i];
                    ResourceDiffTree tree = (ResourceDiffTree) set.getDiffTree();
                    lockedSets.add(tree);
                    tree.beginInput();
                }
                return (ResourceDiffTree[]) lockedSets.toArray(new ResourceDiffTree[lockedSets.size()]);
            } catch (RuntimeException e) {
                try {
                    for (Iterator iter = lockedSets.iterator(); iter.hasNext();) {
                    	ResourceDiffTree tree = (ResourceDiffTree) iter.next();
                        try {
                            tree.endInput(null);
                        } catch (Throwable e1) {
                            // Ignore so that original exception is not masked
                        }
                    }
                } catch (Throwable e1) {
                    // Ignore so that original exception is not masked
                }
                throw e;
            }
        }

        private void endDispatch(ResourceDiffTree[] locked, IProgressMonitor monitor) {
            if (locked == null) {
                // The begin failed so there's nothing to unlock
                return;
            }
            monitor.beginTask(null, 100 * locked.length);
            for (int i = 0; i < locked.length; i++) {
            	ResourceDiffTree tree = locked[i];
                try {
                    tree.endInput(Policy.subMonitorFor(monitor, 100));
                } catch (RuntimeException e) {
                    // Don't worry about ending every set if an error occurs.
                    // Instead, log the error and suggest a restart.
                    TeamPlugin.log(IStatus.ERROR, Messages.SubscriberChangeSetCollector_0, e); 
                    throw e;
                }
            }
            monitor.done();
        }
        
        /* (non-Javadoc)
         * @see org.eclipse.team.internal.core.BackgroundEventHandler#queueEvent(org.eclipse.team.internal.core.BackgroundEventHandler.Event, boolean)
         */
        protected synchronized void queueEvent(Event event, boolean front) {
            // Override to allow access from enclosing class
            super.queueEvent(event, front);
        }
        
        /*
         * Handle the removal
         */
        private void handleRemove(IResource resource) {
            ChangeSet[] sets = getSets();
            for (int i = 0; i < sets.length; i++) {
                ChangeSet set = sets[i];
                // This will remove any descendants from the set and callback to 
                // resourcesChanged which will batch changes
                if (!set.isEmpty()) {
	                set.rootRemoved(resource, IResource.DEPTH_INFINITE);
	                if (set.isEmpty()) {
	                    remove(set);
	                }
                }
            }
        }
        
        /*
         * Handle the change
         */
        private void handleChange(IResource resource, int depth) throws CoreException {
            IDiff diff = getDiff(resource);
            if (isModified(diff)) {
                ActiveChangeSet[] containingSets = getContainingSets(resource);
                if (containingSets.length == 0) {
	                // Consider for inclusion in the default set
	                // if the resource is not already a member of another set
                    if (getDefaultSet() != null) {
                    	getDefaultSet().add(diff);
                     }
                } else {
                    for (int i = 0; i < containingSets.length; i++) {
                        ActiveChangeSet set = containingSets[i];
                        // Update the sync info in the set
                        set.add(diff);
                    }
                }
            } else {
                removeFromAllSets(resource);
            }
            if (depth != IResource.DEPTH_ZERO) {
                IResource[] members = getSubscriber().members(resource);
                for (int i = 0; i < members.length; i++) {
                    IResource member = members[i];
                    handleChange(member, depth == IResource.DEPTH_ONE ? IResource.DEPTH_ZERO : IResource.DEPTH_INFINITE);
                }
            }
        }
        
        private void removeFromAllSets(IResource resource) {
            List toRemove = new ArrayList();
            ChangeSet[] sets = getSets();
            for (int i = 0; i < sets.length; i++) {
                ChangeSet set = sets[i];
                if (set.contains(resource)) {
                    set.remove(resource);
	                if (set.isEmpty()) {
	                    toRemove.add(set);
	                }
                }
            }
            for (Iterator iter = toRemove.iterator(); iter.hasNext();) {
                ActiveChangeSet set = (ActiveChangeSet) iter.next();
                remove(set);
            }
        }

        private ActiveChangeSet[] getContainingSets(IResource resource) {
            Set result = new HashSet();
            ChangeSet[] sets = getSets();
            for (int i = 0; i < sets.length; i++) {
                ChangeSet set = sets[i];
                if (set.contains(resource)) {
                    result.add(set);
                }
            }
            return (ActiveChangeSet[]) result.toArray(new ActiveChangeSet[result.size()]);
        }
    }
    
    private class ResourceCollector extends org.tigris.subversion.subclipse.core.mapping.SubclipseSubscriberResourceCollector {

        public ResourceCollector(Subscriber subscriber) {
            super(subscriber);
        }

        /* (non-Javadoc)
         * @see org.eclipse.team.internal.core.subscribers.SubscriberResourceCollector#remove(org.eclipse.core.resources.IResource)
         */
        protected void remove(IResource resource) {
        	if (handler != null)
        		handler.queueEvent(new BackgroundEventHandler.ResourceEvent(resource, RESOURCE_REMOVAL, IResource.DEPTH_INFINITE), false);
        }

        /* (non-Javadoc)
         * @see org.eclipse.team.internal.core.subscribers.SubscriberResourceCollector#change(org.eclipse.core.resources.IResource, int)
         */
        protected void change(IResource resource, int depth) {
        	if (handler != null)
        		handler.queueEvent(new BackgroundEventHandler.ResourceEvent(resource, RESOURCE_CHANGE, depth), false);
        }
        
        protected boolean hasMembers(IResource resource) {
            return SubclipseSubscriberChangeSetManager.this.hasMembers(resource);
        }
    }
    
    public SubclipseSubscriberChangeSetManager(Subscriber subscriber) {
        collector = new ResourceCollector(subscriber);
        handler = new EventHandler(NLS.bind(Messages.SubscriberChangeSetCollector_1, new String[] { subscriber.getName() }), NLS.bind(Messages.SubscriberChangeSetCollector_2, new String[] { subscriber.getName() })); // 
    }
    
    /* (non-Javadoc)
     * @see org.eclipse.team.internal.core.subscribers.ChangeSetManager#initializeSets()
     */
    protected void initializeSets() {
    	load(getPreferences());
    }
    
    public boolean hasMembers(IResource resource) {
        ChangeSet[] sets = getSets();
        for (int i = 0; i < sets.length; i++) {
            ActiveChangeSet set = (ActiveChangeSet)sets[i];
            if (set.getDiffTree().getChildren(resource.getFullPath()).length > 0)
            	return true;
        }
        if (getDefaultSet() != null)
            return (getDefaultSet().getDiffTree().getChildren(resource.getFullPath()).length > 0);
        return false;
    }

    /**
     * Return the sync info for the given resource obtained
     * from the subscriber.
     * @param resource the resource
     * @return the sync info for the resource
     * @throws CoreException
     */
    public IDiff getDiff(IResource resource) throws CoreException {
        Subscriber subscriber = getSubscriber();
        return subscriber.getDiff(resource);
    }
    
    /**
     * Return the subscriber associated with this collector.
     * @return the subscriber associated with this collector
     */
    public Subscriber getSubscriber() {
        return collector.getSubscriber();
    }

    /* (non-Javadoc)
     * @see org.eclipse.team.internal.core.subscribers.SubscriberResourceCollector#dispose()
     */
    public void dispose() {
        handler.shutdown();
        collector.dispose();
        super.dispose();
        save(getPreferences());
    }

    private Preferences getPreferences() {
        return getParentPreferences().node(getSubscriberIdentifier());
    }
    
	private static Preferences getParentPreferences() {
		return getTeamPreferences().node(PREF_CHANGE_SETS);
	}
	
	private static Preferences getTeamPreferences() {
		return new InstanceScope().getNode(TeamPlugin.getPlugin().getBundle().getSymbolicName());
	}
	
    /**
     * Return the id that will uniquely identify the subscriber across
     * restarts.
     * @return the id that will uniquely identify the subscriber across
     */
    protected String getSubscriberIdentifier() {
        return getSubscriber().getName();
    }

    /**
     * Wait until the collector is done processing any events.
     * This method is for testing purposes only.
     * @param monitor 
     */
    public void waitUntilDone(IProgressMonitor monitor) {
		monitor.worked(1);
		// wait for the event handler to process changes.
		while(handler.getEventHandlerJob().getState() != Job.NONE) {
			monitor.worked(1);
			try {
				Thread.sleep(10);		
			} catch (InterruptedException e) {
			}
			Policy.checkCanceled(monitor);
		}
		monitor.worked(1);
    }

	/* (non-Javadoc)
	 * @see org.eclipse.team.internal.core.subscribers.ActiveChangeSetManager#getName()
	 */
	protected String getName() {
		return getSubscriber().getName();
	}
}
