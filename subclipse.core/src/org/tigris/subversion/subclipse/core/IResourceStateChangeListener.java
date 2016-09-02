/*******************************************************************************
 * Copyright (c) 2003, 2006 Subclipse project and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Subclipse project committers - initial API and implementation
 ******************************************************************************/
package org.tigris.subversion.subclipse.core;


import java.util.EventListener;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;

/**
 * A resource state change listener is notified of changes to resources
 * regarding their team state. 
 * <p>
 * Clients may implement this interface.
 * </p>
 * @see ITeamManager#addResourceStateChangeListener(IResourceStateChangeListener)
 */
public interface IResourceStateChangeListener extends EventListener{
	
	/**
	 * Notifies this listener that some resource sync info state changes have
	 * already happened. For example, a resource's base revision may have
	 * changed. The resource tree is open for modification when this method is
	 * invoked, so markers can be created, etc.
     *
	 * @param changedResources that have sync info state changes
	 * 
	 * [Note: The changed state event is purposely vague. For now it is only
	 * a hint to listeners that they should query the provider to determine the
	 * resources new sync info.]
	 */
	public void resourceSyncInfoChanged(IResource[] changedResources);
	
	/**
	 * Notifies this listener that the resource's have been modified. This
	 * doesn't necessarily mean that the resource state isModified. The listener
	 * must check the state.
	 *
	 * @param changedResources that have changed state
	 * @param changeType the type of state change.
	 */
	public void resourceModified(IResource[] changedResources);
	
	/**
	 * Notifies this listener that the project has just been configured
	 * to be a Subversion project.
	 *
	 * @param project The project that has just been configured
	 */
	public void projectConfigured(IProject project);
	
	/**
	 * Notifies this listener that the project has just been deconfigured
	 * and no longer has the SVN nature.
	 *
	 * @param project The project that has just been configured
	 */
	public void projectDeconfigured(IProject project);
	
	public void initialize();
}

