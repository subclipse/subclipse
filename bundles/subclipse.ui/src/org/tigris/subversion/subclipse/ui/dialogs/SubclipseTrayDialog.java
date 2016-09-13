package org.tigris.subversion.subclipse.ui.dialogs;

import org.eclipse.jface.dialogs.TrayDialog;
import org.eclipse.jface.window.IShellProvider;
import org.eclipse.swt.widgets.Shell;

public class SubclipseTrayDialog extends TrayDialog {

	public SubclipseTrayDialog(Shell shell) {
		super(shell);
	}

	public SubclipseTrayDialog(IShellProvider parentShell) {
		super(parentShell);
	}

	@Override
	public boolean isHelpAvailable() {
		return false;
	}

}
