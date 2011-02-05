package org.tigris.subversion.subclipse.ui.dialogs;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.tigris.subversion.subclipse.ui.ISVNUIConstants;
import org.tigris.subversion.subclipse.ui.Policy;
import org.tigris.subversion.subclipse.ui.SVNUIPlugin;

public class CommitToTagsWarningDialog extends Dialog {
	private Button doNotShowWarningAgainButton;

	public CommitToTagsWarningDialog(Shell parentShell) {
		super(parentShell);
	}
	
	protected Control createDialogArea(Composite parent) {
		getShell().setText(Policy.bind("CommitDialog.title")); //$NON-NLS-1$
		Composite composite = new Composite(parent, SWT.NULL);
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		composite.setLayout(layout);
		composite.setLayoutData(new GridData(GridData.FILL_BOTH));
		
		Label label = new Label(composite, SWT.NONE);
		label.setImage(SVNUIPlugin.getPlugin().getImageDescriptor(ISVNUIConstants.IMG_WARNING).createImage());
		
		Label warningLabel = new Label(composite, SWT.WRAP);
		GridData gd = new GridData();
		gd.widthHint = 500;
		warningLabel.setLayoutData(gd);
		warningLabel.setText(Policy.bind("CommitDialog.tag")); //$NON-NLS-1$
		
		new Label(composite, SWT.NONE); new Label(composite, SWT.NONE);
		
		doNotShowWarningAgainButton = new Button(composite, SWT.CHECK);
		doNotShowWarningAgainButton.setText(Policy.bind("CommitDialog.doNotShowTagWarningAgain")); //$NON-NLS-1$
		gd = new GridData();
		gd.horizontalSpan = 2;
		doNotShowWarningAgainButton.setLayoutData(gd);
		
		return composite;
	}

	protected void okPressed() {
		if (doNotShowWarningAgainButton.getSelection()) {
			IPreferenceStore preferenceStore = SVNUIPlugin.getPlugin().getPreferenceStore();
			preferenceStore.setValue(ISVNUIConstants.PREF_COMMIT_TO_TAGS_PATH_WITHOUT_WARNING, true);
		}
		super.okPressed();
	}

	protected Button createButton(Composite parent, int id, String label, boolean defaultButton) {
		Button button;
		if (id == IDialogConstants.OK_ID) {
			button = super.createButton(parent, id, IDialogConstants.YES_LABEL, defaultButton);
		}
		else if (id == IDialogConstants.CANCEL_ID) {
			button = super.createButton(parent, id, IDialogConstants.NO_LABEL, defaultButton);
		}
		else {
			button = super.createButton(parent, id, label, defaultButton);
		}
		return button;
	}

}
