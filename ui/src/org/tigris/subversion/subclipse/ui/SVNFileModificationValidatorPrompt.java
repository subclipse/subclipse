package org.tigris.subversion.subclipse.ui;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.tigris.subversion.subclipse.core.resources.ISVNFileModificationValidatorPrompt;
import org.tigris.subversion.subclipse.ui.dialogs.LockDialog;

public class SVNFileModificationValidatorPrompt implements ISVNFileModificationValidatorPrompt {
    private String comment;
    private boolean stealLock;
    private boolean success;
    
    public boolean prompt(Object context) {
        if (context == null) {
            comment = "";
            stealLock = false;
            return true;
        }
        success = false;
		SVNUIPlugin.getStandardDisplay().syncExec(new Runnable() {
			public void run() {
		       boolean lock = MessageDialog.openQuestion(Display.getCurrent().getActiveShell(), "Lock",
		                "One or more resource is read-only and must be locked to be edited.  Do you wish to lock resources now?");
		       if (lock) {
			       LockDialog lockDialog = new LockDialog(Display.getCurrent().getActiveShell());
			       if (lockDialog.open() != LockDialog.CANCEL) {
			           success = true;
			           comment = lockDialog.getComment();
			           stealLock = lockDialog.isStealLock();
			       }
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
