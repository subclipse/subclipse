/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     Cédric Chabanois (cchabanois@ifrance.com) - modified for Subversion 
 *******************************************************************************/
package org.tigris.subversion.subclipse.ui.wizards;


import java.lang.reflect.InvocationTargetException;
import java.util.Properties;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.wizard.Wizard;
import org.tigris.subversion.subclipse.core.ISVNRemoteFolder;
import org.tigris.subversion.subclipse.core.SVNException;
import org.tigris.subversion.subclipse.ui.ISVNUIConstants;
import org.tigris.subversion.subclipse.ui.Policy;
import org.tigris.subversion.subclipse.ui.SVNUIPlugin;

/**
 * Wizard to add a new remote folder
 */
public class NewRemoteFolderWizard extends Wizard {
	private NewRemoteFolderWizardMainPage mainPage;
    private CommentCommitWizardPage commitCommentPage; 
    private ISVNRemoteFolder selection;
    private Dialog parentDialog;
	private Properties properties = null;
   
	public NewRemoteFolderWizard(ISVNRemoteFolder selection) {
		setWindowTitle(Policy.bind("NewRemoteFolderWizard.title")); //$NON-NLS-1$
        this.selection = selection;
	}

	/**
	 * Creates the wizard pages
	 */
	public void addPages() {
		// add the main page
        mainPage = new NewRemoteFolderWizardMainPage(
            "newRemoteFolderPage1",  //$NON-NLS-1$ 
            Policy.bind("NewRemoteFolderWizard.heading"), //$NON-NLS-1$
            SVNUIPlugin.getPlugin().getImageDescriptor(ISVNUIConstants.IMG_WIZBAN_NEW_FOLDER));
        mainPage.setParentFolder(selection);  
		addPage(mainPage);
        
        // add commit comment page
        String pageTitle = Policy.bind("CommentCommitWizardPage.pageTitle"); //$NON-NLS-1$
        String pageDescription = Policy.bind("CommentCommitWizardPage.pageDescription"); //$NON-NLS-1$
        ImageDescriptor image = SVNUIPlugin.getPlugin().getImageDescriptor(ISVNUIConstants.IMG_WIZBAN_NEW_FOLDER);
        commitCommentPage = new CommentCommitWizardPage(parentDialog, pageTitle, pageTitle, image, pageDescription);
        addPage(commitCommentPage); 
                
	}
    
	/*
	 * @see IWizard#performFinish
	 */
	public boolean performFinish() {
        try {
            SVNUIPlugin.runWithProgress(getContainer().getShell(), false /*cancelable*/, new IRunnableWithProgress() {
                public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
                    try {
                        ISVNRemoteFolder parentFolder = mainPage.getParentFolder();
                        parentFolder.createRemoteFolder(mainPage.getFolderName(),commitCommentPage.getComment(),monitor);
                    } catch (SVNException e) {
                        throw new InvocationTargetException(e);
                    }
                }
            });
        } catch (InterruptedException e) {
            // operation canceled
        } catch (InvocationTargetException e) {
            SVNUIPlugin.openError(getContainer().getShell(), Policy.bind("exception"), null, e.getCause(), SVNUIPlugin.PERFORM_SYNC_EXEC); //$NON-NLS-1$
            return false;
        }
	   return true;
	}
    
    /**
     * Method setParentDialog.
     * @param dialog
     */
    public void setParentDialog(Dialog dialog) {
        this.parentDialog = dialog;
    }    
    
}
