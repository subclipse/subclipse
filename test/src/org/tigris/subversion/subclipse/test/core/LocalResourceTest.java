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

import java.io.InputStream;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IType;
import org.tigris.subversion.subclipse.core.ISVNLocalResource;
import org.tigris.subversion.subclipse.core.ISVNRemoteResource;
import org.tigris.subversion.subclipse.core.SVNTeamProvider;
import org.tigris.subversion.subclipse.core.resources.SVNWorkspaceRoot;
import org.tigris.subversion.subclipse.test.SubclipseTest;
import org.tigris.subversion.subclipse.test.TestProject;



public class LocalResourceTest extends SubclipseTest {

	public LocalResourceTest(String name) {
		super(name);
	}

	public void testUrl() throws Exception {
		TestProject testProject = new TestProject("testProject");
		shareProject(testProject.getProject());
		
		// create a file
		IPackageFragment package1 = testProject.createPackage("pack1");
		IType type = testProject.createJavaType(package1,"AClass.java",
			"public class AClass { \n" +
			"  public void m() {}\n" +
			"}");

		IResource resource = testProject.getProject().getFile(new Path("src/pack1/AClass.java"));
		ISVNLocalResource svnResource = SVNWorkspaceRoot.getSVNResourceFor(resource);
		
		// get the url : this is not direct as the resource is not yet managed
		assertEquals(getRepositoryLocation().getUrl().toString()+"/"+testProject.getProject().getName()+"/src/pack1/AClass.java",svnResource.getUrl().toString());
		
		// add it to repository
		getProvider(testProject.getProject()).add(new IResource[] {resource},IResource.DEPTH_ZERO, null);
		
		// get the url : this should be direct as the resource is managed
		assertEquals(getRepositoryLocation().getUrl().toString()+"/"+testProject.getProject().getName()+"/src/pack1/AClass.java",svnResource.getUrl().toString());
	}
	
	public void testGetRemote() throws Exception {
			TestProject testProject = new TestProject("testProject");
			shareProject(testProject.getProject());
		
			// create a file
			IPackageFragment package1 = testProject.createPackage("pack1");
			IType type = testProject.createJavaType(package1,"AClass.java",
				"public class AClass { \n" +
				"  public void m() {}\n" +
				"}");
			
			IFile resource = testProject.getProject().getFile(new Path("src/pack1/AClass.java"));
			ISVNLocalResource svnResource = SVNWorkspaceRoot.getSVNResourceFor(resource);
			InputStream isLocal = resource.getContents();
		
			SVNTeamProvider provider = getProvider(testProject.getProject());
		
			// add it to repository
			provider.add(new IResource[] {resource},IResource.DEPTH_ZERO, null);
			
			// commit it
			provider.checkin(new IResource[] {resource},"committed",IResource.DEPTH_ZERO,null);
		
			// get the remote resource 
			ISVNRemoteResource svnRemoteResource = svnResource.getLatestRemoteResource();
			
			// compare the contents
			InputStream isRemote = svnRemoteResource.getContents(null);
			assertEquals(isLocal.read(new byte[1000]),isRemote.read(new byte[1000]));
		}
	
	
}
