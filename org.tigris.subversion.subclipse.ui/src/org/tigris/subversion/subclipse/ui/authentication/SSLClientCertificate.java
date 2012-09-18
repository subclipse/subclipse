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
package org.tigris.subversion.subclipse.ui.authentication;

import java.security.Provider;
import java.security.Security;
import java.util.Properties;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.tigris.subversion.subclipse.core.SVNProviderPlugin;
import org.tigris.subversion.subclipse.ui.Policy;
import org.tigris.subversion.subclipse.ui.SVNUIPlugin;
import org.tigris.subversion.subclipse.ui.dialogs.SubclipseTrayDialog;
import org.tigris.subversion.svnclientadapter.AbstractClientAdapter;

public class SSLClientCertificate extends SubclipseTrayDialog {
    private String realm;
    private String certificate;
    private String passphrase;
    private boolean save;
    private boolean maySave;
    private boolean msCapi;
    private Button mscapiButton;
    private Combo keyFileCombo;
    private Text passphraseText;
    private Text aliasText;
    private Button browseButton;
    private Button saveButton;
    private Button okButton;
    private Button aliasButton;
    private String alias;
    private String[] keyFiles = new String[0];

    private static int WIDTH = 300;

    public SSLClientCertificate(Shell parentShell, String realm, boolean maySave) {
        super(parentShell);
        this.realm = realm;
        this.maySave = maySave;
        keyFiles = SVNUIPlugin.getPlugin().getRepositoryManager().getKeyFilesManager().getPreviousKeyFiles();
    }

	protected Control createDialogArea(Composite parent) {
	    Composite rtnGroup = (Composite)super.createDialogArea(parent);
	    getShell().setText(Policy.bind("SSLPromptDialog.title")); //$NON-NLS-1$
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		rtnGroup.setLayout(layout);
		rtnGroup.setLayoutData(
		new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING));

		Label realmLabel = new Label(rtnGroup, SWT.NONE);
		realmLabel.setText(Policy.bind("PasswordPromptDialog.repository")); //$NON-NLS-1$
		Text realmText = new Text(rtnGroup, SWT.BORDER);
		GridData gd = new GridData();
		gd.widthHint = WIDTH;
		realmText.setLayoutData(gd);
		realmText.setEditable(false);
		realmText.setText(realm);
		boolean mscapiSupport = false;
		Provider pjacapi = Security.getProvider("CAPI"); //$NON-NLS-1$
        Provider pmscapi = Security.getProvider("SunMSCAPI"); //$NON-NLS-1$
        // Check that Java supports MSCAPI
        if (pmscapi != null) {
        	try {
        		ClassLoader.getSystemClassLoader().loadClass("sun.security.mscapi.NONEwithRSASignature");
			} catch (Exception e1) {
				pmscapi = null;
			}
        }
        
        String svnClientText = ""; //$NON-NLS-1$
        if (SVNProviderPlugin.getPlugin() != null && SVNProviderPlugin.getPlugin().getSVNClientManager() != null) {
        	svnClientText = SVNProviderPlugin.getPlugin().getSVNClientManager().getSvnClientInterface();
		}
        // ms capi is only suported for windows and for provider SunMSCAPI and JACAPI from keyon
        // further ms capi is only supported from svnkit as client!
        if (AbstractClientAdapter.isOsWindows() && (pjacapi != null || pmscapi != null) && "svnkit".equals(svnClientText) ) { //$NON-NLS-1$
			mscapiSupport = true;
		    mscapiButton = new Button(rtnGroup, SWT.CHECK);
		    mscapiButton.setText(Policy.bind("SSLClientCertificate.0")); //$NON-NLS-1$
		    gd = new GridData();
		    gd.horizontalSpan = 2;
		    mscapiButton.setLayoutData(gd);
	    	SelectionListener mscapiSelectionListener = new SelectionAdapter() {
				public void widgetSelected(SelectionEvent e) {
					if (mscapiButton.getSelection()) {
						keyFileCombo.setEnabled(false);
						browseButton.setEnabled(false);
						passphraseText.setEnabled(false);
						aliasButton.setEnabled(true);
					} else {
						keyFileCombo.setEnabled(true);
						browseButton.setEnabled(true);
						passphraseText.setEnabled(true);
						aliasButton.setEnabled(false);
						aliasText.setText(""); //$NON-NLS-1$
					}
				}
			};
			mscapiButton.addSelectionListener(mscapiSelectionListener);
			Label aliasLabel = new Label(rtnGroup, SWT.NONE);
			aliasLabel.setText(Policy.bind("SSLClientCertificate.1")); //$NON-NLS-1$

			Composite ailiasSelectGroup = new Composite(rtnGroup, SWT.NONE);
			GridLayout aliasLayout = new GridLayout();
			aliasLayout.numColumns = 2;
			aliasLayout.marginWidth = 0;
			aliasLayout.marginHeight = 0;
			ailiasSelectGroup.setLayout(aliasLayout);

			gd = new GridData(GridData.GRAB_HORIZONTAL | GridData.HORIZONTAL_ALIGN_FILL);
			ailiasSelectGroup.setLayoutData(gd);
			aliasText = new Text(ailiasSelectGroup, SWT.BORDER);
			gd = new GridData();
			gd.widthHint = WIDTH;
			aliasText.setLayoutData(gd);
			aliasText.setEnabled(false);
			aliasButton = new Button(ailiasSelectGroup, SWT.PUSH);
			aliasButton.setText(Policy.bind("SSLClientCertificate.2")); //$NON-NLS-1$
			SelectionListener msCapiCertificateSelectionListener = new SelectionAdapter() {
				public void widgetSelected(SelectionEvent e) {
					SSLClientCertificatesMSCapi dialog = new SSLClientCertificatesMSCapi(SVNUIPlugin.getStandardDisplay().getActiveShell(), realm);
			        if (dialog.open() == SSLClientCertificatesMSCapi.OK) {
			            aliasText.setText(dialog.getAlias());
			        }
				}
			};
			aliasButton.addSelectionListener(msCapiCertificateSelectionListener);
			aliasButton.setEnabled(false);
		}

		Label keyFileLabel = new Label (rtnGroup, SWT.NONE);
		keyFileLabel.setText(Policy.bind("SSLPromptDialog.certificate")); //$NON-NLS-1$

		Composite keyFileGroup = new Composite(rtnGroup, SWT.NONE);
		GridLayout keyFileLayout = new GridLayout();
		keyFileLayout.numColumns = 2;
		keyFileLayout.marginWidth = 0;
		keyFileLayout.marginHeight = 0;
		keyFileGroup.setLayout(keyFileLayout);
		gd = new GridData(GridData.GRAB_HORIZONTAL | GridData.HORIZONTAL_ALIGN_FILL);
		keyFileGroup.setLayoutData(gd);

		keyFileCombo = new Combo(keyFileGroup, SWT.BORDER);
		gd = new GridData();
		gd.widthHint = WIDTH;
		keyFileCombo.setLayoutData(gd);
		keyFileCombo.setEnabled(true);
		boolean msCapiPreselected = false;
		if (mscapiSupport) {
			if (keyFiles != null && keyFiles.length > 0) {
				if (keyFiles[0] != null && keyFiles[0].startsWith("MSCAPI")) { //$NON-NLS-1$
					msCapiPreselected = true;
					keyFileCombo.setEnabled(false);
					aliasButton.setEnabled(true);
					mscapiButton.setSelection(true);
					if (keyFiles[0].split(";").length > 1) { //$NON-NLS-1$
						aliasText.setText(keyFiles[0].split(";")[1]); //$NON-NLS-1$
					}
				}
				boolean textset = false;
				for (int i = 0; i < keyFiles.length; i++) {
					if (keyFiles[i] != null && keyFiles[i].startsWith("MSCAPI")) { //$NON-NLS-1$
						continue;
					}
					if (!textset) {
						keyFileCombo.setText(keyFiles[i]);
						textset = true;
					}
					keyFileCombo.add(keyFiles[i]);
				}
			}
		} else {
			/* origin */
			if (keyFiles != null && keyFiles.length > 0) {
				for (int i = 0; i < keyFiles.length; i++) keyFileCombo.add(keyFiles[i]);
				keyFileCombo.setText(keyFiles[0]);
			}
		}

		browseButton = new Button(keyFileGroup, SWT.PUSH);
		browseButton.setText(Policy.bind("SSHPromptDialog.browse")); //$NON-NLS-1$
		browseButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				Properties properties = System.getProperties();
				String home = (String)properties.get("user.home"); //$NON-NLS-1$
				FileDialog fileDialog = new FileDialog(getShell(), SWT.OPEN);
				if (home != null) fileDialog.setFilterPath(home);
				String returnFile = fileDialog.open();
				if (returnFile != null) keyFileCombo.setText(returnFile);
			}
		});
		browseButton.setEnabled(!msCapiPreselected);

		Label passphraseLabel = new Label(rtnGroup, SWT.NONE);
		passphraseLabel.setText(Policy.bind("SSHPromptDialog.passphrase")); //$NON-NLS-1$
		passphraseText = new Text(rtnGroup, SWT.BORDER);
		gd = new GridData();
		gd.widthHint = WIDTH;
		passphraseText.setEchoChar('*');
		passphraseText.setLayoutData(gd);
		passphraseText.setEnabled(!msCapiPreselected);

		if (maySave) {
		    saveButton = new Button(rtnGroup, SWT.CHECK);
		    saveButton.setText(Policy.bind("SSHPromptDialog.save")); //$NON-NLS-1$
		    gd = new GridData();
		    gd.horizontalSpan = 2;
		    saveButton.setLayoutData(gd);
		}
		return rtnGroup;
	}

	public Button createButton(Composite parent, int id, String label, boolean isDefault) {
		Button button = super.createButton(parent, id, label, isDefault);
		if (id == IDialogConstants.OK_ID) {
		    okButton = button;
		    okButton.setEnabled(true);
		}
		return button;
	}

    protected void okPressed() {
        passphrase = passphraseText.getText().trim();
        if (aliasText != null) {
        	alias = aliasText.getText().trim();
        }
        if (mscapiButton != null) { 
        	msCapi = mscapiButton.getSelection();
        }
        if (msCapi) {
       		certificate = "MSCAPI"+(alias != null && !"".equals(alias)?";"+alias:""); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
        } else {
        	certificate = keyFileCombo.getText().trim();
        }
        if (certificate.length() > 0) SVNUIPlugin.getPlugin().getRepositoryManager().getKeyFilesManager().addKeyFile(certificate);
        if (maySave) save = saveButton.getSelection();
        super.okPressed();
    }
    public boolean isSave() {
        return save;
    }

    public String getKeyFile() {
        return certificate;
    }

    public String getPassphrase() {
    	return passphrase;
    }

    public boolean isMSCapi() {
    	return msCapi;
    }

    public String getAlias() {
    	return alias;
    }
}
