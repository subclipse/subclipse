package org.tigris.subversion.subclipse.ui.dialogs;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.tigris.subversion.subclipse.ui.Policy;

public class DeletePropertyDialog extends Dialog {
	private String message;
	private boolean directory;
	private Button recurseButton;
	private boolean recurse;

	public DeletePropertyDialog(Shell parentShell, String message, boolean directory) {
		super(parentShell);	
		this.message = message;
		this.directory = directory;
	}
	
	protected Control createDialogArea(Composite parent) {
		getShell().setText(Policy.bind("SVNPropertyDeleteAction.title")); //$NON-NLS-1$
		Composite composite = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		composite.setLayout(layout);
		
		Label questionLabel = new Label(composite, SWT.NONE);
		questionLabel.setImage(getShell().getDisplay().getSystemImage(SWT.ICON_QUESTION));
		
		Label confirmLabel = new Label(composite, SWT.WRAP);
		GridData data = new GridData();
		data.widthHint = 400;
		confirmLabel.setText(message);
		
		if (directory) {
			new Label(composite, SWT.NONE);		
			recurseButton = new Button(composite, SWT.CHECK);
			recurseButton.setText(Policy.bind("SVNPropertyDeleteAction.recurse")); //$NON-NLS-1$
			data = new GridData();
			data.horizontalSpan = 2;
			recurseButton.setLayoutData(data);
		}
		return composite;
	}

	protected void okPressed() {
		if (directory) recurse = recurseButton.getSelection();
		super.okPressed();
	}

	public boolean isRecurse() {
		return recurse;
	}

	protected Image getImage() {
		return getShell().getDisplay().getSystemImage(SWT.ICON_QUESTION);
	}		

}
