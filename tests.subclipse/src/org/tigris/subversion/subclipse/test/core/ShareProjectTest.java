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
package org.tigris.subversion.subclipse.test.core;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.team.core.RepositoryProvider;
import org.eclipse.team.core.TeamException;
import org.tigris.subversion.subclipse.core.ISVNRepositoryLocation;
import org.tigris.subversion.subclipse.core.SVNProviderPlugin;
import org.tigris.subversion.subclipse.core.SVNTeamProvider;
import org.tigris.subversion.subclipse.core.repo.SVNRepositoryLocation;
import org.tigris.subversion.subclipse.core.resources.SVNWorkspaceRoot;
import org.tigris.subversion.subclipse.test.SubclipseTest;
import org.tigris.subversion.subclipse.test.TestProject;

public class ShareProjectTest extends SubclipseTest {

	public ShareProjectTest(String name) {
		super(name);
	}
	
	public void testShare() throws Exception {
		
		TestProject testProject = new TestProject("testProject");
		// we share the project
		shareProject(testProject.getProject());
		
		// make sure the project is shared
		assertEquals(
			SVNProviderPlugin.getTypeId(), 
			testProject.getProject().getPersistentProperty(new QualifiedName("org.eclipse.team.core", "repository")));
		
		// get the SVNWorkspaceRoot for this project
		SVNTeamProvider teamProvider = (SVNTeamProvider)RepositoryProvider.getProvider(testProject.getProject(), SVNProviderPlugin.getTypeId());
		SVNWorkspaceRoot svnProject = teamProvider.getSVNWorkspaceRoot();
		
		// just make sure there is a SVNRepositoryLocation associated
		assertNotNull(svnProject.getRepository());
		
		// unshare and make sure it does not have the svn nature anymore
		unshareProject(testProject.getProject());
		assertFalse(
			SVNProviderPlugin.getTypeId().equals(
				testProject.getProject().getPersistentProperty(new QualifiedName("org.eclipse.team.core", "repository"))));
		
	}

	public void testInvalidShare() throws Exception {
		TestProject testProject = new TestProject("testProject");
		
		ISVNRepositoryLocation location = SVNRepositoryLocation.fromString("file:///home/invaliduser/svnrepos");
		
		try {
			SVNWorkspaceRoot.shareProject(location,testProject.getProject(),null,null, true, new NullProgressMonitor());
			fail("project should not have been shared");
		} catch (TeamException e) {
		}
		
	}


}
