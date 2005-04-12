/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.tigris.subversion.subclipse.ui.actions;

import org.eclipse.team.core.TeamException;
import org.tigris.subversion.svnclientadapter.SVNRevision;

public class CompareWithPreviousRevisionAction extends CompareWithRemoteAction {

	/**
	 * Creates a new compare action that will compare against the PREVIOUS revision
	 */
	public CompareWithPreviousRevisionAction() {
		super(SVNRevision.PREVIOUS);
	}
	
	/* (non-Javadoc)
	 * TODO Something is doing a list which is causing the previous revision to fail 
	 * @see org.tigris.subversion.subclipse.ui.actions.WorkspaceAction#isEnabled()
	 */
	protected boolean isEnabled() throws TeamException {
		return false;
	}
}
