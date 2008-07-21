/*******************************************************************************
 * Copyright (c) 2005, 2006 Subclipse project and others.
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

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.team.core.TeamException;
import org.tigris.subversion.subclipse.ui.Policy;
import org.tigris.subversion.subclipse.ui.dialogs.ShowDifferencesAsUnifiedDiffDialogWC;
import org.tigris.subversion.svnclientadapter.utils.Depth;

public class ShowDifferencesAsUnifiedDiffActionWC extends WorkbenchWindowAction {

	public ShowDifferencesAsUnifiedDiffActionWC() {
		super();
	}

	protected void execute(IAction action) throws InvocationTargetException, InterruptedException {
		IResource[] resources = getSelectedResources();
//		if(resources.length != 0) {
		if (resources.length > 0 && resources[0] instanceof IFile && !resources[0].isSynchronized(Depth.immediates)) {
			if (MessageDialog.openQuestion(getShell(), Policy.bind("DifferencesDialog.compare"), Policy.bind("CompareWithRemoteAction.fileChanged"))) {
				try {
					resources[0].refreshLocal(Depth.immediates, new NullProgressMonitor());
				} catch (CoreException e) {
					throw new InvocationTargetException(e);
				}
			}			
		}
			ShowDifferencesAsUnifiedDiffDialogWC dialog = new ShowDifferencesAsUnifiedDiffDialogWC(getShell(), resources[0], getTargetPart());
			dialog.open();
//		}
	}

	protected boolean isEnabled() throws TeamException {
		return getSelectedResources().length == 1;
	}

}
