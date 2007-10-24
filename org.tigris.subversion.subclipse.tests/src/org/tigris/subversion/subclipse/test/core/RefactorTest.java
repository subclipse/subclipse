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

import org.eclipse.core.internal.resources.ResourceException;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IType;
import org.tigris.subversion.subclipse.core.ISVNLocalResource;
import org.tigris.subversion.subclipse.core.SVNTeamProvider;
import org.tigris.subversion.subclipse.core.resources.SVNWorkspaceRoot;
import org.tigris.subversion.subclipse.test.SubclipseTest;
import org.tigris.subversion.subclipse.test.TestProject;
import org.tigris.subversion.svnclientadapter.SVNStatusKind;

public class RefactorTest extends SubclipseTest {

	public RefactorTest(String name) {
		super(name);
	}
	public void testAddedClassRename() throws Exception {
		TestProject testProject = new TestProject("testProject");
		shareProject(testProject.getProject());
		
		// create a file
		IPackageFragment package1 = testProject.createPackage("pack1");
		IType type = testProject.createJavaType(package1,"AClass.java",
			"public class AClass { \n" +
			"  public void m() {}\n" +
			"}");
			
		IFile resource = testProject.getProject().getFile(new Path("src/pack1/AClass.java"));
		
		// add and commit it
		addNoCommit(testProject.getProject(),resource);

		// let's rename the resource
		resource.move(new Path("AClassRenamed.java"),false, null);
		
		// make sure the initial resource is not there anymore
		assertFalse(resource.exists());
		
		// the initial resource should have "DELETED" status
		ISVNLocalResource svnResource = SVNWorkspaceRoot.getSVNResourceFor(resource);
		assertEquals(svnResource.getStatus().getTextStatus(), SVNStatusKind.UNVERSIONED);
		
		// the renamed resource should exist now
		resource = testProject.getProject().getFile(new Path("src/pack1/AClassRenamed.java"));
		assertTrue(resource.exists());
		
		// and should have "ADDED" status
		svnResource = SVNWorkspaceRoot.getSVNResourceFor(resource);
		assertEquals(svnResource.getStatus().getTextStatus(), SVNStatusKind.ADDED);
	}

	public void testClassRename() throws Exception {
		TestProject testProject = new TestProject("testProject");
		shareProject(testProject.getProject());
		
		// create a file
		IPackageFragment package1 = testProject.createPackage("pack1");
		IType type = testProject.createJavaType(package1,"AClass.java",
			"public class AClass { \n" +
			"  public void m() {}\n" +
			"}");
			
		IFile resource = testProject.getProject().getFile(new Path("src/pack1/AClass.java"));
		
		// add and commit it
		addAndCommit(testProject.getProject(),resource,"committed");

		// let's rename the resource
		resource.move(new Path("AClassRenamed.java"),false, null);
		
		// make sure the initial resource is not there anymore
		assertFalse(resource.exists());
		
		// the initial resource should have "DELETED" status
		ISVNLocalResource svnResource = SVNWorkspaceRoot.getSVNResourceFor(resource);
		assertEquals(svnResource.getStatus().getTextStatus(), SVNStatusKind.DELETED);
		
		// the renamed resource should exist now
		resource = testProject.getProject().getFile(new Path("src/pack1/AClassRenamed.java"));
		assertTrue(resource.exists());
		
		// and should have "ADDED" status
		svnResource = SVNWorkspaceRoot.getSVNResourceFor(resource);
		assertEquals(svnResource.getStatus().getTextStatus(), SVNStatusKind.ADDED);
	}
	
	public void testPackageRenameFailsWithForceFalse() throws Exception {
		TestProject testProject = new TestProject("testProject");
		shareProject(testProject.getProject());
		
		// create a file
		IPackageFragment package1 = testProject.createPackage("pack1");
		IType type = testProject.createJavaType(package1,"AClass.java",
			"public class AClass { \n" +
			"  public void m() {}\n" +
			"}");

		SVNTeamProvider provider = getProvider(testProject.getProject());
		
		IFile resource = testProject.getProject().getFile(new Path("src/pack1/AClass.java"));
		
		// add and commit it
		addAndCommit(testProject.getProject(),resource,"committed");
		
		// let's rename the package
		IFolder folder =  testProject.getProject().getFolder(new Path("src/pack1"));
		try{
			folder.move(new Path("pack2"),false, null);
			fail("Should fail, setting force to false will force an exception when tree is out of sync in SVNMoveDeleteHook");
		}catch(ResourceException e){
			
		}

		
		
	}
	
	public void testPackageRenameWithForce() throws Exception {
		TestProject testProject = new TestProject("testProject");
		shareProject(testProject.getProject());
		
		// create a file
		IPackageFragment package1 = testProject.createPackage("pack1");
		IType type = testProject.createJavaType(package1,"AClass.java",
			"public class AClass { \n" +
			"  public void m() {}\n" +
			"}");

		SVNTeamProvider provider = getProvider(testProject.getProject());
		
		IFile resource = testProject.getProject().getFile(new Path("src/pack1/AClass.java"));
		
		// add and commit it
		addAndCommit(testProject.getProject(),resource,"committed");
		
		// let's rename the package
		IFolder folder =  testProject.getProject().getFolder(new Path("src/pack1"));
		folder.move(new Path("pack2"),true, null);
		
		// note that the initial folder still exist after package renaming
		
		// the renamed package should exist now
		folder = testProject.getProject().getFolder(new Path("src/pack2"));
		assertTrue(folder.exists());
	}
	
	public void testPackageRenameScheduledAdd() throws Exception {
		TestProject testProject = new TestProject("testProject");
		shareProject(testProject.getProject());
		
		// create a file
		IPackageFragment package1 = testProject.createPackage("pack1");
		IType type = testProject.createJavaType(package1,"AClass.java",
			"public class AClass { \n" +
			"  public void m() {}\n" +
			"}");

		SVNTeamProvider provider = getProvider(testProject.getProject());
		
		IFile resource = testProject.getProject().getFile(new Path("src/pack1/AClass.java"));
		
		// add and commit it
		addNoCommit(testProject.getProject(),resource);
		
		// let's rename the package
		IFolder folder =  testProject.getProject().getFolder(new Path("src/pack1"));
		folder.move(new Path("pack2"),true, null);
		
		// note that the initial folder still exist after package renaming
		
		// the renamed package should exist now
		folder = testProject.getProject().getFolder(new Path("src/pack2"));
		assertTrue(folder.exists());
	}

}
