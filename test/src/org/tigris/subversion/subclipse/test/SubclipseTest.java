/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     Cédric Chabanois (cchabanois@ifrance.com) - modified for Subversion 
 *******************************************************************************/
package org.tigris.subversion.subclipse.test;

import java.util.Properties;

import junit.framework.TestCase;

import org.eclipse.team.core.RepositoryProvider;
import org.eclipse.team.core.TeamException;
import org.tigris.subversion.subclipse.core.ISVNRepositoryLocation;
import org.tigris.subversion.subclipse.core.SVNProviderPlugin;
import org.tigris.subversion.subclipse.core.SVNTeamProvider;
import org.tigris.subversion.subclipse.core.repo.SVNRepositories;
import org.tigris.subversion.subclipse.core.resources.SVNWorkspaceRoot;


public abstract class SubclipseTest extends TestCase {
	protected TestProject testProject;
	protected SVNRepositories repositories;
	protected ISVNRepositoryLocation repositoryLocation;
	protected SVNTeamProvider provider = null;
	protected BuildFile buildFile = new BuildFile();

	public SubclipseTest(String name) {
		super(name);
	}

	/* (non-Javadoc)
	 * @see junit.framework.TestCase#setUp()
	 */
	protected void setUp() throws Exception {
		buildFile.configureProject("test/build.xml");
		
		// create the repository, set the properties (urlRepos)
		buildFile.executeTarget("init");
		
		// create a project in the workspace
		testProject = new TestProject();
		
		// get the ISVNRepositoryLocation corresponding to our repository
		SVNProviderPlugin plugin = SVNProviderPlugin.getPlugin();
		repositories = plugin.getRepositories();
		Properties properties = new Properties();
		properties.setProperty("url",buildFile.getProject().getProperty("urlRepos"));
		repositoryLocation = repositories.createRepository(properties);
	}

	/* (non-Javadoc)
	 * @see junit.framework.TestCase#tearDown()
	 */
	protected void tearDown() throws Exception {
		unshareProject();
		
		Exception e = null;
		int retry = 0;
		
		try {
			// delete the project
			testProject.dispose();
		} catch(Exception ex) {
			e = ex;
		}
		
		// remove all the repositories
		ISVNRepositoryLocation[] svnRepositoryLocations = repositories.getKnownRepositories();
		for (int i = 0; i < svnRepositoryLocations.length;i++) {
			repositories.disposeRepository(svnRepositoryLocations[i]);			
		}
	}

	/**
	 * share the project using svn
	 * @throws Exception
	 */
	protected void shareProject() throws TeamException {
		SVNWorkspaceRoot.shareProject(repositoryLocation,testProject.getProject(),null,null);
		
		provider = (SVNTeamProvider)RepositoryProvider.getProvider(testProject.getProject(), SVNProviderPlugin.getTypeId());		
	}

	/**
	 * unshare the project (do not delete .svn directories)
	 * @throws TeamException
	 */
	protected void unshareProject() throws TeamException {
		RepositoryProvider.unmap(testProject.getProject());
	}

	/**
	 * @return
	 */
	public BuildFile getBuildFile() {
		return buildFile;
	}

	/**
	 * @return
	 */
	public SVNTeamProvider getProvider() {
		return provider;
	}

	/**
	 * @return
	 */
	public ISVNRepositoryLocation getRepositoryLocation() {
		return repositoryLocation;
	}

	/**
	 * @return
	 */
	public TestProject getTestProject() {
		return testProject;
	}

}
