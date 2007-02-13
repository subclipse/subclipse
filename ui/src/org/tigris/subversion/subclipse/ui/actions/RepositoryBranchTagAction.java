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

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.widgets.Display;
import org.eclipse.team.core.TeamException;
import org.tigris.subversion.subclipse.core.ISVNRemoteResource;
import org.tigris.subversion.subclipse.core.ISVNRepositoryLocation;
import org.tigris.subversion.subclipse.core.SVNProviderPlugin;
import org.tigris.subversion.subclipse.ui.ISVNUIConstants;
import org.tigris.subversion.subclipse.ui.Policy;
import org.tigris.subversion.subclipse.ui.dialogs.BranchTagDialog;
import org.tigris.subversion.svnclientadapter.ISVNClientAdapter;
import org.tigris.subversion.svnclientadapter.SVNRevision;
import org.tigris.subversion.svnclientadapter.SVNUrl;

public class RepositoryBranchTagAction extends SVNAction {

	protected void execute(IAction action) throws InvocationTargetException, InterruptedException {
		ISVNRemoteResource[] resources = getSelectedRemoteResources();
		BranchTagDialog dialog = new BranchTagDialog(getShell(), resources[0]);
		if (dialog.open() == BranchTagDialog.OK) {
            final SVNUrl sourceUrl = dialog.getUrl();
            final SVNUrl destinationUrl = dialog.getToUrl();
            final String message = dialog.getComment();
            final SVNRevision revision = dialog.getRevision();
            BusyIndicator.showWhile(Display.getCurrent(), new Runnable() {
				public void run() {
					try {
						ISVNClientAdapter client = null;
						ISVNRepositoryLocation repository = SVNProviderPlugin.getPlugin().getRepository(sourceUrl.toString());
						if (repository != null)
							client = repository.getSVNClient();
						if (client == null)
							client = SVNProviderPlugin.getPlugin().getSVNClientManager().createSVNClient();
						client.copy(sourceUrl, destinationUrl, message, revision);
					} catch (Exception e) {
						MessageDialog.openError(getShell(), Policy.bind("BranchTagDialog.title"), e.getMessage());
					}
				}           	
            });
		}
	}

	protected boolean isEnabled() throws TeamException {
		return getSelectedRemoteResources().length == 1;
	}

	/*
	 * @see org.tigris.subversion.subclipse.ui.actions.ReplaceableIconAction#getImageId()
	 */
	protected String getImageId() {
		return ISVNUIConstants.IMG_MENU_BRANCHTAG;
	}

}
