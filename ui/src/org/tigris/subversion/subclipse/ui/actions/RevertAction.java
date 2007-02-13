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

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.window.Window;
import org.tigris.subversion.subclipse.core.ISVNLocalResource;
import org.tigris.subversion.subclipse.core.SVNException;
import org.tigris.subversion.subclipse.core.resources.SVNWorkspaceRoot;
import org.tigris.subversion.subclipse.core.util.Util;
import org.tigris.subversion.subclipse.ui.ISVNUIConstants;
import org.tigris.subversion.subclipse.ui.Policy;
import org.tigris.subversion.subclipse.ui.SVNUIPlugin;
import org.tigris.subversion.subclipse.ui.dialogs.RevertDialog;
import org.tigris.subversion.subclipse.ui.operations.RevertOperation;

/**
 * Action to restore pristine working copy file 
 */
public class RevertAction extends WorkbenchWindowAction {
    
    private String url;
	private IResource[] resourcesToRevert;
    
	protected void execute(final IAction action) throws InvocationTargetException, InterruptedException {
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
	    return super.getModifiedResources(resources, iProgressMonitor);
	}
	
	/**
	 * prompt revert of selected resources.
	 */		
	protected boolean confirmRevert(IResource[] modifiedResources) {
	   if (modifiedResources.length == 0) return false;
	   RevertDialog dialog = new RevertDialog(getShell(), modifiedResources, url);
	   boolean revert = (dialog.open() == Window.OK);
	   url = null;
	   resourcesToRevert = dialog.getSelectedResources();
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
