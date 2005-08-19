/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Brock Janiczak - Modified for Subclipse
 *******************************************************************************/
package org.tigris.subversion.subclipse.ui.actions;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.team.core.TeamException;
import org.eclipse.ui.IEditorDescriptor;
import org.eclipse.ui.IEditorRegistry;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.tigris.subversion.subclipse.core.ISVNRemoteFile;
import org.tigris.subversion.subclipse.core.ISVNRemoteResource;
import org.tigris.subversion.subclipse.ui.SVNUIPlugin;
import org.tigris.subversion.subclipse.ui.editor.RemoteFileEditorInput;

public class OpenLogEntryAction extends SVNAction {

    /*
     * @see SVNAction#execute(IAction)
     */
    public void execute(IAction action) throws InterruptedException, InvocationTargetException {
        run(new IRunnableWithProgress() {
            public void run(IProgressMonitor monitor) throws InvocationTargetException {
                IWorkbench workbench = SVNUIPlugin.getPlugin().getWorkbench();
                IEditorRegistry registry = workbench.getEditorRegistry();
                IWorkbenchPage page = workbench.getActiveWorkbenchWindow().getActivePage();
                final ISVNRemoteResource[] entries = getSelectedRemoteResources();
                for (int i = 0; i < entries.length; i++) {
                    ISVNRemoteResource remoteResource = entries[i];
                    if (!(remoteResource instanceof ISVNRemoteFile)) continue;
                    
                    ISVNRemoteFile file = (ISVNRemoteFile)remoteResource;
                    String filename = remoteResource.getName();
                    IEditorDescriptor descriptor = registry.getDefaultEditor(filename);
                    String id;
                    if (descriptor == null) {
                        id = "org.eclipse.ui.DefaultTextEditor"; //$NON-NLS-1$
                    } else {
                        id = descriptor.getId();
                    }
                    try {
                        try {
                            page.openEditor(new RemoteFileEditorInput(file, monitor), id);
                        } catch (PartInitException e) {
                            if (id.equals("org.eclipse.ui.DefaultTextEditor")) { //$NON-NLS-1$
                                throw e;
                            }
                            page.openEditor(new RemoteFileEditorInput(file, monitor), "org.eclipse.ui.DefaultTextEditor"); //$NON-NLS-1$
                        }
                    } catch (PartInitException e) {
                        throw new InvocationTargetException(e);
                    }
                }
            }
        }, false, PROGRESS_BUSYCURSOR); //$NON-NLS-1$
    }
    /*
     * @see TeamAction#isEnabled()
     */
    protected boolean isEnabled() throws TeamException {
        ISVNRemoteResource[] resources = getSelectedRemoteResources();
        if (resources.length == 0) return false;
        if (resources[0] == null) return false;
        return true;
    }
}
