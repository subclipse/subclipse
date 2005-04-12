/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.tigris.subversion.subclipse.core.sync.notused;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.team.core.RepositoryProvider;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.core.synchronize.SyncInfo;
import org.eclipse.team.core.variants.IResourceVariant;
import org.eclipse.team.core.variants.ThreeWayRemoteTree;
import org.eclipse.team.core.variants.ThreeWaySubscriber;
import org.eclipse.team.core.variants.ThreeWaySynchronizer;
import org.tigris.subversion.subclipse.core.SVNProviderPlugin;
import org.tigris.subversion.subclipse.core.SVNTeamProvider;
import org.tigris.subversion.subclipse.core.sync.SVNRevisionComparator;

/**
 * The subscriber for svn.
 * - Provides access to the synchronization state between resources in workspace and
 * a set of resource variants.
 * - It uses a ThreeWaySynchronizer to manage the synchronization state between 
 * workspace and a remote location
 * - It uses a ThreeWayRemoteTree to provide the remote tree access and refresh
 * 
 * @see ThreeWaySubscriber
 * @see ThreeWaySynchronizer
 * @see SVNTeamProvider
 * @see SVNRemoteTree
 */
public class SVNSubscriber extends ThreeWaySubscriber {

	private static SVNSubscriber instance;

	/**
	 * Return the file system subscriber singleton.
	 * 
	 * @return the file system subscriber singleton.
	 */
	public static synchronized SVNSubscriber getInstance() {
		if (instance == null) {
			instance = new SVNSubscriber();
		}
		return instance;
	}

	/**
	 * Create the file system subscriber.
	 */
	private SVNSubscriber() {
		super(new ThreeWaySynchronizer(new QualifiedName(SVNProviderPlugin.ID,
				"workspace-sync"))); //$NON-NLS-1$
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.team.core.variants.ThreeWaySubscriber#createRemoteTree()
	 */
	protected ThreeWayRemoteTree createRemoteTree() {
		return new SVNRemoteTree(this);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.team.core.subscribers.Subscriber#getName()
	 */
	public String getName() {
		return "SVNSubscriber"; //$NON-NLS-1$
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.team.core.subscribers.Subscriber#roots()
	 */
	public IResource[] roots() {
		// root resources this subscriber considers for synchronization ie
        // projects which have a svn nature
        List result = new ArrayList();
		IProject[] projects = ResourcesPlugin.getWorkspace().getRoot()
				.getProjects();
		for (int i = 0; i < projects.length; i++) {
			IProject project = projects[i];
			if (project.isAccessible()) {
				RepositoryProvider provider = RepositoryProvider.getProvider(
						project, SVNProviderPlugin.PROVIDER_ID);
				if (provider != null) {
					result.add(project);
				}
			}
		}
		return (IProject[]) result.toArray(new IProject[result.size()]);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.team.core.variants.ThreeWaySubscriber#handleRootChanged(org.eclipse.core.resources.IResource,
	 *      boolean)
	 */
	public void handleRootChanged(IResource resource, boolean added) {
		// Override to allow SVNTeamProvider to signal the addition and removal
		// of roots
		super.handleRootChanged(resource, added);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.team.core.variants.ResourceVariantTreeSubscriber#getSyncInfo(org.eclipse.core.resources.IResource,
	 *      org.eclipse.team.core.variants.IResourceVariant,
	 *      org.eclipse.team.core.variants.IResourceVariant)
	 */
	protected SyncInfo getSyncInfo(IResource local, IResourceVariant base,
			IResourceVariant remote) throws TeamException {
		// Override to use a custom sync info
		SVNSyncInfo info = new SVNSyncInfo(local, base, remote,
				new SVNRevisionComparator());
		info.init();
		return info;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.team.core.variants.ThreeWaySubscriber#getResourceVariant(org.eclipse.core.resources.IResource,
	 *      byte[])
	 */
	public IResourceVariant getResourceVariant(IResource resource, byte[] bytes)
			throws TeamException {
		RepositoryProvider provider = RepositoryProvider.getProvider(resource
				.getProject(), SVNProviderPlugin.PROVIDER_ID);
		if (provider != null) {
			return ((SVNTeamProvider) provider).getResourceVariant(resource,
					bytes);
		}
		return null;
	}

}