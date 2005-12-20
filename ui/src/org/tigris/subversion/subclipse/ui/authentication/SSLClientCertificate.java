package org.tigris.subversion.subclipse.ui.authentication;

import java.util.Properties;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
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
import org.tigris.subversion.subclipse.ui.Policy;
import org.tigris.subversion.subclipse.ui.SVNUIPlugin;

public class SSLClientCertificate extends Dialog {
    private String realm;
    private String certificate;
    private String passphrase;
    private boolean save;
    private boolean maySave;
    private Combo keyFileCombo;
    private Text passphraseText;
    private Button keyFileButton;
    private Button browseButton;
    private Button saveButton;
    private Button okButton;
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
		browseButton.setEnabled(true);
		
		Label passphraseLabel = new Label(rtnGroup, SWT.NONE);
		passphraseLabel.setText(Policy.bind("SSHPromptDialog.passphrase")); //$NON-NLS-1$
		passphraseText = new Text(rtnGroup, SWT.BORDER);
		gd = new GridData();
		gd.widthHint = WIDTH;
		passphraseText.setEchoChar('*');
		passphraseText.setLayoutData(gd);
		passphraseText.setEnabled(true);
		
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
        certificate = keyFileCombo.getText().trim();
        passphrase = passphraseText.getText().trim();
        if (maySave) save = saveButton.getSelection();
        if (certificate.length() > 0) SVNUIPlugin.getPlugin().getRepositoryManager().getKeyFilesManager().addKeyFile(certificate);
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
}
