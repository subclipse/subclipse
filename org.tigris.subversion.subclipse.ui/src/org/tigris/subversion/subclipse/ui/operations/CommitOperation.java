/*******************************************************************************
 * Copyright (c) 2004, 2006 Subclipse project and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Subclipse project committers - initial API and implementation
 ******************************************************************************/
package org.tigris.subversion.subclipse.ui.operations;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.team.core.RepositoryProvider;
import org.eclipse.team.core.TeamException;
import org.eclipse.ui.IWorkbenchPart;
import org.tigris.subversion.subclipse.core.ISVNLocalResource;
import org.tigris.subversion.subclipse.core.SVNException;
import org.tigris.subversion.subclipse.core.SVNProviderPlugin;
import org.tigris.subversion.subclipse.core.SVNTeamProvider;
import org.tigris.subversion.subclipse.core.resources.SVNWorkspaceRoot;
import org.tigris.subversion.subclipse.ui.ISVNUIConstants;
import org.tigris.subversion.subclipse.ui.Policy;
import org.tigris.subversion.subclipse.ui.SVNUIPlugin;
import org.tigris.subversion.svnclientadapter.ISVNClientAdapter;
import org.tigris.subversion.svnclientadapter.ISVNInfo;
import org.tigris.subversion.svnclientadapter.SVNClientException;
import org.tigris.subversion.svnclientadapter.SVNUrl;

public class CommitOperation extends SVNOperation {
    private IResource[] resourcesToAdd;
    private IResource[] resourcesToDelete;
    private IResource[] resourcesToCommit;
    private String commitComment;
    private boolean keepLocks;
    private ISVNClientAdapter svnClient;
    private boolean atomicCommit = true;
    private boolean canRunAsJob = true;
    private String postCommitError;

    public CommitOperation(IWorkbenchPart part, IResource[] selectedResources, IResource[] resourcesToAdd, IResource[] resourcesToDelete, IResource[] resourcesToCommit, String commitComment, boolean keepLocks) {
        super(part);
        this.resourcesToAdd = resourcesToAdd;
        this.resourcesToDelete = resourcesToDelete;
        this.resourcesToCommit = resourcesToCommit;
        this.commitComment = commitComment;
        this.keepLocks = keepLocks;
    }

    protected void execute(IProgressMonitor monitor) throws SVNException, InterruptedException {
    	postCommitError = null;
    	monitor.beginTask(null, resourcesToAdd.length + resourcesToDelete.length + resourcesToCommit.length);
        try {
        	svnClient = SVNProviderPlugin.getPlugin().getSVNClientManager().getSVNClient();
        	if (resourcesToAdd.length > 0) {
			    Map<SVNTeamProvider, List<IResource>> table = getProviderMapping(resourcesToAdd);
				if (table.get(null) != null) {
					throw new SVNException(Policy.bind("RepositoryManager.addErrorNotAssociated"));  //$NON-NLS-1$
				}
				Set<SVNTeamProvider> keySet = table.keySet();
				for (SVNTeamProvider provider : keySet) {
					List<IResource> list = table.get(provider);
					IResource[] providerResources = list.toArray(new IResource[list.size()]);
					provider.add(providerResources, IResource.DEPTH_ZERO, Policy.subMonitorFor(monitor, resourcesToAdd.length));
				}						
			}
        	if (resourcesToDelete.length > 0) {
				ISVNClientAdapter svnDeleteClient = null; // use an adapter that will log to console
				Map<SVNTeamProvider, List<IResource>> table = getProviderMapping(resourcesToDelete);
				if (table.get(null) != null) {
					throw new SVNException(Policy.bind("RepositoryManager.addErrorNotAssociated"));  //$NON-NLS-1$
				}
				Set<SVNTeamProvider> keySet = table.keySet();
				for (SVNTeamProvider provider : keySet) {
					List<IResource> list = table.get(provider);
					File[] files = new File[list.size()];
					int i=0;
					for (IResource resource : list) {
						ISVNLocalResource svnResource = SVNWorkspaceRoot.getSVNResourceFor(resource);
						if (svnDeleteClient == null)
						    svnDeleteClient = svnResource.getRepository().getSVNClient();
						files[i] = svnResource.getFile();
						i++;
					}
					try {
						svnDeleteClient.remove(files, true);
					} catch (SVNClientException e) {
						throw new TeamException(e.getMessage());
					} finally {
						SVNProviderPlugin.getPlugin().getSVNClientManager().returnSVNClient(svnDeleteClient);
					}
				}						
			}
        	setAtomicCommitMode();
        	Map<ProjectAndRepository, List<IResource>> table = getCommitProviderMapping(resourcesToCommit);
			Set<ProjectAndRepository> keySet = table.keySet();
			for (ProjectAndRepository mapKey : keySet) {
				SVNTeamProvider provider = mapKey.getTeamProvider();
				List<IResource> list = table.get(mapKey);
				IResource[] providerResources = new IResource[list.size()];
				list.toArray(providerResources);
				postCommitError = provider.checkin(providerResources, commitComment, keepLocks, getDepth(providerResources), Policy.subMonitorFor(monitor, providerResources.length));
				for (IResource providerResource : providerResources) {
					if (!providerResource.exists()) {
						SVNProviderPlugin.getPlugin().getStatusCacheManager().removeStatus(providerResource);
					}
				}
				
				if (postCommitError != null) {
					Display.getDefault().syncExec(new Runnable() {						
						public void run() {
							MessageDialog.openError(Display.getDefault().getActiveShell(), Policy.bind("CommitDialog.title"), postCommitError);
						}
					});
				}
			}			
        } catch (TeamException e) {
			throw SVNException.wrapException(e);
		} finally {
			monitor.done();
			SVNProviderPlugin.getPlugin().getSVNClientManager().returnSVNClient(svnClient);
			// refresh the Synch view
			SVNProviderPlugin.broadcastModificationStateChanges(resourcesToCommit);
		}
    }
   

	/**
	 * This method figures out of if we should commit with DEPTH_ZERO or DEPTH_INFINITE
	 * If there are any modified folders (which could only be a prop change) in the list of committed items,
	 * then it should return DEPTH_ZERO, otherwise it should return DEPTH_INFINITE.
	 * @param resources an array of resources to check
	 * @return IResource.DEPTH_ZERO or IResource.DEPTH_INFINITE  
	 */
	private int getDepth(IResource[] resources) {
	    int depth = IResource.DEPTH_INFINITE;
		for (int i = 0; i < resources.length; i++) {
			if (resources[i].getType() != IResource.FILE) {
				ISVNLocalResource svnResource = SVNWorkspaceRoot.getSVNResourceFor(resources[i]);
				try {
				    // If there is a folder delete, then we cannot do a
				    // non-recursive commit
					if (svnResource.getStatus().isDeleted())
						return IResource.DEPTH_INFINITE;
					if (svnResource.getStatus().isPropModified())
						depth = IResource.DEPTH_ZERO;
				} catch (SVNException e) {
				}
			}
		}
		return depth;
	}

	protected String getTaskName() {
        return Policy.bind("CommitOperation.taskName"); //$NON-NLS-1$;
    }
    
	private Map<ProjectAndRepository, List<IResource>> getCommitProviderMapping(IResource[] resources) {
		ProjectAndRepository mapKey = null;
		Map<ProjectAndRepository, List<IResource>> result = new HashMap<ProjectAndRepository, List<IResource>>();
		for (IResource resource : resources) {
			if (mapKey == null || !svnClient.canCommitAcrossWC()) {
				SVNTeamProvider provider = (SVNTeamProvider) RepositoryProvider.getProvider(resource.getProject(), SVNProviderPlugin.getTypeId());
				mapKey = new ProjectAndRepository(provider, getRootURL(SVNWorkspaceRoot.getSVNResourceFor(resource)));
			}
			List<IResource> list = result.get(mapKey);
			if (list == null) {
				list = new ArrayList<IResource>();
				result.put(mapKey, list);
			}
			list.add(resource);
		}
		return result;
	}
    
	private Map<SVNTeamProvider, List<IResource>> getProviderMapping(IResource[] resources) {
		SVNTeamProvider provider = null;
		Map<SVNTeamProvider, List<IResource>> result = new HashMap<SVNTeamProvider, List<IResource>>();
		for (int i = 0; i < resources.length; i++) {
			if (provider == null || !svnClient.canCommitAcrossWC())
				provider = (SVNTeamProvider) RepositoryProvider.getProvider(resources[i].getProject(), SVNProviderPlugin.getTypeId());
			List<IResource> list = result.get(provider);
			if (list == null) {
				list = new ArrayList<IResource>();
				result.put(provider, list);
			}
			list.add(resources[i]);
		}
		return result;
	}

	private String getRootURL(ISVNLocalResource localResource) {
		if (!atomicCommit)
			return null;
		ISVNInfo info = getSVNInfo(localResource);
		if (info == null)
			return null;
		SVNUrl repos = info.getRepository();
		if (repos == null)
			return null;
    	return repos.toString();
	}

	private ISVNInfo getSVNInfo(ISVNLocalResource localResource) {
		if (!atomicCommit)
			return null;
		if (localResource == null)
			return null;
		File file = localResource.getFile();
		if (file == null)
			return null;
		boolean returnSVNClient = svnClient == null;
		if (svnClient == null) {
	    	try {
				svnClient = SVNProviderPlugin.getPlugin().getSVNClientManager().getSVNClient();
			} catch (SVNException e) {
				return null;
			}
		}
    	ISVNInfo info;
		try {
	        SVNProviderPlugin.disableConsoleLogging(); 
			info = svnClient.getInfoFromWorkingCopy(file);
	        SVNProviderPlugin.enableConsoleLogging(); 
		} catch (SVNClientException e) {
	        SVNProviderPlugin.enableConsoleLogging(); 
			return null;
		}
		finally {
			if (returnSVNClient) {
				SVNProviderPlugin.getPlugin().getSVNClientManager().returnSVNClient(svnClient);
			}
		}
    	return info;
	}

	protected boolean canRunAsJob() {
		return canRunAsJob;
	}
	
	/**
	 * This method sets the atomicCommit mode based on the user preference
	 * and capabilities of the client adapter
	 */
	private void setAtomicCommitMode() {
		if (!SVNUIPlugin.getPlugin().getPreferenceStore().getBoolean(ISVNUIConstants.PREF_USE_JAVAHL_COMMIT_HACK)) {
			atomicCommit = false;
			return;
		}
		
		if (svnClient.canCommitAcrossWC()) {
			atomicCommit = false;
			return;
	    }
	}
	
	protected class ProjectAndRepository {
		
		private SVNTeamProvider provider;
		private String rootURL;

		public ProjectAndRepository(SVNTeamProvider provider, String rootURL) {
			super();
			this.provider = provider;
			if (rootURL == null)
				this.rootURL = "";
			else
				this.rootURL = rootURL;
		}

		public SVNTeamProvider getTeamProvider() {
			return provider;
		}

		public String getRootURL() {
			return rootURL;
		}
		

		private String getKey() {
			return rootURL;
		}

		public String toString() {
			return getKey();
		}

		public int hashCode() {
			return getKey().hashCode();
		}

		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			final ProjectAndRepository other = (ProjectAndRepository) obj;
			return this.getKey().equals(other.getKey());
		}
		
	}

	public void setCanRunAsJob(boolean canRunAsJob) {
		this.canRunAsJob = canRunAsJob;
	}
	
}
