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

public class LoadErrorDialog extends SvnDialog {
	private String loadErrors;

	public LoadErrorDialog(Shell shell, String loadErrors) {
		super(shell, "passwordStores"); //$NON-NLS-1$
		this.loadErrors = loadErrors;
	}
	
	protected Control createDialogArea(Composite parent) {
		getShell().setText(Messages.LoadErrorDialog_0);
		Composite composite = new Composite(parent, SWT.NULL);
		composite.setLayout(new GridLayout());
		composite.setLayoutData(new GridData(GridData.FILL_BOTH));
		
		Label problemLabel = new Label(composite, SWT.WRAP);
		problemLabel.setText(Messages.LoadErrorDialog_1);
		GridData gd = new GridData();
		gd.widthHint = 500;
		problemLabel.setLayoutData(gd);
		
		new Label(composite, SWT.NONE);
		
		Label linkLabel = new Label(composite, SWT.WRAP);
		linkLabel.setText(Messages.LoadErrorDialog_2);
		gd = new GridData();
		gd.widthHint = 500;
		linkLabel.setLayoutData(gd);
		
		new Label(composite, SWT.NONE);
		
		FormToolkit toolkit = new FormToolkit(parent.getDisplay());
		toolkit.setBackground(parent.getBackground());
		Hyperlink infoLink = toolkit.createHyperlink(composite, "http://subclipse.tigris.org/wiki/JavaHL", SWT.NONE); //$NON-NLS-1$
		infoLink.addHyperlinkListener(new HyperlinkAdapter() {
			public void linkActivated(HyperlinkEvent evt) {
				try {
					PlatformUI.getWorkbench().getBrowserSupport().getExternalBrowser().openURL(new URL("http://subclipse.tigris.org/wiki/JavaHL")); //$NON-NLS-1$
				} catch (Exception e) {}
			}
        });
		
		new Label(composite, SWT.NULL);
		
		Group errorGroup = new Group(composite, SWT.NULL);
		GridLayout errorLayout = new GridLayout();
		errorGroup.setLayout(errorLayout);
		gd = new GridData(GridData.GRAB_HORIZONTAL | GridData.GRAB_VERTICAL | GridData.FILL_BOTH);
		errorGroup.setLayoutData(gd);
		errorGroup.setText(Messages.LoadErrorDialog_4);
		Text errorText = new Text(errorGroup, SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL | SWT.MULTI | SWT.WRAP);
        gd = new GridData(GridData.HORIZONTAL_ALIGN_FILL
                | GridData.GRAB_HORIZONTAL | GridData.VERTICAL_ALIGN_FILL
                | GridData.GRAB_VERTICAL);
        gd.heightHint = 300;
        errorText.setLayoutData(gd);
        
        if (loadErrors != null) {
        	errorText.setText(loadErrors);
        }
		
		return composite;
	}

}
