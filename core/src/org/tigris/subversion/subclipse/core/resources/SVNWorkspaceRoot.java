/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Cédric Chabanois (cchabanois@ifrance.com) - modified for Subversion  
 *******************************************************************************/
package org.tigris.subversion.subclipse.core.resources;

import java.io.InputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.eclipse.core.internal.resources.ResourceInfo;
import org.eclipse.core.internal.resources.Workspace;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.team.core.RepositoryProvider;
import org.eclipse.team.core.TeamException;
import org.tigris.subversion.subclipse.core.ISVNFolder;
import org.tigris.subversion.subclipse.core.ISVNLocalFile;
import org.tigris.subversion.subclipse.core.ISVNLocalFolder;
import org.tigris.subversion.subclipse.core.ISVNLocalResource;
import org.tigris.subversion.subclipse.core.ISVNRemoteFolder;
import org.tigris.subversion.subclipse.core.ISVNRemoteResource;
import org.tigris.subversion.subclipse.core.ISVNRepositoryLocation;
import org.tigris.subversion.subclipse.core.ISVNResource;
import org.tigris.subversion.subclipse.core.Policy;
import org.tigris.subversion.subclipse.core.SVNException;
import org.tigris.subversion.subclipse.core.SVNProviderPlugin;
import org.tigris.subversion.subclipse.core.SVNStatus;
import org.tigris.subversion.subclipse.core.commands.CheckoutCommand;
import org.tigris.subversion.subclipse.core.commands.ShareProjectCommand;
import org.tigris.subversion.subclipse.core.util.Util;
import org.tigris.subversion.svnclientadapter.SVNConstants;
import org.tigris.subversion.svnclientadapter.SVNUrl;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;


/**
 * This class provides static methods for checking out projects from a repository
 * into the local workspace and for converting IResources into SVNResources
 * <p>
 * Instances of this class represent a local workspace root (i.e. a project).
 * <br>
 * Here is how you can get a SVNWorkspaceRoot from an IProject : <br>
 * <code>
 * SVNTeamProvider teamProvider = (SVNTeamProvider)RepositoryProvider.getProvider(myIProject, SVNProviderPlugin.getTypeId()); <br>
 * SVNWorkspaceRoot svnProject = teamProvider.getSVNWorkspaceRoot();
 * </code> 
 * </p>
 */
public class SVNWorkspaceRoot {

	private ISVNLocalFolder localRoot;
    private SVNUrl url;
	
	public SVNWorkspaceRoot(IContainer resource){
		this.localRoot = getSVNFolderFor(resource);
	}



	/**
	 * get a project for the remote folder. The name is either the name of the 
	 * remote folder or the name in .project if this file exists.
	 * Project is not created. There is no check to see if the project already exists
	 * @param folder
	 * @param monitor
	 * @return
	 */
	public static IProject getProject(ISVNRemoteFolder folder,IProgressMonitor monitor) {
		String name = folder.getName();
						
		// Check for a better name for the project
		try {
			ISVNResource[] children = folder.members(monitor, ISVNFolder.FILE_MEMBERS);
			for (int k = 0; k < children.length; k++) {
				ISVNResource resource = children[k];
				if(".project".equals(resource.getName())){
					RemoteFile dotProject = (RemoteFile)folder.getRepository().getRemoteFile(new SVNUrl(Util.appendPath(folder.getUrl().get(), ".project")));
																
					InputStream is = dotProject.getStorage(monitor).getContents();
					DocumentBuilder db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
					org.w3c.dom.Document doc = db.parse(is);
					is.close();
					NodeList nl = doc.getDocumentElement().getChildNodes();
					for (int j = 0; j < nl.getLength(); ++j) {
						Node child = nl.item(j);
						if (child instanceof Element && "name".equals(child.getNodeName())) {
							Node grandChild = child.getFirstChild();
							if (grandChild instanceof Text) name = ((Text)grandChild).getData(); 	
						}
					}									
				}
			}

		}	
		catch (Exception e) {
		  // no .project exists ... that's ok
		  // or an error occured while parsing .project (not valid ?)
		}
		IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(name);
		return project;		
	}

    /**
	 * Checkout the remote resources into the local workspace as projects. 
	 * Each resource will be checked out into the corresponding project.
	 * You can use getProject to get a project for a given remote Folder 
	 * 
	 * Resources existing in the local file system at the target project location but now 
	 * known to the workbench will be overwritten.
	 */
	public static void checkout(
		final ISVNRemoteFolder[] resources,
		final IProject[] projects,
		final IProgressMonitor monitor)
		throws SVNException {
        CheckoutCommand command = new CheckoutCommand(resources, projects);
        command.run(monitor);
	}
					
	/**
	 * Create a remote directory in the SVN repository and link the project directory to this remote directory.
	 * The contents of the project are not imported.
     * if remoteDirName is null, the name of the project is used
     * if location is not in repositories, it is added 
	 */
	public static void shareProject(ISVNRepositoryLocation location, IProject project, String remoteDirName, IProgressMonitor monitor) throws TeamException {
		ShareProjectCommand command = new ShareProjectCommand(location, project, remoteDirName);
        command.run(monitor);
    }
	
	/**
	 * Set the sharing for a project to enable it to be used with the SVNTeamProvider.
     * This is used when a project has .svn directory but is not shared in Eclipse.
	 * An exception is thrown if project does not have a remote directory counterpart
	 */
	public static void setSharing(IProject project, IProgressMonitor monitor) throws TeamException {
		
		// Ensure provided info matches that of the project
		ISVNLocalFolder folder = (ISVNLocalFolder)SVNWorkspaceRoot.getSVNResourceFor(project);
		LocalResourceStatus status = folder.getStatus();
        
        // this folder needs to be managed but also to have a remote counter-part
        // because we need to know its url
        // we will change this exception !
        if (!status.hasRemote())
            throw new SVNException(new SVNStatus(SVNStatus.ERROR, Policy.bind("SVNProvider.infoMismatch", project.getName())));//$NON-NLS-1$
        
		// Ensure that the provided location is managed
		SVNProviderPlugin.getPlugin().getRepositories().getRepository(status.getUrl().toString());
		
		// Register the project with Team
		RepositoryProvider.map(project, SVNProviderPlugin.getTypeId());
	}
		
	
	/**
	 * Returns a resource path to the given local location. Returns null if
	 * it is not under a project's location.
	 * @see FileSystemResourceManager#pathForLocation(org.eclipse.core.runtime.IPath)
	 */
	public static IPath pathForLocation(IPath location) {
		if (Platform.getLocation().equals(location))
			return Path.ROOT;
		IProject[] projects = ResourcesPlugin.getWorkspace().getRoot().getProjects();
		for (int i = 0; i < projects.length; i++) {
			IProject project = projects[i];
			IPath projectLocation = project.getLocation();
			if (projectLocation != null && projectLocation.isPrefixOf(location)) {
				int segmentsToRemove = projectLocation.segmentCount();
				return project.getFullPath().append(location.removeFirstSegments(segmentsToRemove));
			}
		}
		return null;
	}

    /**
     * get the SVNLocalFolder for the given resource 
     */           	
	public static ISVNLocalFolder getSVNFolderFor(IContainer resource) {
		return new LocalFolder(resource);
	}

    /**
     * get the SVNLocalFile for the given resource 
     */
	public static ISVNLocalFile getSVNFileFor(IFile resource) {
		return new LocalFile(resource);
	}

    /**
     * get the SVNLocalResource for the given resource 
     */
	public static ISVNLocalResource getSVNResourceFor(IResource resource) {
		if (resource.getType() == IResource.FILE)
			return getSVNFileFor((IFile) resource);
		else // container
			return getSVNFolderFor((IContainer) resource);
	}
	
    /**
     * get the SVNLocalResources for the given resources
     * @param resources
     * @return
     */
    public static ISVNLocalResource[] getSVNResourcesFor(IResource resources[]) {
        ISVNLocalResource[] svnResources = new ISVNLocalResource[resources.length];
        for (int i = 0; i < resources.length;i++) {
            svnResources[i] = getSVNResourceFor(resources[i]);
        }
        return svnResources;
    }
    
    /**
     * get the base resource corresponding to the local one
     * @param resource
     * @return
     * @throws SVNException
     */
	public static ISVNRemoteResource getBaseResourceFor(IResource resource) throws SVNException {
		ISVNLocalResource managed = getSVNResourceFor(resource);
		return managed.getBaseResource();
	}
	
    /**
     * get the latest remote resource corresponding to the local one
     * @param resource
     * @return
     * @throws SVNException
     */
    public static ISVNRemoteResource getLatestResourceFor(IResource resource) throws SVNException {
        ISVNLocalResource managed = getSVNResourceFor(resource);
        return managed.getLatestRemoteResource();        
    }
    
   

	/**
     * get the repository for this project 
	 */
	public ISVNRepositoryLocation getRepository() throws SVNException {
		if (url == null)
        {
            LocalResourceStatus status = localRoot.getStatus();
            if (!status.isManaged()) {
                throw new SVNException(Policy.bind("SVNWorkspaceRoot.notSVNFolder", localRoot.getName()));  //$NON-NLS-1$
            }
            url = status.getUrl();
        }
		return SVNProviderPlugin.getPlugin().getRepository(url.toString());
	}

    /**
     * get the svn folder corresponding to the project  
     */
	public ISVNLocalFolder getLocalRoot() {
		return localRoot;
	}
	
	/**
	 * Return true if the resource is part of a link (i.e. a linked resource or
	 * one of it's children.
	 * 
	 * @param container
	 * @return boolean
	 */
	public static boolean isLinkedResource(IResource resource) {
		// check the resource directly first
		if (resource.isLinked()) return true;
		// projects and root cannot be links
		if (resource.getType() == IResource.PROJECT || resource.getType() == IResource.ROOT) {
			return false;
		}
		// look one level under the project to see if the resource is part of a link
		String linkedParentName = resource.getProjectRelativePath().segment(0);
		IFolder linkedParent = resource.getProject().getFolder(linkedParentName);
		return linkedParent.isLinked();
	}
	
	/**
	 * Return true when a resource is a SVN "meta" resource.
	 * I.e. .svn dir or any file within it.
	 * @param resource
	 * @return
	 */
	public static boolean isSvnMetaResource(IResource resource)
	{
		if ((resource.getType() == IResource.FOLDER) && (resource.getName().equals(SVNConstants.SVN_DIRNAME)))
			return true;
		
        IResource parent = resource.getParent();
        if (parent == null) {
            return false;
        }
        else
        {
        	return isSvnMetaResource(parent);
        }
	}
	
	/**
	 * Get the ResourceInfo object for resource identified by location path.
	 * @param statusPath - an absolute path relative to workspace root as returned by pathForLocation()
	 * @return a resourceInfo
	 */
	public static ResourceInfo getResourceInfoFor(IPath statusPath)
	{
		return ((Workspace) ResourcesPlugin.getWorkspace())
					.getResourceInfo(statusPath, true, false);
	}	
}

