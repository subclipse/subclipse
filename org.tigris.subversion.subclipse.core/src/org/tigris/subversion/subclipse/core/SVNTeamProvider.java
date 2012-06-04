/*******************************************************************************
 * Copyright (c) 2003, 2006 Subclipse project and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Subclipse project committers - initial API and implementation
 ******************************************************************************/
package org.tigris.subversion.subclipse.core;
 
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFileModificationValidator;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectNature;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceRuleFactory;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.resources.team.IMoveDeleteHook;
import org.eclipse.core.resources.team.ResourceRuleFactory;
import org.eclipse.core.runtime.CoreException;
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
		try {
			// when a nature is removed from the project, notify the synchronizer that
			// we no longer need the sync info cached. This does not affect the actual SVN
			// meta directories on disk, and will remain unless a client calls unmanage().		
			SVNProviderPlugin.getPlugin().getStatusCacheManager().purgeCache(getProject(), true);
		} catch (SVNException e)
		{
			SVNProviderPlugin.log(e);
		}

		SVNProviderPlugin.broadcastProjectDeconfigured(getProject());
	}

	private void configureTeamPrivateResource(IProject project)
	{
		try {
			project.accept(
					new IResourceVisitor() {
						public boolean visit(IResource resource) throws CoreException {
							if ((resource.getType() == IResource.FOLDER)
									&& (resource.getName().equals(SVNProviderPlugin.getPlugin().getAdminDirectoryName()))
									&& (!resource.isTeamPrivateMember()))
							{
								resource.setTeamPrivateMember(true);
								return false;
							}
							else
							{
								return true;
							}
						}
					}, IResource.DEPTH_INFINITE, IContainer.INCLUDE_PHANTOMS | IContainer.INCLUDE_TEAM_PRIVATE_MEMBERS);
		} catch (CoreException e) {
			SVNProviderPlugin.log(SVNException.wrapException(e));
		}
	}


	/**
	 * @see IProjectNature#setProject(IProject)
	 */
	public void setProject(IProject project) {
		super.setProject(project);
		this.workspaceRoot = new SVNWorkspaceRoot(project);
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
     * @param resources	resources to commit.
     * @param comment	log message.
     * @param keepLocks	whether to keep to locks
     * @param depth		IResource.DEPTH_INFINITE if the commit should be recursive, anything else if otherwise
     * @param progress	progressMonitor or null
     * @exception TeamException
     */
	public String checkin(IResource[] resources, final String comment, boolean keepLocks, final int depth, IProgressMonitor progress) throws TeamException {
		CheckinResourcesCommand command = new CheckinResourcesCommand(getSVNWorkspaceRoot(), resources, depth, comment, keepLocks);
        command.run(progress);
        return command.getPostCommitError();
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
    	configureTeamPrivateResource(getProject()); 
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
	 */
	public IResourceVariant getResourceVariant(IResource resource, byte[] bytes) {
		
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

	public boolean canHandleLinkedResourceURI() {
		return true;
	}
}
