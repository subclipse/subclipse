package org.tigris.subversion.subclipse.ui.dialogs;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.tigris.subversion.subclipse.ui.Policy;
import org.tigris.subversion.subclipse.ui.comments.CommitCommentArea;

public class LockDialog extends Dialog {
    private CommitCommentArea commitCommentArea;
    private Button stealButton;
    private String comment;
    private boolean stealLock;

    public LockDialog(Shell parentShell) {
        super(parentShell);
		int shellStyle = getShellStyle();
		setShellStyle(shellStyle | SWT.RESIZE);
        commitCommentArea = new CommitCommentArea(this, null, Policy.bind("LockDialog.enterComment"), null); //$NON-NLS-1$
    }
    
	protected Control createDialogArea(Composite parent) {
		getShell().setText(Policy.bind("LockDialog.title")); //$NON-NLS-1$
		Composite composite = new Composite(parent, SWT.NULL);
		composite.setLayout(new GridLayout());
		composite.setLayoutData(new GridData(GridData.FILL_BOTH));
		
		commitCommentArea.createArea(composite);
		
		stealButton = new Button(composite, SWT.CHECK);
		stealButton.setText(Policy.bind("LockDialog.stealLock")); //$NON-NLS-1$

		return composite;
	}

    protected void okPressed() {
        stealLock = stealButton.getSelection();
        comment = commitCommentArea.getComment();
        super.okPressed();
    }
    public String getComment() {
        return comment;
    }
    
    public boolean isStealLock() {
        return stealLock;
    }
}
