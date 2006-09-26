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
package org.tigris.subversion.subclipse.ui.repository;


import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.team.core.RepositoryProvider;
import org.eclipse.team.core.TeamException;
import org.tigris.subversion.subclipse.core.ISVNRemoteFolder;
import org.tigris.subversion.subclipse.core.ISVNRemoteResource;
import org.tigris.subversion.subclipse.core.ISVNRepositoryLocation;
import org.tigris.subversion.subclipse.core.SVNException;
import org.tigris.subversion.subclipse.core.SVNProviderPlugin;
import org.tigris.subversion.subclipse.core.SVNTeamProvider;
import org.tigris.subversion.subclipse.core.repo.ISVNListener;
import org.tigris.subversion.subclipse.ui.Policy;
import org.tigris.subversion.subclipse.ui.authentication.KeyFilesManager;
import org.tigris.subversion.subclipse.ui.comments.CommentsManager;
import org.tigris.subversion.subclipse.ui.comments.ReleaseCommentDialog;
import org.tigris.subversion.subclipse.ui.dialogs.AddToVersionControlDialog;

/**
 * This class is responsible for maintaining the UI's list of known repositories
 * 
 * It also provides a number of useful methods for assisting in repository operations.
 */
public class RepositoryManager {
	
	
	List listeners = new ArrayList();

	private CommentsManager commentsManager = new CommentsManager();
	private KeyFilesManager keyFilesManager = new KeyFilesManager();
	
	/**
	 * Answer an array of all known remote roots.
	 */
	public ISVNRepositoryLocation[] getKnownRepositoryLocations(IProgressMonitor monitor) {
		return SVNProviderPlugin.getPlugin().getRepositories().getKnownRepositories(monitor);
	}
	
	/**
	 * A repository root has been added. Notify any listeners.
	 */
	public void rootAdded(ISVNRepositoryLocation root) {
		Iterator it = listeners.iterator();
		while (it.hasNext()) {
			IRepositoryListener listener = (IRepositoryListener)it.next();
			listener.repositoryAdded(root);
		}
	}

    /**
     * A repository root has been modified. Notify any listeners.
     */
    public void rootModified(ISVNRepositoryLocation root) {
        Iterator it = listeners.iterator();
        while (it.hasNext()) {
            IRepositoryListener listener = (IRepositoryListener)it.next();
            listener.repositoryModified(root);
        }
    }    
    
	/**
	 * A repository root has been removed.
	 */
	public void rootRemoved(ISVNRepositoryLocation root) {
        Iterator it = listeners.iterator();
        while (it.hasNext()) {
            IRepositoryListener listener = (IRepositoryListener)it.next();
            listener.repositoryRemoved(root);
        }
    }

    /**
     * A resource has been deleted
     */
    public void resourceDeleted(ISVNRemoteResource resource) {
        Iterator it = listeners.iterator();
        while (it.hasNext()) {
            IRepositoryListener listener = (IRepositoryListener)it.next();
            listener.remoteResourceDeleted(resource);
        }
    }

    /**
     * A resource has been deleted
     */
    public void resourceCreated(ISVNRemoteFolder parent, String resourceName) {
        Iterator it = listeners.iterator();
        while (it.hasNext()) {
            IRepositoryListener listener = (IRepositoryListener)it.next();
            listener.remoteResourceCreated(parent,resourceName);
        }
    }

    /**
     * A resource has been copied
     */
    public void resourceCopied(ISVNRemoteResource source, ISVNRemoteFolder destination) {
        Iterator it = listeners.iterator();
        while (it.hasNext()) {
            IRepositoryListener listener = (IRepositoryListener)it.next();
            listener.remoteResourceCopied(source, destination);
        }
    }

    /**
     * A resource has been moved 
     */
    public void resourceMoved(ISVNRemoteResource resource, ISVNRemoteFolder destinationFolder, String destinationResourceName) {
        Iterator it = listeners.iterator();
        while (it.hasNext()) {
            IRepositoryListener listener = (IRepositoryListener)it.next();
            listener.remoteResourceMoved(resource, destinationFolder,destinationResourceName);
        }
    }


    /**
     * called when plugin is started
     */	
	public void startup() {
        commentsManager.loadCommentHistory();
        commentsManager.loadCommentTemplates();
        keyFilesManager.loadKeyFileHistory();
		
        // we listen to changes to repository so that we can advise concerned views
        SVNProviderPlugin.getPlugin().getRepositoryResourcesManager().addRepositoryListener(new ISVNListener() {
			public void repositoryAdded(ISVNRepositoryLocation root) {
				rootAdded(root);
			}
			public void repositoryRemoved(ISVNRepositoryLocation root) {
				rootRemoved(root);
			}
            public void remoteResourceDeleted(ISVNRemoteResource resource) {
                resourceDeleted(resource);
            }
            public void remoteResourceCreated(ISVNRemoteFolder parent, String resourceName) {
                resourceCreated(parent, resourceName);
            }
            public void remoteResourceCopied(ISVNRemoteResource source, ISVNRemoteFolder destination) {
                resourceCopied(source, destination);
            }
            public void remoteResourceMoved(ISVNRemoteResource resource, ISVNRemoteFolder destinationFolder, String destinationResourceName) {
                resourceMoved(resource, destinationFolder, destinationResourceName);
            }
			public void repositoryModified(ISVNRepositoryLocation root) {
				rootModified(root);
			}
                       
		});
	}
	
    /**
     * Called when plugin is stopped 
     */
	public void shutdown() throws TeamException {
        commentsManager.saveCommentHistory();
        commentsManager.saveCommentTemplates();
        keyFilesManager.saveKeyFilesHistory();
	}


	public void addRepositoryListener(IRepositoryListener listener) {
		listeners.add(listener);
	}
	
	public void removeRepositoryListener(IRepositoryListener listener) {
		listeners.remove(listener);
	}
	
	/**
	 * Add the given resources to their associated providers.
	 * This schedules the resources for addition; they still need to be committed.
	 */
	public void add(IResource[] resources, IProgressMonitor monitor) throws TeamException {
		
		if (monitor == null) {
			monitor = new NullProgressMonitor();
		}
		Map table = getProviderMapping(resources);
		
		// some of the resources are not associated with shared projects
		if (table.get(null) != null) {
			throw new SVNException(Policy.bind("RepositoryManager.addErrorNotAssociated"));  //$NON-NLS-1$
		}
		
		// iterate through the svn providers
		Set keySet = table.keySet();
		monitor.beginTask("", keySet.size() * 1000); //$NON-NLS-1$
		monitor.setTaskName(Policy.bind("RepositoryManager.adding")); //$NON-NLS-1$
		Iterator iterator = keySet.iterator();
		while (iterator.hasNext()) {
			IProgressMonitor subMonitor = new SubProgressMonitor(monitor, 1000);
			SVNTeamProvider provider = (SVNTeamProvider)iterator.next();
			List list = (List)table.get(provider);
			IResource[] providerResources = (IResource[])list.toArray(new IResource[list.size()]);
			provider.add(providerResources, IResource.DEPTH_ZERO, subMonitor);
		}		
	}
	
//	/**
//	 * Delete the given resources from their associated providers.
//	 * This schedules the resources for deletion; they still need to be committed.
//	 */
//	public void delete(IResource[] resources, IProgressMonitor monitor) throws TeamException {
//		Map table = getProviderMapping(resources);
//		Set keySet = table.keySet();
//		monitor.beginTask("", keySet.size() * 1000); //$NON-NLS-1$
//		monitor.setTaskName(Policy.bind("RepositoryManager.deleting")); //$NON-NLS-1$
//		Iterator iterator = keySet.iterator();
//		while (iterator.hasNext()) {
//			IProgressMonitor subMonitor = new SubProgressMonitor(monitor, 1000);
//			SVNTeamProvider provider = (SVNTeamProvider)iterator.next();
//			provider.setComment(getCurrentComment());
//			List list = (List)table.get(provider);
//			IResource[] providerResources = (IResource[])list.toArray(new IResource[list.size()]);
//			provider.delete(providerResources, subMonitor);
//		}		
//	}
//	
//	public void update(IResource[] resources, Command.LocalOption[] options, boolean createBackups, IProgressMonitor monitor) throws TeamException {
//		Map table = getProviderMapping(resources);
//		Set keySet = table.keySet();
//		monitor.beginTask("", keySet.size() * 1000); //$NON-NLS-1$
//		monitor.setTaskName(Policy.bind("RepositoryManager.updating")); //$NON-NLS-1$
//		Iterator iterator = keySet.iterator();
//		while (iterator.hasNext()) {
//			IProgressMonitor subMonitor = new SubProgressMonitor(monitor, 1000);
//			SVNTeamProvider provider = (SVNTeamProvider)iterator.next();
//			List list = (List)table.get(provider);
//			IResource[] providerResources = (IResource[])list.toArray(new IResource[list.size()]);
//			provider.update(providerResources, options, null, createBackups, subMonitor);
//		}		
//	}
//	
//	/**
//	 * Mark the files as merged.
//	 */
//	public void merged(IRemoteSyncElement[] elements) throws TeamException {
//		Map table = getProviderMapping(elements);
//		Set keySet = table.keySet();
//		Iterator iterator = keySet.iterator();
//		while (iterator.hasNext()) {
//			SVNTeamProvider provider = (SVNTeamProvider)iterator.next();
//			provider.setComment(getCurrentComment());
//			List list = (List)table.get(provider);
//			IRemoteSyncElement[] providerElements = (IRemoteSyncElement[])list.toArray(new IRemoteSyncElement[list.size()]);
//			provider.merged(providerElements);
//		}		
//	}

    public CommentsManager getCommentsManager() {
        return commentsManager;
    }
    public KeyFilesManager getKeyFilesManager() {
    	return keyFilesManager;
    }

    /**
     * Return the entered comment or null if canceled.
     */
    public String promptForComment(final Shell shell, IResource[] resourcesToCommit) {
        final int[] result = new int[1];
        final ReleaseCommentDialog dialog = new ReleaseCommentDialog(shell, resourcesToCommit); 
        shell.getDisplay().syncExec(new Runnable() {
            public void run() {
                result[0] = dialog.open();
                if (result[0] != Window.OK) return;
            }
        });
        if (result[0] != Window.OK) return null;
        return dialog.getComment();
    }	

	/**
	 * Prompt to add all or some of the provided resources to version control.
	 * The value null is returned if the dialog is cancelled.
	 * 
	 * @param shell
	 * @param unadded
	 * @return IResource[]
	 */
	public IResource[] promptForResourcesToBeAdded(Shell shell, IResource[] unadded) {
		if (unadded == null) return new IResource[0];
		if (unadded.length == 0) return unadded;
		final IResource[][] result = new IResource[1][0];
		result[0] = null;
		final AddToVersionControlDialog dialog = new AddToVersionControlDialog(shell, unadded);
		shell.getDisplay().syncExec(new Runnable() {
			public void run() {
				int code = dialog.open();
				if (code == IDialogConstants.YES_ID) {
					result[0] = dialog.getResourcesToAdd();
				} else if(code == IDialogConstants.NO_ID) {
					// allow the commit to continue.
					result[0] = new IResource[0];
				}
			}
		});
		return result[0];
	}
	/**
	 * Commit the given resources to their associated providers.
	 * 
	 * @param resources  the resources to commit
	 * @param monitor  the progress monitor
	 */
	public void commit(IResource[] resources, String comment, boolean keepLocks, IProgressMonitor monitor) throws TeamException {
		if (monitor == null) {
			monitor = new NullProgressMonitor();
		}
		Map table = getProviderMapping(resources);
		Set keySet = table.keySet();
		monitor.beginTask("", keySet.size() * 1000); //$NON-NLS-1$
		monitor.setTaskName(Policy.bind("RepositoryManager.committing")); //$NON-NLS-1$
		Iterator iterator = keySet.iterator();
		while (iterator.hasNext()) {
			IProgressMonitor subMonitor = new SubProgressMonitor(monitor, 1000);
			SVNTeamProvider provider = (SVNTeamProvider)iterator.next();
			List list = (List)table.get(provider);
			IResource[] providerResources = (IResource[])list.toArray(new IResource[list.size()]);
			provider.checkin(providerResources, comment, keepLocks, IResource.DEPTH_INFINITE, subMonitor);
		}
	}
	
	/**
	 * Helper method. Return a Map mapping provider to a list of resources
	 * shared with that provider.
	 * If a resource is not associated with a provider, the key is null
	 */
	private Map getProviderMapping(IResource[] resources) {
		Map result = new HashMap();
		for (int i = 0; i < resources.length; i++) {
			RepositoryProvider provider = RepositoryProvider.getProvider(resources[i].getProject(), SVNProviderPlugin.getTypeId());
			List list = (List)result.get(provider);
			if (list == null) {
				list = new ArrayList();
				result.put(provider, list);
			}
			list.add(resources[i]);
		}
		return result;
	}

 
	/**
	 * Run the given runnable
	 */
	public void run(IRunnableWithProgress runnable, IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
		runnable.run(monitor);
	}
	
}
