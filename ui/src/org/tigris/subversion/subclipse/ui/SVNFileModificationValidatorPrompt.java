package org.tigris.subversion.subclipse.ui;

import org.eclipse.core.resources.IFile;
import org.eclipse.swt.widgets.Display;
import org.tigris.subversion.subclipse.core.resources.ISVNFileModificationValidatorPrompt;
import org.tigris.subversion.subclipse.ui.dialogs.LockDialog;

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
			    LockDialog lockDialog = new LockDialog(Display.getCurrent().getActiveShell(), files);
                if (lockDialog.open() != LockDialog.CANCEL) {
                    success = true;
                    comment = lockDialog.getComment();
                    stealLock = lockDialog.isStealLock();
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
