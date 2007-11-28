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

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.ui.actions.WorkspaceModifyOperation;
import org.tigris.subversion.subclipse.core.ISVNRemoteFolder;
import org.tigris.subversion.subclipse.core.SVNProviderPlugin;
import org.tigris.subversion.subclipse.core.SVNTeamProvider;
import org.tigris.subversion.subclipse.core.commands.CheckoutCommand;
import org.tigris.subversion.subclipse.core.resources.SVNWorkspaceRoot;
import org.tigris.subversion.subclipse.test.SubclipseTest;
import org.tigris.subversion.subclipse.test.TestProject;

public class CheckoutTest extends SubclipseTest {

	public CheckoutTest(String name) {
		super(name);
	}

	public void testCheckoutProject() throws Exception {
		// first we create a project, share it and commit it
		TestProject project1 = createProjectWithAClass("project1");
		shareProject(project1.getProject());
		
		SVNTeamProvider provider = getProvider(project1.getProject());
		IResource src = project1.getProject().getFolder(new Path("src"));
		IResource projectFile = project1.getProject().getFile(new Path(".project"));
		provider.add(new IResource[] { src,projectFile },IResource.DEPTH_INFINITE, null);
		provider.checkin(new IResource[] { src,projectFile }, "project committed to repository", false, IResource.DEPTH_INFINITE,null);
		
		// let's remove it
		project1.getProject().delete(true,true,null);
		
		// and checkout it
		ISVNRemoteFolder remoteFolder = repositoryLocation.getRemoteFolder("project1");
		IProject project = SVNWorkspaceRoot.getProject(remoteFolder,null);
//		SVNWorkspaceRoot.checkout(
//			new ISVNRemoteFolder[] {remoteFolder},
//			new IProject[] {project},new NullProgressMonitor());
		final ISVNRemoteFolder[] remoteFolders = { remoteFolder };
		final IProject[] localFolders = { project };
		WorkspaceModifyOperation workspaceModifyOperation = new WorkspaceModifyOperation() {
			protected void execute(IProgressMonitor monitor)
					throws CoreException, InvocationTargetException,
					InterruptedException {
				CheckoutCommand checkoutCommand = new CheckoutCommand(remoteFolders, localFolders);
				checkoutCommand.run(new NullProgressMonitor());
			}			
		};
		workspaceModifyOperation.run(new NullProgressMonitor());
		// make sure the project is shared
		assertEquals(
			SVNProviderPlugin.getTypeId(), 
			project1.getProject().getPersistentProperty(new QualifiedName("org.eclipse.team.core", "repository")));
		
		// make sure project has java nature (.project was committed, so nature should not be lost)
		assertTrue(project1.getProject().hasNature(JavaCore.NATURE_ID));		
	}

}
