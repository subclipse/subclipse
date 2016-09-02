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
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.team.core.TeamException;
import org.tigris.subversion.subclipse.core.ISVNRemoteFolder;
import org.tigris.subversion.subclipse.core.ISVNRemoteResource;
import org.tigris.subversion.subclipse.core.SVNProviderPlugin;
import org.tigris.subversion.subclipse.ui.SVNUIPlugin;
import org.tigris.subversion.subclipse.ui.repository.RepositoryManager;

/**
 * Paste remote resources to selected directory 
 */
public class PasteRemoteResourceAction extends SVNAction {

	/* (non-Javadoc)
	 * @see org.tigris.subversion.subclipse.ui.actions.SVNAction#execute(org.eclipse.jface.action.IAction)
	 */
	protected void execute(IAction action)
		throws InvocationTargetException, InterruptedException {
	        Clipboard clipboard = new Clipboard(getShell().getDisplay());
	        final ISVNRemoteResource resource = (ISVNRemoteResource)clipboard.getContents(RemoteResourceTransfer.getInstance());
	        clipboard.dispose();
		
            RepositoryManager manager = SVNUIPlugin.getPlugin().getRepositoryManager();
            final String message = manager.promptForComment(getShell(), new IResource[]{});

            if (message == null)
                return; // canceled
            
            ISVNRemoteResource selectedResource = getSelectedRemoteResources()[0];
            final ISVNRemoteFolder destination = 
                (selectedResource.isFolder()?
                    (ISVNRemoteFolder)selectedResource:selectedResource.getParent());
            
            run(new IRunnableWithProgress() {
                public void run(IProgressMonitor monitor)throws  InvocationTargetException {
                    try {
                        SVNProviderPlugin.getPlugin().getRepositoryResourcesManager().
                            copyRemoteResource(resource,destination,message,monitor);
                    } catch (TeamException e) {
                        throw new InvocationTargetException(e);
                    }
                }
            }, true /* cancelable */, PROGRESS_BUSYCURSOR); //$NON-NLS-1$
    }

	/* (non-Javadoc)
	 * @see org.eclipse.team.internal.ui.actions.TeamAction#isEnabled()
	 */
	protected boolean isEnabled() {
        if (getSelectedRemoteResources().length != 1)
            return false;
        
        boolean result;
        Clipboard clipboard = new Clipboard(getShell().getDisplay());
        result = clipboard.getContents(RemoteResourceTransfer.getInstance()) != null;
        clipboard.dispose();
		return result;
	}

}
