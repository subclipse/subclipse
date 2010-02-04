package org.tigris.subversion.subclipse.ui.dialogs;

import java.net.URL;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Hyperlink;
import org.tigris.subversion.subclipse.ui.Messages;
import org.tigris.subversion.subclipse.ui.SVNUIPlugin;

public class UnsupportedPasswordStoresDialog extends SvnDialog {

	public UnsupportedPasswordStoresDialog(Shell shell) {
		super(shell, "passwordStores"); //$NON-NLS-1$
	}
	
	protected Control createDialogArea(Composite parent) {
		getShell().setText(Messages.UnsupportedPasswordStoresDialog_0);
		Composite composite = new Composite(parent, SWT.NULL);
		composite.setLayout(new GridLayout());
		composite.setLayoutData(new GridData(GridData.FILL_BOTH));
		
		Label problemLabel = new Label(composite, SWT.WRAP);
		problemLabel.setText(Messages.UnsupportedPasswordStoresDialog_1);
		GridData gd = new GridData();
		gd.widthHint = 500;
		problemLabel.setLayoutData(gd);
		
		new Label(composite, SWT.NONE);
		
		Composite linkGroup = new Composite(composite, SWT.NULL);
		GridLayout linkLayout = new GridLayout();
		linkLayout.numColumns = 2;
		linkLayout.marginWidth = 0;
		linkLayout.marginHeight = 0;
		linkGroup.setLayout(linkLayout);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		linkGroup.setLayoutData(gd);
		
		Label linkLabel = new Label(linkGroup, SWT.NONE);
		linkLabel.setText(Messages.UnsupportedPasswordStoresDialog_2);

		FormToolkit toolkit = new FormToolkit(parent.getDisplay());
		toolkit.setBackground(parent.getBackground());
		Hyperlink infoLink = toolkit.createHyperlink(linkGroup, Messages.UnsupportedPasswordStoresDialog_3, SWT.NONE);
		infoLink.addHyperlinkListener(new HyperlinkAdapter() {
			public void linkActivated(HyperlinkEvent evt) {
				try {
					PlatformUI.getWorkbench().getBrowserSupport().getExternalBrowser().openURL(new URL("http://subclipse.tigris.org/wiki/JavaHL#head-3a1d2d3c54791d2d751794e5d6645f1d77d95b32")); //$NON-NLS-1$
				} catch (Exception e) {}
			}
        });
		
		new Label(linkGroup, SWT.NONE);
		
		Group configGroup = new Group(composite, SWT.NULL);
		GridLayout configLayout = new GridLayout();
		configLayout.numColumns = 2;
		configLayout.marginWidth = 0;
		configLayout.marginHeight = 0;
		configGroup.setLayout(configLayout);
		gd = new GridData(GridData.GRAB_HORIZONTAL | GridData.FILL_HORIZONTAL);
		configGroup.setLayoutData(gd);
		configGroup.setText(Messages.UnsupportedPasswordStoresDialog_5);
		
		Label fileLabel = new Label(configGroup, SWT.NONE);
		fileLabel.setText(Messages.UnsupportedPasswordStoresDialog_6);
		Text fileText = new Text(configGroup, SWT.READ_ONLY | SWT.BORDER);
		gd = new GridData(GridData.GRAB_HORIZONTAL | GridData.FILL_HORIZONTAL);
		fileText.setLayoutData(gd);
		fileText.setText(SVNUIPlugin.getPlugin().getConfigFile().getAbsolutePath());
		Label storesLabel = new Label(configGroup, SWT.NONE);
		storesLabel.setText(Messages.UnsupportedPasswordStoresDialog_7);
		Text storesText = new Text(configGroup, SWT.READ_ONLY | SWT.BORDER);
		gd = new GridData(GridData.GRAB_HORIZONTAL | GridData.FILL_HORIZONTAL);
		storesText.setLayoutData(gd);
		String passwordStores = SVNUIPlugin.getPlugin().getPasswordStores();
		if (passwordStores == null) {
			passwordStores = "gnome-keyring";
		}
		storesText.setText(passwordStores);
		
		new Label(composite, SWT.NONE);
		
		Label editLabel = new Label(composite, SWT.WRAP);
		editLabel.setText(Messages.UnsupportedPasswordStoresDialog_8);
		gd = new GridData();
		gd.widthHint = 500;
		editLabel.setLayoutData(gd);
		
		fileText.setFocus();
		
		return composite;
	}

}
