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
package org.tigris.subversion.subclipse.ui.subscriber;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.compare.structuremergeviewer.IDiffElement;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.team.core.RepositoryProvider;
import org.eclipse.team.core.synchronize.SyncInfoSet;
import org.eclipse.team.internal.core.subscribers.ActiveChangeSet;
import org.eclipse.team.internal.core.subscribers.ChangeSet;
import org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration;
import org.tigris.subversion.subclipse.core.ISVNLocalResource;
import org.tigris.subversion.subclipse.core.SVNException;
import org.tigris.subversion.subclipse.core.SVNProviderPlugin;
import org.tigris.subversion.subclipse.core.SVNTeamProvider;
import org.tigris.subversion.subclipse.core.resources.SVNWorkspaceRoot;
import org.tigris.subversion.subclipse.core.util.File2Resource;
import org.tigris.subversion.subclipse.core.util.Util;
import org.tigris.subversion.subclipse.ui.ISVNUIConstants;
import org.tigris.subversion.subclipse.ui.Policy;
import org.tigris.subversion.subclipse.ui.SVNUIPlugin;
import org.tigris.subversion.subclipse.ui.dialogs.CommitToTagsWarningDialog;
import org.tigris.subversion.subclipse.ui.operations.CommitOperation;
import org.tigris.subversion.subclipse.ui.settings.ProjectProperties;
import org.tigris.subversion.subclipse.ui.wizards.dialogs.SvnWizard;
import org.tigris.subversion.subclipse.ui.wizards.dialogs.SvnWizardCommitPage;
import org.tigris.subversion.subclipse.ui.wizards.dialogs.SvnWizardDialog;

/**
 * Sync view operation for putting file system resources
 */
public class CommitSynchronizeOperation extends SVNSynchronizeOperation {
    private String commitComment;
    private IResource[] resourcesToCommit;
    private String url;
    private ChangeSet changeSet;
    private boolean commit;
    private boolean keepLocks;
    private String proposedComment;
    private ISynchronizePageConfiguration configuration;

	protected CommitSynchronizeOperation(ISynchronizePageConfiguration configuration, IDiffElement[] elements, String url, String proposedComment) {
		super(configuration, elements);
		this.configuration = configuration;
		this.url = url;
		this.proposedComment = proposedComment;
	}
	
	private boolean confirmCommit(SyncInfoSet set) {
	    commit = false;
	    IResource[] modified = set.getResources();
	    List conflictFiles = new ArrayList();
	    List filteredModified = new ArrayList();
	    boolean switched = false;
	    for (int i = 0; i < modified.length; i++) {
	        IResource resource = modified[i];
	        filteredModified.add(resource);
	        if (!(resource instanceof IContainer)) {
		        ISVNLocalResource svnResource = SVNWorkspaceRoot.getSVNResourceFor(resource);	    	
		        try {
					if (svnResource.isManaged() && svnResource.getStatus().isTextConflicted()) {
                        IFile conflictNewFile = (IFile) File2Resource
                        .getResource(svnResource.getStatus()
                                .getConflictNew());
                        if (conflictNewFile != null) conflictFiles.add(conflictNewFile);
                        IFile conflictOldFile = (IFile) File2Resource
                        .getResource(svnResource.getStatus()
                                .getConflictOld());
                        if (conflictOldFile != null) conflictFiles.add(conflictOldFile);
                        IFile conflictWorkingFile = (IFile) File2Resource
                        .getResource(svnResource.getStatus()
                                .getConflictWorking());
                        if (conflictWorkingFile != null) conflictFiles.add(conflictWorkingFile);		                            						
					}
					if (svnResource.getStatus().isSwitched()) {
						url = svnResource.getStatus().getUrlString();
						switched = true;
					}
				} catch (SVNException e) {}
		        }
	    }
	    if (switched && modified.length > 1) {
	    	url = null;
	    }
	    if (conflictFiles.size() > 0) {
		    Iterator iter = conflictFiles.iterator();
		    while (iter.hasNext()) {
		    	IFile conflictFile = (IFile)iter.next();
		    	filteredModified.remove(conflictFile);
		    }
		    modified = new IResource[filteredModified.size()];
		    filteredModified.toArray(modified);
	    }
	    if (modified.length > 0) {
	        try {
	      	    IPreferenceStore preferenceStore = SVNUIPlugin.getPlugin().getPreferenceStore();
	    	    boolean commitToTagsPathWithoutWarning = preferenceStore.getBoolean(ISVNUIConstants.PREF_COMMIT_TO_TAGS_PATH_WITHOUT_WARNING);	        	
	    	    if (!commitToTagsPathWithoutWarning && onTagPath(modified)) {
	    	    	commit = true;
	           		getShell().getDisplay().syncExec(new Runnable() {
	        			public void run() {
	        				CommitToTagsWarningDialog dialog = new CommitToTagsWarningDialog(getShell());
	        				commit = dialog.open() == CommitToTagsWarningDialog.OK;
	        			}
	        		});
	           		if (!commit) {
	           			return false;
	           		}
	    	    }
	        	
                ProjectProperties projectProperties = ProjectProperties.getProjectProperties(modified[0]);
                
                SvnWizardCommitPage commitPage = new SvnWizardCommitPage(modified, url, projectProperties, new HashMap(), changeSet, true);                
         	    if (proposedComment == null || proposedComment.length() == 0)
         		  commitPage.setComment(getProposedComment(modified));  
         		else
                  commitPage.setComment(proposedComment);
                commitPage.setSyncInfoSet(set);
         	    SvnWizard wizard = new SvnWizard(commitPage);
        	    final SvnWizardDialog dialog = new SvnWizardDialog(getShell(), wizard);	                
                wizard.setParentDialog(dialog);
        		getShell().getDisplay().syncExec(new Runnable() {
        			public void run() {
        				commit = (dialog.open() == SvnWizardDialog.OK);
        			}
        		});
        	    if (commit) {
        	        resourcesToCommit = commitPage.getSelectedResources();
        	        keepLocks = commitPage.isKeepLocks();        	    	
        	    }
        	    commitComment = commitPage.getComment();
	        } catch (SVNException e) {
	        	if (!e.operationInterrupted()) {
	        		SVNUIPlugin.log(IStatus.ERROR, e.getMessage(), e);
	        	}
            }
	    }
		return commit;
    }

    /* (non-Javadoc)
	 * @see org.eclipse.team.examples.filesystem.ui.FileSystemSynchronizeOperation#promptForConflictHandling(org.eclipse.swt.widgets.Shell, org.eclipse.team.core.synchronize.SyncInfoSet)
	 */
	protected boolean promptForConflictHandling(Shell shell, SyncInfoSet syncSet) {
		return true;
	}
	
    /*
     * Get a proposed comment by looking at the active change sets
     */
    private String getProposedComment(IResource[] resourcesToCommit) {
    	StringBuffer comment = new StringBuffer();
    	ChangeSet[] sets = SVNProviderPlugin.getPlugin().getChangeSetManager().getSets();
    	int numMatchedSets = 0;
    	for (int i = 0; i < sets.length; i++) {
    		ChangeSet set = sets[i];
    		if (isUserSet(set) && containsOne(set, resourcesToCommit)) {
    			if(numMatchedSets > 0) comment.append(System.getProperty("line.separator")); //$NON-NLS-1$
    			comment.append(set.getComment());
    			numMatchedSets++;
    		}
    	}
    	return comment.toString();
    }
    
    private boolean isUserSet(ChangeSet set) {
    	if (set instanceof ActiveChangeSet) {
    		ActiveChangeSet acs = (ActiveChangeSet) set;
    		return acs.isUserCreated();
    	}
    	return false;
    }

    private boolean containsOne(ChangeSet set, IResource[] resourcesToCommit) {
    	for (int j = 0; j < resourcesToCommit.length; j++) {
    		IResource resource = resourcesToCommit[j];
    		if (set.contains(resource)) {
    			return true;
    		}
    		if (set instanceof ActiveChangeSet) {
    			ActiveChangeSet acs = (ActiveChangeSet) set;
    			if (acs.getDiffTree().members(resource).length > 0)
    				return true;
    		}
    	}
    	return false;
    }
	
	/**
	 * Prompts the user to determine how conflicting changes should be handled.
	 * Note: This method is designed to be overridden by test cases.
	 * @return 0 to sync conflicts, 1 to sync all non-conflicts, 2 to cancel
	 */
	private int promptForConflicts(Shell shell, SyncInfoSet syncSet) {
		String[] buttons = new String[] {IDialogConstants.YES_LABEL, IDialogConstants.NO_LABEL};
		String title = Policy.bind("SyncAction.commit.conflict.title"); //$NON-NLS-1$
		String question = Policy.bind("SyncAction.commit.conflict.question"); //$NON-NLS-1$
		final MessageDialog dialog = new MessageDialog(shell, title, null, question, MessageDialog.QUESTION, buttons, 0);
		shell.getDisplay().syncExec(new Runnable() {
			public void run() {
				dialog.open();
			}
		});
		return dialog.getReturnCode();
	}
	
//	private IResource[] getUnaddedResources(SyncInfoSet set) {
//	    IResource[] resources = set.getResources();
//		List result = new ArrayList();
//		for (int i = 0; i < resources.length; i++) {
//			IResource resource = resources[i];
//			if (isAdded(resource)) {
//				result.add(resource);
//			}
//		}
//		return (IResource[]) result.toArray(new IResource[result.size()]);
//	}

//	private boolean isAdded(IResource resource) {
//	    ISVNLocalResource svnResource = SVNWorkspaceRoot.getSVNResourceFor(resource);
//		try {
//			if (svnResource.isIgnored())
//				return false;
//			// visit the children of shared resources
//			if (svnResource.isManaged())
//				return false;
//			if ((resource.getType() == IResource.FOLDER) && isSymLink(resource)) // don't traverse into symlink folders
//				return false;
//		} catch (SVNException e) {
//		    SVNUIPlugin.log(IStatus.ERROR, e.getMessage(), e);
//		    return false;
//		}
//		return true;
//    }
	
//	private boolean isSymLink(IResource resource) {
//		File file = resource.getLocation().toFile();
//	    try {
//	    	if (!file.exists())
//	    		return true;
//	    	else {
//	    		String cnnpath = file.getCanonicalPath();
//	    		String abspath = file.getAbsolutePath();
//	    		return !abspath.equals(cnnpath);
//	    	}
//	    } catch(IOException ex) {
//	      return true;
//	    }	
//	}
	
	public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
		// First, ask the user if they want to include conflicts
		SyncInfoSet syncSet = getSyncInfoSet();
		if (!promptForConflictHandling(getShell(), syncSet)) return;
		// Divide the sync info by project
		final Map projectSyncInfos = getProjectSyncInfoSetMap(syncSet);
		Iterator iter = projectSyncInfos.keySet().iterator();
		final IProject project = (IProject) iter.next();
		SVNTeamProvider provider = (SVNTeamProvider)RepositoryProvider.getProvider(project, SVNUIPlugin.PROVIDER_ID);
		monitor.beginTask(null, projectSyncInfos.size() * 100);
		run(provider, syncSet, Policy.subMonitorFor(monitor,100));
		monitor.done();
	}

    /* (non-Javadoc)
	 * @see org.eclipse.team.examples.filesystem.ui.FileSystemSynchronizeOperation#run(org.eclipse.team.examples.filesystem.FileSystemProvider, org.eclipse.team.core.synchronize.SyncInfoSet, org.eclipse.core.runtime.IProgressMonitor)
	 */
	protected void run(SVNTeamProvider provider, SyncInfoSet set, IProgressMonitor monitor) {
		if (set.hasConflicts() || set.hasIncomingChanges()) {
			switch (promptForConflicts(getShell(), set)) {
			case 0:
				// Yes, commit non-conflicts
				set.removeConflictingNodes();
				set.removeIncomingNodes();
				break;
			case 1:
				// No, stop here
				return;
			default:
				return;
			}	
		}
	    if (confirmCommit(set)) {
	        final IResource[][] resourcesToBeAdded = new IResource[][] { null };
	        final IResource[][] resourcesToBeDeleted = new IResource[][] { null };
		    List toBeAddedList = new ArrayList();
		    List toBeDeletedList = new ArrayList();
		    for (int i = 0; i < resourcesToCommit.length; i++) {
		        IResource resource = resourcesToCommit[i];
		        ISVNLocalResource svnResource = SVNWorkspaceRoot.getSVNResourceFor(resource);
		        try {
                    if (!svnResource.isManaged()) toBeAddedList.add(resource);
                    if (svnResource.getStatus().isMissing()) toBeDeletedList.add(resource);
                } catch (SVNException e) {
                	SVNUIPlugin.log(IStatus.ERROR, e.getMessage(), e);
                }
		    }
		    resourcesToBeAdded[0] = new IResource[toBeAddedList.size()];
		    toBeAddedList.toArray(resourcesToBeAdded[0]);
		    resourcesToBeDeleted[0] = new IResource[toBeDeletedList.size()];
		    toBeDeletedList.toArray(resourcesToBeDeleted[0]);
		    try {
                CommitOperation commit = new CommitOperation(getPart(), resourcesToCommit, resourcesToBeAdded[0], resourcesToBeDeleted[0], resourcesToCommit, commitComment, keepLocks);
                commit.run();
            } catch (InvocationTargetException e) {
            	SVNUIPlugin.log(IStatus.ERROR, e.getMessage(), e);
            } catch (InterruptedException e) {
            	SVNUIPlugin.log(IStatus.ERROR, e.getMessage(), e);
            }
	    }
	}

	public void setChangeSet(ChangeSet changeSet) {
		this.changeSet = changeSet;
	}
	
	private boolean onTagPath(IResource[] modifiedResources) throws SVNException {
	    // Multiple resources selected.
	    if (url == null) {
			 IResource resource = modifiedResources[0];
			 ISVNLocalResource svnResource = SVNWorkspaceRoot.getSVNResourceFor(resource);	        
             String firstUrl = svnResource.getStatus().getUrlString();
             if ((firstUrl == null) || (resource.getType() == IResource.FILE)) firstUrl = Util.getParentUrl(svnResource);
             if (firstUrl.indexOf("/tags/") != -1) return true; //$NON-NLS-1$
	    }
	    // One resource selected.
        else if (url.indexOf("/tags/") != -1) return true; //$NON-NLS-1$
        return false;
    }

}
