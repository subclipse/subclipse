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
package org.tigris.subversion.subclipse.ui.actions;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.internal.core.subscribers.ActiveChangeSet;
import org.eclipse.team.internal.core.subscribers.ChangeSet;
import org.tigris.subversion.subclipse.core.ISVNLocalResource;
import org.tigris.subversion.subclipse.core.SVNException;
import org.tigris.subversion.subclipse.core.SVNProviderPlugin;
import org.tigris.subversion.subclipse.core.commands.GetStatusCommand;
import org.tigris.subversion.subclipse.core.resources.SVNWorkspaceRoot;
import org.tigris.subversion.subclipse.core.util.Util;
import org.tigris.subversion.subclipse.ui.ISVNUIConstants;
import org.tigris.subversion.subclipse.ui.Policy;
import org.tigris.subversion.subclipse.ui.operations.CommitOperation;
import org.tigris.subversion.subclipse.ui.settings.ProjectProperties;
import org.tigris.subversion.subclipse.ui.wizards.dialogs.SvnWizard;
import org.tigris.subversion.subclipse.ui.wizards.dialogs.SvnWizardCommitPage;
import org.tigris.subversion.subclipse.ui.wizards.dialogs.SvnWizardDialog;
import org.tigris.subversion.svnclientadapter.ISVNStatus;
import org.tigris.subversion.svnclientadapter.utils.SVNStatusUtils;

/**
 * Action for checking in files to a subversion provider
 * Prompts the user for a release comment, and shows a selection
 * list of added and modified resources, including unversioned resources.
 * If selected, unversioned resources will be added to version control,
 * and committed.
 */
public class CommitAction extends WorkbenchWindowAction {
	protected String commitComment;
    protected IResource[] resourcesToCommit;
    protected String url;
    protected boolean hasUnaddedResources;
    protected boolean commit;
    protected boolean keepLocks;
    protected IResource[] selectedResources;
    private String proposedComment;
    private boolean sharing;
    
    private HashMap statusMap;
	
    public CommitAction() {
    	
    }
    
	public CommitAction(String proposedComment) {
		this.proposedComment = proposedComment;
	}

	/*
     * get non added resources and prompts for resources to be added
     * prompts for comments
     * add non added files
     * commit selected files
	 * @see SVNAction#execute(IAction)
	 */
	public void execute(IAction action) throws InvocationTargetException, InterruptedException {
		statusMap = new HashMap();
		final IResource[] resources = getSelectedResources();
	    final List resourcesToBeAdded = new ArrayList();
	    final List resourcesToBeDeleted = new ArrayList();
            if (action != null && !action.isEnabled()) { 
            	action.setEnabled(true);
            } 
            else {
            	run(new IRunnableWithProgress() {
        			public void run(IProgressMonitor monitor) throws InvocationTargetException {
        				try {
        				    // search for modified or added, non-ignored resources in the selection.
        				    IResource[] modified = getModifiedResources(resources, monitor);
        					
        				    // if no changes since last commit, do not show commit dialog.
        				    if (modified.length == 0) {
        					    MessageDialog.openInformation(getShell(), Policy.bind("CommitDialog.title"), Policy.bind("CommitDialog.noChanges")); //$NON-NLS-1$ //$NON-NLS-2$
        					    commit = false;
        					} else {
        					    ProjectProperties projectProperties = ProjectProperties.getProjectProperties(modified[0]);
        					    commit = confirmCommit(modified, projectProperties);
        					}

        				    // if commit was not canceled, create a list of any
        				    // unversioned resources that were selected and a list of any missing
        				    // resources that were selected.
        					if (commit) {
        					    for (int i = 0; i < resourcesToCommit.length; i++) {
        					        IResource resource = resourcesToCommit[i];
        					        ISVNLocalResource svnResource = SVNWorkspaceRoot.getSVNResourceFor(resource);
        					        if (!svnResource.isManaged()) resourcesToBeAdded.add(resource);
        					        if (svnResource.getStatus().isMissing()) resourcesToBeDeleted.add(resource);
        					    }
        					}
        				} catch (TeamException e) {
        					throw new InvocationTargetException(e);
        				}
        			}
        		}, true /* cancelable */, PROGRESS_BUSYCURSOR); //$NON-NLS-1$
        		
        		if (!commit) {
        			return; // user canceled
        		}
        		
        		new CommitOperation(getTargetPart(), resources, 
        				(IResource[]) resourcesToBeAdded.toArray(new IResource[resourcesToBeAdded.size()]),
        				(IResource[]) resourcesToBeDeleted.toArray(new IResource[resourcesToBeDeleted.size()]),
        				resourcesToCommit, commitComment, keepLocks).run();
            }
	}
	
	/**
	 * get the modified and unadded resources in resources parameter
	 */	
	protected IResource[] getModifiedResources(IResource[] resources, IProgressMonitor iProgressMonitor) throws SVNException {
	    final List modified = new ArrayList();
	    List unversionedFolders = new ArrayList();
		hasUnaddedResources = false;
	    for (int i = 0; i < resources.length; i++) {
			 IResource resource = resources[i];
			 ISVNLocalResource svnResource = SVNWorkspaceRoot.getSVNResourceFor(resource);
			 
			 // This check is for when the action is called with unmanaged resources
			 if (svnResource.getRepository() == null) {
				 continue;
			 }
			 
			 // if only one resource selected, get url.  Commit dialog displays this.
			 if (resources.length == 1) {
				   url = svnResource.getStatus().getUrlString();
				   if ((url == null) || (resource.getType() == IResource.FILE)) url = Util.getParentUrl(svnResource);
			 }
			 
			 // get adds, deletes, updates and property updates.
			 GetStatusCommand command = new GetStatusCommand(svnResource, true, false);
			 command.run(iProgressMonitor);
			 ISVNStatus[] statuses = command.getStatuses();
			 for (int j = 0; j < statuses.length; j++) {
			     if (SVNStatusUtils.isReadyForCommit(statuses[j]) || SVNStatusUtils.isMissing(statuses[j])) {
			         IResource currentResource = SVNWorkspaceRoot.getResourceFor(statuses[j]);
			         if (currentResource != null) {
			             ISVNLocalResource localResource = SVNWorkspaceRoot.getSVNResourceFor(currentResource);
			             if (!localResource.isIgnored()) {
			                 if (!SVNStatusUtils.isManaged(statuses[j])) {
			                 	hasUnaddedResources = true;
			                 	if ((currentResource.getType() != IResource.FILE) && !isSymLink(currentResource))
			                 		unversionedFolders.add(currentResource);
			                 	else {
//			                 		if (sharing || SVNUIPlugin.getPlugin().getPreferenceStore().getBoolean(ISVNUIConstants.PREF_SHOW_UNADDED_RESOURCES_ON_COMMIT)) {
			                 			if (!modified.contains(currentResource)) {
			                 				modified.add(currentResource);
			   	                 		 if (currentResource instanceof IContainer) statusMap.put(currentResource, statuses[j].getPropStatus());
				                 		 else statusMap.put(currentResource, statuses[j].getTextStatus());				             
//			                 			}
			                 		}
			                 	}
			                 } else
			                	 if (!modified.contains(currentResource)) {
			                		 modified.add(currentResource);
			                 		 if (currentResource instanceof IContainer) statusMap.put(currentResource, statuses[j].getPropStatus());
			                 		 else statusMap.put(currentResource, statuses[j].getTextStatus());				             
			                	 }
			             }
			         }
			     }
			 }
	    }
	    // get unadded resources and add them to the list.
//	    if (sharing || SVNUIPlugin.getPlugin().getPreferenceStore().getBoolean(ISVNUIConstants.PREF_SHOW_UNADDED_RESOURCES_ON_COMMIT)) {
		    IResource[] unaddedResources = getUnaddedResources(unversionedFolders, iProgressMonitor);
		    for (int i = 0; i < unaddedResources.length; i++)
		    	if (!modified.contains(unaddedResources[i])) modified.add(unaddedResources[i]);
//	    }
	    return (IResource[]) modified.toArray(new IResource[modified.size()]);
	}

	/**
	 * prompt commit of selected resources.
	 * @throws SVNException
	 */		
	protected boolean confirmCommit(IResource[] modifiedResources, ProjectProperties projectProperties) throws SVNException {
	   if (onTagPath(modifiedResources)) {
	       // Warning - working copy appears to be on a tag path.
	       if (!MessageDialog.openQuestion(getShell(), Policy.bind("CommitDialog.title"), Policy.bind("CommitDialog.tag"))) //$NON-NLS-1$ //$NON-NLS-2$
	           return false;	       
	   }
	   SvnWizardCommitPage commitPage = new SvnWizardCommitPage(modifiedResources, url, projectProperties, statusMap, null);
	   commitPage.setSharing(sharing);
	   
	   SvnWizard wizard = new SvnWizard(commitPage);
	   SvnWizardDialog dialog = new SvnWizardDialog(getShell(), wizard);	
	   if (proposedComment == null || proposedComment.length() == 0) {
		  commitPage.setComment(getProposedComment(modifiedResources));
	   } else {
		   commitPage.setComment(proposedComment);
	   }	   
	   wizard.setParentDialog(dialog);
	   boolean commitOK = (dialog.open() == SvnWizardDialog.OK);
	   url = null;
	   commitComment = commitPage.getComment();
	   resourcesToCommit = commitPage.getSelectedResources();
	   keepLocks = commitPage.isKeepLocks();	   
	   return commitOK;
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

    /**
	 * @see org.tigris.subversion.subclipse.ui.actions.SVNAction#getErrorTitle()
	 */
	protected String getErrorTitle() {
		return Policy.bind("CommitAction.commitFailed"); //$NON-NLS-1$
	}

	/**
	 * @see org.tigris.subversion.subclipse.ui.actions.WorkspaceAction#isEnabledForUnmanagedResources()
	 */
	protected boolean isEnabledForUnmanagedResources() {
		return true;
	}
    
    /*
     *  (non-Javadoc)
     * @see org.tigris.subversion.subclipse.ui.actions.WorkspaceAction#isEnabledForInaccessibleResources()
     */
    protected boolean isEnabledForInaccessibleResources() {
        return true;
    }

	/**
	 * get the unadded resources in resources parameter
	 */
	private IResource[] getUnaddedResources(List resources, IProgressMonitor iProgressMonitor) throws SVNException {
		final List unadded = new ArrayList();
		final SVNException[] exception = new SVNException[] { null };
		for (Iterator iter = resources.iterator(); iter.hasNext();) {
			IResource resource = (IResource) iter.next();
	        if (resource.exists()) {
			    // visit each resource deeply
			    try {
				    resource.accept(new IResourceVisitor() {
					public boolean visit(IResource aResource) {
						ISVNLocalResource svnResource = SVNWorkspaceRoot.getSVNResourceFor(aResource);
						// skip ignored resources and their children
						try {
							if (svnResource.isIgnored())
								return false;
							// visit the children of shared resources
							if (svnResource.isManaged())
								return true;
							if ((aResource.getType() == IResource.FOLDER) && isSymLink(aResource)) // don't traverse into symlink folders
								return false;
						} catch (SVNException e) {
							exception[0] = e;
						}
						// file/folder is unshared so record it
						unadded.add(aResource);
						return aResource.getType() == IResource.FOLDER;
					}
				}, IResource.DEPTH_INFINITE, false /* include phantoms */);
			    } catch (CoreException e) {
				    throw SVNException.wrapException(e);
			    }
			    if (exception[0] != null) throw exception[0];
	        }
		}
		if (unadded.size() > 0) hasUnaddedResources = true;
		return (IResource[]) unadded.toArray(new IResource[unadded.size()]);
	}
	
	protected boolean isSymLink(IResource resource) {
		File file = resource.getLocation().toFile();
	    try {
	    	if (!file.exists())
	    		return true;
	    	else {
	    		String cnnpath = file.getCanonicalPath();
	    		String abspath = file.getAbsolutePath();
	    		return !abspath.equals(cnnpath);
	    	}
	    } catch(IOException ex) {
	      return true;
	    }	
	}	
    protected IResource[] getSelectedResources() {
        if (selectedResources == null)
            return super.getSelectedResources();
        else
            return selectedResources;
    }
    public void setSelectedResources(IResource[] selectedResources) {
        this.selectedResources = selectedResources;
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
    
    public boolean hasOutgoingChanges() {
    	try {
    		return getModifiedResources(selectedResources, new NullProgressMonitor()).length > 0;
    	} catch (SVNException e) {
    	}
    	return false;
    }

	public void setSharing(boolean sharing) {
		this.sharing = sharing;
	}    

	protected String getImageId()
	{
		return ISVNUIConstants.IMG_MENU_COMMIT;
	}
    
}
