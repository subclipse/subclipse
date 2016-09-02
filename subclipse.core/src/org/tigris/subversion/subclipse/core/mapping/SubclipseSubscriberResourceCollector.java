/*******************************************************************************
 * Copyright (c) 2000, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.tigris.subversion.subclipse.core.mapping;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Preferences.IPropertyChangeListener;
import org.eclipse.core.runtime.Preferences.PropertyChangeEvent;
import org.eclipse.team.core.subscribers.*;
import org.tigris.subversion.subclipse.core.ISVNCoreConstants;
import org.tigris.subversion.subclipse.core.SVNException;
import org.tigris.subversion.subclipse.core.SVNProviderPlugin;
import org.tigris.subversion.subclipse.core.resources.LocalResourceStatus;

/**
 * This class acts as a superclass for any class that is collecting subscriber
 * resources. It provides functionality that listens to resource deltas and
 * subscriber change events in order to determine when the state of resources
 * that are supervised by a subscriber may have changed.
 */
public abstract class SubclipseSubscriberResourceCollector implements IResourceChangeListener, ISubscriberChangeListener, IPropertyChangeListener {
    
	private boolean ignoreManagedDerivedResources = SVNProviderPlugin.getPlugin().getPluginPreferences().getBoolean(ISVNCoreConstants.PREF_IGNORE_MANAGED_DERIVED_RESOURCES);
	
    Subscriber subscriber;
    
    /**
     * Create the collector and register it as a listener with the workspace
     * and the subscriber.
     * @param subscriber the subscriber to be associated with this collector
     */
    public SubclipseSubscriberResourceCollector(Subscriber subscriber) {
        Assert.isNotNull(subscriber);
        this.subscriber = subscriber;
		ResourcesPlugin.getWorkspace().addResourceChangeListener(this, IResourceChangeEvent.POST_CHANGE);
		subscriber.addListener(this);
		SVNProviderPlugin.getPlugin().getPluginPreferences().addPropertyChangeListener(this);
    }
    
	/**
	 * Returns the <code>Subscriber</code> associated with this collector.
	 * 
	 * @return the <code>Subscriber</code> associated with this collector.
	 */
	public Subscriber getSubscriber() {
		return subscriber;
	}
	
    /**
     * De-register the listeners for this collector.
     */
	public void dispose() {
		getSubscriber().removeListener(this);		
		ResourcesPlugin.getWorkspace().removeResourceChangeListener(this);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.core.sync.ITeamResourceChangeListener#teamResourceChanged(org.eclipse.team.core.sync.TeamDelta[])
	 */
	public void subscriberResourceChanged(ISubscriberChangeEvent[] deltas) {
	    try {
		    beginInput();
			IResource[] roots = getRoots();
			for (int i = 0; i < deltas.length; i++) {
				switch (deltas[i].getFlags()) {
					case ISubscriberChangeEvent.SYNC_CHANGED :
						if (isAllRootsIncluded() || isDescendantOfRoot(deltas[i].getResource(), roots)) {
						    change(deltas[i].getResource(), IResource.DEPTH_ZERO);
						}
						break;
					case ISubscriberChangeEvent.ROOT_REMOVED :
						remove(deltas[i].getResource());
						break;
					case ISubscriberChangeEvent.ROOT_ADDED :
						if (isAllRootsIncluded() || isDescendantOfRoot(deltas[i].getResource(), roots)) {
							change(deltas[i].getResource(), IResource.DEPTH_INFINITE);
						}
						break;
				}
			}
	    } finally {
	        endInput();
	    }
	}

    /**
     * This method is invoked at the beginning of a subscriber change event
     * or resource delta event. The endInput method will be invoked at some point
     * following this. There may be several invocations of remove or change
     * in between.
     */
    protected void beginInput() {
        // Do nothing by default
    }
    
    /**
     * The processing of the resource or subscriber delta has finished.
     * Subclasses can accumulate removals and changes and handle them
     * at this point to allow batched change events.
     */
    protected void endInput() {
        // Do nothing by default
    }
    

    /*(non-Javadoc)
	 * @see org.eclipse.core.resources.IResourceChangeListener#resourceChanged(org.eclipse.core.resources.IResourceChangeEvent)
	 */
	public void resourceChanged(IResourceChangeEvent event) {
	    try {
	        beginInput();
			processDelta(event.getDelta(), getRoots());
	    } finally {
	        endInput();
	    }
	}
	
    /**
	 * Process the resource delta and posts all necessary events to the background
	 * event handler.
	 * 
	 * @param delta the resource delta to analyze
	 */
	protected void processDelta(IResourceDelta delta, IResource[] roots) {
		IResource resource = delta.getResource();
		int kind = delta.getKind();
		
		// Skip derived resources: <<  PATCH >>
		if (resource.isDerived()) {
			LocalResourceStatus aStatus = null;
			try {
				aStatus = SVNProviderPlugin.getPlugin().getStatusCacheManager().getStatusFromCache(resource);
			} catch (SVNException e) {
				SVNProviderPlugin.log(IStatus.ERROR, e.getMessage(), e);
			}
			if ((aStatus == null) || !aStatus.isManaged()) {
				return;
			}
			if (ignoreManagedDerivedResources) {
				return;
			}
		}

		if (resource.getType() == IResource.PROJECT) {
			// Handle projects that should be removed from the collector
			if (((kind & IResourceDelta.REMOVED) != 0) /* deleted project */ 
			        || (delta.getFlags() & IResourceDelta.OPEN) != 0 && !((IProject) resource).isOpen() /* closed project */
			        || !isAncestorOfRoot(resource, roots)) /* not within subscriber roots */ {
				// If the project has any entries in the sync set, remove them
				if (hasMembers(resource)) {
					remove(resource);
				}
			}
		}

		boolean visitChildren = false;
		if (isDescendantOfRoot(resource, roots)) {
			visitChildren = true;
			// If the resource has changed type, remove the old resource handle
			// and add the new one
			if ((delta.getFlags() & IResourceDelta.TYPE) != 0) {
				remove(resource);
				change(resource, IResource.DEPTH_INFINITE);
			}
	
			// Check the flags for changes the SyncSet cares about.
			// Notice we don't care about MARKERS currently.
			int changeFlags = delta.getFlags();
			if ((changeFlags & (IResourceDelta.OPEN | IResourceDelta.CONTENT)) != 0) {
				change(resource, IResource.DEPTH_ZERO);
			}
	
			// Check the kind and deal with those we care about
			if ((delta.getKind() & (IResourceDelta.REMOVED | IResourceDelta.ADDED)) != 0) {
				change(resource, IResource.DEPTH_ZERO);
			}
		}
		

		if (resource.getType() == IFolder.FOLDER) {
		visitChildren = false;
		}

		// Handle changed children
		if (visitChildren || isAncestorOfRoot(resource, roots)) {
			IResourceDelta[] affectedChildren = delta.getAffectedChildren(IResourceDelta.CHANGED | IResourceDelta.REMOVED | IResourceDelta.ADDED);
			for (int i = 0; i < affectedChildren.length; i++) {
				processDelta(affectedChildren[i], roots);
			}
		}
	}
	
	/**
	 * Return the root resources that are to be considered by this handler.
	 * These may be either the subscriber roots or a set of resources that are
	 * contained by the subscriber's roots.
     * @return the root resources that are to be considered by this handler
     */
	protected IResource[] getRoots() {
		return getSubscriber().roots();
	}

	/**
	 * Return whether the given resource, which is not
	 * within the roots of this handler, has children
	 * that are.
     * @param resource the resource
     * @return whether the resource has children that are being considered
     * by this handler.
     */
    protected abstract boolean hasMembers(IResource resource);
    
    /**
     * The resource is no longer of concern to the subscriber.
     * Remove the resource and any of it's descendants
     * from the set of resources being collected.
     * @param resource the resource to be removed along with its
     * descendants.
     */
    protected abstract void remove(IResource resource);

    /**
     * The resource sync state has changed to the depth specified.
     * @param resource the resource
     * @param depth the depth
     */
    protected abstract void change(IResource resource, int depth);
    
	/**
	 * Return whether all roots of a subscriber are included or
	 * if the collector is only consider a subset of the resources.
     * @return whether all roots of a subscriber are included
     */
	protected boolean isAllRootsIncluded() {
		return true;
	}

    private boolean isAncestorOfRoot(IResource parent, IResource[] roots) {
		// Always traverse into projects in case a root was removed
		if (parent.getType() == IResource.ROOT) return true;
		for (int i = 0; i < roots.length; i++) {
			IResource resource = roots[i];
			if (parent.getFullPath().isPrefixOf(resource.getFullPath())) {
				return true;
			}
		}
		return false;
	}

	private boolean isDescendantOfRoot(IResource resource, IResource[] roots) {
		for (int i = 0; i < roots.length; i++) {
			IResource root = roots[i];
			if (root.getFullPath().isPrefixOf(resource.getFullPath())) {
				return true;
			}
		}
		return false;
	}
	
	public void propertyChange(PropertyChangeEvent event) {
		if (event.getProperty().equals(ISVNCoreConstants.PREF_IGNORE_MANAGED_DERIVED_RESOURCES)) {
			ignoreManagedDerivedResources = SVNProviderPlugin.getPlugin().getPluginPreferences().getBoolean(ISVNCoreConstants.PREF_IGNORE_MANAGED_DERIVED_RESOURCES);
		}
	}
}
