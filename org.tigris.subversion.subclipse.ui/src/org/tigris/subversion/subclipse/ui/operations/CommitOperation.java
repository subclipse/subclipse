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
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.team.core.RepositoryProvider;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration;
import org.eclipse.ui.IWorkbenchPart;
import org.tigris.subversion.subclipse.core.ISVNLocalResource;
import org.tigris.subversion.subclipse.core.SVNException;
import org.tigris.subversion.subclipse.core.SVNProviderPlugin;
import org.tigris.subversion.subclipse.core.SVNTeamProvider;
import org.tigris.subversion.subclipse.core.resources.SVNWorkspaceRoot;
import org.tigris.subversion.subclipse.ui.ISVNUIConstants;
import org.tigris.subversion.subclipse.ui.Policy;
import org.tigris.subversion.subclipse.ui.SVNUIPlugin;
import org.tigris.subversion.subclipse.ui.subscriber.SVNSynchronizeParticipant;
import org.tigris.subversion.svnclientadapter.ISVNClientAdapter;
import org.tigris.subversion.svnclientadapter.ISVNInfo;
import org.tigris.subversion.svnclientadapter.SVNClientException;
import org.tigris.subversion.svnclientadapter.SVNUrl;

public class CommitOperation extends SVNOperation {
//    private IResource[] selectedResources;
    private IResource[] resourcesToAdd;
    private IResource[] resourcesToDelete;
    private IResource[] resourcesToCommit;
    private String commitComment;
    private boolean keepLocks;
    private ISVNClientAdapter svnClient;
    private ISynchronizePageConfiguration configuration;
    private boolean useJavaHLHack = true;

    public CommitOperation(IWorkbenchPart part, IResource[] selectedResources, IResource[] resourcesToAdd, IResource[] resourcesToDelete, IResource[] resourcesToCommit, String commitComment, boolean keepLocks) {
        super(part);
//        this.selectedResources = selectedResources;
        this.resourcesToAdd = resourcesToAdd;
        this.resourcesToDelete = resourcesToDelete;
        this.resourcesToCommit = resourcesToCommit;
        this.commitComment = commitComment;
        this.keepLocks = keepLocks;
    }

    protected void execute(IProgressMonitor monitor) throws SVNException, InterruptedException {
		String adminFolderName = SVNProviderPlugin.getPlugin().getAdminDirectoryName();
		List cleanUpPathList = new ArrayList();
        try {
        	svnClient = SVNProviderPlugin.getPlugin().getSVNClientManager().createSVNClient();
        	if (resourcesToAdd.length > 0) {
			    Map table = getProviderMapping(resourcesToAdd);
				if (table.get(null) != null) {
					throw new SVNException(Policy.bind("RepositoryManager.addErrorNotAssociated"));  //$NON-NLS-1$
				}
				Set keySet = table.keySet();
				Iterator iterator = keySet.iterator();
				while (iterator.hasNext()) {
					SVNTeamProvider provider = (SVNTeamProvider)iterator.next();
					List list = (List)table.get(provider);
					IResource[] providerResources = (IResource[])list.toArray(new IResource[list.size()]);
					provider.add(providerResources, IResource.DEPTH_ZERO, null);
				}						
			}
        	if (resourcesToDelete.length > 0) {
				ISVNClientAdapter svnDeleteClient = null; // use an adapter that will log to console
			    Map table = getProviderMapping(resourcesToDelete);
				if (table.get(null) != null) {
					throw new SVNException(Policy.bind("RepositoryManager.addErrorNotAssociated"));  //$NON-NLS-1$
				}
				Set keySet = table.keySet();
				Iterator iterator = keySet.iterator();
				while (iterator.hasNext()) {
					SVNTeamProvider provider = (SVNTeamProvider)iterator.next();
					List list = (List)table.get(provider);
					IResource[] providerResources = (IResource[])list.toArray(new IResource[list.size()]);
					File[] files = new File[providerResources.length];
					for (int i = 0; i < providerResources.length; i++) {
						ISVNLocalResource svnResource = SVNWorkspaceRoot.getSVNResourceFor(providerResources[i]);
						if (svnDeleteClient == null)
						    svnDeleteClient = svnResource.getRepository().getSVNClient();
						files[i] = svnResource.getFile();
					}
					try {
						svnDeleteClient.remove(files, true);
					} catch (SVNClientException e) {
						throw new TeamException(e.getMessage());
					}
				}						
			}
        	setJavaHLHackMode(resourcesToCommit);
			Map table = getCommitProviderMapping(resourcesToCommit);
			Set keySet = table.keySet();
			Iterator iterator = keySet.iterator();
	        monitor.beginTask(null, 100 * keySet.size());
			while (iterator.hasNext()) {
				ProjectAndRepository mapKey = (ProjectAndRepository)iterator.next();
				SVNTeamProvider provider = mapKey.getTeamProvider();
				List list = (List)table.get(mapKey);
				IResource[] providerResources = (IResource[])list.toArray(new IResource[list.size()]);
				if(createAdminFolder(mapKey.getParent(), adminFolderName, providerResources))
					cleanUpPathList.add(mapKey.getParent());
				provider.checkin(providerResources, commitComment, keepLocks, getDepth(providerResources), Policy.subMonitorFor(monitor, 100));
			}			
//			for (int i = 0; i < selectedResources.length; i++) {
//				IResource projectHandle = selectedResources[i].getProject();
//				projectHandle.refreshLocal(IResource.DEPTH_INFINITE, monitor);
//			}		
        } catch (TeamException e) {
			throw SVNException.wrapException(e);
//		} catch (CoreException e) {
//			throw SVNException.wrapException(e);
		} finally {
			monitor.done();
			deleteAdminFolders(cleanUpPathList, adminFolderName);
			// refresh the Synch view
			if (configuration != null) {
				SVNSynchronizeParticipant sync = (SVNSynchronizeParticipant) configuration.getParticipant();
				IResource[] roots = sync.getResources();
				// Reduce the array to just the roots that were affected by this
				// commit
				if (roots.length > 1)
					roots = reduceRoots(roots);
				sync.refresh(roots, monitor);
			}
		}
    }

    /**
     * This method deletes any metadata folders that were produced during the commit
     * process.  They were tracked in a List that is passed in to the method
     * @param pathList
     * @param adminFolderName
     */
    private void deleteAdminFolders(List pathList, String adminFolderName) {
		for (Iterator iterator = pathList.iterator(); iterator.hasNext();) {
			File path = new File((File) iterator.next(), adminFolderName);
			deleteFolder(path);
		}
	}
    
    // Deletes all files and subdirectories under dir.
    // Returns true if all delete was successful.
    private static boolean deleteFolder(File folder) {
        if (folder.isDirectory()) {
            String[] children = folder.list();
            for (int i=0; i< children.length; i++) {
                if (!deleteFolder(new File(folder, children[i]))) {
                    return false;
                }
            }
        }
    
        // The folder should be empty so delete it
        return folder.delete();
    }
    
	/**
	 * This method generate a Subversion metadata folder with an entries and format
	 * file and a tmp folder.  It is used as a hack to trick commit into committing files from
	 * two disjointed working copies from the same repository.
	 * 
	 * If the resource array contains any IProjects then it needs to create a more
	 * detailed entries file because apparently Subversion will look more closely
	 * at its contents in that scenario
	 * 
	 * @param path             Folder to create admin folder beneath
	 * @param adminFolderName  .svn or _svn
	 * @param resources        Resources that will be committed
	 * @return
	 */
	private boolean createAdminFolder(File path, String adminFolderName, IResource[] resources) {
		if (!useJavaHLHack)
			return false;
		String url = null;
		String uuid = null;
		String time = null;
		for (int i = 0; i < resources.length; i++) {
			if (resources[i].getType() == IResource.PROJECT) {
				ISVNInfo info = getSVNInfo(SVNWorkspaceRoot.getSVNResourceFor(resources[i]));
				if (info != null) {
					url = info.getRepository().toString();
					uuid = info.getUuid();
					time = "2005-01-01T21:03:13.980237Z"; // made up value
				}
				break;
			}
		}
		File admin = new File(path, adminFolderName);
		if (admin.mkdir()) {
			File tmp = new File(admin, "tmp");
			tmp.mkdir();
			File entries = new File(admin, "entries");
			File format = new File(admin, "format");
			try {
				entries.createNewFile();
				format.createNewFile();
				FileWriter ew = new FileWriter(entries);
				if (url == null)
					ew.write("9\n");
				else
					ew.write("9\n\ndir\n0\n"
								+ url
								+ "\n"
								+ url
								+ "\n\n\n\n"
								+ time
								+ "\n0\n\n\n\nsvn:special svn:externals svn:needs-lock\n\n\n\n\n\n\n\n\n\n\n\n"
								+ uuid + "\n\f\n");
				ew.flush();
				ew.close();
				FileWriter fw = new FileWriter(format);
				fw.write("9\n");
				fw.flush();
				fw.close();
			} catch (IOException e) {
			}
				
		} else
			return false;
		return true;
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
    
	private Map getCommitProviderMapping(IResource[] resources) {
		ProjectAndRepository mapKey = null;
		Map result = new HashMap();
		for (int i = 0; i < resources.length; i++) {
			if (mapKey == null || !svnClient.canCommitAcrossWC()) {
				SVNTeamProvider provider = (SVNTeamProvider) RepositoryProvider.getProvider(resources[i].getProject(), SVNProviderPlugin.getTypeId());
				mapKey = new ProjectAndRepository(provider, getRootURL(SVNWorkspaceRoot.getSVNResourceFor(resources[i])));
			}
			List list = (List)result.get(mapKey);
			if (list == null) {
				list = new ArrayList();
				result.put(mapKey, list);
			}
			list.add(resources[i]);
		}
		return result;
	}
    
	private Map getProviderMapping(IResource[] resources) {
		RepositoryProvider provider = null;
		Map result = new HashMap();
		for (int i = 0; i < resources.length; i++) {
			if (provider == null || !svnClient.canCommitAcrossWC())
				provider = RepositoryProvider.getProvider(resources[i].getProject(), SVNProviderPlugin.getTypeId());
			List list = (List)result.get(provider);
			if (list == null) {
				list = new ArrayList();
				result.put(provider, list);
			}
			list.add(resources[i]);
		}
		return result;
	}

	private String getRootURL(ISVNLocalResource localResource) {
		if (!useJavaHLHack)
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
		if (!useJavaHLHack)
			return null;
		if (localResource == null)
			return null;
		File file = localResource.getFile();
		if (file == null)
			return null;
    	try {
			svnClient = SVNProviderPlugin.getPlugin().getSVNClientManager().createSVNClient();
		} catch (SVNException e) {
			return null;
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
    	return info;
	}

	public void setConfiguration(ISynchronizePageConfiguration configuration) {
		this.configuration = configuration;
	}

	private IResource[] reduceRoots(IResource[] roots) {
		List rootArray = new ArrayList();
		for (int i = 0; i < roots.length; i++) {
			for (int j = 0; j < resourcesToCommit.length; j++) {
				if (resourcesToCommit[j].getFullPath().toString().startsWith(roots[i].getFullPath().toString())) {
					rootArray.add(roots[i]);
					break;
				}
			}		
		}
		roots = new IResource[rootArray.size()];
		rootArray.toArray(roots);
		return roots;
	}
	
	/**
	 * This method performs an optmization to see if we need to implement
	 * the commit hack of creating a metadata folder.  If all of the resources
	 * belong to the same project we do not need to worry about the hack.
	 * @param resources
	 */
	private void setJavaHLHackMode(IResource[] resources) {
		if (!SVNUIPlugin.getPlugin().getPreferenceStore().getBoolean(ISVNUIConstants.PREF_USE_JAVAHL_COMMIT_HACK)) {
			useJavaHLHack = false;
			return;
		}
		
		if (svnClient.canCommitAcrossWC()) {
			useJavaHLHack = false;
			return;
	    }
//
// This was an optimization to turn this feature off when committing from
// a single project.  The problem is that if the project uses svn:externals
// from a different repository then you cannot commit everything together.
// Removing this optimization makes it work since we group the commits by
// repository.
//
//		Set projects = new HashSet();
//		for (int i = 0; i < resources.length; i++) {
//			projects.add(resources[i].getProject());
//		}
//		if (projects.size() < 2)
//			useJavaHLHack = false;
	}
	
	private class ProjectAndRepository {
		
		private SVNTeamProvider provider;
		private String rootURL;
		private String key;
		private File parent;

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
		
		public File getParent() {
			if (parent == null) {
				if (useJavaHLHack)
					parent = provider.getSVNWorkspaceRoot().getLocalRoot().getFile().getParentFile();
				else
					parent = provider.getProject().getFullPath().toFile();
				if (parent == null)
					parent = provider.getSVNWorkspaceRoot().getLocalRoot().getFile();
			}
			return parent;
		}
		
		private String getKey() {
			if (key == null)
				try {
					key = getParent().getCanonicalPath() + rootURL;
				} catch (IOException e) {
					key = getParent().getAbsolutePath() + rootURL;
				}
			return key;
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
	
}
