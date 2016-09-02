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
package org.tigris.subversion.subclipse.ui.actions;
 
import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.resources.IResource;
import org.eclipse.jface.action.IAction;
import org.eclipse.team.core.TeamException;
import org.tigris.subversion.subclipse.ui.Policy;
import org.tigris.subversion.subclipse.ui.operations.ReplaceOperation;
import org.tigris.subversion.svnclientadapter.SVNRevision;

public class ReplaceWithRemoteAction extends WorkspaceAction {
	
	private final SVNRevision revision;

	public ReplaceWithRemoteAction() {
		this.revision = SVNRevision.HEAD;
	}
	
	public ReplaceWithRemoteAction(SVNRevision revision) {
		this.revision = revision;
	}
	
	public void execute(IAction action)  throws InvocationTargetException, InterruptedException {		
		IResource[] resources = null;

		try {
			resources = checkOverwriteOfDirtyResources(getSelectedResources());
		} catch (TeamException e) {
			throw new InvocationTargetException(e);
		}

		// Peform the replace in the background
		ReplaceOperation replaceOperation = new ReplaceOperation(getTargetPart(), resources, this.revision);
//		replaceOperation.setResourcesToUpdate(getSelectedResources());
		replaceOperation.run();
	}
	
	/**
	 * @see org.eclipse.team.internal.ccvs.ui.actions.CVSAction#getErrorTitle()
	 */
	protected String getErrorTitle() {
		return Policy.bind("ReplaceWithRemoteAction.problemMessage"); //$NON-NLS-1$
	}

	/**
	 * @see org.eclipse.team.internal.ccvs.ui.actions.WorkspaceAction#isEnabledForAddedResources()
	 */
	protected boolean isEnabledForAddedResources() {
		return false;
	}

}
