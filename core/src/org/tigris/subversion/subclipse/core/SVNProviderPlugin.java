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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.ISafeRunnable;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Plugin;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.team.core.TeamException;
import org.osgi.framework.BundleContext;
import org.tigris.subversion.subclipse.core.client.IConsoleListener;
import org.tigris.subversion.subclipse.core.repo.SVNRepositories;
import org.tigris.subversion.subclipse.core.resources.ISVNFileModificationValidatorPrompt;
import org.tigris.subversion.subclipse.core.resources.RepositoryResourcesManager;
import org.tigris.subversion.subclipse.core.resourcesListeners.FileModificationManager;
import org.tigris.subversion.subclipse.core.resourcesListeners.SyncFileChangeListener;
import org.tigris.subversion.subclipse.core.status.StatusCacheManager;
import org.tigris.subversion.subclipse.core.util.ISimpleDialogsHelper;
import org.tigris.subversion.svnclientadapter.ISVNClientAdapter;
import org.tigris.subversion.svnclientadapter.ISVNPromptUserPassword;

/**
 * The plugin itself
 */
public class SVNProviderPlugin extends Plugin {

    // svn plugin id
	public static final String ID = "org.tigris.subversion.subclipse.core"; //$NON-NLS-1$

	public static final String PROVIDER_ID="org.tigris.subversion.subclipse.core.svnnature"; //$NON-NLS-1$
    
    public static final String SVN_PROPERTY_TYPES_EXTENSION = "svnPropertyTypes";
    public static final String SVN_PROPERTY_GROUPS_EXTENSION = "svnPropertyGroups";
    
	// all projects shared with subversion will have this nature
	private static final String NATURE_ID = ID + ".svnnature"; //$NON-NLS-1$

	// the plugin instance. @see getPlugin()
	private static volatile SVNProviderPlugin instance;

	// the console listener
	private IConsoleListener consoleListener;

	// SVN specific resource delta listeners
	private FileModificationManager fileModificationManager;
	private SyncFileChangeListener metaFileSyncListener;

	// the list of all repositories currently handled by this provider
	private SVNRepositories repositories;

    private StatusCacheManager statusCacheManager;
    
	private RepositoryResourcesManager repositoryResourcesManager = new RepositoryResourcesManager();

    private SVNClientManager svnClientManager; 
    
    private SVNAdapterFactories adapterFactories;
    
    private ISVNPromptUserPassword svnPromptUserPassword;
	
	private ISimpleDialogsHelper simpleDialogsHelper;
	
	private ISVNFileModificationValidatorPrompt svnFileModificationValidatorPrompt;
	
	private String dirname;
    
	/**
	 * This constructor required by the bundle loader (calls newInstance())
	 *  
	 */
	public SVNProviderPlugin() {
		super();
		instance = this;
	}
	

    /**
     * Log the given exception along with the provided message and severity indicator
     */
    public static void log(int severity, String message, Throwable e) {
        log(new Status(severity, ID, 0, message, e));
    }
	/**
	 * Convenience method for logging SVNExceptions to the plugin log
	 */
	public static void log(TeamException e) {
		// For now, we'll log the status. However we should do more
		log(e.getStatus());
	}
	public static void log(IStatus status) {
		// For now, we'll log the status. However we should do more
		getPlugin().getLog().log(status);
	}

	/**
	 * Returns the singleton plug-in instance.
	 * 
	 * @return the plugin instance
	 */
	public static SVNProviderPlugin getPlugin() {

		return instance;
	}

	public void start(BundleContext ctxt) throws Exception {
		super.start(ctxt);
		
		// register all the adapter factories
		adapterFactories = new SVNAdapterFactories();
		adapterFactories.startup(null);
	
        statusCacheManager = new StatusCacheManager();
        getPluginPreferences().addPropertyChangeListener(statusCacheManager);
        
		// Initialize SVN change listeners. Note tha the report type is important.
		IWorkspace workspace = ResourcesPlugin.getWorkspace();

		// this listener will listen to modifications to files
		fileModificationManager = new FileModificationManager();

		// this listener will listen to modification to metafiles (files in .svn
		// subdir)
		metaFileSyncListener = new SyncFileChangeListener();

		workspace.addResourceChangeListener(statusCacheManager,
				IResourceChangeEvent.PRE_BUILD);
		workspace.addResourceChangeListener(metaFileSyncListener,
				IResourceChangeEvent.PRE_BUILD);
		workspace.addResourceChangeListener(fileModificationManager,
				IResourceChangeEvent.POST_CHANGE);

		fileModificationManager.registerSaveParticipant();

	}

	/**
	 * @see Plugin#stop(BundleContext ctxt)
	 */
	public void stop(BundleContext ctxt) throws Exception {
		super.stop(ctxt);

		// remove listeners
		IWorkspace workspace = ResourcesPlugin.getWorkspace();		
		workspace.removeResourceChangeListener(statusCacheManager);
		workspace.removeResourceChangeListener(metaFileSyncListener);
		workspace.removeResourceChangeListener(fileModificationManager);

		// save the state which includes the known repositories
		if (repositories != null) {
			repositories.shutdown();
		}
		
		adapterFactories.shutdown(null);
        getPluginPreferences().removePropertyChangeListener(statusCacheManager);
		
        // save the plugin preferences
        savePluginPreferences();
		
		// remove all of this plugin's save participants. This is easier than
		// having
		// each class that added itself as a participant to have to listen to
		// shutdown.
		workspace.removeSaveParticipant(this);
        
        svnClientManager.shutdown(null);
	}

	private static List listeners = new ArrayList();

	/*
	 * @see ITeamManager#addResourceStateChangeListener(IResourceStateChangeListener)
	 */
	public static void addResourceStateChangeListener(
			IResourceStateChangeListener listener) {
		listeners.add(listener);
	}

	/*
	 * @see ITeamManager#removeResourceStateChangeListener(IResourceStateChangeListener)
	 */
	public static void removeResourceStateChangeListener(
			IResourceStateChangeListener listener) {
		listeners.remove(listener);
	}

	/**
	 * This method is called by SyncFileChangeListener when metafiles have
	 * changed
	 */
	public static void broadcastSyncInfoChanges(final IResource[] resources) {
		for (Iterator it = listeners.iterator(); it.hasNext();) {
			final IResourceStateChangeListener listener = (IResourceStateChangeListener) it
					.next();
			ISafeRunnable code = new ISafeRunnable() {
				public void run() throws Exception {
					listener.resourceSyncInfoChanged(resources);
				}
				public void handleException(Throwable e) {
					// don't log the exception....it is already being logged in
					// Platform#run
				}
			};
			Platform.run(code);
		}
	}

	//	public static void broadcastDecoratorEnablementChanged(final boolean
	// enabled) {
	//		for(Iterator it=decoratorEnablementListeners.iterator(); it.hasNext();) {
	//			final ICVSDecoratorEnablementListener listener =
	// (ICVSDecoratorEnablementListener)it.next();
	//			ISafeRunnable code = new ISafeRunnable() {
	//				public void run() throws Exception {
	//					listener.decoratorEnablementChanged(enabled);
	//				}
	//				public void handleException(Throwable e) {
	//					// don't log the exception....it is already being logged in Platform#run
	//				}
	//			};
	//			Platform.run(code);
	//		}
	//	}
	//	
	/**
	 * This method is called by FileModificationManager when some resources have
	 * changed
	 */
	public static void broadcastModificationStateChanges(
			final IResource[] resources) {
		for (Iterator it = listeners.iterator(); it.hasNext();) {
			final IResourceStateChangeListener listener = (IResourceStateChangeListener) it
					.next();
			ISafeRunnable code = new ISafeRunnable() {
				public void run() throws Exception {
					listener.resourceModified(resources);
				}
				public void handleException(Throwable e) {
					// don't log the exception....it is already being logged in
					// Platform#run
				}
			};
			Platform.run(code);
		}
	}

	/**
	 * This method is called by SVNTeamProvider.configureProject which is
	 * invoked when a project is mapped
	 */
	protected static void broadcastProjectConfigured(final IProject project) {
		for (Iterator it = listeners.iterator(); it.hasNext();) {
			final IResourceStateChangeListener listener = (IResourceStateChangeListener) it
					.next();
			ISafeRunnable code = new ISafeRunnable() {
				public void run() throws Exception {
					listener.projectConfigured(project);
				}
				public void handleException(Throwable e) {
					// don't log the exception....it is already being logged in
					// Platform#run
				}
			};
			Platform.run(code);
		}
	}

	/**
	 * This method is called by SVNTeamProvider.deconfigured which is invoked
	 * after a provider has been unmaped
	 */
	protected static void broadcastProjectDeconfigured(final IProject project) {
		for (Iterator it = listeners.iterator(); it.hasNext();) {
			final IResourceStateChangeListener listener = (IResourceStateChangeListener) it
					.next();
			ISafeRunnable code = new ISafeRunnable() {
				public void run() throws Exception {
					listener.projectDeconfigured(project);
				}
				public void handleException(Throwable e) {
					// don't log the exception....it is already being logged in
					// Platform#run
				}
			};
			Platform.run(code);
		}
	}

	/**
	 * Register to receive notification of enablement of sync info decoration
	 * requirements. This can be useful for providing lazy initialization of
	 * caches that are only required for decorating resource with CVS
	 * information.
	 */
	/*
	 * public void
	 * addDecoratorEnablementListener(ISVNDecoratorEnablementListener listener) {
	 * decoratorEnablementListeners.add(listener); }
	 */

	/**
	 * De-register the decorator enablement listener.
	 */
	/*
	 * public void
	 * removeDecoratorEnablementListener(ICVSDecoratorEnablementListener
	 * listener) { decoratorEnablementListeners.remove(listener); }
	 */

	/**
	 * get the repository corresponding to the location location is an url
	 */
	public ISVNRepositoryLocation getRepository(String location)
			throws SVNException {
		return getRepositories().getRepository(location);
	}

	/**
	 * get all the known repositories
	 */
	public SVNRepositories getRepositories() {
	    if (repositories == null) {
	        // load the state which includes the known repositories
	        repositories = new SVNRepositories();
	        repositories.startup();
	    }
		return repositories;
	}
    
    /**
     * get the resource status cache
	 */
    public StatusCacheManager getStatusCacheManager() {
    	return statusCacheManager;
    }

    public SVNClientManager getSVNClientManager() {
        if (svnClientManager == null) {
            svnClientManager = new SVNClientManager();
            try {
                svnClientManager.startup(null);
            } catch (CoreException e) {
            }
        }
    	return svnClientManager;
    }
    
    public ISVNClientAdapter createSVNClient() throws SVNException {
    	return getSVNClientManager().createSVNClient();
    }

	/**
	 * Set the console listener for commands.
	 * 
	 * @param consoleListener
	 *            the listener
	 */
	public void setConsoleListener(IConsoleListener consoleListener) {
		this.consoleListener = consoleListener;
	}

	/**
	 * Get the console listener for commands.
	 * 
	 * @return the consoleListener, or null
	 */
	public IConsoleListener getConsoleListener() {
		return consoleListener;
	}

	/**
	 * Answers the repository provider type id for the svn plugin
	 */
	public static String getTypeId() {
		return NATURE_ID;
	}

	/**
	 * Same as IWorkspace.run but uses a ISVNRunnable
	 */
	public static void run(final ISVNRunnable job, IProgressMonitor monitor)
			throws SVNException {
		final SVNException[] error = new SVNException[1];
		try {
			ResourcesPlugin.getWorkspace().run(new IWorkspaceRunnable() {
				public void run(IProgressMonitor monitor) {
					try {
						monitor = Policy.monitorFor(monitor);
						try {
							job.run(monitor);
						} finally {
							monitor.done();
						}
					} catch (SVNException e) {
						error[0] = e;
					}
				}
			}, monitor);
		} catch (CoreException e) {
			throw SVNException.wrapException(e);
		}
		if (error[0] != null) {
			throw error[0];
		}
	}

	/**
	 * Same as IWorkspace.run but uses a ISVNRunnable
	 */
	public static void run(final ISVNRunnable job, ISchedulingRule rule, IProgressMonitor monitor)
			throws SVNException {
		final SVNException[] error = new SVNException[1];
		try {
			ResourcesPlugin.getWorkspace().run(new IWorkspaceRunnable() {
				public void run(IProgressMonitor monitor) {
					try {
						monitor = Policy.monitorFor(monitor);
						try {
							job.run(monitor);
						} finally {
							monitor.done();
						}
					} catch (SVNException e) {
						error[0] = e;
					}
				}
			}, rule, IWorkspace.AVOID_UPDATE, monitor);
		} catch (CoreException e) {
			throw SVNException.wrapException(e);
		}
		if (error[0] != null) {
			throw error[0];
		}
	}

	/**
	 * @return the repository resources Manager
	 */
	public RepositoryResourcesManager getRepositoryResourcesManager() {
		return repositoryResourcesManager;
	}

    public ISVNPromptUserPassword getSvnPromptUserPassword() {
        return svnPromptUserPassword;
    }
    public void setSvnPromptUserPassword(
            ISVNPromptUserPassword svnPromptUserPassword) {
        this.svnPromptUserPassword = svnPromptUserPassword;
    }
	
	public ISimpleDialogsHelper getSimpleDialogsHelper() {
		return simpleDialogsHelper;
	}
	
	public void setSimpleDialogsHelper(ISimpleDialogsHelper simpleDialogsHelper) {
		this.simpleDialogsHelper = simpleDialogsHelper;
	}
	
    public ISVNFileModificationValidatorPrompt getSvnFileModificationValidatorPrompt() {
        return svnFileModificationValidatorPrompt;
    }
    public void setSvnFileModificationValidatorPrompt(
            ISVNFileModificationValidatorPrompt svnFileModificationValidatorPrompt) {
        this.svnFileModificationValidatorPrompt = svnFileModificationValidatorPrompt;
    }
    
    public String getAdminDirectoryName() {
    	if (dirname == null) {
    		try {
				dirname = createSVNClient().getAdminDirectoryName();
			} catch (SVNException e) {
				dirname = ".svn";
			}
    	}
    	return dirname;
    }
    
    public boolean isAdminDirectory(String name) {
    	if (".svn".equals(name) || getAdminDirectoryName().equals(name))
    		return true;
    	else
    		return false;
//  Calling the adapter method here potentially lead to a thread problem
//  that would make native JavaHL crash.  So I am recreating the logic
//  internally.  This method is likely to be a lot faster so it is worth it.    	
//    	try {
//			return createSVNClient().isAdminDirectory(name);
//		} catch (SVNException e) {
//			return getAdminDirectoryName().equals(name);
//		}
    }
}
