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

import java.util.Properties;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
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
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;
import org.tigris.subversion.subclipse.ui.IHelpContextIds;
import org.tigris.subversion.subclipse.ui.Policy;
import org.tigris.subversion.subclipse.ui.SVNUIPlugin;
import org.tigris.subversion.subclipse.ui.dialogs.SubclipseTrayDialog;

public class SSHPromptDialog extends SubclipseTrayDialog {
    private String realm;
    private String username;
    private String password;
    private int sshPort;
    private String keyFile;
    private String passphrase;
    private boolean save;
    private boolean maySave;
    private Text userText;
    private Text passwordText;
    private Combo keyFileCombo;
    private Text passphraseText;
    private Button passwordButton;
    private Button keyFileButton;
    private Button browseButton;
    private Text sshPortText;
    private boolean portError;
    private Button saveButton;
    private Button okButton;
    private String[] keyFiles = new String[0];
    
    private static int WIDTH = 300;

    public SSHPromptDialog(Shell parentShell, String realm, String username, int sshPort, boolean maySave) {
        super(parentShell);
        this.realm = realm;
        this.username = username;
        this.sshPort = sshPort;
        this.maySave = maySave;
        keyFiles = SVNUIPlugin.getPlugin().getRepositoryManager().getKeyFilesManager().getPreviousKeyFiles();
    }
    
	protected Control createDialogArea(Composite parent) {
	    Composite rtnGroup = (Composite)super.createDialogArea(parent);
	    getShell().setText(Policy.bind("SSHPromptDialog.title")); //$NON-NLS-1$
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
		
		Label userLabel = new Label(rtnGroup, SWT.NONE);
		userLabel.setText(Policy.bind("PasswordPromptDialog.username")); //$NON-NLS-1$
		userText = new Text(rtnGroup, SWT.BORDER);
		gd = new GridData();
		gd.widthHint = WIDTH;
		userText.setLayoutData(gd);
		userText.setText(username == null? "": username);
		
		Group radioGroup = new Group(rtnGroup, SWT.NONE);
		radioGroup.setText(Policy.bind("SSHPromptDialog.authentication")); //$NON-NLS-1$
		GridLayout radioLayout = new GridLayout();
		radioLayout.numColumns = 1;
		radioGroup.setLayout(radioLayout);
		gd = new GridData(GridData.GRAB_HORIZONTAL | GridData.HORIZONTAL_ALIGN_FILL);
		gd.horizontalSpan = 2;
		radioGroup.setLayoutData(gd);
		
		passwordButton = new Button(radioGroup, SWT.RADIO);
		passwordButton.setText(Policy.bind("SSHPromptDialog.passwordauth")); //$NON-NLS-1$
		
		keyFileButton = new Button(radioGroup, SWT.RADIO);
		keyFileButton.setText(Policy.bind("SSHPromptDialog.keyauth")); //$NON-NLS-1$
		
		passwordButton.setSelection(true);
		
		Label pwdLabel = new Label(rtnGroup, SWT.NONE);
		pwdLabel.setText(Policy.bind("PasswordPromptDialog.password")); //$NON-NLS-1$
		passwordText = new Text(rtnGroup, SWT.BORDER);
		gd = new GridData();
		gd.widthHint = WIDTH;
		passwordText.setLayoutData(gd);
		passwordText.setEchoChar('*');
		
		Label keyFileLabel = new Label (rtnGroup, SWT.NONE);
		keyFileLabel.setText(Policy.bind("SSHPromptDialog.keyfile")); //$NON-NLS-1$
		
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
		keyFileCombo.setEnabled(false);
		if (keyFiles != null && keyFiles.length > 0) {
			for (int i = 0; i < keyFiles.length; i++) keyFileCombo.add(keyFiles[i]);
			keyFileCombo.setText(keyFiles[0]);
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
		browseButton.setEnabled(false);
		
		Label passphraseLabel = new Label(rtnGroup, SWT.NONE);
		passphraseLabel.setText(Policy.bind("SSHPromptDialog.passphrase")); //$NON-NLS-1$
		passphraseText = new Text(rtnGroup, SWT.BORDER);
		gd = new GridData();
		gd.widthHint = WIDTH;
		passphraseText.setEchoChar('*');
		passphraseText.setLayoutData(gd);
		passphraseText.setEnabled(false);
		
		Label portLabel = new Label(rtnGroup, SWT.NONE);
		portLabel.setText(Policy.bind("SSHPromptDialog.port")); //$NON-NLS-1$
		sshPortText = new Text(rtnGroup, SWT.BORDER);
		gd = new GridData();
		gd.widthHint = 40;
		sshPortText.setLayoutData(gd);
		sshPortText.setTextLimit(5);
		sshPortText.setText(Integer.toString(sshPort));
		
		if (maySave) {
		    saveButton = new Button(rtnGroup, SWT.CHECK);
		    saveButton.setText(Policy.bind("SSHPromptDialog.save")); //$NON-NLS-1$
		    gd = new GridData();
		    gd.horizontalSpan = 2;
		    saveButton.setLayoutData(gd);
		}
		
		ModifyListener modifyListener = new ModifyListener() {
            public void modifyText(ModifyEvent me) {
                okButton.setEnabled(canFinish());
            }		    
		};
		
		userText.addModifyListener(modifyListener);
		passwordText.addModifyListener(modifyListener);
		keyFileCombo.addModifyListener(modifyListener);
		
		SelectionListener selectionListener = new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				if (passwordButton.getSelection()) {
					keyFileCombo.setEnabled(false);
					browseButton.setEnabled(false);
					passphraseText.setEnabled(false);
					passwordText.setEnabled(true);
					passwordText.setFocus();
				} else {
					keyFileCombo.setEnabled(true);
					browseButton.setEnabled(true);
					passphraseText.setEnabled(true);
					passwordText.setEnabled(false);
					keyFileCombo.setFocus();
				}
				okButton.setEnabled(canFinish());
			}
		};
		
		passwordButton.addSelectionListener(selectionListener);
		keyFileButton.addSelectionListener(selectionListener);
		
		// set F1 help
		PlatformUI.getWorkbench().getHelpSystem().setHelp(rtnGroup, IHelpContextIds.SSH_PROMPT_DIALOG);	

		if (username != null) passwordText.setFocus();
		else userText.setFocus();
		
		return rtnGroup;
	}
	
	private boolean canFinish() {
		if (userText.getText().trim().length() == 0) return false;
		if (passwordButton.getSelection()) return passwordText.getText().trim().length() > 0;
		else return keyFileCombo.getText().trim().length() > 0;
	}
	
	public Button createButton(Composite parent, int id, String label, boolean isDefault) {
		Button button = super.createButton(parent, id, label, isDefault);
		if (id == IDialogConstants.OK_ID) {
		    okButton = button;
		    okButton.setEnabled(false);
		}
		return button;
	}

    protected void okPressed() {
    	portError = false;
    	try {
    		sshPort = Integer.parseInt(sshPortText.getText().trim());
    	} catch (Exception e) {
    		portError = true;
    		MessageDialog.openError(getShell(), Policy.bind("SSHPromptDialog.invalidPortTitle"), Policy.bind("SSHPromptDialog.invalidPortMessage")); //$NON-NLS-1$ //$NON-NLS-1$
    	}
    	if (portError) return;
        username = userText.getText().trim();
        password = passwordText.getText().trim();
		if (passwordButton.getSelection()) {
		    keyFile = null;
		    passphrase = null;
		} else {
	        keyFile = keyFileCombo.getText().trim();
	        passphrase = passphraseText.getText().trim();
		}
        if (maySave) save = saveButton.getSelection();
        if (keyFile != null && keyFile.length() > 0) SVNUIPlugin.getPlugin().getRepositoryManager().getKeyFilesManager().addKeyFile(keyFile);
        super.okPressed();
    }
    public boolean isSave() {
        return save;
    }
    public String getPassword() {
        return password;
    }
    
    public String getUsername() {
        return username;
    }
    
    public int getSshPort() {
        return sshPort;
    }
    
    public String getKeyFile() {
        return keyFile;
    }
    
    public String getPassphrase() {
    	return passphrase;
    }
}
