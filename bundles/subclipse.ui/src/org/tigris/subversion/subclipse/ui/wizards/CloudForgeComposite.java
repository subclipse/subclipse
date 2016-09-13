package org.tigris.subversion.subclipse.ui.wizards;

import java.net.URL;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.ImageHyperlink;
import org.tigris.subversion.subclipse.ui.ISVNUIConstants;
import org.tigris.subversion.subclipse.ui.SVNUIPlugin;

public class CloudForgeComposite extends Composite {
	public final static String SIGNUP_URL = "https://app.cloudforge.com/subscriptions/new/?product=Free&source=subclipse";

	public CloudForgeComposite(Composite parent, int style) {
		super(parent, style);
		setLayout(new GridLayout());
		createControls();
	}
	
	private void createControls() {	
		Composite cloudForgeComposite = new Composite(this, SWT.NULL);
		GridLayout layout = new GridLayout();
		layout.numColumns = 1;
		cloudForgeComposite.setLayout(layout);
		GridData data = new GridData(GridData.GRAB_HORIZONTAL | GridData.FILL_HORIZONTAL);
		cloudForgeComposite.setLayoutData(data);
		
		ImageHyperlink cloudForgeLink = new ImageHyperlink(cloudForgeComposite, SWT.NONE);
		cloudForgeLink.setImage(SVNUIPlugin.getPlugin().getImageDescriptor(ISVNUIConstants.IMG_CLOUDFORGE).createImage());
		cloudForgeLink.addHyperlinkListener(new HyperlinkAdapter() {
			@Override
			public void linkActivated(HyperlinkEvent evt) {
				try {
					PlatformUI.getWorkbench().getBrowserSupport().getExternalBrowser().openURL(new URL(SIGNUP_URL));
				} catch (Exception e) {
					MessageDialog.openError(getShell(), "Sign-up for CloudForge", e.getMessage());
				}
			}			
		});
		cloudForgeLink.setToolTipText(SIGNUP_URL);
	}

}
