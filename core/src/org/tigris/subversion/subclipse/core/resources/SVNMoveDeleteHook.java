/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     Daniel Bradby 
 *******************************************************************************/
package org.tigris.subversion.subclipse.core.resources;

import java.io.File;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.team.IMoveDeleteHook;
import org.eclipse.core.resources.team.IResourceTree;
import org.eclipse.core.runtime.IProgressMonitor;
import org.tigris.subversion.subclipse.core.ISVNLocalFile;
import org.tigris.subversion.subclipse.core.ISVNLocalFolder;
import org.tigris.subversion.subclipse.core.ISVNResource;
import org.tigris.subversion.subclipse.core.SVNException;
import org.tigris.subversion.subclipse.core.client.OperationManager;
import org.tigris.subversion.svnclientadapter.SVNClientAdapter;

import com.qintsoft.jsvn.jni.ClientException;
import com.qintsoft.jsvn.jni.Status;

public class SVNMoveDeleteHook implements IMoveDeleteHook {

	public boolean deleteFile(IResourceTree tree, IFile file, int updateFlags, IProgressMonitor monitor) {

		if (!isMoveable(file))
			return false;

		try {
			ISVNLocalFile resource = new LocalFile(file);
			SVNClientAdapter svnClient = resource.getRepository().getSVNClient();
			monitor.beginTask(null, 1000);
			monitor.setTaskName("Working..");
            resource.delete();
			tree.deletedFile(file);

		} catch (SVNException e) {
			tree.failed(
				new org.eclipse.core.runtime.Status(
					org.eclipse.core.runtime.Status.ERROR,
					"SUBCLIPSE",
					0,
					"Error removing file",
					e));
			e.printStackTrace();
			return false;
		} finally {
			monitor.done();
		}
		return true;

	}

	public boolean deleteFolder(IResourceTree tree, IFolder folder, int updateFlags, IProgressMonitor monitor) {

		ISVNLocalFolder resource = new LocalFolder(folder);

		if (!isMoveable(folder))
			return false;

		try {
			SVNClientAdapter svnClient = resource.getRepository().getSVNClient();
			monitor.beginTask(null, 1000);
			monitor.setTaskName("Working..");
            resource.delete();
			tree.deletedFolder(folder);
		} catch (SVNException e) {
			tree.failed(
				new org.eclipse.core.runtime.Status(
					org.eclipse.core.runtime.Status.ERROR,
					"SUBCLIPSE",
					0,
					"Error removing folder",
					e));
			e.printStackTrace();
			return false;
		} finally {
			monitor.done();
		}
		return true;
	}

	public boolean deleteProject(IResourceTree tree, IProject project, int updateFlags, IProgressMonitor monitor) {
		return false;
	}

	public boolean moveFile(
		IResourceTree tree,
		IFile source,
		IFile destination,
		int updateFlags,
		IProgressMonitor monitor) {

		if (!isMoveable(source))
			return false;

		try {
			ISVNResource resource = new LocalFile(source);
			SVNClientAdapter svnClient = resource.getRepository().getSVNClient();
			monitor.beginTask(null, 1000);
			monitor.setTaskName("Working..");

            try {
                OperationManager.getInstance().beginOperation(svnClient);
			    svnClient.move(source.getLocation().toFile(), destination.getLocation().toFile());
                
                // movedFile must be done before endOperation because 
                // destination file must not already exist in the workspace 
                // resource tree.
                tree.movedFile(source, destination);
            } catch (ClientException e) {
                throw SVNException.wrapException(e); 
            } finally {
                OperationManager.getInstance().endOperation();
            }

			

		} catch (SVNException e) {
			tree.failed(
				new org.eclipse.core.runtime.Status(
					org.eclipse.core.runtime.Status.ERROR,
					"SUBCLIPSE",
					0,
					"Error move file",
					e));
			e.printStackTrace();
			return false;
		} finally {
			monitor.done();
		}
		return true;
	}

	public boolean moveFolder(
		IResourceTree tree,
		IFolder source,
		IFolder destination,
		int updateFlags,
		IProgressMonitor monitor) {

		if (!isMoveable(source))
			return false;

		try {
			monitor.beginTask(null, 1000);
			monitor.setTaskName("Working..");

			ISVNResource resource = new LocalFolder(source);
			SVNClientAdapter svnClient = resource.getRepository().getSVNClient();

            try {
                OperationManager.getInstance().beginOperation(svnClient);
			    svnClient.move(source.getLocation().toFile(), destination.getLocation().toFile());
                tree.movedFolderSubtree(source, destination);
            } catch (ClientException e) {
                throw SVNException.wrapException(e); 
            } finally {
                OperationManager.getInstance().endOperation();
            }

		} catch (SVNException e) {
			tree.failed(
				new org.eclipse.core.runtime.Status(
					org.eclipse.core.runtime.Status.ERROR,
					"SUBCLIPSE",
					0,
					"Error move Folder " + source.getLocation(),
					e));
			e.printStackTrace();
			return false;
		} finally {
			monitor.done();
		}
		return true;
	}

	public boolean moveProject(
		IResourceTree tree,
		IProject source,
		IProjectDescription description,
		int updateFlags,
		IProgressMonitor monitor) {
		return false;
	}

	private boolean isMoveable(IFile file)
	{
		if (file.isTeamPrivateMember())
			return false;
		
		return isMoveable(new LocalFile(file));
	}
	
	private boolean isMoveable(IFolder folder)
	{
		if (folder.isTeamPrivateMember())
			return false;		
		
		return isMoveable(new LocalFolder(folder));	
	}
	

	private boolean isMoveable(LocalResource resource) {
		try {
			LocalResource svnResource = (LocalResource) resource;
			SVNClientAdapter svnClient = svnResource.getRepository().getSVNClient();
			Status status = svnClient.getSingleStatus(svnResource.getFile());

			if (!status.isManaged())
				return false;

		} catch (Exception e1) {
			e1.printStackTrace();
			return false;
		}

		return true;
	}
}
