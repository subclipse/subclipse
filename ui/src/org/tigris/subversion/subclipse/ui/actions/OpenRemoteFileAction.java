/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Cédric Chabanois (cchabanois@ifrance.com) - modified for Subversion 
 *******************************************************************************/
package org.tigris.subversion.subclipse.ui.actions;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Iterator;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.team.core.TeamException;
import org.eclipse.ui.IEditorDescriptor;
import org.eclipse.ui.IEditorRegistry;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.tigris.subversion.subclipse.core.ISVNRemoteFile;
import org.tigris.subversion.subclipse.core.history.ILogEntry;
import org.tigris.subversion.subclipse.ui.SVNUIPlugin;
import org.tigris.subversion.subclipse.ui.editor.RemoteFileEditorInput;

/**
 * This action is used on ISVNRemoteFile or ILogEntry
 */
public class OpenRemoteFileAction extends SVNAction {
	/**
	 * Returns the selected remote files
	 */
	protected ISVNRemoteFile[] getSelectedRemoteFiles() {
		ArrayList resources = null;
		if (!selection.isEmpty()) {
			resources = new ArrayList();
			Iterator elements = ((IStructuredSelection) selection).iterator();
			while (elements.hasNext()) {
				Object next = elements.next();
				if (next instanceof ISVNRemoteFile) {
					resources.add(next);
					continue;
				}
				if (next instanceof ILogEntry) {
					resources.add(((ILogEntry)next).getRemoteResource());
					continue;
				}
				if (next instanceof IAdaptable) {
					IAdaptable a = (IAdaptable) next;
					Object adapter = a.getAdapter(ISVNRemoteFile.class);
					if (adapter instanceof ISVNRemoteFile) {
						resources.add(adapter);
						continue;
					}
				}
			}
		}
		if (resources != null && !resources.isEmpty()) {
			ISVNRemoteFile[] result = new ISVNRemoteFile[resources.size()];
			resources.toArray(result);
			return result;
		}
		return new ISVNRemoteFile[0];
	}
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
						id = "org.eclipse.ui.DefaultTextEditor"; //$NON-NLS-1$
					} else {
						id = descriptor.getId();
					}
					try {
						try {
							page.openEditor(new RemoteFileEditorInput(files[i]), id);
						} catch (PartInitException e) {
							if (id.equals("org.eclipse.ui.DefaultTextEditor")) { //$NON-NLS-1$
								throw e;
							} else {
								page.openEditor(new RemoteFileEditorInput(files[i]), "org.eclipse.ui.DefaultTextEditor"); //$NON-NLS-1$
							}
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
		ISVNRemoteFile[] resources = getSelectedRemoteFiles();
		if (resources.length == 0) return false;
		return true;
	}
}
