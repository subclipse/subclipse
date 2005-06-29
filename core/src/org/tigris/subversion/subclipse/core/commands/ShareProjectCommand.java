/******************************************************************************
 * This program and the accompanying materials are made available under
 * the terms of the Common Public License v1.0 which accompanies this
 * distribution, and is available at the following URL:
 * http://www.eclipse.org/legal/cpl-v10.html
 * Copyright(c) 2003-2005 by the authors indicated in the @author tags.
 *
 * All Rights are Reserved by the various authors.
 *******************************************************************************/
package org.tigris.subversion.subclipse.core.commands;

import java.net.MalformedURLException;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.team.core.RepositoryProvider;
import org.eclipse.team.core.TeamException;
import org.tigris.subversion.subclipse.core.ISVNRepositoryLocation;
import org.tigris.subversion.subclipse.core.ISVNRunnable;
import org.tigris.subversion.subclipse.core.Policy;
import org.tigris.subversion.subclipse.core.SVNException;
import org.tigris.subversion.subclipse.core.SVNProviderPlugin;
import org.tigris.subversion.subclipse.core.util.Util;
import org.tigris.subversion.svnclientadapter.ISVNClientAdapter;
import org.tigris.subversion.svnclientadapter.SVNClientException;
import org.tigris.subversion.svnclientadapter.SVNRevision;
import org.tigris.subversion.svnclientadapter.SVNUrl;

/**
 * <p>
 * Create a remote directory in the SVN repository and link the project
 * directory to this remote directory.
 * </p>
 * <p> 
 * The contents of the project are not imported.
 * </p>
 * <p> 
 * If location is not in repositories, it is added
 * </p>
 */
public class ShareProjectCommand implements ISVNCommand {
	private ISVNRepositoryLocation location;

	private IProject project;

	private String remoteDirName;

    /**
     * if remoteDirName is null, the name of the project is used    
     */
	public ShareProjectCommand(ISVNRepositoryLocation location,
			IProject project, String remoteDirName) {
		this.location = location;
		this.project = project;

		if (remoteDirName == null) {
			this.remoteDirName = project.getName();
		} else {
			this.remoteDirName = remoteDirName;
		}
	}

    public ShareProjectCommand(ISVNRepositoryLocation location, IProject project) {
    	this(location, project, null);
    }
    
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.tigris.subversion.subclipse.core.commands.ISVNCommand#run(org.eclipse.core.runtime.IProgressMonitor)
	 */
	public void run(IProgressMonitor monitor) throws SVNException {
		// Determine if the repository is known
		boolean alreadyExists = SVNProviderPlugin.getPlugin().getRepositories()
				.isKnownRepository(location.getLocation());

		try {
			// Get the import properties
			String projectName = project.getName();

			final ISVNClientAdapter svnClient = location.getSVNClient();

			// perform the workspace modifications in a runnable
            SVNProviderPlugin.run(new ISVNRunnable() {
    				public void run(IProgressMonitor pm) throws SVNException {
							String message = Policy
									.bind("SVNProvider.initialImport"); //$NON-NLS-1$

							try {
								// create the remote dir
								SVNUrl url = new SVNUrl(Util.appendPath(
										location.getUrl().toString(), remoteDirName));
								svnClient.mkdir(url, message);

								// checkout it so that we have .svn
								svnClient.checkout(url, project.getLocation()
										.toFile(), SVNRevision.HEAD, false);
							} catch (SVNClientException e) {
								throw new SVNException(
										"Error while creating module: "
												+ e.getMessage(), e);
							} catch (MalformedURLException e) {
								throw new SVNException(
										"Error while creating module: "
												+ e.getMessage(), e);
							}

							// SharingWizard.doesSVNDirectoryExist calls
							// getStatus on the folder which populates the
							// status cache
							// Need to clear the cache so we can get the new
							// hasRemote value
							SVNProviderPlugin.getPlugin()
									.getStatusCacheManager().refreshStatus(
											project, IResource.DEPTH_INFINITE);

							try {
								//Register it with Team. 
								RepositoryProvider.map(project, SVNProviderPlugin
										.getTypeId());
							} catch (TeamException e) {
								throw new SVNException("Cannot register project with svn provider",e);
							}
					}
				}, monitor);
		} catch (SVNException e) {
			// The checkout may have triggered password caching
			// Therefore, if this is a newly created location, we want to clear
			// its cache
			if (!alreadyExists)
				SVNProviderPlugin.getPlugin().getRepositories()
						.disposeRepository(location);
			throw e;
		}
		// Add the repository if it didn't exist already
		if (!alreadyExists)
			SVNProviderPlugin.getPlugin().getRepositories()
					.addOrUpdateRepository(location);
	}

}