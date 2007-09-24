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
import java.util.List;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.action.IAction;
import org.tigris.subversion.subclipse.core.ISVNLocalResource;
import org.tigris.subversion.subclipse.core.SVNException;
import org.tigris.subversion.subclipse.core.commands.GetStatusCommand;
import org.tigris.subversion.subclipse.core.resources.SVNWorkspaceRoot;
import org.tigris.subversion.subclipse.core.util.Util;
import org.tigris.subversion.subclipse.ui.ISVNUIConstants;
import org.tigris.subversion.subclipse.ui.Policy;
import org.tigris.subversion.subclipse.ui.SVNUIPlugin;
import org.tigris.subversion.subclipse.ui.operations.RevertOperation;
import org.tigris.subversion.subclipse.ui.wizards.dialogs.SvnWizard;
import org.tigris.subversion.subclipse.ui.wizards.dialogs.SvnWizardDialog;
import org.tigris.subversion.subclipse.ui.wizards.dialogs.SvnWizardRevertPage;
import org.tigris.subversion.svnclientadapter.ISVNStatus;
import org.tigris.subversion.svnclientadapter.utils.SVNStatusUtils;

/**
 * Action to restore pristine working copy file 
 */
public class RevertAction extends WorkbenchWindowAction {
    
    private String url;
	private IResource[] resourcesToRevert;
	private HashMap statusMap;
    
	protected void execute(final IAction action) throws InvocationTargetException, InterruptedException {
		statusMap = new HashMap();
		final IResource[] resources = getSelectedResources();
        try {
            IResource[] modifiedResources = getModifiedResources(resources, new NullProgressMonitor());
            if (!confirmRevert(modifiedResources)) return;
            new RevertOperation(getTargetPart(), resourcesToRevert).run();
        } catch (SVNException e) {
            throw new InvocationTargetException(e);
        }
	}
	
	/**
	 * get the modified resources in resources parameter
	 */	
	protected IResource[] getModifiedResources(IResource[] resources, IProgressMonitor iProgressMonitor) throws SVNException {
		 // if only one resource selected, get url.  Revert dialog displays this.
		 if (resources.length == 1) {
			   ISVNLocalResource svnResource = SVNWorkspaceRoot.getSVNResourceFor(resources[0]);
				url = svnResource.getStatus().getUrlString();
			   if ((url == null) || (resources[0].getType() == IResource.FILE)) url = Util.getParentUrl(svnResource);
		 }
		    final List modified = new ArrayList();
		    for (int i = 0; i < resources.length; i++) {
				 IResource resource = resources[i];
				 ISVNLocalResource svnResource = SVNWorkspaceRoot.getSVNResourceFor(resource);
				 
				 // get adds, deletes, updates and property updates.
				 GetStatusCommand command = new GetStatusCommand(svnResource, true, false);
				 command.run(iProgressMonitor);
				 ISVNStatus[] statuses = command.getStatuses();
				 for (int j = 0; j < statuses.length; j++) {
				     if (SVNStatusUtils.isReadyForRevert(statuses[j]) ||
				   		  !SVNStatusUtils.isManaged(statuses[j])) {
				         IResource currentResource = SVNWorkspaceRoot.getResourceFor(statuses[j]);
				         if (currentResource != null) {
				        	 if (SVNStatusUtils.isManaged(statuses[j]) || !Util.isSpecialEclipseFile(currentResource)) {
					             modified.add(currentResource);
		                 		 if (currentResource instanceof IContainer) statusMap.put(currentResource, statuses[j].getPropStatus());
		                 		 else statusMap.put(currentResource, statuses[j].getTextStatus());				             
				        	 }
				         }
				     }
				 }
			}
		    return (IResource[]) modified.toArray(new IResource[modified.size()]);		 
	}
	
	/**
	 * prompt revert of selected resources.
	 */		
	protected boolean confirmRevert(IResource[] modifiedResources) {
	   if (modifiedResources.length == 0) return false;
	   SvnWizardRevertPage revertPage = new SvnWizardRevertPage(modifiedResources, url, statusMap);
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
	
}
