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
package org.tigris.subversion.subclipse.ui;

import org.eclipse.core.resources.IFile;
import org.eclipse.swt.widgets.Display;
import org.tigris.subversion.subclipse.core.resources.ISVNFileModificationValidatorPrompt;
import org.tigris.subversion.subclipse.ui.wizards.dialogs.SvnWizard;
import org.tigris.subversion.subclipse.ui.wizards.dialogs.SvnWizardDialog;
import org.tigris.subversion.subclipse.ui.wizards.dialogs.SvnWizardLockPage;

public class SVNFileModificationValidatorPrompt implements ISVNFileModificationValidatorPrompt {
    private String comment;
    private boolean stealLock;
    private boolean success;
    private IFile[] files;
    
    public boolean prompt(IFile[] lockFiles, Object context) {
        if (context == null) {
            comment = "";
            stealLock = false;
            return true;
        }
        this.files = lockFiles;
        success = false;
		SVNUIPlugin.getStandardDisplay().syncExec(new Runnable() {
			public void run() {
				SvnWizardLockPage lockPage = new SvnWizardLockPage(files);
		        SvnWizard wizard = new SvnWizard(lockPage);
		        SvnWizardDialog dialog = new SvnWizardDialog(Display.getCurrent().getActiveShell(), wizard);
		        wizard.setParentDialog(dialog);			    
		        if (dialog.open() == SvnWizardDialog.OK) {
                    success = true;
                    comment = lockPage.getComment();
                    stealLock = lockPage.isStealLock();
                }
			}
		});        
		return success;
   }

    public String getComment() {
        return comment;
    }

    public boolean isStealLock() {
        return stealLock;
    }

}
