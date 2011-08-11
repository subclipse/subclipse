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

package org.tigris.subversion.subclipse.core;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.MultiRule;
import org.eclipse.team.core.ProjectSetCapability;
import org.eclipse.team.core.ProjectSetSerializationContext;
import org.eclipse.team.core.RepositoryProvider;
import org.eclipse.team.core.TeamException;
import org.tigris.subversion.subclipse.core.commands.CheckoutCommand;
import org.tigris.subversion.subclipse.core.repo.SVNRepositoryLocation;
import org.tigris.subversion.subclipse.core.resources.RemoteFolder;
import org.tigris.subversion.subclipse.core.resources.SVNWorkspaceRoot;
import org.tigris.subversion.svnclientadapter.SVNUrl;

/**
 * An object for serializing and deserializing of references to SVN based
 * projects. Given a project, it can produce a UTF-8 encoded String which can be
 * stored in a file. Given this String, it can load a project into the
 * workspace.
 */
public class SVNProjectSetCapability extends ProjectSetCapability {

    /**
     * Override superclass implementation to return an array of project
     * references.
     * 
     * @see ProjectSetSerializer#asReference(IProject[],
     *      ProjectSetSerializationContext, IProgressMonitor)
     */
    public String[] asReference(IProject[] projects,
            ProjectSetSerializationContext context, IProgressMonitor monitor)
            throws TeamException {
        String[] result = new String[projects.length];
        for (int i = 0; i < projects.length; i++) {
            result[i] = asReference(projects[i]);
        }
        return result;
    }

    /**
     * Answer a string representing the specified project
     * 
     * @param project
     *            the project (not <code>null</code>)
     * @return the project reference (not <code>null</code>)
     */
    private String asReference(IProject project) {
        StringBuffer buffer = new StringBuffer();
        buffer.append("0.9.3,"); //$NON-NLS-1$

        SVNTeamProvider provider = (SVNTeamProvider) RepositoryProvider
                .getProvider(project);
        SVNWorkspaceRoot root = provider.getSVNWorkspaceRoot();

        buffer.append(root.getLocalRoot().getUrl().toString());
        buffer.append(",");
        buffer.append(project.getName());
        return buffer.toString();
    }

    /**
     * Override superclass implementation to load the referenced projects into
     * the workspace.
     * 
     * @see org.eclipse.team.core.ProjectSetSerializer#addToWorkspace(java.lang.String[],
     *      org.eclipse.team.core.ProjectSetSerializationContext,
     *      org.eclipse.core.runtime.IProgressMonitor)
     */
    public IProject[] addToWorkspace(String[] referenceStrings,
            ProjectSetSerializationContext context, IProgressMonitor monitor)
            throws TeamException {

        monitor = Policy.monitorFor(monitor);
        Policy.checkCanceled(monitor);

        // Confirm the projects to be loaded
        Map<IProject, LoadInfo> infoMap = new HashMap<IProject, SVNProjectSetCapability.LoadInfo>(referenceStrings.length);
        IProject[] projects = asProjects(context, referenceStrings, infoMap);
        projects = confirmOverwrite(context, projects);
        if (projects == null) {
            return new IProject[0];
        }
        // Load the projects
        try {
			return checkout(projects, infoMap, monitor);
		} catch (MalformedURLException e) {
			throw SVNException.wrapException(e);
		}
    }

    /**
     * Translate the reference strings into projects to be loaded and build a
     * mapping of project to project load information.
     * 
     * @param context
     *            the context of where the references came from
     * @param referenceStrings
     *            project references
     * @param infoMap
     *            a mapping of project to project load information
     * @return the projects to be loaded
     */
    private IProject[] asProjects(ProjectSetSerializationContext context,
            String[] referenceStrings, Map<IProject, LoadInfo> infoMap) throws SVNException {
        Collection<IProject> result = new ArrayList<IProject>();
        for (String referenceString : referenceStrings) {
            StringTokenizer tokenizer = new StringTokenizer(
                    referenceString, ","); //$NON-NLS-1$
            String version = tokenizer.nextToken();
            // If this is a newer version, then ignore it
            if (!version.equals("0.9.3")) { //$NON-NLS-1$
                continue;
            }
            LoadInfo info = new LoadInfo(context, tokenizer);
            IProject proj = info.getProject();
            result.add(proj);
            infoMap.put(proj, info);
        }
        return (IProject[]) result.toArray(new IProject[result.size()]);
    }

    /**
     * Checkout projects from the SVN repository
     * 
     * @param projects
     *            the projects to be loaded from the repository
     * @param infoMap
     *            a mapping of project to project load information
     * @param monitor
     *            the progress monitor (not <code>null</code>)
     */
    private IProject[] checkout(IProject[] projects, Map<IProject, LoadInfo> infoMap,
            IProgressMonitor monitor) throws TeamException, MalformedURLException {
        if(projects==null || projects.length==0) {
          return new IProject[0];
        }
        ISchedulingRule[] ruleArray = new ISchedulingRule[projects.length];
        for (int i = 0; i < projects.length; i++) {
            ruleArray[i] = projects[i].getWorkspace().getRuleFactory().modifyRule(projects[i]);
		}
        ISchedulingRule rule= MultiRule.combine(ruleArray);
		Job.getJobManager().beginRule(rule, monitor);
        monitor.beginTask("", 1000 * projects.length); //$NON-NLS-1$
        List<IProject> result = new ArrayList<IProject>();
        try {
            for (IProject project : projects) {
                if (monitor.isCanceled()) {
                    break;
                }
                LoadInfo info = infoMap.get(project);
                if (info != null
                        && info.checkout(new SubProgressMonitor(monitor, 1000))) {
                    result.add(project);
                }
            }
        } finally {
    		Job.getJobManager().endRule(rule);
            monitor.done();
        }
        return result.toArray(new IProject[result.size()]);
    }

    /**
     * Internal class for adding projects to the workspace
     */
    protected static class LoadInfo {
        private final ISVNRepositoryLocation repositoryLocation;
        private final String repo;
        private final IProject project;
        private final boolean fromFileSystem;
        private final String directory; // Only used when fromFileSystem is true

        /**
         * Construct a new instance wrappering the specified project reference
         * 
         * @param context
         *            the context of where the reference came from
         * @param projRef
         *            the project reference
         */
        LoadInfo(ProjectSetSerializationContext context,
                StringTokenizer tokenizer) throws SVNException {
            repo = tokenizer.nextToken();
            String projectName = tokenizer.nextToken();

            project = ResourcesPlugin.getWorkspace().getRoot().getProject(
                    projectName);
            if (repo.indexOf("://") != -1) { //$NON-NLS-1$
                // Create connection to repository root.
            	repositoryLocation = SVNRepositoryLocation.fromString(repo, false, true);
                fromFileSystem = false;
                directory = null;
            } else {
                // Assume this is an already checked
                // out project, from the filesystem
                repositoryLocation = null;
                fromFileSystem = true;

                // Is it relative? If so, expand it
                // from the psf file location
                if (!new Path(repo).isAbsolute()) {
                    String baseDir;

                    if (context.getFilename() != null) {
                        baseDir = new File(context.getFilename()).getParent();
                    } else {
                        // Use the workspace root directory as
                        // basedir, this shouldn't happen
                        baseDir = project.getWorkspace().getRoot()
                                .getLocation().toOSString();
                    }
                    try {
                        directory = new File(baseDir + File.separatorChar
                                + repo).getCanonicalPath();
                    } catch (IOException ioe) {
                        throw new SVNException(
                                "Path expansion/canonicalization failed", ioe);
                    }

                } else {
                    directory = repo;
                }
            }

        }

        /**
         * Answer the project referenced by this object. The project may or may
         * not already exist.
         * 
         * @return the project (not <code>null</code>)
         */
        protected IProject getProject() {
            return project;
        }

        /**
         * Checkout the project specified by this reference.
         * 
         * @param monitor
         *            project monitor
         * @return true if loaded, else false
         * @throws TeamException
         */
        boolean checkout(IProgressMonitor monitor) throws TeamException, MalformedURLException {
            if (fromFileSystem) {
                return importExistingProject(monitor);
            } else {
                if (repositoryLocation == null) {
                    return false;
                }
                RemoteFolder remoteFolder = new RemoteFolder(repositoryLocation, new SVNUrl(repo), repositoryLocation.getRootFolder().getRevision());
                CheckoutCommand command = new CheckoutCommand(
                        new ISVNRemoteFolder[] { remoteFolder }, new IProject[] { project });                             
                command.run(monitor);
                return true;
            }
        }

        /**
         * Imports a existing SVN Project to the workbench
         * 
         * @param monitor
         *            project monitor
         * @return true if loaded, else false
         * @throws TeamException
         */

        boolean importExistingProject(IProgressMonitor monitor)
                throws TeamException {
            if (directory == null) {
                return false;
            }
            try {
                monitor.beginTask("Importing", 3 * 1000);

                createExistingProject(new SubProgressMonitor(monitor, 1000));

                monitor.subTask("Refreshing " + project.getName());
                RepositoryProvider.map(project, SVNProviderPlugin.getTypeId());
                monitor.worked(1000);
                SVNWorkspaceRoot.setSharing(project, new SubProgressMonitor(
                        monitor, 1000));

                return true;
            } catch (CoreException ce) {
                throw new SVNException("Failed to import External SVN Project"
                        + ce, ce);
            } finally {
                monitor.done();
            }
        }

        /**
         * Creates a new project in the workbench from an existing one
         * 
         * @param monitor
         * @throws CoreException
         */

        void createExistingProject(IProgressMonitor monitor)
                throws CoreException {
            String projectName = project.getName();
            IProjectDescription description;

            try {
                monitor.beginTask("Creating " + projectName, 2 * 1000);

                description = ResourcesPlugin.getWorkspace()
                        .loadProjectDescription(
                                new Path(directory + File.separatorChar
                                        + ".project")); //$NON-NLS-1$

                description.setName(projectName);
                project.create(description, new SubProgressMonitor(monitor,
                        1000));
                project.open(new SubProgressMonitor(monitor, 1000));
            } finally {
                monitor.done();
            }
        }

    }
}