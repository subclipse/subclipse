/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Common Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors: Cédric Chabanois (cchabanois@ifrance.com) - modified for
 * Subversion
 ******************************************************************************/
package org.tigris.subversion.subclipse.test;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

import junit.framework.TestCase;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IType;
import org.eclipse.team.core.RepositoryProvider;
import org.eclipse.team.core.TeamException;
import org.tigris.subversion.subclipse.core.ISVNLocalResource;
import org.tigris.subversion.subclipse.core.ISVNRepositoryLocation;
import org.tigris.subversion.subclipse.core.SVNClientManager;
import org.tigris.subversion.subclipse.core.SVNException;
import org.tigris.subversion.subclipse.core.SVNProviderPlugin;
import org.tigris.subversion.subclipse.core.SVNTeamProvider;
import org.tigris.subversion.subclipse.core.repo.SVNRepositories;
import org.tigris.subversion.subclipse.core.resources.SVNWorkspaceRoot;
import org.tigris.subversion.svnclientadapter.ISVNClientAdapter;
import org.tigris.subversion.svnclientadapter.SVNUrl;

public abstract class SubclipseTest extends TestCase {
    protected SVNRepositories repositories;

    protected ISVNRepositoryLocation repositoryLocation;

    protected File reposPath;

    protected SVNUrl url;

    protected String user;

    protected String pass;

    protected String remoteHttpsUrl;

    protected String remoteHttpUrl;

    public SubclipseTest(String name) {
        super(name);
    }

    /*
     * (non-Javadoc)
     * 
     * @see junit.framework.TestCase#setUp()
     */
    protected void setUp() throws Exception {
        // in case we're testing auth or something.
        remoteHttpUrl = System.getProperty("remote.http.url");
        remoteHttpsUrl = System.getProperty("remote.https.url");
        user = System.getProperty("svn.user");
        pass = System.getProperty("svn.pass");

        // do we use javahl or command line ?
        String mode = System.getProperty("svn.mode");
        SVNProviderPlugin plugin = SVNProviderPlugin.getPlugin();
        SVNClientManager svnClientManager = plugin.getSVNClientManager();
        if (mode == null)
        	mode = "javahl";
        
        svnClientManager.setSvnClientInterface(mode.toLowerCase());

        // we create the repository
        ISVNClientAdapter svnClient = svnClientManager.createSVNClient();
        reposPath = new File(System.getProperty("java.io.tmpdir")
                + "/test_repos").getAbsoluteFile();
        removeDir(reposPath);

        svnClient.createRepository(reposPath,
                ISVNClientAdapter.REPOSITORY_FSTYPE_FSFS);
        assertTrue(reposPath.exists());
        // we need the corresponding url
        url = new SVNUrl(reposPath.toURI().toString().replaceFirst("file:/",
                "file:///"));
        
        
        // get the ISVNRepositoryLocation corresponding to our repository
        repositories = plugin.getRepositories();
        Properties properties = new Properties();

        properties.setProperty("url", url.toString());
        if (user != null)
            properties.setProperty("user", user);
        if (pass != null)
            properties.setProperty("password", pass);

        repositoryLocation = repositories.createRepository(properties);
    }

    /**
     * remove the given directory
     * 
     * @param d
     * @throws IOException
     */
    private void removeDir(File d) throws IOException {
        if (!d.exists()) {
            return;
        }

        String[] list = d.list();
        if (list == null) {
            list = new String[0];
        }
        for (int i = 0; i < list.length; i++) {
            String s = list[i];
            File f = new File(d, s);
            if (f.isDirectory()) {
                removeDir(f);
            } else {
                if (!f.delete()) {
                    String message = "Unable to delete file "
                            + f.getAbsolutePath();
                    throw new IOException(message);
                }
            }
        }
        if (!d.delete()) {
            String message = "Unable to delete directory "
                    + d.getAbsolutePath();
            throw new IOException(message);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see junit.framework.TestCase#tearDown()
     */
    protected void tearDown() throws Exception {
        TestProject.waitForIndexer();
        // delete all the projects we created in the test
        IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
        IProject[] projects = root.getProjects();
        for (int i = 0; i < projects.length; i++) {
            IProject project = projects[i];
            SVNTeamProvider teamProvider = (SVNTeamProvider) RepositoryProvider
                    .getProvider(project, SVNProviderPlugin.getTypeId());
            if (teamProvider != null) {
                unshareProject(project);
            }
            Exception e = null;
            int retry = 0;
            try {
                // delete the project
                project.delete(true, true, null);
            } catch (Exception ex) {
                e = ex;
            }
        }
        // remove all the repositories
        ISVNRepositoryLocation[] svnRepositoryLocations = repositories
                .getKnownRepositories(new NullProgressMonitor());
        for (int i = 0; i < svnRepositoryLocations.length; i++) {
            repositories.disposeRepository(svnRepositoryLocations[i]);
        }

        // delete the repository
        removeDir(reposPath);
    }

    /**
     * create a project with a Class
     * 
     * @param projectName
     * @return
     * @throws CoreException
     */
    protected TestProject createProjectWithAClass(String projectName)
            throws CoreException {
        TestProject testProject = new TestProject(projectName);

        // create a file
        IPackageFragment package1 = testProject.createPackage("pack1");
        IType type = testProject.createJavaType(package1, "AClass.java",
                "public class AClass { \n" + "  public void m() {}\n" + "}");

        return testProject;
    }

    /**
     * share the project using svn
     * 
     * @throws Exception
     */
    protected void shareProject(IProject project) throws TeamException {
        SVNWorkspaceRoot.shareProject(repositoryLocation, project, null, null, true, new NullProgressMonitor());
    }

    /**
     * unshare the project (do not delete .svn directories)
     * 
     * @throws TeamException
     */
    protected void unshareProject(IProject project) throws TeamException {
        RepositoryProvider.unmap(project);
    }

    /**
     * @return
     */
    protected void addAndCommit(IProject project, IResource resource,
            String comment) throws SVNException, TeamException {
        SVNTeamProvider provider = getProvider(project);
        // add it to repository
        provider.add(new IResource[] { resource }, IResource.DEPTH_ZERO, null);
        // commit it
        provider.checkin(new IResource[] { resource }, comment, false,
                IResource.DEPTH_ZERO, null);
        ISVNLocalResource res = SVNWorkspaceRoot.getSVNResourceFor(resource);
        try {
            resource.refreshLocal(IResource.DEPTH_INFINITE, null);
        } catch (CoreException e) {
            fail(e.getMessage());
        }

    }

    protected void addNoCommit(IProject project, IResource resource)
            throws SVNException, TeamException {
        SVNTeamProvider provider = getProvider(project);
        // add it to repository
        provider.add(new IResource[] { resource }, IResource.DEPTH_ZERO, null);
        try {
            resource.refreshLocal(IResource.DEPTH_INFINITE, null);
            assertTrue(SVNWorkspaceRoot.getSVNResourceFor(resource).getStatus()
                    .isAdded());
        } catch (CoreException e) {
            fail(e.getMessage());
        }

    }

    /**
     * @return
     */
    public ISVNRepositoryLocation getRepositoryLocation() {
        return repositoryLocation;
    }

    public SVNTeamProvider getProvider(IProject project) {
        return (SVNTeamProvider) RepositoryProvider.getProvider(project,
                SVNProviderPlugin.getTypeId());
    }
}