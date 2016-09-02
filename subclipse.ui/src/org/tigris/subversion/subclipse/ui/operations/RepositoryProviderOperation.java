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
package org.tigris.subversion.subclipse.ui.operations;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceRuleFactory;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.MultiRule;
import org.eclipse.team.core.RepositoryProvider;
import org.eclipse.ui.IWorkbenchPart;
import org.tigris.subversion.subclipse.core.ISVNFolder;
import org.tigris.subversion.subclipse.core.ISVNRepositoryLocation;
import org.tigris.subversion.subclipse.core.ISVNResource;
import org.tigris.subversion.subclipse.core.SVNException;
import org.tigris.subversion.subclipse.core.SVNProviderPlugin;
import org.tigris.subversion.subclipse.core.SVNTeamProvider;
import org.tigris.subversion.subclipse.core.resources.SVNWorkspaceRoot;

/**
 * Performs a svn operation on multiple repository providers
 */
public abstract class RepositoryProviderOperation extends SVNOperation {

	private IResource[] resources;

	/**
	 * @param shell
	 */
	public RepositoryProviderOperation(IWorkbenchPart part, IResource[] resources) {
		super(part);
		this.resources = resources;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.internal.cSVN.ui.operations.SVNOperation#execute(org.eclipse.core.runtime.IProgressMonitor)
	 */
	public void execute(IProgressMonitor monitor) throws SVNException, InterruptedException {
		Map table = getProviderMapping(getResources());
		Set keySet = table.keySet();
		monitor.beginTask(null, keySet.size() * 1000);
		Iterator iterator = keySet.iterator();
		while (iterator.hasNext()) {
			IProgressMonitor subMonitor = new SubProgressMonitor(monitor, 1000);
			SVNTeamProvider provider = (SVNTeamProvider)iterator.next();
			List list = (List)table.get(provider);
			IResource[] providerResources = (IResource[])list.toArray(new IResource[list.size()]);
			ISchedulingRule rule = getSchedulingRule(provider);
			try {
				Job.getJobManager().beginRule(rule, monitor);
				monitor.setTaskName(getTaskName(provider));
				execute(provider, providerResources, subMonitor);
			} finally {
				Job.getJobManager().endRule(rule);
			}
		}
	}
	
	/**
	 * Return the taskname to be shown in the progress monitor while operating
	 * on the given provider.
	 * @param provider the provider being processed
	 * @return the taskname to be shown in the progress monitor
	 */
	protected abstract String getTaskName(SVNTeamProvider provider);

	/**
	 * Retgurn the scheduling rule to be obtained before work
	 * begins on the given provider. By default, it is the provider's project.
	 * This can be changed by subclasses.
	 * @param provider
	 * @return
	 */
	protected ISchedulingRule getSchedulingRule(SVNTeamProvider provider) {
		IResourceRuleFactory ruleFactory = provider.getRuleFactory();
		HashSet rules = new HashSet();
		IProject[] projects = ResourcesPlugin.getWorkspace().getRoot().getProjects();
		for (int i = 0; i < resources.length; i++) {			
			IResource[] pathResources = SVNWorkspaceRoot.getResourcesFor(new Path(resources[i].getLocation().toOSString()), false);
			for (IResource pathResource : pathResources) {
				IProject resourceProject = pathResource.getProject();				
				rules.add(ruleFactory.modifyRule(resourceProject));
				if (resourceProject.getLocation() != null) {
					// Add nested projects
					for (IProject project : projects) {
						if (project.getLocation() != null) {
							if (!project.getLocation().equals(resourceProject.getLocation()) && resourceProject.getLocation().isPrefixOf(project.getLocation())) {
								rules.add(ruleFactory.modifyRule(project));
							}
						}
					}	
				}
			}
		}
		return MultiRule.combine((ISchedulingRule[]) rules.toArray(new ISchedulingRule[rules.size()]));
	}

	/*
	 * Helper method. Return a Map mapping provider to a list of resources
	 * shared with that provider.
	 */
	private Map getProviderMapping(IResource[] resources) {
		Map result = new HashMap();
		for (int i = 0; i < resources.length; i++) {
			RepositoryProvider provider = RepositoryProvider.getProvider(resources[i].getProject(), SVNProviderPlugin.getTypeId());
			List list = (List)result.get(provider);
			if (list == null) {
				list = new ArrayList();
				result.put(provider, list);
			}
			list.add(resources[i]);
		}
		return result;
	}
	
	/**
	 * Return the resources that the operation is being performed on
	 * @return
	 */
	protected IResource[] getResources() {
		return resources;
	}

	/**
	 * Set the resources that the operation is to be performed on
	 * @param resources
	 */
	protected void setResources(IResource[] resources) {
		this.resources = resources;
	}

	/**
	 * Execute the operation on the resources for the given provider.
	 * @param provider
	 * @param providerResources
	 * @param subMonitor
	 * @throws SVNException
	 * @throws InterruptedException
	 */
	protected abstract void execute(SVNTeamProvider provider, IResource[] resources, IProgressMonitor monitor) throws SVNException, InterruptedException;

	protected ISVNResource[] getSVNArguments(IResource[] resources) {
		ISVNResource[] SVNResources = new ISVNResource[resources.length];
		for (int i = 0; i < SVNResources.length; i++) {
			SVNResources[i] = SVNWorkspaceRoot.getSVNResourceFor(resources[i]);
		}
		return SVNResources;
	}
	
	/*
	 * Get the arguments to be passed to a commit or update
	 */
	protected String[] getStringArguments(IResource[] resources) throws SVNException {
		List arguments = new ArrayList(resources.length);
		for (int i=0;i<resources.length;i++) {
			IPath svnPath = resources[i].getFullPath().removeFirstSegments(1);
			if (svnPath.segmentCount() == 0) {
				arguments.add(".");//Session.CURRENT_LOCAL_FOLDER); //$NON-NLS-1$
			} else {
				arguments.add(svnPath.toString());
			}
		}
		return (String[])arguments.toArray(new String[arguments.size()]);
	}
	
	public ISVNResource[] getSVNResources() {
		ISVNResource[] svnResources = new ISVNResource[resources.length];
		for (int i = 0; i < resources.length; i++) {
			svnResources[i] = SVNWorkspaceRoot.getSVNResourceFor(resources[i]);
		}
		return svnResources;
	}
	
	protected ISVNRepositoryLocation getRemoteLocation(SVNTeamProvider provider) throws SVNException {
		SVNWorkspaceRoot workspaceRoot = provider.getSVNWorkspaceRoot();
		return workspaceRoot.getRepository();
	}
	
	protected ISVNFolder getLocalRoot(SVNTeamProvider provider) throws SVNException {
		SVNWorkspaceRoot workspaceRoot = provider.getSVNWorkspaceRoot();
		return workspaceRoot.getLocalRoot();
	}

	/**
	 * Update the workspace subscriber for an update operation performed on the 
	 * given resources. After an update, the remote tree is flushed in order
	 * to ensure that stale incoming additions are removed. This need only
	 * be done for folders. At the time of writting, all update operations
	 * are deep so the flush is deep as well.
	 * @param provider the provider (projedct) for all the given resources
	 * @param resources the resources that were updated
	 * @param monitor a progress monitor
	 */
//	protected void updateWorkspaceSubscriber(SVNTeamProvider provider, ISVNResource[] resources, IProgressMonitor monitor) {
//		SVNWorkspaceSubscriber s = SVNProviderPlugin.getPlugin().getSVNWorkspaceSubscriber();
//		monitor.beginTask(null, 100 * resources.length);
//		for (int i = 0; i < resources.length; i++) {
//			ISVNResource resource = resources[i];
//			if (resource.isFolder()) {
//				try {
//					s.updateRemote(provider, (ISVNFolder)resource, Policy.subMonitorFor(monitor, 100));
//				} catch (TeamException e) {
//					// Just log the error and continue
//					SVNUIPlugin.log(e);
//				}
//			} else {
//				monitor.worked(100);
//			}
//		}
//	}
	
}
