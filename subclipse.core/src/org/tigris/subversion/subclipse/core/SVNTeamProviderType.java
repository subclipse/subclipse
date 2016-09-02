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

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.team.core.ProjectSetCapability;
import org.eclipse.team.core.RepositoryProvider;
import org.eclipse.team.core.RepositoryProviderType;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.core.subscribers.Subscriber;
import org.tigris.subversion.subclipse.core.client.PeekStatusCommand;
import org.tigris.subversion.subclipse.core.resources.SVNWorkspaceRoot;
import org.tigris.subversion.subclipse.core.sync.SVNWorkspaceSubscriber;
import org.tigris.subversion.svnclientadapter.ISVNClientAdapter;


/**
 * This class represents the SVN Provider's capabilities in the absence of a
 * particular project.
 */

public class SVNTeamProviderType extends RepositoryProviderType {

    protected static AutoShareJob autoShareJob;
    
    public static class AutoShareJob extends Job {

        List<IProject> projectsToShare = new ArrayList<IProject>();
        
        AutoShareJob() {
            super("Auto-sharing imported subversion projects");
        }

        public boolean isQueueEmpty() {
            return projectsToShare.isEmpty();
        }

        /* (non-Javadoc)
         * @see org.eclipse.core.runtime.jobs.Job#shouldSchedule()
         */
        public boolean shouldSchedule() {
            return !isQueueEmpty();
        }
        
        /* (non-Javadoc)
         * @see org.eclipse.core.runtime.jobs.Job#shouldRun()
         */
        public boolean shouldRun() {
            synchronized (projectsToShare) {
                for (Iterator<IProject> iter = projectsToShare.iterator(); iter.hasNext();) {
                    IProject project = iter.next();
                    if (RepositoryProvider.isShared(project)) {
                        iter.remove();
                    }
                }
                return !projectsToShare.isEmpty();
            }
        }
        
        public void share(IProject project) {
            if (!RepositoryProvider.isShared(project)) {
                synchronized (projectsToShare) {
                    if (!projectsToShare.contains(project)) {
                        SVNWorkspaceRoot.setManagedBySubclipse(project);
                        projectsToShare.add(project);
                    }
                }
                if(getState() == Job.NONE && !isQueueEmpty())
                    schedule();
            }
        }
        
        /* (non-Javadoc)
         * @see org.eclipse.core.runtime.jobs.Job#run(org.eclipse.core.runtime.IProgressMonitor)
         */
        protected IStatus run(IProgressMonitor monitor) {
            IProject next = null;
            next = getNextProject();
            monitor.beginTask(null, IProgressMonitor.UNKNOWN);
            while (next != null) {
            	if (next.isAccessible()) {
            		autoconnectSVNProject(next, Policy.subMonitorFor(monitor, IProgressMonitor.UNKNOWN));
            	}
                next = getNextProject();
            }
            monitor.done();
            return Status.OK_STATUS;
        }

        private IProject getNextProject() {
            IProject next = null;
            synchronized (projectsToShare) {
                if (!projectsToShare.isEmpty()) {
                    next = (IProject)projectsToShare.remove(0);
                }
            }
            return next;
        }
        
        /*
         * Auto-connect to the repository using .svn/ directories
         */
        private void autoconnectSVNProject(IProject project, IProgressMonitor monitor) {
            try {
        		PeekStatusCommand command = new PeekStatusCommand(project);
        		try {
    				command.execute();
    			} catch (SVNException e1) {
    				if (e1.getMessage() != null && e1.getMessage().contains(SVNProviderPlugin.UPGRADE_NEEDED)) {
    					if (!SVNProviderPlugin.handleQuestion("Upgrade Working Copy", project.getName() + " appears to be managed by Subversion, but the working copy needs to be upgraded.  Do you want to upgrade the working copy now?\n\nWarning:  This operation cannot be undone.")) {
    						return;			
    					}
    				}
    				 SVNWorkspaceRoot.upgradeWorkingCopy(project, monitor);
    			}
                SVNWorkspaceRoot.setSharing(project, monitor);
            } catch (TeamException e) {
                SVNProviderPlugin.log(IStatus.ERROR, "Could not auto-share project " + project.getName(), e); //$NON-NLS-1$
            }
        }
    }
    
    public synchronized static AutoShareJob getAutoShareJob() {
        if (autoShareJob == null) {
            autoShareJob = new AutoShareJob();
            autoShareJob.addJobChangeListener(new JobChangeAdapter() {
                public void done(IJobChangeEvent event) {
                    // Reschedule the job if it has unprocessed projects
                    if (!autoShareJob.isQueueEmpty()) {
                        autoShareJob.schedule();
                    }
                }
            });
            autoShareJob.setSystem(true);
            autoShareJob.setPriority(Job.SHORT);
            // Must run with the workspace rule to ensure that projects added while we're running
            // can be shared
            autoShareJob.setRule(ResourcesPlugin.getWorkspace().getRoot());
        }
        return autoShareJob;
    }
    
    
    /**
     * @see org.eclipse.team.core.RepositoryProviderType#supportsProjectSetImportRelocation()
     */
    public boolean supportsProjectSetImportRelocation() {
        return false;
    }

    /* (non-Javadoc)
     * @see org.eclipse.team.core.RepositoryProviderType#getProjectSetCapability()
     */
    public ProjectSetCapability getProjectSetCapability() {
        return new SVNProjectSetCapability();
    }

	public static class AutoAddJob extends Job {
		final static int MAX_RETRIES = 10;
		int reschedCount = 0;
		final IProject project;
		
		protected AutoAddJob(IProject project){
			super("Auto-adding newly created project to subversion: " + project.getName()); //$NON-NLS-1$
			this.project = project;
		}
		
        /* (non-Javadoc)
         * @see org.eclipse.core.runtime.jobs.Job#run(org.eclipse.core.runtime.IProgressMonitor)
         */
        protected IStatus run(IProgressMonitor monitor) {
			monitor.beginTask(null, IProgressMonitor.UNKNOWN);
			SVNProviderPlugin plugin = SVNProviderPlugin.getPlugin();
			SVNClientManager svnClientManager = plugin.getSVNClientManager();
			ISVNClientAdapter client = null;
			try{
				
				if (plugin == null || plugin.getSimpleDialogsHelper() == null){
					if (++reschedCount > MAX_RETRIES){
						String errorString = "Subclipse core and/or ui didn't come up in " + MAX_RETRIES + " retries, failing.";  //$NON-NLS-1$
						System.err.println(errorString); // Let it be visible to the user
						throw new SVNException(errorString);
					}
					schedule(1000);
					return Status.OK_STATUS;
				}
				
				if (!plugin.getSimpleDialogsHelper().promptYesNo(
						"Auto-add "+project.getName()+" to source control", //$NON-NLS-1$
						  "The new project \""+ project.getName() +"\" was created in a subversion " + //$NON-NLS-1$
						  "controlled directory.\n\n" + //$NON-NLS-1$
						  "Would you like to automatically add it to source control?", true)) { //$NON-NLS-1$

					return Status.OK_STATUS;
				}
						
				client = svnClientManager.getSVNClient();

				File file = project.getLocation().toFile();
				client.addDirectory(file, false);

				RepositoryProvider.map(project, SVNProviderPlugin.getTypeId());
				plugin.getStatusCacheManager().refreshStatus(project,
						true);
				
			}catch(Exception e){
                SVNProviderPlugin.log(IStatus.ERROR, "Could not auto-add project " + project.getName(), e); //$NON-NLS-1$
				return Status.CANCEL_STATUS;
			}finally{
				monitor.done();
				svnClientManager.returnSVNClient(client);
			}
			return Status.OK_STATUS;
        }
		
    }	

	/**
     * Create and schedule an auto-add job
     */
	
	private static synchronized void createAutoAddJob(IProject project) {
		Job j = new AutoAddJob(project);
        j.setSystem(true);
        j.setPriority(Job.SHORT);
        j.setRule(ResourcesPlugin.getWorkspace().getRoot());
		j.schedule();
	}

	/* (non-Javadoc)
     * @see org.eclipse.team.core.RepositoryProviderType#metaFilesDetected(org.eclipse.core.resources.IProject, org.eclipse.core.resources.IContainer[])
     */
    public void metaFilesDetected(IProject project, IContainer[] containers) {
    	SVNProviderPlugin plugin = SVNProviderPlugin.getPlugin();
		boolean isProject = false;
		boolean isSvnProject = plugin.isManagedBySubversion(project);
		
        for (int i = 0; i < containers.length; i++) {
            IContainer container = containers[i];
            IContainer svnDir = null;
			
			if (!isProject && container.getType() == IResource.PROJECT)
				isProject = true;
			
            if (plugin.isAdminDirectory(container.getName())) { //$NON-NLS-1$
                svnDir = container;
            } else {
                IResource resource = container.findMember(plugin.getAdminDirectoryName()); //$NON-NLS-1$
                if (resource != null && resource.getType() != IResource.FILE) {
                    svnDir = (IContainer)resource;
                }
            }
            try {
                if (svnDir != null && !svnDir.isTeamPrivateMember()) {
                	if (!isSvnProject) {
                		if (plugin.isManagedBySubversion(svnDir.getParent()))
                    		svnDir.setTeamPrivateMember(true);
                	} else {
                		svnDir.setTeamPrivateMember(true);
                	}
                }
            } catch (CoreException e) {
                SVNProviderPlugin.log(IStatus.ERROR, "Could not flag meta-files as team-private for " + svnDir.getFullPath(), e); //$NON-NLS-1$
            }
        }
		
		if (!isProject)
			return; // Nothing more to do, all remaining operations are on projects

		// Examine whether this project is a nested project. If yes, we don't
		// share it automatically.
		if (!SVNProviderPlugin.getPlugin().getPluginPreferences().getBoolean(ISVNCoreConstants.PREF_SHARE_NESTED_PROJECTS)
				&& isNestedProject(project))
		{
			return;
		}
		
		if (isSvnProject) {
			// It's a project and has toplevel .svn directory, lets share it!
			getAutoShareJob().share(project);
		} else {
			// It's a project and doesn't have .svn dir, let's see if we can add it!
			IPath parentDir = project.getLocation().append("../"); //$NON-NLS-1$
			
			if (plugin.isManagedBySubversion(parentDir)) {
				createAutoAddJob(project);
			}
		}
    }


	/* (non-Javadoc)
	 * @see org.eclipse.team.core.RepositoryProviderType#getSubscriber()
	 */
	public Subscriber getSubscriber() {
		return SVNWorkspaceSubscriber.getInstance();
	}
	
	private boolean isNestedProject(IProject testProject)
	{
		IPath testProjectLocation = testProject.getLocation();

		IProject[] projects = ResourcesPlugin.getWorkspace().getRoot().getProjects();
		for (int i = 0; i < projects.length; i++) {
			IProject project = projects[i];

			if (project.equals(testProject))
				continue;

			IPath projectLocation = project.getLocation();
			if ((projectLocation != null) && projectLocation.isPrefixOf(testProjectLocation))
				return true;
		}

		return false;
	}
}
