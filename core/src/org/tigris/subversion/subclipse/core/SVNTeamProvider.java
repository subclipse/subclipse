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
package org.tigris.subversion.subclipse.core;
 
import org.eclipse.core.resources.IFileModificationValidator;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectNature;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceRuleFactory;
import org.eclipse.core.resources.team.IMoveDeleteHook;
import org.eclipse.core.resources.team.ResourceRuleFactory;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.team.core.RepositoryProvider;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.core.variants.IResourceVariant;
import org.tigris.subversion.subclipse.core.commands.AddResourcesCommand;
import org.tigris.subversion.subclipse.core.commands.CheckinResourcesCommand;
import org.tigris.subversion.subclipse.core.commands.SwitchToUrlCommand;
import org.tigris.subversion.subclipse.core.resources.RemoteFile;
import org.tigris.subversion.subclipse.core.resources.RemoteFolder;
import org.tigris.subversion.subclipse.core.resources.SVNFileModificationValidator;
import org.tigris.subversion.subclipse.core.resources.SVNMoveDeleteHook;
import org.tigris.subversion.subclipse.core.resources.SVNWorkspaceRoot;
import org.tigris.subversion.svnclientadapter.SVNRevision;
import org.tigris.subversion.svnclientadapter.SVNUrl;

/**
 * This class is responsible for configuring a project for repository management
 * and providing the necessary hooks for resource modification
 * This class is created for each project that is associated with a repository provider
 */
public class SVNTeamProvider extends RepositoryProvider {
	private SVNWorkspaceRoot workspaceRoot;
	private Object operations;
	
	/**
	 * Scheduling rule to use when modifying resources.
	 * <code>ResourceRuleFactory</code> only locks the file or its parent if read-only
	 */
	private static final ResourceRuleFactory RESOURCE_RULE_FACTORY = new ResourceRuleFactory() {};
	
	/**
	 * No-arg Constructor for IProjectNature conformance
	 */
	public SVNTeamProvider() {
	}

	/**
	 * @see IProjectNature#deconfigure()
	 */
	public void deconfigure() {

	}
	
    /**
     * @see RepositoryProvider#deconfigured()
     */
	public void deconfigured() {
		SVNProviderPlugin.broadcastProjectDeconfigured(getProject());
	}


	/**
	 * @see IProjectNature#setProject(IProject)
	 */
	public void setProject(IProject project) {
		super.setProject(project);
		try {
			this.workspaceRoot = new SVNWorkspaceRoot(project);
			// Ensure that the project has SVN info
			if (!workspaceRoot.getLocalRoot().hasRemote()) {
				throw new SVNException(new SVNStatus(SVNStatus.ERROR, Policy.bind("SVNTeamProvider.noFolderInfo", project.getName()))); //$NON-NLS-1$
			}
		} catch (SVNException e) {
			// Log any problems creating the CVS managed resource
			SVNProviderPlugin.log(e);
		}
	}

	/**
	 * Add the given resources to the project. 
	 * <p>
	 * The sematics follow that of SVN in the sense that any folders and files
	 * are created remotely on the next commit. 
	 * </p>
	 */
	public void add(IResource[] resources, int depth, IProgressMonitor progress) throws SVNException {	
		AddResourcesCommand command = new AddResourcesCommand(getSVNWorkspaceRoot(), resources, depth); 
        command.run(progress);
	}

	/**
	 * Checkin any local changes to given resources
	 * 
	 */
	public void checkin(IResource[] resources, final String comment, boolean keepLocks, final int depth, IProgressMonitor progress) throws TeamException {
		CheckinResourcesCommand command = new CheckinResourcesCommand(getSVNWorkspaceRoot(), resources, depth, comment, keepLocks);
        command.run(progress);
	}
	
	/**
	 * Switch URL for selected resource
	 * 
	 */
	public void switchToUrl(IResource resource, final SVNUrl svnUrl, final SVNRevision svnRevision, IProgressMonitor progress) throws TeamException {
		SwitchToUrlCommand command = new SwitchToUrlCommand(getSVNWorkspaceRoot(), resource, svnUrl, svnRevision);
        command.run(progress);
	}	

    public SVNWorkspaceRoot getSVNWorkspaceRoot() {
        return workspaceRoot;
    }

    public void configureProject() {
        SVNProviderPlugin.broadcastProjectConfigured(getProject());
    }
    /*
     * @see RepositoryProvider#getID()
     */
    public String getID() {
        return SVNProviderPlugin.getTypeId();
    }
    
    /*
     * (non-Javadoc)
     * @see org.eclipse.team.core.RepositoryProvider#getMoveDeleteHook()
     */
	public IMoveDeleteHook getMoveDeleteHook() {
		return new SVNMoveDeleteHook();
	}

    public IFileModificationValidator getFileModificationValidator() {
        return new SVNFileModificationValidator();
    }
	public IResourceVariant getResourceVariant(IResource resource) throws SVNException{
		ISVNLocalResource local = SVNWorkspaceRoot.getSVNResourceFor(resource);
		return local.getLatestRemoteResource();
        
		
	}

	/**
	 * Create the resource variant for the given local resource from the 
	 * given bytes. The bytes are those that were previously returned
	 * from a call to <code>IResourceVariant#asBytes()</code>.  This means it's already been fetched,
	 * so we should be able to create enough nfo about it to rebuild it to a minimally useable form for
	 * synchronization.
	 * @param resource the local resource
	 * @param bytes the bytes that identify a variant of the resource
	 * @return the resouce variant handle recreated from the bytes
	 * @throws TeamException
	 */
	public IResourceVariant getResourceVariant(IResource resource, byte[] bytes) throws TeamException {
		
		//in this case, asBytes() will return the revision string, so we create 
		//the variant resource with this minimal info.
		
		if(bytes==null)return null;
		if(resource.getType()==IResource.FILE){
			return new RemoteFile(resource, bytes);
		}else if(resource.getType()==IResource.FOLDER || resource.getType()==IResource.PROJECT){
			return new RemoteFolder(resource, bytes);
		}else{
			return null;
		}
		
		

		
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.core.RepositoryProvider#getRuleFactory()
	 */
	public IResourceRuleFactory getRuleFactory() {
		return RESOURCE_RULE_FACTORY;
	}

    /* (non-Javadoc)
     * @see org.eclipse.team.core.RepositoryProvider#canHandleLinkedResources()
     */
    public boolean canHandleLinkedResources() {
        return true;
    }
}
