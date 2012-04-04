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
 
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.tigris.subversion.subclipse.core.ISVNCoreConstants;
import org.tigris.subversion.subclipse.core.ISVNLocalResource;
import org.tigris.subversion.subclipse.core.SVNException;
import org.tigris.subversion.subclipse.core.SVNProviderPlugin;
import org.tigris.subversion.subclipse.core.commands.GetStatusCommand;
import org.tigris.subversion.subclipse.core.resources.SVNWorkspaceRoot;
import org.tigris.subversion.subclipse.core.util.File2Resource;
import org.tigris.subversion.subclipse.core.util.Util;
import org.tigris.subversion.subclipse.ui.ISVNUIConstants;
import org.tigris.subversion.subclipse.ui.Policy;
import org.tigris.subversion.subclipse.ui.SVNUIPlugin;
import org.tigris.subversion.subclipse.ui.operations.RevertOperation;
import org.tigris.subversion.subclipse.ui.wizards.dialogs.SvnWizard;
import org.tigris.subversion.subclipse.ui.wizards.dialogs.SvnWizardDialog;
import org.tigris.subversion.subclipse.ui.wizards.dialogs.SvnWizardRevertPage;
import org.tigris.subversion.svnclientadapter.ISVNStatus;
import org.tigris.subversion.svnclientadapter.SVNStatusKind;
import org.tigris.subversion.svnclientadapter.utils.SVNStatusUtils;

/**
 * Action to restore pristine working copy file 
 */
public class RevertAction extends WorkbenchWindowAction {
    
    private String url;
	private IResource[] resourcesToRevert;
	private HashMap statusMap;
	private SvnWizardRevertPage revertPage;
	private IResource[] selectedResources;
	private boolean canRunAsJob = true;
	private boolean showNothingToRevertMessage = true;
	private boolean includesExternals;
	
	private boolean resourcesHidden;
    
	protected void execute(final IAction action) throws InvocationTargetException, InterruptedException {
		statusMap = new HashMap();
		includesExternals = false;
		
		resourcesHidden = false;
		
		final IResource[] resources = getSelectedResources();
        try {
            IResource[] modifiedResources = getModifiedResources(resources, new NullProgressMonitor());
            if (!confirmRevert(modifiedResources)) return;
            RevertOperation revertOperation = null;
            if (revertPage != null && !revertPage.isResourceRemoved() && !includesExternals) {
            	revertOperation = new RevertOperation(getTargetPart(), resources);
            	revertOperation.setRecurse(true);
            	revertOperation.setResourcesToRevert(resourcesToRevert);
            } else {
            	revertOperation = new RevertOperation(getTargetPart(), resourcesToRevert);
            }
            revertOperation.setCanRunAsJob(canRunAsJob);
            revertOperation.run();
        } catch (SVNException e) {
        	if (!e.operationInterrupted()) {
        		throw new InvocationTargetException(e);
        	}
        }
	}
	
	/**
	 * get the modified resources in resources parameter
	 */	
	protected IResource[] getModifiedResources(IResource[] resources, IProgressMonitor iProgressMonitor) throws SVNException {
		 boolean ignoreHiddenChanges = SVNProviderPlugin.getPlugin().getPluginPreferences().getBoolean(ISVNCoreConstants.PREF_IGNORE_HIDDEN_CHANGES);
		 // if only one resource selected, get url.  Revert dialog displays this.
		 if (resources.length == 1) {
			   ISVNLocalResource svnResource = SVNWorkspaceRoot.getSVNResourceFor(resources[0]);
				url = svnResource.getStatus().getUrlString();
			   if ((url == null) || (resources[0].getType() == IResource.FILE)) url = Util.getParentUrl(svnResource);
		 }
		 	ArrayList conflictFiles = new ArrayList();
		    final List modified = new ArrayList();
		    for (int i = 0; i < resources.length; i++) {
				 IResource resource = resources[i];
				 ISVNLocalResource svnResource = SVNWorkspaceRoot.getSVNResourceFor(resource);
				 // get adds, deletes, updates and property updates.
				 GetStatusCommand command = new GetStatusCommand(svnResource, true, false);
				 command.run(iProgressMonitor);
				 ISVNStatus[] statuses = command.getStatuses();
				 for (int j = 0; j < statuses.length; j++) {
		             if (statuses[j].isFileExternal() || SVNStatusKind.EXTERNAL.equals(statuses[j].getTextStatus())) {
		            	 includesExternals = true;
		             }
					 boolean isManaged = SVNStatusUtils.isManaged(statuses[j]);
				     if (SVNStatusUtils.isReadyForRevert(statuses[j]) || !isManaged) {
				         IResource currentResource = SVNWorkspaceRoot.getResourceFor(resource, statuses[j]);
				         if (currentResource != null) {
				        	 ISVNLocalResource localResource = SVNWorkspaceRoot.getSVNResourceFor(currentResource);
				        	 if (!localResource.isIgnored()) {
					        	 if (isManaged || !Util.isSpecialEclipseFile(currentResource)) {
					        		 boolean hidden = Util.isHidden(currentResource);
					        		 if (ignoreHiddenChanges && hidden) {
					        			 resourcesHidden = true;
					        		 }
					        		 if ((!ignoreHiddenChanges && isManaged) || !hidden) {
							             modified.add(currentResource);							             
				                 		 if (currentResource instanceof IContainer) statusMap.put(currentResource, statuses[j].getPropStatus());
				                 		 else {
				                 			statusMap.put(currentResource, statuses[j].getTextStatus());
				                 			if (SVNStatusUtils.isTextConflicted(statuses[j])) {
				                                IFile conflictNewFile = (IFile) File2Resource
				                                .getResource(statuses[j]
				                                        .getConflictNew());
				                                if (conflictNewFile != null) conflictFiles.add(conflictNewFile);
				                                IFile conflictOldFile = (IFile) File2Resource
				                                .getResource(statuses[j]
				                                        .getConflictOld());
				                                if (conflictOldFile != null) conflictFiles.add(conflictOldFile);
				                                IFile conflictWorkingFile = (IFile) File2Resource
				                                .getResource(statuses[j]
				                                        .getConflictWorking());
				                                if (conflictWorkingFile != null) conflictFiles.add(conflictWorkingFile);		                                
				                 			}
				                 		 }
					        		 }
					        	 }
				        	 }
				         }
				     }
				 }
			}
		    Iterator iter = conflictFiles.iterator();
		    while (iter.hasNext()) {
		    	IFile conflictFile = (IFile)iter.next();
		    	modified.remove(conflictFile);
		    	statusMap.remove(conflictFile);
		    }
		    return (IResource[]) modified.toArray(new IResource[modified.size()]);		 
	}
	
	/**
	 * prompt revert of selected resources.
	 */		
	protected boolean confirmRevert(IResource[] modifiedResources) {
	   if (modifiedResources.length == 0) {
		   if (showNothingToRevertMessage) {
			   MessageDialog.openInformation(Display.getDefault().getActiveShell(), Policy.bind("RevertAction.0"), Policy.bind("RevertAction.1")); //$NON-NLS-1$ //$NON-NLS-2$
		   }
		   return false;
	   }
	   revertPage = new SvnWizardRevertPage(modifiedResources, url, statusMap, false);
	   revertPage.setResourceRemoved(resourcesHidden);
	   SvnWizard wizard = new SvnWizard(revertPage);
	   SvnWizardDialog dialog = new SvnWizardDialog(getShell(), wizard);
	   boolean revert = (dialog.open() == SvnWizardDialog.OK);
	   url = null;
	   resourcesToRevert = revertPage.getSelectedResources();
	   return revert;
	}
	
	/**
	 * @see org.eclipse.team.internal.ccvs.ui.actions.CVSAction#getErrorTitle()
	 */
	protected String getErrorTitle() {
		return Policy.bind("RevertAction.error"); //$NON-NLS-1$
	}

	/**
	 * @see org.eclipse.team.internal.ccvs.ui.actions.WorkspaceAction#isEnabledForManagedResources()
	 */
	protected boolean isEnabledForManagedResources() {
		return true;
	}

	/**
	 * @see org.eclipse.team.internal.ccvs.ui.actions.WorkspaceAction#isEnabledForUnmanagedResources()
	 */
	protected boolean isEnabledForUnmanagedResources() {
		return SVNUIPlugin.getPlugin().getPreferenceStore().getBoolean(ISVNUIConstants.PREF_REMOVE_UNADDED_RESOURCES_ON_REPLACE);
	}

    /*
     *  (non-Javadoc)
     * @see org.tigris.subversion.subclipse.ui.actions.WorkspaceAction#isEnabledForInaccessibleResources()
     */
    protected boolean isEnabledForInaccessibleResources() {
        return true;
    }

	protected String getImageId()
	{
		return ISVNUIConstants.IMG_MENU_REVERT;
	}

	protected IResource[] getSelectedResources() {
		if (selectedResources == null) return super.getSelectedResources();
		else return selectedResources;
	}

	public void setSelectedResources(IResource[] selectedResources) {
		this.selectedResources = selectedResources;
	}

	public void setCanRunAsJob(boolean canRunAsJob) {
		this.canRunAsJob = canRunAsJob;
	}

	public void setShowNothingToRevertMessage(boolean showNothingToRevertMessage) {
		this.showNothingToRevertMessage = showNothingToRevertMessage;
	}
	
}
