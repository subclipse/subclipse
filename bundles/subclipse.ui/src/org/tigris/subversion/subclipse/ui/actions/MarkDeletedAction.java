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

import org.eclipse.core.resources.IResource;
import org.eclipse.jface.action.IAction;
import org.eclipse.team.core.TeamException;
import org.tigris.subversion.subclipse.core.ISVNLocalResource;
import org.tigris.subversion.subclipse.core.resources.SVNWorkspaceRoot;
import org.tigris.subversion.subclipse.ui.operations.RemoveOperation;

public class MarkDeletedAction extends WorkspaceAction {

	public MarkDeletedAction() {
		super();
	}

	protected void execute(IAction action) throws InvocationTargetException, InterruptedException {
		RemoveOperation removeOperation = new RemoveOperation(getTargetPart(), getSelectedResources());
		removeOperation.run();
	}

	protected boolean isEnabled() throws TeamException {
		boolean enabled = super.isEnabled();
		if (!enabled) return false;
		IResource[] resources = getSelectedResources();
		for (int i = 0; i < resources.length; i++) {
			IResource resource = resources[i];
			if (resource.exists()) return false;
			ISVNLocalResource svnResource = SVNWorkspaceRoot.getSVNResourceFor(resource);
			if (!svnResource.getStatusFromCache().isMissing()) return false;
		}
		return true;
	}

	protected boolean isEnabledForInaccessibleResources() {
		return true;
	}

	protected boolean isEnabledForAddedResources() {
		return false;
	}

}
