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

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.ui.IEditorDescriptor;
import org.eclipse.ui.IEditorRegistry;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.tigris.subversion.subclipse.core.ISVNRemoteFile;
import org.tigris.subversion.subclipse.core.resources.RemoteResource;
import org.tigris.subversion.subclipse.ui.Policy;
import org.tigris.subversion.subclipse.ui.SVNUIPlugin;
import org.tigris.subversion.subclipse.ui.editor.RemoteFileEditorInput;
import org.tigris.subversion.svnclientadapter.SVNRevision;

/**
 * This action is used on ISVNRemoteFile or ILogEntry
 */
public class OpenRemoteFileAction extends SVNAction {
	private boolean usePegRevision;

	/*
	 * @see SVNAction#execute(IAction)
	 */
	public void execute(IAction action) throws InterruptedException, InvocationTargetException {
		run(new IRunnableWithProgress() {
			public void run(IProgressMonitor monitor) throws InvocationTargetException {


				IWorkbench workbench = SVNUIPlugin.getPlugin().getWorkbench();
				IEditorRegistry registry = workbench.getEditorRegistry();
				IWorkbenchPage page = workbench.getActiveWorkbenchWindow().getActivePage();
				ISVNRemoteFile[] files = getSelectedRemoteFiles();
				for (int i = 0; i < files.length; i++) {
					ISVNRemoteFile file = files[i];
					String filename = file.getName();
					IEditorDescriptor descriptor = registry.getDefaultEditor(filename);
					String id;
					if (descriptor == null) {
						descriptor = registry.findEditor(IEditorRegistry.SYSTEM_EXTERNAL_EDITOR_ID);
					}
					if (descriptor == null) {
						id = "org.eclipse.ui.DefaultTextEditor"; //$NON-NLS-1$
					} else {
						id = descriptor.getId();
					}
					try {
						try {
							if (usePegRevision && files[i] instanceof RemoteResource) 
								((RemoteResource)files[i]).setPegRevision(files[i].getRevision());
							else
								((RemoteResource)files[i]).setPegRevision(SVNRevision.HEAD);
							RemoteFileEditorInput input = new RemoteFileEditorInput(files[i],monitor);
							if (descriptor != null && descriptor.isOpenExternal()) {
								input.writeToTempFile();
							}
							page.openEditor(input, id);
						} catch (PartInitException e) {
							if (id.equals("org.eclipse.ui.DefaultTextEditor")) { //$NON-NLS-1$
								throw e;
							} else {
								RemoteFileEditorInput input = new RemoteFileEditorInput(files[i],monitor);
								page.openEditor(input, "org.eclipse.ui.DefaultTextEditor"); //$NON-NLS-1$
							}
						}
					} catch (Exception e) {
						 MessageDialog.openError(getShell(), Policy.bind("OpenRemoteFileAction.0"), e.getMessage());  //$NON-NLS-1$
					}
				}
			}
		}, false, PROGRESS_BUSYCURSOR); //$NON-NLS-1$
	}
	/*
	 * @see TeamAction#isEnabled()
	 */
	protected boolean isEnabled() {
		ISVNRemoteFile[] resources = getSelectedRemoteFiles();
		if (resources.length == 0) return false;
		return true;
	}
	public void setUsePegRevision(boolean usePegRevision) {
		this.usePegRevision = usePegRevision;
	}
}
