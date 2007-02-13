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
import org.eclipse.jface.action.IAction;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.Transfer;
import org.tigris.subversion.subclipse.core.ISVNRemoteResource;
import org.tigris.subversion.subclipse.ui.ISVNUIConstants;

/**
 * Copy selected remote resources to clipboard
 */
public class CopyRemoteResourceAction extends SVNAction {
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.tigris.subversion.subclipse.ui.actions.SVNAction#execute(org.eclipse.jface.action.IAction)
	 */
	protected void execute(IAction action) {
		ISVNRemoteResource remoteResources[] = getSelectedRemoteResources();
		Clipboard clipboard = new Clipboard(getShell().getDisplay());
		clipboard.setContents(new Object[]{remoteResources[0]},
				new Transfer[]{RemoteResourceTransfer.getInstance()});
		clipboard.dispose();
	}
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.team.internal.ui.actions.TeamAction#isEnabled()
	 */
	protected boolean isEnabled() {
		return getSelectedRemoteResources().length == 1;
	}

	/*
	 * @see org.tigris.subversion.subclipse.ui.actions.ReplaceableIconAction#getImageId()
	 */
	protected String getImageId() {
		return ISVNUIConstants.IMG_MENU_COPY;
	}
}