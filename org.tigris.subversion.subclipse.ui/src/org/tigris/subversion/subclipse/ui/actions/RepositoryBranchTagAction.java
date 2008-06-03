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
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.team.core.TeamException;
import org.tigris.subversion.subclipse.core.ISVNRemoteResource;
import org.tigris.subversion.subclipse.core.ISVNRepositoryLocation;
import org.tigris.subversion.subclipse.core.SVNProviderPlugin;
import org.tigris.subversion.subclipse.ui.ISVNUIConstants;
import org.tigris.subversion.subclipse.ui.Policy;
import org.tigris.subversion.subclipse.ui.operations.RepositoryBranchTagOperation;
import org.tigris.subversion.subclipse.ui.wizards.BranchTagWizard;
import org.tigris.subversion.subclipse.ui.wizards.ClosableWizardDialog;
import org.tigris.subversion.svnclientadapter.ISVNClientAdapter;
import org.tigris.subversion.svnclientadapter.SVNRevision;
import org.tigris.subversion.svnclientadapter.SVNUrl;

public class RepositoryBranchTagAction extends SVNAction {

	protected void execute(IAction action) throws InvocationTargetException, InterruptedException {
		ISVNRemoteResource[] resources = getSelectedRemoteResources();
    	BranchTagWizard wizard = new BranchTagWizard(resources);
    	WizardDialog dialog = new ClosableWizardDialog(getShell(), wizard);
    	if (dialog.open() == WizardDialog.OK) {	
    	  SVNUrl[] sourceUrls = wizard.getUrls();
          SVNUrl destinationUrl = wizard.getToUrl();
          String message = wizard.getComment();
          SVNRevision revision = wizard.getRevision();
          boolean makeParents = wizard.isMakeParents();
		  try {
			  ISVNClientAdapter client = null;
			  ISVNRepositoryLocation repository = SVNProviderPlugin.getPlugin().getRepository(sourceUrls[0].toString());
			  if (repository != null)
			  	client = repository.getSVNClient();
			  if (client == null)
				client = SVNProviderPlugin.getPlugin().getSVNClientManager().getSVNClient();
			  RepositoryBranchTagOperation branchTagOperation = new RepositoryBranchTagOperation(getTargetPart(), client, sourceUrls, destinationUrl, revision, message, makeParents);
			  branchTagOperation.run();
		  } catch (Exception e) {
			  MessageDialog.openError(getShell(), Policy.bind("BranchTagDialog.title"), e.getMessage());
		  }  		
    	}
//		SvnWizardBranchTagPage branchTagPage = new SvnWizardBranchTagPage(resources[0]);
//    	SvnWizard wizard = new SvnWizard(branchTagPage);
//        SvnWizardDialog dialog = new SvnWizardDialog(getShell(), wizard);
//        wizard.setParentDialog(dialog);    	
//		if (dialog.open() == SvnWizardDialog.OK) {
//            final SVNUrl sourceUrl = branchTagPage.getUrl();
//            final SVNUrl destinationUrl = branchTagPage.getToUrl();
//            final String message = branchTagPage.getComment();
//            final SVNRevision revision = branchTagPage.getRevision();
//            final boolean makeParents = branchTagPage.isMakeParents();
//            BusyIndicator.showWhile(Display.getCurrent(), new Runnable() {
//				public void run() {
//					try {
//						ISVNClientAdapter client = null;
//						ISVNRepositoryLocation repository = SVNProviderPlugin.getPlugin().getRepository(sourceUrl.toString());
//						if (repository != null)
//							client = repository.getSVNClient();
//						if (client == null)
//							client = SVNProviderPlugin.getPlugin().getSVNClientManager().createSVNClient();
//						client.copy(sourceUrl, destinationUrl, message, revision, makeParents);
//					} catch (Exception e) {
//						MessageDialog.openError(getShell(), Policy.bind("BranchTagDialog.title"), e.getMessage());
//					}
//				}           	
//            });
//		}
	}

	protected boolean isEnabled() throws TeamException {
		ISVNRepositoryLocation repository = null;
		ISVNRemoteResource[] resources = getSelectedRemoteResources();
		for (int i = 0; i < resources.length; i++) {
			if (repository != null && !(resources[i].getRepository().equals(repository))) return false;
			repository = resources[i].getRepository();
		}
		return true;
	}

	/*
	 * @see org.tigris.subversion.subclipse.ui.actions.ReplaceableIconAction#getImageId()
	 */
	protected String getImageId() {
		return ISVNUIConstants.IMG_MENU_BRANCHTAG;
	}

}
